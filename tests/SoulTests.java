import io.wayfarer.rpg.core.*;
import java.io.*;
import java.util.*;

public final class SoulTests {
    private static int count;
    private static void check(boolean ok,String why){count++;if(!ok)throw new AssertionError(why);}
    private static void near(float a,float b,String why){check(Math.abs(a-b)<.035f,why+": "+a+" != "+b);}
    private static ActionGame game(int kind,int art,int style){ActionGame g=new ActionGame();g.selectCustom(0);BuildSystem.Profile p=g.profile().copy();p.weapon=style;p.skill=style;p.origin=2;p.trait=1;g.customize(0,p);g.bindSoul(kind,art);g.start(91);g.blocks.clear();g.enemies.clear();g.x=560;g.y=600;g.faceX=1;g.faceY=0;g.invulnerable=0;return g;}
    private static ActionGame.Enemy mob(ActionGame g,float x,float y){ActionGame.Enemy e=new ActionGame.Enemy(g.nextId++,0,x,y,1000);e.stun=1;g.enemies.add(e);return e;}
    private static void step(ActionGame g,int ticks,boolean attack){ActionGame.Input in=new ActionGame.Input();in.attack=attack;for(int i=0;i<ticks;i++){for(ActionGame.Enemy e:g.enemies)e.stun=1;g.update(1f/60f,in);g.signals.clear();}}
    private static void bindings()throws Exception{
        ActionGame g=new ActionGame();g.experience[0]=160;g.bank=200;g.learn(0);check(g.bindSoul(1,1),"bind preset soul");g.selectHero(1);check(g.soulKind()==0,"preset souls independent");g.selectCustom(0);g.bindSoul(2,1);g.selectHero(0);check(g.soulKind()==1&&g.soulArt()==1&&g.heroXp()==160&&g.bank==200&&g.rank(0)==1,"binding preserves progression");
        ActionGame a=ActionSave.decode(ActionSave.encode(g));a.selectCustom(0);check(a.soulKind()==2&&a.soulArt()==1,"custom binding persisted independently");
        g.start(90);check(!g.bindSoul(2,0)&&g.soulKind()==1,"cannot swap souls midrun");near(g.soulEnergy,40,"initial energy");g.pause();g.abandon();g.returnCamp();near(g.soulEnergy,0,"camp clears runtime but preserves binding");check(g.soulKind()==1,"binding persists between runs");
        for(int[] pair:new int[][]{{0,1},{3,0},{-1,0},{1,2}}){boolean bad=false;try{SoulSystem.validate(pair[0],pair[1]);}catch(IllegalArgumentException e){bad=true;}check(bad,"invalid binding rejected");}
        System.out.println("PASS soul binding independence, camp-only swaps, progression preservation and fresh-run resources");
    }
    private static void variants()throws Exception{
        ActionGame g=game(1,0,0);ActionGame.Enemy e=mob(g,745,600);g.shots.add(new ActionGame.Shot(600,600,0,1,10,false));g.skill();check(e.hp<1000&&g.shots.isEmpty(),"holy shock reaches farther and purifies shots");near(g.soulWard,8,"radiant ward granted");near(g.skillCd,7.5f,"radiant cooldown tradeoff");
        g=game(1,0,2);mob(g,650,600);g.skill();near(g.invulnerable,.85f,"radiant ring longer invulnerability");near(g.soulWard,14,"radiant ring additional ward");
        g=game(1,0,1);e=mob(g,610,600);ActionGame.Enemy e2=mob(g,660,600),e3=mob(g,710,600),e4=mob(g,760,600);g.skill();check(g.shots.size()==3&&g.shots.get(1).pierce==2,"prismatic volley three piercing arrows");ActionGame.Shot arrow=g.shots.get(1);g.shots.clear();g.shots.add(arrow);float power=arrow.damage;step(g,1,false);near(e.hp,1000-power,"first pierce hit");check(arrow.hitIds.contains(e.id),"records hit ID");
        g=ActionSave.decode(ActionSave.encode(g));float first=g.enemies.get(0).hp;step(g,2,false);near(g.enemies.get(0).hp,first,"save restore cannot repeat hit on same target");step(g,20,false);near(g.enemies.get(1).hp,1000-power,"second pierced target");near(g.enemies.get(2).hp,1000-power,"third pierced target");near(g.enemies.get(3).hp,1000,"three-target cap");
        g=game(2,0,0);e=mob(g,750,600);float hp=g.hp;g.skill();check(e.x<750&&e.hex==3,"abyss pulls and curses");near(hp-g.hp,g.maxHp()*.04f,"forbidden base skill life cost");near(g.corruption,18,"base skill debt");near(g.skillCd,6.4f,"forbidden cooldown reduced");hp=g.hp;g.skill();near(hp-g.hp,g.maxHp()*.02f,"forbidden follow-up pays its explicit additional life cost");hp=g.hp;g.skill();near(g.hp,hp,"third cast has no cost after follow-up is spent");
        g=game(2,0,1);mob(g,710,600);g.skill();check(g.shots.size()==7&&g.shots.stream().allMatch(s->s.effect==2),"curse volley seven tagged arrows");step(g,20,false);check(g.enemies.get(0).hex>0,"curse arrow actually applies curse");
        g=game(2,0,2);mob(g,680,600);g.hp=.5f;g.skill();near(g.hp,.5f,"self-cost neither kills nor heals critically low player");near(g.invulnerable,.35f,"forbidden ring shorter protection");
        // Confirm curse multiplies damage rather than being cosmetic.
        g=game(2,0,0);e=mob(g,650,600);e.hex=3;g.shots.add(new ActionGame.Shot(640,600,10,0,20,true));step(g,1,false);near(e.hp,977,"curse adds fifteen percent incoming enemy damage");
        System.out.println("PASS six active-skill mutations, piercing collision history, curse, pull, self-cost and defense tradeoffs");
    }
    private static void arts()throws Exception{
        ActionGame g=game(1,0,0);mob(g,680,600);g.soulEnergy=39;g.soulSkill();check(g.soulFields.isEmpty()&&g.soulCd==0,"insufficient energy blocks art");g.soulEnergy=40;g.hp=20;g.soulSkill();near(g.soulEnergy,0,"art consumes forty energy");near(g.soulWard,30,"domain grants ward");float base=g.damage(),max=g.maxHp();step(g,305,false);near(g.enemies.get(0).hp,1000-base*.35f*5,"domain exactly five pulses");near(g.hp,20+max*.02f*1.15f*5,"domain heals only over its five pulses");check(g.soulFields.isEmpty()&&g.soulEnergy==0,"domain expires without recharging itself");
        g=game(1,1,1);ActionGame.Enemy e=mob(g,760,600);g.soulSkill();near(e.hp,1000-g.damage()*3.6f,"chosen lance art damages");
        g=game(1,1,0);e=mob(g,760,600);g.blocks.add(new ActionGame.Block(640,550,30,100,0));g.soulSkill();near(e.hp,1000,"lance respects cover");
        g=game(2,0,0);e=mob(g,800,600);float hp=g.hp;g.soulSkill();near(hp-g.hp,g.maxHp()*.08f,"forbidden art life cost");near(g.corruption,30,"forbidden art debt");step(g,31,false);check(e.x<800&&e.hex>0&&e.hp<1000,"rift pulls curses and damages");near(g.soulEnergy,0,"rift cannot self-charge");ActionGame restored=ActionSave.decode(ActionSave.encode(g));step(g,100,false);step(restored,100,false);near(restored.enemies.get(0).hp,g.enemies.get(0).hp,"saved rift continues same pulse schedule");near(restored.soulFields.get(0).life,g.soulFields.get(0).life,"field lifetime continuity");
        g=game(2,1,0);e=mob(g,610,600);g.hp=50;float original=g.damage(),interval=g.attackPeriod();g.soulSkill();near(g.soulBuff,6,"pact six second buff");near(g.damage(),original*1.25f,"pact damage bonus");near(g.attackPeriod(),interval*.8f,"pact attack speed");hp=g.hp;step(g,10,true);check(g.hp>hp&&g.soulEnergy>0,"pact actual attacks steal life and charge");
        float cd=g.soulCd;g.soulEnergy=100;g.soulSkill();check(g.soulCd==cd&&g.soulEnergy==100,"cooldown blocks repeat art even with energy");g.pause();float buff=g.soulBuff,corrupt=g.corruption;step(g,120,false);near(g.soulCd,cd,"pause freezes soul cooldown");near(g.soulBuff,buff,"pause freezes pact");near(g.corruption,corrupt,"pause freezes corruption");
        System.out.println("PASS four exclusive arts, resource and cooldown gates, field lifetime, pause, lifesteal and no self-refill");
    }
    private static void costsAndSynergies()throws Exception{
        ActionGame g=game(1,0,0);mob(g,300,300);g.hp=10;g.potion();near(g.hp,10+g.maxHp()*.45f*1.15f,"radiance amplifies healing");
        g=game(2,0,0);ActionGame.Enemy e=mob(g,590,600);g.corruption=100;g.soulQuiet=3;g.hp=10;g.potion();near(g.hp,10+g.maxHp()*.45f*.7f,"corruption reduces potion healing");float hp=g.hp;e.stun=0;e.state=1;e.timer=.001f;g.update(1f/60f,new ActionGame.Input());near(hp-g.hp,14*1.4f*1.12f,"corruption incoming damage composes with trait");step(g,240,false);check(g.corruption<100&&g.corruption>95,"corruption decays only after quiet delay");
        g=new ActionGame();g.experience[0]=1000;g.bindSoul(1,0);g.learn(8);g.learn(9);g.learn(9);near(g.maxHp(),110+12*3+10,"light wick talent resonance");g.start(7);g.skill();near(g.soulWard,16,"warmth grants additional radiant ward");
        g=new ActionGame();g.experience[1]=1000;g.selectHero(1);g.bindSoul(2,0);g.learn(0);g.learn(1);g.learn(1);g.start(7);g.blocks.clear();g.enemies.clear();g.x=560;g.y=600;mob(g,710,600);g.combo=1;g.comboWindow=.5f;step(g,1,true);near(g.shots.get(0).damage,g.damage()*1.71f,"forbidden third-beat resonance");
        g=game(1,0,0);e=mob(g,590,600);g.soulEnergy=0;g.soulWard=20;e.stun=0;e.state=1;e.timer=.001f;g.update(1f/60f,new ActionGame.Input());check(g.soulEnergy>0&&g.soulWard<20,"radiant ward damage absorption charges soul");
        System.out.println("PASS corruption damage/healing balance, decay, radiant shield charge and talent resonances");
    }
    private static byte[] edit(byte[] bytes,String key,String value)throws Exception{Properties p=new Properties();p.load(new ByteArrayInputStream(bytes));p.setProperty(key,value);ByteArrayOutputStream out=new ByteArrayOutputStream();p.store(out,"");return out.toByteArray();}
    private static void reject(byte[] bytes,String k,String v)throws Exception{boolean bad=false;try{ActionSave.decode(edit(bytes,k,v));}catch(IOException e){bad=true;}check(bad,"reject "+k);}
    private static void saves()throws Exception{
        ActionGame g=new ActionGame();g.selectCustom(1);g.profile().xp=320;g.learn(0);g.bank=83;g.start(7);byte[] bytes=ActionSave.encode(g);
        ActionGame old=ActionSave.decode(edit(bytes,"version","2"));check(old.custom==1&&old.profile().xp==320&&old.rank(0)==1&&old.bank==83&&old.soulKind()==0&&old.soulEnergy==0,"v2 migration preserves builds and does not invent a soul");
        g=game(2,1,0);mob(g,610,600);g.skill();g.soulSkill();g.pause();bytes=ActionSave.encode(g);ActionGame a=ActionSave.decode(bytes);near(a.soulBuff,g.soulBuff,"pact persisted");near(a.corruption,g.corruption,"debt persisted");near(a.soulCd,g.soulCd,"exclusive cooldown persisted");near(a.enemies.get(0).hex,g.enemies.get(0).hex,"curse persisted");
        reject(bytes,"soulEnergy","101");reject(bytes,"soulCd","19");reject(bytes,"corruption","NaN");reject(bytes,"profile0.soul","3");reject(bytes,"profile0.art","2");reject(bytes,"soulFields","20");reject(bytes,"soulWard","1");reject(bytes,"hex0","4");reject(bytes,"soulBuff","9");
        g=game(1,0,1);mob(g,710,600);g.skill();bytes=ActionSave.encode(g);reject(bytes,"shotPierce0","3");reject(bytes,"shotHits0","1,1");reject(bytes,"shotHits0","9999");reject(bytes,"shotEffect0","2");
        g=game(1,0,0);mob(g,700,600);g.soulSkill();bytes=ActionSave.encode(g);reject(bytes,"field0","2,560,600,4,0.5,22");reject(bytes,"field0","1,560,600,5,2,22");
        System.out.println("PASS v2 migration, soul snapshots, pulse and curse continuity, malformed resources and pierce histories rejected");
    }
    private static void stress()throws Exception{
        for(int kind=1;kind<=2;kind++)for(int art=0;art<2;art++)for(int style=0;style<3;style++){
            ActionGame g=game(kind,art,style);g.mode=ActionGame.Mode.CAMP;g.profile().xp=1200;
            for(int root:new int[]{0,4,8}){for(int rank=0;rank<3;rank++)check(g.learn(root),"max root");for(int rank=0;rank<2;rank++)check(g.learn(root+1),"max middle");}
            check(g.learn(new int[]{2,3,6,7,10,11}[style+art*3]),"select capstone");g.start(91+style);ActionGame.Input in=new ActionGame.Input();in.attack=true;
            for(int t=0;t<1800;t++){
                if(g.mode==ActionGame.Mode.RESULT){g.returnCamp();g.start(100+t);}
                if(g.mode==ActionGame.Mode.REWARD)g.chooseReward(t%3);
                in.mx=(float)Math.cos(t*.023);in.my=(float)Math.sin(t*.017);
                if(t%67==0)g.dodge(in.mx,in.my);if(t%300==0)g.skill();if(t%780==0)g.soulSkill();if(g.hp<30)g.potion();g.update(1f/60f,in);g.signals.clear();
                if(t%60==0){g=ActionSave.decode(ActionSave.encode(g));check(g.hp>=0&&g.soulEnergy>=0&&g.soulEnergy<=100&&g.corruption<=100,"soul stress state bounded");}
            }
        }
        System.out.println("PASS 21600 simulation steps across both souls, four arts, three base skills and maxed talent builds with periodic restore");
    }
    public static void main(String[] args)throws Exception{bindings();variants();arts();costsAndSynergies();saves();stress();System.out.println("SOUL CHECKS PASSED: "+count);}
}
