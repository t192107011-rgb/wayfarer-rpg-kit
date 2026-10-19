package io.wayfarer.rpg.core;

import java.io.*;
import java.util.*;

/** Pure Java rules; Android rendering and persistence are separate adapters. */
public final class GameEngine {
    public static final int SAVE_VERSION=1, MAX_LEVEL=20;
    public String area="village";
    public int x=6,y=5,level=1,xp=0,hp=60,mp=20,gold=35,potions=3,ethers=1;
    public int sword=0,armor=0,quest=0,kills=0;
    public boolean bossDefeated=false;
    public final Set<String> cleared=new HashSet<>();
    public String battleId="";
    public int enemyHp=0,turn=0;
    public String message="欢迎来到风栖村。靠近长老，点击「交互」开始旅程。";
    private final Random random;
    public GameEngine() { this(new Random()); }
    public GameEngine(Random random) { this.random=Objects.requireNonNull(random); }
    public int maxHp() { return 60+(level-1)*12; }
    public int maxMp() { return 20+(level-1)*3; }
    public int attack() { return 10+(level-1)*3+sword*5; }
    public int defense() { return 3+(level-1)+armor*3; }
    public int nextXp() { return level*30; }
    public GameData.Area map() { return GameData.AREAS.get(area); }
    public boolean inBattle() { return !battleId.isEmpty(); }
    public GameData.Enemy enemy() { return inBattle()?GameData.ENEMIES.get(GameData.event(battleId).target):null; }
    public boolean visible(GameData.Event e) { return !cleared.contains(e.id); }
    public String questText() {
        if(quest==0) return "拜访村庄长老，接受委托";
        if(quest==1) return "清理林地敌人 "+Math.min(3,kills)+"/3，然后回村找长老";
        if(quest==2) return bossDefeated?"古塔已恢复宁静，回村向长老复命":"前往灯塔遗迹，击败古塔守卫";
        return "旅程完成！可继续探索，或以此扩展新章节。";
    }
    public void move(int dx,int dy) {
        if(inBattle()) { message="战斗中无法移动。"; return; }
        if(Math.abs(dx)+Math.abs(dy)!=1) return;
        int nx=x+dx,ny=y+dy;
        if(!map().walkable(nx,ny)) { message="前方无法通行。"; return; }
        GameData.Event e=map().eventAt(nx,ny);
        if(e!=null && visible(e) && (e.kind.equals("elder")||e.kind.equals("shop")||e.kind.equals("inn")||e.kind.equals("chest"))) {
            message="靠近后点击「交互」。"; return;
        }
        if(e!=null && e.id.equals("f_r") && quest<2) { message="先完成林地委托，长老会为你打开遗迹。"; return; }
        x=nx; y=ny;
        if(e!=null && visible(e)) {
            if(e.kind.equals("portal")) {
                String[] p=e.target.split(":"); area=p[0]; x=Integer.parseInt(p[1]); y=Integer.parseInt(p[2]);
                message="抵达："+map().name;
            } else if(e.kind.equals("enemy")) startBattle(e);
            else message="正在探索："+map().name;
        } else message="正在探索："+map().name;
    }
    private void startBattle(GameData.Event e) {
        battleId=e.id; enemyHp=enemy().hp; turn=0;
        message="遭遇 "+enemy().name+"！选择攻击、技能、防御、药水或逃跑。";
    }
    public GameData.Event nearby() {
        if(inBattle()) return null;
        for(GameData.Event e:map().events) if(visible(e) && Math.abs(e.x-x)+Math.abs(e.y-y)<=1 &&
            !e.kind.equals("enemy") && !e.kind.equals("portal")) return e;
        return null;
    }
    public void interact() {
        GameData.Event e=nearby();
        if(e==null) { message="附近没有可交互的角色或宝箱。"; return; }
        if(e.kind.equals("elder")) {
            if(quest==0) { quest=1; message="长老：清理三只林地敌人，再来找我。我会赠你通往灯塔的钥匙。"; }
            else if(quest==1 && kills>=3) { quest=2; addGold(60); potions=Math.min(99,potions+2); message="长老：做得好！获得 60 金币、2 瓶药水和遗迹通行许可。请让古塔重获宁静。"; }
            else if(quest==2 && bossDefeated) { quest=3; addGold(150); message="长老：灯火再次亮起。获得 150 金币。示例主线完成，感谢你的旅程！"; }
            else message="长老："+questText();
        } else if(e.kind.equals("chest")) {
            cleared.add(e.id); addGold(25); potions=Math.min(99,potions+1); message="打开宝箱：25 金币、1 瓶药水。";
        } else if(e.kind.equals("shop")) message="商人：补给与装备都准备好了。";
        else message="旅店老板：免费休息，恢复生命和法力；普通敌人会重新出现。";
    }
    public void rest() {
        GameData.Event e=nearby();
        if(e==null || !e.kind.equals("inn")) { message="需要在村庄旅店旁休息。"; return; }
        hp=maxHp(); mp=maxMp();
        cleared.removeIf(id->{GameData.Event v=GameData.event(id);return v!=null && v.kind.equals("enemy") && !v.target.equals("warden");});
        message="已恢复生命与法力。林地普通敌人刷新，宝箱与首领进度保留。";
    }
    public void buy(String item) {
        GameData.Event e=nearby();
        if(e==null || !e.kind.equals("shop")) { message="需要在商人旁购物。"; return; }
        int cost;
        switch(item) {
            case "potion": if(potions>=99) {message="药水已满。";return;} cost=15;break;
            case "ether": if(ethers>=99) {message="法力药水已满。";return;} cost=20;break;
            case "sword": if(sword==1) {message="已经装备铁剑。";return;} cost=60;break;
            case "armor": if(armor==1) {message="已经装备皮甲。";return;} cost=50;break;
            default: message="没有这种商品。";return;
        }
        if(gold<cost) {message="金币不足。";return;}
        gold-=cost;
        switch(item) { case "potion":potions++;break;case "ether":ethers++;break;case "sword":sword=1;break;case "armor":armor=1;break;default:break; }
        message="购买成功"+((item.equals("sword")||item.equals("armor"))?"，已自动装备。":"。");
    }
    public void useItem(String item) {
        if(!item.equals("potion") && !item.equals("ether")) {message="没有这种物品。";return;}
        if(item.equals("potion")) {
            if(potions==0) {message="药水不足。";return;}
            if(hp==maxHp()) {message="生命已满，未消耗药水。";return;}
            potions--; hp=Math.min(maxHp(),hp+40); message="使用药水，恢复最多 40 点生命。";
        } else {
            if(ethers==0) {message="法力药水不足。";return;}
            if(mp==maxMp()) {message="法力已满，未消耗药水。";return;}
            ethers--;mp=Math.min(maxMp(),mp+15);message="使用法力药水，恢复最多 15 点法力。";
        }
        if(inBattle()) counter(false);
    }
    public void act(String action) {
        if(!inBattle()) {message="当前没有战斗。";return;}
        if(action.equals("potion")||action.equals("ether")) {useItem(action);return;}
        if(action.equals("flee")) {
            if(enemy().id.equals("warden")) {message="古塔守卫封锁了退路，无法逃跑（不消耗回合）。";return;}
            if(random.nextInt(100)<70) {battleId="";enemyHp=0;turn=0;area="village";x=6;y=5;message="逃离战斗，回到了村庄。";return;}
            message="逃跑失败！";counter(false);return;
        }
        if(action.equals("guard")) {mp=Math.min(maxMp(),mp+3);message="防御，恢复 3 点法力，并减轻本回合伤害。";counter(true);return;}
        int damage;
        if(action.equals("skill")) {
            if(mp<5) {message="法力不足，技能需要 5 点法力。";return;}
            mp-=5;damage=Math.max(1,attack()*2-enemy().defense);message="使用星火斩，造成 "+damage+" 点伤害。";
        } else if(action.equals("attack")) {
            damage=Math.max(1,attack()-enemy().defense+random.nextInt(3));message="普通攻击造成 "+damage+" 点伤害。";
        } else {message="未知战斗指令。";return;}
        enemyHp=Math.max(0,enemyHp-damage);
        if(enemyHp==0) win(); else counter(false);
    }
    private void counter(boolean guard) {
        turn=Math.min(1_000_000,turn+1);
        boolean charged=turn%3==0;
        int damage=Math.max(1,enemy().attack-defense()+(charged?4:0));
        if(guard) damage=Math.max(1,damage/2);
        hp=Math.max(0,hp-damage);
        message+="\n"+enemy().name+(charged?"重击":"攻击")+"造成 "+damage+" 点伤害。";
        if(hp==0) {
            int loss=Math.min(20,gold);gold-=loss;battleId="";enemyHp=0;turn=0;area="village";x=6;y=5;hp=maxHp();mp=maxMp();
            message+="\n你被救回村庄，损失 "+loss+" 金币。任务和装备保留。";
        }
    }
    private void win() {
        GameData.Enemy e=enemy();cleared.add(battleId);
        if(e.id.equals("warden")) bossDefeated=true; else kills=Math.min(9999,kills+1);
        addGold(e.gold);xp+=e.xp;
        message+="\n胜利！经验 +"+e.xp+"，金币 +"+e.gold+"。";
        while(level<MAX_LEVEL && xp>=nextXp()) {xp-=nextXp();level++;hp=maxHp();mp=maxMp();message+="\n升至 "+level+" 级，生命与法力恢复！";}
        if(level==MAX_LEVEL) xp=0;
        battleId="";enemyHp=0;turn=0;
    }
    private void addGold(int amount) {gold=Math.min(999999,gold+amount);}
    public byte[] encode() throws IOException {
        Properties p=new Properties();
        p.setProperty("format","wayfarer-rpg");p.setProperty("version",Integer.toString(SAVE_VERSION));
        p.setProperty("area",area);p.setProperty("battle",battleId);p.setProperty("boss",Boolean.toString(bossDefeated));
        p.setProperty("cleared",String.join(",",new TreeSet<>(cleared)));
        String[] keys={"x","y","level","xp","hp","mp","gold","potions","ethers","sword","armor","quest","kills","enemyHp","turn"};
        int[] values={x,y,level,xp,hp,mp,gold,potions,ethers,sword,armor,quest,kills,enemyHp,turn};
        for(int i=0;i<keys.length;i++) p.setProperty(keys[i],Integer.toString(values[i]));
        ByteArrayOutputStream out=new ByteArrayOutputStream();p.store(out,"Wayfarer RPG save v1");return out.toByteArray();
    }
    public static GameEngine decode(byte[] bytes) throws IOException {return decode(bytes,new Random());}
    public static GameEngine decode(byte[] bytes,Random random) throws IOException {
        if(bytes==null || bytes.length>65536) throw new IOException("存档为空或超过 64 KiB");
        try {
            Properties p=new Properties();p.load(new ByteArrayInputStream(bytes));
            if(!"wayfarer-rpg".equals(p.getProperty("format")) || number(p,"version",1,1)!=SAVE_VERSION) throw new IllegalArgumentException("未知存档格式");
            GameEngine g=new GameEngine(random);
            g.area=p.getProperty("area");if(!GameData.AREAS.containsKey(g.area)) throw new IllegalArgumentException("地图不存在");
            g.x=number(p,"x",0,g.map().width()-1);g.y=number(p,"y",0,g.map().height()-1);
            if(!g.map().walkable(g.x,g.y)) throw new IllegalArgumentException("存档位置不可通行");
            g.level=number(p,"level",1,MAX_LEVEL);g.xp=number(p,"xp",0,g.level==MAX_LEVEL?0:g.nextXp()-1);
            g.hp=number(p,"hp",1,g.maxHp());g.mp=number(p,"mp",0,g.maxMp());g.gold=number(p,"gold",0,999999);
            g.potions=number(p,"potions",0,99);g.ethers=number(p,"ethers",0,99);
            g.sword=number(p,"sword",0,1);g.armor=number(p,"armor",0,1);g.quest=number(p,"quest",0,3);g.kills=number(p,"kills",0,9999);
            String boss=p.getProperty("boss");if(!"true".equals(boss)&&!"false".equals(boss)) throw new IllegalArgumentException("首领状态无效");
            g.bossDefeated=Boolean.parseBoolean(boss);
            String done=Objects.requireNonNull(p.getProperty("cleared"));
            if(!done.isEmpty()) for(String id:done.split(",",-1)) {
                GameData.Event e=GameData.event(id);
                if(e==null || !(e.kind.equals("enemy")||e.kind.equals("chest")) || !g.cleared.add(id)) throw new IllegalArgumentException("事件状态无效");
            }
            g.battleId=Objects.requireNonNull(p.getProperty("battle"));g.enemyHp=number(p,"enemyHp",0,100000);g.turn=number(p,"turn",0,1000000);
            if(g.inBattle()) {
                GameData.Event e=g.map().eventAt(g.x,g.y);
                if(e==null || !e.id.equals(g.battleId) || !e.kind.equals("enemy") || g.cleared.contains(e.id) || g.enemyHp<1 || g.enemyHp>g.enemy().hp) throw new IllegalArgumentException("战斗状态无效");
            } else if(g.enemyHp!=0 || g.turn!=0) throw new IllegalArgumentException("战斗残留状态无效");
            GameData.Event at=g.map().eventAt(g.x,g.y);
            if(at!=null && g.visible(at) && (at.kind.equals("elder")||at.kind.equals("shop")||at.kind.equals("inn")||at.kind.equals("chest"))) throw new IllegalArgumentException("角色位置与事件重叠");
            if((g.quest>=2 && g.kills<3) || (g.quest==3&&!g.bossDefeated) || g.bossDefeated!=g.cleared.contains("warden") || (g.bossDefeated&&g.quest<2) || (g.area.equals("ruins")&&g.quest<2)) throw new IllegalArgumentException("任务状态无效");
            g.message="已读取存档。";return g;
        } catch(IllegalArgumentException|NullPointerException e) {throw new IOException("存档校验失败："+e.getMessage(),e);}
    }
    private static int number(Properties p,String key,int min,int max) {
        int value=Integer.parseInt(p.getProperty(key));
        if(value<min || value>max) throw new IllegalArgumentException("数值越界："+key);
        return value;
    }
}
