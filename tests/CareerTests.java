import io.wayfarer.rpg.core.*;
import java.io.*;
import java.util.*;
import static io.wayfarer.rpg.core.CareerSystem.*;

public final class CareerTests {
    private static int count;
    private static void check(boolean b,String why){count++;if(!b)throw new AssertionError(why);}
    private static void near(float a,float b,String why){check(Math.abs(a-b)<.04f,why+": "+a+" != "+b);}
    private static ActionGame game(int job,int spec,int background){
        ActionGame g=new ActionGame();g.experience[0]=160;check(g.chooseCareer(job,spec,background),"configure career");g.start(181);g.blocks.clear();g.enemies.clear();g.x=560;g.y=600;g.invulnerable=0;mob(g,620,600);return g;
    }
    private static ActionGame.Enemy mob(ActionGame g,float x,float y){ActionGame.Enemy e=new ActionGame.Enemy(g.nextId++,0,x,y,1000);e.stun=1;g.enemies.add(e);return e;}
    private static void tick(ActionGame g){for(ActionGame.Enemy e:g.enemies)e.stun=1;g.update(1f/60f,new ActionGame.Input());g.signals.clear();}
    private static void hit(ActionGame g,float value){g.hitStop=0;g.invulnerable=0;g.shots.add(new ActionGame.Shot(g.x,g.y,0,0,value,false));tick(g);}
    private static void shot(ActionGame g,float value){g.hitStop=0;ActionGame.Enemy e=g.enemies.get(0);g.shots.add(new ActionGame.Shot(e.x,e.y,0,0,value,true));tick(g);}
    private static void selection()throws Exception{
        ActionGame g=new ActionGame();check(!g.chooseCareer(1,1,1)&&g.job()==0,"spec locked before level three");check(g.chooseCareer(1,0,1),"class and background available at level one");near(g.maxHp(),126,"guardian base health");g.selectHero(1);check(g.job()==0&&g.background()==0,"preset career independent");g.selectCustom(1);g.profile().xp=160;check(g.chooseCareer(3,2,4),"custom chooses career");g.selectHero(0);check(g.job()==1&&g.background()==1,"original configuration retained");
        g.experience[0]=160;check(g.chooseCareer(2,2,3),"level three unlock");g.start(1);check(!g.chooseCareer(1,1,6),"cannot respec in combat");g.pause();check(!g.chooseCareer(1,1,6),"cannot respec paused");ActionGame a=ActionSave.decode(ActionSave.encode(g));a.abandon();a.returnCamp();a.selectCustom(1);check(a.job()==3&&a.specialization()==2&&a.background()==4,"all character configurations persist");BuildSystem.Profile draft=a.profile().copy();draft.weapon=0;a.customize(1,draft);check(a.job()==3&&a.specialization()==2&&a.hero==0,"changing weapon preserves independent career");
        for(int[] v:new int[][]{{0,1,0},{CareerSystem.CLASSES.length,0,0},{-1,0,0},{2,3,0},{1,0,7}})check(!a.chooseCareer(v[0],v[1],v[2]),"invalid class/spec/background rejected");
        System.out.println("PASS class selection, specialization level gate, camp-only respec, independent builds and unrestricted weapon changes");
    }
    private static void specializations()throws Exception{
        ActionGame g=game(1,1,0);float hp=g.hp;hit(g,20);near(hp-g.hp,18.4f,"guardian reduces enemy damage");g.skill();near(g.careerWard,14,"bulwark grants shield");g.skillCd=0;g.skill();near(g.careerWard,14,"bulwark does not stack");hp=g.hp;hit(g,10);near(g.hp,hp,"career shield absorbs reduced damage");near(g.careerWard,4.8f,"career shield remainder");
        g=game(1,2,0);hit(g,10);near(g.careerCharge,4,"revenge primes on life damage");float target=g.enemies.get(0).hp;shot(g,20);near(target-g.enemies.get(0).hp,25,"revenge boosts first attack");near(g.careerCharge,0,"revenge consumed");target=g.enemies.get(0).hp;shot(g,20);near(target-g.enemies.get(0).hp,20,"second hit normal");
        g=game(1,2,1);hit(g,5);near(g.careerCharge,0,"fully blocked hit cannot arm revenge");g.mode=ActionGame.Mode.CAMP;g.chooseCareer(1,2,0);g.bindSoul(2,0);g.start(1);g.skill();near(g.careerCharge,0,"sacrifice cannot arm revenge");near(g.maxHp()-g.hp,g.maxHp()*.04f,"guardian does not reduce sacrifice");
        g=game(2,1,0);near(g.speed(),205*1.06f,"ranger base movement");g.enemies.get(0).x=800;shot(g,20);near(g.enemies.get(0).hp,976.4f,"hunter ranged hit bonus");g.enemies.get(0).x=650;shot(g,20);near(g.enemies.get(0).hp,956.4f,"hunter close hit normal");
        g=game(2,2,0);g.dodge(0,-1);near(g.careerCharge,3,"skirmish arms on dodge");g.rollTime=0;shot(g,20);near(g.enemies.get(0).hp,975,"skirmish one empowered hit");
        g=game(3,1,0);g.hp=20;g.skill();near(g.hp,20+g.maxHp()*.03f,"healer active restores health");near(g.careerWard,8,"healer grants shield");near(g.skillCd,6.6f,"prayer class base cooldown");
        g=game(3,2,0);float damage=g.damage();g.skill();near(g.enemies.get(0).hp,1000-damage*1.8f*1.15f,"ritual improves radial skill");g=game(3,2,0);g.mode=ActionGame.Mode.CAMP;g.bindSoul(1,1);g.start(1);g.soulSkill();near(g.soulCd,10.8f,"ritual shortens soul cooldown");
        g=game(2,2,0);g.dodge(0,-1);g.pause();float charge=g.careerCharge;for(int i=0;i<60;i++)tick(g);near(g.careerCharge,charge,"pause freezes profession proc");ActionGame a=ActionSave.decode(ActionSave.encode(g));near(a.careerCharge,charge,"proc duration persisted");a.resume();for(int i=0;i<200;i++)tick(a);near(a.careerCharge,0,"proc expires");g.resume();g.room=2;g.generateRoom();near(g.careerCharge,0,"room clears attack proc");
        System.out.println("PASS all six specializations, class passives, conditional hit range, shield consumption and proc lifetime");
    }
    private static void backgrounds()throws Exception{
        ActionGame g=game(0,0,1);near(g.careerWard,10,"smith entry ward");hit(g,4);near(g.careerWard,6,"smith shield consumed");ActionGame a=ActionSave.decode(ActionSave.encode(g));near(a.careerWard,6,"load does not refill shield");g.room=2;g.generateRoom();near(g.careerWard,10,"new room refreshes smith shield");
        g=game(0,0,2);near(g.speed(),205*1.03f,"guide movement");g.hp=10;g.enemies.clear();g.roomCleared=true;g.x=280;g.y=80;float max=g.maxHp();tick(g);near(g.hp,10+max*.26f,"guide actual spring healing");check(g.room==2,"spring transition still works");
        g=game(0,0,3);float speed=g.speed();g.hp=g.maxHp()*.34f;near(g.speed(),speed*1.12f,"messenger low health movement");g.hp=g.maxHp()*.35f;near(g.speed(),speed,"messenger threshold is strict");
        g=game(0,0,4);g.hp=10;max=g.maxHp();g.potion();near(g.hp,10+max*.45f+6,"medic adds six base potion health");
        g=game(0,0,5);g.hp=20;g.enemies.get(0).hp=0;mob(g,850,200);tick(g);near(g.hp,21,"gravekeeper heal on defeat");near(g.soulEnergy,0,"unbound gravekeeper cannot gain soul");int kills=g.kills;tick(g);near(g.hp,21,"same corpse cannot heal twice");check(g.kills==kills,"kill counted once");
        g=game(0,0,5);g.mode=ActionGame.Mode.CAMP;g.bindSoul(1,0);g.start(10);g.blocks.clear();g.enemies.clear();g.soulEnergy=0;ActionGame.Enemy boss=new ActionGame.Enemy(g.nextId++,3,800,200,100);boss.hp=0;g.enemies.add(boss);mob(g,900,200);tick(g);near(g.soulEnergy,8,"gravekeeper boss energy");
        g=game(0,0,6);g.enemies.get(0).hp=0;g.enemies.get(0).x=800;g.enemies.get(0).y=200;mob(g,900,200);tick(g);check(g.drops.stream().anyMatch(d->!d.heal&&d.amount==8),"scavenger increases actual room-one coin drop");
        System.out.println("PASS all six background passives, new-room shield, spring transition, conditional movement, potion and defeat rewards");
    }
    private static void synergies()throws Exception{
        ActionGame g=game(2,2,0);g.mode=ActionGame.Mode.CAMP;g.relicOwned=RelicSystem.ALL;g.equipRelics(3,6);g.bindSoul(2,0);g.start(91);g.blocks.clear();g.enemies.clear();g.x=560;g.y=600;ActionGame.Enemy e=mob(g,620,600);e.hex=3;g.dodge(0,-1);g.rollTime=0;shot(g,20);near(e.hp,1000-20*1.25f*1.35f*1.15f*1.1f,"skirmish, bell, curse and ring multiply once");near(g.relicEcho,0,"bell consumed alongside skirmish");near(g.careerCharge,0,"profession consumed alongside bell");
        g=game(3,1,4);g.mode=ActionGame.Mode.CAMP;g.relicOwned=RelicSystem.ALL;g.equipRelics(4,0);g.bindSoul(2,0);g.start(5);g.corruption=60;g.soulQuiet=3;g.hp=10;g.potion();near(g.hp,10+(g.maxHp()*.45f+6)*.88f*1.1f,"medic potion composes with chalice cleansing before healing");
        g=game(1,1,1);g.mode=ActionGame.Mode.CAMP;g.experience[0]=1200;g.learn(8);g.learn(9);g.bindSoul(1,0);g.start(1);g.blocks.clear();g.enemies.clear();mob(g,900,200);g.skill();float total=g.soulWard+g.careerWard+g.shield;g.soulEnergy=0;float hp=g.hp;hit(g,20);near(g.hp,hp,"stacked shield layers protect");near(g.soulWard+g.careerWard+g.shield,total-18.4f,"shield layer sum correct");check(g.soulEnergy>0,"radiance gains energy from career shield absorption");
        g=game(2,2,5);g.mode=ActionGame.Mode.CAMP;g.bindSoul(1,0);g.start(2);g.blocks.clear();g.enemies.clear();g.x=560;g.y=600;mob(g,650,600).hp=1;mob(g,900,200);g.dodge(0,-1);g.rollTime=0;g.soulSkill();for(int i=0;i<65;i++)tick(g);check(g.soulEnergy==2&&g.careerCharge>0,"domain kill gives one gravekeeper proc without consuming attack charge");
        System.out.println("PASS profession/background combinations with relics, souls, talent shields and kill-source exclusions");
    }
    private static byte[] edit(byte[] bytes,String key,String value)throws Exception{Properties p=new Properties();p.load(new ByteArrayInputStream(bytes));p.setProperty(key,value);ByteArrayOutputStream out=new ByteArrayOutputStream();p.store(out,"");return out.toByteArray();}
    private static void reject(byte[] bytes,String key,String value)throws Exception{boolean bad=false;try{ActionSave.decode(edit(bytes,key,value));}catch(IOException e){bad=true;}check(bad,"reject "+key+"="+value);}
    private static void saves()throws Exception{
        ActionGame g=new ActionGame();g.selectCustom(1);g.profile().xp=160;g.bindSoul(2,1);g.equipRelics(1,2);g.start(1);g.soulSkill();g.pause();byte[] bytes=ActionSave.encode(g);ActionGame old=ActionSave.decode(edit(bytes,"version","4"));check(old.custom==1&&old.soulBuff>0&&old.soulKind()==2&&old.hasRelic(1)&&old.heroXp()==160&&old.job()==0&&old.background()==0,"v4 migration preserves soul/relic snapshot with no invented career");
        reject(bytes,"career4.0",Integer.toString(CareerSystem.CLASSES.length));reject(bytes,"career4.1","1");reject(bytes,"career4.2","7");reject(bytes,"careerWard","1");reject(bytes,"careerCharge","1");reject(bytes,"careerCharge","NaN");
        g=new ActionGame();g.chooseCareer(1,0,1);bytes=ActionSave.encode(g);reject(bytes,"career0.1","1");reject(bytes,"careerWard","10");g=game(1,1,1);g.skill();g.pause();bytes=ActionSave.encode(g);ActionGame a=ActionSave.decode(bytes);near(a.careerWard,14,"profession shield persists exactly");reject(bytes,"careerWard","15");reject(bytes,"careerCharge","1");
        g=game(3,2,0);g.mode=ActionGame.Mode.CAMP;g.bindSoul(1,1);g.start(1);g.soulSkill();bytes=ActionSave.encode(g);ActionSave.decode(bytes);reject(bytes,"soulCd","12");
        System.out.println("PASS v4 migration, all-career persistence, level/provenance checks and modified cooldown validation");
    }
    private static void ordinaryCareers()throws Exception{
        for(int job=POLICE;job<=REPORTER;job++){
            ActionGame a=new ActionGame();check(a.chooseCareer(job,0,0),"ordinary career available without soul");check(!a.chooseCareer(job,1,0),"ordinary specialization level gate");a.experience[0]=160;
            for(int spec=1;spec<=2;spec++){check(a.chooseCareer(job,spec,4),"ordinary specialization choice");ActionGame loaded=ActionSave.decode(ActionSave.encode(a));check(loaded.job()==job&&loaded.specialization()==spec&&loaded.background()==4&&loaded.soulKind()==0,"ordinary career snapshot and unbound operation");}
        }
        ActionGame g=game(POLICE,0,0);float hp=g.hp;hit(g,20);near(hp-g.hp,19,"police reduces actual enemy damage");
        g=game(POLICE,1,0);near(g.speed(),205*1.05f,"patrol movement");g.dodge(0,-1);near(g.rollCd,1.05f,"patrol actual dodge cooldown");
        g=game(POLICE,2,0);g.enemies.get(0).hp=500;shot(g,20);near(g.enemies.get(0).hp,480,"detective strict half-health threshold");shot(g,20);near(g.enemies.get(0).hp,456.4f,"detective wounded hit");
        g=game(SOLDIER,0,0);near(g.damage(),27,"veteran adds three base damage at level three");
        g=game(SOLDIER,1,0);shot(g,20);near(g.enemies.get(0).hp,977.6f,"assault close hit");g.enemies.get(0).x=700;shot(g,20);near(g.enemies.get(0).hp,957.6f,"assault strict range boundary");
        g=game(SOLDIER,2,0);near(g.attackPeriod(),.48f,"rifle instructor does not accelerate saber");g.pause();g.abandon();g.returnCamp();g.selectHero(1);g.experience[1]=160;g.chooseCareer(SOLDIER,2,0);g.start(1);near(g.attackPeriod(),.36f,"rifle instructor accelerates carbine");near(g.skillPeriod(),7,"rifle instructor leaves skill cooldown");
        g=game(DOCTOR,0,0);g.hp=10;g.potion();near(g.hp,10+g.maxHp()*.45f+8,"doctor actual potion bonus");
        g=game(DOCTOR,1,0);g.hp=10;g.skill();near(g.hp,10+g.maxHp()*.04f,"first aid actual skill healing");near(g.careerWard,0,"doctor does not invent magic ward");
        g=game(DOCTOR,2,4);g.mode=ActionGame.Mode.CAMP;g.relicOwned=RelicSystem.ALL;g.equipRelics(4,0);g.bindSoul(2,0);g.start(1);g.corruption=60;g.soulQuiet=3;g.hp=1;g.potion();near(g.hp,1+(g.maxHp()*.45f+8+6)*.88f*1.1f*1.12f,"nursing composes with doctor, background, chalice and purification");
        g=game(MECHANIC,0,0);g.skill();near(g.skillCd,6.7f,"mechanic actual base skill cooldown");g=game(MECHANIC,1,0);g.skill();near(g.skillCd,6.2f,"maintenance extra cooldown reduction");
        g=game(MECHANIC,2,0);near(g.maxHp(),126,"protective equipment health");hp=g.hp;hit(g,20);near(hp-g.hp,19,"protective equipment actual reduction");
        g=game(MINER,0,0);near(g.maxHp(),128,"miner stamina");g=game(MINER,1,0);shot(g,20);near(g.enemies.get(0).hp,977.6f,"excavation equipped melee weapon bonus");g=game(MINER,2,1);near(g.careerWard,12,"shoring and smith use greater ward");hit(g,5);near(g.careerWard,7,"shoring ward spent");ActionGame loaded=ActionSave.decode(ActionSave.encode(g));near(loaded.careerWard,7,"shoring does not refill on restore");loaded.room=2;loaded.generateRoom();near(loaded.careerWard,12,"shoring refreshes next region");
        g=game(BOXER,0,0);near(g.attackPeriod(),.48f*.94f,"boxer melee attack interval");g=game(BOXER,1,0);g.combo=1;g.comboWindow=1;ActionGame.Input in=new ActionGame.Input();in.attack=true;float damage=g.damage();g.update(.05f,in);g.update(.05f,in);near(g.enemies.get(0).hp,1000-damage*1.60f,"heavy punch actual third melee hit");
        g=game(BOXER,2,0);g.dodge(0,-1);near(g.rollCd,.97f,"footwork actual dodge cooldown");
        for(int job:new int[]{BOXER,MINER}){g=new ActionGame();g.selectHero(1);g.experience[1]=160;g.chooseCareer(job,1,0);g.start(1);g.blocks.clear();g.enemies.clear();g.x=560;g.y=600;mob(g,620,600);g.combo=1;g.comboWindow=1;damage=g.damage();g.update(.05f,in);near(g.enemies.get(0).hp,1000-damage*1.35f,"carbine excludes melee training: "+job);near(g.attackPeriod(),.4f,"carbine base cadence preserved");}
        g=game(MERCHANT,0,6);g.enemies.get(0).hp=0;g.enemies.get(0).x=800;mob(g,900,200);tick(g);check(g.drops.stream().anyMatch(d->!d.heal&&d.amount==9),"merchant and scavenger actual coin stack");
        g=game(MERCHANT,1,0);near(g.speed(),205*1.05f,"travelling merchant speed");g=new ActionGame();g.experience[0]=160;g.chooseCareer(MERCHANT,2,0);g.bank=100;check(g.upgradeCost(0)==26&&g.upgradeCost(1)==47,"bargain rounds price upward");g.upgrade(false);check(g.bank==74&&g.power==1,"bargain charges actual price");g.chooseCareer(POLICE,0,0);check(g.upgradeCost(1)==55&&g.bank==74,"leaving merchant restores price without refund");
        g=game(REPORTER,0,0);near(g.speed(),205*1.03f,"reporter mobility");g=game(REPORTER,1,0);g.enemies.get(0).type=3;shot(g,20);near(g.enemies.get(0).hp,977,"war reporter boss attack");g.enemies.get(0).type=0;shot(g,20);near(g.enemies.get(0).hp,957,"war reporter normal enemy unchanged");
        g=game(REPORTER,1,0);g.mode=ActionGame.Mode.CAMP;g.bindSoul(1,1);g.start(1);g.blocks.clear();g.enemies.clear();g.x=560;g.y=600;ActionGame.Enemy boss=mob(g,560,500);boss.type=3;damage=g.damage();g.soulSkill();near(boss.hp,1000-damage*3.6f,"conditional profession hit bonuses exclude soul lance");
        g=game(REPORTER,2,0);g.kills=3;g.pause();g.abandon();check(g.heroXp()==160+33,"investigation failure XP floors 28 times 1.2");loaded=ActionSave.decode(ActionSave.encode(g));loaded.abandon();check(loaded.heroXp()==193,"loaded result does not repeat XP reward");
        g=game(REPORTER,2,0);g.room=5;g.enemies.clear();tick(g);check(g.won&&g.heroXp()==220,"investigation victory XP");
        // Existing v5 careers retain temporary states and never reinterpret IDs.
        g=game(1,2,0);hit(g,10);g.pause();byte[] v5=edit(ActionSave.encode(g),"version","5");loaded=ActionSave.decode(v5);check(loaded.isSpec(1,2)&&loaded.careerCharge==g.careerCharge,"v5 career migration exact");reject(v5,"career0.0","4");
        g=game(MINER,2,0);byte[] bytes=ActionSave.encode(g);reject(bytes,"careerWard","13");reject(bytes,"careerCharge","1");reject(bytes,"version","7");
        g=game(POLICE,2,0);g.mode=ActionGame.Mode.CAMP;g.bindSoul(2,0);g.start(1);hp=g.hp;g.skill();near(hp-g.hp,g.maxHp()*.04f,"police reduction does not reduce sacrifice");
        System.out.println("PASS eight ordinary careers, sixteen specializations, actual damage/healing/economy/XP, exclusions, unbound play and v5 compatibility");
    }
    private static void combinations()throws Exception{
        int steps=0;
        for(int job=0;job<CareerSystem.CLASSES.length;job++)for(int spec=0;spec<(job==0?1:3);spec++)for(int bg=0;bg<=6;bg++)for(int soul=0;soul<=2;soul++){
            ActionGame g=new ActionGame();g.selectCustom(0);BuildSystem.Profile p=g.profile().copy();p.weapon=(job+spec+bg)%3;p.skill=(bg+spec)%3;p.origin=bg%3;p.trait=spec%3;g.customize(0,p);g.profile().xp=1200;g.chooseCareer(job,spec,bg);g.bindSoul(soul,soul==0?0:(job+bg)%2);g.relicOwned=RelicSystem.ALL;g.equipRelics(1+(bg%8),1+((bg+3)%8));
            for(int root:new int[]{0,4,8}){for(int k=0;k<3;k++)g.learn(root);for(int k=0;k<2;k++)g.learn(root+1);}g.learn(new int[]{2,3,6,7,10,11}[(job+spec+bg)%6]);g.start(980+bg*31+spec);ActionGame.Input in=new ActionGame.Input();in.attack=true;
            for(int t=0;t<360;t++){
                if(g.mode==ActionGame.Mode.RESULT){g.returnCamp();g.start(1+t);}if(g.mode==ActionGame.Mode.REWARD)g.chooseReward(t%3);
                in.mx=(float)Math.sin(t*.04);in.my=(float)Math.cos(t*.03);if(t%87==0)g.dodge(in.mx,in.my);if(t%180==0)g.skill();if(t%270==0)g.soulSkill();if(g.hp<30)g.potion();g.update(1f/60f,in);g.signals.clear();steps++;
                if(t%60==0){g=ActionSave.decode(ActionSave.encode(g));check(g.hp>=0&&g.hp<=g.maxHp()&&g.careerWard<=g.careerWardLimit()&&g.careerCharge<=g.careerChargeLimit(),"combined build roundtrip and bounds");}
            }
        }
        System.out.println("PASS "+steps+" steps across 714 class/spec/background/soul combinations with weapon variation, relic pairs and maxed talents");
    }
    public static void main(String[] args)throws Exception{selection();specializations();backgrounds();synergies();saves();ordinaryCareers();combinations();System.out.println("CAREER CHECKS PASSED: "+count);}
}
