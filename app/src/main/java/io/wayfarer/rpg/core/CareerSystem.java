package io.wayfarer.rpg.core;

/** Four rebuilt careers with compact IDs. */
public final class CareerSystem {
    private CareerSystem(){}
    public static final int SPECIALIZATION_XP=160;
    public static final int HUNTER=0,ASSASSIN=1,DETECTIVE=2,REPORTER=3;
    public static final String[] CLASSES={"猎人","刺客","侦探","记者"};
    public static final String[] SKILL_NAMES={"瞬时猎杀","刀尖之舞","灯中之影","静默之声"};
    public static final String[] CLASS_DETAILS={
        "猎人：瞬时猎杀。发动时造成大范围伤害，随后30秒内攻速与移速大幅提高并获得吸血；期间每秒损失1%最大生命，生命降至30%时停止损失。",
        "刺客：刀尖之舞。持续30秒，攻击额外扇形发射3把小刀；击杀立即重置职业技能冷却并恢复3%最大生命，攻速小幅降低。",
        "侦探：灯中之影。持续30秒生成全场吸引场，持续吸引敌人并造成低额伤害；职业技能冷却速度提高，受到的攻击伤害提高15%。",
        "记者：静默之声。持续30秒，大幅提高攻速与职业技能冷却速度；治疗效果降低80%。"
    };
    public static final String[][] SPECS={{"无专精","狙猎","游击"},{"无专精","处决者","幽步"},{"无专精","审讯官","鉴证员"},{"无专精","战地采访","调查报道"}};
    public static final String[][] SPEC_DETAILS={
        {"先选择职业；角色 Lv.3（160经验）后开放专精。","对距离超过200的目标造成伤害+18%。","移速+8%，闪避冷却-0.12秒，适合贴身游击。"},
        {"先选择职业；角色 Lv.3（160经验）后开放专精。","目标生命低于50%时，造成伤害+25%。","普攻扇形飞刀由3把增至5把，单刀伤害略低。"},
        {"先选择职业；角色 Lv.3（160经验）后开放专精。","对带有诅咒标记的目标造成伤害+25%。","对眩晕中的目标造成伤害+20%。"},
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