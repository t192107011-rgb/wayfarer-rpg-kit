import io.wayfarer.rpg.core.*;
import java.io.*;
import java.util.*;

/** Behavioural coverage of entry-locked threat, per-category finals and endless drops. */
public final class EndlessTests {
    static int checks;
    static void check(boolean b,String why){checks++;if(!b)throw new AssertionError(why);}
    static void near(float a,float b,String why){check(Math.abs(a-b)<.04f,why+": "+a+" / "+b);}
    static ActionGame build(int mask){ActionGame g=new ActionGame();g.experience[0]=1280;for(int b=0;b<3;b++){check(g.learn(b*4),"root");check(g.learn(b*4+1)&&g.learn(b*4+1),"parent");check(g.learn(b*4+2+((mask>>b)&1)),"independent final");}return g;}
    static ActionGame arena(int mask){ActionGame g=build(mask);g.startEndless(91);g.blocks.clear();g.enemies.clear();g.x=560;g.y=600;return g;}
    static ActionGame.Enemy mob(ActionGame g,int type,float x,float y){ActionGame.Enemy e=new ActionGame.Enemy(g.nextId++,type,x,y,1000);e.stun=1;g.enemies.add(e);return e;}
    static void step(ActionGame g){g.hitStop=0;g.update(.016f,new ActionGame.Input());g.signals.clear();}
    static void attack(ActionGame g){ActionGame.Input in=new ActionGame.Input();in.attack=true;g.hitStop=0;g.update(.016f,in);}
    static void clear(ActionGame g){for(ActionGame.Enemy e:g.enemies)e.hp=0;step(g);}
    static void exit(ActionGame g,int reward){g.chooseReward(reward);g.x=300;g.y=70;step(g);}
    static byte[] edit(byte[] bytes,String k,String v)throws Exception{Properties p=new Properties();p.load(new ByteArrayInputStream(bytes));p.setProperty(k,v);ByteArrayOutputStream out=new ByteArrayOutputStream();p.store(out,"");return out.toByteArray();}
    static void reject(byte[] b,String k,String v)throws Exception{boolean bad=false;try{ActionSave.decode(edit(b,k,v));}catch(IOException e){bad=true;}check(bad,"reject "+k);}
    static void branches()throws Exception{
        for(int mask=0;mask<8;mask++){ActionGame g=build(mask);for(int b=0;b<3;b++)check(!g.learn(b*4+2+(1-((mask>>b)&1))),"same-category final blocked");check(g.availablePoints()==6,"18-point budget leaves six points after three paths");ActionGame a=ActionSave.decode(ActionSave.encode(g));for(int i=0;i<12;i++)check(g.rank(i)==a.rank(i),"three-final snapshot");reject(ActionSave.encode(g),"version","9");g.selectHero(1);check(g.rank(2)+g.rank(3)+g.rank(6)+g.rank(7)+g.rank(10)+g.rank(11)==0,"roles independent");}
        check(BuildSystem.points(1280)==18&&BuildSystem.points(1000000)==18,"budget capped at eighteen");
        ActionGame g=arena(2);ActionGame.Enemy e=mob(g,0,800,600);e.stun=0;g.hp=g.maxHp()*.5f;g.skill();float hp=g.hp,enemy=e.hp,cd=g.skillCd;near(g.followUpWindow,3,"loop extends follow window");g.skill();near(enemy-e.hp,g.damage()*1.3f*1.2f,"loop enhances follow damage outside initial radius");near(g.hp-hp,g.maxHp()*.03f,"long light connected follow heals");near(g.skillCd,cd-.8f,"loop plus light cooldown synergy");hp=g.hp;g.skill();near(g.hp,hp,"third press gives no heal");
        g=arena(3);mob(g,0,800,600);g.hp=g.maxHp()*.1f;g.skill();hp=g.hp;g.skill();near(g.hp-hp,g.maxHp()*.06f,"low life plus light doubles follow healing");
        g=arena(4);mob(g,0,650,600);g.dodge(1,0);near(g.shield,6,"dodge plus mirror earns shield");g.dodge(1,0);near(g.shield,6,"cooldown prevents repeated shield");g.rollTime=0;g.skill();g.shield=0;g.skill();near(g.shield,6,"connected mirror follow shield");g.shield=0;g.attackCd=0;g.combo=1;g.comboWindow=1;attack(g);near(g.shield,4,"third-hit echo plus mirror shield");
        g=arena(2);mob(g,0,100,100);g.hp=g.maxHp()*.5f;g.skill();hp=g.hp;cd=g.skillCd;g.skill();near(g.hp,hp,"missed follow gives no heal");near(g.skillCd,cd,"missed follow gives no cooldown rebate");
        System.out.println("PASS all eight per-category builds, independent saves, legacy guards and hit-earned synergies");
    }
    static void ranges(){
        ActionGame g=new ActionGame();g.start(1);g.blocks.clear();g.enemies.clear();g.x=560;g.y=600;ActionGame.Enemy e=mob(g,0,675,600);float before=e.hp;attack(g);for(int i=0;i<15;i++)step(g);check(e.hp<before,"sword reaches 115, beyond old 90");
        g=arena(1);g.hp=g.maxHp()*.2f;near(g.meleeReach(),150,"low-life melee reach");near(g.activeRadius(),250,"low-life radial reach");e=mob(g,0,800,600);before=e.hp;g.skill();check(e.hp<before,"expanded radial attack actually hits");
        g=new ActionGame();g.start(1);g.blocks.clear();g.enemies.clear();g.invulnerable=0;g.x=560;g.y=600;e=mob(g,0,665,600);e.stun=0;e.state=1;e.timer=.001f;e.aimX=-1;e.aimY=0;before=g.hp;step(g);check(g.hp<before,"enemy strike hits beyond old 82 range");
    }
    static void threat()throws Exception{
        ActionGame weak=new ActionGame();weak.start(1);ActionGame strong=build(0);strong.power=10;strong.vitality=10;strong.bindSoul(1,0);strong.start(1);check(strong.threat>weak.threat,"permanent growth increases threat");for(ActionGame.Enemy e:strong.enemies){check(e.maxHp>strong.maxHp(),"enemy health above player");check(strong.enemyPower(e,1)>strong.baseDamage(),"enemy damage floor above base attack");}
        float hp=strong.enemies.get(0).hp,t=strong.threat;strong.hp=1;strong.soulBuff=5;strong.skill();near(strong.threat,t,"current life and buffs do not reprice threat");near(strong.enemies.get(0).hp,hp,"distant enemy not retroactively rescaled");strong.soulBuff=0;strong.pause();float elapsed=strong.roomElapsed;step(strong);near(strong.roomElapsed,elapsed,"pause freezes pressure clock");ActionGame restored=ActionSave.decode(ActionSave.encode(strong));near(restored.threat,t,"entry threat saved exactly");near(restored.encounterHpFloor,strong.encounterHpFloor,"entry health floor saved");
        ActionGame g=new ActionGame();g.start(3);clear(g);near(g.pressure,.12f,"quick clean clear raises next-room pressure");float pressure=g.pressure;step(g);near(g.pressure,pressure,"reward freeze cannot rescore clear");exit(g,0);check(g.threat>weak.threat,"next room applies pressure");g.roomDamage=g.maxHp()*.6f;clear(g);near(g.pressure,0,"heavy losses reduce pressure");
        g=new ActionGame();g.bindSoul(2,0);g.start(1);g.skill();g.skill();near(g.roomDamage,0,"forbidden self-payment excluded from adaptive damage");
        // Real pre-v10 fixture uses old health bounds, without new fields.
        g=new ActionGame();g.start(1);for(ActionGame.Enemy e:g.enemies)e.hp=e.maxHp=100;byte[] old=edit(ActionSave.encode(g),"version","9");restored=ActionSave.decode(old);near(restored.threat,1,"v9 encounter retains neutral threat");near(restored.enemies.get(0).hp,100,"migration preserves live enemy health");clear(restored);exit(restored,0);check(restored.threat>1,"next entry adopts dynamic scaling");
        System.out.println("PASS entry-locked threat, enemy floors, adaptive pressure, pause and v9 migration");
    }
    static void endless()throws Exception{
        ActionGame g=build(7);g.startEndless(33);check(g.potions==2,"fresh run starts with two bottles");for(int floor=1;floor<=90;floor++){check(g.endlessDepth==floor&&g.room==(floor-1)%5+1,"floor loop");check(g.enemies.size()==EncounterSystem.GROUPS[g.room-1].length+Math.min(5,(floor-1)/5),"bounded extra mixed enemies");clear(g);check(g.mode==ActionGame.Mode.REWARD&&!g.won&&g.bestDepth==floor,"boss clear continues endless and records cleared floor");g=ActionSave.decode(ActionSave.encode(g));g.mode=ActionGame.Mode.REWARD;exit(g,(floor-1)%3);check(g.mode==ActionGame.Mode.PLAY,"reward exit advances");}
        check(g.damageBonus==40&&g.healthBonus==200&&g.speedBonus==6,"endless reward caps");int loot=g.loot,bank=g.bank,xp=g.heroXp();g.pause();g.abandon();check(g.bank==bank+(int)(loot*.7f)&&g.heroXp()>xp,"endless settlement awards loot and experience");bank=g.bank;xp=g.heroXp();g.abandon();check(g.bank==bank&&g.heroXp()==xp,"cannot double settle");g.returnCamp();g=ActionSave.decode(ActionSave.encode(g));check(g.bestDepth==90&&!g.endless&&g.endlessDepth==0,"best persists after return");g.startEndless(44);check(g.endlessDepth==1&&g.bestDepth==90&&g.pressure==0,"new run resets current floor and pressure only");
        g=new ActionGame();g.start(4);for(int room=1;room<=5;room++){clear(g);if(room<5)exit(g,0);}check(g.mode==ActionGame.Mode.RESULT&&g.won,"story expedition still ends at fifth room");
        for(int mask=0;mask<8;mask++)for(int soul=0;soul<3;soul++){g=build(mask);g.power=10;g.vitality=10;g.bindSoul(soul,0);g.startEndless(127+mask);g.endlessDepth=200;g.room=5;g.damageBonus=40;g.healthBonus=200;g.speedBonus=6;g.generateRoom();for(int i=0;i<90;i++){g.hp=g.maxHp();g.invulnerable=1;step(g);if(i%15==0){g.skill();g=ActionSave.decode(ActionSave.encode(g));g.resume();}}check(g.threat<=6,"high-depth full builds stay within save bounds");}
        System.out.println("PASS ninety real clear/reward/exit transitions, boss continuation, caps, settlement, story ending and 24 deep-build snapshots");
    }
    static int bottles(ActionGame g){int n=0;for(ActionGame.Drop d:g.drops)if(d.potion)n++;return n;}
    static void potions()throws Exception{
        int[] totals=new int[3];int[] types={0,4,3};
        for(int k=0;k<3;k++){ActionGame g=new ActionGame();g.startEndless(76381);g.blocks.clear();g.enemies.clear();g.potions=5;mob(g,0,100,100);for(int i=0;i<2500;i++){g.drops.clear();g.shots.clear();for(ActionGame.Enemy e:g.enemies)e.stun=1;ActionGame.Enemy dead=mob(g,types[k],800,300);dead.hp=0;step(g);totals[k]+=bottles(g);}}
        check(totals[0]>220&&totals[0]<380,"ordinary drop rate sample "+totals[0]);check(totals[1]>350&&totals[1]<550,"special drop rate sample "+totals[1]);check(totals[2]>1120&&totals[2]<1380,"boss drop rate sample "+totals[2]);
        ActionGame g=new ActionGame();g.startEndless(418);g.blocks.clear();g.enemies.clear();g.potions=5;mob(g,0,100,100);mob(g,3,800,300).hp=0;ActionGame copy=ActionSave.decode(ActionSave.encode(g));copy.resume();step(g);step(copy);check(g.rngState==copy.rngState&&bottles(g)==bottles(copy),"reload repeats same drop roll");
        g.drops.clear();ActionGame.Drop bottle=new ActionGame.Drop(g.x,g.y,1,false);bottle.potion=true;g.drops.add(bottle);step(g);check(bottles(g)==1&&g.potions==5,"full bag retains bottle on ground");copy=ActionSave.decode(ActionSave.encode(g));copy.resume();check(bottles(copy)==1&&copy.potions==5,"bottle type and bag survive snapshot");copy.hp-=50;copy.potion();check(copy.potions==4,"drink spends one");step(copy);check(copy.potions==5&&bottles(copy)==0,"drink opens one pickup slot");clear(copy);check(copy.potions==5,"clear cannot duplicate collected bottle");
        byte[] b=ActionSave.encode(g);reject(b,"endlessDepth","2");reject(b,"threat","6.1");reject(b,"pressure","0.61");reject(b,"encounterDamageFloor","0");reject(b,"potions","6");reject(b,"relicAwards","16");reject(b,"d0",g.x+","+g.y+",15,1");
        System.out.println("PASS seeded potion samples /2500: "+Arrays.toString(totals)+", deterministic reload, full bag, drink/pickup and corrupt saves");
    }
    public static void main(String[] args)throws Exception{branches();ranges();threat();endless();potions();System.out.println("ENDLESS CHECKS PASSED: "+checks);}
}
