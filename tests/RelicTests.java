import io.wayfarer.rpg.core.*;
import java.io.*;
import java.util.*;

public final class RelicTests {
    private static int count;
    private static void check(boolean b,String s){count++;if(!b)throw new AssertionError(s);}
    private static void near(float a,float b,String s){check(Math.abs(a-b)<.035f,s+": "+a+" != "+b);}
    private static ActionGame game(int kind,int a,int b){ActionGame g=new ActionGame();g.relicOwned=RelicSystem.ALL;g.equipRelics(a,b);g.bindSoul(kind,0);g.start(83);g.blocks.clear();g.enemies.clear();g.x=560;g.y=600;g.invulnerable=0;ActionGame.Enemy e=new ActionGame.Enemy(g.nextId++,0,620,600,1000);e.stun=1;g.enemies.add(e);return g;}
    private static void tick(ActionGame g){g.update(1f/60f,new ActionGame.Input());g.signals.clear();}
    private static void attackShot(ActionGame g,float damage){ActionGame.Enemy e=g.enemies.get(0);g.shots.add(new ActionGame.Shot(e.x,e.y,0,0,damage,true));tick(g);}
    private static void enemyHit(ActionGame g,float damage){g.invulnerable=0;g.hitStop=0;g.shots.add(new ActionGame.Shot(g.x,g.y,0,0,damage,false));tick(g);}
    private static void equipment()throws Exception{
        ActionGame g=new ActionGame();check(g.relicOwned==6&&g.relics[0][0]==0,"starters owned but not automatically equipped");check(!g.equipRelics(3,0),"locked relic rejected");check(!g.equipRelics(1,1),"duplicate rejected");check(!g.equipRelics(-1,0),"invalid id rejected");
        check(g.equipRelics(1,2),"equip two starters");near(g.maxHp(),122,"wick maximum health");near(g.dodgePeriod(),1,"compass cooldown");g.selectHero(1);check(!g.hasRelic(1),"preset independent");g.selectCustom(2);check(g.equipRelics(2,1),"custom shares collection");g.profile().xp=160;g.learn(4);g.selectHero(0);check(g.hasRelic(1)&&g.hasRelic(2),"original loadout retained");
        ActionGame restored=ActionSave.decode(ActionSave.encode(g));restored.selectCustom(2);check(restored.hasRelic(2)&&restored.rank(4)==1&&restored.heroXp()==160,"custom loadout and talents persisted");
        g.start(4);check(!g.equipRelics(0,0)&&g.hasRelic(1),"combat swap blocked");g.pause();check(!g.equipRelics(0,0),"paused swap blocked");g.abandon();g.returnCamp();check(g.equipRelics(0,0),"unequip camp");near(g.maxHp(),110,"unequip restores baseline");
        System.out.println("PASS collection ownership, two-slot constraints, six character identities, camp-only equip and stat removal");
    }
    private static void effects()throws Exception{
        ActionGame g=game(0,3,0);g.dodge(0,-1);near(g.relicEcho,3,"dodge primes bell");g.rollTime=0;attackShot(g,20);near(g.enemies.get(0).hp,973,"bell boosts one hit");near(g.relicEcho,0,"bell consumed");attackShot(g,20);near(g.enemies.get(0).hp,953,"second hit unboosted");
        g=game(1,3,0);g.dodge(0,-1);g.rollTime=0;g.soulSkill();for(int i=0;i<65;i++){g.enemies.get(0).stun=1;tick(g);}check(g.relicEcho>0,"domain cannot consume bell");g.pause();float echo=g.relicEcho;for(int i=0;i<200;i++)tick(g);near(g.relicEcho,echo,"pause freezes bell");ActionGame a=ActionSave.decode(ActionSave.encode(g));near(a.relicEcho,echo,"bell persists");a.resume();for(int i=0;i<200;i++){a.enemies.get(0).stun=1;tick(a);}near(a.relicEcho,0,"bell expires");
        g=game(1,4,0);g.hp=10;g.potion();near(g.hp,10+g.maxHp()*.45f*1.15f*1.1f,"chalice and light healing multiply");near(g.soulWard,12,"chalice light ward");g.soulWard=30;g.hp=10;g.potion();near(g.soulWard,30,"chalice cannot lower stronger ward");
        g=game(2,4,0);g.corruption=60;g.soulQuiet=3;g.hp=10;g.potion();near(g.corruption,40,"chalice removes debt before healing");near(g.hp,10+g.maxHp()*.45f*.88f*1.1f,"chalice uses purified corruption");g.hp=g.maxHp();g.potion();check(g.potions==0&&g.corruption==20,"full health cleanse consumes potion");
        g=game(0,4,0);g.potion();check(g.potions==2,"full health neutral potion not wasted");g=game(1,5,0);near(g.soulEnergy,70,"radiance crown initial energy");g=game(2,5,0);near(g.soulEnergy,55,"other bound crown initial energy");g=game(0,5,0);near(g.soulEnergy,0,"unbound crown inert");
        g=game(2,6,0);float hp=g.hp;g.skill();near(hp-g.hp,g.maxHp()*.03f,"ring reduces base sacrifice");near(g.corruption,18,"ring retains debt");g.hitStop=0;hp=g.hp;g.soulSkill();near(hp-g.hp,g.maxHp()*.06f,"ring reduces soul sacrifice");near(g.corruption,48,"soul debt remains");
        g=game(2,6,0);g.enemies.get(0).hex=3;attackShot(g,20);near(g.enemies.get(0).hp,1000-20*1.15f*1.1f,"ring multiplies cursed damage");
        g=game(1,7,3);g.relicEcho=3;g.soulEnergy=0;g.soulWard=20;enemyHit(g,10);near(g.enemies.get(0).hp,1000,"fully blocked hit does not reflect");g.soulWard=0;g.soulEnergy=0;enemyHit(g,20);near(g.enemies.get(0).hp,995,"thorns reflects actual life damage");near(g.soulEnergy,0,"thorns cannot charge energy");check(g.relicEcho>0,"thorns cannot spend bell");
        g=game(2,8,0);g.corruption=100;g.soulQuiet=3;g.hp=2;enemyHit(g,100);check(g.mode==ActionGame.Mode.PLAY&&g.relicRevived,"hourglass rescues lethal enemy hit");near(g.hp,g.maxHp()*.25f,"rescue ignores healing penalty");near(g.invulnerable,1,"rescue invulnerability");g.pause();a=ActionSave.decode(ActionSave.encode(g));check(a.relicRevived,"rescue use persists");a.resume();a.room=2;a.generateRoom();a.blocks.clear();a.enemies.clear();a.enemies.add(new ActionGame.Enemy(a.nextId++,0,650,500,100));a.hp=1;enemyHit(a,100);check(a.mode==ActionGame.Mode.RESULT,"second lethal hit across room cannot rescue");a.returnCamp();a.start(12);check(!a.relicRevived,"new run replenishes hourglass");
        g=game(2,8,6);g.hp=.5f;g.skill();check(g.hp==.5f&&!g.relicRevived,"nonlethal self sacrifice never consumes rescue");
        System.out.println("PASS all eight relic effects, radiant/forbidden synergies, proc exclusions, pause and once-per-run rescue");
    }
    private static void discoveries()throws Exception{
        ActionGame g=new ActionGame();g.start(400);g.enemies.clear();tick(g);check(g.lastRelic==0&&g.relicOwned==6,"first room has no relic");g.chooseReward(0);g.x=800;g.y=80;tick(g);check(g.room==2,"normal exit advances room");g.enemies.clear();tick(g);check(g.mode==ActionGame.Mode.REWARD&&g.lastRelic>=3&&Integer.bitCount(g.relicOwned)==3,"second room discovers unowned relic");int found=g.lastRelic,owned=g.relicOwned;long rng=g.rngState;
        ActionGame a=ActionSave.decode(ActionSave.encode(g));tick(a);check(a.relicOwned==owned&&a.rngState==rng,"reward restore never rolls again");a.chooseReward(0);for(int i=0;i<10;i++)tick(a);check(a.lastRelic==found&&a.relicOwned==owned,"claim cannot duplicate relic");a.pause();a.abandon();a.returnCamp();check(a.relicOwned==owned&&a.relicAwards==0&&a.lastRelic==0,"death keeps collection, camp clears run state");check(a.equipRelics(found,1),"discovery equip after return");
        g=new ActionGame();for(int run=0;run<2;run++){
            g.start(500+run);for(int room=1;room<=5;room++){g.room=room;g.generateRoom();g.enemies.clear();tick(g);if(room<5)g.chooseReward(0);}check(g.won,"fixture clears boss");g.returnCamp();
        }check(g.relicOwned==RelicSystem.ALL,"two complete expeditions collect all six nonstarters without duplicates");
        g.start(33);g.room=2;g.generateRoom();g.enemies.clear();tick(g);check(g.lastRelic==-1&&g.loot==15,"complete collection gives run currency");ActionSave.decode(ActionSave.encode(g));
        ActionGame d1=new ActionGame(),d2=new ActionGame();for(ActionGame d:new ActionGame[]{d1,d2}){d.start(73);d.room=2;d.generateRoom();d.enemies.clear();tick(d);}check(d1.lastRelic==d2.lastRelic&&d1.rngState==d2.rngState,"discovery uses serialized deterministic RNG");
        System.out.println("PASS checkpoint discovery, no duplicates, no reward reroll, failure retention, boss reward and full-collection compensation");
    }
    private static byte[] edit(byte[] bytes,String k,String v)throws Exception{Properties p=new Properties();p.load(new ByteArrayInputStream(bytes));p.setProperty(k,v);ByteArrayOutputStream out=new ByteArrayOutputStream();p.store(out,"");return out.toByteArray();}
    private static void reject(byte[] bytes,String k,String v)throws Exception{boolean bad=false;try{ActionSave.decode(edit(bytes,k,v));}catch(IOException e){bad=true;}check(bad,"reject "+k+"="+v);}
    private static void saves()throws Exception{
        ActionGame g=new ActionGame();g.selectCustom(1);g.profile().xp=160;g.learn(0);g.bindSoul(2,1);g.start(3);g.soulSkill();g.pause();byte[] bytes=ActionSave.encode(g);ActionGame old=ActionSave.decode(edit(bytes,"version","3"));check(old.soulBuff>0&&old.corruption==30&&old.rank(0)==1&&old.heroXp()==160&&old.relicOwned==6&&!old.hasRelic(1),"v3 migration preserves soul snapshot and leaves loadouts empty");
        reject(bytes,"relicOwned","7");reject(bytes,"relicOwned","0");reject(bytes,"relic4.0","8");reject(bytes,"relicAwards","1");reject(bytes,"relicAwards","16");reject(bytes,"lastRelic","3");reject(bytes,"relicEcho","NaN");reject(bytes,"relicEcho","1");reject(bytes,"relicRevived","true");
        g=new ActionGame();g.equipRelics(1,2);bytes=ActionSave.encode(g);reject(bytes,"relic0.1","1");reject(bytes,"relicAwards","2");reject(bytes,"relicEcho","4");g.relicOwned=RelicSystem.ALL;g.equipRelics(3,8);bytes=ActionSave.encode(g);reject(bytes,"relicEcho","1");reject(bytes,"relicRevived","true");
        System.out.println("PASS v3 migration, independent loadouts and malformed collection, discovery and proc state rejection");
    }
    private static void combinations()throws Exception{
        int steps=0;
        for(int first=0;first<=8;first++)for(int second=0;second<=8;second++){
            if(second!=0&&second<=first)continue;
            for(int soul=0;soul<=2;soul++){
                ActionGame g=game(soul,first,second);g.pause();g.abandon();g.returnCamp();g.experience[0]=1200;
                for(int node:new int[]{0,4,8}){for(int k=0;k<3;k++)g.learn(node);for(int k=0;k<2;k++)g.learn(node+1);}g.learn((first+second)%2==0?3:11);g.start(991+first*9+second);
                ActionGame.Input in=new ActionGame.Input();in.attack=true;
                for(int t=0;t<480;t++){
                    if(g.mode==ActionGame.Mode.RESULT){g.returnCamp();g.start(1+t);}if(g.mode==ActionGame.Mode.REWARD)g.chooseReward(t%3);
                    in.mx=(float)Math.sin(t*.04);in.my=(float)Math.cos(t*.03);if(t%83==0)g.dodge(in.mx,in.my);if(t%210==0)g.skill();if(t%290==0)g.soulSkill();if(g.hp<30)g.potion();g.update(1f/60f,in);g.signals.clear();steps++;
                    if(t%60==0){g=ActionSave.decode(ActionSave.encode(g));check(Float.isFinite(g.hp)&&g.hp>=0&&g.hp<=g.maxHp()&&g.relicEcho>=0&&g.relicEcho<=3,"combination bounds and roundtrip");}
                }
            }
        }
        System.out.println("PASS "+steps+" steps across all relic pairs/empty slots, three soul states and maxed talent builds");
    }
    public static void main(String[] args)throws Exception{equipment();effects();discoveries();saves();combinations();System.out.println("RELIC CHECKS PASSED: "+count);}
}
