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
            ui(()->activity.world.replace(new ActionGame()));
            ui(()->activity.world.replace(new ActionGame()));
            careerFlow();
            ui(()->activity.world.replace(new ActionGame()));
            growthTreesFlow();
            ui(()->activity.world.replace(new ActionGame()));
            inscriptionFlow();
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
            tap(855,210); // Career skill is the upper-right control; 855,298 is the general active skill.
            SystemClock.sleep(330);tap(855,210);ui(()->check(activity.world.game.skillCd>0,"career skill button triggers"));
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
            result.putString("stream","ARPG_SMOKE_OK: independent three-final purchases and same-category locks; actual endless entry, fifth-boss reward and sixth-floor exit; full bag potion pickup after drinking; v11 AtomicFile floor, threat and build; main and personal story choices, once-only rewards, personal passive and AtomicFile; legal-cast 100 erosion; real burner zones and bell shields with snapshot; launch, landscape, character selection, simultaneous move/attack, dodge, skill, pause, atomic save and resume; custom editor save/cancel, career-bound active skill, talent purchase/locks and profile persistence; deliberate two-step inscription selection; soul binding/cancel, independent soul button, radiant domain, forbidden soul art, corruption costs and soul snapshot persistence; relic cabinet equip/cancel, duplicate guard, character isolation, checkpoint discovery and relic manual save; career level gate, save/cancel, specialization persistence, class-independent weapon and background passives; 1887 setting dossier; four rebuilt careers, career selection and save migration.\n");
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
            ActionGame.Drop bottle=new ActionGame.Drop(g.x+12,g.y+12,1,false);bottle.potion=true;g.drops.add(bottle);g.hitStop=0;g.update(.016f,new ActionGame.Input());g.mode=ActionGame.Mode.PLAY;g.potions=5;g.hp=g.maxHp()-50;check(g.potions==5&&g.drops.size()==1,"full bag bottle stays visible");});
        screenshot("29-potion-pickup-fixture.png");tap(648,456);
        ui(()->{ActionGame g=activity.world.game;check(g.potions==4,"actual potion button drinks: mode="+g.mode+" potions="+g.potions+" hp="+g.hp+" maxHp="+g.maxHp()+" position="+g.x+","+g.y);g.hitStop=0;g.update(.016f,new ActionGame.Input());check(g.potions==5&&g.drops.isEmpty(),"open slot collects bottle once");g.pause();try{SaveStore store=new SaveStore(activity);store.write(3,g);ActionGame a=store.read(3);check(a.endless&&a.endlessDepth==6&&a.bestDepth==5&&a.potions==5&&a.threat==g.threat&&a.rank(2)==1&&a.rank(6)==1&&a.rank(11)==1,"v10 AtomicFile restores complete endless build");}catch(IOException e){throw new RuntimeException(e);}});
    }
    private void growthTreesFlow(){
        ui(()->{activity.world.stopFrames();ActionGame g=activity.world.game;g.experience[g.hero]=800;g.bank=1000;});
        tap(537,501);waitForIdleSync();ui(()->check(activity.world.showTalents,"growth hub opens from camp"));
        tap(432,35);waitForIdleSync();ui(()->check(activity.world.showCoreTalents,"five-branch tab opens"));
        tap(110,180);waitForIdleSync();ui(()->{check(activity.talentDialog.getButton(AlertDialog.BUTTON_POSITIVE).isEnabled(),"root five-branch node is purchasable");activity.talentDialog.getButton(AlertDialog.BUTTON_POSITIVE).performClick();});waitForIdleSync();
        ui(()->check(activity.world.game.coreRank(0)==1,"five-branch node purchase persists"));
        tap(257,35);waitForIdleSync();ui(()->check(activity.world.showCareerTalents,"career skill tab opens"));
        tap(450,185);waitForIdleSync();ui(()->{check(activity.talentDialog.getButton(AlertDialog.BUTTON_POSITIVE).isEnabled(),"root career node is purchasable");activity.talentDialog.getButton(AlertDialog.BUTTON_POSITIVE).performClick();});waitForIdleSync();
        ui(()->check(activity.world.game.careerTalentRank(0)==1,"career node purchase persists"));
        tap(805,496);waitForIdleSync();ui(()->{check(activity.resetDialog!=null&&activity.resetDialog.isShowing(),"full reset confirmation opens");activity.resetDialog.getButton(AlertDialog.BUTTON_POSITIVE).performClick();});waitForIdleSync();
        ui(()->check(activity.world.game.coreRank(0)==0&&activity.world.game.careerTalentRank(0)==0,"full reset refunds five-branch and career nodes"));
        tap(770,35);waitForIdleSync();ui(()->check(activity.world.showTraining,"camp training tab opens"));
        tap(830,79);waitForIdleSync();ui(()->check(activity.world.showPermits,"expedition permit shop opens"));
        tap(365,211);waitForIdleSync();ui(()->check(activity.world.game.fieldPermits[activity.world.game.buildId()][0]==1&&activity.world.game.bank==920,"permit purchase debits camp bank and unlocks start shield"));
        tap(770,35);tap(900,35);waitForIdleSync();ui(()->{check(!activity.world.showTalents,"growth hub returns to camp");activity.world.startFrames();});
    }
    private void inscriptionFlow(){
        ui(()->{activity.world.stopFrames();ActionGame g=new ActionGame();g.start(8821);g.enemies.clear();g.update(.016f,new ActionGame.Input());check(g.mode==ActionGame.Mode.REWARD&&g.hasTacticDraft()&&!g.tacticDraftOpen,"combat clear pauses before inscription selection");activity.world.replace(g);});
        waitForIdleSync();screenshot("30-inscription-intermission.png");
        tap(480,410);ui(()->check(activity.world.game.tacticDraftOpen,"explicitly enter inscription selection after intermission"));
        screenshot("30-inscription-selection.png");
        tap(180,290);ui(()->{ActionGame g=activity.world.game;check(g.tacticSelected==0&&g.tacticRanks[g.tacticOffers[0]]==0&&!g.tacticChosen,"first card tap only previews and cannot spend inscription");});
        tap(480,449);ui(()->{ActionGame g=activity.world.game;check(g.tacticChosen&&!g.tacticDraftOpen&&g.tacticRanks[0]==1,"separate confirm button commits selected inscription");activity.world.startFrames();});
    }
    private void journalItem(int index){ui(()->{AlertDialog dialog=activity.storyDialog;ListView list=dialog==null?null:dialog.getListView();if(list==null){if(dialog!=null)dialog.dismiss();StoryJournal.show(activity);dialog=activity.storyDialog;list=dialog.getListView();}check(list!=null,"story journal list is present");list.performItemClick(null,index,index);});waitForIdleSync();}
    private void storyButton(int which){ui(()->activity.storyDialog.getButton(which).performClick());waitForIdleSync();}
    private void challengeFlow(){
        ui(()->{activity.world.stopFrames();ActionGame g=activity.world.game;g.chooseCareer(CareerSystem.HUNTER,0,0);});
        tap(835,105);waitForIdleSync();journalItem(0);
        ui(()->{check(((TextView)activity.storyDialog.getWindow().getDecorView().findViewWithTag("story-text")).getText().toString().contains("诺拉"),"journal intro opens from real camp entry");});storyButton(AlertDialog.BUTTON_NEGATIVE);
        tap(785,405);waitForIdleSync();ui(()->check(activity.world.game.mode==ActionGame.Mode.PLAY,"story flow starts expedition: "+activity.world.game.mode));
        // Deterministic clear fixture exercises the actual clear event, choice UI and durable award.
        ui(()->{ActionGame g=activity.world.game;for(ActionGame.Enemy e:g.enemies)e.hp=0;g.update(.05f,new ActionGame.Input());check(g.mode==ActionGame.Mode.REWARD&&g.storyUnlocked==1,"clear reveals main scene: "+g.mode+" flags="+g.storyUnlocked);});
        // The inscription intermission owns the first reward-screen tap; complete its two-step flow before opening the story scene.
        ui(()->check(activity.world.game.hasTacticDraft()&&!activity.world.game.tacticDraftOpen,"main-story fixture begins with an inscription intermission"));
        tap(480,410);ui(()->check(activity.world.game.tacticDraftOpen,"inscription intermission opens before story scene"));
        tap(180,290);tap(480,449);ui(()->check(activity.world.game.tacticChosen&&!activity.world.game.tacticDraftOpen,"inscription is confirmed before opening story scene"));
        tap(480,412);waitForIdleSync();screenshotNativeDialog("23-main-story-fixture.png",activity.storyDialog);
        int[] bank={0};ui(()->bank[0]=activity.world.game.bank);storyButton(AlertDialog.BUTTON_POSITIVE);ui(()->check(activity.world.game.storyChoices[0]==1&&activity.world.game.bank==bank[0]+12,"main choice commits actual once-only reward"));
        tap(480,412);waitForIdleSync();journalItem(1);
        ui(()->check(activity.world.game.bank==bank[0]+12,"rereading cannot duplicate main reward"));storyButton(AlertDialog.BUTTON_NEGATIVE);ui(()->{ActionGame g=activity.world.game;g.chooseReward(0);g.pause();g.abandon();g.returnCamp();});
        tap(835,105);waitForIdleSync();journalItem(6);journalItem(0);screenshotNativeDialog("24-hunter-story-fixture.png",activity.storyDialog);
        float[] personalHp={0};ui(()->personalHp[0]=activity.world.game.maxHp());storyButton(AlertDialog.BUTTON_POSITIVE);
        ui(()->{ActionGame g=activity.world.game;check(g.maxHp()==personalHp[0]+4,"personal decision grants actual persistent passive");try{SaveStore store=new SaveStore(activity);store.write(2,g);ActionGame loaded=store.read(2);check(loaded.storyChoices[0]==1&&loaded.characterJobs[0]==CareerSystem.HUNTER&&loaded.characterChoices[0][0]==1&&loaded.maxHp()==g.maxHp(),"story and derived health survive AtomicFile");}catch(IOException e){throw new RuntimeException(e);}activity.world.replace(new ActionGame());});
        ui(()->{ActionGame g=new ActionGame();g.bindSoul(2,0);g.start(91);g.blocks.clear();g.enemies.clear();g.x=560;g.y=430;ActionGame.Enemy e=new ActionGame.Enemy(g.nextId++,0,g.x+100,g.y,1000);g.enemies.add(e);
            // Six legal hunter taboo casts; combat decay is slower and each cast uses its full 14-second cooldown.
            for(int cast=0;cast<6;cast++){g.soulEnergy=40;g.soulSkill();if(cast<5)for(int t=0;t<841;t++){e.stun=1;g.hitStop=0;g.update(1f/60,new ActionGame.Input());g.signals.clear();}}
            check(g.corruption==100&&g.ruptureTime>0,"legal soul-art sequence reaches actual 100 erosion");activity.world.replace(g);g.resume();});
        screenshot("25-erosion-100-fixture.png");
        ui(()->{ActionGame g=activity.world.game;g.enemies.clear();g.shots.clear();g.signals.clear();g.hitStop=0;g.room=4;
            ActionGame.Enemy hunter=new ActionGame.Enemy(g.nextId++,4,710,320,130);hunter.state=1;hunter.timer=.5f;hunter.aimX=-.8f;hunter.aimY=.6f;hunter.targetX=g.x;hunter.targetY=g.y;g.enemies.add(hunter);
            ActionGame.Enemy fire=new ActionGame.Enemy(g.nextId++,5,440,280,120);fire.state=1;fire.timer=.001f;fire.targetX=g.x;fire.targetY=g.y;fire.aimX=.6f;fire.aimY=.8f;g.enemies.add(fire);
            ActionGame.Enemy bell=new ActionGame.Enemy(g.nextId++,6,580,250,140);bell.state=1;bell.timer=.001f;bell.aimX=0;bell.aimY=1;g.enemies.add(bell);g.update(1f/60,new ActionGame.Input());check(g.hazards.size()==2&&hunter.guard==24,"new enemy AI produces actual ground warnings and ally shields");try{ActionGame loaded=ActionSave.decode(ActionSave.encode(g));check(loaded.hazards.size()==2&&loaded.enemies.get(0).guard==24,"new enemy runtime restores");}catch(IOException e){throw new RuntimeException(e);}});
        screenshot("26-mixed-enemies-fixture.png");
    }
    private void careerFlow(){
        tap(350,108);waitForIdleSync();
        ui(()->{check(activity.settingDialog!=null&&activity.settingDialog.isShowing(),"1887 dossier opens from camp");check(WorldSetting.DISTRICTS.length==5&&WorldSetting.DOSSIER.contains("1887"),"setting content present");});
        screenshotNativeDialog("17-greybridge-dossier.png",activity.settingDialog);
        ui(()->activity.settingDialog.getButton(AlertDialog.BUTTON_POSITIVE).performClick());waitForIdleSync();
        tap(510,142);waitForIdleSync();
        ui(()->{View root=activity.careerDialog.getWindow().getDecorView();Spinner jobs=root.findViewWithTag("career-job");check(jobs.getCount()==4,"exactly four rebuilt careers");jobs.setSelection(CareerSystem.ASSASSIN);((Spinner)root.findViewWithTag("career-background")).setSelection(1);check(!root.findViewWithTag("career-spec").isEnabled(),"level-one specialization remains locked");activity.careerDialog.getButton(AlertDialog.BUTTON_POSITIVE).performClick();});waitForIdleSync();
        ui(()->check(activity.world.game.job()==CareerSystem.ASSASSIN&&activity.world.game.background()==1,"new career and background commit"));
        tap(785,405);ui(()->check(activity.world.game.careerWard==10,"background grants entry shield"));float basePeriod=activity.world.game.attackPeriod();tap(855,210);
        ui(()->check(activity.world.game.careerSkillMode==2&&activity.world.game.careerSkillTime>=3.8f,"assassin shadow strike opens a short fast-attack window: mode="+activity.world.game.careerSkillMode+" time="+activity.world.game.careerSkillTime+" period="+activity.world.game.attackPeriod()+" base="+basePeriod));
        screenshot("16-career-combat.png");tap(900,37);
        ui(()->{try{SaveStore store=new SaveStore(activity);store.write(1,activity.world.game);ActionGame loaded=store.read(1);check(loaded.job()==CareerSystem.ASSASSIN&&loaded.background()==1&&loaded.careerSkillMode==2&&loaded.careerSkillTime>0,"rebuilt career and skill state survive AtomicFile");}catch(IOException e){throw new RuntimeException(e);}});
    }
    private void screenshotNativeDialog(String name,AlertDialog dialog){ui(()->{try{if(dialog==null||dialog.getWindow()==null)return;View v=dialog.getWindow().getDecorView();if(!v.isAttachedToWindow()||v.getWidth()<=0||v.getHeight()<=0)return;Bitmap b=Bitmap.createBitmap(v.getWidth(),v.getHeight(),Bitmap.Config.ARGB_8888);v.draw(new Canvas(b));writeBitmap(b,name);}catch(IOException e){throw new RuntimeException(e);}});}
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
        ui(()->{check(activity.world.game.mode==ActionGame.Mode.REWARD,"relic discovery leaves the run at its reward screen");activity.world.game.chooseReward(0);check(activity.world.game.mode==ActionGame.Mode.PLAY,"reward selection returns to active expedition");activity.world.game.pause();check(activity.world.game.mode==ActionGame.Mode.PAUSED,"relic expedition pauses before abandonment");});
        ui(()->{try{SaveStore store=new SaveStore(activity);store.write(2,activity.world.game);ActionGame loaded=store.read(2);check(loaded.relicOwned==found[1]&&loaded.hasRelic(1)&&loaded.lastRelic==found[0],"relic discovery and loadout survive atomic manual save");activity.world.replace(loaded);loaded.abandon();loaded.returnCamp();check(loaded.mode==ActionGame.Mode.CAMP,"failed relic expedition returns to camp");check(loaded.relicOwned==found[1],"discovery survives failed expedition");}catch(IOException e){throw new RuntimeException(e);}});
        tap(815,142);waitForIdleSync();ui(()->{java.util.List<Integer> ownedIds=new java.util.ArrayList<>();for(int id=0;id<RelicSystem.NAMES.length;id++)if(RelicSystem.owns(activity.world.game.relicOwned,id))ownedIds.add(id);int ownedIndex=ownedIds.indexOf(found[0]);Spinner slot=(Spinner)activity.relicDialog.getWindow().getDecorView().findViewWithTag("relic-slot-0");check(found[0]>0&&ownedIndex>=0,"discovered relic is in shared collection: id="+found[0]+" mask="+activity.world.game.relicOwned+" ids="+ownedIds);slot.setSelection(ownedIndex);});waitForIdleSync();ui(()->{Spinner slot=(Spinner)activity.relicDialog.getWindow().getDecorView().findViewWithTag("relic-slot-0");check(slot.getSelectedItemPosition()>=0&&activity.relicDialog.getButton(AlertDialog.BUTTON_POSITIVE).isEnabled(),"new relic selection validates: id="+found[0]+" position="+slot.getSelectedItemPosition()+" count="+slot.getCount());activity.relicDialog.getButton(AlertDialog.BUTTON_POSITIVE).performClick();});waitForIdleSync();
        ui(()->check(activity.world.game.hasRelic(found[0]),"newly found relic can be equipped in camp"));
    }
    private void screenshotRelicDialog(String name){ui(()->{try{if(activity.relicDialog==null||activity.relicDialog.getWindow()==null)return;View v=activity.relicDialog.getWindow().getDecorView();if(!v.isAttachedToWindow()||v.getWidth()<=0||v.getHeight()<=0)return;Bitmap b=Bitmap.createBitmap(v.getWidth(),v.getHeight(),Bitmap.Config.ARGB_8888);v.draw(new Canvas(b));writeBitmap(b,name);}catch(IOException e){throw new RuntimeException(e);}});}
    private void soulFlow(){
        tap(780,500);waitForIdleSync();
        ui(()->{check(activity.soulDialog.isShowing(),"soul altar opens");((Spinner)activity.soulDialog.getWindow().getDecorView().findViewWithTag("soul-kind")).setSelection(1);});waitForIdleSync();
        screenshotSoulDialog("09-radiant-altar.png");
        ui(()->activity.soulDialog.getButton(AlertDialog.BUTTON_POSITIVE).performClick());
        ui(()->check(activity.world.game.soulKind()==1&&activity.world.game.soulArt()==0,"radiant binding committed"));
        tap(780,500);waitForIdleSync();ui(()->((Spinner)activity.soulDialog.getWindow().getDecorView().findViewWithTag("soul-kind")).setSelection(2));waitForIdleSync();ui(()->activity.soulDialog.getButton(AlertDialog.BUTTON_NEGATIVE).performClick());
        ui(()->check(activity.world.game.soulKind()==1,"cancel soul draft preserves binding"));
        tap(785,405);tap(735,240);ui(()->{ActionGame g=activity.world.game;check(g.soulCd>0&&(g.soulWard>0||g.soulBuff>0||g.enemies.stream().anyMatch(e->e.hex>0||e.hp<e.maxHp)),"dedicated soul button casts the selected career's radiant form");});
        SystemClock.sleep(150);screenshot("10-radiant-career-form.png");tap(900,37);
        float[] paused={0,0,0,0};ui(()->{ActionGame g=activity.world.game;paused[0]=g.soulCd;paused[1]=g.soulWard;paused[2]=g.soulBuff;paused[3]=(float)g.enemies.stream().mapToDouble(e->e.hex).sum();});SystemClock.sleep(180);
        ui(()->{ActionGame g=activity.world.game;check(g.soulCd==paused[0]&&g.soulWard==paused[1]&&g.soulBuff==paused[2]&&Math.abs((float)g.enemies.stream().mapToDouble(e->e.hex).sum()-paused[3])<.01f,"pause freezes career soul effects and cooldown");
            try{SaveStore store=new SaveStore(activity);store.write(3,g);ActionGame loaded=store.read(3);check(loaded.soulKind()==1&&loaded.soulEnergy==g.soulEnergy&&loaded.soulWard==g.soulWard&&loaded.soulBuff==g.soulBuff,"career soul snapshot restored");}catch(IOException e){throw new RuntimeException(e);}
            ActionGame next=new ActionGame();next.selectCustom(1);check(next.chooseCareer(CareerSystem.HUNTER,0,0)&&next.job()==CareerSystem.HUNTER,"forbidden soul fixture binds an explicit hunter career");activity.world.replace(next);
        });
        tap(350,500);waitForIdleSync();ui(()->((Spinner)activity.workshopDialog.getWindow().getDecorView().findViewWithTag("profile-soul")).setSelection(2));waitForIdleSync();
        ui(()->((Spinner)activity.workshopDialog.getWindow().getDecorView().findViewWithTag("profile-soul-art")).setSelection(1));waitForIdleSync();
        ui(()->activity.workshopDialog.getButton(AlertDialog.BUTTON_POSITIVE).performClick());
        ui(()->check(activity.world.game.soulKind()==2&&activity.world.game.soulArt()==1,"workshop selects forbidden soul art"));
        tap(780,500);waitForIdleSync();screenshotSoulDialog("11-forbidden-altar.png");ui(()->activity.soulDialog.getButton(AlertDialog.BUTTON_POSITIVE).performClick());
        ui(()->check(activity.world.game.soulArt()==1,"reopening altar preserves selected second art"));
        tap(785,405);float[] hp={0};ui(()->hp[0]=activity.world.game.hp);tap(735,240);
        ui(()->check(activity.world.game.soulBuff>0&&activity.world.game.corruption>29&&activity.world.game.corruption<=30&&activity.world.game.hp<hp[0],"forbidden soul art has actual cost and buff"));
        tap(855,210);SystemClock.sleep(100);screenshot("12-forbidden-soul-art.png");
        ui(()->{check(activity.world.game.soulBuff>0&&activity.world.game.soulArt()==1&&activity.world.game.careerSkillMode==1&&activity.world.game.skillCd>0,"career active is independent of forbidden soul art: corruption="+activity.world.game.corruption+" job="+activity.world.game.job()+" mode="+activity.world.game.mode+" skillMode="+activity.world.game.careerSkillMode+" skillCd="+activity.world.game.skillCd+" custom="+activity.world.game.custom);try{ActionGame g=ActionSave.decode(ActionSave.encode(activity.world.game));check(g.soulBuff>0&&g.soulArt()==1,"forbidden runtime persists");}catch(IOException e){throw new RuntimeException(e);}});
    }
    private void screenshotSoulDialog(String name){ui(()->{try{if(activity.soulDialog==null||activity.soulDialog.getWindow()==null)return;View v=activity.soulDialog.getWindow().getDecorView();if(!v.isAttachedToWindow()||v.getWidth()<=0||v.getHeight()<=0)return;Bitmap b=Bitmap.createBitmap(v.getWidth(),v.getHeight(),Bitmap.Config.ARGB_8888);v.draw(new Canvas(b));writeBitmap(b,name);}catch(IOException e){throw new RuntimeException(e);}});}
    private void workshopFlow(){
        tap(250,142);tap(140,230);ui(()->check(activity.world.game.custom==0,"custom roster selects independent slot"));
        tap(350,500);waitForIdleSync();
        ui(()->{AlertDialog d=activity.workshopDialog;check(d!=null&&d.isShowing(),"editor opens from camp");View root=d.getWindow().getDecorView();
            ((EditText)root.findViewWithTag("profile-name")).setText("暮灯艾文");
            ((Spinner)root.findViewWithTag("profile-weapon")).setSelection(1);
            ((Spinner)root.findViewWithTag("profile-color")).setSelection(4);
            ((Spinner)root.findViewWithTag("profile-hair")).setSelection(2);
        });waitForIdleSync();SystemClock.sleep(150);
        screenshotDialog("05-character-workshop.png");
        ui(()->activity.workshopDialog.getButton(AlertDialog.BUTTON_POSITIVE).performClick());waitForIdleSync();
        ui(()->{ActionGame g=activity.world.game;check(g.heroName().equals("暮灯艾文")&&g.hero==1&&g.skillName().equals("瞬时猎杀")&&g.profile().color==4&&g.profile().hair==2,"native editor commits customization");});
        tap(350,500);waitForIdleSync();ui(()->{View root=activity.workshopDialog.getWindow().getDecorView();((EditText)root.findViewWithTag("profile-name")).setText("取消的修改");activity.workshopDialog.getButton(AlertDialog.BUTTON_NEGATIVE).performClick();});
        ui(()->check(activity.world.game.heroName().equals("暮灯艾文"),"cancel preserves profile"));
        tap(530,500);ui(()->check(activity.world.showTalents,"tree opens"));tap(175,164);
        ui(()->{check(activity.talentDialog.getButton(AlertDialog.BUTTON_POSITIVE).isEnabled(),"root learn enabled");activity.talentDialog.getButton(AlertDialog.BUTTON_POSITIVE).performClick();});
        ui(()->check(activity.world.game.rank(0)==1&&activity.world.game.availablePoints()==1,"learn spends exactly one point"));
        tap(110,380);ui(()->{check(!activity.talentDialog.getButton(AlertDialog.BUTTON_POSITIVE).isEnabled(),"locked capstone cannot be learned");activity.talentDialog.getButton(AlertDialog.BUTTON_NEGATIVE).performClick();});
        screenshot("06-talent-tree.png");tap(900,38);screenshot("07-custom-camp.png");
        ui(()->{try{SaveStore store=new SaveStore(activity);store.write(2,activity.world.game);ActionGame loaded=store.read(2);check(loaded.custom==0,"custom snapshot restores selected slot");check(loaded.hero==1,"custom snapshot restores weapon");check(loaded.skillName().equals("瞬时猎杀"),"custom snapshot restores career");check(loaded.rank(0)==1,"custom snapshot restores talent rank");check(loaded.heroName().equals("暮灯艾文"),"custom snapshot restores profile name");activity.world.replace(loaded);check(new WorldView(activity,loaded,activity).customRoster,"restored custom profile opens custom roster");}catch(IOException e){throw new RuntimeException(e);}});
        tap(785,405);waitForIdleSync();SystemClock.sleep(250);tap(855,210);ui(()->check(activity.world.game.skillCd>0&&activity.world.game.careerSkillMode==1,"bow user casts hunter career skill"));
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
        try{WorldView v=activity.world;if(!v.isAttachedToWindow()||v.getWidth()<=0||v.getHeight()<=0)return;Bitmap b=Bitmap.createBitmap(v.getWidth(),v.getHeight(),Bitmap.Config.ARGB_8888);v.draw(new Canvas(b));File dir=new File(activity.getExternalFilesDir(null),"smoke");if(!dir.isDirectory()&&!dir.mkdirs())throw new IOException("screenshot directory");
            try(FileOutputStream out=new FileOutputStream(new File(dir,name))){b.compress(Bitmap.CompressFormat.PNG,100,out);}b.recycle();
        }catch(IOException e){throw new RuntimeException(e);}
    });}
}
