import io.wayfarer.rpg.core.*;
import static io.wayfarer.rpg.core.CareerSystem.*;

/** Regression checks for the four redesigned career abilities. */
public final class CareerAbilityTests {
    private static int checks;
    private static void check(boolean ok,String why){checks++;if(!ok)throw new AssertionError(why);}
    private static ActionGame arena(int career){
        ActionGame g=new ActionGame();g.chooseCareer(career,0,0);g.start(91);g.experience[g.hero]=160;g.blocks.clear();g.enemies.clear();g.x=560;g.y=600;g.invulnerable=0;return g;
    }
    private static void careerTreeEffects(){
        ActionGame g=arena(HUNTER);g.experience[g.hero]=800;g.careerTalents[g.buildId()][g.job()*3+0]=2;ActionGame.Enemy e=new ActionGame.Enemy(g.nextId++,0,700,600,1000);g.enemies.add(e);g.skill();check(1000-e.hp>g.damage()*2.2f,"hunter first node increases burst damage");
        g=arena(HUNTER);g.experience[g.hero]=800;float period=g.skillPeriod();g.careerTalents[g.buildId()][g.job()*3+1]=2;check(g.skillPeriod()<period,"hunter second node shortens career cooldown");
        g=arena(HUNTER);g.careerTalents[g.buildId()][g.job()*3+2]=2;g.skill();check(g.shield==100,"hunter final node strengthens cast shield");
        g=arena(ASSASSIN);g.careerTalents[g.buildId()][g.job()*3+0]=2;g.skill();check(g.careerSkillTime==38,"assassin first node extends overdrive duration");
        g=arena(ASSASSIN);g.careerTalents[g.buildId()][g.job()*3+1]=2;g.skill();g.shield=0;e=new ActionGame.Enemy(g.nextId++,0,620,600,1000);g.enemies.add(e);g.hitForTest(e,100);check(1000-e.hp>100,"assassin second node increases overdrive hit damage");
        g=arena(ASSASSIN);g.careerTalents[g.buildId()][g.job()*3+2]=2;g.skill();g.shield=0;e=new ActionGame.Enemy(g.nextId++,0,620,600,1000);g.enemies.add(e);g.hitForTest(e,20);check(g.shield==19,"assassin final node adds its talent ward to the base overdrive shield on each successful hit");
        g=arena(DETECTIVE);g.careerTalents[g.buildId()][g.job()*3+0]=2;e=new ActionGame.Enemy(g.nextId++,0,1000,600,1000);g.enemies.add(e);g.skill();check(e.x-g.x<100,"detective first node increases forced pull distance");
        g=arena(DETECTIVE);g.careerTalents[g.buildId()][g.job()*3+1]=2;e=new ActionGame.Enemy(g.nextId++,0,700,600,1000);g.enemies.add(e);g.skill();check(1000-e.hp>g.damage()*2.4f,"detective second node increases pulse damage");
        g=arena(DETECTIVE);g.careerTalents[g.buildId()][g.job()*3+2]=2;g.skill();check(g.shield==100,"detective final node strengthens cast shield");
        g=arena(REPORTER);g.careerTalents[g.buildId()][g.job()*3+0]=2;e=new ActionGame.Enemy(g.nextId++,0,700,600,1000);g.enemies.add(e);g.skill();check(Math.abs((1000-e.hp)-g.damage()*1.5f)<.05f,"reporter first node increases conduction damage");
        g=arena(REPORTER);g.careerTalents[g.buildId()][g.job()*3+1]=2;g.hp=g.maxHp()-20;float hp=g.hp;g.skill();check(g.hp-hp>=g.maxHp()*.08f-.05f,"reporter second node improves skill recovery");
        g=arena(REPORTER);g.careerTalents[g.buildId()][g.job()*3+2]=2;e=new ActionGame.Enemy(g.nextId++,0,700,600,1000);g.enemies.add(e);g.skill();check(g.shield==80,"reporter final node adds twenty ward per hit to the base cast ward");
        System.out.println("PASS twelve career skill-tree node effects");
    }
    private static void careerTreeBudgetAndReset(){
        ActionGame g=arena(HUNTER);g.experience[g.hero]=1200;g.pause();g.abandon();g.returnCamp();
        check(!g.learnCareerTalent(2),"career final node is locked before its prerequisite");
        check(g.learnCareerTalent(0)&&g.learnCareerTalent(0)&&g.learnCareerTalent(1)&&g.learnCareerTalent(1)&&g.learnCareerTalent(2),"career skill points follow rank and prerequisite rules");
        check(g.availableCareerPoints()==2,"career skill points share a capped career budget");
        g.resetTalents();check(g.careerTalentRank(0)==0&&g.careerTalentRank(1)==0&&g.careerTalentRank(2)==0&&g.availableCareerPoints()==7,"full respec refunds career points");
        System.out.println("PASS career talent point budget, prerequisites and full respec");
    }
    public static void main(String[] args)throws Exception{
        careerTreeEffects();careerTreeBudgetAndReset();
        ActionGame g=arena(HUNTER);ActionGame.Enemy e=new ActionGame.Enemy(g.nextId++,0,700,600,1000);g.enemies.add(e);g.skill();
        check(g.careerSkillMode==1&&g.careerSkillTime==30&&e.hp<1000,"hunter burst and sustained state");
        g=arena(ASSASSIN);float basePeriod=g.attackPeriod();g.skill();
        check(g.careerSkillMode==2&&g.careerSkillTime==30&&g.attackPeriod()>basePeriod,"assassin activates knife state with a slight attack interval increase");
        g.activeSkillCd=8;g.skillCd=11;ActionGame.Enemy victim=new ActionGame.Enemy(g.nextId++,0,600,600,1);g.enemies.add(victim);g.hitForTest(victim,20);
        check(g.activeSkillCd==0&&g.skillCd==11,"assassin kills refresh generic active skill, not career skill");
        g=arena(DETECTIVE);e=new ActionGame.Enemy(g.nextId++,0,720,600,1000);g.enemies.add(e);float beforeDistance=(float)Math.hypot(e.x-g.x,e.y-g.y);g.skill();
        check(g.careerSkillMode==0&&g.careerSkillTime==0&&e.hp<1000&&Math.hypot(e.x-g.x,e.y-g.y)<beforeDistance,"detective instantly attracts and damages without a persistent field");
        ActionGame.Enemy archer=new ActionGame.Enemy(g.nextId++,1,800,380,1000);g.enemies.add(archer);archer.state=1;archer.timer=.01f;
        for(int i=0;i<40;i++){g.hitStop=0;g.update(1f/60f,new ActionGame.Input());}
        check(archer.state==2||g.shots.size()>0,"detective one-shot attraction allows enemy AI to resume after its brief stun");
        g=arena(REPORTER);ActionGame.Enemy reporter=new ActionGame.Enemy(g.nextId++,0,700,600,1000);g.enemies.add(reporter);float expectedReporterDamage=g.damage()*1.3f;g.skill();
        check(Math.abs((1000-reporter.hp)-expectedReporterDamage)<.05f&&g.careerSkillMode==0&&g.careerSkillTime==0,"reporter single-target burst deals thirty percent bonus with no healing penalty");
        g.careerCooldownRank=4;ActionGame restored=ActionSave.decode(ActionSave.encode(g));
        check(restored.careerCooldownRank==4&&restored.careerSkillMode==0,"v26 saves career cooldown without obsolete reporter state");
        g=arena(HUNTER);g.careers[g.buildId()][1]=2;check(g.speed()>205&&g.dodgePeriod()<1.15f,"hunter skirmisher improves mobility");
        g=arena(ASSASSIN);g.careers[g.buildId()][1]=1;ActionGame.Enemy low=new ActionGame.Enemy(g.nextId++,0,600,600,100);low.hp=40;g.enemies.add(low);float hpBefore=low.hp;g.hitForTest(low,20);check(hpBefore-low.hp>20,"assassin executioner deals bonus damage to wounded targets");
        g=arena(DETECTIVE);g.careers[g.buildId()][1]=1;ActionGame.Enemy marked=new ActionGame.Enemy(g.nextId++,0,600,600,100);marked.hex=2;g.enemies.add(marked);g.hitForTest(marked,20);check(100-marked.hp>20,"detective interrogator exploits marked targets");
        g=arena(DETECTIVE);g.careers[g.buildId()][1]=2;marked=new ActionGame.Enemy(g.nextId++,0,600,600,100);marked.stun=1;g.enemies.add(marked);g.hitForTest(marked,20);check(100-marked.hp>20,"detective analyst exploits stunned targets");
        g=arena(ASSASSIN);g.careers[g.buildId()][1]=2;check(g.careerKnifeCount()==5,"assassin phantom specialization adds two knives to each volley");
        g.soulCd=5;g.generateRoom();check(g.skillCd==0&&g.soulCd==0,"room transition resets both skill cooldowns");
        g=arena(HUNTER);ActionGame.Enemy distant=new ActionGame.Enemy(g.nextId++,0,g.x+300,g.y,1000);g.enemies.add(distant);g.hitForTest(distant,100);check(1000-distant.hp>112,"hunter gains a reliable long-range tracking damage bonus");
        g=arena(ASSASSIN);ActionGame.Enemy close=new ActionGame.Enemy(g.nextId++,0,g.x+90,g.y,1000);g.enemies.add(close);float shieldBefore=g.shield;g.hitForTest(close,40);check(g.shield>shieldBefore,"assassin gains guard from close-quarters hits");
        g=arena(DETECTIVE);ActionGame.Enemy suspect=new ActionGame.Enemy(g.nextId++,0,g.x+90,g.y,1000);g.enemies.add(suspect);g.hitForTest(suspect,20);check(suspect.hex>=2.5f,"detective marks evidence targets on hit");
        g=arena(REPORTER);ActionGame.Enemy source=new ActionGame.Enemy(g.nextId++,0,g.x+90,g.y,1000),witness=new ActionGame.Enemy(g.nextId++,0,g.x+180,g.y,1000);g.enemies.add(source);g.enemies.add(witness);float witnessHp=witness.hp;g.hitForTest(source,100);check(witness.hp<witnessHp&&Math.abs((witnessHp-witness.hp)-15)<.1f,"reporter relays fifteen percent of hit damage to a nearby second target");
        System.out.println("CAREER ABILITY CHECKS PASSED: "+checks);
    }
}
