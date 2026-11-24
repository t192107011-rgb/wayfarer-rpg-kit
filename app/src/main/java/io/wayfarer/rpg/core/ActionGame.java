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
    public float threat=1,pressure=0,encounterHpFloor=0,encounterDamageFloor=0,roomDamage=0,roomElapsed=0,bossSummonCd=0,burnTime=0,burnTick=0,burnDamage=0;
    public float enemyRange(float base){return base*(1+Math.min(.65f,(threat-1)*.05f));}
    public float enemySpeed(float base){return base*(1+Math.min(1f,(threat-1)*.09f));}
    public float enemyPower(Enemy e,float base){float growth=1f+Math.min(1.6f,Math.max(0,threat-1)*.22f);return Math.max(base*growth,encounterDamageFloor)*enemyForce(e);}
    public float reachFactor(){return rank(3)>0&&hp<maxHp()*.4f?1.25f:1;}
    public float meleeReach(){return (hero==0?120:100)*reachFactor();}
    public float activeRadius(){int k=soulKind();return (skillStyle()==0?(k==1?235:k==2?260:200):(k==1?180:k==2?190:160))*reachFactor()*(specialtyRank(7)>0?1.10f:1); }
    public float followUpDuration(){return rank(7)>0?3:2;}
    /** Persistent shields are deliberately bounded to a modest fraction of maximum health. */
    public float talentShieldLimit(){return Math.max(8f,maxHp()*.28f);}
    public float soulWardLimit(){return Math.max(6f,maxHp()*.18f);}
    public float shieldEfficiency(){return 1f+coreRank(10)*.08f;}
    public float addTalentShield(float base){if(base<=0)return 0;float before=shield;shield=Math.min(talentShieldLimit(),shield+base*shieldEfficiency());float gained=shield-before;if(gained>0)signal("shield",x,y,Math.round(gained));return gained;}
    public float addSoulWard(float base){if(base<=0)return 0;float before=soulWard;soulWard=Math.min(soulWardLimit(),soulWard+base);float gained=soulWard-before;if(gained>0)signal("shield",x,y,Math.round(gained));return gained;}
    public String shieldSourceSummary(){List<String> sources=new ArrayList<>();sources.add("闪避+"+(8+coreRank(4)*4));sources.add("击杀+1");if(fieldPermits[buildId()][0]>0)sources.add("远征许可开局+20（受上限约束）");sources.add("职业技能护盾（按职业变化，受上限约束）");if(tacticRanks[5]>0||coreRank(7)>0)sources.add("通用主动技触发+"+(3*tacticRanks[5]+4*coreRank(7)));if(rank(11)>0)sources.add("镜火反震");if(coreRank(10)>0)sources.add("护盾获取效率+"+(coreRank(10)*8)+"%");sources.add("护盾上限 "+Math.round(talentShieldLimit())+"（最大生命的28%）");return String.join(" · ",sources);}
    public String threatLabel(){return String.format(java.util.Locale.ROOT,"敌势 ×%.2f",threat);}
    private void clearThreat(){threat=1;pressure=0;encounterHpFloor=0;encounterDamageFloor=0;roomDamage=0;roomElapsed=0;bossSummonCd=0;burnTime=0;burnTick=0;burnDamage=0;endless=false;endlessDepth=0;}
    private boolean collectDrop(Drop d){if(d.potion){if(potions>=MAX_POTIONS)return false;potions++;signal("potion",d.x,d.y,potions);notice="拾取药瓶 · "+potions+" / "+MAX_POTIONS;}else if(d.heal)heal(d.amount);else{int gain=Math.round(d.amount*(1+.12f*tacticRanks[7]));gain=Math.round(gain*(1+coreRank(12)*.08f));loot=Math.min(99999,loot+gain);signal("coin",d.x,d.y,gain);}return true;}
    private void advanceRoom(){if(endless){endlessDepth=Math.min(999999,endlessDepth+1);room=(endlessDepth-1)%5+1;}else room++;generateRoom();}

    public final int[] experience=new int[3];
    public int custom=-1;
    public final BuildSystem.Profile[] profiles=new BuildSystem.Profile[3];
    public final int[][] talents=new int[6][12];
    public final int[][] specialties=new int[6][8];
    public final int[][] coreTalents=new int[6][15];
    public final int[][] careerTalents=new int[6][12];
    public final int[][] fieldPermits=new int[6][5];
    public final int[][] training=new int[6][4];
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
    public float soulPeriod(){float base=soulKind()==1?new float[]{12,10,14,12}[job()]:new float[]{14,16,18,20}[job()];return Math.max(4f,base-.3f*training[buildId()][3]-.8f*specialtyRank(1));}
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
    public String soulArtName(){return soulKind()==0?"未缔结":SoulSystem.ARTS[soulKind()][job()];}
    public boolean bindSoul(int kind,int art){
        if(mode!=Mode.CAMP)return false;SoulSystem.validate(kind,art);
        if(custom<0){presetSouls[hero]=kind;presetArts[hero]=art;}else{profile().soul=kind;profile().soulArt=art;}
        clearSoulRuntime();hp=maxHp();notice="已缔结："+SoulSystem.NAMES[kind];return true;
    }
    private void clearSoulRuntime(){soulEnergy=0;soulCd=0;soulWard=0;corruption=0;soulQuiet=0;soulBuff=0;ruptureTime=0;ruptureTick=0;ruptureLatched=false;soulFields.clear();}
    private void gainSoul(float value){if(soulKind()==0||mode!=Mode.PLAY||value<=0)return;float gain=value*(hasRelic(12)?1.1f:1)*(specialtyRank(3)>0?1.2f:1)*(1+.15f*tacticRanks[2]);float accepted=Math.min(gain,Math.max(0,100-soulEnergy));soulEnergy+=accepted;float overflow=gain-accepted;if(overflow>0){if(soulCd>0){float reduced=Math.min(soulCd,overflow*.18f);soulCd-=reduced;overflow-=reduced/.18f;}if(overflow>0)addTalentShield(overflow*.65f);}}
    public float healingFactor(){return ordinaryHealingFactor();}
    private float ordinaryHealingFactor(){return (soulKind()==1?1.15f:soulKind()==2?1-corruption*.003f:1)*(hasRelic(4)?1.1f:1);}
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
    public int specialtyRank(int node){return specialties[buildId()][node];}
    public int coreRank(int node){return coreTalents[buildId()][node];}
    public int careerTalentRank(int local){return careerTalents[buildId()][Math.max(0,Math.min(3,job()))*3+local];}
    public int availableCorePoints(){return BuildSystem.corePoints(heroXp())-BuildSystem.coreSpent(coreTalents[buildId()]);}
    public int availableSpecialtyPoints(){return BuildSystem.specialtyPoints(heroXp())-BuildSystem.spent(specialties[buildId()]);}
    public int availableCareerPoints(){return Math.min(8,1+heroXp()/200)-careerTalentSpent();}
    private int careerTalentSpent(){int n=0;for(int i=job()*3;i<job()*3+3;i++)n+=careerTalents[buildId()][i];return n;}
    public boolean learnCoreTalent(int id){if(mode!=Mode.CAMP||id<0||id>=15)return false;BuildSystem.Node n=BuildSystem.CORE_NODES[id];int[] ranks=coreTalents[buildId()];if(ranks[id]>=n.maxRank||1+Math.min(19,heroXp()/80)<n.minLevel||n.parent>=0&&ranks[n.parent]<(id%3==2?2:1)||availableCorePoints()<=0){notice="五大天赋的等级、前置或点数不足";return false;}ranks[id]++;hp=maxHp();notice="五大分支：习得「"+n.name+"」";return true;}
    public boolean learnCareerTalent(int local){if(mode!=Mode.CAMP||local<0||local>=3)return false;int id=job()*3+local;int[] ranks=careerTalents[buildId()];int max=BuildSystem.careerTalentMax(id);if(ranks[id]>=max||availableCareerPoints()<=0||local==2&&ranks[job()*3+1]<1){notice="职业技能强化天赋尚未满足前置或点数不足";return false;}ranks[id]++;notice="职业技能强化："+BuildSystem.CAREER_TALENT_NAMES[id];return true;}
    public boolean buyFieldPermit(int id){if(mode!=Mode.CAMP||id<0||id>=5)return false;int[] ranks=fieldPermits[buildId()];int[] costs={80,120,160,200,250};if(ranks[id]>0){notice="该远征特许升级已购入";return false;}if(bank<costs[id]){notice="局外储备不足，还差"+(costs[id]-bank)+"枚";return false;}bank-=costs[id];ranks[id]=1;notice="永久解锁远征特许升级：「"+new String[]{"守备护盾","急救储备","先遣票券","魂能预充","武装开局"}[id]+"」";return true;}
    public int availablePoints(){return BuildSystem.points(heroXp())-BuildSystem.spent(talents[buildId()]);}
    public boolean learn(int node){if(mode!=Mode.CAMP)return false;String reason=BuildSystem.lockReason(talents[buildId()],heroXp(),node);if(!reason.isEmpty()){notice=reason;return false;}if(BuildSystem.spent(talents[buildId()])>=BuildSystem.points(heroXp())){notice="没有可用天赋点";return false;}talents[buildId()][node]++;hp=maxHp();notice="习得「"+BuildSystem.NODES[node].name+"」";return true;}
    public boolean learnSpecialty(int node){if(mode!=Mode.CAMP)return false;String reason=BuildSystem.specialtyLockReason(specialties[buildId()],heroXp(),node);if(!reason.isEmpty()){notice=reason;return false;}if(availableSpecialtyPoints()<=0){notice="没有可用专精点";return false;}specialties[buildId()][node]++;hp=maxHp();notice="习得专精「"+BuildSystem.SPECIALTIES[node].name+"」";return true;}
    public void resetTalents(){if(mode==Mode.CAMP){int id=buildId();Arrays.fill(talents[id],0);Arrays.fill(specialties[id],0);Arrays.fill(coreTalents[id],0);Arrays.fill(careerTalents[id],0);hp=maxHp();shield=0;dodgeCharge=0;notice="全部天赋与专精已重置；训练和远征特许保留。";}}
    public void selectCustom(int slot){if(mode==Mode.CAMP&&slot>=0&&slot<3){custom=slot;hero=profiles[slot].weapon;hp=maxHp();notice="已选择 "+heroName();}}
    public void customize(int slot,BuildSystem.Profile draft){
        if(mode!=Mode.CAMP||slot<0||slot>=3)throw new IllegalArgumentException("仅能在营地编辑角色");
        BuildSystem.Profile copy=draft.copy();copy.name=BuildSystem.cleanName(copy.name);copy.xp=profiles[slot].xp;BuildSystem.validate(copy);profiles[slot]=copy;selectCustom(slot);
    }
    public float skillPeriod(){float base=Math.max(8f,24f-rank(9)*.4f-training[buildId()][2]*.35f-(custom>=0&&profile().trait==2?.8f:0)+(soulKind()==1?.5f:soulKind()==2?-.6f:0));return job()==HUNTER?base*(1-.05f*careerTalentRank(1)):base;}
    public float activeSkillPeriod(){return Math.max(4f,18f-rank(9)*.35f-coreRank(6)*.7f-training[buildId()][2]*.35f-(custom>=0&&profile().trait==2?.7f:0)+(soulKind()==1?-.5f:soulKind()==2?.4f:0)-tacticRanks[3]);}
    public float careerCooldownRate(){return 1f+careerCooldownRank*.1f+((careerSkillTime>0&&(careerSkillMode==3||careerSkillMode==4))?1f:0);}
    public float dodgePeriod(){return Math.max(.45f,1.15f-rank(4)*.07f-coreRank(3)*.08f-training[buildId()][2]*.045f-(custom>=0&&profile().trait==1?.12f:0)-(hasRelic(2)?.15f:0)-(hasRelic(11)?.12f:0)-(hasRelic(2)&&hasRelic(11)?.08f:0)-(isSpec(HUNTER,2)?.12f:0));}
    public float potionRatio(){return custom>=0&&profile().trait==0?.55f:.45f;}
    private float originHp(){return custom<0?0:profile().origin==0?18:profile().origin==1?-10:-8;}
    public int room=0,loot=0,kills=0,potions=2,damageBonus=0,healthBonus=0,speedBonus=0,careerCooldownRank=0,combo=0;
    public int bombs=0,snares=0,tonics=0;public float battleTonicTime=0;public boolean shopOpen=false;
    public final int[] tacticRanks=new int[8],tacticOffers={0,1,2};
    public int tacticSelected=-1;
    public boolean tacticChosen=false,tacticDraftOpen=false;
    public static final String[] TACTIC_NAMES={"专属技式","追击余势","魂流共鸣","迅捷循环","诅咒猎杀","护盾回路","战斗节拍","战地搜刮"};
    public static final String[] TACTIC_DESCRIPTIONS={"主动技能伤害 +12% / 级；范围或弹幕强化。","追击伤害 +12% / 级。","魂能获取 +15% / 级。","主动技能冷却 -1秒 / 级。","诅咒目标额外伤害 +12% / 级。","主动技能获得6点护盾 / 级。","攻击速度提高4% / 级。","票券拾取提高12% / 级。"};
    public boolean hasTacticDraft(){for(int id:tacticOffers)if(id>=0&&id<8&&tacticRanks[id]<3)return true;return false;}
    private int randomTactic(int a,int b){int[] pool=new int[8];int n=0;for(int i=0;i<8;i++)if(i!=a&&i!=b&&tacticRanks[i]<3)pool[n++]=i;return n==0?-1:pool[random(n)];}
    private void prepareTacticDraft(){tacticChosen=false;tacticSelected=-1;tacticDraftOpen=false;tacticOffers[0]=tacticRanks[0]<3?0:randomTactic(-1,-1);tacticOffers[1]=randomTactic(tacticOffers[0],-1);tacticOffers[2]=randomTactic(tacticOffers[0],tacticOffers[1]);}
    private void clearTactics(){Arrays.fill(tacticRanks,0);tacticOffers[0]=0;tacticOffers[1]=1;tacticOffers[2]=2;tacticChosen=false;tacticSelected=-1;tacticDraftOpen=false;}
    public void openTacticDraft(){if(mode==Mode.REWARD&&!shopOpen&&!tacticChosen&&hasTacticDraft()){tacticDraftOpen=true;tacticSelected=-1;}}
    public void closeTacticDraft(){tacticDraftOpen=false;tacticSelected=-1;}
    public boolean selectTactic(int slot){if(mode!=Mode.REWARD||!tacticDraftOpen||tacticChosen||slot<0||slot>=3)return false;int id=tacticOffers[slot];if(id<0||id>=8||tacticRanks[id]>=3)return false;tacticSelected=slot;notice="已选中「"+TACTIC_NAMES[id]+"」；确认前不会消耗刻印";return true;}
    public boolean confirmTactic(){if(mode!=Mode.REWARD||!tacticDraftOpen||tacticChosen||tacticSelected<0||tacticSelected>=3)return false;int id=tacticOffers[tacticSelected];if(id<0||id>=8||tacticRanks[id]>=3)return false;tacticRanks[id]++;tacticOffers[tacticSelected]=-1;tacticChosen=true;tacticSelected=-1;tacticDraftOpen=false;notice="战技刻印：「"+TACTIC_NAMES[id]+"」提升至 "+tacticRanks[id]+" 级";signal("tactic",x,y,id);return true;}

    public float x=560,y=620,hp=110,faceX=0,faceY=-1,time=0;
    public float attackCd=0,attackAnim=0,rollTime=0,rollCd=0,skillCd=0,activeSkillCd=0,activeSkillDelay=0,activeSkillX=560,activeSkillY=380,invulnerable=0,comboWindow=0;
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
        public float hex=0,guard=0,targetX=560,targetY=380,guardBreak=0,guardDelay=0;
        /** 0 none, 1 ward (weak), 2 plated (resistant), 3 regenerative. */
        public int guardType=0,pattern=0;
        public boolean struck=false;
        public Enemy(int id,int type,float x,float y,float hp){this.id=id;this.type=type;this.x=x;this.y=y;this.hp=hp;maxHp=hp;if(type==3)guardType=3;else if(type==4)guardType=2;else if(type==6)guardType=1;}
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
    private float baseMaxHp(){return (hero==0?110:hero==1?85:90)+vitality*10+(heroLevel()-1)*3+rank(8)*(soulKind()==1?10:8)+originHp()-(custom>=0&&profile().trait==2?8:0)+(hasRelic(1)?12:0)+(hasRelic(9)?18:0)+(hasRelic(1)&&hasRelic(9)?6:0)+characterTrait(1)*4;}
    public float maxHp(){return Math.min(99999,baseMaxHp()+healthBonus+training[buildId()][1]*4+coreRank(9)*12);}
    public float baseDamage(){return (hero==0?22:hero==1?18:15)+power*2+damageBonus+(heroLevel()-1)+rank(0)*2+(custom>=0&&profile().origin==2?2:0)+characterTrait(2);}
    public float damage(){return baseDamage()*(1+coreRank(0)*.04f+specialtyRank(4)*.05f)*(hasRelic(10)?1.05f:1)*(hasRelic(3)&&hasRelic(10)?1.04f:1)*(1+.025f*Math.min(10,training[buildId()][0])+.005f*(float)Math.sqrt(Math.max(0,training[buildId()][0]-10)))*(rank(3)>0&&hp<maxHp()*.4f?1.55f:1)*(soulBuff>0?1.25f:1)*(battleTonicTime>0?1.30f:1)*(soulKind()==2?(ruptureTime>0?1.4f:corruption>=75?1.25f:corruption>=50?1.15f:1):1);}
    public float speed(){return (hero==2?240:hero==1?215:205)*(1+speedBonus*.08f+rank(4)*.03f)*(custom<0?1:profile().origin==0?.95f:profile().origin==1?1.07f:1)*(hasRelic(11)?1.03f:1)*(background()==2?1.03f:1)*(background()==3&&hp<maxHp()*.35f?1.12f:1)*(isSpec(HUNTER,2)?1.08f:1)*(job()==HUNTER&&careerSkillMode==1&&careerSkillTime>0?1.45f:1);}
    public float attackPeriod(){return (hero==0?.48f:hero==1?.4f:.12f)*(hero==0?1.1f:1)*(custom>=0&&profile().trait==0?1.06f:1)*(soulBuff>0?.8f:1)*(job()==HUNTER&&careerSkillMode==1&&careerSkillTime>0?.62f:1)*(job()==ASSASSIN&&careerSkillMode==2&&careerSkillTime>0?.90f:1)*(job()==REPORTER&&careerSkillMode==4&&careerSkillTime>0?.90f:1)*Math.max(.35f,training[buildId()][0]<=10?1f-.012f*training[buildId()][0]:.88f/(1f+.001f*(training[buildId()][0]-10)))*(1-.03f*coreRank(1))*(1-.04f*tacticRanks[6])*(battleTonicTime>0?.8f:1);}
    public int upgradeCost(int rank){return (int)Math.min(999999L,30L+Math.max(0,rank)*25L);}
    public int trainingCost(int rank){return (int)Math.min(999999L,45L+Math.max(0,rank)*35L);}
    public boolean train(int route){if(mode!=Mode.CAMP||route<0||route>=4)return false;int rank=training[buildId()][route];if(rank>=999999){notice="该训练路线已达到安全等级上限。";return false;}int cost=trainingCost(rank);if(bank<cost){notice="训练还需要 "+(cost-bank)+" 枚营地储备。";return false;}bank-=cost;training[buildId()][route]++;hp=maxHp();notice=new String[]{"武器训练提升：伤害与攻速成长。","生存训练提升：最大生命成长。","战术训练提升：闪避与技能周转成长。","灵魂研究提升：初始魂能与魂技冷却成长。"}[route]+" · 等级 "+training[buildId()][route];return true;}
    public int random(int bound){rngState^=rngState<<13;rngState^=rngState>>>7;rngState^=rngState<<17;return (int)Long.remainderUnsigned(rngState,bound);}
    public void signal(String kind,float x,float y,int amount){if(signals.size()<64)signals.add(new Signal(kind,x,y,amount));}
    public void selectHero(int value){if(mode==Mode.CAMP && value>=0&&value<3){custom=-1;hero=value;hp=maxHp();}}
    public void upgrade(boolean health){
        if(mode!=Mode.CAMP)return;int rank=health?vitality:power;
        if(rank>=999999){notice="这项营地升级已达到安全等级上限。";return;}
        int cost=upgradeCost(rank);if(bank<cost){notice="还需要 "+(cost-bank)+" 枚票券。";return;}
        bank-=cost;if(health)vitality++;else power++;hp=maxHp();notice="升级完成，所有角色共同受益。";
    }
    public void start(long seed){beginRun(seed,false);}
    public void startEndless(long seed){beginRun(seed,true);}
    private void beginRun(long seed,boolean infinite){
        if(mode!=Mode.CAMP)return;clearThreat();endless=infinite;endlessDepth=infinite?1:0;clearSoulRuntime();clearRelicRun();careerSkillTime=0;careerSkillDeathCd=0;careerSkillMode=0;careerCooldownRank=0;clearTactics();soulEnergy=soulKind()==0?0:Math.min(100,40+(hasRelic(5)?(soulKind()==1?30:15):0)+training[buildId()][3]*3+(hasRelic(12)?12:0)+(hasRelic(5)&&hasRelic(12)?10:0)+specialtyRank(0)*8);rngState=seed==0?1:seed;runs=Math.min(999999,runs+1);
        loot=coreRank(14)*20+fieldPermits[buildId()][2]*30;kills=0;nextId=1;room=1;potions=2+fieldPermits[buildId()][1];bombs=0;snares=0;tonics=0;battleTonicTime=0;shopOpen=false;damageBonus=0;healthBonus=0;speedBonus=0;combo=0;time=0;won=false;hp=maxHp();shield=0;addTalentShield(20*fieldPermits[buildId()][0]);soulWard=0;careerWard=entryCareerWard();if(fieldPermits[buildId()][3]>0&&soulKind()!=0)soulEnergy=Math.min(100,soulEnergy+20);if(fieldPermits[buildId()][4]>0)damageBonus=4;
        generateRoom();
    }
    public void generateRoom(){
        mode=Mode.PLAY;roomCleared=false;lastRelic=0;relicEcho=0;x=WIDTH/2;y=HEIGHT-100;faceX=0;faceY=-1;
        attackCd=0;attackAnim=0;rollTime=0;rollCd=0;skillCd=0;activeSkillCd=0;activeSkillDelay=0;soulCd=0;careerSkillTime=0;careerSkillDeathCd=0;careerSkillMode=0;dodgeCharge=0;swingBoost=1;followUpWindow=0;hazards.clear();soulBuff=0;burnTime=0;burnTick=0;burnDamage=0;bossSummonCd=0;soulFields.clear();invulnerable=.8f;comboWindow=0;hitStop=0;
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
            float life=type==3?(room==2?900:room==4?1500:2200):55+room*12+(type==1?10:type==2?24:type==5?15:type==6?25:0);
            Enemy spawned=new Enemy(nextId++,type,ex,ey,Math.max(life*threat,encounterHpFloor));spawned.pattern=random(type==3?3:2);if(type==3){spawned.guardType=3;spawned.guard=18+room*3;bossSummonCd=5f;}else if(type==4){spawned.guardType=2;spawned.guard=14;}else if(type==6){spawned.guardType=1;spawned.guard=24;}enemies.add(spawned);
        }
        notice=ROOM_NAMES[room-1]+" · 清除敌人开启出口";signal("enter",x,y,room);
    }
    public void chooseReward(int which){
        if(mode!=Mode.REWARD || which<0||which>2)return;
        if(which==0){if(damageBonus<40){damageBonus=Math.min(40,damageBonus+4);notice="锋芒：本轮伤害 +4";}else if(careerCooldownRank<8){careerCooldownRank++;notice="职业技能冷却速度 +10%（累计 "+(careerCooldownRank*10)+"%）";}else{loot=Math.min(99999,loot+12);notice="职业冷却已满 · 本轮票券 +12";}}
        if(which==1){if(maxHp()<99999){int cap=healthBonus<200?200:99999-(int)baseMaxHp();int gain=(int)Math.min(20,Math.min(cap-healthBonus,99999-maxHp()));healthBonus+=Math.max(0,gain);heal(35);notice=(healthBonus<=200?"生息":"生命上限")+ "：生命上限 +"+Math.max(0,gain)+"，"+(healthBonus<=200?"阶段上限200":"最高99999")+"，基础恢复35";}else{loot=Math.min(99999,loot+15);notice="生命上限已达99999 · 本轮票券 +15";}}
        if(which==2){if(speedBonus<6){speedBonus=Math.min(6,speedBonus+1);skillCd=0;followUpWindow=0;notice="轻羽：移速增益 +8%，技能立即就绪";}else{loot=Math.min(99999,loot+15);notice="轻羽已满 · 本轮票券 +15";}}
        shopOpen=false;tacticDraftOpen=false;tacticChosen=false;tacticSelected=-1;Arrays.fill(tacticOffers,-1);roomCleared=true;mode=Mode.PLAY;signal("clear",x,y,0);
    }
    public void openShop(){if(mode==Mode.REWARD){tacticDraftOpen=false;tacticSelected=-1;shopOpen=true;}}
    public void closeShop(){shopOpen=false;}
    public int shopPrice(int item){return item==0?24:item==1?28:item==2?32:item==3?38:item==4?42:item==5?55:-1;}
    public boolean buyShopItem(int item){
        if(mode!=Mode.REWARD||!shopOpen||item<0||item>5)return false;
        int price=shopPrice(item);if(loot<price){notice="票券不足，还差 "+(price-loot)+"。";return false;}
        if(item==0&&bombs>=3||item==1&&snares>=3||item==2&&tonics>=3){notice="该道具已达到携带上限（3）。";return false;}
        if(item==3&&hp>=maxHp()-.01f){notice="生命值已满，无需急救补给。";return false;}
        if(item==4&&(soulKind()==0||soulEnergy>=99.99f)){notice="当前无法使用魂能结晶。";return false;}
        if(item==5&&damageBonus>=40&&careerCooldownRank>=8&&speedBonus>=6){notice="本轮战术改装已全部达到上限。";return false;}
        loot-=price;
        if(item==0)bombs++;else if(item==1)snares++;else if(item==2)tonics++;
        else if(item==3){float before=hp;heal(maxHp()*.30f);notice="急救补给 · 恢复 "+Math.round(hp-before)+" 点生命";}
        else if(item==4){float before=soulEnergy;soulEnergy=Math.min(100,soulEnergy+35);notice="魂能结晶 · 恢复 "+Math.round(soulEnergy-before)+" 点魂能";}
        else if(damageBonus<40){damageBonus=Math.min(40,damageBonus+4);notice="战术改装 · 本轮伤害 +4";}
        else if(careerCooldownRank<8){careerCooldownRank++;notice="战术改装 · 职业技能冷却速度 +5%";}
        else{speedBonus=Math.min(6,speedBonus+1);notice="战术改装 · 移动速度 +8%";}
        if(item<3)notice=item==0?"购入爆裂瓶":item==1?"购入捕缚钉":"购入战斗兴奋剂";
        signal("purchase",x,y,item);return true;
    }
    public void useBomb(){
        if(mode!=Mode.PLAY||bombs<=0)return;bombs--;int hitCount=0;
        for(Enemy e:enemies)if(e.hp>0&&len(e.x-x,e.y-y)<270){hit(e,damage()*2.1f,false);hitCount++;}
        signal("slam",x,y,270);notice="爆裂瓶 · 波及 "+hitCount+" 名敌人";
    }
    public void useSnare(){
        if(mode!=Mode.PLAY||snares<=0)return;snares--;int caught=0;
        for(Enemy e:enemies)if(e.hp>0&&len(e.x-x,e.y-y)<300){e.stun=Math.max(e.stun,2.4f);hit(e,damage()*.25f,false);caught++;}
        signal("echo",x,y,300);notice="捕缚钉 · 控制 "+caught+" 名敌人";
    }
    public void useTonic(){
        if(mode!=Mode.PLAY||tonics<=0)return;tonics--;battleTonicTime=Math.max(battleTonicTime,10f);notice="战斗兴奋剂 · 10秒内伤害 +30%，攻速 +20%";
    }
    public void pause(){if(mode==Mode.PLAY){mode=Mode.PAUSED;notice="已暂停";}}
    public void resume(){if(mode==Mode.PAUSED)mode=Mode.PLAY;}
    public void returnCamp(){if(mode==Mode.RESULT){mode=Mode.CAMP;clearThreat();clearSoulRuntime();clearRelicRun();careerCooldownRank=0;careerWard=0;clearTactics();room=0;damageBonus=0;healthBonus=0;speedBonus=0;bombs=0;snares=0;tonics=0;battleTonicTime=0;shopOpen=false;roomCleared=false;shield=0;dodgeCharge=0;enemies.clear();shots.clear();blocks.clear();drops.clear();hazards.clear();followUpWindow=0;hp=maxHp();notice="冒险所得已结算。选择角色，再次出发。";}}
    public void abandon(){if(mode==Mode.PAUSED)finish(false);}
    public void dodge(float dx,float dy){
        if(mode!=Mode.PLAY||rollCd>0||rollTime>0)return;
        float len=len(dx,dy);if(len<.1f){dx=faceX;dy=faceY;len=1;}
        rollX=dx/len;rollY=dy/len;rollTime=.28f;rollCd=dodgePeriod();invulnerable=Math.max(invulnerable,.3f);attackAnim=0;
        dodgeCharge=rank(5)>0?3:0;float dodgeShield=8+coreRank(4)*4;addTalentShield(dodgeShield);
        if(rank(6)>0||coreRank(5)>0){float pulseRadius=120+coreRank(5)*24;for(Enemy e:enemies)if(len(e.x-x,e.y-y)<pulseRadius){hit(e,damage()*1.05f*(1+.25f*coreRank(5)));e.stun=Math.max(e.stun,.45f);}signal("echo",x,y,(int)pulseRadius);}
        if(rank(6)>0&&rank(11)>0)addTalentShield(3);
        if(rank(7)>0)skillCd=Math.max(0,skillCd-2.5f);
        if(hasRelic(3))relicEcho=3;
        signal("roll",x,y,0);
    }
    public void potion(){
        if(mode!=Mode.PLAY)return;
        if(potions<=0||hp>=maxHp()&&!(hasRelic(4)&&soulKind()==2&&corruption>0))return;
        potions--;if(hasRelic(4)){if(soulKind()==1)addSoulWard(8);if(soulKind()==2)corruption=Math.max(0,corruption-20);}heal(maxHp()*potionRatio()+(background()==4?6:0));
    }
    public void skill(){
        if(mode!=Mode.PLAY||rollTime>0||skillCd>0)return;
        int j=job();skillCd=skillPeriod();careerSkillMode=j+1;careerSkillTime=4f;float power=damage();int affected=0;
        if(j==HUNTER){
            if(careerTalentRank(2)>0)addTalentShield(8*careerTalentRank(2));
            Enemy target=null;float best=-1;for(Enemy e:enemies)if(e.hp>0){float score=e.type==3?e.hp*1.4f:e.hp;if(score>best){best=score;target=e;}}
            if(target!=null){target.hex=Math.max(target.hex,8f);hit(target,power*2.15f*(1+.15f*careerTalentRank(0)),false);signal("careerSkill",target.x,target.y,1);}
            for(Enemy e:enemies)if(e.hp>0&&e!=target&&len(e.x-x,e.y-y)<300)e.hex=Math.max(e.hex,3f);
            notice="猎人：追踪锁定 · 猎物已标记";
        }else if(j==ASSASSIN){
            aimAtNearest(600);float ox=x,oy=y;invulnerable=Math.max(invulnerable,.85f);
            for(int i=0;i<Math.min(64,44+careerTalentRank(0)*5);i++){if(!free(x+faceX*5,y+faceY*5,PLAYER_RADIUS))break;movePlayer(faceX*5,faceY*5);}
            for(Enemy e:enemies)if(e.hp>0&&segmentDistance(e.x,e.y,ox,oy,x,y)<e.radius()+35){hit(e,power*1.65f,false);affected++;}
            addTalentShield(8+affected*3);signal("careerSkill",x,y,2);notice="刺客：影袭 · 突进并获得影盾";
        }else if(j==DETECTIVE){
            addTalentShield(10+5*careerTalentRank(2));
            for(Enemy e:enemies)if(e.hp>0){e.hex=Math.max(e.hex,4.5f+.5f*careerTalentRank(0));e.stun=Math.max(e.stun,.3f);affected++;}
            signal("careerSkill",x,y,3);notice="警探：现场重构 · "+affected+" 个目标进入调查";
        }else{
            Enemy headline=null;float best=-1;for(Enemy e:enemies)if(e.hp>best&&e.hp>0){best=e.hp;headline=e;}
            if(headline!=null){hit(headline,power*1.8f,false);headline.hex=Math.max(headline.hex,5f);Enemy relay=null;float near=300;
                for(Enemy e:enemies)if(e!=headline&&e.hp>0){float d=len(e.x-headline.x,e.y-headline.y);if(d<near){near=d;relay=e;}}
                if(relay!=null){hit(relay,power*(.55f+.05f*careerTalentRank(0)),false);relay.hex=Math.max(relay.hex,3f);}
            }
            signal("careerSkill",x,y,4);if(careerTalentRank(1)>0)heal(maxHp()*.04f*careerTalentRank(1));if(careerTalentRank(2)>0)addTalentShield(4*careerTalentRank(2));notice="记者：头条发布 · 伤害向邻近目标传播";
        }
        if(soulKind()==1){addSoulWard(10);cleanse(x,y,120);}
        else if(soulKind()==2){corruption=Math.min(100,corruption+5);triggerRupture();}
    }
    public void followUp(){
        if(mode!=Mode.PLAY||followUpWindow<=0||rollTime>0)return;
        followUpWindow=0;aimAtNearest(680);int style=skillStyle();float ox=x,oy=y,range=(style==0?340:style==1?620:style==2?210:style==3?420:260);float followPower=damage()*(1+.12f*tacticRanks[1]);
        float endX=x+faceX*range,endY=y+faceY*range;boolean connected=false;
        if(style==0||style==1||style==2){
            if(style==2){invulnerable=Math.max(invulnerable,.35f);for(int i=0;i<Math.min(42,(int)(range/5));i++)movePlayer(faceX*5,faceY*5);endX=x;endY=y;}
            for(Enemy e:enemies)if(e.hp>0&&segmentDistance(e.x,e.y,ox,oy,endX,endY)<(style==0?42:style==1?22:34)+e.radius()&&lineClear(ox,oy,e.x,e.y)){
                if(style==1&&e.guard>0)breakEnemyGuard(e);float before=e.hp;hit(e,followPower*(specialtyRank(6)>0?1.15f:1)*(style==0?(e.stun>0?2.15f:1.55f):style==1?2.05f:1.8f));connected|=e.hp<before;
            }
        }else if(style==3){
            for(Enemy e:enemies)if(e.hp>0&&len(e.x-x,e.y-y)<range&&lineClear(x,y,e.x,e.y)&&(e.hex>0||e.stun>0)){float before=e.hp;hit(e,damage()*2.2f);connected|=e.hp<before;if(soulKind()==2&&e.hex>0){e.hex=0;heal(maxHp()*.025f);}if(soulKind()==1){e.stun=Math.max(e.stun,.35f);addSoulWard(2);}}
        }else{
            for(Enemy e:enemies)if(e.hp>0&&len(e.x-activeSkillX,e.y-activeSkillY)<190){float before=e.hp;hit(e,damage()*2.25f);connected|=e.hp<before;e.stun=Math.max(e.stun,.25f);if(soulKind()==2)e.hex=3;}
            signal("thunder",activeSkillX,activeSkillY,190);
        }
        if(soulKind()==2){payForbidden(.015f,8);for(Enemy e:enemies)if(e.hp>0&&e.hex>0&&len(e.x-x,e.y-y)<115)hit(e,damage()*.35f,false);}
        if(soulKind()==1){cleanse(x,y,125);if(connected)addSoulWard(4);}
        if(connected){
            resolveFollowUpChain(style,followPower);
            if(job()==HUNTER)careerSkillTime=Math.min(30,careerSkillTime+1.5f);
            if(job()==ASSASSIN)activeSkillCd=Math.max(0,activeSkillCd-2);
            if(job()==DETECTIVE)for(Enemy e:enemies)if(e.hp>0&&len(e.x-x,e.y-y)<160)e.stun=Math.max(e.stun,.25f);
            if(job()==REPORTER)activeSkillCd=Math.max(0,activeSkillCd-1);
            // A confirmed follow-up hit converts career identity into a distinct payoff.
            if(job()==HUNTER){soulBuff=Math.max(soulBuff,1.25f);if(soulKind()==1)addSoulWard(2);}
            else if(job()==ASSASSIN){double a=Math.atan2(faceY,faceX);throwKnife((float)Math.cos(a-.12),(float)Math.sin(a-.12),damage()*.20f);throwKnife((float)Math.cos(a+.12),(float)Math.sin(a+.12),damage()*.20f);}
            else if(job()==DETECTIVE){for(Enemy e:enemies)if(e.hp>0&&len(e.x-x,e.y-y)<520){e.hex=Math.max(e.hex,3.5f);e.stun=Math.max(e.stun,.35f);}}
            else {heal(maxHp()*.03f);gainSoul(4);}
        }
        signal("followup",ox,oy,style);notice=SkillSystem.followup(style);
    }
    /** Resolve the skill-specific second beat only after a confirmed follow-up hit. */
    private void resolveFollowUpChain(int style,float power){
        int affected=0;
        if(style==0){
            for(Enemy e:enemies)if(e.hp>0&&len(e.x-x,e.y-y)<145&&lineClear(x,y,e.x,e.y)){
                hit(e,power*.48f,false);e.stun=Math.max(e.stun,.28f);affected++;
            }
            signal("chainEarth",x,y,145);
        }else if(style==1){
            double a=Math.atan2(faceY,faceX);
            for(int n=-1;n<=1;n+=2){
                Shot s=new Shot(x+faceX*18,y+faceY*18,(float)Math.cos(a+n*.075)*720,(float)Math.sin(a+n*.075)*720,power*.38f,true);
                s.pierce=soulKind()==1?1:0;shots.add(s);
            }
            for(Enemy e:enemies)if(e.hp>0&&len(e.x-x,e.y-y)<560&&(e.hex>0||e.stun>0)){e.hex=Math.max(e.hex,2.5f);affected++;}
            signal("chainFan",x,y,2);
        }else if(style==2){
            invulnerable=Math.max(invulnerable,.28f);gainSoul(4);
            for(Enemy e:enemies)if(e.hp>0&&len(e.x-x,e.y-y)<125){hit(e,power*.42f,false);affected++;}
            signal("chainRing",x,y,125);
        }else if(style==3){
            for(Enemy e:enemies)if(e.hp>0&&len(e.x-x,e.y-y)<230){
                e.hex=Math.max(e.hex,soulKind()==2?5:3);e.stun=Math.max(e.stun,.3f);affected++;
            }
            signal("chainLink",x,y,230);
        }else{
            float radius=210;
            for(Enemy e:enemies)if(e.hp>0&&len(e.x-activeSkillX,e.y-activeSkillY)<radius&&lineClear(activeSkillX,activeSkillY,e.x,e.y)){
                hit(e,power*.62f,false);e.stun=Math.max(e.stun,.35f);if(soulKind()==2)e.hex=Math.max(e.hex,4);affected++;
            }
            signal("chainThunder",activeSkillX,activeSkillY,(int)radius);
        }
        if(soulKind()==1){addSoulWard(Math.min(5,2+affected));cleanse(x,y,95);}
        else if(soulKind()==2&&affected>0){for(Enemy e:enemies)if(e.hp>0&&e.hex>0&&len(e.x-x,e.y-y)<260)e.hex=Math.min(8,e.hex+.5f);}
    }
    private void resolveThunderTrap(){
        float radius=(soulKind()==1?220:190)*(1+.08f*tacticRanks[0]);
        for(Enemy e:enemies)if(e.hp>0&&len(e.x-activeSkillX,e.y-activeSkillY)<radius&&lineClear(activeSkillX,activeSkillY,e.x,e.y)){hit(e,damage()*(1+.20f*coreRank(8))*(1+.12f*tacticRanks[0])*(soulKind()==2?1.5f:1.25f),false);e.stun=Math.max(e.stun,soulKind()==1?.65f:.25f);if(soulKind()==2)e.hex=Math.max(e.hex,4);}
        if(soulKind()==1){cleanse(activeSkillX,activeSkillY,radius);addSoulWard(3);}signal("thunder",activeSkillX,activeSkillY,(int)radius);
    }
    public void activeSkill(){
        if(mode!=Mode.PLAY||rollTime>0||activeSkillCd>0||roomCleared)return;
        int style=skillStyle(),kind=soulKind();float power=damage()*(1+.20f*coreRank(8))*(specialtyRank(7)>0?1.12f:1)*(1+.12f*tacticRanks[0]),areaScale=(specialtyRank(7)>0?1.10f:1)*(1+.08f*tacticRanks[0]);aimAtNearest(650);activeSkillCd=activeSkillPeriod();followUpWindow=followUpDuration();
        if(kind==2)payForbidden(.025f,10);
        if(style==0){
            float radius=(kind==1?235:kind==2?210:190)*areaScale;
            for(Enemy e:enemies)if(e.hp>0&&len(e.x-x,e.y-y)<radius){hit(e,power*(kind==2?1.25f:1.4f),false);e.stun=Math.max(e.stun,kind==1?.8f:.4f);if(kind==2)e.hex=4;}
            if(kind==1){addSoulWard(4);cleanse(x,y,radius);}
        }else if(style==1){
            double angle=Math.atan2(faceY,faceX);int count=(kind==2?7:5)+tacticRanks[0];
            for(int i=0;i<count;i++){double a=angle+(i-(count-1)/2f)*.18;Shot shot=new Shot(x+faceX*20,y+faceY*20,(float)Math.cos(a)*610,(float)Math.sin(a)*610,power*(kind==2?.72f:.9f),true);shot.pierce=kind==1?2:0;shot.effect=kind==2?2:0;shots.add(shot);}
            if(kind==1)cleanse(x+faceX*80,y+faceY*80,90);
        }else if(style==2){
            invulnerable=Math.max(invulnerable,kind==1?.8f:.55f);float radius=(kind==1?185:160)*areaScale;
            for(Enemy e:enemies)if(e.hp>0&&len(e.x-x,e.y-y)<radius){hit(e,power*(kind==2?1.8f:1.55f),false);if(kind==2)e.hex=3;}
            if(kind==1)addSoulWard(6);if(kind==2)heal(maxHp()*.045f);
        }else if(style==3){
            Enemy target=null;float best=500*areaScale;
            for(Enemy e:enemies){float dx=e.x-x,dy=e.y-y,d=len(dx,dy);if(e.hp>0&&d<best&&d>1&&(dx*faceX+dy*faceY)/d>.35f&&lineClear(x,y,e.x,e.y)){best=d;target=e;}}
            if(target!=null){if(target.type==3)target.stun=Math.max(target.stun,.65f);else pull(target,x,y,(kind==1?190:kind==2?155:125)*areaScale);hit(target,power*(kind==2?1.5f:1.25f),false);if(kind==1){target.stun=Math.max(target.stun,1.1f);for(Enemy e:enemies)if(e!=target&&e.hp>0&&len(e.x-target.x,e.y-target.y)<120)hit(e,power*.45f,false);}if(kind==2)target.hex=4;}
        }else{
            activeSkillX=Math.max(70,Math.min(WIDTH-70,x+faceX*180));activeSkillY=Math.max(70,Math.min(HEIGHT-70,y+faceY*180));activeSkillDelay=.65f;signal("thunderWarning",activeSkillX,activeSkillY,kind==1?220:190);
        }
        if(job()==HUNTER){swingBoost=Math.max(swingBoost,1.18f);careerSkillTime=Math.min(30,careerSkillTime+1f);}
        if(job()==ASSASSIN){double a=Math.atan2(faceY,faceX);for(int n=-1;n<=1;n++)throwKnife((float)Math.cos(a+n*.24),(float)Math.sin(a+n*.24),power*.22f);}
        if(job()==DETECTIVE){for(Enemy e:enemies)if(e.hp>0&&len(e.x-x,e.y-y)<(style==0?190:520))e.hex=Math.max(e.hex,2.5f);}
        if(job()==REPORTER)activeSkillCd=Math.max(0,activeSkillCd-.8f);
        // Career-specific soul resonance: every profession converts the same soul
        // into a different tactical advantage when using its general active skill.
        if(kind==1){
            if(job()==HUNTER){soulWard=Math.min(99999,soulWard+6);cleanse(x,y,120);signal("radiant",x,y,120);}
            else if(job()==ASSASSIN){soulWard=Math.min(99999,soulWard+3);invulnerable=Math.max(invulnerable,.35f);}
            else if(job()==DETECTIVE){soulWard=Math.min(99999,soulWard+4);cleanse(x,y,180);for(Enemy e:enemies)if(e.hp>0&&e.hex>0&&len(e.x-x,e.y-y)<520)e.stun=Math.max(e.stun,.25f);}
            else {heal(maxHp()*.025f);soulWard=Math.min(99999,soulWard+2);}
        }else if(kind==2){
            if(job()==HUNTER){for(Enemy e:enemies)if(e.hp>0&&len(e.x-x,e.y-y)<220)e.hex=Math.max(e.hex,3);signal("forbidden",x,y,220);}
            else if(job()==ASSASSIN){double a=Math.atan2(faceY,faceX);for(int n=-2;n<=2;n+=2)throwKnife((float)Math.cos(a+n*.18),(float)Math.sin(a+n*.18),power*.16f);payForbidden(.01f,4);}
            else if(job()==DETECTIVE){for(Enemy e:enemies)if(e.hp>0&&e.hex>0&&len(e.x-x,e.y-y)<520){hit(e,power*.22f,false);e.stun=Math.max(e.stun,.35f);}}
            else {gainSoul(5);corruption=Math.min(100,corruption+3);triggerRupture();}
        }
        if(tacticRanks[5]>0||coreRank(7)>0)addTalentShield(3*tacticRanks[5]+4*coreRank(7));
        signal("activeSkill",x,y,style);notice=SkillSystem.name(style)+(kind==1?" · 光辉共鸣":kind==2?" · 禁忌共鸣":"");
    }
    public void soulSkill(){
        if(mode!=Mode.PLAY||soulKind()==0||soulCd>0||rollTime>0||roomCleared||enemies.isEmpty())return;
        if(soulEnergy<40){notice="专属技能需要40魂能，命中敌人可积累。";signal("soulLow",x,y,0);return;}
        int kind=soulKind(),j=job();soulEnergy-=40;soulCd=soulPeriod();aimAtNearest(600);float power=damage();int affected=0;
        if(kind==1){
            if(j==HUNTER){
                Enemy target=null;float best=-1;for(Enemy e:enemies)if(e.hp>0){float score=e.type==3?e.hp*1.5f:e.hp;if(score>best){best=score;target=e;}}
                addSoulWard(14);for(Enemy e:enemies)if(e.hp>0)e.hex=Math.max(e.hex,7f);
                if(target!=null){hit(target,power*3.1f,false);target.stun=Math.max(target.stun,.45f);}
                signal("radiant",x,y,1);notice="曙光猎令 · 全场标记，精准猎杀";
            }else if(j==ASSASSIN){
                float ox=x,oy=y;invulnerable=Math.max(invulnerable,1.1f);cleanse(x,y,180);
                for(int i=0;i<54;i++){if(!free(x+faceX*5,y+faceY*5,PLAYER_RADIUS))break;movePlayer(faceX*5,faceY*5);}
                for(Enemy e:enemies)if(e.hp>0&&segmentDistance(e.x,e.y,ox,oy,x,y)<e.radius()+42){hit(e,power*2.25f,false);affected++;}
                addSoulWard(10+affected*3);signal("radiant",x,y,2);notice="无影圣步 · 净化弹幕，突袭并获得辉盾";
            }else if(j==DETECTIVE){
                for(Enemy e:enemies)if(e.hp>0){e.hex=Math.max(e.hex,8f);e.stun=Math.max(e.stun,e.type==3?.2f:.75f);affected++;}
                addSoulWard(16);cleanse(x,y,320);signal("radiant",x,y,3);notice="真相显影 · 全场线索完成，弱点暴露";
            }else{
                Enemy headline=null;float best=-1;for(Enemy e:enemies)if(e.hp>best&&e.hp>0){best=e.hp;headline=e;}
                if(headline!=null){hit(headline,power*2.4f,false);Enemy relay=null;float near=360;
                    for(Enemy e:enemies)if(e!=headline&&e.hp>0){float d=len(e.x-headline.x,e.y-headline.y);if(d<near){near=d;relay=e;}}
                    if(relay!=null)hit(relay,power*.9f,false);}
                addSoulWard(10);heal(maxHp()*.03f);signal("radiant",x,y,4);notice="光辉头条 · 强化传播并支援自身";
            }
        }else{
            payForbidden(j==REPORTER?.10f:.08f,j==REPORTER?38:30);
            if(j==HUNTER){
                Enemy target=null;float best=Float.MAX_VALUE;for(Enemy e:enemies)if(e.hp>0){float d=len(e.x-x,e.y-y);if(d<best){best=d;target=e;}}
                if(target!=null){target.hex=Math.max(target.hex,9f);hit(target,power*(target.hp<target.maxHp*.35f?4.1f:3.3f),false);if(target.hp<=0)heal(maxHp()*.06f);}
                signal("forbidden",x,y,1);notice="血猎追魂 · 献祭生命，追猎残血目标";
            }else if(j==ASSASSIN){
                float ox=x,oy=y;invulnerable=Math.max(invulnerable,.75f);
                for(int i=0;i<48;i++){if(!free(x+faceX*5,y+faceY*5,PLAYER_RADIUS))break;movePlayer(faceX*5,faceY*5);}
                for(Enemy e:enemies)if(e.hp>0&&segmentDistance(e.x,e.y,ox,oy,x,y)<e.radius()+45){e.hex=Math.max(e.hex,6f);hit(e,power*2.8f,false);affected++;}
                if(affected>0)heal(Math.min(maxHp()*.05f,affected*maxHp()*.015f));corruption=Math.min(100,corruption+8);triggerRupture();signal("forbidden",x,y,2);notice="蚀影处决 · 生命换取连续影袭";
            }else if(j==DETECTIVE){
                for(Enemy e:enemies)if(e.hp>0&&e.hex>0){hit(e,power*(1.6f+Math.min(1.4f,e.hex*.2f)),false);e.hex=Math.max(e.hex,6f);e.stun=Math.max(e.stun,e.type==3?.15f:.55f);affected++;}
                if(affected==0)for(Enemy e:enemies)if(e.hp>0){e.hex=6f;hit(e,power*.9f,false);}
                signal("forbidden",x,y,3);notice="深渊审讯 · 以线索完整度裁定目标";
            }else{
                for(Enemy e:enemies)if(e.hp>0){e.hex=Math.max(e.hex,6f);hit(e,power*1.25f,false);}
                List<Enemy> sources=new ArrayList<>();for(Enemy e:enemies)if(e.hp>0)sources.add(e);
                for(Enemy source:sources){Enemy relay=null;float near=190;for(Enemy e:enemies)if(e!=source&&e.hp>0){float d=len(e.x-source.x,e.y-source.y);if(d<near){near=d;relay=e;}}
                    if(relay!=null)hit(relay,power*.35f,false);}
                corruption=Math.min(100,corruption+8);triggerRupture();signal("forbidden",x,y,4);notice="血色头条 · 诅咒扩散并引发连锁爆发";
            }
        }
        if(kind==1&&specialtyRank(2)>0)addSoulWard(6);
        if(j==HUNTER){soulBuff=Math.max(soulBuff,1.5f);if(careerSkillTime>0)careerSkillTime=Math.min(8,careerSkillTime+1.5f);}
        if(j==ASSASSIN)activeSkillCd=Math.max(0,activeSkillCd-1.5f);
        if(j==DETECTIVE)for(Enemy e:enemies)if(e.hp>0&&e.hex>0)e.hex=Math.min(10,e.hex+1f);
        if(j==REPORTER&&kind==1)gainSoul(4);
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
        if(soulKind()==2){float decay=enemies.stream().anyMatch(e->e.hp>0&&(e.state>0||len(e.x-x,e.y-y)<560))?1f:4f;corruption=Math.max(0,corruption-dt*decay);}
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
    public int careerKnifeCount(){return isSpec(ASSASSIN,2)?5:3;}
    private void beginAttack(){
        aimAtNearest(hero==1?640:meleeReach()+30);
        if(job()==ASSASSIN&&careerSkillMode==2&&careerSkillTime>0){double base=Math.atan2(faceY,faceX);int spread=(careerKnifeCount()-1)/2;for(int i=-spread;i<=spread;i++)throwKnife((float)Math.cos(base+i*.22),(float)Math.sin(base+i*.22),damage()*(spread==2?.28f:.35f));}
        attackCd=attackPeriod();attackAnim=hero==1?.18f:.24f;
        combo=comboWindow>0?(combo+1)%3:0;comboWindow=.85f;swingHits.clear();
        swingBoost=(dodgeCharge>0?1+rank(5)*(soulKind()==2?.20f:.15f):1)*(combo==2?1.35f+rank(1)*(soulKind()==2?.18f:.12f):1);dodgeCharge=0;
        if(combo==2&&rank(2)>0){boolean connected=false;for(Enemy e:enemies)if(e.hp>0&&len(e.x-x,e.y-y)<185){float before=e.hp;hit(e,damage()*.75f);connected|=e.hp<before;}if(connected&&rank(11)>0)addTalentShield(4);signal("echo",x,y,185);}
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
        roomElapsed=Math.min(10000000,roomElapsed+dt);battleTonicTime=dec(battleTonicTime,dt);followUpWindow=dec(followUpWindow,dt);activeSkillCd=dec(activeSkillCd,dt*(1f+careerCooldownRank*.05f+(job()==REPORTER&&careerSkillTime>0?.5f:0)));if(activeSkillDelay>0){activeSkillDelay=dec(activeSkillDelay,dt);if(activeSkillDelay<=0)resolveThunderTrap();}careerSkillTime=dec(careerSkillTime,dt);careerSkillDeathCd=dec(careerSkillDeathCd,dt);if(careerSkillTime<=0)careerSkillMode=0;
        if(careerSkillMode==1&&careerSkillTime>0&&careerSkillDeathCd<=0){float floor=maxHp()*.30f;if(hp>floor){float before=hp;hp=Math.max(floor,hp-maxHp()*.01f);signal("sacrifice",x,y,(int)Math.ceil(before-hp));}careerSkillDeathCd=1f;}
        updateSoul(dt);updateBurn(dt);relicEcho=dec(relicEcho,dt);time+=dt;attackCd=dec(attackCd,dt);rollCd=dec(rollCd,dt);skillCd=dec(skillCd,dt*careerCooldownRate());invulnerable=dec(invulnerable,dt);comboWindow=dec(comboWindow,dt);dodgeCharge=dec(dodgeCharge,dt);
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
        for(Enemy e:enemies)if(e.hp>0)updateEnemy(e,dt);
        if(mode!=Mode.PLAY)return;
        updateBossReinforcements(dt);
        updateHazards(dt);if(mode!=Mode.PLAY)return;updateShots(dt);if(mode!=Mode.PLAY)return;
        Iterator<Enemy> it=enemies.iterator();while(it.hasNext()){
            Enemy e=it.next();if(e.hp<=0){it.remove();addTalentShield(1);kills=Math.min(99999,kills+1);drops.add(new Drop(e.x,e.y,(e.type==3?55:5+room)+(background()==6?2:0),false));if(background()==5){heal(e.type==3?12:4);gainSoul(e.type==3?8:2);}if(random(6)==0)drops.add(new Drop(e.x+15,e.y,15,true));if(random(100)<ThreatSystem.potionChance(e.type)){Drop bottle=new Drop(e.x-15,e.y,1,false);bottle.potion=true;drops.add(bottle);}signal("defeat",e.x,e.y,e.type);}
        }
        Iterator<Drop> d=drops.iterator();while(d.hasNext()){
            Drop p=d.next();float dist=len(p.x-x,p.y-y);if(dist<100&&dist>1){float speed=270*dt;p.x+=(x-p.x)/dist*Math.min(speed,dist);p.y+=(y-p.y)/dist*Math.min(speed,dist);}
            if(dist<24&&collectDrop(p))d.remove();
        }
        if(enemies.isEmpty()&&!roomCleared){
            drops.removeIf(this::collectDrop);shots.clear();hazards.clear();ThreatSystem.clear(this);if(endless){bestDepth=Math.max(bestDepth,endlessDepth);if(room==5)loot=Math.min(99999,loot+35);}
            unlockStory();discoverRelic();if(room==5&&!endless)finish(true);else{mode=Mode.REWARD;prepareTacticDraft();}
        }
        if(roomCleared&&y<100){
            if(x<430){float recovery=springRatio();heal(maxHp()*recovery);advanceRoom();notice="穿过补给所，基础恢复"+Math.round(recovery*100)+"%生命（受治疗修正）。";}
            else if(x>690){loot=Math.min(99999,loot+12);advanceRoom();notice="穿过货栈，额外获得 12 票券。";}
        }
    }
    private void ignite(float duration,float damage){burnTime=Math.max(burnTime,duration);burnDamage=Math.max(burnDamage,damage);burnTick=Math.min(burnTick<=0?1f:burnTick,1f);signal("burn",x,y,(int)Math.ceil(burnTime));}
    private void updateBurn(float dt){if(burnTime<=0)return;float step=Math.min(dt,burnTime);burnTime=Math.max(0,burnTime-step);burnTick-=step;if(burnTick<=.0001f&&burnTime>0){burnTick=1f;hurt(burnDamage);signal("burn",x,y,(int)Math.ceil(burnTime));}if(burnTime<=0){burnTick=0;burnDamage=0;}}
    private void updateBossReinforcements(float dt){
        Enemy boss=null;int support=0;for(Enemy e:enemies)if(e.hp>0){if(e.type==3)boss=e;else support++;}
        if(boss==null){bossSummonCd=0;return;}bossSummonCd=Math.max(0,bossSummonCd-dt);if(support>=3||bossSummonCd>0)return;
        int type=4+random(3);float ex=560,ey=145;int tries=0;do{ex=100+random(920);ey=100+random(330);tries++;}while((!free(ex,ey,type==6?22:18)||len(ex-x,ey-y)<230||len(ex-boss.x,ey-boss.y)<110)&&tries<80);
        if(!free(ex,ey,type==6?22:18)){ex=Math.max(80,Math.min(WIDTH-80,boss.x+180));ey=Math.max(80,Math.min(HEIGHT-80,boss.y+120));}
        float life=90+room*22+(type==4?35:type==5?25:45);Enemy add=new Enemy(nextId++,type,ex,ey,Math.max(encounterHpFloor*1.35f,life*threat*1.45f));add.pattern=random(2);add.stun=.35f;
        if(type==4){add.guardType=2;add.guard=22+room*2;}else if(type==6){add.guardType=1;add.guard=30+room*2;}
        enemies.add(add);bossSummonCd=5.5f;signal("bossSummon",ex,ey,type);notice="首领召援 · 强敌加入战场";
    }
    private void addHazard(int type,float px,float py,float radius,float warning,float life,float power){
        if(hazards.size()<18)hazards.add(new Hazard(type,Math.max(64,Math.min(WIDTH-64,px)),Math.max(64,Math.min(HEIGHT-64,py)),enemyRange(radius),warning,life,power));
    }
    private void updateHazards(float dt){
        Iterator<Hazard> it=hazards.iterator();while(it.hasNext()){
            Hazard h=it.next();float wait=Math.min(h.warning,dt);h.warning=dec(h.warning,wait);float active=dt-wait;
            if(h.warning<=0&&active>0){
                if(h.type==0){
                    if(len(x-h.x,y-h.y)<h.radius+PLAYER_RADIUS&&lineClear(h.x,h.y,x,y)){hurt(h.damage);if(h.type==1)ignite(4f,Math.max(1.5f,h.damage*.10f));}
                    h.life=0;signal("hazard",h.x,h.y,(int)h.radius);
                }else{
                    float step=Math.min(h.life,active);h.life=dec(h.life,step);h.tick-=step;
                    if(h.tick<=.0001f){h.tick+=.65f;if(len(x-h.x,y-h.y)<h.radius+PLAYER_RADIUS&&lineClear(h.x,h.y,x,y)){hurt(h.damage);if(h.type==1)ignite(4f,Math.max(1.5f,h.damage*.10f));}signal("hazard",h.x,h.y,(int)h.radius);}
                }
            }
            if(h.warning<=0&&h.life<=0)it.remove();if(mode!=Mode.PLAY)return;
        }
    }
    private void aimEnemy(Enemy e,float prediction){
        e.targetX=Math.max(64,Math.min(WIDTH-64,x+motionX*prediction));e.targetY=Math.max(64,Math.min(HEIGHT-64,y+motionY*prediction));
        float dx=e.targetX-e.x,dy=e.targetY-e.y,d=Math.max(.01f,len(dx,dy));float angle=(random(2001)-1000)*.00018f;float cs=(float)Math.cos(angle),sn=(float)Math.sin(angle);e.aimX=(dx/d)*cs-(dy/d)*sn;e.aimY=(dx/d)*sn+(dy/d)*cs;
    }
    private void fan(Enemy e,int half,float spread,float speed,float power){double base=Math.atan2(e.aimY,e.aimX);for(int i=-half;i<=half;i++)enemyShot(e,(float)Math.cos(base+i*spread),(float)Math.sin(base+i*spread),speed,power);}
    private void updateEnemy(Enemy e,float dt){
        e.hex=dec(e.hex,dt*.35f);e.flash=dec(e.flash,dt);e.guardBreak=dec(e.guardBreak,dt);e.guardDelay=dec(e.guardDelay,dt);
        if(e.stun>0){e.stun=dec(e.stun,dt);return;}
        float dx=x-e.x,dy=y-e.y,distance=Math.max(.01f,len(dx,dy));boolean enraged=e.type==3&&e.hp<e.maxHp*.5f;
        if(e.state==0){
            float reach=enemyRange(e.type==0?90:e.type==1?520:e.type==2?360:e.type==3?550:e.type==4?300:e.type==5?520:420);
            if(distance<reach&&lineClear(e.x,e.y,x,y)){
                e.state=1;float windup=e.type==0?.42f:e.type==1?.7f:e.type==2?.7f:e.type==3?(enraged?.72f:1.05f):e.type==4?.55f:e.type==5?.75f:1;e.timer=windup*(.88f+random(25)*.01f);
                aimEnemy(e,e.type==1?.22f:e.type==4?.35f:e.type==5?.5f:0);e.struck=false;
            }else {
                float speed=enemySpeed(e.type==3?115:e.type==4?235:135+room*5);
                float side=(e.pattern%2==0?1:-1)*(e.type==4?.45f:distance>150?.2f:0);
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
                else if(e.type==6){e.timer=2;for(Enemy other:enemies)if(other!=e&&other.hp>0&&len(other.x-e.x,other.y-e.y)<260&&lineClear(e.x,e.y,other.x,other.y)){if(other.guardType==0)other.guardType=1;other.guard=Math.min(60,other.guard+24);other.guardDelay=2.5f;}fan(e,2,.3f,240,12);signal("enemyWard",e.x,e.y,260);}
                else {
                    e.timer=enraged?.62f:room>=4?.88f:1.08f;
                    if(e.pattern==0){float radius=room>=4?250:220;addHazard(1,e.x,e.y,radius,.3f,.1f,enemyPower(e,room>=4?34:28));int count=enraged?(room>=4?20:16):(room>=4?16:12);for(int i=0;i<count;i++){double a=i*Math.PI*2/count+time*.1;enemyShot(e,(float)Math.cos(a),(float)Math.sin(a),enraged?300:room>=4?255:220,room>=4?22:18);}}
                    else if(e.pattern==1)fan(e,enraged?(room>=4?4:3):(room>=4?3:2),.19f,room>=4?410:350,room>=4?24:20);
                    else {addHazard(1,e.targetX,e.targetY,room>=4?145:120,.8f,.1f,enemyPower(e,room>=4?36:30));addHazard(1,e.targetX-145,e.targetY+75,room>=4?125:110,1.15f,.1f,enemyPower(e,room>=4?29:24));addHazard(1,e.targetX+145,e.targetY-75,room>=4?125:110,1.5f,.1f,enemyPower(e,room>=4?29:24));}
                    signal("slam",e.x,e.y,e.pattern);
                }
            }
        }else if(e.state==3){e.timer-=dt;if(e.type==3){e.hp=Math.min(e.maxHp,e.hp+e.maxHp*(enraged?.012f:.006f)*dt);if(e.guardType==3&&e.guardBreak<=0&&e.guardDelay<=0)e.guard=Math.min(enraged?90:55,e.guard+(enraged?7.5f:4.5f)*dt);}if(e.timer<=0)e.state=0;}
        else {
            e.timer-=dt;
            if(e.type==2||e.type==4){float speed=enemySpeed(e.type==2?440:520);moveEnemy(e,e.aimX*speed*dt,e.aimY*speed*dt);if(!e.struck&&len(x-e.x,y-e.y)<e.radius()+PLAYER_RADIUS+7){e.struck=true;hurt(enemyPower(e,18+room*2));}}
            if((e.type==1||e.type==3&&e.pattern==1)&&!e.struck&&e.timer<(e.type==1?.5f:.4f)){e.struck=true;fan(e,e.type==1?1:2,.12f,e.type==1?330:370,e.type==1?12+room*2:20);}
            if(e.timer<=0){if(e.type==2||e.type==4){e.state=3;e.timer=e.type==2?.65f:.5f;}else if(e.type==3){e.state=3;e.timer=.9f*(.9f+random(21)*.01f);e.pattern=random(3);}else e.state=0;}
        }
        for(Enemy other:enemies)if(other!=e&&other.hp>0){float sx=e.x-other.x,sy=e.y-other.y,d=len(sx,sy),min=e.radius()+other.radius();if(d>.1&&d<min)moveEnemy(e,sx/d*30*dt,sy/d*30*dt);}
    }
    private float enemyForce(Enemy e){return e.type==3?(room==2?1.22f:room==4?1.32f:room>=5?1.42f:1.15f):1f;}
    private void enemyShot(Enemy e,float dx,float dy,float speed,float damage){if(shots.size()<160)shots.add(new Shot(e.x+dx*24,e.y+dy*24,dx*enemySpeed(speed),dy*enemySpeed(speed),enemyPower(e,damage),false));}
    private void updateShots(float dt){
        Iterator<Shot> it=shots.iterator();while(it.hasNext()){
            Shot p=it.next();p.life-=dt;float ox=p.x,oy=p.y,nx=p.x+p.vx*dt,ny=p.y+p.vy*dt;
            boolean gone=p.life<=0||!free(nx,ny,p.radius)||!lineClear(ox,oy,nx,ny);
            if(!gone&&p.friendly){for(Enemy e:enemies)if(e.hp>0&&!p.hitIds.contains(e.id)&&segmentDistance(e.x,e.y,ox,oy,nx,ny)<e.radius()+p.radius){
                p.hitIds.add(e.id);hit(e,p.damage);if(p.effect==2)e.hex=3;
                if(p.pierce<=0){gone=true;break;}p.pierce--;
            }}
            else if(!gone&&segmentDistance(x,y,ox,oy,nx,ny)<PLAYER_RADIUS+p.radius){hurt(p.damage);if(!p.friendly&&p.effect==1)ignite(4f,Math.max(1.5f,p.damage*.10f));gone=true;}
            p.x=nx;p.y=ny;if(gone)it.remove();
        }
    }
    private void breakEnemyGuard(Enemy e){e.guard=0;e.guardBreak=e.guardType==1?.75f:e.guardType==2?1.65f:2.4f;e.guardDelay=Math.max(e.guardDelay,3f);e.stun=Math.max(e.stun,e.guardType==2?.45f:.7f);signal("shieldBreak",e.x,e.y,e.guardType);}
    public void hitForTest(Enemy e,float value){hit(e,value,true);}
    public void hurtForTest(float value){hurt(value);}
    public void gainSoulForTest(float value){gainSoul(value);}
    private void hit(Enemy e,float value){hit(e,value,true);}
    private void hit(Enemy e,float value,boolean charge){hit(e,value,charge,charge);}
    private void hit(Enemy e,float value,boolean charge,boolean echoEligible){
        if(e.hp<=0)return;
        if(echoEligible){
            if(specialtyRank(5)>0&&e.hex>0)value*=1.10f;
            if(isSpec(HUNTER,1)&&len(e.x-x,e.y-y)>200)value*=1.18f;
            if(isSpec(HUNTER,2)&&len(e.x-x,e.y-y)<=200)value*=1.10f;
            if(isSpec(ASSASSIN,1)&&e.hp<=e.maxHp*.5f)value*=1.25f;
            if(isSpec(DETECTIVE,1)&&e.hex>0)value*=1.25f;
            if(isSpec(DETECTIVE,2)&&e.stun>0)value*=1.20f;
            if(isSpec(REPORTER,1)&&e.type==3)value*=1.15f;
            // Career identities shape every deliberate hit, not only the active skill.
            if(job()==HUNTER&&e.hex>0)value*=1.18f;if(job()==DETECTIVE&&e.hex>0)value*=1+.15f*careerTalentRank(1);
        }
        if(echoEligible&&relicEcho>0&&hasRelic(3)){value*=1.35f;relicEcho=0;signal("echo",e.x,e.y,65);}
        value*=e.hex>0?1.15f*(hasRelic(6)?1.1f:1)*(1+.12f*tacticRanks[4]):1;if(echoEligible&&coreRank(2)>0&&e.stun>0)value*=1+.12f*coreRank(2);if(echoEligible&&job()==ASSASSIN&&careerSkillMode==2&&careerSkillTime>0)value*=1+.10f*careerTalentRank(1);float beforeGuard=e.guard;float guardFactor=e.guardType==2?.65f:1f;float guardDamage=value*guardFactor;float absorbed=Math.min(e.guard,guardDamage);e.guard=Math.max(0,e.guard-absorbed);float healthLeak=Math.min(value,absorbed/guardFactor);value-=healthLeak;if(absorbed>0){e.guardDelay=2.5f;signal("enemyShield",e.x,e.y,(int)absorbed);}if(beforeGuard>0&&e.guard<=.0001f)breakEnemyGuard(e);float dealt=Math.min(e.hp,value);e.hp=Math.max(0,e.hp-value);
        e.flash=.12f;if(absorbed==0)e.stun=Math.max(e.stun,e.type==3?.03f:e.type==2||e.type==4?.06f:.12f);signal("hit",e.x,e.y,(int)value);
        if(dealt>0&&job()==HUNTER&&careerSkillMode==1&&careerSkillTime>0)heal(dealt*.12f);
        if(dealt>0&&echoEligible){
            if(job()==ASSASSIN&&len(e.x-x,e.y-y)<150)addTalentShield(1f);
            if(job()==DETECTIVE){e.hex=Math.max(e.hex,3.5f);skillCd=Math.max(0,skillCd-.05f);}
            if(job()==REPORTER){
                Enemy relay=null;float nearest=240f;
                for(Enemy candidate:enemies){if(candidate!=e&&candidate.hp>0){float d=len(candidate.x-e.x,candidate.y-e.y);if(d<nearest){nearest=d;relay=candidate;}}}
                if(relay!=null){hit(relay,dealt*(.20f+.05f*careerTalentRank(0)),false,false);signal("careerRelay",relay.x,relay.y,(int)(dealt*(.20f+.05f*careerTalentRank(0))));}
            }
        }
        if(job()==ASSASSIN&&careerSkillMode==2&&careerSkillTime>0&&dealt>0){addTalentShield(1+2*careerTalentRank(2));if(e.hp<=0){activeSkillCd=0;heal(maxHp()*.03f);}}
        if(charge&&dealt>0){gainSoul(Math.min(4,dealt*.08f));if(soulBuff>0)heal(Math.min(maxHp()*.015f,dealt*.08f));}
    }
    private void hurt(float value){
        if(invulnerable>0||mode!=Mode.PLAY)return;
        value*= (custom>=0&&profile().trait==1?1.12f:1)*(job()==DETECTIVE&&careerSkillMode==3&&careerSkillTime>0?.90f:1)*(job()==ASSASSIN&&careerSkillMode==2&&careerSkillTime>0?.75f:1)*(rank(3)>0?1.15f:1)*(soulKind()==2?1+corruption*.004f:1);
        float wardTaken=Math.min(soulWard,value);soulWard-=wardTaken;value-=wardTaken;
        float careerTaken=Math.min(careerWard,value);careerWard-=careerTaken;value-=careerTaken;
        float shieldBefore=shield;float talentTaken=Math.min(shield,value);shield-=talentTaken;value-=talentTaken;float absorbed=talentTaken+wardTaken+careerTaken;if(shieldBefore>0&&shield<=.0001f&&coreRank(11)>0){for(Enemy e:enemies)if(e.hp>0&&len(e.x-x,e.y-y)<145)hit(e,talentTaken*.35f*coreRank(11),false);signal("shieldBreak",x,y,145);}
        if(soulKind()==1&&absorbed>0)gainSoul(Math.min(6,absorbed*.2f));
        if(absorbed>0)signal("shield",x,y,(int)absorbed);if(talentTaken>0){for(Enemy e:enemies)if(len(e.x-x,e.y-y)<(rank(11)>0?150:95))hit(e,talentTaken*(rank(11)>0?.65f:.25f),true,false);}
        float lost=Math.min(hp,value);roomDamage=Math.min(100000,roomDamage+lost);hp=Math.max(0,hp-value);invulnerable=.65f;signal("hurt",x,y,(int)lost);
        if(hasRelic(7)&&lost>0){for(Enemy e:enemies)if(e.hp>0&&len(e.x-x,e.y-y)<120)hit(e,lost*.25f,false);signal("echo",x,y,120);}
        if(hp<=0&&hasRelic(8)&&!relicRevived){relicRevived=true;hp=maxHp()*.25f;invulnerable=1;signal("revive",x,y,0);}
        if(hp<=0)finish(false);
    }
    private void finish(boolean victory){
        if(mode==Mode.RESULT||mode==Mode.CAMP)return;
        won=victory;int rawReward=victory?loot+40:(int)(loot*.7f);int reward=Math.round(rawReward*(1+coreRank(13)*.05f));bank=Math.min(999999,bank+reward);
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
