package io.wayfarer.rpg;

import android.app.AlertDialog;
import android.view.*;
import android.widget.*;
import io.wayfarer.rpg.core.*;

/** Camp-only editor for the four rebuilt careers. */
public final class CareerHall {
    private CareerHall(){}
    private static int dp(MainActivity a,int n){return Math.round(n*a.getResources().getDisplayMetrics().density);}
    private static TextView text(MainActivity a,LinearLayout p,String value,int size){TextView t=new TextView(a);t.setText(value);t.setTextSize(size);t.setPadding(0,dp(a,6),0,dp(a,6));p.addView(t);return t;}
    private static ArrayAdapter<String> adapter(MainActivity a,String[] names){ArrayAdapter<String> d=new ArrayAdapter<>(a,android.R.layout.simple_spinner_item,names);d.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);return d;}
    private static Spinner picker(MainActivity a,LinearLayout p,String label,String tag,String[] names,int selected){text(a,p,label,14);Spinner s=new Spinner(a);s.setTag(tag);s.setContentDescription(label);s.setAdapter(adapter(a,names));s.setSelection(selected);p.addView(s,new LinearLayout.LayoutParams(-1,dp(a,48)));return s;}
    public static AlertDialog show(MainActivity a){
        ActionGame g=a.world.game;int[] draft=g.careers[g.buildId()].clone();
        LinearLayout row=new LinearLayout(a);row.setOrientation(LinearLayout.HORIZONTAL);row.setPadding(dp(a,18),0,dp(a,18),0);
        LinearLayout left=new LinearLayout(a),right=new LinearLayout(a);left.setOrientation(LinearLayout.VERTICAL);right.setOrientation(LinearLayout.VERTICAL);right.setPadding(dp(a,18),0,0,0);
        ScrollView ls=new ScrollView(a),rs=new ScrollView(a);ls.addView(left);rs.addView(right);row.addView(ls,new LinearLayout.LayoutParams(0,-1,1));row.addView(rs,new LinearLayout.LayoutParams(0,-1,1));
        Spinner job=picker(a,left,"职业 · 仅四种","career-job",CareerSystem.CLASSES,draft[0]);
        TextView base=text(a,left,"",14);
        Spinner spec=picker(a,left,"专精 · 角色 Lv.3 后开放","career-spec",CareerSystem.SPECS[draft[0]],draft[1]);
        TextView special=text(a,left,"",14);
        Spinner background=picker(a,right,"背景经历 · 独立被动","career-background",CareerSystem.BACKGROUNDS,draft[2]);
        TextView passive=text(a,right,"",15);
        text(a,right,"职业主动技能由职业决定；背景经历与职业独立组合。原有角色、武器、灵魂、出身与特质均保留。旧职业存档已统一重置为猎人。",13);
        TextView level=text(a,right,"",14),error=text(a,right,"",13);
        AlertDialog dialog=new AlertDialog.Builder(a).setTitle(g.heroName()+" · 职业").setView(row)
            .setPositiveButton("确认更换",(d,w)->{draft[0]=job.getSelectedItemPosition();draft[1]=Math.min(spec.getSelectedItemPosition(),CareerSystem.SPECS[draft[0]].length-1);draft[2]=background.getSelectedItemPosition();if(g.chooseCareer(draft[0],draft[1],draft[2])){a.save();a.world.invalidate();}})
            .setNegativeButton("取消",null).create();
        Runnable refresh=()->{
            base.setText(CareerSystem.CLASS_DETAILS[draft[0]]);
            special.setText(CareerSystem.SPEC_DETAILS[draft[0]][draft[1]]);
            passive.setText(CareerSystem.BACKGROUND_DETAILS[draft[2]]);
            boolean unlocked=g.heroXp()>=CareerSystem.SPECIALIZATION_XP;
            spec.setEnabled(unlocked&&CareerSystem.SPECS[draft[0]].length>1);
            level.setText("当前 Lv."+g.heroLevel()+" · "+g.heroXp()+"经验"+(unlocked?" · 专精已开放":" · 再获得 "+(CareerSystem.SPECIALIZATION_XP-g.heroXp())+" 经验开放专精"));
            String issue=CareerSystem.invalid(draft[0],draft[1],draft[2],g.heroXp());error.setText(issue);
            if(dialog.isShowing())dialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(issue.isEmpty());
        };
        spec.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener(){public void onItemSelected(AdapterView<?> p,View v,int pos,long id){draft[1]=pos;refresh.run();}public void onNothingSelected(AdapterView<?> p){}});
        job.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener(){public void onItemSelected(AdapterView<?> p,View v,int pos,long id){if(draft[0]!=pos){draft[0]=pos;draft[1]=0;spec.setAdapter(adapter(a,CareerSystem.SPECS[pos]));spec.setSelection(0);}refresh.run();}public void onNothingSelected(AdapterView<?> p){}});
        background.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener(){public void onItemSelected(AdapterView<?> p,View v,int pos,long id){draft[2]=pos;refresh.run();}public void onNothingSelected(AdapterView<?> p){}});
        dialog.setOnShowListener(d->{Window w=dialog.getWindow();if(w!=null)w.setLayout((int)(a.getResources().getDisplayMetrics().widthPixels*.94f),(int)(a.getResources().getDisplayMetrics().heightPixels*.94f));refresh.run();});
        dialog.show();return dialog;
    }
}