package io.wayfarer.rpg.core;

/**
 * Stable IDs and design data for the five general active skills.
 * Career overdrive and soul arts remain separate action systems.
 * Runtime execution is wired by ActionGame; this registry is deliberately
 * side-effect free so UI, saves and combat can share the same IDs.
 */
public final class SkillSystem {
    private SkillSystem() {}

    public static final int EARTHQUAKE = 0;
    public static final int FAN_SHOT = 1;
    public static final int RING_BURST = 2;
    public static final int CHAIN_PULL = 3;
    public static final int THUNDER_TRAP = 4;
    public static final int COUNT = 5;

    public static final String[] NAMES = {
        "震地 · 范围重击",
        "散射 · 扇形投射",
        "环刃 · 近身爆发与无敌",
        "锁链 · 牵引突袭",
        "雷霆 · 延时陷阱"
    };

    public static final String[] FOLLOWUPS = {
        "断层追击",
        "破盾点射",
        "穿影突刺",
        "断罪回斩",
        "雷狱引爆"
    };

    public static final String[] BASE_DETAILS = {
        "自身周围释放震荡波，造成范围伤害并打断敌人。",
        "向前方扇形发射多枚投射物。",
        "近身爆发攻击，命中后获得短暂无敌。",
        "向前发射锁链，牵引首个可牵引敌人；大型敌人改为短暂硬直。",
        "在指定位置布置延时陷阱，爆发造成范围伤害并减速。"
    };

    public static final String[] FOLLOWUP_DETAILS = {
        "沿朝向劈出裂线，对硬直目标造成额外伤害。",
        "释放集中贯穿攻击，优先破除敌人护盾。",
        "沿朝向突进并攻击路径上的敌人，对蓄招目标造成额外伤害。",
        "斩击被牵引或硬直的目标；可触发对应灵魂的诅咒/辉光追加效果。",
        "引爆场上雷霆陷阱，对范围内敌人造成追加伤害。"
    };

    public static void validate(int id) {
        if (id < 0 || id >= COUNT) throw new IllegalArgumentException("主动技能编号无效");
    }

    public static String name(int id) {
        validate(id);
        return NAMES[id];
    }

    public static String followup(int id) {
        validate(id);
        return FOLLOWUPS[id];
    }
}
