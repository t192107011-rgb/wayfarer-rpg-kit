import io.wayfarer.rpg.core.*;
import static io.wayfarer.rpg.core.CareerSystem.*;

/** Regression checks for the four redesigned career abilities. */
public final class CareerAbilityTests {
    private static int checks;
    private static void check(boolean ok,String why){checks++;if(!ok)throw new AssertionError(why);}
    private static ActionGame arena(int career){
        ActionGame g=new ActionGame();g.chooseCareer(career,0,0);g.start(91);g.blocks.clear();g.enemies.clear();g.x=560;g.y=600;g.invulnerable=0;return g;
    }
    public static void main(String[] args)throws Exception{
        ActionGame g=arena(HUNTER);ActionGame.Enemy e=new ActionGame.Enemy(g.nextId++,0,700,600,1000);g.enemies.add(e);g.skill();
        check(g.careerSkillMode==1&&g.careerSkillTime==10&&e.hp<1000,"hunter burst and sustained state");
        g=arena(ASSASSIN);float basePeriod=g.attackPeriod();g.skill();
        check(g.careerSkillMode==2&&g.careerSkillTime==10&&g.attackPeriod()>basePeriod,"assassin activates knife state with a slight attack interval increase");
        g=arena(DETECTIVE);e=new ActionGame.Enemy(g.nextId++,0,720,600,1000);g.enemies.add(e);float x=e.x;g.skill();
        for(int i=0;i<20;i++){g.hitStop=0;g.update(1f/60f,new ActionGame.Input());}
        check(g.careerSkillMode==3&&e.x<x&&e.hp<1000,"detective field attracts and damages");
        g=arena(REPORTER);g.skill();check(g.careerSkillMode==4&&g.healingFactor()<.21f,"reporter attack state and healing penalty");
        g.careerCooldownRank=4;ActionGame restored=ActionSave.decode(ActionSave.encode(g));
        check(restored.careerCooldownRank==4&&restored.careerSkillMode==4,"v12 saves career cooldown and active state");
        g.soulCd=5;g.generateRoom();check(g.skillCd==0&&g.soulCd==0,"room transition resets both skill cooldowns");
        System.out.println("CAREER ABILITY CHECKS PASSED: "+checks);
    }
}
