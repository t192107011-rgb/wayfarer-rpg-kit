package io.wayfarer.rpg.core;

import java.util.*;
import static io.wayfarer.rpg.core.CareerSystem.*;

/** Fixed-step, Android-free action simulation. World units are pixels; time is seconds. */
public final class ActionGame {
    public static final float WIDTH=1120, HEIGHT=760, PLAYER_RADIUS=15;
    public enum Mode { CAMP, PLAY, REWARD, PAUSED, RESULT }
    public Mode mode=Mode.CAMP;
    public int hero=0,bank=0,power=0,vitality=0,runs=0,wins=0;
    public static final int MAX_POTIONS=5;
    public boolean endless=false;
    public int endlessDepth=0,bestDepth=0;
    public float threat=1,pressure=0,encounterHpFloor=0,encounterDamageFloor=0,roomDamage=0,roomElapsed=0;
    public float enemyRange(float base){return base*(1+Math.min(.4f,(threat-1)*.04f));}
    public float enemySpeed(float base){return base*(1+Math.min(.75f,(threat-1)*.08f));}
    public float enemyPower(Enemy e,float base){return Math.max(base*threat,encounterDamageFloor)*enemyForce(e);}
    public float reachFactor(){return rank(3)>0&&hp<maxHp()*.4f?1.25f:1;}
    public float meleeReach(){return (hero==0?120:100)*reachFactor();}
    public float activeRadius(){int k=soulKind();return (skillStyle()==0?(k==1?235:k==2?260:200):(k==1?180:k==2?190:160))*reachFactor();}
    public float followUpDuration(){return rank(7)>0?3:2;}
    /** Persistent talent shield has only a safety cap; it does not decay by time or room. */
    public float talentShieldLimit(){return maxHp();}
    public String threatLabel(){return String.format(java.util.Locale.ROOT,"敌势 ×%.2f",threat);}
    private void clearThreat(){threat=1;pressure=0;encounterHpFloor=0;encounterDamageFloor=0;roomDamage=0;roomElapsed=0;endless=false;endlessDepth=0;}
    private boolean collectDrop(Drop d){if(d.potion){if(potions>=MAX_POTIONS)return false;potions++;signal("potion",d.x,d.y,potions);notice="拾取药瓶 · "+potions+" / "+MAX_POTIONS;}else if(d.heal)heal(d.amount);else{loot=Math.min(99999,loot+d.amount);signal("coin",d.x,d.y,d.amount);}return true;}
    private void advanceRoom(){if(endless){endlessDepth=Math.min(999999,endlessDepth+1);room=(endlessDepth-1)%5+1;}else room++;generateRoom();}

    public final int[] experience=new int[3];
    public int custom=-1;
    public final BuildSystem.Profile[] profiles=new BuildSystem.Profile[3];
    public final int[][] talents=new int[6][12];
    public float shield=0,dodgeCharge=0;
    public final int[][] careers=new int[6][3];
    public float careerWard=0;
    // Career active skill runtime. Mode: 1 hunter, 2 assassin, 3 detective, 4 reporter.
    public float careerSkillTime=0,careerSkillDeathCd=0;
    public int careerSkillMode=0;
    public int job(){return careers[buildId()][0];}
    public int specialization(){return careers[buildId()][1];}
    public int background(){return careers[buildId()][2];}
    public boolean isSpec(int job,int spec){return job()==job&&specialization()==spec;}
    public String careerName(){return CareerSystem.name(job(),specialization());}
    public boolean chooseCareer(int job,int spec,int background){
        if(mode!=Mode.CAMP)return false;
        String error=CareerSystem.invalid(job,spec,background,heroXp());if(!error.isEmpty()){notice=error;return false;}
        careers[buildId()][0]=job;careers[buildId()][1]=spec;careers[buildId()][2]=background;careerWard=0;hp=maxHp();notice="职业已更换："+careerName()+" / "+CareerSystem.BACKGROUNDS[background];return true;
    }
    public float careerWardLimit(){return entryCareerWard();}
    public float entryCareerWard(){return background()==1?10:0;}
    public float soulPeriod(){return SoulSystem.COOLDOWNS[soulKind()][soulArt()];}
    public float springRatio(){return background()==2?.26f:.18f;}
    public int storyUnlocked=0;
    public final int[] storyChoices=new int[5],characterUnlocked=new int[6],characterJobs={-1,-1,-1,-1,-1,-1};
    public final int[][] characterChoices=new int[6][3];
    public int characterTrait(int choice){int n=0;for(int c:characterChoices[buildId()])if(c==choice)n++;return n;}
    private void unlockStory(){storyUnlocked|=1<<(room-1);int id=buildId();if(characterJobs[id]<0)characterJobs[id]=job();characterUnlocked[id]|=room>=5?7:room>=3?3:1;}
    public boolean decideStory(int scene,int choice){
        if(mode==Mode.PLAY||scene<0||scene>=5||choice<1||choice>2||(storyUnlocked&(1<<scene))==0||storyChoices[scene]!=0)return false;
        storyChoices[scene]=choice;if(choice==1)bank=Math.min(999999,bank+12);else gainExperience(20);
        notice="已记录："+StorySystem.OPTIONS[scene][choice-1];return true;
    }
    public boolean decideCharacter(int chapter,int choice){
        int id=buildId();if(mode!=Mode.CAMP||chapter<0||chapter>=3||choice<1||choice>2||(characterUnlocked[id]&(1<<chapter))==0||characterChoices[id][chapter]!=0)return false;
        characterChoices[id][chapter]=choice;hp=maxHp();notice=choice==1?"人物历练：守护 +4生命上限":"人物历练：决意 +1基础伤害";return true;
    }
    public int unreadStories(){int n=0;for(int i=0;i<5;i++)if((storyUnlocked&(1<<i))!=0&&storyChoices[i]==0)n++;for(int i=0;i<3;i++)if((characterUnlocked[buildId()]&(1<<i))!=0&&characterChoices[buildId()][i]==0)n++;return n;}
    private void gainExperience(int amount){if(custom<0)experience[hero]=Math.min(1000000,experience[hero]+amount);else profile().xp=Math.min(1000000,profile().xp+amount);}
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
    public String skillName(){return CareerSystem.SKILL_NAMES[job()];}
    public String soulArtName(){return SoulSystem.ARTS[soulKind()][soulArt()];}
    public boolean bindSoul(int kind,int art){
        if(mode!=Mode.CAMP)return false;SoulSystem.validate(kind,art);
        if(custom<0){presetSouls[hero]=kind;presetArts[hero]=art;}else{profile().soul=kind;profile().soulArt=art;}
        clearSoulRuntime();hp=maxHp();notice="已缔结："+SoulSystem.NAMES[kind];return true;
    }
    private void clearSoulRuntime(){soulEnergy=0;soulCd=0;soulWard=0;corruption=0;soulQuiet=0;soulBuff=0;ruptureTime=0;ruptureTick=0;ruptureLatched=false;soulFields.clear();}
    private void gainSoul(float value){if(soulKind()!=0&&mode==Mode.PLAY)soulEnergy=Math.min(100,soulEnergy+value);}
    public float healingFactor(){return ordinaryHealingFactor();}
    private float ordinaryHealingFactor(){return (job()==REPORTER&&careerSkillMode==4&&careerSkillTime>0?.2f:1)*(soulKind()==1?1.15f:soulKind()==2?1-corruption*.003f:1)*(hasRelic(4)?1.1f:1);}
    private void heal(float amount){float restored=Math.min(maxHp()-hp,Math.max(0,amount)*healingFactor());hp+=restored;if(restored>.01f)signal("heal",x,y,(int)restored);}
    private void payForbidden(float ratio,float debt){if(hasRelic(6))ratio*=.75f;float before=hp;hp=Math.max(Math.min(1,hp),hp-maxHp()*ratio);corruption=Math.min(100,corruption+debt);triggerRupture();signal("sacrifice",x,y,(int)(before-hp));}
    public float followUpWindow=0,ruptureTime=0,ruptureTick=0;
    public boolean ruptureLatched=false;
    private float motionX=0,motionY=0;
    public final List<Hazard> hazards=new ArrayList<>();
    public static final class Hazard {
        public int type;public float x,y,radius,warning,life,tick,damage;
        public Hazard(int type,float x,float y,float radius,float warning,float life,float damage){this.type=type;this.x=x;this.y=y;this.radius=radius;this.warning=warning;this.life=life;this.damage=damage;}
    }
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
    public float skillPeriod(){return Math.max(12f,24f-rank(9)*.4f-(custom>=0&&profile().trait==2?.8f:0)+(soulKind()==1?.5f:soulKind()==2?-.6f:0));}
    public float careerCooldownRate(){return 1f+careerCooldownRank*.1f+((careerSkillTime>0&&(careerSkillMode==3||careerSkillMode==4))?1f:0);}
    public float dodgePeriod(){return 1.15f-rank(4)*.07f-(custom>=0&&profile().trait==1?.12f:0)-(hasRelic(2)?.15f:0);}
    public float potionRatio(){return custom>=0&&profile().trait==0?.55f:.45f;}
    private float originHp(){return custom<0?0:profile().origin==0?18:profile().origin==1?-10:-8;}
    public int room=0,loot=0,kills=0,potions=2,damageBonus=0,healthBonus=0,speedBonus=0,careerCooldownRank=0,combo=0;
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
        public float hex=0,guard=0,targetX=560,targetY=380;
        public int pattern=0;
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
        public float x,y;public int amount;public boolean heal,potion;
        public Drop(float x,float y,int amount,boolean heal){this.x=x;this.y=y;this.amount=amount;this.heal=heal;}
    }
    public static final class Signal {
        public String kind;public float x,y;public int value;
        public Signal(String kind,float x,float y,int value){this.kind=kind;this.x=x;this.y=y;this.value=value;}
    }
    public int heroLevel(){return 1+Math.min(19,heroXp()/80);}
    private float baseMaxHp(){return (hero==0?110:hero==1?85:90)+vitality*10+(heroLevel()-1)*3+rank(8)*(soulKind()==1?10:8)+originHp()-(custom>=0&&profile().trait==2?8:0)+(hasRelic(1)?12:0)+characterTrait(1)*4;}
    public float maxHp(){return Math.min(99999,baseMaxHp()+healthBonus);}
    public float baseDamage(){return (hero==0?22:hero==1?18:15)+power*2+damageBonus+(heroLevel()-1)+rank(0)*2+(custom>=0&&profile().origin==2?2:0)+characterTrait(2);}
    public float damage(){return baseDamage()*(rank(3)>0&&hp<maxHp()*.4f?1.55f:1)*(soulBuff>0?1.25f:1)*(soulKind()==2?(ruptureTime>0?1.4f:corruption>=75?1.25f:corruption>=50?1.15f:1):1);}
    public float speed(){return (hero==2?240:hero==1?215:205)*(1+speedBonus*.08f+rank(4)*.03f)*(custom<0?1:profile().origin==0?.95f:profile().origin==1?1.07f:1)*(background()==2?1.03f:1)*(background()==3&&hp<maxHp()*.35f?1.12f:1)*(job()==HUNTER&&careerSkillMode==1&&careerSkillTime>0?1.45f:1);}
    public float attackPeriod(){return (hero==0?.48f:hero==1?.4f:.12f)*(hero==0?1.1f:1)*(custom>=0&&profile().trait==0?1.06f:1)*(soulBuff>0?.8f:1)*(job()==HUNTER&&careerSkillMode==1&&careerSkillTime>0?.62f:1)*(job()==ASSASSIN&&careerSkillMode==2&&careerSkillTime>0?1.12f:1)*(job()==REPORTER&&careerSkillMode==4&&careerSkillTime>0?.62f:1);}
    public int upgradeCost(int rank){return 30+rank*25;}
    public int random(int bound){rngState^=rngState<<13;rngState^=rngState>>>7;rngState^=rngState<<17;return (int)Long.remainderUnsigned(rngState,bound);}
    public void signal(String kind,float x,float y,int amount){if(signals.size()<64)signals.add(new Signal(kind,x,y,amount));}
    public void selectHero(int value){if(mode==Mode.CAMP && value>=0&&value<3){custom=-1;hero=value;hp=maxHp();}}
    public void upgrade(boolean health){
        if(mode!=Mode.CAMP)return;int rank=health?vitality:power;
        if(rank>=10){notice="这项营地升级已满级。";return;}
        int cost=upgradeCost(rank);if(bank<cost){notice="还需要 "+(cost-bank)+" 枚票券。";return;}
        bank-=cost;if(health)vitality++;else power++;hp=maxHp();notice="升级完成，所有角色共同受益。";
    }
    public void start(long seed){beginRun(seed,false);}
    public void startEndless(long seed){beginRun(seed,true);}
    private void beginRun(long seed,boolean infinite){
        if(mode!=Mode.CAMP)return;clearThreat();endless=infinite;endlessDepth=infinite?1:0;clearSoulRuntime();clearRelicRun();careerSkillTime=0;careerSkillDeathCd=0;careerSkillMode=0;careerCooldownRank=0;soulEnergy=soulKind()==0?0:40+(hasRelic(5)?(soulKind()==1?30:15):0);rngState=seed==0?1:seed;runs=Math.min(999999,runs+1);
        loot=0;kills=0;nextId=1;room=1;potions=2;damageBonus=0;healthBonus=0;speedBonus=0;combo=0;time=0;won=false;hp=maxHp();shield=0;soulWard=0;careerWard=entryCareerWard();
        generateRoom();
    }
    public void generateRoom(){
        mode=Mode.PLAY;roomCleared=false;lastRelic=0;relicEcho=0;x=WIDTH/2;y=HEIGHT-100;faceX=0;faceY=-1;
        attackCd=0;attackAnim=0;rollTime=0;rollCd=0;skillCd=0;soulCd=0;careerSkillTime=0;careerSkillDeathCd=0;careerSkillMode=0;dodgeCharge=0;swingBoost=1;followUpWindow=0;hazards.clear();soulBuff=0;soulFields.clear();invulnerable=.8f;comboWindow=0;hitStop=0;
        enemies.clear();shots.clear();blocks.clear();drops.clear();signals.clear();swingHits.clear();
        // Keep the central route and both exits connected in every layout.
        for(int i=0;i<6;i++) {
            float bx=(i%2==0?150:730)+random(120),by=180+(i/2)*140+random(25);
            blocks.add(new Block(bx,by,55+random(35),45+random(25),room<3?0:1));
        }
        ThreatSystem.enter(this);
        int[] baseGroup=EncounterSystem.GROUPS[room-1];int extra=endless?Math.min(5,(endlessDepth-1)/5):0;int[] group=Arrays.copyOf(baseGroup,baseGroup.length+extra);for(int i=baseGroup.length;i<group.length;i++)group[i]=4+random(3);
        for(int i=0;i<group.length;i++){
            int type=group[i];float ex=560,ey=150;int tries=0;
            do{ex=120+random(880);ey=130+random(300);tries++;}while((!free(ex,ey,type==3?32:24)||len(ex-x,ey-y)<230)&&tries<100);
            if(!free(ex,ey,type==3?32:24)){ex=560;ey=130+i*40;}
            float life=type==3?900:55+room*12+(type==1?10:type==2?24:type==5?15:type==6?25:0);
            enemies.add(new Enemy(nextId++,type,ex,ey,Math.max(life*threat,encounterHpFloor)));
        }
        notice=ROOM_NAMES[room-1]+" · 清除敌人开启出口";signal("enter",x,y,room);
    }
    public void chooseReward(int which){
        if(mode!=Mode.REWARD || which<0||which>2)return;
        if(which==0){if(damageBonus<40){damageBonus=Math.min(40,damageBonus+4);notice="锋芒：本轮伤害 +4";}else if(careerCooldownRank<8){careerCooldownRank++;notice="职业技能冷却速度 +10%（累计 "+(careerCooldownRank*10)+"%）";}else{loot=Math.min(99999,loot+12);notice="职业冷却已满 · 本轮票券 +12";}}
        if(which==1){if(maxHp()<99999){int cap=healthBonus<200?200:99999-(int)baseMaxHp();int gain=(int)Math.min(20,Math.min(cap-healthBonus,99999-maxHp()));healthBonus+=Math.max(0,gain);heal(35);notice=(healthBonus<=200?"生息":"生命上限")+ "：生命上限 +"+Math.max(0,gain)+"，"+(healthBonus<=200?"阶段上限200":"最高99999")+"，基础恢复35";}else{loot=Math.min(99999,loot+15);notice="生命上限已达99999 · 本轮票券 +15";}}
        if(which==2){if(speedBonus<6){speedBonus=Math.min(6,speedBonus+1);skillCd=0;followUpWindow=0;notice="轻羽：移速增益 +8%，技能立即就绪";}else{loot=Math.min(99999,loot+15);notice="轻羽已满 · 本轮票券 +15";}}
        roomCleared=true;mode=Mode.PLAY;signal("clear",x,y,0);
    }
    public void pause(){if(mode==Mode.PLAY){mode=Mode.PAUSED;notice="已暂停";}}
    public void resume(){if(mode==Mode.PAUSED)mode=Mode.PLAY;}
    public void returnCamp(){if(mode==Mode.RESULT){mode=Mode.CAMP;clearThreat();clearSoulRuntime();clearRelicRun();careerCooldownRank=0;careerWard=0;room=0;damageBonus=0;healthBonus=0;speedBonus=0;roomCleared=false;shield=0;dodgeCharge=0;enemies.clear();shots.clear();blocks.clear();drops.clear();hazards.clear();followUpWindow=0;hp=maxHp();notice="冒险所得已结算。选择角色，再次出发。";}}
    public void abandon(){if(mode==Mode.PAUSED)finish(false);}
    public void dodge(float dx,float dy){
        if(mode!=Mode.PLAY||rollCd>0||rollTime>0)return;
        float len=len(dx,dy);if(len<.1f){dx=faceX;dy=faceY;len=1;}
        rollX=dx/len;rollY=dy/len;rollTime=.28f;rollCd=dodgePeriod();invulnerable=Math.max(invulnerable,.3f);attackAnim=0;
        dodgeCharge=rank(5)>0?3:0;
        if(rank(6)>0){for(Enemy e:enemies)if(len(e.x-x,e.y-y)<120){hit(e,damage()*1.05f);e.stun=Math.max(e.stun,.45f);}signal("echo",x,y,120);}
        if(rank(6)>0&&rank(11)>0)shield=Math.min(talentShieldLimit(),shield+6);
        if(rank(7)>0)skillCd=Math.max(0,skillCd-2.5f);
        if(hasRelic(3))relicEcho=3;
        signal("roll",x,y,0);
    }
    public void potion(){
        if(mode!=Mode.PLAY)return;
        if(potions<=0||hp>=maxHp()&&!(hasRelic(4)&&soulKind()==2&&corruption>0))return;
        potions--;if(hasRelic(4)){if(soulKind()==1)soulWard=Math.max(soulWard,12);if(soulKind()==2)corruption=Math.max(0,corruption-20);}heal(maxHp()*potionRatio()+(background()==4?6:0));
    }
    public void skill(){
        if(mode!=Mode.PLAY||rollTime>0||skillCd>0)return;
        int j=job();
        careerSkillMode=j+1;
        careerSkillTime=30f;
        careerSkillDeathCd=j==HUNTER?1f:0f;
        skillCd=skillPeriod();
        followUpWindow=0;
        if(j==HUNTER){
            for(Enemy e:enemies)if(e.hp>0&&len(e.x-x,e.y-y)<430)hit(e,damage()*2.2f,false);
            signal("careerSkill",x,y,1);
            notice="猎人：瞬时猎杀";
        }else if(j==ASSASSIN){
            signal("careerSkill",x,y,2);
            notice="刺客：刀尖之舞";
        }else if(j==DETECTIVE){
            signal("careerSkill",x,y,3);
            notice="侦探：灯中之影";
        }else{
            signal("careerSkill",x,y,4);
            notice="记者：静默之声";
        }
    }
    private void followUp(){
        followUpWindow=0;aimAtNearest(680);int style=skillStyle();
        if(soulKind()==2)payForbidden(.02f,12);
        float ox=x,oy=y,range=(style==0?320:style==1?640:180)*reachFactor();
        float endX=x+faceX*range,endY=y+faceY*range;
        if(style==2){for(int i=0;i<(int)(range/5);i++)movePlayer(faceX*5,faceY*5);endX=x;endY=y;}
        boolean connected=false;
        for(Enemy e:enemies)if(e.hp>0&&segmentDistance(e.x,e.y,ox,oy,endX,endY)<(style==0?36:style==1?19:30)+e.radius()&&lineClear(ox,oy,e.x,e.y)){
            boolean cursed=e.hex>0;float factor=style==0?(e.stun>0?1.95f:1.3f):style==1?1.8f:e.state==1?2.1f:1.5f;
            if(style==1)e.guard=0;float before=e.hp;hit(e,damage()*factor*(rank(7)>0?1.2f:1));connected|=e.hp<before;
            if(soulKind()==2&&cursed){e.hex=0;signal("echo",e.x,e.y,85);for(Enemy other:enemies)if(other!=e&&other.hp>0&&len(other.x-e.x,other.y-e.y)<85&&lineClear(e.x,e.y,other.x,other.y))hit(other,damage()*.65f,false);}
        }
        if(soulKind()==1){if(connected)soulWard=Math.min(30,soulWard+8);cleanse(x,y,110);}
        if(connected){if(rank(10)>0){heal(maxHp()*(rank(3)>0&&hp<maxHp()*.4f?.06f:.03f));if(rank(7)>0)skillCd=Math.max(0,skillCd-.8f);}if(rank(11)>0)shield=Math.min(talentShieldLimit(),shield+6);}
        signal("followup",ox,oy,style);notice=EncounterSystem.FOLLOWUPS[style];
    }
    public void soulSkill(){
        if(mode!=Mode.PLAY||soulKind()==0||soulCd>0||rollTime>0||roomCleared||enemies.isEmpty())return;
        if(soulEnergy<40){notice="专属技能需要40魂能，命中敌人可积累。";signal("soulLow",x,y,0);return;}
        int kind=soulKind(),art=soulArt();soulEnergy-=40;soulCd=soulPeriod();aimAtNearest(520);
        if(kind==2)payForbidden(.08f,30);
        float power=damage();
        if(kind==1&&art==0){soulWard=Math.max(soulWard,30);soulFields.add(new SoulField(1,x,y,power));signal("radiant",x,y,175);}
        else if(kind==1&&art==1){
            soulWard=Math.max(soulWard,12);float endX=x+faceX*520,endY=y+faceY*520;
            for(Enemy e:enemies)if(e.hp>0&&segmentDistance(e.x,e.y,x,y,endX,endY)<28+e.radius()&&lineClear(x,y,e.x,e.y)){hit(e,power*3.6f,false);e.stun=Math.max(e.stun,e.type==3?.15f:.6f);}
            shots.removeIf(p->!p.friendly&&segmentDistance(p.x,p.y,x,y,endX,endY)<35&&lineClear(x,y,p.x,p.y));
            signal("lance",x,y,(int)Math.toDegrees(Math.atan2(faceY,faceX)));
        }else if(art==0){
            float fx=Math.max(60,Math.min(WIDTH-60,x+faceX*120)),fy=Math.max(60,Math.min(HEIGHT-60,y+faceY*120));
            soulFields.add(new SoulField(2,fx,fy,power));signal("forbidden",fx,fy,185);
        }else if(kind==2&&art==1){
            soulBuff=6;
            for(Enemy e:enemies)if(e.hp>0&&len(e.x-x,e.y-y)<160){hit(e,power*1.2f,false);e.hex=3;}
            signal("forbidden",x,y,160);
        }else if(kind==1){
            soulBuff=5;soulWard=Math.min(60,soulWard+20);signal("radiant",x,y,160);
        }else{
            for(Enemy e:enemies)if(e.hp>0&&len(e.x-x,e.y-y)<190){hit(e,power*2.8f,false);e.hex=4;}
            signal("forbidden",x,y,190);
        }
    }
    private void cleanse(float cx,float cy,float radius){shots.removeIf(p->!p.friendly&&len(p.x-cx,p.y-cy)<radius);}
    private void pull(Enemy e,float cx,float cy,float distance){
        float dx=cx-e.x,dy=cy-e.y,d=len(dx,dy);if(d<45||e.hp<=0)return;
        float length=Math.min(d-45,distance)*(e.type==3?.2f:1);int steps=Math.max(1,(int)Math.ceil(length/6));
        for(int i=0;i<steps;i++){float mx=dx/d*length/steps,my=dy/d*length/steps;if(free(e.x+mx,e.y,e.radius()))e.x+=mx;if(free(e.x,e.y+my,e.radius()))e.y+=my;}
    }
    private void triggerRupture(){if(corruption>=100&&!ruptureLatched){ruptureLatched=true;ruptureTime=5f;ruptureTick=1.5f;soulBuff=5f;signal("rupture",x,y,0);}}
    private void updateSoul(float dt){
        soulCd=dec(soulCd,dt);soulBuff=dec(soulBuff,dt);
        if(soulKind()==2)corruption=Math.max(0,corruption-dt);
        triggerRupture();
        if(ruptureTime>0){float elapsed=Math.min(dt,ruptureTime);ruptureTime=dec(ruptureTime,elapsed);ruptureTick-=elapsed;
            if(ruptureTick<=.0001f){ruptureTick+=1.5f;float before=hp;hp=Math.max(Math.min(1,hp),hp-maxHp()*.03f);signal("sacrifice",x,y,(int)(before-hp));}
            if(ruptureTime<=0){ruptureTick=0;if(corruption<100)ruptureLatched=false;}
        }
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
        aimAtNearest(hero==1?640:meleeReach()+30);
        if(job()==ASSASSIN&&careerSkillMode==2&&careerSkillTime>0){double base=Math.atan2(faceY,faceX);for(int i=-1;i<=1;i++)throwKnife((float)Math.cos(base+i*.22),(float)Math.sin(base+i*.22),damage()*.35f);}
        attackCd=attackPeriod();attackAnim=hero==1?.18f:.24f;
        combo=comboWindow>0?(combo+1)%3:0;comboWindow=.85f;swingHits.clear();
        swingBoost=(dodgeCharge>0?1+rank(5)*(soulKind()==2?.20f:.15f):1)*(combo==2?1.35f+rank(1)*(soulKind()==2?.18f:.12f):1);dodgeCharge=0;
        if(combo==2&&rank(2)>0){boolean connected=false;for(Enemy e:enemies)if(e.hp>0&&len(e.x-x,e.y-y)<185){float before=e.hp;hit(e,damage()*.75f);connected|=e.hp<before;}if(connected&&rank(11)>0)shield=Math.min(talentShieldLimit(),shield+4);signal("echo",x,y,185);}
        if(hero==1)fire(faceX,faceY,damage()*swingBoost);else if(hero==2){attackAnim=.18f;signal("daggerDash",x,y,combo);}signal(hero==1?"bow":"swing",x,y,combo);
    }
    private void fire(float dx,float dy,float value){fire(dx,dy,value,0,0);}
    private void fire(float dx,float dy,float value,int effect,int pierce){if(shots.size()<160){Shot s=new Shot(x+dx*20,y+dy*20,dx*640,dy*640,value,true);s.effect=effect;s.pierce=pierce;shots.add(s);}}
    private void throwKnife(float dx,float dy,float value){if(shots.size()<160){Shot s=new Shot(x+dx*18,y+dy*18,dx*760,dy*760,value,true);s.radius=3.5f;s.life=1.5f;shots.add(s);}}
    private void aimAtNearest(float range){
        Enemy nearest=null;float best=range;
        for(Enemy e:enemies){float distance=len(e.x-x,e.y-y);if(e.hp>0&&distance<best&&lineClear(x,y,e.x,e.y)){best=distance;nearest=e;}}
        if(nearest!=null&&best>.01f){faceX=(nearest.x-x)/best;faceY=(nearest.y-y)/best;}
    }
    public void update(float dt,Input in){
        if(mode!=Mode.PLAY)return;
        if(!Float.isFinite(dt)||dt<=0)return;dt=Math.min(dt,.05f);
        if(hitStop>0){hitStop=Math.max(0,hitStop-dt);return;}
        roomElapsed=Math.min(10000000,roomElapsed+dt);followUpWindow=dec(followUpWindow,dt);careerSkillTime=dec(careerSkillTime,dt);careerSkillDeathCd=dec(careerSkillDeathCd,dt);if(careerSkillTime<=0)careerSkillMode=0;
        if(careerSkillMode==1&&careerSkillTime>0&&careerSkillDeathCd<=0){float floor=maxHp()*.30f;if(hp>floor){float before=hp;hp=Math.max(floor,hp-maxHp()*.01f);signal("sacrifice",x,y,(int)Math.ceil(before-hp));}careerSkillDeathCd=1f;}
        updateSoul(dt);relicEcho=dec(relicEcho,dt);time+=dt;attackCd=dec(attackCd,dt);rollCd=dec(rollCd,dt);skillCd=dec(skillCd,dt*careerCooldownRate());invulnerable=dec(invulnerable,dt);comboWindow=dec(comboWindow,dt);dodgeCharge=dec(dodgeCharge,dt);
        float mx=Float.isFinite(in.mx)?in.mx:0,my=Float.isFinite(in.my)?in.my:0,mag=len(mx,my);
        if(mag>1){mx/=mag;my/=mag;mag=1;}
        motionX=mx*speed();motionY=my*speed();
        if(rollTime>0){movePlayer(rollX*530*dt,rollY*530*dt);rollTime=dec(rollTime,dt);}else if(hero==2&&attackAnim>0){}
        else {
            if(mag>.1f){if(attackAnim<=0){faceX=mx/mag;faceY=my/mag;}float slow=attackAnim>0?.65f:1;movePlayer(mx*speed()*slow*dt,my*speed()*slow*dt);}
            if(in.attack&&attackCd<=0)beginAttack();
        }
        if(attackAnim>0){
            attackAnim=dec(attackAnim,dt);
            if(hero==2&&attackAnim>0){
                movePlayer(faceX*520*dt,faceY*520*dt);
                for(Enemy e:enemies)if(e.hp>0&&!swingHits.contains(e.id)&&segmentDistance(e.x,e.y,x-faceX*35,y-faceY*35,x+faceX*35,y+faceY*35)<e.radius()+24){swingHits.add(e.id);hit(e,damage()*swingBoost);}
            }else if(hero!=1&&attackAnim<.18f&&attackAnim>.04f){
                float range=meleeReach(),cos=hero==0?-.05f:.1f;
                if(hero==0){final float r=meleeReach();final float c=.0f;shots.removeIf(p->{if(p.friendly)return false;float dx=p.x-x,dy=p.y-y,d=len(dx,dy);return d<r&&(d<.01f||(dx*faceX+dy*faceY)/d>c);});}
                for(Enemy e:enemies){float dx=e.x-x,dy=e.y-y,d=len(dx,dy);
                    if(e.hp>0&&!swingHits.contains(e.id)&&d<range+e.radius()&&(d<.01f||(dx*faceX+dy*faceY)/d>cos)&&lineClear(x,y,e.x,e.y)){
                        swingHits.add(e.id);hit(e,damage()*swingBoost);hitStop=.025f;
                    }
                }
            }
        }
        if(careerSkillMode==3&&careerSkillTime>0){for(Enemy e:enemies)if(e.hp>0){pull(e,x,y,55f*dt);hit(e,damage()*.12f*dt,false);}}
        for(Enemy e:enemies)if(e.hp>0)updateEnemy(e,dt);
        if(mode!=Mode.PLAY)return;
        updateHazards(dt);if(mode!=Mode.PLAY)return;updateShots(dt);if(mode!=Mode.PLAY)return;
        Iterator<Enemy> it=enemies.iterator();while(it.hasNext()){
            Enemy e=it.next();if(e.hp<=0){it.remove();kills=Math.min(99999,kills+1);drops.add(new Drop(e.x,e.y,(e.type==3?55:5+room)+(background()==6?2:0),false));if(background()==5){heal(1);gainSoul(e.type==3?8:2);}if(random(6)==0)drops.add(new Drop(e.x+15,e.y,15,true));if(random(100)<ThreatSystem.potionChance(e.type)){Drop bottle=new Drop(e.x-15,e.y,1,false);bottle.potion=true;drops.add(bottle);}signal("defeat",e.x,e.y,e.type);}
        }
        Iterator<Drop> d=drops.iterator();while(d.hasNext()){
            Drop p=d.next();float dist=len(p.x-x,p.y-y);if(dist<100&&dist>1){float speed=270*dt;p.x+=(x-p.x)/dist*Math.min(speed,dist);p.y+=(y-p.y)/dist*Math.min(speed,dist);}
            if(dist<24&&collectDrop(p))d.remove();
        }
        if(enemies.isEmpty()&&!roomCleared){
            drops.removeIf(this::collectDrop);shots.clear();hazards.clear();ThreatSystem.clear(this);if(endless){bestDepth=Math.max(bestDepth,endlessDepth);if(room==5)loot=Math.min(99999,loot+35);}
            unlockStory();discoverRelic();if(room==5&&!endless)finish(true);else mode=Mode.REWARD;
        }
        if(roomCleared&&y<100){
            if(x<430){float recovery=springRatio();heal(maxHp()*recovery);advanceRoom();notice="穿过补给所，基础恢复"+Math.round(recovery*100)+"%生命（受治疗修正）。";}
            else if(x>690){loot=Math.min(99999,loot+12);advanceRoom();notice="穿过货栈，额外获得 12 票券。";}
        }
    }
    private void addHazard(int type,float px,float py,float radius,float warning,float life,float power){
        if(hazards.size()<18)hazards.add(new Hazard(type,Math.max(64,Math.min(WIDTH-64,px)),Math.max(64,Math.min(HEIGHT-64,py)),enemyRange(radius),warning,life,power));
    }
    private void updateHazards(float dt){
        Iterator<Hazard> it=hazards.iterator();while(it.hasNext()){
            Hazard h=it.next();float wait=Math.min(h.warning,dt);h.warning=dec(h.warning,wait);float active=dt-wait;
            if(h.warning<=0&&active>0){
                if(h.type==0){
                    if(len(x-h.x,y-h.y)<h.radius+PLAYER_RADIUS&&lineClear(h.x,h.y,x,y))hurt(h.damage);
                    h.life=0;signal("hazard",h.x,h.y,(int)h.radius);
                }else{
                    float step=Math.min(h.life,active);h.life=dec(h.life,step);h.tick-=step;
                    if(h.tick<=.0001f){h.tick+=.65f;if(len(x-h.x,y-h.y)<h.radius+PLAYER_RADIUS&&lineClear(h.x,h.y,x,y))hurt(h.damage);signal("hazard",h.x,h.y,(int)h.radius);}
                }
            }
            if(h.warning<=0&&h.life<=0)it.remove();if(mode!=Mode.PLAY)return;
        }
    }
    private void aimEnemy(Enemy e,float prediction){
        e.targetX=Math.max(64,Math.min(WIDTH-64,x+motionX*prediction));e.targetY=Math.max(64,Math.min(HEIGHT-64,y+motionY*prediction));
        float dx=e.targetX-e.x,dy=e.targetY-e.y,d=Math.max(.01f,len(dx,dy));e.aimX=dx/d;e.aimY=dy/d;
    }
    private void fan(Enemy e,int half,float spread,float speed,float power){double base=Math.atan2(e.aimY,e.aimX);for(int i=-half;i<=half;i++)enemyShot(e,(float)Math.cos(base+i*spread),(float)Math.sin(base+i*spread),speed,power);}
    private void updateEnemy(Enemy e,float dt){
        e.hex=dec(e.hex,dt);e.flash=dec(e.flash,dt);
        if(e.stun>0){e.stun=dec(e.stun,dt);return;}
        float dx=x-e.x,dy=y-e.y,distance=Math.max(.01f,len(dx,dy));boolean enraged=e.type==3&&e.hp<e.maxHp*.5f;
        if(e.state==0){
            float reach=enemyRange(e.type==0?90:e.type==1?520:e.type==2?360:e.type==3?550:e.type==4?300:e.type==5?520:420);
            if(distance<reach&&lineClear(e.x,e.y,x,y)){
                e.state=1;e.timer=e.type==0?.42f:e.type==1?.7f:e.type==2?.7f:e.type==3?(enraged?.72f:1.05f):e.type==4?.55f:e.type==5?.75f:1;
                aimEnemy(e,e.type==1?.22f:e.type==4?.35f:e.type==5?.5f:0);e.struck=false;
            }else {
                float speed=enemySpeed(e.type==3?115:e.type==4?235:135+room*5);
                float side=(e.id%2==0?1:-1)*(e.type==4?.45f:distance>150?.2f:0);
                float nx=dx/distance-dy/distance*side,ny=dy/distance+dx/distance*side,n=len(nx,ny);
                moveEnemy(e,nx/n*speed*dt,ny/n*speed*dt);
            }
        }else if(e.state==1){
            e.timer-=dt;
            // Ranged units keep their locked aim while backing away from close attackers.
            if((e.type==1||e.type==5)&&distance<160)moveEnemy(e,-dx/distance*75*dt,-dy/distance*75*dt);
            if(e.timer<=0){
                e.state=2;
                if(e.type==0){e.timer=.48f;if(distance<enemyRange(110)&&lineClear(e.x,e.y,x,y))hurt(enemyPower(e,12+room*2));signal("enemySwing",e.x,e.y,0);}
                else if(e.type==1){e.timer=1.05f;fan(e,1,.15f,310,12+room*2);}
                else if(e.type==2||e.type==4)e.timer=e.type==2?.5f:.34f;
                else if(e.type==5){e.timer=1.5f;addHazard(0,e.targetX,e.targetY,100,1.05f,2.3f,enemyPower(e,12+room*2));addHazard(0,e.targetX-e.aimY*135,e.targetY+e.aimX*135,85,1.4f,1.8f,enemyPower(e,10+room*2));}
                else if(e.type==6){e.timer=2;for(Enemy other:enemies)if(other!=e&&other.hp>0&&len(other.x-e.x,other.y-e.y)<260&&lineClear(e.x,e.y,other.x,other.y))other.guard=Math.min(40,other.guard+24);fan(e,2,.3f,240,12);signal("enemyWard",e.x,e.y,260);}
                else {
                    e.timer=enraged?.85f:1.25f;
                    if(e.pattern==0){addHazard(1,e.x,e.y,220,.3f,.1f,enemyPower(e,28));int count=enraged?16:12;for(int i=0;i<count;i++){double a=i*Math.PI*2/count+time*.1;enemyShot(e,(float)Math.cos(a),(float)Math.sin(a),enraged?270:220,18);}}
                    else if(e.pattern==1)fan(e,enraged?3:2,.19f,350,20);
                    else {addHazard(1,e.targetX,e.targetY,120,.8f,.1f,enemyPower(e,30));addHazard(1,e.targetX-135,e.targetY+70,110,1.15f,.1f,enemyPower(e,24));addHazard(1,e.targetX+135,e.targetY-70,110,1.5f,.1f,enemyPower(e,24));}
                    signal("slam",e.x,e.y,e.pattern);
                }
            }
        }else if(e.state==3){e.timer-=dt;if(e.timer<=0)e.state=0;}
        else {
            e.timer-=dt;
            if(e.type==2||e.type==4){float speed=enemySpeed(e.type==2?440:520);moveEnemy(e,e.aimX*speed*dt,e.aimY*speed*dt);if(!e.struck&&len(x-e.x,y-e.y)<e.radius()+PLAYER_RADIUS+7){e.struck=true;hurt(enemyPower(e,18+room*2));}}
            if((e.type==1||e.type==3&&e.pattern==1)&&!e.struck&&e.timer<(e.type==1?.5f:.4f)){e.struck=true;fan(e,e.type==1?1:2,.12f,e.type==1?330:370,e.type==1?12+room*2:20);}
            if(e.timer<=0){if(e.type==2||e.type==4){e.state=3;e.timer=e.type==2?.65f:.5f;}else{e.state=0;if(e.type==3)e.pattern=(e.pattern+1)%3;}}
        }
        for(Enemy other:enemies)if(other!=e&&other.hp>0){float sx=e.x-other.x,sy=e.y-other.y,d=len(sx,sy),min=e.radius()+other.radius();if(d>.1&&d<min)moveEnemy(e,sx/d*30*dt,sy/d*30*dt);}
    }
    private float enemyForce(Enemy e){return 1;}
    private void enemyShot(Enemy e,float dx,float dy,float speed,float damage){if(shots.size()<160)shots.add(new Shot(e.x+dx*24,e.y+dy*24,dx*enemySpeed(speed),dy*enemySpeed(speed),enemyPower(e,damage),false));}
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
            if(isSpec(HUNTER,1)&&len(e.x-x,e.y-y)>200)value*=1.18f;
            if(isSpec(REPORTER,1)&&e.type==3)value*=1.15f;
        }
        if(echoEligible&&relicEcho>0&&hasRelic(3)){value*=1.35f;relicEcho=0;signal("echo",e.x,e.y,65);}
        value*=e.hex>0?1.15f*(hasRelic(6)?1.1f:1):1;float absorbed=Math.min(e.guard,value);e.guard-=absorbed;value-=absorbed;if(absorbed>0)signal("enemyShield",e.x,e.y,(int)absorbed);float dealt=Math.min(e.hp,value);e.hp=Math.max(0,e.hp-value);
        e.flash=.12f;if(absorbed==0)e.stun=Math.max(e.stun,e.type==3?.03f:e.type==2||e.type==4?.06f:.12f);signal("hit",e.x,e.y,(int)value);
        if(dealt>0&&job()==HUNTER&&careerSkillMode==1&&careerSkillTime>0)heal(dealt*.20f);
        if(e.hp<=0&&job()==ASSASSIN&&careerSkillMode==2&&careerSkillTime>0){skillCd=0;heal(maxHp()*.03f);}
        if(charge&&dealt>0){gainSoul(Math.min(4,dealt*.08f));if(soulBuff>0)heal(Math.min(maxHp()*.015f,dealt*.08f));}
    }
    private void hurt(float value){
        if(invulnerable>0||mode!=Mode.PLAY)return;
        value*= (custom>=0&&profile().trait==1?1.12f:1)*(job()==DETECTIVE&&careerSkillMode==3&&careerSkillTime>0?1.15f:1)*(rank(3)>0?1.15f:1)*(soulKind()==2?1+corruption*.004f:1);
        float wardTaken=Math.min(soulWard,value);soulWard-=wardTaken;value-=wardTaken;
        float careerTaken=Math.min(careerWard,value);careerWard-=careerTaken;value-=careerTaken;
        float talentTaken=Math.min(shield,value);shield-=talentTaken;value-=talentTaken;float absorbed=talentTaken+wardTaken+careerTaken;
        if(soulKind()==1&&absorbed>0)gainSoul(Math.min(6,absorbed*.2f));
        if(absorbed>0){signal("shield",x,y,(int)absorbed);if(rank(11)>0)for(Enemy e:enemies)if(len(e.x-x,e.y-y)<150)hit(e,absorbed*.65f,true,false);}
        float lost=Math.min(hp,value);roomDamage=Math.min(100000,roomDamage+lost);hp=Math.max(0,hp-value);invulnerable=.65f;signal("hurt",x,y,(int)lost);
        if(hasRelic(7)&&lost>0){for(Enemy e:enemies)if(e.hp>0&&len(e.x-x,e.y-y)<120)hit(e,lost*.25f,false);signal("echo",x,y,120);}
        if(hp<=0&&hasRelic(8)&&!relicRevived){relicRevived=true;hp=maxHp()*.25f;invulnerable=1;signal("revive",x,y,0);}
        if(hp<=0)finish(false);
    }
    private void finish(boolean victory){
        if(mode==Mode.RESULT||mode==Mode.CAMP)return;
        won=victory;int reward=victory?loot+40:(int)(loot*.7f);bank=Math.min(999999,bank+reward);
        int gained=kills*6+(victory?50:10);if(isSpec(REPORTER,2))gained=gained*120/100;
        gainExperience(gained);
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
