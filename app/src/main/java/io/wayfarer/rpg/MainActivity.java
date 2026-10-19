package io.wayfarer.rpg;

import android.app.*;
import android.os.Bundle;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.view.*;
import android.widget.*;
import java.io.IOException;
import io.wayfarer.rpg.core.*;

/** Native Android UI. No WebView, networking, accounts or external storage permission. */
public final class MainActivity extends Activity {
    private GameEngine game;
    private SaveStore saves;
    private WorldView world;
    private TextView stats, location, quest, log, saveStatus;
    private LinearLayout actions;
    private boolean renderedBattle=false,ready=false;
    private boolean saveErrorShown=false;
    private final int ink=Color.rgb(231,236,218),muted=Color.rgb(164,186,173);
    @Override public void onCreate(Bundle state) {
        super.onCreate(state);saves=new SaveStore(this);
        try {game=saves.exists(0)?saves.read(0):new GameEngine();}
        catch(IOException e) {
            game=new GameEngine();
            // Do not write over an unreadable autosave without the player's explicit choice.
            build();refresh();
            new AlertDialog.Builder(this).setTitle("自动存档无法读取")
                .setMessage("原存档未覆盖。可先尝试读取手动存档；开始新游戏会替换自动存档。\n"+e.getMessage())
                .setPositiveButton("查看存档",(d,w)->slots())
                .setNegativeButton("开始新游戏",(d,w)->{ready=true;changed();}).setCancelable(false).show();
            return;
        }
        ready=true;build();refresh();
    }
    private int dp(int n) {return Math.round(n*getResources().getDisplayMetrics().density);}
    private GradientDrawable panel(int color) {GradientDrawable d=new GradientDrawable();d.setColor(color);d.setCornerRadius(dp(12));return d;}
    private TextView text(String value,int size,int color) {
        TextView t=new TextView(this);t.setText(value);t.setTextSize(size);t.setTextColor(color);t.setPadding(dp(12),dp(5),dp(12),dp(5));return t;
    }
    private Button button(String title,Runnable action) {
        Button b=new Button(this);b.setText(title);b.setTextSize(14);b.setAllCaps(false);b.setTextColor(ink);b.setMinHeight(dp(48));
        b.setBackground(panel(Color.rgb(45,67,65)));
        b.setOnClickListener(v->{if(ready)action.run();else new AlertDialog.Builder(this).setTitle("选择存档")
            .setMessage("请先读取有效的手动存档，或确认开始新游戏。")
            .setPositiveButton("存档",(d,w)->slots()).setNegativeButton("新游戏",(d,w)->newGame()).show();});return b;
    }
    private void row(LinearLayout parent,String[] titles,Runnable[] jobs) {
        LinearLayout r=new LinearLayout(this);r.setPadding(dp(4),dp(3),dp(4),dp(3));
        for(int i=0;i<titles.length;i++) {
            Button b=button(titles[i],jobs[i]);LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(0,dp(50),1);p.setMargins(dp(3),0,dp(3),0);r.addView(b,p);
        }
        parent.addView(r);
    }
    private void build() {
        ScrollView scroll=new ScrollView(this);scroll.setFillViewport(true);scroll.setBackgroundColor(Color.rgb(16,28,32));
        LinearLayout root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);root.setPadding(dp(8),dp(8),dp(8),dp(16));scroll.addView(root);
        root.setOnApplyWindowInsetsListener((v,insets)->{root.setPadding(dp(8)+insets.getSystemWindowInsetLeft(),dp(8)+insets.getSystemWindowInsetTop(),dp(8)+insets.getSystemWindowInsetRight(),dp(16)+insets.getSystemWindowInsetBottom());return insets;});
        TextView title=text("WAYFARER  /  旅人 RPG",21,ink);title.setTypeface(null,Typeface.BOLD);root.addView(title);
        location=text("",13,muted);root.addView(location);
        stats=text("",14,ink);stats.setBackground(panel(Color.rgb(30,47,49)));root.addView(stats);
        quest=text("",13,Color.rgb(205,223,161));root.addView(quest);
        world=new WorldView(this);root.addView(world,new LinearLayout.LayoutParams(-1,-2));
        TextView legend=text("浅绿：你  ·  长：长老  ·  商：商店  ·  宿：旅店  ·  !：敌人",11,muted);root.addView(legend);
        log=text("",14,ink);log.setMinHeight(dp(65));log.setAccessibilityLiveRegion(View.ACCESSIBILITY_LIVE_REGION_POLITE);root.addView(log);
        actions=new LinearLayout(this);actions.setOrientation(LinearLayout.VERTICAL);root.addView(actions);
        row(root,new String[]{"背包 / 装备","任务","存档"},new Runnable[]{this::inventory,this::journal,this::slots});
        row(root,new String[]{"操作说明","新游戏"},new Runnable[]{this::help,this::newGame});
        saveStatus=text("",11,muted);root.addView(saveStatus);
        setContentView(scroll);root.requestApplyInsets();makeActions();
    }
    private void makeActions() {
        actions.removeAllViews();renderedBattle=game.inBattle();
        if(renderedBattle) {
            row(actions,new String[]{"攻击","星火斩 · 5 MP","防御 +3 MP"},new Runnable[]{()->act("attack"),()->act("skill"),()->act("guard")});
            row(actions,new String[]{"生命药水","法力药水","逃跑"},new Runnable[]{()->act("potion"),()->act("ether"),()->act("flee")});
        } else {
            row(actions,new String[]{"↑ 向上","交互"},new Runnable[]{()->move(0,-1),this::interact});
            row(actions,new String[]{"← 向左","↓ 向下","向右 →"},new Runnable[]{()->move(-1,0),()->move(0,1),()->move(1,0)});
        }
    }
    private void refresh() {
        location.setText(game.map().name+"  /  "+(game.inBattle()?"回合制战斗":"探索模式")+"  ·  v0.1.0");
        stats.setText("旅人  Lv."+game.level+"     金币 "+game.gold+"\n生命 "+game.hp+"/"+game.maxHp()+"     法力 "+game.mp+"/"+game.maxMp()+"     经验 "+(game.level==GameEngine.MAX_LEVEL?"MAX":game.xp+"/"+game.nextXp()));
        quest.setText("当前目标  /  "+game.questText());log.setText(game.message);world.show(game);
        if(renderedBattle!=game.inBattle())makeActions();
    }
    private void changed() {refresh();autoSave();}
    private void autoSave() {
        if(!ready)return;
        try {saves.write(0,game);saveStatus.setText("自动存档已保存 · 可另存至 3 个手动槽");saveErrorShown=false;}
        catch(IOException e) {saveStatus.setText("保存失败："+e.getMessage());if(!saveErrorShown){Toast.makeText(this,"保存失败，请检查设备存储空间",Toast.LENGTH_LONG).show();saveErrorShown=true;}}
    }
    private void move(int dx,int dy) {game.move(dx,dy);changed();}
    private void act(String action) {game.act(action);changed();}
    private void interact() {
        GameData.Event e=game.nearby();game.interact();changed();
        if(e==null)return;
        if(e.kind.equals("shop")) shop();
        else if(e.kind.equals("inn")) new AlertDialog.Builder(this).setTitle("风栖旅店")
            .setMessage("免费恢复全部生命和法力。林地普通敌人刷新，任务进度保留。")
            .setPositiveButton("休息",(d,w)->{game.rest();changed();}).setNegativeButton("离开",null).show();
        else new AlertDialog.Builder(this).setTitle(e.kind.equals("elder")?"长老":"宝箱").setMessage(game.message).setPositiveButton("继续",null).show();
    }
    private void shop() {
        new AlertDialog.Builder(this).setTitle("商店 · "+game.gold+" 金币")
            .setItems(new String[]{"生命药水（恢复 40 HP）· 15 金币","法力药水（恢复 15 MP）· 20 金币","铁剑（攻击 +5，自动装备）· 60 金币","皮甲（防御 +3，自动装备）· 50 金币"},
                (d,w)->{game.buy(new String[]{"potion","ether","sword","armor"}[w]);changed();Toast.makeText(this,game.message,Toast.LENGTH_SHORT).show();})
            .setNegativeButton("离开",null).show();
    }
    private void inventory() {
        new AlertDialog.Builder(this).setTitle("背包与装备")
            .setItems(new String[]{"使用生命药水 ×"+game.potions,"使用法力药水 ×"+game.ethers,
                "查看装备："+(game.sword==1?"铁剑":"木剑")+" / "+(game.armor==1?"皮甲":"布衣")},(d,w)->{
                if(w<2){game.useItem(w==0?"potion":"ether");changed();}
                else new AlertDialog.Builder(this).setTitle("角色属性").setMessage("攻击 "+game.attack()+"\n防御 "+game.defense()+"\n装备在购买时自动穿戴。\n等级上限 "+GameEngine.MAX_LEVEL).setPositiveButton("返回",null).show();})
            .setNegativeButton("关闭",null).show();
    }
    private void journal() {
        new AlertDialog.Builder(this).setTitle("任务日志")
            .setMessage("01  林地委托\n"+(game.quest==0?"未接取":game.quest==1?"进行中":"已完成")+" · 清理敌人 "+Math.min(3,game.kills)+"/3\n\n02  灯塔的回声\n"+(game.quest<2?"未解锁":game.quest==3?"已完成":"进行中")+"\n\n"+game.questText())
            .setPositiveButton("继续",null).show();
    }
    private void slots() {
        new AlertDialog.Builder(this).setTitle("手动存档 · 自动存档独立保留")
            .setItems(new String[]{slotLabel(1),slotLabel(2),slotLabel(3)},(d,w)->slotOptions(w+1)).setNegativeButton("关闭",null).show();
    }
    private String slotLabel(int s) {return "存档 "+s+(saves.exists(s)?" · 已有记录":" · 空");}
    private void slotOptions(int index) {
        AlertDialog.Builder dialog=new AlertDialog.Builder(this).setTitle(slotLabel(index));
        if(ready)dialog.setPositiveButton("保存到此槽",(d,w)->{
            if(saves.exists(index))new AlertDialog.Builder(this).setTitle("覆盖存档 "+index+"？").setMessage("此槽原进度将被替换，其他槽不受影响。")
                .setPositiveButton("覆盖",(a,b)->saveManual(index)).setNegativeButton("取消",null).show();
            else saveManual(index);
        });
        if(saves.exists(index))dialog.setNeutralButton("读取",(d,w)->new AlertDialog.Builder(this).setTitle("读取存档 "+index+"？")
            .setMessage("将替换当前游戏进度，并更新自动存档。")
            .setPositiveButton("读取",(a,b)->{
                try {GameEngine loaded=saves.read(index);game=loaded;ready=true;changed();}
                catch(IOException e){new AlertDialog.Builder(this).setTitle("读取失败，当前游戏未改变").setMessage(e.getMessage()).setPositiveButton("知道了",null).show();}
            }).setNegativeButton("取消",null).show());
        dialog.setNegativeButton("返回",(d,w)->slots()).show();
    }
    private void saveManual(int index) {
        try {saves.write(index,game);Toast.makeText(this,"已保存到存档 "+index,Toast.LENGTH_SHORT).show();}
        catch(IOException e){new AlertDialog.Builder(this).setTitle("保存失败").setMessage(e.getMessage()).setPositiveButton("知道了",null).show();}
    }
    private void newGame() {
        new AlertDialog.Builder(this).setTitle("开始新游戏？").setMessage("自动存档会被替换，三个手动存档保留。")
            .setPositiveButton("开始",(d,w)->{game=new GameEngine();ready=true;changed();})
            .setNegativeButton("取消",null).show();
    }
    private void help() {
        new AlertDialog.Builder(this).setTitle("旅人手册")
            .setMessage("方向按钮或键盘方向键 / WASD 移动；E 或空格交互。\n\n靠近长老接任务；走到 ! 处开始战斗。金色箭头切换区域。任务完成后回到长老身边交互。\n\n星火斩消耗 5 MP；防御恢复 3 MP 并减伤。敌人每第三回合重击。战斗中使用药水也消耗一回合。普通战斗有 70% 逃跑机会，首领不可逃跑。\n\n旅店免费恢复并刷新普通敌人。战败扣除最多 20 金币，回村恢复。宝箱只能领取一次。\n\n每次操作后自动保存，切后台也会保存。卸载会清除本地存档。本版是原生 Android 可玩模板，图形均由程序绘制。")
            .setPositiveButton("出发",null).show();
    }
    @Override public boolean onKeyDown(int code,KeyEvent event) {
        if(ready && !game.inBattle() && event.getRepeatCount()==0) {
            switch(code) {
                case KeyEvent.KEYCODE_DPAD_UP:case KeyEvent.KEYCODE_W:move(0,-1);return true;
                case KeyEvent.KEYCODE_DPAD_DOWN:case KeyEvent.KEYCODE_S:move(0,1);return true;
                case KeyEvent.KEYCODE_DPAD_LEFT:case KeyEvent.KEYCODE_A:move(-1,0);return true;
                case KeyEvent.KEYCODE_DPAD_RIGHT:case KeyEvent.KEYCODE_D:move(1,0);return true;
                case KeyEvent.KEYCODE_E:case KeyEvent.KEYCODE_SPACE:interact();return true;
                default:break;
            }
        }
        return super.onKeyDown(code,event);
    }
    @Override protected void onPause() {super.onPause();if(game!=null && saves!=null)autoSave();}
}
