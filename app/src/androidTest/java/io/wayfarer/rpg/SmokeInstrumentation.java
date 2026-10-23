package io.wayfarer.rpg;

import android.app.*;
import android.content.Intent;
import android.graphics.*;
import android.os.*;
import android.view.*;
import io.wayfarer.rpg.core.*;
import java.io.*;

/** Framework-only UI smoke runner. No test service, account or third-party runtime. */
public final class SmokeInstrumentation extends Instrumentation {
    private MainActivity activity;
    private Throwable error;
    private void ui(Runnable job){runOnMainSync(()->{try{job.run();}catch(Throwable t){error=t;}});if(error!=null)throw new AssertionError(error);}
    private void check(boolean ok,String message){if(!ok)throw new AssertionError(message);}
    @Override public void onCreate(Bundle args){super.onCreate(args);start();}
    @Override public void onStart(){
        Bundle result=new Bundle();
        try{
            Intent launch=new Intent(getTargetContext(),MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_CLEAR_TASK);
            activity=(MainActivity)startActivitySync(launch);waitForIdleSync();SystemClock.sleep(700);
            ui(()->{activity.world.replace(new ActionGame());check(activity.world.getWidth()>activity.world.getHeight(),"landscape viewport");});
            screenshot("01-camp.png");
            tap(440,230);ui(()->check(activity.world.game.hero==1,"character card selects archer"));
            tap(785,405);SystemClock.sleep(150);ui(()->check(activity.world.game.mode==ActionGame.Mode.PLAY,"start button enters dungeon"));
            float[] initial={0,0};ui(()->{initial[0]=activity.world.game.x;initial[1]=activity.world.game.y;});
            long down=SystemClock.uptimeMillis();
            pointers(down,MotionEvent.ACTION_DOWN,new float[][]{{117,370}},new int[]{0});
            pointers(down,MotionEvent.ACTION_POINTER_DOWN|(1<<MotionEvent.ACTION_POINTER_INDEX_SHIFT),new float[][]{{117,370},{850,420}},new int[]{0,1});
            SystemClock.sleep(350);
            ui(()->{check(activity.world.game.y<initial[1]-10,"joystick moves while second finger attacks");check(activity.world.game.attackCd>0||!activity.world.game.shots.isEmpty(),"held attack fires while moving");});
            pointers(down,MotionEvent.ACTION_POINTER_UP|(1<<MotionEvent.ACTION_POINTER_INDEX_SHIFT),new float[][]{{117,370},{850,420}},new int[]{0,1});
            pointers(down,MotionEvent.ACTION_UP,new float[][]{{117,370}},new int[]{0});
            tap(737,456);ui(()->check(activity.world.game.rollCd>0,"dodge button triggers"));
            tap(855,298); // May be ignored during dodge, as designed.
            SystemClock.sleep(330);tap(855,298);ui(()->check(activity.world.game.skillCd>0,"skill button triggers"));
            screenshot("02-dungeon.png");
            tap(900,37);ui(()->check(activity.world.game.mode==ActionGame.Mode.PAUSED,"pause button works"));
            float[] snapshot={0,0};ui(()->{snapshot[0]=activity.world.game.time;snapshot[1]=activity.world.game.hp;});SystemClock.sleep(350);
            ui(()->check(activity.world.game.time==snapshot[0]&&activity.world.game.hp==snapshot[1],"pause freezes combat"));
            screenshot("03-pause.png");
            ui(()->{try{SaveStore store=new SaveStore(activity);store.write(1,activity.world.game);ActionGame restored=store.read(1);check(restored.hp==activity.world.game.hp&&restored.room==1&&restored.hero==1,"atomic manual save roundtrip");activity.world.replace(restored);}catch(IOException e){throw new RuntimeException(e);}});
            tap(480,231);ui(()->check(activity.world.game.mode==ActionGame.Mode.PLAY,"resume restored snapshot"));
            // Produce a deterministic visible combat frame, using the actual renderer.
            ui(()->{ActionGame g=activity.world.game;g.pause();g.x=560;g.y=390;g.enemies.clear();g.shots.clear();g.blocks.clear();
                ActionGame.Enemy e=new ActionGame.Enemy(g.nextId++,0,620,370,75);e.state=1;e.timer=.5f;g.enemies.add(e);
                ActionGame.Enemy s=new ActionGame.Enemy(g.nextId++,1,400,260,80);s.state=1;s.aimX=.78f;s.aimY=.62f;s.timer=.8f;g.enemies.add(s);
                g.mode=ActionGame.Mode.PLAY;activity.world.stopFrames();});
            screenshot("04-combat-render.png");
            result.putString("stream","ARPG_SMOKE_OK: launch, landscape, character selection, simultaneous move/attack, dodge, skill, pause, atomic save and resume.\n");
            finish(Activity.RESULT_OK,result);
        }catch(Throwable t){result.putString("stream","ARPG_SMOKE_FAILED: "+t+"\n");finish(Activity.RESULT_CANCELED,result);}
    }
    private void tap(float x,float y){long t=SystemClock.uptimeMillis();pointers(t,MotionEvent.ACTION_DOWN,new float[][]{{x,y}},new int[]{0});pointers(t,MotionEvent.ACTION_UP,new float[][]{{x,y}},new int[]{0});}
    private void pointers(long down,int action,float[][] coords,int[] ids){ui(()->{
        MotionEvent.PointerProperties[] properties=new MotionEvent.PointerProperties[ids.length];MotionEvent.PointerCoords[] points=new MotionEvent.PointerCoords[ids.length];
        for(int i=0;i<ids.length;i++){properties[i]=new MotionEvent.PointerProperties();properties[i].id=ids[i];properties[i].toolType=MotionEvent.TOOL_TYPE_FINGER;
            points[i]=new MotionEvent.PointerCoords();points[i].x=activity.world.screenX(coords[i][0]);points[i].y=activity.world.screenY(coords[i][1]);points[i].pressure=1;points[i].size=1;}
        MotionEvent event=MotionEvent.obtain(down,SystemClock.uptimeMillis(),action,ids.length,properties,points,0,0,1,1,0,0,android.view.InputDevice.SOURCE_TOUCHSCREEN,0);
        activity.world.dispatchTouchEvent(event);event.recycle();
    });}
    private void screenshot(String name){ui(()->{
        try{WorldView v=activity.world;Bitmap b=Bitmap.createBitmap(v.getWidth(),v.getHeight(),Bitmap.Config.ARGB_8888);v.draw(new Canvas(b));File dir=new File(activity.getExternalFilesDir(null),"smoke");if(!dir.isDirectory()&&!dir.mkdirs())throw new IOException("screenshot directory");
            try(FileOutputStream out=new FileOutputStream(new File(dir,name))){b.compress(Bitmap.CompressFormat.PNG,100,out);}b.recycle();
        }catch(IOException e){throw new RuntimeException(e);}
    });}
}
