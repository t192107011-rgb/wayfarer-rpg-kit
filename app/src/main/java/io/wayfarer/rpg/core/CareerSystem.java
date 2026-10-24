package io.wayfarer.rpg.core;

/** Stable class/specialization/background IDs; independent of weapon and origin. */
public final class CareerSystem {
    private CareerSystem(){}
    public static final int SPECIALIZATION_XP=160;
    public static final String[] CLASSES={"无职业","护路卫士","边地猎人","秘仪学者"};
    public static final String[] CLASS_DETAILS={"保持原有战斗属性。","护路巡逻磨练体魄。最大生命 +16，敌方伤害减少 8%。不减免禁忌献祭。","边地追踪训练。移动速度 +6%，可使用任意武器。","秘密结社的仪式训练。主动技能冷却缩短 0.4 秒，可使用任意武器。"};
    public static final String[][] SPECS={{"无专精"},{"无专精","堡垒","反击"},{"无专精","狙猎","游击"},{"无专精","施疗","仪式"}};
    public static final String[][] SPEC_DETAILS={
        {"先选择职业；角色 Lv.3（160经验）后开放专精。"},
        {"Lv.3 后可在两个专精中择一。","施放主动技能获得14职能护盾，重复授予取较高值。","被敌人实际扣除生命后，4秒内下一次有效攻击伤害 +25%，命中消耗。"},
        {"Lv.3 后可在两个专精中择一。","有效攻击命中时，若自身与目标距离超过200，伤害 +18%。魂技、领域和反震不触发。","闪避后3秒内下一次有效攻击伤害 +25%，命中消耗。"},
        {"Lv.3 后可在两个专精中择一。","施放主动技能基础恢复3%最大生命，并获得8职能护盾。","主动技能伤害 +15%，灵魂专属技能冷却缩短10%；魂技伤害不增加。"}
    };
    public static final String[] BACKGROUNDS={"无额外经历","铁厂学徒","乡间向导","铁路信使","乡村医护","墓园看守","码头拾荒者"};
    public static final String[] BACKGROUND_DETAILS={
        "不附加背景被动。背景经历独立于原有出身与特质。",
        "工伤经验转化为防护：每次进入新区域获得10职能护盾。与专精护盾取较高值，不叠加。",
        "移速额外 +3%；补给所的基础恢复由18%提高到26%最大生命。",
        "生命低于35%时，移动速度额外 +12%，脱离低血状态后结束。",
        "每瓶药水的基础恢复量额外 +6生命，再应用灵魂与圣杯的治疗修正。",
        "击败敌人基础恢复1生命，并在缔结灵魂时获得2魂能，首领改为8魂能。任意击杀来源均可触发，每个敌人仅一次。",
        "每名敌人的票券掉落额外 +2，首领也生效。仅增加敌人掉落，不增加门、圣遗物补偿或结算奖励。"
    };
    public static String invalid(int job,int spec,int background,int xp){
        if(job<0||job>=CLASSES.length||spec<0||spec>=SPECS[job].length||background<0||background>=BACKGROUNDS.length)return "职业、专精或背景编号无效";
        if(spec>0&&xp<SPECIALIZATION_XP)return "专精需要角色 Lv.3（160经验）";
        return "";
    }
    public static String name(int job,int spec){return CLASSES[job]+(spec>0?" · "+SPECS[job][spec]:"");}
}
