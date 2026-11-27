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
        "震地：以自身为中心释放圆形冲击波，对范围内敌人造成伤害并施加短暂硬直。光辉形态扩大作用范围、延长硬直并净化范围内敌方弹幕；禁忌形态造成较低基础倍率、施加诅咒，并额外消耗生命与增加侵蚀。",
        "散射：朝当前自动瞄准方向发射扇形弹幕。基础形态发射5枚投射物；光辉形态的投射物可贯穿敌人并净化前方弹幕；禁忌形态发射7枚较低单发伤害的诅咒弹，并为命中目标添加标记。",
        "环刃：以自身为中心发动近身爆发，对周围敌人造成伤害并获得短暂无敌。光辉形态扩大范围、延长无敌并获得辉盾；禁忌形态提高爆发倍率、为命中目标添加诅咒，并恢复少量生命。",
        "锁链：沿瞄准方向寻找首个可命中的敌人并将其牵引；首领等大型目标不会被强行拉动，而是受到短暂硬直。光辉形态牵引距离更远、额外震荡附近敌人；禁忌形态以更高伤害命中并施加诅咒。",
        "雷霆：在角色面向方向约180距离处布置陷阱，经过0.65秒延迟后在指定区域爆发。光辉形态扩大爆炸范围、造成较长硬直并净化敌方弹幕；禁忌形态提高爆发伤害并施加诅咒。"
    };

    public static final String[] FOLLOWUP_DETAILS = {
        "断层追击：沿自动瞄准方向劈出长距离裂线。普通目标受到高额伤害，硬直目标受到更高倍率；命中后在自身周围追加一次小范围震荡并造成短暂硬直。",
        "破盾点射：沿瞄准方向发动远距离贯穿点射，命中带护盾目标时优先击破其护盾；随后补射两枚高速弹，并为已被控制或标记的目标补充线索。",
        "穿影突刺：沿瞄准方向突进并攻击路径上的敌人，突进期间获得短暂无敌；抵达后追加近身环刃伤害，并补充魂能。",
        "断罪回斩：对附近带有线索标记或处于硬直的敌人发动高倍率斩击；命中后在较大范围内追加标记与短暂硬直。",
        "雷狱引爆：以场上已布置的雷霆陷阱为中心追加爆发，对陷阱周围敌人造成高额伤害与短暂硬直；必须先成功布置陷阱。"
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
