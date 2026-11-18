import io.wayfarer.rpg.core.*;
import static io.wayfarer.rpg.core.CareerSystem.*;

/** Regression checks for the rebuilt career abilities; the retired pact system no longer exists. */
public final class PactTests {
    private static int checks;
    private static void check(boolean ok,String why){checks++;if(!ok)throw new AssertionError(why);}
    public static void main(String[] args)throws Exception{
        ActionGame g=new ActionGame();
        check(g.chooseCareer(HUNTER,0,0),"hunter selectable");g.start(1);g.blocks.clear();g.enemies.clear();
        ActionGame.Enemy e=new ActionGame.Enemy(g.nextId++,0,700,600,1000);g.enemies.add(e);g.skill();
        check(g.careerSkillMode==1&&g.careerSkillTime>0,"hunter mode active");
        g=new ActionGame();g.chooseCareer(ASSASSIN,0,0);g.start(2);g.skill();
        check(g.careerSkillMode==2&&g.shield>=g.maxHp()*.4f,"assassin defense active");
        g=new ActionGame();g.chooseCareer(DETECTIVE,0,0);g.start(3);g.skill();
        check(g.careerSkillMode==3&&g.careerSkillTime>0,"detective field active");
        g=new ActionGame();g.chooseCareer(REPORTER,0,0);g.start(4);g.skill();
        check(g.careerSkillMode==4&&g.careerSkillTime>0,"reporter sharing active");
        ActionGame restored=ActionSave.decode(ActionSave.encode(g));
        check(restored.job()==REPORTER&&restored.careerSkillMode==4,"reporter state restores");
        System.out.println("CAREER ABILITY CHECKS PASSED: "+checks);
    }
}