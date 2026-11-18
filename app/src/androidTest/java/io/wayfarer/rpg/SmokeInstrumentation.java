package io.wayfarer.rpg;

import android.app.*;
import android.content.Intent;
import android.graphics.*;
import android.os.*;
import android.view.*;
import android.widget.*;
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
            endlessFlow();
            ui(()->activity.world.replace(new ActionGame()));
            challengeFlow();
            ui(()->{activity.world.replace(new ActionGame());activity.world.startFrames();});
            pactFlow();
            ui(()->activity.world.replace(new ActionGame()));
            ordinaryCareerFlow();
            ui(()->activity.world.replace(new ActionGame()));
            careerFlow();
            ui(()->activity.world.replace(new ActionGame()));
            relicFlow();
            ui(()->activity.world.replace(new ActionGame()));
            soulFlow();
            ui(()->activity.world.replace(new ActionGame()));
            workshopFlow();
            ui(()->activity.world.replace(new ActionGame()));
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
            result.putString("stream","ARPG_SMOKE_OK: independent three-final purchases and same-category locks; actual endless entry, fifth-boss reward and sixth-floor exit; full bag potion pickup after drinking; v10 AtomicFile floor, threat and build; main and personal story choices, once-only rewards, personal passive and AtomicFile; real two-touch combo and blocked third touch; legal-cast 100 erosion; real burner zones and bell shields with snapshot; launch, landscape, character selection, simultaneous move/attack, dodge, skill, pause, atomic save and resume; custom editor save/cancel, cross-class skill, talent purchase/locks and profile persistence; soul binding/cancel, independent soul button, radiant domain, forbidden pact, corruption costs and soul snapshot persistence; relic cabinet equip/cancel, duplicate guard, character isolation, checkpoint discovery and relic manual save; career level gate, save/cancel, specialization persistence, class-independent weapon, background/skill shields, dodge charge and atomic snapshot; 1887 setting dossier; all eight ordinary career selections, doctor treatment and potion, merchant actual upgrade price, miner protection and v6 manual save; optional pact cancel/confirm, job-change opt-out, separate pact/soul touch buttons, healing lock, frozen penalty, AtomicFile persistence and local-only merchant payment; all eight rewritten arts mark real test victims, donor damage and healing, pulse snapshot and machine/burial rendering.\n");
            finish(Activity.RESULT_OK,result);
        }catch(Throwable t){result.putString("stream","ARPG_SMOKE_FAILED: "+t+"\n");finish(Activity.RESULT_CANCELED,result);}
    }
    private void buyNode(int id){
        int tier=id%4;tap(50+(id/4)*305+(tier==3?190:tier<2?125:60),tier==0?165:tier==1?268:380);waitForIdleSync();
        ui(()->{check(activity.talentDialog.getButton(AlertDialog.BUTTON_POSITIVE).isEnabled(),"node purchasable "+id);activity.talentDialog.getButton(AlertDialog.BUTTON_POSITIVE).performClick();});waitForIdleSync();
        ui(()->check(activity.world.game.rank(id)>0,"node purchased "+id));
    }
    private void endlessFlow(){
        ui(()->{activity.world.stopFrames();activity.world.game.experience[0]=1280;activity.world.game.hp=activity.world.game.maxHp();});
        tap(540,500);for(int b=0;b<3;b++){buyNode(b*4);buyNode(b*4+1);buyNode(b*4+1);buyNode(b*4+(b==2?3:2));}
        ui(()->{ActionGame g=activity.world.game;check(g.rank(2)==1&&g.rank(6)==1&&g.rank(11)==1&&g.availablePoints()==6,"all three finals coexist");});
        tap(240,380);waitForIdleSync();ui(()->{check(!activity.talentDialog.getButton(AlertDialog.BUTTON_POSITIVE).isEnabled(),"same-category alternative locked");activity.talentDialog.getButton(AlertDialog.BUTTON_NEGATIVE).performClick();});waitForIdleSync();
        screenshot("27-three-finals.png");tap(900,38);tap(790,461);
        ui(()->{ActionGame g=activity.world.game;check(g.endless&&g.endlessDepth==1&&g.threat>1,"actual camp endless button starts dynamic run");
            // Controlled clear fixture follows the real boss/reward/exit path; it is not a player balance run.
            for(int floor=1;floor<=5;floor++){for(ActionGame.Enemy e:g.enemies)e.hp=0;g.hitStop=0;g.update(.016f,new ActionGame.Input());check(g.mode==ActionGame.Mode.REWARD,"endless boss also offers reward");g.chooseReward(0);g.x=300;g.y=70;g.hitStop=0;g.update(.016f,new ActionGame.Input());}
            check(g.endlessDepth==6&&g.room==1&&g.bestDepth==5,"fifth boss leads to sixth floor");g.x=560;g.y=430;for(ActionGame.Enemy e:g.enemies)e.stun=1;});
        screenshot("28-endless-sixth-floor.png");
        ui(()->{ActionGame g=activity.world.game;g.blocks.clear();g.enemies.clear();g.shots.clear();g.drops.clear();g.hp=g.maxHp()-50;g.potions=5;ActionGame.Enemy e=new ActionGame.Enemy(g.nextId++,0,800,250,500);e.stun=1;g.enemies.add(e);
            // Pickup fixture isolates bag capacity; seeded death probability is tested in EndlessTests.
            ActionGame.Drop bottle=new ActionGame.Drop(g.x+12,g.y+12,1,false);bottle.potion=true;g.drops.add(bottle);g.hitStop=0;g.update(.016f,new ActionGame.Input());check(g.potions==5&&g.drops.size()==1,"full bag bottle stays visible");});
        screenshot("29-potion-pickup-fixture.png");tap(648,456);
        ui(()->{ActionGame g=activity.world.game;check(g.potions==4,"actual potion button drinks");g.hitStop=0;g.update(.016f,new ActionGame.Input());check(g.potions==5&&g.drops.isEmpty(),"open slot collects bottle once");g.pause();try{SaveStore store=new SaveStore(activity);store.write(3,g);ActionGame a=store.read(3);check(a.endless&&a.endlessDepth==6&&a.bestDepth==5&&a.potions==5&&a.threat==g.threat&&a.rank(2)==1&&a.rank(6)==1&&a.rank(11)==1,"v10 AtomicFile restores complete endless build");}catch(IOException e){throw new RuntimeException(e);}});
    }
    private void journalItem(int index){ui(()->{ListView list=activity.storyDialog.getListView();list.performItemClick(null,index,index);});waitForIdleSync();}
    private void storyButton(int which){ui(()->activity.storyDialog.getButton(which).performClick());waitForIdleSync();}
    private void challengeFlow(){
        ui(()->{activity.world.stopFrames();ActionGame g=activity.world.game;g.chooseCareer(CareerSystem.DOCTOR,0,0);});
        tap(835,105);waitForIdleSync();journalItem(0);
        ui(()->{check(((TextView)activity.storyDialog.getWindow().getDecorView().findViewWithTag("story-text")).getText().toString().contains("诺拉"),"journal intro opens from real camp entry");});storyButton(AlertDialog.BUTTON_NEGATIVE);
        tap(785,405);waitForIdleSync();ui(()->check(activity.world.game.mode==ActionGame.Mode.PLAY,"story flow starts expedition: "+activity.world.game.mode));
        // Deterministic clear fixture exercises the actual clear event, choice UI and durable award.
        ui(()->{ActionGame g=activity.world.game;for(ActionGame.Enemy e:g.enemies)e.hp=0;g.update(.05f,new ActionGame.Input());check(g.mode==ActionGame.Mode.REWARD&&g.storyUnlocked==1,"clear reveals main scene: "+g.mode+" flags="+g.storyUnlocked);});
        tap(480,412);waitForIdleSync();screenshotNativeDialog("23-main-story-fixture.png",activity.storyDialog);
        int[] bank={0};ui(()->bank[0]=activity.world.game.bank);storyButton(AlertDialog.BUTTON_POSITIVE);ui(()->check(activity.world.game.storyChoices[0]==1&&activity.world.game.bank==bank[0]+12,"main choice commits actual once-only reward"));
        tap(480,412);waitForIdleSync();journalItem(1);
        ui(()->check(activity.world.game.bank==bank[0]+12,"rereading cannot duplicate main reward"));storyButton(AlertDialog.BUTTON_NEGATIVE);ui(()->{ActionGame g=activity.world.game;g.chooseReward(0);g.pause();g.abandon();g.returnCamp();});
        tap(835,105);waitForIdleSync();journalItem(6);journalItem(0);screenshotNativeDialog("24-doctor-story-fixture.png",activity.storyDialog);
        float[] personalHp={0};ui(()->personalHp[0]=activity.world.game.maxHp());storyButton(AlertDialog.BUTTON_POSITIVE);
        ui(()->{ActionGame g=activity.world.game;check(g.maxHp()==personalHp[0]+4,"personal decision grants actual persistent passive");try{SaveStore store=new SaveStore(activity);store.write(2,g);ActionGame loaded=store.read(2);check(loaded.storyChoices[0]==1&&loaded.characterJobs[0]==CareerSystem.DOCTOR&&loaded.characterChoices[0][0]==1&&loaded.maxHp()==g.maxHp(),"story and derived health survive AtomicFile");}catch(IOException e){throw new RuntimeException(e);}activity.world.replace(new ActionGame());});
        tap(785,405);ui(()->{ActionGame g=activity.world.game;pactArena(g);g.enemies.get(0).y=g.y-140;});tap(855,298);
        float[] target={0};ui(()->{check(activity.world.game.followUpWindow>0,"real skill button opens combo");target[0]=activity.world.game.enemies.get(0).hp;});tap(855,298);
        ui(()->{check(activity.world.game.followUpWindow==0&&activity.world.game.enemies.get(0).hp<target[0],"second touch performs damaging follow-up");target[0]=activity.world.game.enemies.get(0).hp;});tap(855,298);ui(()->check(activity.world.game.enemies.get(0).hp==target[0],"third touch cannot repeat combo"));
        ui(()->{ActionGame g=new ActionGame();g.bindSoul(2,0);g.start(91);g.blocks.clear();g.enemies.clear();g.x=560;g.y=430;ActionGame.Enemy e=new ActionGame.Enemy(g.nextId++,0,100,100,1000);g.enemies.add(e);
            // No injected erosion: real casts and natural cooldown updates, with a frozen distant actor.
            for(int cast=0;cast<4;cast++){g.skill();g.skill();if(cast<3)for(int t=0;t<391;t++){e.stun=1;g.hitStop=0;g.update(1f/60,new ActionGame.Input());g.signals.clear();}}
            check(g.corruption==100&&g.ruptureTime>0,"legal cast sequence reaches actual 100 erosion");activity.world.replace(g);g.resume();});
        screenshot("25-erosion-100-fixture.png");
        ui(()->{ActionGame g=activity.world.game;g.enemies.clear();g.shots.clear();g.signals.clear();g.hitStop=0;g.room=4;
            ActionGame.Enemy hunter=new ActionGame.Enemy(g.nextId++,4,710,320,130);hunter.state=1;hunter.timer=.5f;hunter.aimX=-.8f;hunter.aimY=.6f;hunter.targetX=g.x;hunter.targetY=g.y;g.enemies.add(hunter);
            ActionGame.Enemy fire=new ActionGame.Enemy(g.nextId++,5,440,280,120);fire.state=1;fire.timer=.001f;fire.targetX=g.x;fire.targetY=g.y;fire.aimX=.6f;fire.aimY=.8f;g.enemies.add(fire);
            ActionGame.Enemy bell=new ActionGame.Enemy(g.nextId++,6,580,250,140);bell.state=1;bell.timer=.001f;bell.aimX=0;bell.aimY=1;g.enemies.add(bell);g.update(1f/60,new ActionGame.Input());check(g.hazards.size()==2&&hunter.guard==24,"new enemy AI produces actual ground warnings and ally shields");try{ActionGame loaded=ActionSave.decode(ActionSave.encode(g));check(loaded.hazards.size()==2&&loaded.enemies.get(0).guard==24,"new enemy runtime restores");}catch(IOException e){throw new RuntimeException(e);}});
        screenshot("26-mixed-enemies-fixture.png");
    }
    private void pactFlow(){
        ui(()->{activity.world.game.experience[0]=160;activity.world.game.hp=activity.world.game.maxHp();});
        pickCareer(CareerSystem.DOCTOR,1,4);tap(510,142);waitForIdleSync();
        ui(()->{View root=activity.careerDialog.getWindow().getDecorView();CheckBox pact=root.findViewWithTag("career-pact");check(!pact.isChecked()&&pact.isEnabled(),"occupational forbidden art defaults off");check(((TextView)root.findViewWithTag("career-pact-details")).getText().toString().contains("供体"),"pact explains the transgressive act itself");check(((TextView)root.findViewWithTag("career-temperament")).getText().toString().contains("同情心"),"doctor has distinctive temperament");pact.setChecked(true);activity.careerDialog.getButton(AlertDialog.BUTTON_NEGATIVE).performClick();});waitForIdleSync();
        ui(()->check(!activity.world.game.pactEnabled(),"cancel never accepts forbidden cost"));
        tap(510,142);waitForIdleSync();ui(()->((CheckBox)activity.careerDialog.getWindow().getDecorView().findViewWithTag("career-pact")).setChecked(true));waitForIdleSync();
        screenshotNativeDialog("19-doctor-pact-choice.png",activity.careerDialog);
        ui(()->activity.careerDialog.getButton(AlertDialog.BUTTON_POSITIVE).performClick());waitForIdleSync();ui(()->check(activity.world.game.pactEnabled(),"explicit confirmation enables occupational art"));
        tap(510,142);waitForIdleSync();ui(()->((Spinner)activity.careerDialog.getWindow().getDecorView().findViewWithTag("career-job")).setSelection(CareerSystem.SOLDIER));waitForIdleSync();
        ui(()->{check(!((CheckBox)activity.careerDialog.getWindow().getDecorView().findViewWithTag("career-pact")).isChecked(),"changing job resets forbidden consent draft");activity.careerDialog.getButton(AlertDialog.BUTTON_NEGATIVE).performClick();});waitForIdleSync();
        ui(()->{check(activity.world.game.job()==CareerSystem.DOCTOR&&activity.world.game.pactEnabled(),"cancel preserves doctor choice");activity.world.game.bindSoul(1,0);});
        tap(785,405);float[] expected={0};ui(()->{ActionGame g=activity.world.game;pactArena(g);g.hp=10;expected[0]=10+g.damage()*2.5f*.8f*g.healingFactor();});tap(648,332);
        ui(()->check(activity.world.game.pactTime>0&&Math.abs(activity.world.game.hp-expected[0])<.05f,"dedicated forbidden button steals life from actual donor"));tap(648,456);
        ui(()->check(activity.world.game.potions==2&&activity.world.game.healingFactor()==0,"forbidden treatment blocks potion consumption and healing"));tap(735,332);
        ui(()->check(activity.world.game.soulCd>0&&activity.world.game.pactCd>0,"soul and occupational buttons remain independent"));
        screenshot("20-doctor-pact-combat.png");tap(900,37);float[] paused={0,0};ui(()->{paused[0]=activity.world.game.pactTime;paused[1]=activity.world.game.pactCd;});SystemClock.sleep(180);
        ui(()->{ActionGame g=activity.world.game;check(g.pactTime==paused[0]&&g.pactCd==paused[1],"paused forbidden cost freezes");try{SaveStore store=new SaveStore(activity);store.write(3,g);ActionGame a=store.read(3);check(a.pactEnabled()&&a.pactTime==g.pactTime&&a.hp==g.hp&&a.healingFactor()==0&&a.enemies.get(0).pactMark==g.enemies.get(0).pactMark&&a.enemies.get(0).pactTick==g.enemies.get(0).pactTick,"pact and donor pulse survive AtomicFile without replay");}catch(IOException e){throw new RuntimeException(e);}ActionGame merchant=new ActionGame();merchant.experience[0]=160;merchant.chooseCareer(CareerSystem.MERCHANT,2,0,true);merchant.bank=500;activity.world.replace(merchant);});
        tap(785,405);ui(()->activity.world.game.loot=29);tap(648,332);ui(()->check(activity.world.game.pactCd==0&&activity.world.game.bank==500,"merchant cannot spend camp bank"));ui(()->{pactArena(activity.world.game);activity.world.game.loot=30;});tap(648,332);
        ui(()->check(activity.world.game.pactTime>0&&activity.world.game.loot==0&&activity.world.game.bank==500,"merchant pays only run tickets"));
        for(int j=CareerSystem.POLICE;j<=CareerSystem.REPORTER;j++){
            final int job=j;ui(()->{ActionGame g=new ActionGame();g.experience[0]=160;g.chooseCareer(job,1,0,true);activity.world.replace(g);});tap(785,405);
            ui(()->{ActionGame g=activity.world.game;pactArena(g);g.loot=30;g.hp=g.maxHp()*.6f;});tap(648,332);
            ui(()->check(activity.world.game.pactCd>0&&activity.world.game.enemies.get(0).pactMark>0,"each profession touch button marks an actual victim"));
            if(job==CareerSystem.MECHANIC)screenshot("21-living-engine-fixture.png");
            if(job==CareerSystem.MINER)screenshot("22-burial-fixture.png");
        }
    }
    private void pactArena(ActionGame g){
        // Deterministic test actors, not a claim of natural leveling or an actual campaign encounter.
        g.enemies.clear();g.blocks.clear();g.shots.clear();g.x=560;g.y=430;g.time=9;
        ActionGame.Enemy e=new ActionGame.Enemy(g.nextId++,0,560,330,1000);e.stun=1;g.enemies.add(e);
    }
    private void pickCareer(int job,int spec,int background){
        tap(510,142);waitForIdleSync();
        ui(()->{Spinner jobs=activity.careerDialog.getWindow().getDecorView().findViewWithTag("career-job");check(jobs.getCount()==12,"all eleven careers present");jobs.setSelection(job);});waitForIdleSync();
        ui(()->{View root=activity.careerDialog.getWindow().getDecorView();((Spinner)root.findViewWithTag("career-spec")).setSelection(spec);((Spinner)root.findViewWithTag("career-background")).setSelection(background);});waitForIdleSync();
        ui(()->activity.careerDialog.getButton(AlertDialog.BUTTON_POSITIVE).performClick());waitForIdleSync();
        ui(()->check(activity.world.game.isSpec(job,spec)&&activity.world.game.background()==background,"ordinary career commits without soul: "+job));
    }
    private void ordinaryCareerFlow(){
        ui(()->{activity.world.game.experience[0]=160;activity.world.game.hp=activity.world.game.maxHp();});
        for(int job=CareerSystem.POLICE;job<=CareerSystem.REPORTER;job++)pickCareer(job,2,0);
        pickCareer(CareerSystem.DOCTOR,1,4);
        tap(510,142);waitForIdleSync();screenshotNativeDialog("18-ordinary-doctor.png",activity.careerDialog);
        ui(()->activity.careerDialog.getButton(AlertDialog.BUTTON_POSITIVE).performClick());waitForIdleSync();
        tap(785,405);float[] expected={0};ui(()->{ActionGame g=activity.world.game;g.hp=20;expected[0]=20+g.maxHp()*.04f;});tap(855,298);
        ui(()->check(Math.abs(activity.world.game.hp-expected[0])<.05f,"ordinary first aid actually heals without soul"));
        ui(()->{ActionGame g=activity.world.game;expected[0]+=g.maxHp()*.45f+8+6;});tap(648,456);tap(900,37);
        ui(()->{ActionGame g=activity.world.game;check(Math.abs(g.hp-expected[0])<.05f&&g.potions==1,"doctor and medic background actual potion bonus");try{SaveStore store=new SaveStore(activity);store.write(3,g);ActionGame loaded=store.read(3);check(loaded.isSpec(CareerSystem.DOCTOR,1)&&loaded.background()==4&&loaded.hp==g.hp&&loaded.potions==1,"ordinary career v6 AtomicFile snapshot");loaded.abandon();loaded.returnCamp();activity.world.replace(loaded);}catch(IOException e){throw new RuntimeException(e);}});
        pickCareer(CareerSystem.MERCHANT,2,0);ui(()->activity.world.game.bank=100);tap(180,393);
        ui(()->check(activity.world.game.bank==74&&activity.world.game.power==1,"merchant UI charges discounted 26 tickets"));
        pickCareer(CareerSystem.MINER,2,1);tap(785,405);
        ui(()->check(activity.world.game.careerWard==12,"miner protective equipment combines with background using max"));
    }
    private void careerFlow(){
        tap(350,108);waitForIdleSync();
        ui(()->{check(activity.settingDialog!=null&&activity.settingDialog.isShowing(),"1887 dossier opens from camp");check(WorldSetting.DISTRICTS.length==5&&WorldSetting.DOSSIER.contains("1887"),"setting content present");});
        screenshotNativeDialog("17-greybridge-dossier.png",activity.settingDialog);
        ui(()->activity.settingDialog.getButton(AlertDialog.BUTTON_POSITIVE).performClick());waitForIdleSync();
        tap(510,142);waitForIdleSync();
        ui(()->{View root=activity.careerDialog.getWindow().getDecorView();((Spinner)root.findViewWithTag("career-job")).setSelection(1);((Spinner)root.findViewWithTag("career-background")).setSelection(1);});waitForIdleSync();
        ui(()->{check(!activity.careerDialog.getWindow().getDecorView().findViewWithTag("career-spec").isEnabled(),"level-one specialization locked");activity.careerDialog.getButton(AlertDialog.BUTTON_NEGATIVE).performClick();});waitForIdleSync();
        // A level-three test fixture exercises unlocks without claiming natural progression.
        ui(()->{check(activity.world.game.job()==0&&activity.world.game.background()==0,"cancel leaves neutral career");activity.world.game.experience[0]=160;activity.world.game.hp=activity.world.game.maxHp();});
        tap(510,142);waitForIdleSync();ui(()->((Spinner)activity.careerDialog.getWindow().getDecorView().findViewWithTag("career-job")).setSelection(1));waitForIdleSync();
        ui(()->{View root=activity.careerDialog.getWindow().getDecorView();check(root.findViewWithTag("career-spec").isEnabled(),"level-three specialization unlocked");((Spinner)root.findViewWithTag("career-spec")).setSelection(1);((Spinner)root.findViewWithTag("career-background")).setSelection(1);});waitForIdleSync();
        screenshotNativeDialog("15-career-hall.png",activity.careerDialog);
        ui(()->activity.careerDialog.getButton(AlertDialog.BUTTON_POSITIVE).performClick());waitForIdleSync();
        ui(()->check(activity.world.game.isSpec(1,1)&&activity.world.game.background()==1,"career confirmed"));
        tap(510,142);waitForIdleSync();ui(()->{check(((Spinner)activity.careerDialog.getWindow().getDecorView().findViewWithTag("career-spec")).getSelectedItemPosition()==1,"reopening preserves specialization");((Spinner)activity.careerDialog.getWindow().getDecorView().findViewWithTag("career-job")).setSelection(3);});waitForIdleSync();
        ui(()->{check(((Spinner)activity.careerDialog.getWindow().getDecorView().findViewWithTag("career-spec")).getSelectedItemPosition()==0,"changing class resets draft specialization");activity.careerDialog.getButton(AlertDialog.BUTTON_NEGATIVE).performClick();});waitForIdleSync();
        ui(()->check(activity.world.game.isSpec(1,1),"cancel class change preserves committed spec"));
        tap(440,230);ui(()->check(activity.world.game.job()==0,"other preset has independent career"));tap(140,230);
        tap(785,405);ui(()->check(activity.world.game.careerWard==10,"smith background grants actual entry shield"));tap(855,298);
        ui(()->check(activity.world.game.careerWard==14,"bulwark active grants actual shield"));screenshot("16-career-combat.png");tap(900,37);
        ui(()->{try{SaveStore store=new SaveStore(activity);store.write(1,activity.world.game);ActionGame loaded=store.read(1);check(loaded.isSpec(1,1)&&loaded.background()==1&&loaded.careerWard==14,"career and shield survive AtomicFile");loaded.abandon();loaded.returnCamp();loaded.selectCustom(0);loaded.profile().xp=160;activity.world.replace(loaded);}catch(IOException e){throw new RuntimeException(e);}});
        tap(510,142);waitForIdleSync();ui(()->((Spinner)activity.careerDialog.getWindow().getDecorView().findViewWithTag("career-job")).setSelection(2));waitForIdleSync();
        ui(()->{View root=activity.careerDialog.getWindow().getDecorView();((Spinner)root.findViewWithTag("career-spec")).setSelection(2);((Spinner)root.findViewWithTag("career-background")).setSelection(3);});waitForIdleSync();
        ui(()->activity.careerDialog.getButton(AlertDialog.BUTTON_POSITIVE).performClick());waitForIdleSync();
        tap(510,142);waitForIdleSync();ui(()->{check(((Spinner)activity.careerDialog.getWindow().getDecorView().findViewWithTag("career-spec")).getSelectedItemPosition()==2,"second specialization survives reopen");activity.careerDialog.getButton(AlertDialog.BUTTON_POSITIVE).performClick();});waitForIdleSync();
        ui(()->check(activity.world.game.isSpec(2,2)&&activity.world.game.hero==0,"career does not force a different weapon"));
        tap(785,405);tap(737,456);tap(900,37);
        ui(()->{check(activity.world.game.careerCharge>0,"skirmish dodge actually primes hit");try{ActionGame loaded=ActionSave.decode(ActionSave.encode(activity.world.game));check(loaded.isSpec(2,2)&&loaded.background()==3&&loaded.careerCharge==activity.world.game.careerCharge,"second spec and remaining proc persist");}catch(IOException e){throw new RuntimeException(e);}});
    }
    private void screenshotNativeDialog(String name,AlertDialog dialog){ui(()->{try{View v=dialog.getWindow().getDecorView();Bitmap b=Bitmap.createBitmap(v.getWidth(),v.getHeight(),Bitmap.Config.ARGB_8888);v.draw(new Canvas(b));writeBitmap(b,name);}catch(IOException e){throw new RuntimeException(e);}});}
    private void relicFlow(){
        tap(815,142);waitForIdleSync();
        ui(()->{check(activity.relicDialog.isShowing(),"relic cabinet opens from camp");View root=activity.relicDialog.getWindow().getDecorView();((Spinner)root.findViewWithTag("relic-slot-0")).setSelection(1);((Spinner)root.findViewWithTag("relic-slot-1")).setSelection(1);});waitForIdleSync();
        ui(()->{check(!activity.relicDialog.getButton(AlertDialog.BUTTON_POSITIVE).isEnabled(),"duplicate equip blocked in native editor");((Spinner)activity.relicDialog.getWindow().getDecorView().findViewWithTag("relic-slot-1")).setSelection(2);});waitForIdleSync();
        screenshotRelicDialog("13-relic-cabinet.png");
        ui(()->{check(activity.relicDialog.getButton(AlertDialog.BUTTON_POSITIVE).isEnabled(),"distinct pair enabled");activity.relicDialog.getButton(AlertDialog.BUTTON_POSITIVE).performClick();});waitForIdleSync();
        ui(()->check(activity.world.game.hasRelic(1)&&activity.world.game.hasRelic(2)&&activity.world.game.maxHp()==122,"starter relics really affect stats: "+activity.world.game.relicLoadout()+" hp="+activity.world.game.maxHp()));
        tap(815,142);waitForIdleSync();ui(()->activity.relicDialog.getButton(AlertDialog.BUTTON_NEUTRAL).performClick());waitForIdleSync();ui(()->activity.relicDialog.getButton(AlertDialog.BUTTON_NEGATIVE).performClick());ui(()->check(activity.world.game.hasRelic(1)&&activity.world.game.hasRelic(2),"cancel clearing keeps actual loadout"));
        tap(440,230);ui(()->check(!activity.world.game.hasRelic(1),"other character has independent relic loadout"));tap(140,230);
        tap(785,405);tap(737,456);ui(()->check(activity.world.game.rollCd>0&&activity.world.game.rollCd<=1.001f,"equipped compass changes dodge cooldown"));
        // Accelerate to a clearable second-room fixture, then exercise the actual reward UI.
        ui(()->{ActionGame g=activity.world.game;g.room=2;g.generateRoom();g.enemies.clear();g.update(1f/60f,new ActionGame.Input());check(g.mode==ActionGame.Mode.REWARD&&g.lastRelic>=3,"second room reveals a new relic");});
        screenshot("14-relic-discovery-fixture.png");int[] found={0,0};ui(()->{found[0]=activity.world.game.lastRelic;found[1]=activity.world.game.relicOwned;});
        tap(180,290);tap(900,37);
        ui(()->{try{SaveStore store=new SaveStore(activity);store.write(2,activity.world.game);ActionGame loaded=store.read(2);check(loaded.relicOwned==found[1]&&loaded.hasRelic(1)&&loaded.lastRelic==found[0],"relic discovery and loadout survive atomic manual save");activity.world.replace(loaded);loaded.abandon();loaded.returnCamp();check(loaded.relicOwned==found[1],"discovery survives failed expedition");}catch(IOException e){throw new RuntimeException(e);}});
        tap(815,142);waitForIdleSync();ui(()->((Spinner)activity.relicDialog.getWindow().getDecorView().findViewWithTag("relic-slot-0")).setSelection(3));waitForIdleSync();ui(()->activity.relicDialog.getButton(AlertDialog.BUTTON_POSITIVE).performClick());
        ui(()->check(activity.world.game.hasRelic(found[0]),"newly found relic can be equipped in camp"));
    }
    private void screenshotRelicDialog(String name){ui(()->{try{View v=activity.relicDialog.getWindow().getDecorView();Bitmap b=Bitmap.createBitmap(v.getWidth(),v.getHeight(),Bitmap.Config.ARGB_8888);v.draw(new Canvas(b));writeBitmap(b,name);}catch(IOException e){throw new RuntimeException(e);}});}
    private void soulFlow(){
        tap(780,500);waitForIdleSync();
        ui(()->{check(activity.soulDialog.isShowing(),"soul altar opens");((Spinner)activity.soulDialog.getWindow().getDecorView().findViewWithTag("soul-kind")).setSelection(1);});waitForIdleSync();
        screenshotSoulDialog("09-radiant-altar.png");
        ui(()->activity.soulDialog.getButton(AlertDialog.BUTTON_POSITIVE).performClick());
        ui(()->check(activity.world.game.soulKind()==1&&activity.world.game.soulArt()==0,"radiant binding committed"));
        tap(780,500);waitForIdleSync();ui(()->((Spinner)activity.soulDialog.getWindow().getDecorView().findViewWithTag("soul-kind")).setSelection(2));waitForIdleSync();ui(()->activity.soulDialog.getButton(AlertDialog.BUTTON_NEGATIVE).performClick());
        ui(()->check(activity.world.game.soulKind()==1,"cancel soul draft preserves binding"));
        tap(785,405);tap(735,332);ui(()->check(activity.world.game.soulCd>0&&activity.world.game.soulFields.size()==1&&activity.world.game.soulWard==30,"dedicated soul button casts domain"));
        SystemClock.sleep(150);screenshot("10-radiant-domain.png");tap(900,37);
        float[] paused={0,0};ui(()->{paused[0]=activity.world.game.soulFields.get(0).life;paused[1]=activity.world.game.soulCd;});SystemClock.sleep(180);
        ui(()->{check(activity.world.game.soulFields.get(0).life==paused[0]&&activity.world.game.soulCd==paused[1],"pause freezes domain and exclusive cooldown");
            try{SaveStore store=new SaveStore(activity);store.write(3,activity.world.game);ActionGame loaded=store.read(3);check(loaded.soulKind()==1&&loaded.soulFields.size()==1&&loaded.soulEnergy==activity.world.game.soulEnergy,"soul snapshot restored");}catch(IOException e){throw new RuntimeException(e);}
            ActionGame g=new ActionGame();g.selectCustom(1);activity.world.replace(g);
        });
        tap(350,500);waitForIdleSync();ui(()->((Spinner)activity.workshopDialog.getWindow().getDecorView().findViewWithTag("profile-soul")).setSelection(2));waitForIdleSync();
        ui(()->((Spinner)activity.workshopDialog.getWindow().getDecorView().findViewWithTag("profile-soul-art")).setSelection(1));waitForIdleSync();
        ui(()->activity.workshopDialog.getButton(AlertDialog.BUTTON_POSITIVE).performClick());
        ui(()->check(activity.world.game.soulKind()==2&&activity.world.game.soulArt()==1,"workshop selects forbidden pact"));
        tap(780,500);waitForIdleSync();screenshotSoulDialog("11-forbidden-altar.png");ui(()->activity.soulDialog.getButton(AlertDialog.BUTTON_POSITIVE).performClick());
        ui(()->check(activity.world.game.soulArt()==1,"reopening altar preserves selected second art"));
        tap(785,405);float[] hp={0};ui(()->hp[0]=activity.world.game.hp);tap(735,332);
        ui(()->check(activity.world.game.soulBuff>0&&activity.world.game.corruption==30&&activity.world.game.hp<hp[0],"forbidden pact has actual cost and buff"));
        tap(855,298);SystemClock.sleep(100);screenshot("12-forbidden-pact.png");
        ui(()->{check(activity.world.game.corruption>=47&&activity.world.game.skillCd>0,"mutated active increases corruption");try{ActionGame g=ActionSave.decode(ActionSave.encode(activity.world.game));check(g.soulBuff>0&&g.soulArt()==1,"forbidden runtime persists");}catch(IOException e){throw new RuntimeException(e);}});
    }
    private void screenshotSoulDialog(String name){ui(()->{try{View v=activity.soulDialog.getWindow().getDecorView();Bitmap b=Bitmap.createBitmap(v.getWidth(),v.getHeight(),Bitmap.Config.ARGB_8888);v.draw(new Canvas(b));writeBitmap(b,name);}catch(IOException e){throw new RuntimeException(e);}});}
    private void workshopFlow(){
        tap(250,142);tap(140,230);ui(()->check(activity.world.game.custom==0,"custom roster selects independent slot"));
        tap(350,500);waitForIdleSync();
        ui(()->{AlertDialog d=activity.workshopDialog;check(d!=null&&d.isShowing(),"editor opens from camp");View root=d.getWindow().getDecorView();
            ((EditText)root.findViewWithTag("profile-name")).setText("暮灯艾文");
            ((Spinner)root.findViewWithTag("profile-weapon")).setSelection(1);
            ((Spinner)root.findViewWithTag("profile-skill")).setSelection(0);
            ((Spinner)root.findViewWithTag("profile-color")).setSelection(4);
            ((Spinner)root.findViewWithTag("profile-hair")).setSelection(2);
        });waitForIdleSync();SystemClock.sleep(150);
        screenshotDialog("05-character-workshop.png");
        ui(()->activity.workshopDialog.getButton(AlertDialog.BUTTON_POSITIVE).performClick());waitForIdleSync();
        ui(()->{ActionGame g=activity.world.game;check(g.heroName().equals("暮灯艾文")&&g.hero==1&&g.skillStyle()==0&&g.profile().color==4&&g.profile().hair==2,"native editor commits customization");});
        tap(350,500);waitForIdleSync();ui(()->{View root=activity.workshopDialog.getWindow().getDecorView();((EditText)root.findViewWithTag("profile-name")).setText("取消的修改");activity.workshopDialog.getButton(AlertDialog.BUTTON_NEGATIVE).performClick();});
        ui(()->check(activity.world.game.heroName().equals("暮灯艾文"),"cancel preserves profile"));
        tap(530,500);ui(()->check(activity.world.showTalents,"tree opens"));tap(175,164);
        ui(()->{check(activity.talentDialog.getButton(AlertDialog.BUTTON_POSITIVE).isEnabled(),"root learn enabled");activity.talentDialog.getButton(AlertDialog.BUTTON_POSITIVE).performClick();});
        ui(()->check(activity.world.game.rank(0)==1&&activity.world.game.availablePoints()==1,"learn spends exactly one point"));
        tap(110,380);ui(()->{check(!activity.talentDialog.getButton(AlertDialog.BUTTON_POSITIVE).isEnabled(),"locked capstone cannot be learned");activity.talentDialog.getButton(AlertDialog.BUTTON_NEGATIVE).performClick();});
        screenshot("06-talent-tree.png");tap(900,38);screenshot("07-custom-camp.png");
        ui(()->{try{SaveStore store=new SaveStore(activity);store.write(2,activity.world.game);ActionGame loaded=store.read(2);check(loaded.custom==0&&loaded.hero==1&&loaded.skillStyle()==0&&loaded.rank(0)==1&&loaded.heroName().equals("暮灯艾文"),"custom manual snapshot roundtrip");activity.world.replace(loaded);check(new WorldView(activity,loaded,activity).customRoster,"restored custom profile opens custom roster");}catch(IOException e){throw new RuntimeException(e);}});
        tap(785,405);tap(855,298);ui(()->check(activity.world.game.skillCd>0&&activity.world.game.shots.isEmpty(),"bow user casts selected radial skill"));
        screenshot("08-custom-dungeon.png");
    }
    private void screenshotDialog(String name){ui(()->{try{View v=activity.workshopDialog.getWindow().getDecorView();Bitmap b=Bitmap.createBitmap(v.getWidth(),v.getHeight(),Bitmap.Config.ARGB_8888);v.draw(new Canvas(b));writeBitmap(b,name);}catch(IOException e){throw new RuntimeException(e);}});}
    private void writeBitmap(Bitmap b,String name)throws IOException{
        File dir=new File(activity.getExternalFilesDir(null),"smoke");if(!dir.isDirectory()&&!dir.mkdirs())throw new IOException("screenshot directory");
        try(FileOutputStream out=new FileOutputStream(new File(dir,name))){b.compress(Bitmap.CompressFormat.PNG,100,out);}b.recycle();
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
