package io.wayfarer.rpg;

import android.app.AlertDialog;
import android.view.*;
import android.widget.*;
import io.wayfarer.rpg.core.*;

/** Camp-only soul binding. Draft choices never change the live game before confirmation. */
public final class SoulAltar {
    private SoulAltar(){}
    private static int dp(MainActivity a,int n){return Math.round(n*a.getResources().getDisplayMetrics().density);}
    private static TextView text(MainActivity a,LinearLayout parent,String value,int size){TextView t=new TextView(a);t.setText(value);t.setTextSize(size);t.setPadding(0,dp(a,8),0,dp(a,8));parent.addView(t);return t;}
    private static ArrayAdapter<String> adapter(MainActivity a,String[] values){ArrayAdapter<String> d=new ArrayAdapter<String>(a,android.R.layout.simple_spinner_item,values){
        @Override public View getView(int position,View recycled,ViewGroup parent){TextView t=(TextView)super.getView(position,recycled,parent);t.setTextSize(16);return t;}
    };d.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);return d;}
    public static AlertDialog show(MainActivity a){
        ActionGame g=a.world.game;int[] draft={g.soulKind(),g.soulArt()};
        LinearLayout row=new LinearLayout(a);row.setOrientation(LinearLayout.HORIZONTAL);row.setPadding(dp(a,18),0,dp(a,18),0);
        LinearLayout left=new LinearLayout(a),right=new LinearLayout(a);left.setOrientation(LinearLayout.VERTICAL);right.setOrientation(LinearLayout.VERTICAL);right.setPadding(dp(a,20),0,0,0);
        ScrollView ls=new ScrollView(a),rs=new ScrollView(a);ls.addView(left);rs.addView(right);row.addView(ls,new LinearLayout.LayoutParams(0,-1,1));row.addView(rs,new LinearLayout.LayoutParams(0,-1,1));
        text(a,left,"灵魂归属 · 当前角色独立保存",14);Spinner kind=new Spinner(a);kind.setTag("soul-kind");kind.setContentDescription("灵魂归属");kind.setAdapter(adapter(a,SoulSystem.NAMES));kind.setSelection(draft[0]);left.addView(kind,new LinearLayout.LayoutParams(-1,dp(a,48)));
        TextView passive=text(a,left,"",14),variant=text(a,left,"",14);
        text(a,right,"专属技能 · 独立按键施放",14);Spinner art=new Spinner(a);art.setTag("soul-art");art.setContentDescription("专属技能");art.setAdapter(adapter(a,SoulSystem.ARTS[draft[0]]));art.setSelection(draft[1]);right.addView(art,new LinearLayout.LayoutParams(-1,dp(a,48)));
        TextView detail=text(a,right,"",14);text(a,right,"每轮初始40魂能，最多100。有效攻击积累魂能；光辉护盾吸收伤害也可充能。专属技能与领域的伤害不能回充。\n\n更换只在营地生效，保留经验、天赋与外观。取消不修改。",13);
        Runnable refresh=()->{passive.setText(SoulSystem.PASSIVES[draft[0]]);variant.setText("当前主动技能变化\n"+SoulSystem.DETAILS[draft[0]][g.skillStyle()]);detail.setText(SoulSystem.ART_DETAILS[draft[0]][draft[1]]);art.setEnabled(draft[0]!=0);};
        art.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener(){public void onItemSelected(AdapterView<?> p,View v,int pos,long id){draft[1]=draft[0]==0?0:pos;refresh.run();}public void onNothingSelected(AdapterView<?> p){}});
        kind.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener(){public void onItemSelected(AdapterView<?> p,View v,int pos,long id){if(pos!=draft[0]){draft[0]=pos;draft[1]=0;art.setAdapter(adapter(a,SoulSystem.ARTS[pos]));art.setSelection(0);}refresh.run();}public void onNothingSelected(AdapterView<?> p){}});
        AlertDialog dialog=new AlertDialog.Builder(a).setTitle(g.heroName()+" · 灵魂祭坛").setView(row).setPositiveButton("确认缔结",(d,w)->{if(g.bindSoul(draft[0],draft[1])){a.save();a.world.invalidate();}}).setNegativeButton("取消",null).create();
        dialog.setOnShowListener(d->{Window window=dialog.getWindow();if(window!=null)window.setLayout((int)(a.getResources().getDisplayMetrics().widthPixels*.9f),(int)(a.getResources().getDisplayMetrics().heightPixels*.94f));refresh.run();});dialog.show();return dialog;
    }
}
