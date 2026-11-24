import io.wayfarer.rpg.core.*;
import static io.wayfarer.rpg.core.CareerSystem.*;

public final class CareerAbilityTests {
    private static int checks;
    private static void check(boolean ok,String why){checks++;if(!ok)throw new AssertionError(why);}
    private static ActionGame arena(int career,int soul){
        ActionGame g=new ActionGame();g.chooseCareer(career,0,0);if(soul>0)g.bindSoul(soul,0);g.start(91);
        g.experience[g.hero]=160;g.blocks.clear();g.enemies.clear();g.shots.clear();g.x=560;g.y=600;g.invulnerable=0;return g;
    }
    private static ActionGame.Enemy mob(ActionGame g,float x,float y,float hp){ActionGame.Enemy e=new ActionGame.Enemy(g.nextId++,0,x,y,hp);g.enemies.add(e);return e;}
    private static boolean signal(ActionGame g,String kind){for(ActionGame.Signal s:g.signals)if(s.kind.equals(kind))return true;return false;}
    private static void activeSkills(){
        ActionGame g=arena(HUNTER,0);ActionGame.Enemy target=mob(g,700,600,1000);float speed=g.speed();g.skill();
        check(target.hex>=8&&target.hp<1000&&g.speed()>speed,"hunter marks and hunts a high-threat target while entering a short hunt window");
        g=arena(ASSASSIN,0);float x=g.x;g.skill();
        check(g.invulnerable>=.8f&&Math.hypot(g.x-x,g.y-600)>200&&g.shield>0,"assassin performs a protected forward shadow strike and gains ward");
        g=arena(DETECTIVE,0);target=mob(g,720,600,1000);g.skill();
        check(target.hex>=4.5f&&target.stun>=.29f&&g.careerSkillMode==3,"detective reconstructs the scene, records clues and interrupts suspects");
        g=arena(REPORTER,0);target=mob(g,700,600,1000);ActionGame.Enemy witness=mob(g,800,600,1000),farWitness=mob(g,100,100,1000);float before=witness.hp,farBefore=farWitness.hp;g.skill();
        check(target.hp<1000&&witness.hp<before&&farWitness.hp<farBefore&&g.careerSkillMode==4,"reporter headline conducts damage to every enemy on the field");
        g=arena(REPORTER,0);target=mob(g,700,600,1000);float singleBefore=target.hp,singleBase=g.damage();g.skill();check(singleBefore-target.hp>=singleBase*2.69f,"reporter headline keeps its 1.8 base multiplier plus the global fifty-percent solo bonus");
        g=arena(REPORTER,0);target=mob(g,700,600,1000);float soloBase=g.damage();g.hitForTest(target,100);check(Math.abs((1000-target.hp)-150)<.05f,"reporter solo bonus applies to ordinary damage, not only the headline skill");
        g=arena(HUNTER,0);ActionGame.Enemy ordinary=mob(g,610,700,5000),boss=mob(g,900,600,100);g.skill();check(boss.hex>=8&&boss.hp<100&&ordinary.hp==5000,"hunter career skill prioritizes the boss even when an ordinary enemy has more health");
        g=arena(HUNTER,0);ordinary=mob(g,610,700,5000);boss=mob(g,900,600,100);g.soulEnergy=100;g.soulSkill();check(boss.hp<100&&ordinary.hp==5000,"hunter soul tracking prioritizes the boss");
        g=new ActionGame();g.selectHero(1);g.chooseCareer(HUNTER,0,0);g.start(912);g.blocks.clear();g.enemies.clear();g.x=560;g.y=600;g.skill();ordinary=mob(g,610,700,5000);boss=mob(g,900,600,100);g.blocks.add(new ActionGame.Block(700,570,50,60,0));ActionGame.Input fire=new ActionGame.Input();fire.attack=true;for(int i=0;i<10;i++)g.update(.05f,fire);check(boss.hp<100&&ordinary.hp==5000,"hunter arrows pierce obstacles during the career hunt window and remain boss-locked");
        System.out.println("PASS four distinct career active loops");
    }
    private static void soulForms(){
        for(int career=0;career<4;career++){
            ActionGame radiant=arena(career,1);ActionGame.Enemy e=mob(radiant,700,600,1000);radiant.soulEnergy=100;radiant.soulSkill();
            check(radiant.soulEnergy>=60&&radiant.soulEnergy<=100&&radiant.soulCd>0&&signal(radiant,"radiant"),"radiant soul form is unique and spends soul energy for career "+career);
            check(e.hp<1000||radiant.soulWard>0||e.hex>0,"radiant soul form resolves a real combat or defense effect for career "+career);
            ActionGame forbidden=arena(career,2);e=mob(forbidden,700,600,1000);forbidden.soulEnergy=100;float hp=forbidden.hp;forbidden.soulSkill();
            check(forbidden.soulEnergy==60&&forbidden.soulCd>0&&signal(forbidden,"forbidden"),"forbidden soul form is unique and spends soul energy for career "+career);
            check(forbidden.hp<hp&&forbidden.corruption>0,"forbidden soul form exacts a life and erosion cost for career "+career);
        }
        System.out.println("PASS eight career-bound radiant and forbidden soul forms");
    }
    private static void careerTreeEffects(){
        ActionGame g=arena(HUNTER,0);ActionGame.Enemy e=mob(g,700,600,1000);g.careerTalents[g.buildId()][g.job()*3]=2;g.skill();check(e.hp<1000,"hunter tracking calibration affects marked target damage");
        g=arena(HUNTER,0);float cd=g.skillPeriod();g.careerTalents[g.buildId()][g.job()*3+1]=2;check(g.skillPeriod()<cd,"hunter cooldown node works");
        g=arena(HUNTER,0);g.careerTalents[g.buildId()][g.job()*3+2]=2;g.skill();check(g.shield>=16,"hunter shield node works");
        g=arena(ASSASSIN,0);g.careerTalents[g.buildId()][g.job()*3]=2;g.skill();check(Math.hypot(g.x-560,g.y-600)>220,"assassin mobility node extends shadow strike");
        g=arena(ASSASSIN,0);g.careerTalents[g.buildId()][g.job()*3+1]=2;e=mob(g,700,600,1000);g.skill();float hp=e.hp;g.hitForTest(e,20);check(e.hp<hp-20,"assassin damage node works during strike window");
        g=arena(ASSASSIN,0);g.careerTalents[g.buildId()][g.job()*3+2]=2;e=mob(g,620,600,1000);g.skill();float ward=g.shield;g.hitForTest(e,20);check(g.shield>ward,"assassin shield node works on hit");
        g=arena(DETECTIVE,0);g.careerTalents[g.buildId()][g.job()*3]=2;e=mob(g,700,600,1000);g.skill();check(e.hex>=5.5f,"detective evidence node lengthens clue exposure");
        g=arena(DETECTIVE,0);g.careerTalents[g.buildId()][g.job()*3+1]=2;e=mob(g,700,600,1000);g.skill();float before=e.hp;g.hitForTest(e,20);check(before-e.hp>20,"detective analysis node increases damage to investigated targets");
        g=arena(DETECTIVE,0);g.careerTalents[g.buildId()][g.job()*3+2]=2;g.skill();check(g.shield>=20,"detective protection node adds ward");
        g=arena(REPORTER,0);g.careerTalents[g.buildId()][g.job()*3]=2;e=mob(g,700,600,1000);ActionGame.Enemy witness=mob(g,800,600,1000);float wb=witness.hp;g.hitForTest(e,100);check(wb-witness.hp>=59.9f,"reporter distribution node adds five percent per rank to the fifty-percent relay ratio");
        g=arena(REPORTER,0);g.careerTalents[g.buildId()][g.job()*3+1]=2;g.hp=g.maxHp()-20;float old=g.hp;g.skill();check(g.hp>old,"reporter recovery node works");
        g=arena(REPORTER,0);g.careerTalents[g.buildId()][g.job()*3+2]=2;e=mob(g,700,600,1000);g.skill();check(g.shield>=8,"reporter ward node works");
        System.out.println("PASS twelve career talent effects");
    }
    private static void careerTreeBudgetAndReset(){
        ActionGame g=arena(HUNTER,0);g.experience[g.hero]=1200;g.pause();g.abandon();g.returnCamp();
        check(!g.learnCareerTalent(2),"career final node is locked before its prerequisite");
        check(g.learnCareerTalent(0)&&g.learnCareerTalent(0)&&g.learnCareerTalent(1)&&g.learnCareerTalent(1)&&g.learnCareerTalent(2),"career skill points follow rank and prerequisite rules");
        check(g.availableCareerPoints()==2,"career skill points share a capped career budget");
        g.resetTalents();check(g.careerTalentRank(0)==0&&g.careerTalentRank(1)==0&&g.careerTalentRank(2)==0&&g.availableCareerPoints()==7,"full respec refunds career points");
        System.out.println("PASS career talent point budget, prerequisites and full respec");
    }
    public static void main(String[] args)throws Exception{
        activeSkills();soulForms();careerTreeEffects();careerTreeBudgetAndReset();
        ActionGame g=arena(HUNTER,0);ActionGame.Enemy distant=mob(g,g.x+300,g.y,1000);distant.hex=5;g.hitForTest(distant,100);check(distant.hp<900,"hunter's marked target passive is tested through a mark");
        g=arena(ASSASSIN,0);ActionGame.Enemy close=mob(g,g.x+90,g.y,1000);float shield=g.shield;g.hitForTest(close,40);check(g.shield>shield,"assassin gains guard from close-quarters hits");
        g=arena(DETECTIVE,0);ActionGame.Enemy suspect=mob(g,g.x+90,g.y,1000);g.hitForTest(suspect,20);check(suspect.hex>=3.5f,"detective marks evidence targets on hit");
        g=arena(REPORTER,0);ActionGame.Enemy source=mob(g,g.x+90,g.y,1000),witness=mob(g,g.x+180,g.y,1000),farRelay=mob(g,100,100,1000);float witnessHp=witness.hp,farRelayHp=farRelay.hp;g.hitForTest(source,100);check(witness.hp<witnessHp&&witnessHp-witness.hp>=49.9f&&farRelay.hp<farRelayHp,"reporter relays fifty percent damage to every living enemy regardless of distance");
        g.soulCd=5;g.generateRoom();check(g.skillCd==0&&g.soulCd==0,"room transition resets both skill cooldowns");
        ActionGame restored=ActionSave.decode(ActionSave.encode(g));check(restored.job()==REPORTER,"career identity survives save restore");
        System.out.println("CAREER ABILITY CHECKS PASSED: "+checks);
    }
}
