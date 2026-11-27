import io.wayfarer.rpg.core.*;
import java.io.*;
import java.util.*;
import static io.wayfarer.rpg.core.CareerSystem.*;

public final class CareerTests {
    private static int checks;
    private static void check(boolean ok,String why){checks++;if(!ok)throw new AssertionError(why);}
    private static void near(float a,float b,String why){check(Math.abs(a-b)<.05f,why+": "+a+" vs "+b);}
    private static byte[] edit(byte[] data,String key,String value)throws Exception{Properties p=new Properties();p.load(new ByteArrayInputStream(data));p.setProperty(key,value);ByteArrayOutputStream out=new ByteArrayOutputStream();p.store(out,"");return out.toByteArray();}
    public static void main(String[] args)throws Exception{
        check(CLASSES.length==5&&CLASSES[4].contains("时针")&&CLASSES[4].contains("限定"),"Timekeeper is the fifth limited career");
        ActionGame g=new ActionGame();check(g.job()==HUNTER,"new builds default to hunter");
        for(int job=HUNTER;job<=TIMEKEEPER;job++){check(g.chooseCareer(job,0,0),"career "+job+" selectable");check(g.job()==job,"career ID committed");}
        check(!g.chooseCareer(5,0,0),"unknown career IDs rejected");check(!g.chooseCareer(REPORTER,2,0),"specialization level gate");
        g.experience[0]=160;check(g.chooseCareer(REPORTER,2,4),"reporter specialization available at level three");
        g.bindSoul(2,1);g.start(3);ActionGame a=ActionSave.decode(ActionSave.encode(g));
        check(a.job()==REPORTER&&a.specialization()==2&&a.background()==4&&a.soulKind()==2,"new career, specialization, background and soul persist");
        g.careerSkillMode=5;g.careerSkillTime=5;g.skillCd=2;byte[] oldData=ActionSave.encode(g);oldData=edit(oldData,"version","10");oldData=edit(oldData,"career0.0","13");oldData=edit(oldData,"career0.1","2");oldData=edit(oldData,"career0.2","4");oldData=edit(oldData,"characterJob0","13");oldData=edit(oldData,"characterUnlocked0","1");
        ActionGame migrated=ActionSave.decode(oldData);check(migrated.job()==HUNTER&&migrated.specialization()==0&&migrated.background()==4,"v1-v10 migration resets retired career but keeps background");check(migrated.characterJobs[0]==HUNTER,"historical character career IDs are reset too");check(migrated.careerSkillMode==0&&migrated.careerSkillTime==0&&migrated.skillCd==0,"retired career runtime is cleared during migration");

        g=new ActionGame();g.chooseCareer(TIMEKEEPER,0,0);g.bindSoul(1,0);g.start(8);g.blocks.clear();g.enemies.clear();ActionGame.Enemy clockTarget=new ActionGame.Enemy(g.nextId++,0,740,575,5000);g.enemies.add(clockTarget);g.skill();check(g.careerSkillMode==5&&g.careerSkillTime==5&&g.soulFields.size()==1&&g.soulFields.get(0).type==3,"Timekeeper career skill creates its separate five-second gravity field");ActionGame clockSave=ActionSave.decode(ActionSave.encode(g));check(clockSave.job()==TIMEKEEPER&&clockSave.soulFields.size()==1,"Timekeeper gravity field and limited career survive save restore");
        g=new ActionGame();g.experience[0]=160;g.chooseCareer(HUNTER,1,0);g.start(4);g.blocks.clear();g.enemies.clear();
        ActionGame.Enemy e=new ActionGame.Enemy(g.nextId++,0,700,600,1000);g.enemies.add(e);g.skill();check(g.careerSkillMode==1&&g.careerSkillTime==6&&e.hp<1000&&e.hex>=8,"hunter tracking mark and hunt window start");
        g=new ActionGame();g.chooseCareer(ASSASSIN,0,0);g.start(5);float basePeriod=g.attackPeriod();g.skill();check(g.careerSkillTime==6&&g.attackPeriod()<basePeriod,"assassin enters a brief fast shadow-strike window");ActionGame assassinSave=ActionSave.decode(ActionSave.encode(g));check(assassinSave.careerSkillMode==2&&assassinSave.careerSkillTime==6,"assassin state survives save and restore");
        g=new ActionGame();g.chooseCareer(DETECTIVE,0,0);g.start(6);g.blocks.clear();g.enemies.clear();ActionGame.Enemy detectiveTarget=new ActionGame.Enemy(g.nextId++,0,850,600,1000);g.enemies.add(detectiveTarget);float detectiveDistance=(float)Math.hypot(detectiveTarget.x-g.x,detectiveTarget.y-g.y);g.skill();check(g.careerSkillMode==3&&g.careerSkillTime==6&&detectiveTarget.hex>=4.5f&&detectiveTarget.stun>0&&Math.abs(Math.hypot(detectiveTarget.x-g.x,detectiveTarget.y-g.y)-detectiveDistance)<.01,"detective reconstructs clues without forced pulling");
        g=new ActionGame();g.chooseCareer(REPORTER,0,0);g.start(7);g.blocks.clear();g.enemies.clear();ActionGame.Enemy reporterTarget=new ActionGame.Enemy(g.nextId++,0,700,600,1000);g.enemies.add(reporterTarget);float reporterDamage=g.damage()*2.7f;g.skill();near(1000-reporterTarget.hp,reporterDamage,"reporter headline applies focused damage");check(g.careerSkillMode==4&&g.careerSkillTime==6,"reporter skill opens a short propagation window");
        System.out.println("CAREER CHECKS PASSED: "+checks);
    }
}