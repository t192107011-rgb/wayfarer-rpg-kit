package io.wayfarer.rpg.core;

import java.util.*;
import static io.wayfarer.rpg.core.CareerSystem.*;

/** Fixed-step, Android-free action simulation. World units are pixels; time is seconds. */
public final class ActionGame {
    public static final float WIDTH=1120, HEIGHT=760, PLAYER_RADIUS=15;
    public enum Mode { CAMP, PLAY, REWARD, PAUSED, RESULT }
    public Mode mode=Mode.CAMP;
    public int hero=0,bank=0,power=0,vitality=0,runs=0,wins=0;
    public final int[] experience=new int[3];
    public int custom=-1;
    public final BuildSystem.Profile[] profiles=new BuildSystem.Profile[3];
    public final int[][] talents=new int[6][12];
    public float shield=0,dodgeCharge=0;
    public final int[][] careers=new int[6][3];
    public float careerWard=0,careerCharge=0;
    public final boolean[] careerPacts=new boolean[6];
    public float pactCd=0,pactTime=0,pactWard=0;
    public boolean pactEnabled(){return careerPacts[buildId()];}
    public boolean pactActive(int job){return job()==job&&pactEnabled()&&pactTime>0;}
    private void clearPactRuntime(){pactCd=0;pactTime=0;pactWard=0;}
    public int job(){return careers[buildId()][0];}
    public int specialization(){return careers[buildId()][1];}
    public int background(){return careers[buildId()][2];}
    public boolean isSpec(int job,int spec){return job()==job&&specialization()==spec;}
    public String careerName(){return CareerSystem.name(job(),specialization());}
    public boolean chooseCareer(int job,int spec,int background){return chooseCareer(job,spec,background,job==job()&&pactEnabled());}
    public boolean chooseCareer(int job,int spec,int background,boolean pact){
        if(mode!=Mode.CAMP)return false;
        String error=CareerSystem.invalid(job,spec,background,heroXp());if(!error.isEmpty()){notice=error;return false;}
        if(pact&&(!ProfessionPacts.available(job)||heroXp()<CareerSystem.SPECIALIZATION_XP)){notice="职业禁术需要对应日常职业与Lv.3，可选择不开启。";return false;}
        careerPacts[buildId()]=pact;clearPactRuntime();careers[buildId()][0]=job;careers[buildId()][1]=spec;careers[buildId()][2]=background;careerWard=0;careerCharge=0;hp=maxHp();notice="历练已更新："+careerName()+" / "+CareerSystem.BACKGROUNDS[background];return true;
    }
    public float careerWardLimit(){return Math.max(entryCareerWard(),isSpec(1,1)?14:isSpec(3,1)?8:0);}
    public float entryCareerWard(){return Math.max(background()==1?10:0,isSpec(MINER,2)?12:0);}
    public float careerChargeLimit(){return isSpec(1,2)?4:isSpec(2,2)?3:0;}
    public float soulPeriod(){return SoulSystem.COOLDOWNS[soulKind()][soulArt()]*(isSpec(3,2)?.9f:1);}
    public float springRatio(){return background()==2?.26f:.18f;}
    public int relicOwned=RelicSystem.STARTERS,relicAwards=0,lastRelic=0;
    public final int[][] relics=new int[6][2];
    public float relicEcho=0;
    public boolean relicRevived=false;
    public boolean hasRelic(int id){return id>0&&(relics[buildId()][0]==id||relics[buildId()][1]==id);}
    public String relicLoadout(){return RelicSystem.NAMES[relics[buildId()][0]]+" / "+RelicSystem.NAMES[relics[buildId()][1]];}
    public boolean equipRelics(int first,int second){
        if(mode!=Mode.CAMP)return false;
        String error=RelicSystem.invalid(relicOwned,first,second);if(!error.isEmpty()){notice=error;return false;}
        relics[buildId()][0]=first;relics[buildId()][1]=second;hp=maxHp();relicEcho=0;notice="圣遗物已装备："+relicLoadout();return true;
    }
    private void clearRelicRun(){relicAwards=0;lastRelic=0;relicEcho=0;relicRevived=false;}
    private void discoverRelic(){
        int bit=1<<(room-1);if((RelicSystem.DISCOVERY_ROOMS&bit)==0||(relicAwards&bit)!=0)return;
        relicAwards|=bit;List<Integer> missing=new ArrayList<>();for(int id=3;id<RelicSystem.NAMES.length;id++)if(!RelicSystem.owns(relicOwned,id))missing.add(id);
        if(missing.isEmpty()){lastRelic=-1;loot=Math.min(99999,loot+15);}
        else {lastRelic=missing.get(random(missing.size()));relicOwned|=1<<lastRelic;}
        signal("relic",x,y,lastRelic);
    }
    public String relicDiscovery(){return lastRelic==0?"":lastRelic<0?"收藏已齐 · 本轮票券 +15":"发现「"+RelicSystem.NAMES[lastRelic]+"」· 已永久收录，回营装备";}
    public final int[] presetSouls=new int[3],presetArts=new int[3];
    public float soulEnergy=0,soulCd=0,soulWard=0,corruption=0,soulQuiet=0,soulBuff=0;
    public final List<SoulField> soulFields=new ArrayList<>();
    public static final class SoulField {
        public int type;public float x,y,life,tick,power;
        public SoulField(int type,float x,float y,float power){this.type=type;this.x=x;this.y=y;this.power=power;life=type==1?5:4;tick=type==1?1:.5f;}
    }
    public int soulKind(){return custom<0?presetSouls[hero]:profile().soul;}
    public int soulArt(){return custom<0?presetArts[hero]:profile().soulArt;}
    public String skillName(){return SoulSystem.SKILLS[soulKind()][skillStyle()];}
    public String soulArtName(){return SoulSystem.ARTS[soulKind()][soulArt()];}
    public boolean bindSoul(int kind,int art){
        if(mode!=Mode.CAMP)return false;SoulSystem.validate(kind,art);
        if(custom<0){presetSouls[hero]=kind;presetArts[hero]=art;}else{profile().soul=kind;profile().soulArt=art;}
        clearSoulRuntime();hp=maxHp();notice="已缔结："+SoulSystem.NAMES[kind];return true;
    }
    private void clearSoulRuntime(){soulEnergy=0;soulCd=0;soulWard=0;corruption=0;soulQuiet=0;soulBuff=0;soulFields.clear();}
    private void gainSoul(float value){if(soulKind()!=0&&mode==Mode.PLAY)soulEnergy=Math.min(100,soulEnergy+value);}
    public float healingFactor(){if(pactActive(DOCTOR))return 0;return (soulKind()==1?1.15f:soulKind()==2?1-corruption*.003f:1)*(hasRelic(4)?1.1f:1)*(isSpec(DOCTOR,2)?1.12f:1);}
    private void heal(float amount){float restored=Math.min(maxHp()-hp,Math.max(0,amount)*healingFactor());hp+=restored;if(restored>.01f)signal("heal",x,y,(int)restored);}
    private void payForbidden(float ratio,float debt){if(hasRelic(6))ratio*=.75f;float before=hp;hp=Math.max(Math.min(1,hp),hp-maxHp()*ratio);corruption=Math.min(100,corruption+debt);soulQuiet=3;signal("sacrifice",x,y,(int)(before-hp));}
    private float swingBoost=1;
    public ActionGame(){for(int i=0;i<3;i++){profiles[i]=new BuildSystem.Profile();profiles[i].name=new String[]{"灰桥旅人","林缘信使","夜班工人"}[i];profiles[i].weapon=i;profiles[i].skill=(i+1)%3;profiles[i].origin=i;profiles[i].trait=i;profiles[i].color=i;}}
    public int buildId(){return custom<0?hero:3+custom;}
    public BuildSystem.Profile profile(){return custom<0?null:profiles[custom];}
    public String heroName(){return custom<0?HERO_NAMES[hero]:profile().name;}
    public int heroXp(){return custom<0?experience[hero]:profile().xp;}
    public int rank(int node){return talents[buildId()][node];}
    public int skillStyle(){return custom<0?hero:profile().skill;}
    public int availablePoints(){return BuildSystem.points(heroXp())-BuildSystem.spent(talents[buildId()]);}
    public boolean learn(int node){if(mode!=Mode.CAMP)return false;String reason=BuildSystem.lockReason(talents[buildId()],heroXp(),node);if(!reason.isEmpty()){notice=reason;return false;}talents[buildId()][node]++;hp=maxHp();notice="习得「"+BuildSystem.NODES[node].name+"」";return true;}
    public void resetTalents(){if(mode==Mode.CAMP){Arrays.fill(talents[buildId()],0);hp=maxHp();shield=0;dodgeCharge=0;notice="天赋已重置，全部点数返还。";}}
    public void selectCustom(int slot){if(mode==Mode.CAMP&&slot>=0&&slot<3){custom=slot;hero=profiles[slot].weapon;hp=maxHp();notice="已选择 "+heroName();}}
    public void customize(int slot,BuildSystem.Profile draft){
        if(mode!=Mode.CAMP||slot<0||slot>=3)throw new IllegalArgumentException("仅能在营地编辑角色");
        BuildSystem.Profile copy=draft.copy();copy.name=BuildSystem.cleanName(copy.name);copy.xp=profiles[slot].xp;BuildSystem.validate(copy);profiles[slot]=copy;selectCustom(slot);
    }
    public float skillPeriod(){return (7-rank(9)*.4f-(custom>=0&&profile().trait==2?.8f:0)+(soulKind()==1?.5f:soulKind()==2?-.6f:0)-(job()==3?.4f:0)-(job()==MECHANIC?.3f:0)-(isSpec(MECHANIC,1)?.5f:0))*(pactActive(MECHANIC)?.45f:1);}
    public float dodgePeriod(){return 1.15f-rank(4)*.07f-(custom>=0&&profile().trait==1?.12f:0)-(hasRelic(2)?.15f:0)-(isSpec(POLICE,1)?.1f:0)-(isSpec(BOXER,2)?.18f:0);}
    public float potionRatio(){return custom>=0&&profile().trait==0?.55f:.45f;}
    private float originHp(){return custom<0?0:profile().origin==0?18:profile().origin==1?-10:-8;}
    public int room=0,loot=0,kills=0,potions=2,damageBonus=0,healthBonus=0,speedBonus=0,combo=0;
    public float x=560,y=620,hp=110,faceX=0,faceY=-1,time=0;
    public float attackCd=0,attackAnim=0,rollTime=0,rollCd=0,skillCd=0,invulnerable=0,comboWindow=0;
    public float rollX=0,rollY=-1,hitStop=0;
    public boolean roomCleared=false,won=false;
    public String notice=WorldSetting.DATE+" · 点击上方事件档案了解封锁线。";
    public long rngState=123456789;
    public int nextId=1;
    public final List<Enemy> enemies=new ArrayList<>();
    public final List<Shot> shots=new ArrayList<>();
    public final List<Block> blocks=new ArrayList<>();
    public final List<Drop> drops=new ArrayList<>();
    public final List<Signal> signals=new ArrayList<>();
    private final Set<Integer> swingHits=new HashSet<>();
    public static final String[] HERO_NAMES={"护路刀手","边地枪手","夜行密探"};
    public static final String[] ROOM_NAMES=WorldSetting.DISTRICTS;
    public static final class Input {public float mx,my;public boolean attack;}
    public static final class Enemy {
        public int id,type,state=0; // 0 approach, 1 windup, 2 recovery/rush
        public float x,y,hp,maxHp,timer=0,aimX=0,aimY=1,stun=0,flash=0;
        public float hex=0;
        public boolean struck=false;
        public Enemy(int id,int type,float x,float y,float hp){this.id=id;this.type=type;this.x=x;this.y=y;this.hp=hp;maxHp=hp;}
        public float radius(){return type==3?30:16;}
    }
    public static final class Shot {
        public float x,y,vx,vy,life,damage,radius;
        public boolean friendly;
        public int effect=0,pierce=0;
        public final Set<Integer> hitIds=new LinkedHashSet<>();
        public Shot(float x,float y,float vx,float vy,float damage,boolean friendly){this.x=x;this.y=y;this.vx=vx;this.vy=vy;this.damage=damage;this.friendly=friendly;life=3;radius=friendly?5:8;}
    }
    public static final class Block {
        public float x,y,w,h;public int style;
        public Block(float x,float y,float w,float h,int style){this.x=x;this.y=y;this.w=w;this.h=h;this.style=style;}
    }
    public static final class Drop {
        public float x,y;public int amount;public boolean heal;
        public Drop(float x,float y,int amount,boolean heal){this.x=x;this.y=y;this.amount=amount;this.heal=heal;}
    }
    public static final class Signal {
        public String kind;public float x,y;public int value;
        public Signal(String kind,float x,float y,int value){this.kind=kind;this.x=x;this.y=y;this.value=value;}
    }
    public int heroLevel(){return 1+Math.min(19,heroXp()/80);}
    public float maxHp(){return (hero==0?110:hero==1?85:90)+vitality*10+healthBonus+(heroLevel()-1)*3+rank(8)*(soulKind()==1?10:8)+originHp()-(custom>=0&&profile().trait==2?8:0)+(hasRelic(1)?12:0)+(job()==1?16:job()==MINER?12:0)+(isSpec(MECHANIC,2)?10:0);}
    public float damage(){float value=(hero==0?22:hero==1?18:15)+power*2+damageBonus+(heroLevel()-1)+rank(0)*2+(custom>=0&&profile().origin==2?2:0)+(job()==SOLDIER?3:0);return value*(rank(3)>0&&hp<maxHp()*.4f?1.4f:1)*(soulBuff>0?1.25f:1);}
    public float speed(){return (hero==2?240:hero==1?215:205)*(1+speedBonus*.08f+rank(4)*.03f)*(custom<0?1:profile().origin==0?.95f:profile().origin==1?1.07f:1)*(job()==2?1.06f:job()==REPORTER?1.03f:1)*(isSpec(POLICE,1)||isSpec(MERCHANT,1)?1.05f:1)*(background()==2?1.03f:1)*(background()==3&&hp<maxHp()*.35f?1.12f:1)*(pactActive(POLICE)?.65f:pactActive(MINER)?.7f:1);}
    public float attackPeriod(){return (hero==0?.48f:hero==1?.4f:.29f)*(custom>=0&&profile().trait==0?1.06f:1)*(soulBuff>0?.8f:1)*(job()==BOXER&&hero!=1?.94f:1)*(isSpec(SOLDIER,2)&&hero==1?.9f:1)*(pactActive(SOLDIER)?.55f:pactActive(MECHANIC)?.85f:pactActive(BOXER)&&hero!=1?.7f:1);}
    public int upgradeCost(int rank){int base=30+rank*25;return isSpec(MERCHANT,2)?(base*85+99)/100:base;}
    public int random(int bound){rngState^=rngState<<13;rngState^=rngState>>>7;rngState^=rngState<<17;return (int)Long.remainderUnsigned(rngState,bound);}
    public void signal(String kind,float x,float y,int amount){if(signals.size()<64)signals.add(new Signal(kind,x,y,amount));}
    public void selectHero(int value){if(mode==Mode.CAMP && value>=0&&value<3){custom=-1;hero=value;hp=maxHp();}}
    public void upgrade(boolean health){
        if(mode!=Mode.CAMP)return;int rank=health?vitality:power;
        if(rank>=10){notice="这项营地升级已满级。";return;}
        int cost=upgradeCost(rank);if(bank<cost){notice="还需要 "+(cost-bank)+" 枚票券。";return;}
        bank-=cost;if(health)vitality++;else power++;hp=maxHp();notice="升级完成，所有角色共同受益。";
    }
    public void start(long seed){
        if(mode!=Mode.CAMP)return;clearSoulRuntime();clearRelicRun();clearPactRuntime();soulEnergy=soulKind()==0?0:40+(hasRelic(5)?(soulKind()==1?30:15):0);rngState=seed==0?1:seed;runs=Math.min(999999,runs+1);
        loot=0;kills=0;nextId=1;room=1;potions=2;damageBonus=0;healthBonus=0;speedBonus=0;combo=0;time=0;won=false;hp=maxHp();
        generateRoom();
    }
    public void generateRoom(){
        mode=Mode.PLAY;roomCleared=false;lastRelic=0;relicEcho=0;careerWard=entryCareerWard();careerCharge=0;x=WIDTH/2;y=HEIGHT-100;faceX=0;faceY=-1;
        attackCd=0;attackAnim=0;rollTime=0;rollCd=0;skillCd=0;shield=0;dodgeCharge=0;swingBoost=1;soulWard=0;soulBuff=0;soulFields.clear();invulnerable=.8f;comboWindow=0;hitStop=0;
        enemies.clear();shots.clear();blocks.clear();drops.clear();signals.clear();swingHits.clear();
        // Keep the central route and both exits connected in every layout.
        for(int i=0;i<6;i++) {
            float bx=(i%2==0?150:730)+random(120),by=180+(i/2)*140+random(25);
            blocks.add(new Block(bx,by,55+random(35),45+random(25),room<3?0:1));
        }
        if(room==5)enemies.add(new Enemy(nextId++,3,560,230,520));
        else {
            int count=3+room;
            for(int i=0;i<count;i++) {
                int type=room==1?0:random(Math.min(3,room));float ex,ey;int tries=0;
                do{ex=130+random(860);ey=150+random(340);tries++;}while(!free(ex,ey,24)&&tries<100);
                if(!free(ex,ey,24)){ex=560;ey=150+i*35;}
                enemies.add(new Enemy(nextId++,type,ex,ey,40+room*10+(type==2?12:0)));
            }
        }
        notice=ROOM_NAMES[room-1]+" · 清除敌人开启出口";signal("enter",x,y,room);
    }
    public void chooseReward(int which){
        if(mode!=Mode.REWARD || which<0||which>2)return;
        if(which==0){damageBonus+=4;notice="锋芒：本轮伤害 +4";}
        if(which==1){healthBonus+=20;heal(35);notice="生息：本轮生命上限 +20，基础恢复 35";}
        if(which==2){speedBonus++;skillCd=0;notice="轻羽：本轮移速 +8%，技能立即就绪";}
        roomCleared=true;mode=Mode.PLAY;signal("clear",x,y,0);
    }
    public void pause(){if(mode==Mode.PLAY){mode=Mode.PAUSED;notice="已暂停";}}
    public void resume(){if(mode==Mode.PAUSED)mode=Mode.PLAY;}
    public void returnCamp(){if(mode==Mode.RESULT){mode=Mode.CAMP;clearSoulRuntime();clearRelicRun();clearPactRuntime();careerWard=0;careerCharge=0;room=0;damageBonus=0;healthBonus=0;speedBonus=0;roomCleared=false;shield=0;dodgeCharge=0;enemies.clear();shots.clear();blocks.clear();drops.clear();hp=maxHp();notice="冒险所得已结算。选择角色，再次出发。";}}
    public void abandon(){if(mode==Mode.PAUSED)finish(false);}
    public void dodge(float dx,float dy){
        if(mode!=Mode.PLAY||rollCd>0||rollTime>0)return;
        float len=len(dx,dy);if(len<.1f){dx=faceX;dy=faceY;len=1;}
        rollX=dx/len;rollY=dy/len;rollTime=.28f;rollCd=dodgePeriod();invulnerable=Math.max(invulnerable,.3f);attackAnim=0;
        dodgeCharge=rank(5)>0?3:0;
        if(rank(6)>0){for(Enemy e:enemies)if(len(e.x-x,e.y-y)<85){hit(e,damage()*.75f);e.stun=Math.max(e.stun,.45f);}signal("echo",x,y,85);}
        if(rank(7)>0)skillCd=Math.max(0,skillCd-2.5f);
        if(hasRelic(3))relicEcho=3;
        if(isSpec(2,2))careerCharge=3;
        signal("roll",x,y,0);
    }
    public void potion(){
        if(mode!=Mode.PLAY)return;
        if(pactActive(DOCTOR)){notice="代命手术的8秒疗养禁忌尚未结束，暂时不能喝药。";signal("pactLow",x,y,0);return;}
        if(mode!=Mode.PLAY||potions<=0||hp>=maxHp()&&!(hasRelic(4)&&soulKind()==2&&corruption>0))return;
        potions--;if(hasRelic(4)){if(soulKind()==1)soulWard=Math.max(soulWard,12);if(soulKind()==2)corruption=Math.max(0,corruption-20);}heal(maxHp()*potionRatio()+(background()==4?6:0)+(job()==DOCTOR?8:0));
    }
    public void skill(){
        if(mode!=Mode.PLAY||skillCd>0||rollTime>0)return;
        if(pactActive(MECHANIC)){float cost=maxHp()*.04f;if(hp<=cost){notice="超载施法需要支付4%最大生命并存活。";signal("pactLow",x,y,0);return;}payPact(cost);}
        aimAtNearest(440);int kind=soulKind(),style=skillStyle();
        if(kind==2)payForbidden(.04f,18);
        skillCd=skillPeriod();signal(kind==1?"radiant":kind==2?"forbidden":"skill",x,y,style);
        shield=Math.max(shield,rank(9)*6+(rank(11)>0?12:0));
        if(kind==1)soulWard=Math.max(soulWard,8+rank(9)*4+(style==2?6:0));
        if(rank(10)>0)heal(maxHp()*.08f);
        if(isSpec(1,1))careerWard=Math.max(careerWard,14);
        if(isSpec(3,1)){careerWard=Math.max(careerWard,8);heal(maxHp()*.03f);}
        if(isSpec(DOCTOR,1))heal(maxHp()*.04f);
        float activeDamage=damage()*(isSpec(3,2)?1.15f:1);
        if(style==1){
            double base=Math.atan2(faceY,faceX);int half=kind==1?1:kind==2?3:2;
            for(int i=-half;i<=half;i++)fire((float)Math.cos(base+i*(kind==1?.18:.14)),(float)Math.sin(base+i*(kind==1?.18:.14)),activeDamage*(kind==1?1.5f:kind==2?1.05f:1.3f),kind,kind==1?2:0);
        }else{
            float range=style==0?(kind==1?195:kind==2?220:160):(kind==1?140:kind==2?150:125);
            float multiplier=style==0?(kind==1?1.65f:kind==2?1.5f:1.8f):(kind==1?1.8f:kind==2?2.6f:2.2f);
            for(Enemy e:enemies)if(e.hp>0&&len(e.x-x,e.y-y)<range){
                hit(e,activeDamage*multiplier);e.stun=kind==1?.9f:.7f;
                if(kind==2){e.hex=3;if(style==0)pull(e,x,y,55);}
            }
            if(kind==1)cleanse(x,y,range);
            if(style==2)invulnerable=Math.max(invulnerable,kind==1?.85f:kind==2?.35f:.65f);
            hitStop=.045f;
        }
    }
    private void payPact(float amount){if(amount>0){hp-=amount;signal("sacrifice",x,y,(int)amount);}}
    public boolean professionPact(){
        if(mode!=Mode.PLAY||!pactEnabled()||!ProfessionPacts.available(job())||roomCleared||enemies.isEmpty()||rollTime>0)return false;
        if(pactCd>0){notice="职业禁术尚在冷却。";signal("pactLow",x,y,0);return false;}
        float cost=maxHp()*ProfessionPacts.COST[job()];
        if(hp<=cost||pactActive(DOCTOR)){notice="生命不足以支付禁术代价并存活。";signal("pactLow",x,y,0);return false;}
        if(job()==BOXER&&hero==1){notice="断骨搏命需要军刀或双匕首。";signal("pactLow",x,y,0);return false;}
        if(job()==DOCTOR&&hp>=maxHp()){notice="生命已满，无需发动代命手术。";signal("pactLow",x,y,0);return false;}
        if(job()==MERCHANT&&loot<30){notice="血本交易需要本轮30票券，共享存款不会扣除。";signal("pactLow",x,y,0);return false;}
        payPact(cost);
        // Restore before installing the doctor's no-healing debt.
        if(job()==DOCTOR)heal(maxHp()*.55f);
        if(job()==MERCHANT){loot-=30;heal(maxHp()*.30f);}
        pactCd=ProfessionPacts.COOLDOWN[job()];pactTime=ProfessionPacts.DURATION[job()];
        if(job()==POLICE)for(Enemy e:enemies)if(e.hp>0&&len(e.x-x,e.y-y)<260)e.stun=Math.max(e.stun,e.type==3?.15f:1);
        if(job()==MECHANIC)skillCd=0;
        if(job()==MINER){pactWard=35;for(Enemy e:enemies)if(e.hp>0&&len(e.x-x,e.y-y)<210)hit(e,damage()*3.2f,false);}
        if(job()==REPORTER)for(Enemy e:enemies)if(e.hp>0&&len(e.x-x,e.y-y)<400){e.stun=Math.max(e.stun,e.type==3?.15f:.8f);if(e.type!=3){e.state=0;e.timer=0;e.struck=false;}}
        notice=ProfessionPacts.LINES[job()];signal("pact",x,y,job());return true;
    }
    public String pactStatus(){
        if(pactTime<=0)return pactCd>0?"禁术冷却 "+(int)Math.ceil(pactCd)+"秒":"禁术可用";
        String debt=job()==POLICE?"减伤50% / 移速−35%":job()==SOLDIER?"增伤30% / 受伤+40%":job()==DOCTOR?"禁止治疗 / 受伤+25%":job()==MECHANIC?"技能加速 / 每次付4%生命":job()==MINER?"护盾"+(int)pactWard+" / 移速−30%":job()==BOXER?"近战增伤80% / 受伤+50%":job()==MERCHANT?"增伤50% / 受伤+25%":"增伤60% / 受伤+35%";
        return (int)Math.ceil(pactTime)+"秒 · "+debt;
    }
    public void soulSkill(){
        if(mode!=Mode.PLAY||soulKind()==0||soulCd>0||rollTime>0||roomCleared||enemies.isEmpty())return;
        if(soulEnergy<40){notice="专属技能需要40魂能，命中敌人可积累。";signal("soulLow",x,y,0);return;}
        int kind=soulKind(),art=soulArt();soulEnergy-=40;soulCd=soulPeriod();aimAtNearest(520);
        if(kind==2)payForbidden(.08f,30);
        float power=damage();
        if(kind==1&&art==0){soulWard=Math.max(soulWard,30);soulFields.add(new SoulField(1,x,y,power));signal("radiant",x,y,175);}
        else if(kind==1){
            soulWard=Math.max(soulWard,12);float endX=x+faceX*520,endY=y+faceY*520;
            for(Enemy e:enemies)if(e.hp>0&&segmentDistance(e.x,e.y,x,y,endX,endY)<28+e.radius()&&lineClear(x,y,e.x,e.y)){hit(e,power*3.6f,false);e.stun=Math.max(e.stun,e.type==3?.15f:.6f);}
            shots.removeIf(p->!p.friendly&&segmentDistance(p.x,p.y,x,y,endX,endY)<35&&lineClear(x,y,p.x,p.y));
            signal("lance",x,y,(int)Math.toDegrees(Math.atan2(faceY,faceX)));
        }else if(art==0){
            float fx=Math.max(60,Math.min(WIDTH-60,x+faceX*120)),fy=Math.max(60,Math.min(HEIGHT-60,y+faceY*120));
            soulFields.add(new SoulField(2,fx,fy,power));signal("forbidden",fx,fy,185);
        }else{
            soulBuff=6;
            for(Enemy e:enemies)if(e.hp>0&&len(e.x-x,e.y-y)<160){hit(e,power*1.2f,false);e.hex=3;}
            signal("forbidden",x,y,160);
        }
    }
    private void cleanse(float cx,float cy,float radius){shots.removeIf(p->!p.friendly&&len(p.x-cx,p.y-cy)<radius);}
    private void pull(Enemy e,float cx,float cy,float distance){
        float dx=cx-e.x,dy=cy-e.y,d=len(dx,dy);if(d<45||e.hp<=0)return;
        float length=Math.min(d-45,distance)*(e.type==3?.2f:1);int steps=Math.max(1,(int)Math.ceil(length/6));
        for(int i=0;i<steps;i++){float mx=dx/d*length/steps,my=dy/d*length/steps;if(free(e.x+mx,e.y,e.radius()))e.x+=mx;if(free(e.x,e.y+my,e.radius()))e.y+=my;}
    }
    private void updateSoul(float dt){
        soulCd=dec(soulCd,dt);soulBuff=dec(soulBuff,dt);
        float quiet=Math.min(soulQuiet,dt);soulQuiet=dec(soulQuiet,dt);if(soulKind()==2)corruption=Math.max(0,corruption-(dt-quiet)*4);
        Iterator<SoulField> it=soulFields.iterator();while(it.hasNext()){
            SoulField f=it.next();float elapsed=Math.min(dt,f.life);f.life=Math.max(0,f.life-elapsed);f.tick-=elapsed;
            if(f.tick<=.0001f){
                f.tick+=f.type==1?1:.5f;
                for(Enemy e:enemies)if(e.hp>0&&len(e.x-f.x,e.y-f.y)<(f.type==1?175:185)){
                    if(f.type==2){pull(e,f.x,f.y,40);e.hex=3;}hit(e,f.power*(f.type==1?.35f:.5f),false);
                }
                if(f.type==1){cleanse(f.x,f.y,175);if(len(x-f.x,y-f.y)<175)heal(maxHp()*.02f);}
                signal(f.type==1?"radiant":"forbidden",f.x,f.y,f.type==1?175:185);
            }
            if(f.life<=0)it.remove();
        }
    }
    private void beginAttack(){
        aimAtNearest(hero==1?500:150);attackCd=attackPeriod();attackAnim=hero==1?.18f:.24f;
        combo=comboWindow>0?(combo+1)%3:0;comboWindow=.85f;swingHits.clear();
        swingBoost=(dodgeCharge>0?1+rank(5)*(soulKind()==2?.20f:.15f):1)*(combo==2?1.35f+rank(1)*(soulKind()==2?.18f:.12f)+(isSpec(BOXER,1)&&hero!=1?.25f:0):1);dodgeCharge=0;
        if(combo==2&&rank(2)>0){for(Enemy e:enemies)if(len(e.x-x,e.y-y)<140)hit(e,damage()*.6f);signal("echo",x,y,140);}
        if(hero==1)fire(faceX,faceY,damage()*swingBoost);signal(hero==1?"bow":"swing",x,y,combo);
    }
    private void fire(float dx,float dy,float value){fire(dx,dy,value,0,0);}
    private void fire(float dx,float dy,float value,int effect,int pierce){if(shots.size()<160){Shot s=new Shot(x+dx*20,y+dy*20,dx*640,dy*640,value,true);s.effect=effect;s.pierce=pierce;shots.add(s);}}
    private void aimAtNearest(float range){
        Enemy nearest=null;float best=range;
        for(Enemy e:enemies){float distance=len(e.x-x,e.y-y);if(e.hp>0&&distance<best&&lineClear(x,y,e.x,e.y)){best=distance;nearest=e;}}
        if(nearest!=null&&best>.01f){faceX=(nearest.x-x)/best;faceY=(nearest.y-y)/best;}
    }
    public void update(float dt,Input in){
        if(mode!=Mode.PLAY)return;
        if(!Float.isFinite(dt)||dt<=0)return;dt=Math.min(dt,.05f);
        if(hitStop>0){hitStop=Math.max(0,hitStop-dt);return;}
        pactCd=dec(pactCd,dt);pactTime=dec(pactTime,dt);if(pactTime<=0)pactWard=0;
        updateSoul(dt);relicEcho=dec(relicEcho,dt);careerCharge=dec(careerCharge,dt);time+=dt;attackCd=dec(attackCd,dt);rollCd=dec(rollCd,dt);skillCd=dec(skillCd,dt);invulnerable=dec(invulnerable,dt);comboWindow=dec(comboWindow,dt);dodgeCharge=dec(dodgeCharge,dt);
        float mx=Float.isFinite(in.mx)?in.mx:0,my=Float.isFinite(in.my)?in.my:0,mag=len(mx,my);
        if(mag>1){mx/=mag;my/=mag;mag=1;}
        if(rollTime>0){movePlayer(rollX*530*dt,rollY*530*dt);rollTime=dec(rollTime,dt);}
        else {
            if(mag>.1f){if(attackAnim<=0){faceX=mx/mag;faceY=my/mag;}float slow=attackAnim>0?.65f:1;movePlayer(mx*speed()*slow*dt,my*speed()*slow*dt);}
            if(in.attack&&attackCd<=0)beginAttack();
        }
        if(attackAnim>0){
            attackAnim=dec(attackAnim,dt);
            if(hero!=1&&attackAnim<.18f&&attackAnim>.04f){
                float range=hero==0?90:72,cos=hero==0?-.05f:.1f;
                for(Enemy e:enemies){float dx=e.x-x,dy=e.y-y,d=len(dx,dy);
                    if(e.hp>0&&!swingHits.contains(e.id)&&d<range+e.radius()&&(d<.01f||(dx*faceX+dy*faceY)/d>cos)&&lineClear(x,y,e.x,e.y)){
                        swingHits.add(e.id);hit(e,damage()*swingBoost);hitStop=.025f;
                    }
                }
            }
        }
        for(Enemy e:enemies)if(e.hp>0)updateEnemy(e,dt);
        if(mode!=Mode.PLAY)return;
        updateShots(dt);if(mode!=Mode.PLAY)return;
        Iterator<Enemy> it=enemies.iterator();while(it.hasNext()){
            Enemy e=it.next();if(e.hp<=0){it.remove();kills=Math.min(99999,kills+1);drops.add(new Drop(e.x,e.y,(e.type==3?55:5+room)+(background()==6?2:0)+(job()==MERCHANT?1:0),false));if(background()==5){heal(1);gainSoul(e.type==3?8:2);}if(random(6)==0)drops.add(new Drop(e.x+15,e.y,15,true));signal("defeat",e.x,e.y,e.type);}
        }
        Iterator<Drop> d=drops.iterator();while(d.hasNext()){
            Drop p=d.next();float dist=len(p.x-x,p.y-y);if(dist<100&&dist>1){float speed=270*dt;p.x+=(x-p.x)/dist*Math.min(speed,dist);p.y+=(y-p.y)/dist*Math.min(speed,dist);}
            if(dist<24){if(p.heal)heal(p.amount);else loot=Math.min(99999,loot+p.amount);if(!p.heal)signal("coin",p.x,p.y,p.amount);d.remove();}
        }
        if(enemies.isEmpty()&&!roomCleared){
            for(Drop p:drops){if(p.heal)heal(p.amount);else loot=Math.min(99999,loot+p.amount);}drops.clear();shots.clear();
            discoverRelic();if(room==5)finish(true);else mode=Mode.REWARD;
        }
        if(roomCleared&&y<100){
            if(x<430){float recovery=springRatio();heal(maxHp()*recovery);room++;generateRoom();notice="穿过补给所，基础恢复"+Math.round(recovery*100)+"%生命（受治疗修正）。";}
            else if(x>690){loot=Math.min(99999,loot+12);room++;generateRoom();notice="穿过货栈，额外获得 12 票券。";}
        }
    }
    private void updateEnemy(Enemy e,float dt){
        e.hex=dec(e.hex,dt);e.flash=dec(e.flash,dt);if(e.stun>0){e.stun=dec(e.stun,dt);return;}
        float dx=x-e.x,dy=y-e.y,distance=Math.max(.01f,len(dx,dy));
        if(e.state==0){
            float reach=e.type==0?56:e.type==1?330:e.type==2?260:210;
            if(distance<reach&&lineClear(e.x,e.y,x,y)){
                e.state=1;e.timer=e.type==0?.58f:e.type==1?.9f:e.type==2?.85f:1.2f;e.aimX=dx/distance;e.aimY=dy/distance;e.struck=false;
            }else moveEnemy(e,dx/distance*(e.type==3?65:90+room*3)*dt,dy/distance*(e.type==3?65:90+room*3)*dt);
        } else if(e.state==1){
            e.timer-=dt;
            if(e.timer<=0){
                e.state=2;
                if(e.type==0){e.timer=.7f;if(distance<72)hurt(10+room*2);signal("enemySwing",e.x,e.y,0);}
                else if(e.type==1){e.timer=1.1f;enemyShot(e,e.aimX,e.aimY,220,10+room*2);}
                else if(e.type==2)e.timer=.6f;
                else {
                    e.timer=1.3f;signal("slam",e.x,e.y,0);if(distance<185)hurt(24);
                    int count=e.hp<e.maxHp*.5f?14:10;
                    for(int i=0;i<count;i++){double a=i*Math.PI*2/count+time*.1;enemyShot(e,(float)Math.cos(a),(float)Math.sin(a),170,15);}
                }
            }
        }else{
            e.timer-=dt;
            if(e.type==2){moveEnemy(e,e.aimX*380*dt,e.aimY*380*dt);if(!e.struck&&len(x-e.x,y-e.y)<35){e.struck=true;hurt(16+room*2);}}
            if(e.timer<=0)e.state=0;
        }
        // Gentle separation avoids enemies stacking into an unreadable single hitbox.
        for(Enemy other:enemies)if(other!=e&&other.hp>0){float sx=e.x-other.x,sy=e.y-other.y,d=len(sx,sy),min=e.radius()+other.radius();if(d>.1&&d<min)moveEnemy(e,sx/d*22*dt,sy/d*22*dt);}
    }
    private void enemyShot(Enemy e,float dx,float dy,float speed,float damage){if(shots.size()<160)shots.add(new Shot(e.x+dx*24,e.y+dy*24,dx*speed,dy*speed,damage,false));}
    private void updateShots(float dt){
        Iterator<Shot> it=shots.iterator();while(it.hasNext()){
            Shot p=it.next();p.life-=dt;float ox=p.x,oy=p.y,nx=p.x+p.vx*dt,ny=p.y+p.vy*dt;
            boolean gone=p.life<=0||!free(nx,ny,p.radius)||!lineClear(ox,oy,nx,ny);
            if(!gone&&p.friendly){for(Enemy e:enemies)if(e.hp>0&&!p.hitIds.contains(e.id)&&segmentDistance(e.x,e.y,ox,oy,nx,ny)<e.radius()+p.radius){
                p.hitIds.add(e.id);hit(e,p.damage);if(p.effect==2)e.hex=3;
                if(p.pierce<=0){gone=true;break;}p.pierce--;
            }}
            else if(!gone&&segmentDistance(x,y,ox,oy,nx,ny)<PLAYER_RADIUS+p.radius){hurt(p.damage);gone=true;}
            p.x=nx;p.y=ny;if(gone)it.remove();
        }
    }
    private void hit(Enemy e,float value){hit(e,value,true);}
    private void hit(Enemy e,float value,boolean charge){hit(e,value,charge,charge);}
    private void hit(Enemy e,float value,boolean charge,boolean echoEligible){
        if(e.hp<=0)return;
        if(echoEligible){
            value*=pactActive(SOLDIER)?1.3f:pactActive(BOXER)&&hero!=1?1.8f:pactActive(MERCHANT)?1.5f:pactActive(REPORTER)?1.6f:1;
            if(isSpec(POLICE,2)&&e.hp<e.maxHp*.5f)value*=1.18f;
            if(isSpec(SOLDIER,1)&&len(e.x-x,e.y-y)<140)value*=1.12f;
            if(isSpec(MINER,1)&&hero!=1)value*=1.12f;
            if(isSpec(REPORTER,1)&&e.type==3)value*=1.15f;
        }
        if(echoEligible&&isSpec(2,1)&&len(e.x-x,e.y-y)>200)value*=1.18f;
        if(echoEligible&&careerCharge>0&&careerChargeLimit()>0){value*=1.25f;careerCharge=0;signal("echo",e.x,e.y,80);}
        if(echoEligible&&relicEcho>0&&hasRelic(3)){value*=1.35f;relicEcho=0;signal("echo",e.x,e.y,65);}
        value*=e.hex>0?1.15f*(hasRelic(6)?1.1f:1):1;float dealt=Math.min(e.hp,value);e.hp=Math.max(0,e.hp-value);
        e.flash=.12f;e.stun=Math.max(e.stun,e.type==3?.03f:.12f);signal("hit",e.x,e.y,(int)value);
        if(charge&&dealt>0){gainSoul(Math.min(4,dealt*.08f));if(soulBuff>0)heal(Math.min(maxHp()*.015f,dealt*.08f));}
    }
    private void hurt(float value){
        if(invulnerable>0||mode!=Mode.PLAY)return;
        value*= (custom>=0&&profile().trait==1?1.12f:1)*(rank(3)>0?1.15f:1)*(soulKind()==2?1+corruption*.004f:1)*(job()==1?.92f:job()==POLICE||isSpec(MECHANIC,2)?.95f:1)*(pactActive(POLICE)?.5f:pactActive(SOLDIER)?1.4f:pactActive(BOXER)?1.5f:pactActive(DOCTOR)||pactActive(MERCHANT)?1.25f:pactActive(REPORTER)?1.35f:1);
        float wardTaken=Math.min(soulWard,value);soulWard-=wardTaken;value-=wardTaken;
        float pactTaken=Math.min(pactWard,value);pactWard-=pactTaken;value-=pactTaken;
        float careerTaken=Math.min(careerWard,value);careerWard-=careerTaken;value-=careerTaken;
        float talentTaken=Math.min(shield,value);shield-=talentTaken;value-=talentTaken;float absorbed=talentTaken+wardTaken+careerTaken+pactTaken;
        if(soulKind()==1&&absorbed>0)gainSoul(Math.min(6,absorbed*.2f));
        if(absorbed>0){signal("shield",x,y,(int)absorbed);if(rank(11)>0)for(Enemy e:enemies)if(len(e.x-x,e.y-y)<100)hit(e,absorbed*.5f,true,false);}
        float lost=Math.min(hp,value);hp=Math.max(0,hp-value);invulnerable=.65f;signal("hurt",x,y,(int)lost);
        if(lost>0&&isSpec(1,2))careerCharge=4;
        if(hasRelic(7)&&lost>0){for(Enemy e:enemies)if(e.hp>0&&len(e.x-x,e.y-y)<120)hit(e,lost*.25f,false);signal("echo",x,y,120);}
        if(hp<=0&&hasRelic(8)&&!relicRevived){relicRevived=true;hp=maxHp()*.25f;invulnerable=1;signal("revive",x,y,0);}
        if(hp<=0)finish(false);
    }
    private void finish(boolean victory){
        if(mode==Mode.RESULT||mode==Mode.CAMP)return;
        won=victory;int reward=victory?loot+40:(int)(loot*.7f);bank=Math.min(999999,bank+reward);
        int gained=kills*6+(victory?50:10);if(isSpec(REPORTER,2))gained=gained*120/100;
        if(custom<0)experience[hero]=Math.min(1000000,experience[hero]+gained);else profile().xp=Math.min(1000000,profile().xp+gained);
        if(victory)wins=Math.min(999999,wins+1);mode=Mode.RESULT;notice=(victory?"长夜已尽":"灯火尚存")+" · 带回 "+reward+" 票券";signal(victory?"victory":"death",x,y,reward);
    }
    private void movePlayer(float dx,float dy){if(free(x+dx,y,PLAYER_RADIUS))x+=dx;if(free(x,y+dy,PLAYER_RADIUS))y+=dy;}
    private void moveEnemy(Enemy e,float dx,float dy){
        float ox=e.x,oy=e.y;
        if(free(e.x+dx,e.y,e.radius()))e.x+=dx;if(free(e.x,e.y+dy,e.radius()))e.y+=dy;
        if(e.state!=0||len(e.x-ox,e.y-oy)>.1f)return;
        // Sidestep the nearest blocking edge rather than pushing forever into a pillar.
        float step=Math.max(Math.abs(dx),Math.abs(dy));
        for(Block b:blocks){
            if(e.x>=b.x-e.radius()-5&&e.x<=b.x+b.w+e.radius()+5&&e.y>=b.y-e.radius()-5&&e.y<=b.y+b.h+e.radius()+5){
                if(Math.abs(dx)>=Math.abs(dy)){float sy=e.y<b.y+b.h/2?-step:step;if(free(e.x,e.y+sy,e.radius()))e.y+=sy;}
                else {float sx=e.x<b.x+b.w/2?-step:step;if(free(e.x+sx,e.y,e.radius()))e.x+=sx;}
                break;
            }
        }
    }
    public boolean free(float px,float py,float radius){
        if(px-radius<48||py-radius<48||px+radius>WIDTH-48||py+radius>HEIGHT-48)return false;
        for(Block b:blocks){float cx=Math.max(b.x,Math.min(px,b.x+b.w)),cy=Math.max(b.y,Math.min(py,b.y+b.h));if(len(px-cx,py-cy)<radius)return false;}
        return true;
    }
    public boolean lineClear(float ax,float ay,float bx,float by){
        int steps=Math.max(1,(int)(len(bx-ax,by-ay)/8));for(int i=0;i<=steps;i++){float t=i/(float)steps;if(!free(ax+(bx-ax)*t,ay+(by-ay)*t,1))return false;}return true;
    }
    public static float segmentDistance(float px,float py,float ax,float ay,float bx,float by){float dx=bx-ax,dy=by-ay,q=dx*dx+dy*dy;if(q<.0001f)return len(px-ax,py-ay);float t=Math.max(0,Math.min(1,((px-ax)*dx+(py-ay)*dy)/q));return len(px-ax-t*dx,py-ay-t*dy);}
    public static float len(float x,float y){return (float)Math.sqrt(x*x+y*y);}
    private static float dec(float value,float dt){return Math.max(0,value-dt);}
    /** A restored partial swing is cancelled without granting a second hit. */
    public void settleRestoredAttack(){attackAnim=0;swingHits.clear();}
}
