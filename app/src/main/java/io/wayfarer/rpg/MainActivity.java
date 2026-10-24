package io.wayfarer.rpg;

import android.app.*;
import android.os.Bundle;
import android.view.*;
import android.widget.*;
import io.wayfarer.rpg.core.*;
import java.io.IOException;

public final class MainActivity extends Activity implements WorldView.Host {
    public WorldView world;
    private SaveStore store;
    private GameAudio audio;
    private boolean ready=false,saveFailed=false;
    public AlertDialog workshopDialog,talentDialog,soulDialog,relicDialog;
    @Override public void onCreate(Bundle state){
        super.onCreate(state);getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY|View.SYSTEM_UI_FLAG_FULLSCREEN|View.SYSTEM_UI_FLAG_HIDE_NAVIGATION|View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
        store=new SaveStore(this);audio=new GameAudio(this);ActionGame game;String failure=null;
        try{game=store.exists(0)?store.read(0):new ActionGame();ready=true;}
        catch(IOException e){game=new ActionGame();failure=e.getMessage();}
        if(game.mode==ActionGame.Mode.PLAY)game.pause();
        world=new WorldView(this,game,this);FrameLayout root=new FrameLayout(this);root.addView(world,new FrameLayout.LayoutParams(-1,-1));
        root.setOnApplyWindowInsetsListener((v,insets)->{int left=insets.getSystemWindowInsetLeft(),right=insets.getSystemWindowInsetRight(),top=insets.getSystemWindowInsetTop(),bottom=insets.getSystemWindowInsetBottom();
            if(android.os.Build.VERSION.SDK_INT>=28&&insets.getDisplayCutout()!=null){left=Math.max(left,insets.getDisplayCutout().getSafeInsetLeft());right=Math.max(right,insets.getDisplayCutout().getSafeInsetRight());top=Math.max(top,insets.getDisplayCutout().getSafeInsetTop());bottom=Math.max(bottom,insets.getDisplayCutout().getSafeInsetBottom());}
            root.setPadding(left,top,right,bottom);return insets;});setContentView(root);root.requestApplyInsets();
        if(failure!=null){world.setEnabled(false);new AlertDialog.Builder(this).setTitle("自动存档无法读取").setMessage("原存档未覆盖。可从手动槽恢复，或确认建立新档。\n"+failure)
            .setPositiveButton("手动存档",(d,w)->slots()).setNegativeButton("建立新档",(d,w)->newProfile()).setCancelable(false).show();}
    }
    @Override public void save(){if(!ready||world==null)return;try{store.write(0,world.game);saveFailed=false;}catch(IOException e){if(!saveFailed){Toast.makeText(this,"保存失败："+e.getMessage(),Toast.LENGTH_LONG).show();saveFailed=true;}}}
    @Override public void slots(){
        world.resetInput();world.game.pause();
        new AlertDialog.Builder(this).setTitle("三个手动存档 · 自动存档独立保留")
            .setItems(new String[]{label(1),label(2),label(3)},(d,w)->slot(w+1))
            .setNeutralButton("新档",(d,w)->newProfile()).setNegativeButton("返回",(d,w)->{if(!ready)blocked();}).show();
    }
    private String label(int i){return "存档 "+i+(store.exists(i)?" · 已保存":" · 空");}
    private void slot(int i){
        AlertDialog.Builder b=new AlertDialog.Builder(this).setTitle(label(i));
        if(ready)b.setPositiveButton("保存",(d,w)->{if(store.exists(i))new AlertDialog.Builder(this).setTitle("覆盖手动存档 "+i+"？").setMessage("将替换此槽进度，其他存档槽保留。")
            .setPositiveButton("覆盖",(a,z)->writeSlot(i)).setNegativeButton("取消",null).show();else writeSlot(i);});
        if(store.exists(i))b.setNeutralButton("读取",(d,w)->new AlertDialog.Builder(this).setTitle("读取存档 "+i+"？").setMessage("成功读取后将替换当前进度。")
            .setPositiveButton("读取",(a,z)->{try{ActionGame loaded=store.read(i);world.replace(loaded);world.setEnabled(true);ready=true;save();}catch(IOException e){new AlertDialog.Builder(this).setTitle("读取失败，当前进度保留").setMessage(e.getMessage()).setPositiveButton("返回",(q,j)->{if(!ready)blocked();}).show();}}).setNegativeButton("取消",(a,z)->{if(!ready)blocked();}).show());
        b.setNegativeButton("返回",(d,w)->slots()).setOnCancelListener(d->{if(!ready)blocked();}).show();
    }
    private void writeSlot(int i){try{store.write(i,world.game);Toast.makeText(this,"已保存到存档 "+i,Toast.LENGTH_SHORT).show();}catch(IOException e){Toast.makeText(this,"保存失败："+e.getMessage(),Toast.LENGTH_LONG).show();}}
    private void blocked(){new AlertDialog.Builder(this).setTitle("先恢复进度或建立新档").setPositiveButton("存档",(d,w)->slots()).setNegativeButton("新档",(d,w)->newProfile()).setCancelable(false).show();}
    private void newProfile(){new AlertDialog.Builder(this).setTitle("建立全新档案？").setMessage("营地升级、自定义角色、全部角色经验、技能树、灵魂、圣遗物收藏和当前远征将重置。三个手动存档保留。")
        .setPositiveButton("建立新档",(d,w)->{world.replace(new ActionGame());world.setEnabled(true);ready=true;save();}).setNegativeButton("取消",(d,w)->{if(!ready)blocked();}).show();}
    @Override public void help(){world.resetInput();world.game.pause();new AlertDialog.Builder(this).setTitle("守灯者手册")
        .setMessage("左侧摇杆移动；按住右侧攻击连续出招，可边移动边攻击。攻击优先瞄准附近视线内敌人。\n\n闪避按当前移动方向发动，未移动时沿朝向闪避；带短暂无敌，基础冷却 1.15 秒。技能基础冷却 7 秒。每轮有两瓶基础恢复 45% 生命的药水；出身、特质与天赋会改变这些数值。\n\n地面红圈或直线表示敌人即将攻击，请及时离开。攻击和闪避可同时用不同手指操作。\n\n清空房间后选择一项本轮增益；向上进入左侧泉径恢复生命，或右侧余烬门获得额外资源。第五庭院有首领，半血后弹幕变密。\n\n胜利带回全部余烬与 40 奖励；失败或主动结束带回已拾取余烬的 70%。角色经验和营地升级永久保留。\n\n每 15 秒、换房、暂停与切后台自动存档。读取战斗存档后先暂停，点击继续。卸载会删除本地存档。\n\n营地的角色工坊可改造三个独立角色：武器、主动技能、出身、特质与外观自由组合。技能树初始2点，每80经验增加1点，最多16点。三个分支的六个终阶天赋只能选一个，营地可免费重置。\n\n灵魂祭坛可为每个角色缔结光辉或禁忌灵魂，并选择专属技能。光辉强化护盾、治疗与穿透；禁忌改造牵引、诅咒与血契，但消耗生命并增加侵蚀。右侧魂技键消耗40魂能，命中可充能；魂技和领域造成的伤害不回充。侵蚀提高受伤、降低治疗，停用禁忌技能3秒后逐渐消退。\n\n营地右上方圣遗物入口可查看图鉴与装备。每个角色可携带两件，初始赠予灯芯和罗盘；第2、4庭院及首领清场发现新圣遗物，失败也保留。回营后装备生效。逆命沙漏每轮仅触发一次，读档不补充。")
        .setPositiveButton("知道了",null).show();}
    @Override public void confirmAbandon(){new AlertDialog.Builder(this).setTitle("结束本轮远征？").setMessage("按失败规则结算：带回已拾取余烬的 70%，保留本轮获得的角色经验。")
        .setPositiveButton("结算",(d,w)->{world.game.abandon();world.resetInput();save();}).setNegativeButton("继续休息",null).show();}
    @Override public void editCharacter(){
        if(world.game.mode!=ActionGame.Mode.CAMP)return;world.resetInput();
        if(world.game.custom>=0)workshopDialog=CharacterWorkshop.show(this,world.game.custom);
        else new AlertDialog.Builder(this).setTitle("选择要编辑的独立角色槽").setItems(new String[]{world.game.profiles[0].name,world.game.profiles[1].name,world.game.profiles[2].name},(d,w)->{workshopDialog=CharacterWorkshop.show(this,w);}).setNegativeButton("取消",null).show();
    }
    @Override public void relics(){if(world.game.mode==ActionGame.Mode.CAMP){world.resetInput();relicDialog=RelicCabinet.show(this);}}
    @Override public void souls(){if(world.game.mode==ActionGame.Mode.CAMP){world.resetInput();soulDialog=SoulAltar.show(this);}}
    @Override public void talent(int id){
        ActionGame g=world.game;if(g.mode!=ActionGame.Mode.CAMP||id<0||id>=12)return;
        BuildSystem.Node node=BuildSystem.NODES[id];String reason=BuildSystem.lockReason(g.talents[g.buildId()],g.heroXp(),id);
        String requirements="需要角色 Lv."+node.minLevel+(node.parent<0?" · 无前置":" · 前置「"+BuildSystem.NODES[node.parent].name+"」"+(BuildSystem.ultimate(id)?2:1)+"级");
        talentDialog=new AlertDialog.Builder(this).setTitle(node.name+"  "+g.rank(id)+" / "+node.maxRank)
            .setMessage(node.description+"\n"+SoulSystem.talentExtra(g.soulKind(),id)+"\n\n"+requirements+"\n"+(BuildSystem.ultimate(id)?"六个终阶天赋只能选择一个。\n":"")+"可用天赋点："+g.availablePoints()+"\n"+(reason.isEmpty()?"消耗1点学习。":reason))
            .setPositiveButton("学习 · 1点",(d,w)->{if(g.learn(id)){save();world.invalidate();}}).setNegativeButton("返回",null).create();
        talentDialog.show();talentDialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(reason.isEmpty());
    }
    @Override public void resetTalents(){if(world.game.mode!=ActionGame.Mode.CAMP)return;new AlertDialog.Builder(this).setTitle("重置「"+world.game.heroName()+"」的天赋？").setMessage("免费返还全部已投入点数，保留角色经验、外观和其他角色的天赋。")
        .setPositiveButton("重置",(d,w)->{world.game.resetTalents();save();world.invalidate();}).setNegativeButton("取消",null).show();}
    @Override public void sound(String event){if(audio!=null)audio.play(event);}
    @Override protected void onResume(){super.onResume();if(world!=null)world.startFrames();}
    @Override protected void onPause(){if(world!=null){world.game.pause();world.stopFrames();save();}super.onPause();}
    @Override protected void onDestroy(){if(world!=null)world.stopFrames();if(audio!=null)audio.close();super.onDestroy();}
    @Override public void onBackPressed(){if(world!=null&&world.closeWorkshop())return;if(world!=null&&world.game.mode!=ActionGame.Mode.CAMP){world.game.pause();world.resetInput();save();}else super.onBackPressed();}
}
