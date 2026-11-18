package io.wayfarer.rpg.core;

/** Four rebuilt careers with compact IDs. */
public final class CareerSystem {
    private CareerSystem(){}
    public static final int SPECIALIZATION_XP=160;
    public static final int HUNTER=0,ASSASSIN=1,DETECTIVE=2,REPORTER=3;
    public static final String[] CLASSES={"猎人","刺客","侦探","记者"};
    public static final String[] CLASS_DETAILS={
        "猎人：远程压制与近身控场。主动技能期间，子弹命中产生小范围溅射，近战范围增加35%。",
        "刺客：5秒突袭状态。受到伤害降低80%，获得相当于最大生命值40%的护盾，普攻伤害提高50%，移动速度提高25%。",
        "侦探：展开3.5秒广域引力场，持续吸附敌人并造成低额伤害；敌人死亡时引发范围追击，触发间隔至少2秒。",
        "记者：发动大范围攻击并进入群体曝光状态。之后攻击让敌人分担原攻击伤害的70%；场上只剩一个敌人时，攻击伤害提高至200%。"
    };
    public static final String[][] SPECS={{"无专精","狙猎","游击"},{"无专精"},{"无专精"},{"无专精","战地采访","调查报道"}};
    public static final String[][] SPEC_DETAILS={
        {"先选择职业；角色 Lv.3（160经验）后开放专精。","远距离有效攻击伤害+18%。","闪避后3秒内下一次有效攻击伤害+25%。"},
        {"该职业暂无专精。"},{"该职业暂无专精。"},
        {"先选择职业；角色 Lv.3（160经验）后开放专精。","对首领的有效攻击伤害+15%。","本轮结算角色经验+20%，向下取整。"}
    };
    public static final String[] BACKGROUNDS={"无额外经历","铁厂学徒","乡间向导","铁路信使","乡村医护","墓园看守","码头拾荒者"};
    public static final String[] BACKGROUND_DETAILS={
        "不附加背景被动。背景经历独立于原有出身与特质。",
        "工伤经验转化为防护：每次进入新区域获得10职能护盾。",
        "移速额外 +3%；补给所的基础恢复由18%提高到26%最大生命。",
        "生命低于35%时，移动速度额外 +12%。",
        "每瓶药水的基础恢复量额外 +6生命。",
        "击败敌人基础恢复1生命，并在缔结灵魂时获得2魂能，首领改为8魂能。",
        "每名敌人的票券掉落额外 +2。"
    };
    public static String invalid(int job,int spec,int background,int xp){
        if(job<0||job>=CLASSES.length||spec<0||spec>=SPECS[job].length||background<0||background>=BACKGROUNDS.length)return "职业、专精或背景编号无效";
        if(spec>0&&xp<SPECIALIZATION_XP)return "专精需要角色 Lv.3（160经验）";
        return "";
    }
    public static String name(int job,int spec){return CLASSES[job]+(spec>0?" · "+SPECS[job][spec]:"");}
}