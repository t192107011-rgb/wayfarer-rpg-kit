import io.wayfarer.rpg.core.*;
import static io.wayfarer.rpg.core.CareerSystem.*;

public final class CareerTests {
    private static int checks;
    private static void check(boolean ok,String why){checks++;if(!ok)throw new AssertionError(why);}
    private static void near(float a,float b,String why){check(Math.abs(a-b)<.05f,why+": "+a+" vs "+b);}
    public static void main(String[] args)throws Exception{
        check(CLASSES.length==4&&CLASSES[0].equals("猎人")&&CLASSES[1].equals("刺客")&&CLASSES[2].equals("侦探")&&CLASSES[3].equals("记者"),"only four rebuilt careers exist");
        ActionGame g=new ActionGame();check(g.job()==HUNTER,"new builds default to hunter");
        for(int job=HUNTER;job<=REPORTER;job++){check(g.chooseCareer(job,0,0),"career "+job+" selectable");check(g.job()==job,"career ID committed");}
        check(!g.chooseCareer(4,0,0),"retired IDs rejected");check(!g.chooseCareer(REPORTER,2,0),"specialization level gate");
        g.experience[0]=160;check(g.chooseCareer(REPORTER,2,4),"reporter specialization available at level three");
        g.bindSoul(2,1);g.start(3);ActionGame a=ActionSave.decode(ActionSave.encode(g));
        check(a.job()==REPORTER&&a.specialization()==2&&a.background()==4&&a.soulKind()==2,"new career, specialization, background and soul persist");
        g=new ActionGame();g.experience[0]=160;g.chooseCareer(HUNTER,1,0);g.start(4);g.blocks.clear();g.enemies.clear();
        ActionGame.Enemy e=new ActionGame.Enemy(g.nextId++,0,700,600,1000);g.enemies.add(e);g.skill();check(g.careerSkillMode==1&&g.careerSkillTime==5,"hunter active skill starts");
        g=new ActionGame();g.chooseCareer(ASSASSIN,0,0);g.start(5);float hp=g.maxHp();g.skill();near(g.shield,hp*.4f,"assassin gains 40 percent max-health shield");near(g.damage(),g.baseDamage()*1.5f,"assassin gains attack damage");
        g=new ActionGame();g.chooseCareer(DETECTIVE,0,0);g.start(6);g.blocks.clear();g.enemies.clear();g.enemies.add(new ActionGame.Enemy(g.nextId++,0,700,600,1000));g.skill();check(g.careerSkillMode==3&&g.careerSkillTime==3.5f,"detective gravity field starts");
        g=new ActionGame();g.chooseCareer(REPORTER,0,0);g.start(7);g.blocks.clear();g.enemies.clear();g.enemies.add(new ActionGame.Enemy(g.nextId++,0,700,600,1000));g.skill();check(g.careerSkillMode==4&&g.careerSkillTime==5,"reporter exposure starts");
        System.out.println("CAREER CHECKS PASSED: "+checks);
    }
}