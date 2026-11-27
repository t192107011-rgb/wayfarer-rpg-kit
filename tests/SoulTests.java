import io.wayfarer.rpg.core.*;
import static io.wayfarer.rpg.core.CareerSystem.*;
import java.io.*;
import java.util.*;
public final class SoulTests {
    private static int count;
    private static void check(boolean ok,String why){count++;if(!ok)throw new AssertionError(why);}
    private static ActionGame game(int career,int kind){
        ActionGame g=new ActionGame();g.chooseCareer(career,0,0);if(kind>0)g.bindSoul(kind,0);g.start(91);
        g.blocks.clear();g.enemies.clear();g.shots.clear();g.x=560;g.y=600;g.faceX=1;g.faceY=0;g.invulnerable=0;g.threat=1;g.encounterHpFloor=0;g.encounterDamageFloor=0;return g;
    }
    private static ActionGame.Enemy mob(ActionGame g,float x,float y){ActionGame.Enemy e=new ActionGame.Enemy(g.nextId++,0,x,y,1000);e.stun=1;g.enemies.add(e);return e;}
    private static boolean signal(ActionGame g,String name){for(ActionGame.Signal s:g.signals)if(s.kind.equals(name))return true;return false;}
    private static void bindings()throws Exception{
        ActionGame g=new ActionGame();g.experience[0]=160;g.bank=200;check(g.bindSoul(1,1),"bind preset soul");g.selectHero(1);check(g.soulKind()==0,"preset souls remain independent");
        g.selectCustom(0);g.bindSoul(2,1);g.selectHero(0);check(g.soulKind()==1&&g.soulArt()==1&&g.bank==200,"custom and preset bindings remain independent");
        ActionGame restored=ActionSave.decode(ActionSave.encode(g));restored.selectCustom(0);check(restored.soulKind()==2&&restored.soulArt()==1,"custom binding persists");
        g.start(90);check(!g.bindSoul(2,0)&&g.soulKind()==1,"cannot swap soul during a run");check(g.soulEnergy==40,"fresh run begins with forty energy");
        g.pause();g.abandon();g.returnCamp();check(g.soulEnergy==0&&g.soulKind()==1,"camp clears runtime but preserves binding");
        for(int[] pair:new int[][]{{0,1},{3,0},{-1,0},{1,5}}){boolean bad=false;try{SoulSystem.validate(pair[0],pair[1]);}catch(IllegalArgumentException e){bad=true;}check(bad,"invalid binding rejected");}
        System.out.println("PASS soul binding, save compatibility and camp-only swaps");
    }
    private static void eightCareerForms(){
        for(int career=0;career<5;career++){
            ActionGame radiant=game(career,1);ActionGame.Enemy e=mob(radiant,700,600);radiant.soulEnergy=100;radiant.soulSkill();
            check(radiant.soulEnergy>=60&&radiant.soulEnergy<=100&&radiant.soulCd>0&&signal(radiant,"radiant"),"radiant form spends energy and triggers career effect "+career);
            check(e.hp<1000||e.hex>0||radiant.soulWard>0,"radiant form has an actual combat or defense result "+career);
            ActionGame taboo=game(career,2);e=mob(taboo,700,600);taboo.soulEnergy=100;float hp=taboo.hp;taboo.soulSkill();
            check(taboo.soulEnergy==60&&taboo.soulCd>0&&signal(taboo,"forbidden"),"taboo form spends energy and triggers career effect "+career);
            check(taboo.hp<hp&&taboo.corruption>0,"taboo form exacts a life and erosion cost "+career);
        }
        System.out.println("PASS all ten profession-bound radiant and forbidden forms");
    }
    private static void energyAndCooldown(){
        ActionGame g=game(0,1);mob(g,700,600);g.soulEnergy=39;g.soulSkill();check(g.soulCd==0&&g.soulEnergy==39,"insufficient energy blocks soul form");
        g.soulEnergy=100;g.soulSkill();float cd=g.soulCd;check(cd>0&&g.soulEnergy==60,"soul form consumes forty energy and starts cooldown");
        g.soulEnergy=100;g.soulSkill();check(g.soulCd==cd&&g.soulEnergy==100,"cooldown prevents repeat even with full energy");
        g.pause();float energy=g.soulEnergy;g.update(1,new ActionGame.Input());check(g.soulEnergy==energy,"pause freezes soul resources");
        System.out.println("PASS soul energy gate, cooldown and pause");
    }
    private static void costsAndSynergies(){
        ActionGame g=game(0,1);g.hp=10;g.potion();check(g.hp>10,"radiance amplifies healing");
        g=game(0,2);ActionGame.Enemy e=mob(g,g.x+100,g.y);g.corruption=50;g.hp=10;g.potion();check(g.hp>10&&g.hp<10+g.maxHp()*.45f,"corruption reduces healing");
        float before=g.hp;g.hurtForTest(10);check(g.hp<before,"corruption increases incoming damage");
        g=game(0,2);g.corruption=80;mob(g,700,600);g.soulEnergy=100;g.soulSkill();check(g.corruption>=100&&g.ruptureTime>0&&g.ruptureLatched,"taboo form crossing one hundred triggers rupture");
        System.out.println("PASS radiant healing, taboo risk and overrun threshold");
    }
    private static void saves()throws Exception{
        ActionGame g=game(TIMEKEEPER,2);mob(g,700,600);g.soulEnergy=80;g.soulSkill();g.pause();byte[] bytes=ActionSave.encode(g);
        ActionGame a=ActionSave.decode(bytes);check(a.job()==TIMEKEEPER&&a.soulKind()==2,"Timekeeper career and soul binding survive save");check(a.soulEnergy==g.soulEnergy&&a.corruption==g.corruption&&a.soulCd==g.soulCd,"energy, erosion and cooldown survive save");
        reject(bytes,"soulEnergy","101");reject(bytes,"soulCd","21");reject(bytes,"corruption","NaN");reject(bytes,"profile0.soul","3");reject(bytes,"profile0.art","5");reject(bytes,"soulFields","20");
        System.out.println("PASS soul state persistence and malformed save rejection");
    }
    private static void reject(byte[] bytes,String key,String value)throws Exception{Properties p=new Properties();p.load(new ByteArrayInputStream(bytes));p.setProperty(key,value);ByteArrayOutputStream out=new ByteArrayOutputStream();p.store(out,"");boolean bad=false;try{ActionSave.decode(out.toByteArray());}catch(IOException e){bad=true;}check(bad,"reject malformed "+key);}
    private static void stress()throws Exception{
        for(int career=0;career<5;career++)for(int kind=1;kind<=2;kind++){
            ActionGame g=game(career,kind);mob(g,700,600);g.soulEnergy=100;g.soulSkill();
            for(int t=0;t<900;t++){if(t%60==0){g=ActionSave.decode(ActionSave.encode(g));check(g.hp>=0&&g.soulEnergy>=0&&g.soulEnergy<=100&&g.corruption<=100,"bounded soul stress state");}g.update(1f/60,new ActionGame.Input());g.signals.clear();}
        }
        System.out.println("PASS periodic save/restore stress across ten soul forms");
    }
    public static void main(String[] args)throws Exception{bindings();eightCareerForms();energyAndCooldown();costsAndSynergies();saves();stress();System.out.println("SOUL CHECKS PASSED: "+count);}
}
