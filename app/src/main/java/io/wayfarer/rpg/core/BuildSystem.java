package io.wayfarer.rpg.core;

/** Stable, data-driven character and talent definitions. IDs are save protocol values. */
public final class BuildSystem {
    private BuildSystem(){}
    public static final String[] WEAPONS={"长剑 · 弧形三连斩","长弓 · 自动瞄准","双刃 · 高速连击"};
    public static final String[] SKILLS={"震地 · 范围重击","箭雨 · 扇形投射","环刃 · 近身爆发与无敌"};
    public static final String[] ORIGINS={"炉乡 · 生命 +18 / 移速 -5%","林径 · 移速 +7% / 生命 -10","流亡 · 伤害 +2 / 生命 -8"};
    public static final String[] TRAITS={"守誓 · 药水恢复55% / 攻速降低","逐猎 · 闪避冷却 -0.12秒 / 受伤 +12%","寻火 · 技能冷却 -0.8秒 / 生命 -8"};
    public static final String[] COLORS={"苔绿","赭金","雾紫","霜蓝","绯红","月白"};
    public static final int[] PALETTE={0xFFA9C59A,0xFFC39C70,0xFF9A9FCE,0xFF7CBAC5,0xFFCB8586,0xFFD5D4C5};
    public static final int[] SKIN={0xFFD6B48E,0xFFAD7954,0xFF744E40};
    public static final String[] HAIR={"短发","长发","兜帽"};
    public static final class Profile {
        public String name="无名守灯者";
        public int weapon,skill,origin,trait,color,skin,hair,xp;
        public Profile copy(){Profile p=new Profile();p.name=name;p.weapon=weapon;p.skill=skill;p.origin=origin;p.trait=trait;p.color=color;p.skin=skin;p.hair=hair;p.xp=xp;return p;}
    }
    public static String cleanName(String value){
        if(value==null)throw new IllegalArgumentException("请输入角色名字");String n=value.trim();
        if(n.isEmpty()||n.codePointCount(0,n.length())>10)throw new IllegalArgumentException("名字需为1至10个字符");
        for(int i=0;i<n.length();){int cp=n.codePointAt(i);int type=Character.getType(cp);if(Character.isISOControl(cp)||type==Character.FORMAT||type==Character.SURROGATE)throw new IllegalArgumentException("名字包含不可见控制字符");i+=Character.charCount(cp);}
        return n;
    }
    public static void validate(Profile p){
        if(!cleanName(p.name).equals(p.name)||p.weapon<0||p.weapon>2||p.skill<0||p.skill>2||p.origin<0||p.origin>2||p.trait<0||p.trait>2||p.color<0||p.color>5||p.skin<0||p.skin>2||p.hair<0||p.hair>2||p.xp<0||p.xp>1000000)throw new IllegalArgumentException("角色配置无效");
    }
    public static final class Node {
        public final String name,description;public final int maxRank,parent,minLevel;
        Node(String n,String d,int max,int parent,int level){name=n;description=d;maxRank=max;this.parent=parent;minLevel=level;}
    }
    public static final String[] BRANCHES={"锋刃 · 连击与风险","逐影 · 闪避与节奏","守火 · 技能与续航"};
    public static final Node[] NODES={
        new Node("淬锋","每级所有攻击伤害 +2。",3,-1,1),
        new Node("三拍","每级第三次普攻的伤害倍率 +12%；长弓也计连击。",2,0,2),
        new Node("裂地回响","第三次普攻同时向周围140范围释放60%伤害震波。",1,1,4),
        new Node("孤焰","生命低于40%时伤害 +40%；受到伤害始终 +15%。",1,1,4),
        new Node("轻步","每级移速 +3%，闪避冷却 -0.07秒。",3,-1,1),
        new Node("借势","闪避后3秒内下一次普攻每级伤害 +15%，一次消耗。",2,4,2),
        new Node("残影破","闪避起点85范围造成75%伤害并短暂眩晕。",1,5,4),
        new Node("回环","每次有效闪避使主动技能剩余冷却减少2.5秒。",1,5,4),
        new Node("灯芯","每级生命上限 +8。",3,-1,1),
        new Node("余温","每级技能冷却 -0.4秒；施放技能获得6点护盾，上限随等级。",2,8,2),
        new Node("长明","每次施放主动技能恢复8%最大生命。",1,9,4),
        new Node("镜火","技能额外获得12点护盾；护盾受击时向100范围反震吸收量的50%。",1,9,4)
    };
    public static boolean ultimate(int id){return id%4>=2;}
    public static int points(int xp){return 2+Math.min(14,xp/80);}
    public static int spent(int[] ranks){int n=0;for(int v:ranks)n+=v;return n;}
    public static String lockReason(int[] ranks,int xp,int id){
        if(id<0||id>=NODES.length)return "未知天赋";Node n=NODES[id];
        if(ranks[id]>=n.maxRank)return "已学满";
        if(1+Math.min(19,xp/80)<n.minLevel)return "需要角色 Lv."+n.minLevel;
        if(n.parent>=0&&ranks[n.parent]<(ultimate(id)?2:1))return "需要「"+NODES[n.parent].name+"」"+(ultimate(id)?2:1)+"级";
        if(ultimate(id))for(int i=0;i<12;i++)if(ultimate(i)&&ranks[i]>0)return "终阶天赋只能选择一个，可在营地免费重置";
        if(spent(ranks)>=points(xp))return "没有可用天赋点，远征获得经验后再来";
        return "";
    }
    public static void validateTree(int[] ranks,int xp){
        if(ranks.length!=12||spent(ranks)>points(xp))throw new IllegalArgumentException("天赋点超支");int finals=0;
        for(int i=0;i<12;i++){Node n=NODES[i];if(ranks[i]<0||ranks[i]>n.maxRank)throw new IllegalArgumentException("天赋等级无效");
            if(ranks[i]>0){if(1+Math.min(19,xp/80)<n.minLevel||n.parent>=0&&ranks[n.parent]<(ultimate(i)?2:1))throw new IllegalArgumentException("缺少天赋前置");if(ultimate(i))finals++;}}
        if(finals>1)throw new IllegalArgumentException("终阶天赋互斥");
    }
}
