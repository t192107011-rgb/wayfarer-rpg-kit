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
    private static void rejectSavedValue(byte[] source,String key,String value,String reason)throws Exception{
        Properties p=new Properties();p.load(new ByteArrayInputStream(source));p.setProperty(key,value);
        ByteArrayOutputStream out=new ByteArrayOutputStream();p.store(out,"");
        boolean rejected=false;try{ActionSave.decode(out.toByteArray());}catch(IOException expected){rejected=true;}
        check(rejected,reason+" must reject "+key+"="+value);
    }
    private static void shieldSaveValidation()throws Exception{
        ActionGame g=game(0);g.shield=99999;byte[] valid=ActionSave.encode(g);
        ActionGame restored=ActionSave.decode(valid);near(restored.shield,99999,"maximum legal player shield round-trips");
        rejectSavedValue(valid,"shield","100000","shield above hard cap");
        rejectSavedValue(valid,"shield","NaN","NaN shield");
        rejectSavedValue(valid,"shield","Infinity","infinite shield");
        rejectSavedValue(valid,"shield","-1","negative shield");
        System.out.println("PASS strict player shield save bounds, finite checks and max-value roundtrip");
    }
    private static void shieldSourceSummary(){
        ActionGame g=game(0);String base=g.shieldSourceSummary();
        check(base.contains("闪避+12")&&base.contains("击杀+2"),"baseline shield sources are visible");
        g.coreTalents[0][4]=1;g.coreTalents[0][7]=1;g.coreTalents[0][10]=2;g.talents[0][11]=1;
        g.fieldPermits[0][0]=1;g.careers[0][0]=CareerSystem.HUNTER;
        String summary=g.shieldSourceSummary();
        check(summary.contains("闪避+20")&&summary.contains("远征许可开局+40"),"dodge and permit shield sources are accurate");
        check(summary.contains("职业技触发+60")&&summary.contains("通用主动技触发+10"),"active skill shield sources are labeled as triggers");
        check(summary.contains("镜火反震")&&summary.contains("护盾获取效率+30%"),"shield reflection and efficiency upgrades are visible");
        System.out.println("PASS accurate player shield source summary and trigger labels");
    }
    private static void enemyShieldTypes()throws Exception{
        ActionGame g=game(0);ActionGame.Enemy ward=mob(g,0,700,600,500);ward.guardType=1;ward.guard=20;
        g.hitForTest(ward,10);near(ward.guard,10f,"ward shield has standard durability");near(ward.hp,500,"ward shield absorbs before health");
        g.hitForTest(ward,10);check(ward.guard==0&&ward.guardBreak>0&&ward.stun>=.7f,"ward breaks with a short stagger");
        ActionGame.Enemy plated=mob(g,4,760,600,500);plated.guardType=2;plated.guard=20;
        g.hitForTest(plated,10);near(plated.guard,13.5f,"plated shield resists ordinary damage");near(plated.hp,500,"plating absorbs its reduced damage");
        ActionGame.Enemy regen=mob(g,3,820,600,500);regen.guardType=3;regen.guard=18;regen.guardDelay=0;
        ActionGame saved=ActionSave.decode(ActionSave.encode(g));ActionGame.Enemy restored=saved.enemies.get(2);
        check(restored.guardType==3&&restored.guard==18&&restored.guardBreak==0,"enemy shield class survives save restore");
        byte[] data=ActionSave.encode(g);rejects(data,"enemyGuardType2","4");rejects(data,"enemyGuardBreak2","4");
        System.out.println("PASS ward, plated and regenerative enemy shield classes, break stagger and persistence");
    }
    private static void combat()throws Exception{
        ActionGame g=game(0);ActionGame.Enemy e=mob(g,0,620,600,200);e.stun=1;
        ActionGame.Input in=new ActionGame.Input();in.attack=true;ticks(g,in,14);
        check(e.hp==178,"single sword swing cannot repeatedly hit same target");
        ticks(g,in,60);check(e.hp<150,"held attack repeats combo");
        g=game(1);e=mob(g,0,720,600,100);e.stun=1;in.attack=true;ticks(g,in,22);check(e.hp<100,"bow projectile travels and hits");
        g=new ActionGame();g.selectHero(2);g.chooseCareer(CareerSystem.REPORTER,0,0);g.start(38471);g.blocks.clear();g.enemies.clear();g.x=560;g.y=600;e=mob(g,0,615,600,100);g.skill();check(e.hp<100&&g.careerSkillMode==0&&g.careerSkillTime==0&&g.skillCd>0,"reporter opens with a full-field damage pulse and no healing penalty");float eh=e.hp;g.skill();check(e.hp==eh,"career skill cooldown prevents repeat");
        g=game(0);e=mob(g,0,590,600,100);e.state=1;e.timer=.01f;g.dodge(0,-1);float health=g.hp;in.attack=false;ticks(g,in,2);check(g.hp==health,"dodge invulnerability avoids melee");check(g.y<600,"dodge moves immediately");
        float cd=g.rollCd;g.dodge(1,0);check(g.rollCd==cd,"dodge cooldown honored");
        ticks(g,in,25);e.x=g.x+25;e.y=g.y;e.state=1;e.timer=.001f;e.stun=0;ticks(g,in,1);check(g.hp<health,"melee damages after dodge ends");
        health=g.hp;e.state=1;e.timer=.001f;ticks(g,in,1);check(g.hp==health,"hurt invulnerability prevents damage stacking");
        g.hp=g.maxHp();g.potion();check(g.potions==2,"full health does not consume potion");g.hp=10;g.potion();check(g.potions==1&&g.hp>10,"potion heals and consumes one");
        g=new ActionGame();g.chooseCareer(CareerSystem.REPORTER,0,0);g.start(1);g.blocks.clear();g.enemies.clear();g.x=560;g.y=600;e=mob(g,0,610,600,100);g.blocks.add(new ActionGame.Block(580,560,15,80,0));g.skill();check(e.hp<100&&g.careerSkillTime==0,"reporter full-field pulse ignores obstacle layout");g.soulCd=5;g.generateRoom();check(g.skillCd==0&&g.soulCd==0,"entering a new room resets career and soul skill cooldowns");
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
        check(g.mode==ActionGame.Mode.REWARD,"room clear enters reward selection");check(g.hasTacticDraft()&&g.tacticOffers[0]>=0,"room clear prepares tactical inscription offers");g.chooseReward(0);check(g.damageBonus==4&&g.roomCleared,"reward applies once");g.chooseReward(0);check(g.damageBonus==4,"cannot claim reward twice");
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
        check(g.selectTactic(0)&&g.tacticSelected==0&&g.tacticRanks[0]==0&&!g.tacticChosen,"first tap only previews an inscription without spending it");
        ActionGame draftSave=ActionSave.decode(ActionSave.encode(g));check(draftSave.tacticDraftOpen&&draftSave.tacticSelected==0&&draftSave.tacticRanks[0]==0,"pending inscription selection persists safely across save restore");
        check(g.selectTactic(1)&&g.tacticSelected==1&&g.tacticRanks[1]==0,"second card tap changes preview instead of claiming");
        check(g.confirmTactic()&&g.tacticRanks[1]==1&&g.tacticChosen&&!g.tacticDraftOpen&&g.tacticSelected==-1,"explicit confirmation claims exactly one inscription");
        g.openTacticDraft();check(!g.tacticDraftOpen&&!g.selectTactic(0)&&!g.confirmTactic(),"second inscription cannot be claimed in the same room");
        ActionGame saved=ActionSave.decode(ActionSave.encode(g));check(saved.tacticRanks[1]==1&&saved.tacticChosen,"confirmed inscription persists in reward snapshot");
        g=game(0);g.tacticRanks[0]=2;g.enemies.add(new ActionGame.Enemy(1,0,620,600,1000));g.activeSkill();check(g.activeSkillCd>0,"signature technique still uses normal cooldown");
        ActionGame baseline=game(0);baseline.enemies.add(new ActionGame.Enemy(1,0,620,600,1000));baseline.activeSkill();check(1000-g.enemies.get(0).hp>1000-baseline.enemies.get(0).hp,"signature technique increases active skill damage");
        ActionGame radiant=new ActionGame();radiant.bindSoul(1,0);radiant.start(1);radiant.blocks.clear();radiant.enemies.clear();radiant.tacticRanks[2]=2;radiant.hitForTest(new ActionGame.Enemy(1,0,620,600,1000),100);
        ActionGame radiantBase=new ActionGame();radiantBase.bindSoul(1,0);radiantBase.start(1);radiantBase.blocks.clear();radiantBase.enemies.clear();radiantBase.hitForTest(new ActionGame.Enemy(1,0,620,600,1000),100);
        check(radiant.soulEnergy>radiantBase.soulEnergy,"soul current inscription increases energy gained from hits");
        g=game(0);g.tacticRanks[1]=2;g.followUpWindow=1;g.enemies.add(new ActionGame.Enemy(1,0,620,600,1000));g.followUp();float enhanced=1000-g.enemies.get(0).hp;
        baseline=game(0);baseline.followUpWindow=1;baseline.enemies.add(new ActionGame.Enemy(1,0,620,600,1000));baseline.followUp();check(enhanced>1000-baseline.enemies.get(0).hp,"follow-through inscription increases follow-up damage");
        g=game(0);g.tacticRanks[3]=2;near(g.activeSkillPeriod(),baseline.activeSkillPeriod()-2,"cooldown inscription shortens active skill cycle");
        g=game(0);g.tacticRanks[4]=2;ActionGame.Enemy marked=new ActionGame.Enemy(1,0,620,600,1000);marked.hex=3;g.enemies.add(marked);g.hitForTest(marked,100);
        baseline=game(0);marked=new ActionGame.Enemy(1,0,620,600,1000);marked.hex=3;baseline.enemies.add(marked);baseline.hitForTest(marked,100);check(1000-g.enemies.get(0).hp>1000-baseline.enemies.get(0).hp,"curse inscription amplifies marked-target damage");
        g=game(0);g.tacticRanks[5]=2;g.enemies.add(new ActionGame.Enemy(1,0,620,600,1000));g.activeSkill();check(g.shield>=12,"ward circuit grants shield on active skill");
        g=game(0);g.tacticRanks[6]=2;baseline=game(0);check(g.attackPeriod()<baseline.attackPeriod(),"combat tempo increases attack speed");
        g=game(0);g.tacticRanks[7]=2;g.drops.add(new ActionGame.Drop(g.x,g.y,100,false));g.update(1f/60f,new ActionGame.Input());check(g.loot==124,"scavenging inscription increases currency pickup");
        byte[] bytes=ActionSave.encode(g);rejects(bytes,"tacticRank0","4");rejects(bytes,"tacticOffer0","8");
        System.out.println("PASS tactical inscription draft, per-room choice, eight run modifiers and v24 persistence");
    }
    private static void persistence()throws Exception{
        ActionGame g=new ActionGame();g.selectHero(1);g.start(123);g.skill();g.hp-=20;g.potion();
        byte[] bytes=ActionSave.encode(g);ActionGame a=ActionSave.decode(bytes);
        check(a.hero==g.hero&&a.hp==g.hp&&a.potions==g.potions&&a.shots.size()==g.shots.size()&&a.rngState==g.rngState,"save restores action snapshot");
        check(a.random(1000)==g.random(1000),"RNG continues from persisted state");
        rejects(bytes,"version","99");rejects(bytes,"x","NaN");rejects(bytes,"hp","Infinity");rejects(bytes,"hero","3");rejects(bytes,"potions","-1");rejects(bytes,"enemies","500");rejects(bytes,"power","1000000");rejects(bytes,"training0.0","1000000");rejects(bytes,"bank","2147483647");rejects(bytes,"mode","BAD");rejects(bytes,"rng","0");rejects(bytes,"x","1");rejects(bytes,"room","0");rejects(bytes,"format","wayfarer-rpg");
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
    private static void careerSoulResonance(){
        ActionGame radiant=new ActionGame();radiant.bindSoul(1,0);radiant.start(811);radiant.shots.add(new ActionGame.Shot(radiant.x+60,radiant.y,0,0,5,false));radiant.skill();
        check(radiant.soulWard>=12&&radiant.shots.isEmpty(),"radiant career skill grants ward and cleanses nearby hostile shots");
        ActionGame forbidden=new ActionGame();forbidden.bindSoul(2,0);forbidden.start(812);ActionGame.Enemy target=new ActionGame.Enemy(forbidden.nextId++,0,forbidden.x+120,forbidden.y,500);forbidden.enemies.add(target);forbidden.skill();
        check(forbidden.corruption==8&&target.hex>=3,"forbidden career skill builds corruption and marks nearby enemies");
        System.out.println("PASS radiant and forbidden soul resonance with career overdrive");
    }
    private static void careerActiveSoulResonance(){
        int[] jobs={CareerSystem.HUNTER,CareerSystem.ASSASSIN,CareerSystem.DETECTIVE,CareerSystem.REPORTER};
        for(int kind=1;kind<=2;kind++)for(int job:jobs){
            ActionGame g=new ActionGame();g.bindSoul(kind,0);g.careers[g.buildId()][0]=job;g.start(910+kind*10+job);
            g.blocks.clear();g.enemies.clear();g.x=500;g.y=500;g.faceX=1;g.faceY=0;g.invulnerable=0;
            ActionGame.Enemy e=new ActionGame.Enemy(g.nextId++,0,620,500,1200);e.stun=1;g.enemies.add(e);
            if(kind==1){g.soulWard=0;g.hp=g.maxHp()-12;g.shots.add(new ActionGame.Shot(550,500,0,0,5,false));}
            else {g.soulEnergy=10;g.corruption=0;}
            float before=e.hp,hpBefore=g.hp;g.activeSkill();
            check(g.activeSkillCd>0&&g.followUpWindow>0,"career active soul resonance consumes active skill "+kind+"/"+job);
            if(kind==1){
                if(job==CareerSystem.HUNTER)check(g.soulWard>=6&&g.shots.isEmpty(),"radiant hunter converts active skill into ward and cleanse");
                else if(job==CareerSystem.ASSASSIN)check(g.soulWard>=3&&g.invulnerable>=.35f,"radiant assassin gains ward and brief protection");
                else if(job==CareerSystem.DETECTIVE)check(g.soulWard>=4&&g.shots.isEmpty(),"radiant detective gains ward and wider cleanse");
                else check(g.hp>hpBefore&&g.soulWard>=2,"radiant reporter converts resonance into recovery");
            }else{
                if(job==CareerSystem.HUNTER)check(e.hex>=3,"forbidden hunter spreads the mark");
                else if(job==CareerSystem.ASSASSIN)check(g.corruption>=14&&g.shots.size()>=2,"forbidden assassin pays extra debt for bonus knives");
                else if(job==CareerSystem.DETECTIVE)check(e.hp<before,"forbidden detective detonates marked targets");
                else check(g.soulEnergy>10&&g.corruption>=13,"forbidden reporter converts overdrive into soul energy");
            }
        }
        System.out.println("PASS eight career-specific radiant/forbidden active-skill resonances");
    }
    private static void careerFollowUpAndSoulSynergy(){
        int[] jobs={CareerSystem.HUNTER,CareerSystem.ASSASSIN,CareerSystem.DETECTIVE,CareerSystem.REPORTER};
        for(int kind=1;kind<=2;kind++)for(int job:jobs){
            ActionGame f=new ActionGame();f.careers[f.buildId()][0]=job;f.bindSoul(kind,0);f.start(1200+kind*10+job);
            f.blocks.clear();f.enemies.clear();f.x=500;f.y=500;f.faceX=1;f.faceY=0;f.followUpWindow=2;
            ActionGame.Enemy target=new ActionGame.Enemy(f.nextId++,0,700,500,1500);target.stun=1;f.enemies.add(target);
            f.hp=f.maxHp()-10;f.soulEnergy=0;float hpBefore=f.hp,energyBefore=f.soulEnergy;f.followUp();
            check(f.followUpWindow==0&&target.hp<1500,"career follow-up connects "+kind+"/"+job);
            if(job==CareerSystem.HUNTER)check(f.soulBuff>=1.25f,"hunter follow-up primes a short damage window");
            else if(job==CareerSystem.ASSASSIN)check(f.shots.size()>=2,"assassin follow-up emits twin knives");
            else if(job==CareerSystem.DETECTIVE)check(target.hex>=3.5f&&target.stun>=.35f,"detective follow-up marks and controls");
            else check(f.hp>hpBefore-(kind==2?f.maxHp()*.016f:0)&&f.soulEnergy>energyBefore,"reporter follow-up restores health and soul energy");

            ActionGame a=new ActionGame();a.careers[a.buildId()][0]=job;a.bindSoul(kind,0);a.start(1300+kind*10+job);
            a.blocks.clear();a.enemies.clear();a.x=500;a.y=500;a.faceX=1;a.faceY=0;
            ActionGame.Enemy marked=new ActionGame.Enemy(a.nextId++,0,650,500,1500);marked.hex=1;marked.stun=1;a.enemies.add(marked);
            a.hp=a.maxHp()-10;a.soulEnergy=80;a.activeSkillCd=10;float beforeHp=a.hp,cd=a.activeSkillCd,energy=a.soulEnergy;
            a.soulSkill();
            check(a.soulCd>0&&a.soulEnergy>=energy-40,"career soul art still pays its normal resource cost before career refund");
            if(job==CareerSystem.HUNTER)check(a.soulBuff>=1.5f,"hunter soul art primes damage and attack speed");
            else if(job==CareerSystem.ASSASSIN)check(a.activeSkillCd<cd,"assassin soul art accelerates active skill recovery");
            else if(job==CareerSystem.DETECTIVE)check(marked.hex>=2.25f&&marked.stun>=.2f,"detective soul art extends curse and control");
            else check(a.hp>beforeHp-(kind==2?a.maxHp()*.08f:0)&&a.soulEnergy>energy-40,"reporter soul art refunds health and soul energy after paying its base cost");
        }
        System.out.println("PASS eight career/soul combinations across follow-up and dedicated soul arts");
    }
    private static void skillChainReactions(){
        for(int kind=1;kind<=2;kind++)for(int style=0;style<5;style++){
            ActionGame g=new ActionGame();g.selectCustom(0);BuildSystem.Profile draft=g.profile().copy();draft.skill=style;g.customize(0,draft);
            g.bindSoul(kind,0);g.start(2100+kind*10+style);g.blocks.clear();g.enemies.clear();g.x=500;g.y=500;g.faceX=1;g.faceY=0;
            ActionGame.Enemy primary=new ActionGame.Enemy(g.nextId++,0,620,500,2500);primary.stun=1;primary.hex=2;
            ActionGame.Enemy secondary=new ActionGame.Enemy(g.nextId++,0,600,500,2500);secondary.stun=1;secondary.hex=1;
            g.enemies.add(primary);g.enemies.add(secondary);
            g.activeSkill();int shotsBefore=g.shots.size();float secondaryBefore=secondary.hp;
            g.followUp();check(g.followUpWindow==0,"chain consumes follow-up window "+kind+"/"+style);
            if(style==0||style==2||style==4)check(secondary.hp<secondaryBefore,"area chain reaches secondary target "+kind+"/"+style);
            if(style==1)check(g.shots.size()>=shotsBefore+2,"fan follow-up adds converging projectiles "+kind);
            if(style==3)check(secondary.hex>=3||secondary.stun>=.3f,"chain follow-up propagates control "+kind);
            if(kind==1)check(g.soulWard>0,"radiant chain converts confirmed follow-up into ward "+style);
            else check(g.corruption>0&&g.enemies.stream().anyMatch(e->e.hex>0),"forbidden chain preserves curse interaction "+style);
        }
        System.out.println("PASS five active-skill chain reactions across both soul types");
    }
    private static void expandedEconomy(){
        ActionGame g=new ActionGame();g.bindSoul(1,0);g.start(71);g.mode=ActionGame.Mode.REWARD;g.loot=250;g.openShop();g.hp-=30;
        float hp=g.hp;check(g.buyShopItem(3)&&g.hp>hp&&g.loot==212,"medical supply spends tickets and heals immediately");
        g.soulEnergy=10;check(g.buyShopItem(4)&&g.soulEnergy==45&&g.loot==170,"soul crystal spends tickets and restores energy");
        int oldDamage=g.damageBonus;check(g.buyShopItem(5)&&g.damageBonus==oldDamage+4&&g.loot==115,"field modification spends tickets and improves run damage");
        check(g.shopPrice(5)>g.shopPrice(0)&&!g.buyShopItem(6),"expanded shop prices and invalid item guard");
        ActionGame radiant=new ActionGame();radiant.bindSoul(1,0);radiant.start(72);radiant.soulEnergy=80;radiant.soulCd=8;
        check(radiant.soulWardAction()&&radiant.soulEnergy==55&&radiant.soulWard==30,"radiant soul infusion converts energy into ward");
        check(radiant.soulTuneAction()&&radiant.soulEnergy==25&&radiant.soulCd==3,"radiant tuning spends energy to shorten soul cooldown");
        radiant.soulEnergy=35;float boosted=radiant.damage();check(radiant.soulResonanceAction()&&radiant.soulEnergy==0&&radiant.soulBuff==4&&radiant.damage()>boosted,"soul resonance spends energy for a timed combat boost");
        ActionGame forbidden=new ActionGame();forbidden.bindSoul(2,0);forbidden.start(73);forbidden.soulEnergy=60;
        check(forbidden.soulWardAction()&&forbidden.soulEnergy==35&&forbidden.corruption==8&&forbidden.soulWard==24,"forbidden infusion trades corruption for ward");
        forbidden.soulCd=8;check(forbidden.soulTuneAction()&&forbidden.soulEnergy==5&&forbidden.soulCd==4&&forbidden.corruption==12,"forbidden tuning has distinct cost and corruption");
        System.out.println("PASS expanded ticket shop and radiant/forbidden soul energy sinks");
    }
    private static void currencySeparation()throws Exception{
        ActionGame g=new ActionGame();g.bank=500;g.start(701);g.loot=80;
        check(g.bank==500&&g.loot==80,"pickup currency remains isolated from camp bank");
        g.mode=ActionGame.Mode.REWARD;g.openShop();check(g.buyShopItem(0)&&g.loot==56&&g.bank==500,"run shop spends only expedition tickets");
        g.closeShop();g.mode=ActionGame.Mode.PAUSED;g.abandon();check(g.bank==539&&g.loot==56,"defeat settlement transfers only seventy percent of remaining run tickets");
        g.returnCamp();int before=g.bank;check(g.buyFieldPermit(0)&&g.bank==before-80,"permanent expedition permit spends only camp bank");
        g.start(702);check(g.shield==40&&g.loot==0,"guard permit grants its starting shield without adding ticket permit rewards");g.loot=30;g.enemies.clear();
        g.mode=ActionGame.Mode.REWARD;g.openShop();check(g.buyShopItem(0)&&g.loot==6&&g.bank==before-80,"second run shop purchase leaves camp bank untouched");
        ActionGame restored=ActionSave.decode(ActionSave.encode(g));check(restored.bank==before-80&&restored.loot==6&&restored.fieldPermits[0][0]==1,"both currencies and permanent purchases persist independently");
        System.out.println("PASS strict separation of expedition tickets, camp bank, settlement and permit purchases");
    }
    private static void fullPermitFlow()throws Exception{
        ActionGame g=new ActionGame();g.bank=79;check(!g.buyFieldPermit(0)&&g.bank==79,"insufficient camp funds reject permit without debit");g.bank=1000;g.bindSoul(1,0);
        for(int i=0;i<5;i++)check(g.buyFieldPermit(i),"permit purchase "+i);
        check(g.bank==190,"all five permit prices debit camp bank exactly once");
        check(!g.buyFieldPermit(0)&&g.bank==190,"duplicate permanent permit purchase is rejected without debit");
        g.start(703);check(!g.buyFieldPermit(0)&&g.bank==190,"permit purchases are camp-only during an expedition");
        check(g.shield==40&&g.potions==3&&g.loot==30&&g.damageBonus==4&&g.soulEnergy>=20,"all five permits apply their correct run-start effects");
        ActionGame saved=ActionSave.decode(ActionSave.encode(g));check(saved.bank==190&&saved.shield==40&&saved.potions==3&&saved.loot==30&&saved.damageBonus==4,"all permit effects and ledgers persist");
        System.out.println("PASS all five permanent permit purchases, costs, start effects and persistence");
    }
    private static void permanentEconomy()throws Exception{
        ActionGame g=new ActionGame();g.bank=100000;
        for(int i=0;i<20;i++){g.upgrade(false);g.upgrade(true);}
        for(int route=0;route<4;route++)for(int i=0;i<10;i++)check(g.train(route),"permanent training purchase "+route+"/"+i);
        check(g.power==20&&g.vitality==20,"permanent camp upgrades reach level 20");
        for(int route=0;route<4;route++)check(g.training[0][route]==10,"permanent training route reaches level 10");
        int bank=g.bank;int nextCost=g.upgradeCost(g.power);g.upgrade(false);check(g.power==21&&g.bank==bank-nextCost,"shared camp upgrade remains purchasable beyond former level-20 cap");
        int vitalityCost=g.upgradeCost(g.vitality);g.upgrade(true);check(g.vitality==21&&g.bank==bank-nextCost-vitalityCost,"health camp upgrade also remains repeatable");
        for(int route=0;route<4;route++){int cost=g.trainingCost(g.training[0][route]);check(g.train(route)&&g.training[0][route]==11,"training route "+route+" remains purchasable beyond former level-10 cap");}
        ActionGame restored=ActionSave.decode(ActionSave.encode(g));check(restored.power==21&&restored.vitality==21,"repeatable permanent upgrade levels persist");
        for(int route=0;route<4;route++)check(restored.training[0][route]==11,"repeatable training route "+route+" persists");
        System.out.println("PASS repeatable bank-funded camp upgrades and all four training routes beyond former caps");
    }
    private static void legacyTalentBudgetMigration()throws Exception{
        ActionGame legacy=game(0);legacy.experience[0]=400;legacy.talents[0][0]=3;legacy.specialties[0][0]=2;legacy.specialties[0][1]=2;
        byte[] bytes=mutate(ActionSave.encode(legacy),"version","27");
        ActionGame restored=ActionSave.decode(bytes);
        check(BuildSystem.spent(restored.talents[0])==3&&BuildSystem.spent(restored.specialties[0])==4,"legacy v27 shared-point build still loads before migration");
        System.out.println("PASS legacy v27 shared talent-point save migration");
    }
    private static void completeTalentBudgets()throws Exception{
        ActionGame g=new ActionGame();g.selectHero(0);g.experience[0]=1540;
        check(BuildSystem.points(g.heroXp())==18&&BuildSystem.specialtyPoints(g.heroXp())==9&&BuildSystem.corePoints(g.heroXp())==29,"max level grants enough independent points for every tree");
        for(int branch=0;branch<3;branch++){int root=branch*4;for(int i=0;i<3;i++)check(g.learn(root),"base root rank "+root);for(int i=0;i<2;i++)check(g.learn(root+1),"base middle rank "+root);check(g.learn(root+2),"base capstone "+root);}
        check(g.availablePoints()==0,"all 18 base points are spent on the base tree");
        for(int i=0;i<2;i++)check(g.learnSpecialty(0),"soul specialty root");for(int i=0;i<2;i++)check(g.learnSpecialty(1),"soul specialty middle");check(g.learnSpecialty(2),"soul specialty capstone");
        for(int i=0;i<2;i++)check(g.learnSpecialty(4),"hunt specialty root");check(g.learnSpecialty(5),"hunt specialty middle");check(g.learnSpecialty(6),"hunt specialty capstone");
        check(g.availableSpecialtyPoints()==0,"all 9 specialty points can be spent independently");
        for(int branch=0;branch<5;branch++){int root=branch*3;for(int i=0;i<BuildSystem.CORE_NODES[root].maxRank;i++)check(g.learnCoreTalent(root),"core root "+root);for(int i=0;i<BuildSystem.CORE_NODES[root+1].maxRank;i++)check(g.learnCoreTalent(root+1),"core middle "+root);check(g.learnCoreTalent(root+2),"core capstone "+root);}
        check(g.availableCorePoints()==0,"all 29 five-branch points can be spent");
        for(int i=0;i<3;i++)check(g.learnCareerTalent(0),"career root rank "+i);for(int i=0;i<3;i++)check(g.learnCareerTalent(1),"career middle rank "+i);for(int i=0;i<2;i++)check(g.learnCareerTalent(2),"career capstone rank "+i);
        check(g.availableCareerPoints()==0,"all 8 career skill points can be spent");
        ActionGame restored;try{restored=ActionSave.decode(ActionSave.encode(g));}catch(Exception e){throw new AssertionError("full talent save rejected: "+e.getMessage(),e);}
        check(restored.availablePoints()==0&&restored.availableSpecialtyPoints()==0&&restored.availableCorePoints()==0&&restored.availableCareerPoints()==0,"all full talent trees persist and validate");
        System.out.println("PASS full independent point budgets: base 18, specialties 9, five branches 29, career 8");
    }
    private static void threatCap(){
        ActionGame g=new ActionGame();g.startEndless(91);g.endlessDepth=2;float early=ThreatSystem.rating(g);
        g.endlessDepth=80;float late=ThreatSystem.rating(g);check(late>early,"endless enemy threat grows with depth");
        g.endlessDepth=10000;near(ThreatSystem.rating(g),99,"enemy threat reaches the new level-99 cap");
        System.out.println("PASS endless enemy growth scales progressively and caps at 99");
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
    public static void main(String[] args)throws Exception{movement();shieldSaveValidation();shieldSourceSummary();enemyShieldTypes();combat();shopAndItems();currencySeparation();fullPermitFlow();expandedEconomy();careerSoulResonance();careerActiveSoulResonance();careerFollowUpAndSoulSynergy();skillChainReactions();permanentEconomy();legacyTalentBudgetMigration();completeTalentBudgets();threatCap();fiveActiveSkills();roomsAndRewards();tacticalInscriptions();persistence();stress();System.out.println("ACTION CHECKS PASSED: "+count);}
}
