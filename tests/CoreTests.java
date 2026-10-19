import io.wayfarer.rpg.core.*;
import java.util.*;
import java.io.*;

/** Dependency-free integration/regression tests. Run with scripts/test-core.sh. */
public final class CoreTests {
    private static int checks=0;
    private static void check(boolean yes,String why) {checks++;if(!yes)throw new AssertionError(why);}
    private static GameEngine fresh(){return new GameEngine(new Random(42));}
    private static void roundtrip(GameEngine g) throws Exception {
        GameEngine restored=GameEngine.decode(g.encode(),new Random(42));
        Properties a=new Properties(),b=new Properties();a.load(new ByteArrayInputStream(g.encode()));b.load(new ByteArrayInputStream(restored.encode()));
        check(a.equals(b),"save roundtrip must preserve all state");
    }
    private static void rejects(GameEngine g,String key,String value) throws Exception {
        Properties p=new Properties();p.load(new ByteArrayInputStream(g.encode()));p.setProperty(key,value);
        ByteArrayOutputStream out=new ByteArrayOutputStream();p.store(out,"");
        boolean failed=false;try{GameEngine.decode(out.toByteArray());}catch(IOException e){failed=true;}
        check(failed,"must reject "+key+"="+value);
    }
    private static void walk(GameEngine g,int tx,int ty) {
        // BFS stays in this area, avoiding portals and live enemies except the target.
        GameData.Area area=g.map();Queue<int[]> q=new ArrayDeque<>();q.add(new int[]{g.x,g.y});
        Map<String,int[]> previous=new HashMap<>();previous.put(g.x+":"+g.y,null);
        while(!q.isEmpty()) {
            int[] p=q.remove();if(p[0]==tx&&p[1]==ty)break;
            for(int[] d:new int[][]{{1,0},{-1,0},{0,1},{0,-1}}) {
                int nx=p[0]+d[0],ny=p[1]+d[1];String key=nx+":"+ny;
                if(!area.walkable(nx,ny)||previous.containsKey(key))continue;
                GameData.Event e=area.eventAt(nx,ny);
                if(e!=null && g.visible(e) && !(nx==tx&&ny==ty))continue;
                previous.put(key,p);q.add(new int[]{nx,ny});
            }
        }
        check(previous.containsKey(tx+":"+ty),"path exists to "+tx+","+ty+" on "+g.area);
        List<int[]> path=new ArrayList<>();int[] p=new int[]{tx,ty};
        while(p!=null){path.add(p);p=previous.get(p[0]+":"+p[1]);}
        Collections.reverse(path);
        for(int i=1;i<path.size();i++){int[] a=path.get(i-1),b=path.get(i);g.move(b[0]-a[0],b[1]-a[1]);}
    }
    private static void fight(GameEngine g) throws Exception {
        check(g.inBattle(),"encounter started");int count=0;
        while(g.inBattle() && count++<100) {
            if(g.hp<25 && g.potions>0)g.act("potion");else g.act(g.mp>=5?"skill":"attack");
            roundtrip(g);
        }
        check(count<100,"battle terminates");
    }
    private static void completeStory() throws Exception {
        GameEngine g=fresh();roundtrip(g);
        walk(g,3,3);g.interact();check(g.quest==1,"quest accepted");
        walk(g,11,4);check(g.area.equals("forest"),"village to forest");
        walk(g,4,4);fight(g);walk(g,6,4);fight(g);walk(g,9,4);fight(g);
        check(g.kills==3 && g.level>=2,"three kills level up");
        walk(g,11,4);check(g.area.equals("forest"),"ruins locked until turn in");
        walk(g,1,4);walk(g,3,3);g.interact();check(g.quest==2,"quest turn in unlocks ruins");
        int gold=g.gold;g.interact();check(g.gold==gold,"reward not repeated");
        walk(g,9,3);g.buy("sword");g.buy("armor");check(g.sword==1&&g.armor==1,"equipment bought");
        gold=g.gold;g.buy("sword");check(g.gold==gold,"duplicate equipment not charged");
        walk(g,3,5);g.rest();check(g.hp==g.maxHp()&&g.mp==g.maxMp(),"inn heals");
        check(!g.cleared.contains("slime_a"),"inn respawns normal enemies");
        walk(g,11,4);walk(g,11,4);check(g.area.equals("ruins"),"forest to ruins");
        walk(g,9,4);g.act("flee");check(g.inBattle() && g.turn==0,"boss cannot flee without wasting turn");
        fight(g);check(g.bossDefeated,"boss defeated");roundtrip(g);
        walk(g,1,4);walk(g,1,4);walk(g,3,3);g.interact();check(g.quest==3,"story complete");
        gold=g.gold;g.interact();check(g.gold==gold,"ending rewards only once");
        walk(g,3,5);g.rest();check(g.cleared.contains("warden"),"boss never respawns");roundtrip(g);
        System.out.println("PASS complete story using movement, interactions and combat");
    }
    private static void battleRules() throws Exception {
        GameEngine g=fresh();g.area="forest";g.x=3;g.y=4;g.move(1,0);roundtrip(g);
        int hp=g.hp,mp=g.mp,enemy=g.enemyHp;g.move(1,0);check(g.x==4,"movement blocked during battle");
        g.mp=0;g.act("skill");check(g.hp==hp && g.enemyHp==enemy && g.turn==0,"insufficient MP consumes no turn");
        g.act("guard");check(g.mp==3&&hp-g.hp==2,"guard restores mana and halves damage");
        g.potions=0;hp=g.hp;g.act("potion");check(g.hp==hp&&g.turn==1,"empty inventory consumes no turn");
        g.potions=1;g.act("potion");check(g.potions==0&&g.turn==2,"potion consumes one turn");
        g.hp=1;g.gold=5;g.act("guard");check(!g.inBattle()&&g.area.equals("village")&&g.gold==0&&g.hp==g.maxHp(),"death recovers safely");roundtrip(g);
        g=fresh();g.area="forest";g.x=3;g.y=4;g.move(1,0);g.act("flee");check(!g.inBattle()&&g.area.equals("village"),"seeded escape works");roundtrip(g);
        g=fresh();int n=g.potions;g.useItem("potion");check(g.potions==n,"full HP preserves potion");
        g.hp=20;g.useItem("potion");check(g.hp==60&&g.potions==n-1,"out of combat potion");
        System.out.println("PASS combat costs, guard, escape and defeat");
    }
    private static void contentAndEconomy() throws Exception {
        GameData.validate();GameEngine g=fresh();g.x=1;g.y=1;g.move(-1,0);check(g.x==1,"wall collision");
        g.buy("potion");check(g.gold==35,"remote shop blocked");
        g.x=9;g.y=3;g.gold=0;int n=g.potions;g.buy("potion");check(g.potions==n,"cannot buy without gold");
        g.gold=100;g.potions=99;g.buy("potion");check(g.gold==100,"inventory cap preserves gold");
        g.area="forest";g.x=2;g.y=2;g.interact();int gold=g.gold;check(g.cleared.contains("forest_chest"),"chest opens");
        g.interact();check(g.gold==gold,"chest reward only once");roundtrip(g);
        System.out.println("PASS collision, content validation and economy");
    }
    private static void saves() throws Exception {
        GameEngine g=fresh();
        rejects(g,"version","2");rejects(g,"level","-1");rejects(g,"gold","999999999999");
        rejects(g,"area","unknown");rejects(g,"x","0");rejects(g,"hp","0");rejects(g,"mp","999");
        rejects(g,"cleared","not-an-event");rejects(g,"battle","slime_a");rejects(g,"quest","3");
        rejects(g,"boss","truthy");rejects(g,"enemyHp","1");rejects(g,"potions","100");
        rejects(g,"xp","30");rejects(g,"format","different-game");
        check(g.level==1&&g.hp==60,"failed decoding cannot mutate current game");
        boolean failed=false;try{GameEngine.decode(new byte[65537]);}catch(IOException e){failed=true;}check(failed,"save size limit");
        failed=false;try{GameEngine.decode("garbage".getBytes(java.nio.charset.StandardCharsets.UTF_8));}catch(IOException e){failed=true;}check(failed,"malformed save rejected");
        System.out.println("PASS save roundtrips and invalid data rejection");
    }
    public static void main(String[] args) throws Exception {completeStory();battleRules();contentAndEconomy();saves();System.out.println("ALL PASSED: "+checks+" checks");}
}
