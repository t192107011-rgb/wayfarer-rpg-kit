package io.wayfarer.rpg.core;

/** Stable soul IDs: 0 unbound, 1 radiance, 2 forbidden; each bound soul has two arts. */
public final class SoulSystem {
    private SoulSystem(){}
    public static final String[] NAMES={"未缔结","光辉灵魂","禁忌灵魂"};
    public static final String[][] SKILLS={{"震地","箭雨","环刃"},{"圣钟震地","折光箭雨","守誓环刃"},{"深渊牵引","咒棘箭雨","噬影环刃"}};
    public static final String[][] ARTS={{"无专属技能","无专属技能"},{"曙光圣域","裁决光矛"},{"裂隙之井","血契狂宴"}};
    public static final float[][] COOLDOWNS={{0,0},{16,12},{16,18}};
    public static final String[] PASSIVES={
        "维持原有技能与天赋，不使用魂能。旧存档默认保持未缔结。",
        "治疗量 +15%；主动技能冷却 +0.5秒并获得辉盾。灯芯每级额外+2生命，余温每级额外+4辉盾。",
        "主动技能冷却 -0.6秒；施放消耗4%最大生命并增加18侵蚀，生命最低保留1。每点侵蚀使受伤+0.4%、治疗-0.3%；停用3秒后每秒消退4点。三拍与借势每级额外+6%与+5%伤害。"
    };
    public static final String[][] DETAILS={
        {"范围重击。","五发扇形箭。","近身爆发与短暂无敌。"},
        {"圣钟震地：范围160→195，伤害1.8→1.65倍；眩晕0.9秒，净化范围内敌方弹丸。",
         "折光箭雨：五发改为三发，每发1.5倍伤害、最多命中三个不同敌人；障碍仍会挡住箭。",
         "守誓环刃：范围125→140，伤害2.2→1.8倍；无敌0.85秒，额外辉盾与弹丸净化。"},
        {"深渊牵引：范围220、1.5倍伤害，向自身拉近敌人并施加3秒诅咒；首领抗牵引。",
         "咒棘箭雨：七发扇形箭，每发1.05倍伤害，命中施加3秒诅咒。",
         "噬影环刃：范围150、2.6倍伤害并施加诅咒；无敌缩短至0.35秒。"}
    };
    public static final String[][] ART_DETAILS={
        {"先缔结灵魂。","先缔结灵魂。"},
        {"消耗40魂能，冷却16秒。获得30辉盾，在原地展开5秒圣域；每秒灼烧175范围敌人、净化弹丸，留在其中每秒恢复2%生命。",
         "消耗40魂能，冷却12秒。沿瞄准方向释放520距离光矛，造成3.6倍伤害、净化路径弹丸并获得12辉盾；墙体会阻挡伤害。"},
        {"消耗40魂能，冷却16秒；消耗8%最大生命、增加30侵蚀。前方展开4秒裂隙，每0.5秒牵引、诅咒并造成0.5倍伤害。",
         "消耗40魂能，冷却18秒；消耗8%最大生命、增加30侵蚀。周围爆发1.2倍伤害，进入6秒血契：伤害+25%、普攻间隔-20%，有效攻击吸取伤害的8%生命（每次不超过最大生命的1.5%，仍受侵蚀影响）。"}
    };
    public static void validate(int kind,int art){if(kind<0||kind>2||art<0||art>1||kind==0&&art!=0)throw new IllegalArgumentException("灵魂配置无效");}
    public static String talentExtra(int kind,int node){
        if(kind==1&&node==8)return "光辉共鸣：每级额外 +2 生命。";
        if(kind==1&&node==9)return "光辉共鸣：每级额外 +4 辉盾。";
        if(kind==2&&node==1)return "禁忌共鸣：每级第三次普攻额外 +6% 伤害倍率。";
        if(kind==2&&node==5)return "禁忌共鸣：每级借势额外 +5% 伤害。";
        return "";
    }
}
