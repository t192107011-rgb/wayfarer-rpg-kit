package io.wayfarer.rpg.core;

/** Stable relic IDs. Collection is shared, loadouts are per character. */
public final class RelicSystem {
    private RelicSystem(){}
    public static final int STARTERS=6, ALL=510, DISCOVERY_ROOMS=26;
    public static final String[] NAMES={"空槽","余烬灯芯","旅人罗盘","回响铜铃","慈悲圣杯","日冕碎片","渎誓指环","荆棘圣印","逆命沙漏"};
    public static final String[] DETAILS={
        "不装备圣遗物。",
        "最大生命 +12。与等级、营地和天赋加成叠加。",
        "闪避冷却缩短 0.15 秒，与步法天赋及轻装特质叠加。",
        "闪避后 3 秒内，下一次有效攻击伤害 +35%，命中即消耗。普攻、主动技能与攻击天赋可触发；魂技、领域及反震不触发。",
        "所有治疗量 +10%。喝药时：光辉额外获得 12 辉盾（取较高值）；禁忌先净化 20 侵蚀，再恢复生命。满血且有侵蚀时也可喝药。",
        "缔结灵魂时，每轮远征初始魂能 +15；光辉再额外 +15。未缔结时不生效，不改变 100 点魂能上限。",
        "对受诅咒敌人造成的伤害额外 +10%；禁忌技能的生命献祭减少 25%。不减少侵蚀，不减免敌方伤害。",
        "实际损失生命的敌方攻击，将该次生命损失的 25% 反震给 120 范围内敌人。完全被护盾吸收时不触发；反震不回魂能、不吸血、不触发铜铃。",
        "每轮远征一次，受到致命敌方伤害时保留 25% 最大生命并获得 1 秒无敌。此生命保留不受治疗修正；跨房间、暂停和读档均不会补充次数。"
    };
    public static boolean owns(int mask,int id){return id==0||id>0&&id<NAMES.length&&(mask&(1<<id))!=0;}
    public static String invalid(int mask,int first,int second){
        if(first<0||first>=NAMES.length||second<0||second>=NAMES.length)return "圣遗物编号无效";
        if(!owns(mask,first)||!owns(mask,second))return "尚未发现这件圣遗物";
        if(first!=0&&first==second)return "同一角色不能重复装备同一件圣遗物";
        return "";
    }
}
