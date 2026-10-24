package io.wayfarer.rpg;

import android.app.AlertDialog;
import android.graphics.Color;
import android.view.*;
import android.widget.*;
import io.wayfarer.rpg.core.*;
import java.util.*;

/** Draft-only loadout editor alongside the complete discoverable collection. */
public final class RelicCabinet {
    private RelicCabinet(){}
    private static int dp(MainActivity a,int n){return Math.round(n*a.getResources().getDisplayMetrics().density);}
    private static TextView text(MainActivity a,LinearLayout parent,String value,int size){TextView t=new TextView(a);t.setText(value);t.setTextSize(size);t.setPadding(0,dp(a,6),0,dp(a,6));parent.addView(t);return t;}
    public static AlertDialog show(MainActivity a){
        ActionGame g=a.world.game;int[] draft=g.relics[g.buildId()].clone();
        LinearLayout row=new LinearLayout(a);row.setOrientation(LinearLayout.HORIZONTAL);row.setPadding(dp(a,18),0,dp(a,18),0);
        LinearLayout left=new LinearLayout(a),right=new LinearLayout(a);left.setOrientation(LinearLayout.VERTICAL);right.setOrientation(LinearLayout.VERTICAL);right.setPadding(dp(a,18),0,0,0);
        ScrollView ls=new ScrollView(a),rs=new ScrollView(a);ls.addView(left);rs.addView(right);row.addView(ls,new LinearLayout.LayoutParams(0,-1,1));row.addView(rs,new LinearLayout.LayoutParams(0,-1,1.25f));
        text(a,left,"当前角色 · 两件携行",16);
        List<Integer> ids=new ArrayList<>();List<String> names=new ArrayList<>();for(int id=0;id<RelicSystem.NAMES.length;id++)if(RelicSystem.owns(g.relicOwned,id)){ids.add(id);names.add(RelicSystem.NAMES[id]);}
        Spinner[] slots=new Spinner[2];
        for(int slot=0;slot<2;slot++){
            text(a,left,"装备槽 "+(slot+1),13);Spinner spinner=new Spinner(a);slots[slot]=spinner;spinner.setTag("relic-slot-"+slot);spinner.setContentDescription("圣遗物装备槽 "+(slot+1));
            ArrayAdapter<String> adapter=new ArrayAdapter<>(a,android.R.layout.simple_spinner_item,names);adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);spinner.setAdapter(adapter);spinner.setSelection(ids.indexOf(draft[slot]));left.addView(spinner,new LinearLayout.LayoutParams(-1,dp(a,48)));
        }
        TextView detail=text(a,left,"",14),error=text(a,left,"",13);error.setTextColor(Color.rgb(239,175,141));
        text(a,left,"确认后生效；取消不修改。\n收藏共享，各角色独立装备。\n同件不能在同一角色上叠加。",13);
        text(a,right,"圣遗物图鉴 · "+Integer.bitCount(g.relicOwned)+" / 8",17);
        text(a,right,"初始赠予灯芯与罗盘。第2、4庭院及首领清场各发现一件未拥有的圣遗物；发现即保留，回营装备。收集齐后改为本轮余烬+15。",13);
        for(int id=1;id<RelicSystem.NAMES.length;id++){
            boolean owned=RelicSystem.owns(g.relicOwned,id);
            TextView name=text(a,right,(owned?"◆ 已收录 · ":"◇ 待发现 · ")+RelicSystem.NAMES[id],16);name.setTextColor(owned?Color.rgb(232,212,147):Color.rgb(170,176,176));
            text(a,right,RelicSystem.DETAILS[id],13);
        }
        AlertDialog dialog=new AlertDialog.Builder(a).setTitle(g.heroName()+" · 圣遗物收藏").setView(row)
            .setPositiveButton("确认装备",(d,w)->{if(g.equipRelics(draft[0],draft[1])){a.save();a.world.invalidate();}})
            .setNeutralButton("清空两槽",null).setNegativeButton("取消",null).create();
        Runnable refresh=()->{
            detail.setText((draft[0]==0?"槽1未装备":RelicSystem.NAMES[draft[0]]+"\n"+RelicSystem.DETAILS[draft[0]])+"\n\n"+(draft[1]==0?"槽2未装备":RelicSystem.NAMES[draft[1]]+"\n"+RelicSystem.DETAILS[draft[1]]));
            String issue=RelicSystem.invalid(g.relicOwned,draft[0],draft[1]);error.setText(issue);
            if(dialog.isShowing())dialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(issue.isEmpty());
        };
        for(int slot=0;slot<2;slot++){final int index=slot;slots[slot].setOnItemSelectedListener(new AdapterView.OnItemSelectedListener(){public void onItemSelected(AdapterView<?> p,View v,int position,long id){draft[index]=ids.get(position);refresh.run();}public void onNothingSelected(AdapterView<?> p){}});}
        dialog.setOnShowListener(d->{Window window=dialog.getWindow();if(window!=null)window.setLayout((int)(a.getResources().getDisplayMetrics().widthPixels*.94f),(int)(a.getResources().getDisplayMetrics().heightPixels*.94f));dialog.getButton(AlertDialog.BUTTON_NEUTRAL).setOnClickListener(v->{draft[0]=0;draft[1]=0;slots[0].setSelection(0);slots[1].setSelection(0);refresh.run();});refresh.run();});
        dialog.show();return dialog;
    }
}
