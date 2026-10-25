package io.wayfarer.rpg;

import android.app.AlertDialog;
import android.widget.*;
import io.wayfarer.rpg.core.*;

/** Native, scrollable story scenes with once-only decisions performed by the core. */
public final class StoryJournal {
    private StoryJournal(){}
    private static ScrollView body(MainActivity a,String text){ScrollView s=new ScrollView(a);TextView t=new TextView(a);t.setTag("story-text");t.setText(text);t.setTextSize(16);int p=Math.round(20*a.getResources().getDisplayMetrics().density);t.setPadding(p,p,p,p);s.addView(t);return s;}
    public static void show(MainActivity a){
        ActionGame g=a.world.game;String[] items=new String[9];items[0]="序章 · 迟到的夜班";
        for(int i=0;i<5;i++)items[i+1]=((g.storyUnlocked&(1<<i))==0?"未发现 · ":g.storyChoices[i]==0?"待决定 · ":"已记录 · ")+StorySystem.TITLES[i];
        items[6]=g.heroName()+" · 人物故事";items[7]="灰桥的结局";items[8]="敌情与技能衔接";
        a.storyDialog=new AlertDialog.Builder(a).setTitle("灰桥手记 · 主线与人物").setItems(items,(d,w)->{
            if(w==0)read(a,"序章 · 迟到的夜班",StorySystem.INTRO);
            else if(w<=5)scene(a,w-1);
            else if(w==6)characterMenu(a);
            else if(w==7)read(a,"灰桥的结局",StorySystem.ending(g.storyChoices));
            else {StringBuilder b=new StringBuilder("主动技能发动后，2秒内再次点击同一技能键可使用一次追击；错过窗口不会退还冷却。\n\n");for(int i=0;i<3;i++)b.append(EncounterSystem.FOLLOWUP_DETAILS[i]).append("\n\n");b.append("光辉追击命中补8辉盾，并净化自身110范围弹丸；禁忌追击消耗2%最大生命、增加12侵蚀，引爆目标的既有诅咒，对附近其他敌人造成次生伤害。\n\n");for(int i=0;i<7;i++)b.append(EncounterSystem.NAMES[i]).append("：").append(EncounterSystem.COUNTERS[i]).append("\n\n");read(a,"敌情与技能衔接",b.toString());}
        }).setNegativeButton("返回",null).show();
    }
    private static void read(MainActivity a,String title,String text){a.storyDialog=new AlertDialog.Builder(a).setTitle(title).setView(body(a,text)).setPositiveButton("返回手记",(d,w)->show(a)).setNegativeButton("关闭",null).show();}
    public static void scene(MainActivity a,int scene){
        ActionGame g=a.world.game;if((g.storyUnlocked&(1<<scene))==0){read(a,StorySystem.TITLES[scene],"清理对应区域后，可在现场调查这条线索。未发现的内容不会提前展示。");return;}
        int choice=g.storyChoices[scene];String text=StorySystem.SCENES[scene]+"\n\n"+(choice==0?"选择会永久记录，影响结局。救援路线获得12营地票券；取证路线让当前角色获得20经验。可稍后在营地决定。":"已记录："+StorySystem.OPTIONS[scene][choice-1]+"。本次阅读不会再次发放奖励。");
        AlertDialog.Builder b=new AlertDialog.Builder(a).setTitle(StorySystem.TITLES[scene]).setView(body(a,text));
        if(choice==0)b.setPositiveButton(StorySystem.OPTIONS[scene][0],(d,w)->{if(g.decideStory(scene,1)){a.save();a.world.invalidate();}}).setNegativeButton(StorySystem.OPTIONS[scene][1],(d,w)->{if(g.decideStory(scene,2)){a.save();a.world.invalidate();}}).setNeutralButton("稍后决定",null);
        else b.setPositiveButton("返回手记",(d,w)->show(a)).setNegativeButton("关闭",null);
        a.storyDialog=b.show();
    }
    private static void characterMenu(MainActivity a){ActionGame g=a.world.game;int id=g.buildId();String[] labels=new String[3];for(int i=0;i<3;i++)labels[i]=((g.characterUnlocked[id]&(1<<i))==0?"未解锁 · ":g.characterChoices[id][i]==0?"待决定 · ":"已记录 · ")+new String[]{"旧事：首次清理区域","裂痕：清理旧纺织厂","回声：击败泵房监工"}[i];a.storyDialog=new AlertDialog.Builder(a).setTitle(g.heroName()+" · 人物故事").setItems(labels,(d,w)->character(a,w)).setNegativeButton("返回手记",(d,w)->show(a)).show();}
    public static void character(MainActivity a,int chapter){ActionGame g=a.world.game;int id=g.buildId();
        if((g.characterUnlocked[id]&(1<<chapter))==0){read(a,"尚未经历的故事","由当前角色完成对应区域后解锁：首次清理区域、清理旧纺织厂、击败泵房监工。每个角色独立记录。");return;}
        int job=g.characterJobs[id],choice=g.characterChoices[id][chapter];String text=g.heroName()+" · 经历起点："+CareerSystem.CLASSES[job]+"\n\n"+StorySystem.PEOPLE[job][chapter]+"\n\n"+(choice==0?"这次决定会留下人物历练：守护使生命上限+4，决意使基础伤害+1。每章只能选择一次；两种选择都不会自动开启禁术。":"你的决定："+StorySystem.PERSONAL_OPTIONS[job][choice-1]+"。已获得"+(choice==1?"守护 +4生命上限":"决意 +1基础伤害")+"。")+"\n\n人物线记住最初经历时的职业，之后换职业不会改写往事。";
        AlertDialog.Builder b=new AlertDialog.Builder(a).setTitle(StorySystem.personalTitle(job,chapter)).setView(body(a,text));
        if(choice==0&&g.mode==ActionGame.Mode.CAMP)b.setPositiveButton(StorySystem.PERSONAL_OPTIONS[job][0],(d,w)->{if(g.decideCharacter(chapter,1)){a.save();a.world.invalidate();}}).setNegativeButton(StorySystem.PERSONAL_OPTIONS[job][1],(d,w)->{if(g.decideCharacter(chapter,2)){a.save();a.world.invalidate();}}).setNeutralButton("稍后决定",null);
        else b.setPositiveButton(choice==0?"回营后决定":"返回人物故事",(d,w)->characterMenu(a)).setNegativeButton("关闭",null);
        a.storyDialog=b.show();
    }
}
