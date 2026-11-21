import io.wayfarer.rpg.core.*;
import java.io.*;
import java.util.*;

public final class ActionTests {
    private static int count;
    private static void check(boolean ok,String why){count++;if(!ok)throw new AssertionError(why);}
    private static void near(float a,float b,String why){check(Math.abs(a-b)<.035f,why+": "+a+" != "+b);}
    private static ActionGame game(int hero){ActionGame g=new ActionGame();g.selectHero(hero);g.start(38471);g.blocks.clear();g.enemies.clear();g.x=560;g.y=600;g.invulnerable=0;return g;}
    private static ActionGame.Enemy mob(ActionGame g,int type,float x,float y,float hp){ActionGame.Enemy e=new ActionGame.Enemy(g.nextId++,type,x,y,hp);g.enemies.add(e);return e;}
    private static void ticks(ActionGame g,ActionGame.Input in,int n){for(int i=0;i<n;i++)g.update(1f/60f,in);}
    private static void movement(){
        ActionGame a=game(0),b=game(0);mob(a,0,100,100,100);mob(b,0,100,100,100);
        ActionGame.Input in=new ActionGame.Input();in.mx=1;ticks(a,in,30);in.my=1;ticks(b,in,30);
        check(Math.abs(ActionGame.len(a.x-560,a.y-600)-ActionGame.len(b.x-560,b.y-600))<.05,"diagonal input normalized");
        a.x=1050;a.y=600;in.my=0;ticks(a,in,60);check(a.x<=1057.01,"player bounded by arena");
        a.x=560;a.y=500;a.blocks.add(new ActionGame.Block(585,470,60,60,0));ticks(a,in,60);check(a.x<571,"solid obstacle blocks movement");
        float x=a.x;a.pause();ticks(a,in,60);check(a.x==x,"pause freezes simulation");a.resume();
        in.mx=Float.NaN;in.my=Float.POSITIVE_INFINITY;ticks(a,in,1);check(Float.isFinite(a.x)&&Float.isFinite(a.y),"nonfinite input rejected");
        System.out.println("PASS normalized movement, collisions, pause and invalid input");
    }
    private static void combat()throws Exception{
        ActionGame g=game(0);ActionGame.Enemy e=mob(g,0,620,600,200);e.stun=1;
        ActionGame.Input in=new ActionGame.Input();in.attack=true;ticks(g,in,14);
        check(e.hp==178,"single sword swing cannot repeatedly hit same target");
        ticks(g,in,60);check(e.hp<150,"held attack repeats combo");
        g=game(1);e=mob(g,0,720,600,100);e.stun=1;in.attack=true;ticks(g,in,22);check(e.hp<100,"bow projectile travels and hits");
        g=new ActionGame();g.selectHero(2);g.chooseCareer(CareerSystem.REPORTER,0,0);g.start(38471);g.blocks.clear();g.enemies.clear();g.x=560;g.y=600;e=mob(g,0,615,600,100);g.skill();check(e.hp==100&&g.careerSkillMode==4&&g.careerSkillTime==30&&g.skillCd>0,"reporter activates the thirty-second speed state without an opening burst");float eh=e.hp;g.skill();check(e.hp==eh,"career skill cooldown prevents repeat");
        g=game(0);e=mob(g,0,590,600,100);e.state=1;e.timer=.01f;g.dodge(0,-1);float health=g.hp;in.attack=false;ticks(g,in,2);check(g.hp==health,"dodge invulnerability avoids melee");check(g.y<600,"dodge moves immediately");
        float cd=g.rollCd;g.dodge(1,0);check(g.rollCd==cd,"dodge cooldown honored");
        ticks(g,in,25);e.x=g.x+25;e.y=g.y;e.state=1;e.timer=.001f;e.stun=0;ticks(g,in,1);check(g.hp<health,"melee damages after dodge ends");
        health=g.hp;e.state=1;e.timer=.001f;ticks(g,in,1);check(g.hp==health,"hurt invulnerability prevents damage stacking");
        g.hp=g.maxHp();g.potion();check(g.potions==2,"full health does not consume potion");g.hp=10;g.potion();check(g.potions==1&&g.hp>10,"potion heals and consumes one");
        g=new ActionGame();g.chooseCareer(CareerSystem.REPORTER,0,0);g.start(1);g.blocks.clear();g.enemies.clear();g.x=560;g.y=600;e=mob(g,0,610,600,100);g.blocks.add(new ActionGame.Block(580,560,15,80,0));g.skill();check(e.hp==100&&g.careerSkillTime==30,"reporter state activates independently of obstacle layout");g.soulCd=5;g.generateRoom();check(g.skillCd==0&&g.soulCd==0,"entering a new room resets career and soul skill cooldowns");
        check(ActionGame.segmentDistance(5,2,0,0,10,0)==2,"swept collision geometry");
        System.out.println("PASS melee combos, ranged hits, skills, dodge and damage windows");
    }
    private static void roomsAndRewards()throws Exception{
        for(int seed=1;seed<=80;seed++){
            ActionGame g=new ActionGame();g.start(seed);
            for(int room=1;room<=5;room++){
                g.room=room;g.generateRoom();check(g.free(g.x,g.y,15),"spawn walkable");
                for(ActionGame.Enemy e:g.enemies)check(g.free(e.x,e.y,e.radius()),"enemy spawn walkable");
                // A permanent clear central corridor guarantees a route between spawn and exits.
                for(int y=70;y<=660;y+=20)check(g.free(560,y,15),"central corridor remains connected");
                check(g.free(280,80,15)&&g.free(840,80,15),"exit pads walkable");
                ActionSave.decode(ActionSave.encode(g));
            }
        }
        ActionGame g=game(0);mob(g,0,600,600,0);ActionGame.Input in=new ActionGame.Input();ticks(g,in,1);
        check(g.mode==ActionGame.Mode.REWARD,"room clear enters reward selection");g.chooseReward(0);check(g.damageBonus==4&&g.roomCleared,"reward applies once");g.chooseReward(0);check(g.damageBonus==4,"cannot claim reward twice");
        g.hp=50;g.x=280;g.y=80;ticks(g,in,1);check(g.room==2&&g.hp>50,"spring exit advances and heals");
        g.enemies.clear();ticks(g,in,1);g.chooseReward(2);int loot=g.loot;g.x=840;g.y=80;ticks(g,in,1);check(g.room==3&&g.loot==loot+12,"ember exit advances and rewards");
        g.loot=100;g.pause();g.abandon();check(g.mode==ActionGame.Mode.RESULT&&g.bank==70,"abandon uses defeat settlement");int bank=g.bank;g.abandon();check(g.bank==bank,"settlement idempotent");
        g.returnCamp();g.upgrade(false);check(g.power==1&&g.bank==40,"shared upgrade purchase");g.selectHero(1);check(g.damage()==20,"upgrade shared across heroes");
        g.start(29);check(g.damageBonus==0&&g.speedBonus==0&&g.power==1,"new run resets temporary bonuses only");
        g.enemies.clear();g.room=5;g.loot=100;ticks(g,in,1);check(g.won&&g.bank==180&&g.wins==1,"boss-room victory pays full loot plus bonus");
        ActionSave.decode(ActionSave.encode(g));g.returnCamp();ActionSave.decode(ActionSave.encode(g));
        System.out.println("PASS 400 procedural room layouts, reward choices, exits and persistent progression");
    }
    private static byte[] mutate(byte[] bytes,String key,String value)throws Exception{Properties p=new Properties();p.load(new ByteArrayInputStream(bytes));p.setProperty(key,value);ByteArrayOutputStream out=new ByteArrayOutputStream();p.store(out,"");return out.toByteArray();}
    private static void rejects(byte[] bytes,String key,String value)throws Exception{boolean rejected=false;try{ActionSave.decode(mutate(bytes,key,value));}catch(IOException e){rejected=true;}check(rejected,"reject invalid "+key);}
    private static void tacticalInscriptions()throws Exception{
        ActionGame g=game(0);g.mode=ActionGame.Mode.REWARD;g.tacticOffers[0]=0;g.tacticOffers[1]=1;g.tacticOffers[2]=2;
        g.openTacticDraft();check(g.tacticDraftOpen,"reward opens tactical inscription draft");
        check(g.chooseTactic(0)&&g.tacticRanks[0]==1&&g.tacticChosen&&!g.tacticDraftOpen,"one inscription can be selected per cleared room");
        g.openTacticDraft();check(!g.tacticDraftOpen&&!g.chooseTactic(1),"second inscription cannot be claimed in the same room");
        ActionGame saved=ActionSave.decode(ActionSave.encode(g));check(saved.tacticRanks[0]==1&&saved.tacticChosen,"chosen inscription persists in reward snapshot");
        g=game(0);g.tacticRanks[0]=2;g.activeSkill();check(g.activeSkillCd>0,"signature technique still uses normal cooldown");
        ActionGame baseline=game(0);baseline.activeSkill();check(g.enemies.size()==0&&baseline.enemies.size()==0,"empty arena remains safe");
        g=game(0);g.tacticRanks[1]=2;g.followUpWindow=1;g.enemies.add(new ActionGame.Enemy(1,0,620,600,1000));g.followUp();float enhanced=1000-g.enemies.get(0).hp;
        baseline=game(0);baseline.followUpWindow=1;baseline.enemies.add(new ActionGame.Enemy(1,0,620,600,1000));baseline.followUp();check(enhanced>1000-baseline.enemies.get(0).hp,"follow-through inscription increases follow-up damage");
        g=game(0);g.tacticRanks[3]=2;near(g.activeSkillPeriod(),baseline.activeSkillPeriod()-2,"cooldown inscription shortens active skill cycle");
        g=game(0);g.tacticRanks[4]=2;ActionGame.Enemy marked=new ActionGame.Enemy(1,0,620,600,1000);marked.hex=3;g.enemies.add(marked);g.hitForTest(marked,100);
        baseline=game(0);marked=new ActionGame.Enemy(1,0,620,600,1000);marked.hex=3;baseline.enemies.add(marked);baseline.hitForTest(marked,100);check(1000-g.enemies.get(0).hp>1000-baseline.enemies.get(0).hp,"curse inscription amplifies marked-target damage");
        g=game(0);g.tacticRanks[5]=2;g.enemies.add(new ActionGame.Enemy(1,0,620,600,1000));g.activeSkill();check(g.shield>=12,"ward circuit grants shield on active skill");
        g=game(0);g.tacticRanks[6]=2;baseline=game(0);check(g.attackPeriod()<baseline.attackPeriod(),"combat tempo increases attack speed");
        g=game(0);g.tacticRanks[7]=2;g.drops.add(new ActionGame.Drop(g.x,g.y,100,false));g.update(1f/60f,new ActionGame.Input());check(g.loot==124,"scavenging inscription increases currency pickup");
        byte[] bytes=ActionSave.encode(g);rejects(bytes,"tacticRank0","4");rejects(bytes,"tacticOffer0","8");
        System.out.println("PASS tactical inscription draft, per-room choice, eight run modifiers and v23 persistence");
    }
    private static void persistence()throws Exception{
        ActionGame g=new ActionGame();g.selectHero(1);g.start(123);g.skill();g.hp-=20;g.potion();
        byte[] bytes=ActionSave.encode(g);ActionGame a=ActionSave.decode(bytes);
        check(a.hero==g.hero&&a.hp==g.hp&&a.potions==g.potions&&a.shots.size()==g.shots.size()&&a.rngState==g.rngState,"save restores action snapshot");
        check(a.random(1000)==g.random(1000),"RNG continues from persisted state");
        rejects(bytes,"version","99");rejects(bytes,"x","NaN");rejects(bytes,"hp","Infinity");rejects(bytes,"hero","3");rejects(bytes,"potions","-1");rejects(bytes,"enemies","500");rejects(bytes,"power","11");rejects(bytes,"bank","2147483647");rejects(bytes,"mode","BAD");rejects(bytes,"rng","0");rejects(bytes,"x","1");rejects(bytes,"room","0");rejects(bytes,"format","wayfarer-rpg");
        boolean bad=false;try{ActionSave.decode(new byte[131073]);}catch(IOException e){bad=true;}check(bad,"oversize saves rejected");
        g=game(0);ActionGame.Enemy e=mob(g,0,610,600,200);e.stun=1;ActionGame.Input in=new ActionGame.Input();in.attack=true;ticks(g,in,8);float enemyHp=e.hp;
        a=ActionSave.decode(ActionSave.encode(g));in.attack=false;ticks(a,in,8);check(a.enemies.get(0).hp==enemyHp,"restore cannot replay a partially resolved swing");
        System.out.println("PASS snapshot continuity, malformed saves and no replayed attack");
    }
    private static void stress()throws Exception{
        ActionGame.Input in=new ActionGame.Input();Random r=new Random(8);
        for(int h=0;h<3;h++){
            ActionGame g=new ActionGame();g.selectHero(h);g.start(930+h);
            for(int t=0;t<18000;t++){
                if(g.mode==ActionGame.Mode.RESULT){g.returnCamp();g.start(930+t);}
                if(g.mode==ActionGame.Mode.REWARD)g.chooseReward(r.nextInt(3));
                in.mx=(float)Math.cos(t*.009);in.my=(float)Math.sin(t*.011);in.attack=true;
                if(t%83==0)g.dodge(in.mx,in.my);if(t%420==0)g.skill();if(g.hp<35)g.potion();g.update(1f/60f,in);
                if(t%120==0){g=ActionSave.decode(ActionSave.encode(g));check(g.free(g.x,g.y,15),"stress player remains valid");}
                g.signals.clear();
            }
        }
        System.out.println("PASS 54000 simulation steps with periodic save/restore");
    }
    private static void shopAndItems()throws Exception{
        ActionGame g=game(0);g.mode=ActionGame.Mode.REWARD;g.loot=100;g.openShop();
        check(g.buyShopItem(0)&&g.bombs==1&&g.loot==76,"shop purchases bomb with run currency");
        check(g.buyShopItem(1)&&g.snares==1&&g.loot==48,"shop purchases control item");
        check(g.buyShopItem(2)&&g.tonics==1&&g.loot==16,"shop purchases tonic");
        check(!g.buyShopItem(0),"shop rejects insufficient currency");
        ActionGame a=ActionSave.decode(ActionSave.encode(g));check(a.bombs==1&&a.snares==1&&a.tonics==1&&a.shopOpen,"shop and inventory persist");
        a.closeShop();a.mode=ActionGame.Mode.PLAY;a.enemies.clear();ActionGame.Enemy e=new ActionGame.Enemy(a.nextId++,0,a.x+100,a.y,1000);a.enemies.add(e);
        a.useBomb();check(a.bombs==0&&e.hp<1000,"bomb deals area damage");
        a.snares=1;a.useSnare();check(a.snares==0&&e.stun>=2.4f,"snare controls nearby enemy");
        a.tonics=1;a.useTonic();check(a.tonics==0&&a.battleTonicTime==10,"tonic starts timed combat buff");
        float boosted=a.damage();a.update(.5f,new ActionGame.Input());check(a.battleTonicTime<10,"tonic timer advances during play");a.battleTonicTime=0;check(a.damage()<boosted,"tonic damage bonus ends when duration expires");a.battleTonicTime=9.5f;
        a=ActionSave.decode(ActionSave.encode(a));check(a.battleTonicTime>0,"active tonic duration persists through save");
        a.mode=ActionGame.Mode.RESULT;a.returnCamp();check(a.bombs==0&&a.snares==0&&a.tonics==0&&a.battleTonicTime==0,"run items clear after settlement");
        System.out.println("PASS in-run shop, currency validation, inventory, consumables and timed buff persistence");
    }
    private static void fiveActiveSkills()throws Exception{
        for(int style=0;style<5;style++){
            ActionGame g=new ActionGame();g.selectCustom(0);BuildSystem.Profile draft=g.profile().copy();draft.skill=style;g.customize(0,draft);g.start(44+style);g.blocks.clear();g.enemies.clear();g.x=500;g.y=500;g.faceX=1;g.faceY=0;
            ActionGame.Enemy e=new ActionGame.Enemy(g.nextId++,0,620,500,1000);e.stun=1;g.enemies.add(e);
            g.activeSkill();check(g.activeSkillCd>0&&g.followUpWindow>0,"active skill "+style+" starts independent cooldown and follow-up");
            if(style==0||style==2||style==3)check(e.hp<1000,"active skill "+style+" resolves combat effect");
            if(style==1)check(!g.shots.isEmpty(),"fan shot creates projectiles");
            if(style==4){check(g.activeSkillDelay>0,"thunder trap has a warning delay");ActionGame a=ActionSave.decode(ActionSave.encode(g));for(int i=0;i<50;i++){g.update(1f/60f,new ActionGame.Input());a.update(1f/60f,new ActionGame.Input());}near(a.enemies.get(0).hp,g.enemies.get(0).hp,"trap persists through save restore");}
            float careerCd=g.skillCd;g.skill();check(g.skillCd>careerCd&&g.activeSkillCd>0,"career overdrive and general active have independent cooldowns");
            ActionGame saved=ActionSave.decode(ActionSave.encode(g));near(saved.activeSkillCd,g.activeSkillCd,"active cooldown persists");near(saved.followUpWindow,g.followUpWindow,"follow-up window persists");
            if(g.followUpWindow>0){g.followUp();check(g.followUpWindow==0,"follow-up consumes its window");}
        }
        ActionGame g=new ActionGame();g.start(12);g.pause();float cd=g.activeSkillCd;for(int i=0;i<30;i++)g.update(1f/60f,new ActionGame.Input());near(g.activeSkillCd,cd,"pause freezes active cooldown");
        System.out.println("PASS five active skills, independent career cooldown, follow-ups, trap persistence and pause");
    }
    public static void main(String[] args)throws Exception{movement();combat();shopAndItems();fiveActiveSkills();roomsAndRewards();tacticalInscriptions();persistence();stress();System.out.println("ACTION CHECKS PASSED: "+count);}
}
