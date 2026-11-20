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
        check(CLASSES.length==4&&CLASSES[0].equals("猎人")&&CLASSES[1].equals("刺客")&&CLASSES[2].equals("侦探")&&CLASSES[3].equals("记者"),"only four rebuilt careers exist");
        ActionGame g=new ActionGame();check(g.job()==HUNTER,"new builds default to hunter");
        for(int job=HUNTER;job<=REPORTER;job++){check(g.chooseCareer(job,0,0),"career "+job+" selectable");check(g.job()==job,"career ID committed");}
        check(!g.chooseCareer(4,0,0),"retired IDs rejected");check(!g.chooseCareer(REPORTER,2,0),"specialization level gate");
        g.experience[0]=160;check(g.chooseCareer(REPORTER,2,4),"reporter specialization available at level three");
        g.bindSoul(2,1);g.start(3);ActionGame a=ActionSave.decode(ActionSave.encode(g));
        check(a.job()==REPORTER&&a.specialization()==2&&a.background()==4&&a.soulKind()==2,"new career, specialization, background and soul persist");
        g.careerSkillMode=4;g.careerSkillTime=4;g.skillCd=2;byte[] oldData=ActionSave.encode(g);oldData=edit(oldData,"version","10");oldData=edit(oldData,"career0.0","13");oldData=edit(oldData,"career0.1","2");oldData=edit(oldData,"career0.2","4");oldData=edit(oldData,"characterJob0","13");oldData=edit(oldData,"characterUnlocked0","1");
        ActionGame migrated=ActionSave.decode(oldData);check(migrated.job()==HUNTER&&migrated.specialization()==0&&migrated.background()==4,"v1-v10 migration resets retired career but keeps background");check(migrated.characterJobs[0]==HUNTER,"historical character career IDs are reset too");check(migrated.careerSkillMode==0&&migrated.careerSkillTime==0&&migrated.skillCd==0,"retired career runtime is cleared during migration");

        g=new ActionGame();g.experience[0]=160;g.chooseCareer(HUNTER,1,0);g.start(4);g.blocks.clear();g.enemies.clear();
        ActionGame.Enemy e=new ActionGame.Enemy(g.nextId++,0,700,600,1000);g.enemies.add(e);g.skill();check(g.careerSkillMode==1&&g.careerSkillTime==10&&e.hp<1000,"hunter burst and ten-second state start");
        g=new ActionGame();g.chooseCareer(ASSASSIN,0,0);g.start(5);float basePeriod=g.attackPeriod();g.skill();check(g.careerSkillTime==10&&g.attackPeriod()>basePeriod,"assassin skill slightly slows attack interval");ActionGame assassinSave=ActionSave.decode(ActionSave.encode(g));check(assassinSave.careerSkillMode==2&&assassinSave.careerSkillTime==10,"assassin state survives save and restore");
        g=new ActionGame();g.chooseCareer(DETECTIVE,0,0);g.start(6);g.blocks.clear();g.enemies.clear();g.enemies.add(new ActionGame.Enemy(g.nextId++,0,700,600,1000));g.skill();check(g.careerSkillMode==3&&g.careerSkillTime==10,"detective global attraction field starts");
        g=new ActionGame();g.chooseCareer(REPORTER,0,0);g.start(7);g.blocks.clear();g.enemies.clear();g.enemies.add(new ActionGame.Enemy(g.nextId++,0,700,600,1000));g.skill();check(g.careerSkillMode==4&&g.careerSkillTime==10&&Math.abs(g.healingFactor()-.2f)<.01f,"reporter speed state and healing penalty start");
        System.out.println("CAREER CHECKS PASSED: "+checks);
    }
}