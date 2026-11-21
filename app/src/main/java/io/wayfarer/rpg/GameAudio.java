package io.wayfarer.rpg;
import android.content.Context;
import android.media.*;
import java.util.*;
/** Short original generated PCM effects; no microphone or network access. */
final class GameAudio {
    private final SoundPool pool;
    private final Map<String,Integer> sounds=new HashMap<>();
    private final Set<Integer> ready=new HashSet<>();
    private long lastHit=0;
    GameAudio(Context c){
        pool=new SoundPool.Builder().setMaxStreams(6).setAudioAttributes(new AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_GAME).setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build()).build();
        pool.setOnLoadCompleteListener((p,id,status)->{if(status==0)ready.add(id);});
        sounds.put("swing",pool.load(c,R.raw.swing,1));sounds.put("bow",pool.load(c,R.raw.bow,1));sounds.put("hit",pool.load(c,R.raw.hit,1));sounds.put("hurt",pool.load(c,R.raw.hurt,1));sounds.put("roll",pool.load(c,R.raw.roll,1));sounds.put("clear",pool.load(c,R.raw.clear,1));
    }
    void play(String event){
        long now=android.os.SystemClock.uptimeMillis();float rate=1f,volume=.48f;
        if(event.equals("hit")){if(now-lastHit<42)return;lastHit=now;rate=.88f+(now%5)*.045f;volume=.38f;}
        else if(event.equals("hurt")){rate=.82f;volume=.62f;}
        else if(event.equals("heal")||event.equals("revive")){event="clear";rate=1.42f;volume=.55f;}
        else if(event.equals("radiant")){event="clear";rate=1.48f;volume=.62f;}
        else if(event.equals("forbidden")||event.equals("rupture")){event="swing";rate=.68f;volume=.62f;}
        else if(event.equals("lance")||event.equals("thunder")){event="clear";rate=1.18f;volume=.7f;}
        else if(event.equals("slam")){event="hurt";rate=.72f;volume=.68f;}
        else if(event.equals("shield")||event.equals("enemyWard")||event.equals("enemyShield")){event="clear";rate=.92f;volume=.42f;}
        else if(event.equals("roll")||event.equals("daggerDash")){event="roll";rate=1.25f;volume=.52f;}
        else if(event.equals("skill")||event.equals("activeSkill")||event.equals("careerSkill")||event.equals("followup")){event="swing";rate=event.equals("followup")?1.45f:1.12f;volume=.58f;}
        else if(event.equals("enemySwing")||event.equals("hazard")){event="hurt";rate=.92f;volume=.3f;}
        else if(event.equals("clear")||event.equals("victory")||event.equals("defeat")||event.equals("relic")||event.equals("coin")||event.equals("potion")){rate=event.equals("coin")?1.65f:event.equals("potion")?1.3f:1f;event="clear";volume=.52f;}
        else if(event.equals("swing"))rate=.95f;
        Integer id=sounds.get(event);if(id!=null&&ready.contains(id))pool.play(id,volume,volume,1,0,rate);
    }
    void close(){pool.release();ready.clear();}
}
