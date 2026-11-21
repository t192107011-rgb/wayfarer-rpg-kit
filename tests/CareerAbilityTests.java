import io.wayfarer.rpg.core.*;
import static io.wayfarer.rpg.core.CareerSystem.*;

/** Regression checks for the four redesigned career abilities. */
public final class CareerAbilityTests {
    private static int checks;
    private static void check(boolean ok,String why){checks++;if(!ok)throw new AssertionError(why);}
    private static ActionGame arena(int career){
        ActionGame g=new ActionGame();g.chooseCareer(career,0,0);g.start(91);g.experience[g.hero]=160;g.blocks.clear();g.enemies.clear();g.x=560;g.y=600;g.invulnerable=0;return g;
    }
    public static void main(String[] args)throws Exception{
        ActionGame g=arena(HUNTER);ActionGame.Enemy e=new ActionGame.Enemy(g.nextId++,0,700,600,1000);g.enemies.add(e);g.skill();
        check(g.careerSkillMode==1&&g.careerSkillTime==30&&e.hp<1000,"hunter burst and sustained state");
        g=arena(ASSASSIN);float basePeriod=g.attackPeriod();g.skill();
        check(g.careerSkillMode==2&&g.careerSkillTime==30&&g.attackPeriod()>basePeriod,"assassin activates knife state with a slight attack interval increase");
        g.activeSkillCd=8;g.skillCd=11;ActionGame.Enemy victim=new ActionGame.Enemy(g.nextId++,0,600,600,1);g.enemies.add(victim);g.hitForTest(victim,20);
        check(g.activeSkillCd==0&&g.skillCd==11,"assassin kills refresh generic active skill, not career skill");
        g=arena(DETECTIVE);e=new ActionGame.Enemy(g.nextId++,0,720,600,1000);g.enemies.add(e);float x=e.x;g.skill();
        for(int i=0;i<20;i++){g.hitStop=0;g.update(1f/60f,new ActionGame.Input());}
        check(g.careerSkillMode==3&&e.y<600&&e.hp<1000,"detective field attracts toward arena center and damages");
        ActionGame.Enemy archer=new ActionGame.Enemy(g.nextId++,1,800,380,1000);g.enemies.add(archer);archer.state=1;archer.timer=.01f;
        g.hitStop=0;g.update(.03f,new ActionGame.Input());
        check(archer.state==2&&g.shots.size()>0,"detective attraction does not interrupt enemy attack AI");
        g=arena(REPORTER);g.skill();check(g.careerSkillMode==4&&g.healingFactor()<.21f,"reporter attack state and healing penalty");
        g.careerCooldownRank=4;ActionGame restored=ActionSave.decode(ActionSave.encode(g));
        check(restored.careerCooldownRank==4&&restored.careerSkillMode==4,"v13 saves career cooldown and active state");
        g=arena(HUNTER);g.careers[g.buildId()][1]=2;check(g.speed()>205&&g.dodgePeriod()<1.15f,"hunter skirmisher improves mobility");
        g=arena(ASSASSIN);g.careers[g.buildId()][1]=1;ActionGame.Enemy low=new ActionGame.Enemy(g.nextId++,0,600,600,100);low.hp=40;g.enemies.add(low);float hpBefore=low.hp;g.hitForTest(low,20);check(hpBefore-low.hp>20,"assassin executioner deals bonus damage to wounded targets");
        g=arena(DETECTIVE);g.careers[g.buildId()][1]=1;ActionGame.Enemy marked=new ActionGame.Enemy(g.nextId++,0,600,600,100);marked.hex=2;g.enemies.add(marked);g.hitForTest(marked,20);check(100-marked.hp>20,"detective interrogator exploits marked targets");
        g=arena(DETECTIVE);g.careers[g.buildId()][1]=2;marked=new ActionGame.Enemy(g.nextId++,0,600,600,100);marked.stun=1;g.enemies.add(marked);g.hitForTest(marked,20);check(100-marked.hp>20,"detective analyst exploits stunned targets");
        g=arena(ASSASSIN);g.careers[g.buildId()][1]=2;check(g.careerKnifeCount()==5,"assassin phantom specialization adds two knives to each volley");
        g.soulCd=5;g.generateRoom();check(g.skillCd==0&&g.soulCd==0,"room transition resets both skill cooldowns");
        System.out.println("CAREER ABILITY CHECKS PASSED: "+checks);
    }
}
