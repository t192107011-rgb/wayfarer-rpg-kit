import io.wayfarer.rpg.core.*;
import static io.wayfarer.rpg.core.CareerSystem.*;
import java.io.*;
import java.util.*;

public final class PactTests {
    private static int count;
    private static void check(boolean v,String why){count++;if(!v)throw new AssertionError(why);}
    private static void near(float a,float b,String why){check(Math.abs(a-b)<.05f,why+": "+a+" vs "+b);}
    private static ActionGame game(int job,int spec){ActionGame g=new ActionGame();g.experience[0]=160;check(g.chooseCareer(job,spec,0,true),"select optional pact");g.start(91);arena(g);return g;}
    private static void arena(ActionGame g){g.enemies.clear();g.blocks.clear();g.shots.clear();g.x=560;g.y=600;g.enemies.add(new ActionGame.Enemy(g.nextId++,0,620,600,1000));g.enemies.get(0).stun=1;g.invulnerable=0;}
    private static void tick(ActionGame g){for(ActionGame.Enemy e:g.enemies)e.stun=1;g.hitStop=0;g.update(1f/60f,new ActionGame.Input());g.signals.clear();}
    private static void hurt(ActionGame g,float damage){g.invulnerable=0;g.shots.add(new ActionGame.Shot(g.x,g.y,0,0,damage,false));tick(g);}
    private static void hit(ActionGame g,float damage){ActionGame.Enemy e=g.enemies.get(0);g.shots.add(new ActionGame.Shot(e.x,e.y,0,0,damage,true));tick(g);}
    private static byte[] edit(byte[] data,String key,String value)throws Exception{Properties p=new Properties();p.load(new ByteArrayInputStream(data));p.setProperty(key,value);ByteArrayOutputStream out=new ByteArrayOutputStream();p.store(out,"");return out.toByteArray();}
    private static void reject(byte[] data,String key,String value)throws Exception{boolean bad=false;try{ActionSave.decode(edit(data,key,value));}catch(IOException e){bad=true;}check(bad,"reject "+key+"="+value);}
    private static void choices()throws Exception{
        ActionGame g=new ActionGame();check(!g.pactEnabled(),"new game opts out");check(!g.chooseCareer(POLICE,0,0,true),"pact level gate without specialization");g.experience[0]=160;check(!g.chooseCareer(3,1,0,true),"unavailable career refused");check(g.chooseCareer(POLICE,0,0,true),"level-three optional pact without specialization");g.selectHero(1);check(!g.pactEnabled(),"independent preset pact");g.selectHero(0);check(g.pactEnabled(),"original choice retained");g.chooseCareer(DOCTOR,0,0);check(!g.pactEnabled(),"changing career does not silently accept another cost");
        g.chooseCareer(DOCTOR,1,4,true);g.start(1);check(!g.chooseCareer(DOCTOR,1,4,false),"cannot remove debt during run");g.pause();check(!g.professionPact(),"cannot cast paused");g.abandon();g.returnCamp();check(g.chooseCareer(DOCTOR,1,4,false)&&!g.pactEnabled(),"explicit camp opt-out");
        for(int job=POLICE;job<=REPORTER;job++)check(!ProfessionPacts.TEMPERAMENTS[job].isEmpty()&&!ProfessionPacts.LINES[job].isEmpty()&&ProfessionPacts.DETAILS[job].contains("代价"),"each ordinary career has character and explicit price");
    }
    private static void effects()throws Exception{
        ActionGame g=game(POLICE,1);float hp=g.hp;g.enemies.get(0).stun=0;check(g.professionPact(),"police cast");near(hp-g.hp,g.maxHp()*.12f,"police pays life");near(g.enemies.get(0).stun,1,"police restrains normal enemy");near(g.speed(),205*1.05f*.65f,"police accepts slow movement");hp=g.hp;hurt(g,20);near(hp-g.hp,20*.95f*.5f,"police actual damage halved after career reduction");check(!g.professionPact(),"cooldown prevents double charge");
        g=game(SOLDIER,0);hp=g.hp;check(g.professionPact(),"soldier cast");near(hp-g.hp,g.maxHp()*.15f,"soldier life cost");near(g.attackPeriod(),.48f*.55f,"soldier rapid fire");hit(g,20);near(g.enemies.get(0).hp,974,"soldier empowered hit");hp=g.hp;hurt(g,20);near(hp-g.hp,28,"soldier vulnerability");
        g=game(DOCTOR,1);check(!g.professionPact()&&g.pactCd==0,"full-health doctor does not waste cooldown");g.hp=10;check(g.professionPact(),"doctor emergency treatment");near(g.hp,10+g.maxHp()*.55f,"doctor initial heal before treatment lock");hp=g.hp;int bottles=g.potions;g.potion();near(g.hp,hp,"doctor cannot heal with potion while indebted");check(g.potions==bottles,"blocked potion not consumed");g.skill();near(g.hp,hp,"first-aid passive also obeys healing lock");hurt(g,20);near(hp-g.hp,25,"doctor vulnerability");for(int i=0;i<500;i++)tick(g);near(g.pactTime,0,"doctor penalty expires");check(g.healingFactor()==1,"healing returns after debt");
        g=game(MECHANIC,1);g.skillCd=5;hp=g.hp;check(g.professionPact(),"mechanic cast");near(g.skillCd,0,"mechanic resets active skill");near(hp-g.hp,g.maxHp()*.1f,"mechanic upfront cost");hp=g.hp;g.skill();near(hp-g.hp,g.maxHp()*.04f,"mechanic per-active cost");near(g.skillCd,6.2f*.45f,"mechanic overclock cooldown");g.skillCd=0;g.hp=g.maxHp()*.04f;hp=g.hp;g.skill();near(g.hp,hp,"mechanic refuses lethal cost");near(g.skillCd,0,"refused skill no cooldown");
        g=game(MINER,2);float damage=g.damage();hp=g.hp;check(g.professionPact(),"miner cast");near(g.enemies.get(0).hp,1000-damage*3.2f,"miner area damage");near(g.soulEnergy,0,"miner blast does not charge soul");near(g.pactWard,35,"miner temporary equipment ward");near(g.careerWard,12,"miner permanent room ward independent");near(hp-g.hp,g.maxHp()*.18f,"miner upfront cost");near(g.speed(),205*.7f,"miner burden");hurt(g,10);near(g.pactWard,25,"temporary ward absorbs first");near(g.careerWard,12,"entry ward remains");
        g=game(BOXER,0);check(g.professionPact(),"boxer cast");hit(g,20);near(g.enemies.get(0).hp,964,"boxer empowered melee-equipped hit");near(g.attackPeriod(),.48f*.94f*.7f,"boxer fast rhythm");hp=g.hp;hurt(g,20);near(hp-g.hp,30,"boxer risk");g=new ActionGame();g.selectHero(1);g.experience[1]=160;g.chooseCareer(BOXER,1,0,true);g.start(1);check(!g.professionPact()&&g.pactCd==0,"carbine cannot cast melee pact");
        g=game(MERCHANT,0);g.bank=500;g.loot=29;check(!g.professionPact()&&g.loot==29&&g.bank==500,"merchant cannot spend shared bank");g.loot=30;g.hp=20;check(g.professionPact(),"merchant cast");check(g.loot==0&&g.bank==500,"merchant exact local currency cost");near(g.hp,20+g.maxHp()*.3f,"merchant initial healing");hit(g,20);near(g.enemies.get(0).hp,970,"merchant power bought");hp=g.hp;hurt(g,20);near(hp-g.hp,25,"merchant vulnerability");
        g=game(REPORTER,0);ActionGame.Enemy e=g.enemies.get(0);e.state=1;e.timer=.1f;e.stun=0;ActionGame.Enemy boss=new ActionGame.Enemy(g.nextId++,3,750,500,1000);boss.state=1;boss.timer=.8f;g.enemies.add(boss);check(g.professionPact(),"reporter cast");check(e.state==0&&boss.state==1,"reporter interrupts normal but not boss attack");near(e.stun,.8f,"reporter normal stagger");near(boss.stun,.15f,"boss resists interruption");hit(g,20);near(e.hp,968,"reporter revealed weak point damage");hp=g.hp;hurt(g,20);near(hp-g.hp,27,"reporter exposure penalty");
        System.out.println("PASS eight occupational forbidden arts: real benefits, costs, conditions and expiry");
    }
    private static void synergies()throws Exception{
        ActionGame g=new ActionGame();g.experience[0]=160;g.chooseCareer(MINER,2,1,true);g.bindSoul(2,1);g.relicOwned=RelicSystem.ALL;g.equipRelics(3,6);g.start(1);arena(g);g.dodge(0,-1);g.rollTime=0;float hp=g.hp;check(g.professionPact(),"bound miner pact");near(g.soulEnergy,40,"pact blast does not self-charge existing soul");near(g.relicEcho,3,"pact blast cannot consume copper bell");near(hp-g.hp,g.maxHp()*.18f,"soul sacrifice relic does not discount occupational price");near(g.corruption,0,"occupational cost does not silently create soul corruption");
        g=new ActionGame();g.experience[0]=160;g.chooseCareer(DOCTOR,2,4,true);g.bindSoul(2,1);g.relicOwned=RelicSystem.ALL;g.equipRelics(4,0);g.start(1);arena(g);g.hp=10;g.corruption=40;g.soulQuiet=3;float expected=10+g.maxHp()*.55f*.88f*1.1f*1.12f;g.professionPact();near(g.hp,expected,"initial forbidden treatment obeys existing healing factors");g.soulSkill();hp=g.hp;hit(g,20);near(g.hp,hp,"blood pact lifesteal cannot bypass treatment lock");check(g.soulBuff>0&&g.pactTime>0,"soul and occupational forbidden states coexist");
        System.out.println("PASS pact/soul/relic combinations, healing lock, no hidden soul cost or amplification loop");
    }
    private static void persistence()throws Exception{
        for(int job=POLICE;job<=REPORTER;job++){
            ActionGame g=game(job,0);if(job==DOCTOR)g.hp=10;if(job==MERCHANT)g.loot=30;check(g.professionPact(),"cast for persistence");if(job==MINER)hurt(g,10);g.pause();float time=g.pactTime,cd=g.pactCd,hp=g.hp,ward=g.pactWard;for(int i=0;i<30;i++)tick(g);near(g.pactTime,time,"paused debt freezes");near(g.pactCd,cd,"paused cooldown freezes");
            ActionGame a=ActionSave.decode(ActionSave.encode(g));near(a.hp,hp,"restore never pays or heals again");near(a.pactWard,ward,"restore never refills equipment");near(a.pactTime,time,"remaining penalty restored");a.room=2;a.generateRoom();near(a.pactTime,time,"room transition cannot erase debt");near(a.pactCd,cd,"room transition cannot refresh cooldown");near(a.pactWard,ward,"room transition cannot refill temporary shield");
            a.pause();a.abandon();a.returnCamp();check(a.pactTime==0&&a.pactCd==0&&a.pactWard==0&&a.pactEnabled(),"camp ends run state but retains choice");a.start(12);check(a.pactCd==0&&a.pactTime==0,"next run starts clean");
            if(ProfessionPacts.COST[job]>0){a.hp=a.maxHp()*ProfessionPacts.COST[job];float before=a.hp;check(!a.professionPact()&&a.hp==before&&a.pactCd==0,"unaffordable life cost refused atomically");}
        }
        ActionGame g=game(MINER,2);g.professionPact();byte[] data=ActionSave.encode(g);reject(data,"pactWard","36");reject(data,"pactTime","9");reject(data,"pactCd","33");reject(data,"pactTime","NaN");reject(data,"pactCd","0");reject(data,"careerPact0","false");reject(data,"careerPact1","true");
        g=game(POLICE,1);g.professionPact();reject(ActionSave.encode(g),"pactWard","1");g=new ActionGame();g.experience[0]=160;g.chooseCareer(POLICE,1,0,true);reject(ActionSave.encode(g),"pactCd","1");
        g=new ActionGame();g.experience[0]=160;g.chooseCareer(MECHANIC,1,4);g.bindSoul(2,1);g.start(1);g.soulSkill();data=edit(ActionSave.encode(g),"version","6");ActionGame old=ActionSave.decode(data);check(old.isSpec(MECHANIC,1)&&old.background()==4&&old.soulBuff>0&&!old.pactEnabled()&&old.pactCd==0,"v6 migration preserves ordinary career and soul, defaults pact off");
        System.out.println("PASS opt-in, costs never replay, penalty/cooldown across rooms, pause, v6 migration and corrupt-state rejection");
    }
    private static void combinations()throws Exception{
        int steps=0;
        for(int job=POLICE;job<=REPORTER;job++)for(int weapon=0;weapon<3;weapon++)for(int soul=0;soul<3;soul++)for(int spec=1;spec<=2;spec++){
            ActionGame g=new ActionGame();g.selectCustom(0);BuildSystem.Profile p=g.profile().copy();p.weapon=weapon;p.skill=(job+weapon)%3;p.origin=job%3;p.trait=weapon;g.customize(0,p);g.profile().xp=1200;g.chooseCareer(job,spec,job%7,true);g.bindSoul(soul,soul==0?0:spec-1);g.relicOwned=RelicSystem.ALL;g.equipRelics(4,8);
            for(int root:new int[]{0,4,8}){for(int i=0;i<3;i++)g.learn(root);for(int i=0;i<2;i++)g.learn(root+1);}g.learn(new int[]{2,3,6,7,10,11}[(job+weapon)%6]);g.start(job*100+weapon);g.loot=60;g.hp=g.maxHp()*.7f;ActionGame.Input in=new ActionGame.Input();in.attack=true;
            for(int t=0;t<720;t++){
                if(g.mode==ActionGame.Mode.RESULT){g.returnCamp();g.start(t+1);g.loot=60;}if(g.mode==ActionGame.Mode.REWARD)g.chooseReward(t%3);
                in.mx=(float)Math.sin(t*.04);in.my=(float)Math.cos(t*.03);if(t%90==0)g.professionPact();if(t%120==0)g.skill();if(t%190==0)g.soulSkill();if(t%75==0)g.dodge(in.mx,in.my);if(g.hp<30)g.potion();g.update(1f/60f,in);g.signals.clear();steps++;
                if(t%60==0){g=ActionSave.decode(ActionSave.encode(g));check(g.hp>=0&&g.hp<=g.maxHp()&&g.pactWard<=35&&g.pactTime<=ProfessionPacts.DURATION[job],"combined pact snapshot bounds");}
            }
        }
        System.out.println("PASS "+steps+" steps across 144 pact/weapon/soul/specialization builds with talents and relics");
    }
    public static void main(String[] args)throws Exception{choices();effects();synergies();persistence();combinations();System.out.println("PACT CHECKS PASSED: "+count);}
}
