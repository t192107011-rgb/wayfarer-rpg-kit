import io.wayfarer.rpg.core.*;
import java.io.*;
import java.util.*;

public final class BuildTests {
    private static int count;
    private static void check(boolean v,String message){count++;if(!v)throw new AssertionError(message);}
    private static void near(float a,float b,String m){check(Math.abs(a-b)<.02f,m+": "+a+" vs "+b);}
    private static ActionGame.Enemy mob(ActionGame g,float x,float y){ActionGame.Enemy e=new ActionGame.Enemy(g.nextId++,0,x,y,500);e.stun=1;g.enemies.add(e);return e;}
    private static void arena(ActionGame g){g.start(123);g.blocks.clear();g.enemies.clear();g.x=560;g.y=600;g.invulnerable=0;g.hitStop=0;g.threat=1;g.encounterHpFloor=0;g.encounterDamageFloor=0;}
    private static void tick(ActionGame g,boolean attack,int n){ActionGame.Input i=new ActionGame.Input();i.attack=attack;for(int x=0;x<n;x++)g.update(1f/60f,i);}
    private static void points(ActionGame g){if(g.custom<0)g.experience[g.hero]=1200;else g.profile().xp=1200;g.hp=g.maxHp();}
    private static void learn(ActionGame g,int id,int times){for(int i=0;i<times;i++)check(g.learn(id),"learn node "+id);}
    private static ActionGame ultimate(int id){ActionGame g=new ActionGame();points(g);int root=id/4*4;learn(g,root,1);learn(g,root+1,2);learn(g,id,1);return g;}
    private static void profiles()throws Exception{
        ActionGame g=new ActionGame();g.bank=85;g.experience[0]=180;g.selectCustom(0);g.profile().xp=240;
        BuildSystem.Profile draft=g.profile().copy();draft.name="暮灯·艾文";draft.weapon=1;draft.skill=2;draft.skin=2;draft.hair=1;draft.color=4;draft.xp=1000000;
        g.customize(0,draft);check(g.hero==1&&g.skillStyle()==2&&g.profile().xp==240,"custom editing preserves existing XP and equips cross-class skill");
        draft.name="cancelled";check(g.heroName().equals("暮灯·艾文"),"committed profile detached from editor draft");learn(g,8,1);
        g.selectCustom(1);check(g.heroXp()==0&&g.rank(8)==0,"custom slots keep independent progression");g.selectHero(0);check(g.heroXp()==180&&g.bank==85&&g.rank(8)==0,"legacy roster preserved");
        g.selectCustom(0);ActionGame restored=ActionSave.decode(ActionSave.encode(g));check(restored.heroName().equals("暮灯·艾文")&&restored.profile().skin==2&&restored.profile().color==4&&restored.rank(8)==1,"name cosmetics and talents persist");
        arena(g);boolean rejected=false;try{g.customize(0,draft);}catch(IllegalArgumentException e){rejected=true;}check(rejected,"combat forbids editing");int selected=g.custom;g.selectHero(2);check(g.custom==selected,"combat forbids roster switching");
        for(String name:new String[]{""," ","abcdefghijkl","a\nb","a\u202Eb","\uD800"}){boolean bad=false;try{BuildSystem.cleanName(name);}catch(IllegalArgumentException e){bad=true;}check(bad,"reject invalid name");}
        check(BuildSystem.cleanName("  星灯  ").equals("星灯"),"trim name");
        // Every weapon / active skill / origin / trait combination is valid and persists mid-skill.
        for(int w=0;w<3;w++)for(int s=0;s<3;s++)for(int o=0;o<3;o++)for(int t=0;t<3;t++){
            g=new ActionGame();draft=g.profiles[0].copy();draft.weapon=w;draft.skill=s;draft.origin=o;draft.trait=t;g.customize(0,draft);float max=g.maxHp();arena(g);mob(g,600,600);g.skill();
            check(g.hero==w&&g.skillStyle()==s&&g.hp==max,"81 combinations configure combat");if(s==1)check(g.shots.size()==5,"ranged active independent of weapon");else check(g.enemies.get(0).hp<500,"radial active independent of weapon");
            restored=ActionSave.decode(ActionSave.encode(g));near(restored.skillCd,g.skillCd,"custom cooldown persisted");
        }
        System.out.println("PASS 81 custom combat combinations, draft isolation, cosmetics, independent XP and roster switching");
    }
    private static void treeRules()throws Exception{
        ActionGame g=new ActionGame();check(g.availablePoints()==2,"starts with two points");check(!g.learn(1),"level/parent lock");learn(g,0,2);check(!g.learn(8),"cannot overspend");g.resetTalents();check(g.availablePoints()==2&&g.rank(0)==0,"free refund exact");
        points(g);learn(g,0,1);learn(g,1,1);check(!g.learn(2),"capstone requires rank2 parent");learn(g,1,1);learn(g,2,1);learn(g,4,1);learn(g,5,2);check(g.learn(7)&&!g.learn(3),"different branches can coexist; same branch remains exclusive");
        int before=g.availablePoints();g.start(35);check(!g.learn(8)&&g.availablePoints()==before,"no learning in run");g.resetTalents();check(g.rank(2)==1,"no midrun respec");g.pause();g.abandon();g.returnCamp();g.resetTalents();check(g.availablePoints()==17,"full refund capped points");
        for(int id:new int[]{2,3,6,7,10,11}){g=ultimate(id);ActionSave.decode(ActionSave.encode(g));}
        System.out.println("PASS point budget, level/parent gates, one capstone per branch and camp-only respec");
    }
    private static void effects()throws Exception{
        ActionGame g=new ActionGame();learn(g,0,1);near(g.damage(),24,"tempered damage");g.resetTalents();learn(g,8,1);near(g.maxHp(),118,"wick health");g.resetTalents();learn(g,4,1);check(g.speed()>205&&g.dodgePeriod()<1.15,"lightstep speed and dodge");
        g=ultimate(2);arena(g);ActionGame.Enemy e=mob(g,680,600);g.combo=1;g.comboWindow=.7f;tick(g,true,1);check(e.hp<500,"third attack creates shockwave outside sword range");
        g=ultimate(3);arena(g);float normal=g.damage();g.hp=g.maxHp()*.3f;near(g.damage(),normal*1.55f,"low health capstone boosts damage");
        g=ultimate(6);arena(g);e=mob(g,610,600);g.dodge(0,-1);check(e.hp<500&&e.stun>=.45,"dodge pulse has real damage and stun");
        g=ultimate(7);arena(g);mob(g,300,300);g.skillCd=6;g.dodge(0,-1);near(g.skillCd,3.5f,"dodge reduces skill cooldown");g.dodge(0,-1);near(g.skillCd,3.5f,"failed dodge grants no free cooldown reduction");
        g=new ActionGame();points(g);learn(g,4,1);learn(g,5,2);arena(g);e=mob(g,610,600);g.dodge(0,-1);g.rollTime=0;g.x=560;g.y=600;float damage=g.damage();tick(g,true,10);near(500-e.hp,damage*1.3f,"dodge primes one empowered attack");check(g.dodgeCharge==0,"charge consumed once");
        g=ultimate(10);arena(g);mob(g,300,300);g.hp=30;float expected=30+g.maxHp()*.08f;g.skill();near(g.hp,expected,"longlight healing");near(g.shield,12,"warmth shield");near(g.skillCd,6.2f,"warmth cooldown");g.skill();near(g.hp,expected,"cooldown blocks duplicate heal");
        g=ultimate(11);arena(g);e=mob(g,590,600);g.skill();g.hp=g.maxHp();g.hitStop=0;e.stun=0;e.hp=500;e.state=1;e.timer=.001f;float hp=g.hp;tick(g,false,1);near(g.hp,hp,"shield absorbs melee");near(g.shield,10,"shield is consumed");near(e.hp,490.9f,"mirror reflects 65 percent of absorbed damage");
        ActionGame a=ActionSave.decode(ActionSave.encode(g));near(a.shield,g.shield,"remaining shield persists");
        g=new ActionGame();points(g);g.selectHero(1);g.experience[1]=1200;learn(g,0,1);learn(g,1,2);arena(g);e=mob(g,720,600);g.combo=1;g.comboWindow=.7f;tick(g,true,1);near(g.shots.get(0).damage,g.damage()*1.59f,"archer benefits from third-beat talent");
        // Each custom character gets only their own earned XP on settlement.
        g=new ActionGame();g.selectCustom(2);arena(g);mob(g,300,300);g.kills=3;g.pause();g.abandon();check(g.profiles[2].xp==28&&g.profiles[0].xp==0&&g.experience[2]==0,"settlement awards active profile only");
        System.out.println("PASS tangible damage, combo, dodge charge, pulse, cooldown recovery, healing, shield and reflect effects");
    }
    private static byte[] altered(byte[] data,String key,String value)throws Exception{Properties p=new Properties();p.load(new ByteArrayInputStream(data));p.setProperty(key,value);ByteArrayOutputStream out=new ByteArrayOutputStream();p.store(out,"");return out.toByteArray();}
    private static void reject(byte[] data,String key,String value)throws Exception{boolean bad=false;try{ActionSave.decode(altered(data,key,value));}catch(IOException e){bad=true;}check(bad,"malformed build rejected: "+key);}
    private static void persistence()throws Exception{
        ActionGame g=new ActionGame();g.experience[1]=320;g.bank=170;g.power=2;g.selectHero(1);g.start(557);g.hp-=10;
        Properties p=new Properties();p.load(new ByteArrayInputStream(ActionSave.encode(g)));p.setProperty("version","1");p.keySet().removeIf(k->k.toString().startsWith("profile")||k.toString().startsWith("talent")||k.equals("custom")||k.equals("shield")||k.equals("dodgeCharge"));ByteArrayOutputStream out=new ByteArrayOutputStream();p.store(out,"v1 layout");
        ActionGame a=ActionSave.decode(out.toByteArray());check(a.hero==1&&a.bank==170&&a.heroXp()==320&&a.hp==g.hp&&a.power==2&&a.rngState==g.rngState,"v1 migration keeps old run and progression");check(a.custom==-1&&a.availablePoints()==6,"v1 migration grants earned unspent points");ActionSave.decode(ActionSave.encode(a));
        g=ultimate(11);byte[] data=ActionSave.encode(g);reject(data,"custom","5");reject(data,"profile0.name","bad\nname");reject(data,"profile0.data","0,0,0,0,99,0,0,0");reject(data,"talent0.0","9");reject(data,"talent0.2","1");reject(data,"talent0.8","0");reject(data,"shield","NaN");reject(data,"dodgeCharge","2");
        g=new ActionGame();data=ActionSave.encode(g);reject(data,"talent0.0","3");reject(data,"shield","3");g.selectCustom(0);data=ActionSave.encode(g);reject(data,"hero","2");
        // Armed dodge charge survives restart but a partially executed attack cannot re-arm it.
        g=new ActionGame();points(g);learn(g,4,1);learn(g,5,1);arena(g);mob(g,610,600);g.dodge(0,-1);a=ActionSave.decode(ActionSave.encode(g));near(a.dodgeCharge,3,"armed charge preserved");g.rollTime=0;tick(g,true,1);a=ActionSave.decode(ActionSave.encode(g));check(a.dodgeCharge==0&&a.attackAnim==0,"restoring cannot refund consumed charge or replay swing");
        System.out.println("PASS v1-to-v2 migration, new snapshots, corrupt-tree rejection and no duplicate charge on restore");
    }
    private static void stress()throws Exception{
        Random r=new Random(19);ActionGame.Input in=new ActionGame.Input();
        for(int cap:new int[]{2,3,6,7,10,11}){
            ActionGame g=new ActionGame();g.selectCustom(cap%3);g.profile().xp=1200;int branch=cap/4*4;learn(g,branch,3);learn(g,branch+1,2);learn(g,cap,1);
            for(int root:new int[]{0,4,8})if(root!=branch){learn(g,root,3);learn(g,root+1,2);}
            arena(g);mob(g,300,250);
            for(int t=0;t<3000;t++){
                if(g.mode==ActionGame.Mode.RESULT){g.returnCamp();g.start(t+99);}
                if(g.mode==ActionGame.Mode.REWARD)g.chooseReward(r.nextInt(3));
                in.mx=(float)Math.cos(t*.021);in.my=(float)Math.sin(t*.017);in.attack=true;
                if(t%65==0)g.dodge(in.mx,in.my);if(t%320==0)g.skill();if(g.hp<35)g.potion();g.update(1f/60f,in);
                if(t%120==0){g=ActionSave.decode(ActionSave.encode(g));check(g.shield>=0&&g.hp<=g.maxHp(),"maxed build bounded during stress");}g.signals.clear();
            }
        }
        System.out.println("PASS 18000 simulation steps with six maxed custom builds and periodic save/restore");
    }
    public static void main(String[] args)throws Exception{profiles();treeRules();effects();persistence();stress();System.out.println("BUILD CHECKS PASSED: "+count);}
}
