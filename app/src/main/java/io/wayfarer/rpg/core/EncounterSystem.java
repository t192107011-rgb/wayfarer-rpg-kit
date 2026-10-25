package io.wayfarer.rpg.core;

/** Encounter identity and legible counterplay; IDs 0-3 remain compatible. */
public final class EncounterSystem {
    private EncounterSystem(){}
    public static final String[] NAMES={"失魂工人","封锁枪手","冲撞搬运工","失魂监工","追猎者","焚灯者","敲钟人"};
    public static final String[] COUNTERS={"贴近挥击，交错逼近","两轮扇射，近身会退让","蓄势直冲，冲完有恢复期","震波、齐射、裂地轮换；半血加速","侧向追踪后扑击，躲开锁定线","预判落点，投下持续燃烧区域","给附近同伴补盾，优先打断或破盾"};
    public static final int[][] GROUPS={{0,0,1,1,2},{0,1,2,4,4,0},{0,1,2,4,5,5,0},{0,1,2,4,5,6,0,1},{3,4,5}};
    public static final String[] FOLLOWUPS={"断层追击","破盾点射","穿影突刺"};
    public static final String[] FOLLOWUP_DETAILS={"震地后2秒内再按技能，沿朝向劈开260距离裂线；对仍在硬直的敌人伤害更高。","散射后2秒内再按技能，射出540距离的集中贯穿打击，先破除路径上的敌人护盾。","环刃后2秒内再按技能，沿朝向突进145距离并攻击路径，正在蓄招的敌人承受额外伤害。"};
}
