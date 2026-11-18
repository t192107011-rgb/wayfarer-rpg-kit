package io.wayfarer.rpg;

import android.app.AlertDialog;
import android.view.*;
import android.widget.*;
import io.wayfarer.rpg.core.*;

/** A single camp-only editor for all six independent career builds. */
public final class CareerHall {
    private CareerHall(){}
    private static int dp(MainActivity a,int n){return Math.round(n*a.getResources().getDisplayMetrics().density);}
    private static TextView text(MainActivity a,LinearLayout parent,String value,int size){TextView t=new TextView(a);t.setText(value);t.setTextSize(size);t.setPadding(0,dp(a,6),0,dp(a,6));parent.addView(t);return t;}
    private static ArrayAdapter<String> adapter(MainActivity a,String[] names){ArrayAdapter<String> d=new ArrayAdapter<>(a,android.R.layout.simple_spinner_item,names);d.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);return d;}
    private static Spinner picker(MainActivity a,LinearLayout parent,String label,String tag,String[] names,int chosen){text(a,parent,label,14);Spinner s=new Spinner(a);s.setTag(tag);s.setContentDescription(label);s.setAdapter(adapter(a,names));s.setSelection(chosen);parent.addView(s,new LinearLayout.LayoutParams(-1,dp(a,48)));return s;}
    public static AlertDialog show(MainActivity a){
        ActionGame g=a.world.game;int[] draft=g.careers[g.buildId()].clone();if(CareerSystem.selectableIndex(draft[0])==0&&draft[0]!=CareerSystem.HUNTER){draft[0]=CareerSystem.HUNTER;draft[1]=0;}boolean[] pact={g.pactEnabled()&&draft[0]==g.job()};
        LinearLayout row=new LinearLayout(a);row.setOrientation(LinearLayout.HORIZONTAL);row.setPadding(dp(a,18),0,dp(a,18),0);
        LinearLayout left=new LinearLayout(a),right=new LinearLayout(a);left.setOrientation(LinearLayout.VERTICAL);right.setOrientation(LinearLayout.VERTICAL);right.setPadding(dp(a,18),0,0,0);
        ScrollView ls=new ScrollView(a),rs=new ScrollView(a);ls.addView(left);rs.addView(right);ls.setScrollbarFadingEnabled(false);rs.setScrollbarFadingEnabled(false);row.addView(ls,new LinearLayout.LayoutParams(0,-1,1));row.addView(rs,new LinearLayout.LayoutParams(0,-1,1));
        Spinner job=picker(a,left,"职业 · 仅开放四种职业","career-job",CareerSystem.SELECTABLE_NAMES,CareerSystem.selectableIndex(draft[0]));
        TextView base=text(a,left,"",14);
        Spinner spec=picker(a,left,"专精 · 角色 Lv.3 后择一","career-spec",CareerSystem.SPECS[draft[0]],draft[1]);
        TextView special=text(a,left,"",14);
        TextView temperament=text(a,left,"",14);temperament.setTag("career-temperament");
        text(a,left,"行事倾向供角色扮演参考，不强制道德立场。职业能力无需缔结灵魂即可使用。",12);
        CheckBox forbidden=new CheckBox(a);forbidden.setTag("career-pact");forbidden.setText("选择越界禁术 · 可选，Lv.3开放");forbidden.setChecked(pact[0]);right.addView(forbidden);
        TextView pactDetails=text(a,right,"",14);pactDetails.setTag("career-pact-details");
        Spinner background=picker(a,right,"背景经历 · 提供独立被动","career-background",CareerSystem.BACKGROUNDS,draft[2]);
        TextView passive=text(a,right,"",15);
        text(a,right,"目前仅保留猎人、刺客、侦探、记者四种职业。背景经历与职业独立，可自由组合；原有出身与特质继续保留。\n\n每个角色独立保存，营地可免费调整。确认才提交，取消保留原配置。",13);
        TextView level=text(a,right,"",14),error=text(a,right,"",13);
        AlertDialog dialog=new AlertDialog.Builder(a).setTitle(g.heroName()+" · 职业与历练").setView(row)
            .setPositiveButton("确认历练",(d,w)->{if(g.chooseCareer(draft[0],draft[1],draft[2],pact[0])){a.save();a.world.invalidate();}}).setNegativeButton("取消",null).create();
        Runnable refresh=()->{
            base.setText(CareerSystem.CLASS_DETAILS[draft[0]]);boolean hasPact=ProfessionPacts.available(draft[0]);temperament.setText("行事风格："+(draft[0]<ProfessionPacts.TEMPERAMENTS.length?ProfessionPacts.TEMPERAMENTS[draft[0]]:"擅长高速近身作战与战场控制。")+"\n「"+(draft[0]<ProfessionPacts.ORDINARY_LINES.length?ProfessionPacts.ORDINARY_LINES[draft[0]]:"保持冷静，抓住机会。")+"」");pactDetails.setText(hasPact?ProfessionPacts.NAMES[draft[0]]+"\n"+ProfessionPacts.TRANSGRESSIONS[draft[0]]+"\n禁术发动：「"+ProfessionPacts.LINES[draft[0]]+"」\n\n"+ProfessionPacts.DETAILS[draft[0]]+"\n"+(pact[0]?"已选择；确认后才生效。":"未开启；普通职业能力照常生效。"):"该职业暂不开放职业越界技；灵魂技能仍可独立使用。");special.setText(CareerSystem.SPEC_DETAILS[draft[0]][draft[1]]);passive.setText(CareerSystem.BACKGROUND_DETAILS[draft[2]]);
            boolean unlocked=g.heroXp()>=CareerSystem.SPECIALIZATION_XP;spec.setEnabled(draft[0]>0&&unlocked);forbidden.setEnabled(unlocked&&ProfessionPacts.available(draft[0]));
            level.setText("当前 Lv."+g.heroLevel()+" · "+g.heroXp()+"经验\n"+(unlocked?"专精已开放：选职业后可选择一个专精。":"再获得 "+(CareerSystem.SPECIALIZATION_XP-g.heroXp())+" 经验开放专精。职业与背景现在即可选择。"));
            String issue=CareerSystem.invalid(draft[0],draft[1],draft[2],g.heroXp());error.setText(issue);if(dialog.isShowing())dialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(issue.isEmpty());
        };
        forbidden.setOnCheckedChangeListener((button,checked)->{pact[0]=checked;refresh.run();});
        spec.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener(){public void onItemSelected(AdapterView<?> p,View v,int pos,long id){draft[1]=pos<CareerSystem.SPECS[draft[0]].length?pos:0;refresh.run();}public void onNothingSelected(AdapterView<?> p){}});
        job.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener(){public void onItemSelected(AdapterView<?> p,View v,int pos,long id){int selected=CareerSystem.SELECTABLE_JOBS[pos];if(selected!=draft[0]){draft[0]=selected;draft[1]=0;pact[0]=false;forbidden.setChecked(false);spec.setAdapter(adapter(a,CareerSystem.SPECS[selected]));spec.setSelection(0);}refresh.run();}public void onNothingSelected(AdapterView<?> p){}});
        background.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener(){public void onItemSelected(AdapterView<?> p,View v,int pos,long id){draft[2]=pos;refresh.run();}public void onNothingSelected(AdapterView<?> p){}});
        dialog.setOnShowListener(d->{Window window=dialog.getWindow();if(window!=null)window.setLayout((int)(a.getResources().getDisplayMetrics().widthPixels*.94f),(int)(a.getResources().getDisplayMetrics().heightPixels*.94f));refresh.run();});dialog.show();return dialog;
    }
}
