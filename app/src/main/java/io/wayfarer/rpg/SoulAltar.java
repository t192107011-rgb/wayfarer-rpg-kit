package io.wayfarer.rpg;

import android.app.AlertDialog;
import android.view.*;
import android.widget.*;
import io.wayfarer.rpg.core.*;

/** Camp-only soul binding. Each profession selects its own radiant or forbidden form. */
public final class SoulAltar {
    private SoulAltar(){}
    private static int dp(MainActivity a,int n){return Math.round(n*a.getResources().getDisplayMetrics().density);}
    private static TextView text(MainActivity a,LinearLayout parent,String value,int size){TextView t=new TextView(a);t.setText(value);t.setTextSize(size);t.setPadding(0,dp(a,8),0,dp(a,8));parent.addView(t);return t;}
    private static ArrayAdapter<String> adapter(MainActivity a,String[] values){ArrayAdapter<String> d=new ArrayAdapter<String>(a,android.R.layout.simple_spinner_item,values){
        @Override public View getView(int position,View recycled,ViewGroup parent){TextView t=(TextView)super.getView(position,recycled,parent);t.setTextSize(16);return t;}
    };d.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);return d;}
    private static String formName(int kind,int job){return kind==0?"未缔结":SoulSystem.ARTS[kind][job];}
    private static String formDetail(int kind,int job){
        if(kind==0)return "尚未缔结灵魂。缔结光辉或禁忌后，将解锁与当前职业绑定的专属魂技。";
        if(kind==1)switch(job){
            case CareerSystem.HUNTER:return "曙光猎令：获得辉盾并标记全场，精准打击高威胁目标。猎人魂技冷却12秒。";
            case CareerSystem.ASSASSIN:return "无影圣步：短暂无敌突进，净化路径弹幕，按命中数量获得辉盾。刺客魂技冷却10秒。";
            case CareerSystem.DETECTIVE:return "真相显影：完成全场调查，短暂中断敌人并暴露弱点，获得辉盾。警探魂技冷却14秒。";
            default:return "光辉头条：锁定头条目标并向附近敌人传播伤害，同时恢复少量生命与辉盾。记者魂技冷却12秒。";
        }
        switch(job){
            case CareerSystem.HUNTER:return "血猎追魂：献祭生命与侵蚀，追猎最近目标；目标濒危时伤害显著提高。猎人魂技冷却14秒。";
            case CareerSystem.ASSASSIN:return "蚀影处决：献祭生命，突进并对路径敌人造成高额伤害，命中后少量恢复生命。刺客魂技冷却16秒。";
            case CareerSystem.DETECTIVE:return "深渊审讯：按目标调查程度施加审判伤害与硬直；没有线索时先进行初步调查。警探魂技冷却18秒。";
            default:return "血色头条：献祭生命与侵蚀，对敌群施加诅咒并引发有限次数的连锁爆发。记者魂技冷却20秒。";
        }
    }
    public static AlertDialog show(MainActivity a){
        ActionGame g=a.world.game;int[] draft={g.soulKind(),g.soulArt()};
        LinearLayout row=new LinearLayout(a);row.setOrientation(LinearLayout.HORIZONTAL);row.setPadding(dp(a,18),0,dp(a,18),0);
        LinearLayout left=new LinearLayout(a),right=new LinearLayout(a);left.setOrientation(LinearLayout.VERTICAL);right.setOrientation(LinearLayout.VERTICAL);right.setPadding(dp(a,20),0,0,0);
        ScrollView ls=new ScrollView(a),rs=new ScrollView(a);ls.addView(left);rs.addView(right);row.addView(ls,new LinearLayout.LayoutParams(0,-1,1));row.addView(rs,new LinearLayout.LayoutParams(0,-1,1));
        text(a,left,"灵魂归属 · 当前角色独立保存",14);Spinner kind=new Spinner(a);kind.setTag("soul-kind");kind.setContentDescription("灵魂归属");kind.setAdapter(adapter(a,SoulSystem.NAMES));kind.setSelection(draft[0]);left.addView(kind,new LinearLayout.LayoutParams(-1,dp(a,48)));
        TextView passive=text(a,left,"",14),variant=text(a,left,"",14);
        text(a,right,"职业专属形态 · 自动匹配当前职业",14);TextView form=text(a,right,"",18),detail=text(a,right,"",14);
        text(a,right,"每轮初始40魂能，最多100。有效攻击积累魂能；光辉护盾吸收伤害也可充能。专属魂技与其伤害不能回充。

光辉偏向防护、控制与稳定收益；禁忌以生命和侵蚀换取爆发。更换只在营地生效，取消不修改。",13);
        Runnable refresh=()->{passive.setText(SoulSystem.PASSIVES[draft[0]]);variant.setText("当前通用主动技\n"+SoulSystem.DETAILS[draft[0]][g.skillStyle()]);form.setText(formName(draft[0],g.job()));detail.setText(formDetail(draft[0],g.job()));};
        kind.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener(){public void onItemSelected(AdapterView<?> p,View v,int pos,long id){if(pos!=draft[0]){draft[0]=pos;draft[1]=0;}refresh.run();}public void onNothingSelected(AdapterView<?> p){}});
        AlertDialog dialog=new AlertDialog.Builder(a).setTitle(g.heroName()+" · 灵魂祭坛").setView(row).setPositiveButton("确认缔结",(d,w)->{if(g.bindSoul(draft[0],draft[1])){a.save();a.world.invalidate();}}).setNegativeButton("取消",null).create();
        dialog.setOnShowListener(d->{Window window=dialog.getWindow();if(window!=null)window.setLayout((int)(a.getResources().getDisplayMetrics().widthPixels*.9f),(int)(a.getResources().getDisplayMetrics().heightPixels*.94f));refresh.run();});dialog.show();return dialog;
    }
}
