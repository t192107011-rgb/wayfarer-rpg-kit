package io.wayfarer.rpg.core;

/** Stable, data-driven character and talent definitions. IDs are save protocol values. */
public final class BuildSystem {
    private BuildSystem(){}
    public static final String[] WEAPONS={"军刀 · 弧形三连斩","卡宾枪 · 自动瞄准","双匕首 · 高速连击"};
    public static final String[] SKILLS=SkillSystem.NAMES.clone();
    public static final String[] ORIGINS={"工业镇 · 生命 +18 / 移速 -5%","乡村 · 移速 +7% / 生命 -10","流民 · 伤害 +2 / 生命 -8"};
    public static final String[] TRAITS={"守誓 · 药水恢复55% / 攻速降低","逐猎 · 闪避冷却 -0.12秒 / 受伤 +12%","寻火 · 技能冷却 -0.8秒 / 生命 -8"};
    public static final String[] COLORS={"苔绿","赭金","雾紫","霜蓝","绯红","月白"};
    public static final int[] PALETTE={0xFFA9C59A,0xFFC39C70,0xFF9A9FCE,0xFF7CBAC5,0xFFCB8586,0xFFD5D4C5};
    public static final int[] SKIN={0xFFD6B48E,0xFFAD7954,0xFF744E40};
    public static final String[] HAIR={"短发","长发","便帽"};
    public static final class Profile {
        public String name="无名守灯者";
        public int weapon,skill,origin,trait,color,skin,hair,xp,soul,soulArt;
        public Profile copy(){Profile p=new Profile();p.name=name;p.weapon=weapon;p.skill=skill;p.origin=origin;p.trait=trait;p.color=color;p.skin=skin;p.hair=hair;p.xp=xp;p.soul=soul;p.soulArt=soulArt;return p;}
    }
    public static String cleanName(String value){
        if(value==null)throw new IllegalArgumentException("请输入角色名字");String n=value.trim();
        if(n.isEmpty()||n.codePointCount(0,n.length())>10)throw new IllegalArgumentException("名字需为1至10个字符");
        for(int i=0;i<n.length();){int cp=n.codePointAt(i);int type=Character.getType(cp);if(Character.isISOControl(cp)||type==Character.FORMAT||type==Character.SURROGATE)throw new IllegalArgumentException("名字包含不可见控制字符");i+=Character.charCount(cp);}
        return n;
    }
    public static void validate(Profile p){
        SoulSystem.validate(p.soul,p.soulArt);
        if(!cleanName(p.name).equals(p.name)||p.weapon<0||p.weapon>2||p.skill<0||p.skill>=SKILLS.length||p.origin<0||p.origin>2||p.trait<0||p.trait>2||p.color<0||p.color>5||p.skin<0||p.skin>2||p.hair<0||p.hair>2||p.xp<0||p.xp>1000000)throw new IllegalArgumentException("角色配置无效");
    }
    public static final class Node {
        public final String name,description;public final int maxRank,parent,minLevel;
        Node(String n,String d,int max,int parent,int level){name=n;description=d;maxRank=max;this.parent=parent;minLevel=level;}
    }
    public static final String[] BRANCHES={"锋刃 · 连击与风险","逐影 · 闪避与节奏","守火 · 技能与续航"};
    public static final String[] SPECIALTY_BRANCHES={"灵契 · 魂能循环","猎场 · 战斗专精"};
    public static final Node[] SPECIALTIES={
        new Node("灵脉","每级远征开始时额外获得8点魂能。",2,-1,1),
        new Node("深流","每级魂技冷却缩短0.8秒。",2,0,2),
        new Node("魂盾","光辉灵魂施放专属技能时额外获得12点魂盾。",1,1,4),
        new Node("回响汲取","魂能获取提高20%。",1,1,4),
        new Node("猎意","每级所有攻击伤害提高5%。",2,-1,1),
        new Node("猎场识别","对带诅咒标记的目标伤害提高10%。",1,4,2),
        new Node("终结追击","追击伤害提高15%。",1,5,4),
        new Node("战术布置","主动技能伤害提高12%，范围提高10%。",1,5,4)
    };
    public static boolean specialtyUltimate(int id){return id==2||id==3||id==6||id==7;}\n    public static int specialtyPrerequisite(int id){return id==2||id==3?2:1;}
    public static String specialtyLockReason(int[] ranks,int xp,int id){
        if(id<0||id>=SPECIALTIES.length)return "未知专精";
        Node n=SPECIALTIES[id];if(ranks[id]>=n.maxRank)return "已学满";
        if(1+Math.min(19,xp/80)<n.minLevel)return "需要角色 Lv."+n.minLevel;
        if(n.parent>=0&&ranks[n.parent]<specialtyPrerequisite(id))return "需要「"+SPECIALTIES[n.parent].name+"」"+specialtyPrerequisite(id)+"级";
        if(specialtyUltimate(id)){int first=id<4?2:6;for(int i=first;i<first+2;i++)if(i!=id&&ranks[i]>0)return "该分支终阶二选一";}
        return "";
    }
    public static void validateSpecialties(int[] ranks,int xp,int talentSpent){
        if(ranks.length!=8)throw new IllegalArgumentException("专精结构无效");
        int spent=0;for(int i=0;i<8;i++){Node n=SPECIALTIES[i];if(ranks[i]<0||ranks[i]>n.maxRank)throw new IllegalArgumentException("专精等级无效");spent+=ranks[i];
            if(ranks[i]>0){if(1+Math.min(19,xp/80)<n.minLevel||n.parent>=0&&ranks[n.parent]<specialtyPrerequisite(i))throw new IllegalArgumentException("缺少专精前置");}}
        if(spent+talentSpent>points(xp))throw new IllegalArgumentException("天赋与专精点数超支");
        if(ranks[2]>0&&ranks[3]>0||ranks[6]>0&&ranks[7]>0)throw new IllegalArgumentException("专精终阶互斥");
    }

    public static final Node[] NODES={
        new Node("淬锋","每级所有攻击伤害 +2。",3,-1,1),
        new Node("三拍","每级第三次普攻的伤害倍率 +12%；卡宾枪也计连击。",2,0,2),
        new Node("裂地回响","第三次普攻同时向周围185范围释放75%伤害震波；与镜火组合时，命中可补充最多4护盾。",1,1,4),
        new Node("孤焰","生命低于40%时伤害 +55%、近战与范围技能覆盖 +25%；受到伤害始终 +15%。",1,1,4),
        new Node("轻步","每级移速 +3%，闪避冷却 -0.07秒。",3,-1,1),
        new Node("借势","闪避后3秒内下一次普攻每级伤害 +15%，一次消耗。",2,4,2),
        new Node("残影破","闪避起点120范围造成105%伤害并短暂眩晕；与镜火组合时有效闪避补6护盾。",1,5,4),
        new Node("回环","每次有效闪避使主动技能剩余冷却减少2.5秒；追击窗口延长至3秒，追击伤害+20%。",1,5,4),
        new Node("灯芯","每级生命上限 +8。",3,-1,1),
        new Node("余温","每级技能冷却 -0.4秒；施放技能获得6点护盾，上限随等级。",2,8,2),
        new Node("长明","每次施放主动技能恢复8%最大生命；追击实际命中再恢复3%，孤焰低血时改为6%；与回环组合时命中追击再减0.8秒技能冷却。",1,9,4),
        new Node("镜火","技能额外获得12点护盾；护盾受击时向150范围反震吸收量的65%；追击实际命中补6天赋护盾。",1,9,4)
    };
    public static boolean ultimate(int id){return id%4>=2;}
    public static int points(int xp){return 2+Math.min(16,xp/80);}
    public static int spent(int[] ranks){int n=0;for(int v:ranks)n+=v;return n;}
    public static String lockReason(int[] ranks,int xp,int id){
        if(id<0||id>=NODES.length)return "未知天赋";Node n=NODES[id];
        if(ranks[id]>=n.maxRank)return "已学满";
        if(1+Math.min(19,xp/80)<n.minLevel)return "需要角色 Lv."+n.minLevel;
        if(n.parent>=0&&ranks[n.parent]<(ultimate(id)?2:1))return "需要「"+NODES[n.parent].name+"」"+(ultimate(id)?2:1)+"级";
        if(ultimate(id))for(int i=id/4*4+2;i<id/4*4+4;i++)if(ranks[i]>0)return "本类别的终阶二选一，其他类别可分别选择；营地可免费重置";
        if(spent(ranks)>=points(xp))return "没有可用天赋点，远征获得经验后再来";
        return "";
    }
    public static void validateTree(int[] ranks,int xp){
        if(ranks.length!=12||spent(ranks)>points(xp))throw new IllegalArgumentException("天赋点超支");int[] finals=new int[3];
        for(int i=0;i<12;i++){Node n=NODES[i];if(ranks[i]<0||ranks[i]>n.maxRank)throw new IllegalArgumentException("天赋等级无效");
            if(ranks[i]>0){if(1+Math.min(19,xp/80)<n.minLevel||n.parent>=0&&ranks[n.parent]<(ultimate(i)?2:1))throw new IllegalArgumentException("缺少天赋前置");if(ultimate(i))finals[i/4]++;}}
        for(int n:finals)if(n>1)throw new IllegalArgumentException("同类别终阶天赋互斥");
    }
    public static void validateLegacyTree(int[] ranks,int xp){validateTree(ranks,xp);int n=0;for(int i=0;i<ranks.length;i++)if(ultimate(i)&&ranks[i]>0)n++;if(n>1||spent(ranks)>2+Math.min(14,xp/80))throw new IllegalArgumentException("旧版天赋树不合法");}
    public static String doctrine(int[] ranks){StringBuilder b=new StringBuilder();for(int branch=0;branch<3;branch++)for(int id=branch*4+2;id<branch*4+4;id++)if(ranks[id]>0){if(b.length()>0)b.append(" / ");b.append(NODES[id].name);}return b.length()==0?"尚未定型 · 每类终阶可选一":b.toString();}
}
