package io.wayfarer.rpg;

import android.app.AlertDialog;
import android.graphics.Canvas;
import android.graphics.Color;
import android.text.*;
import android.view.*;
import android.widget.*;
import io.wayfarer.rpg.core.*;

/** Native draft editor. Only the explicit save button commits any changes. */
public final class CharacterWorkshop {
    private CharacterWorkshop(){}
    public static AlertDialog show(MainActivity a,int slot){
        ActionGame g=a.world.game;BuildSystem.Profile draft=g.profiles[slot].copy();
        LinearLayout body=new LinearLayout(a);body.setOrientation(LinearLayout.VERTICAL);body.setPadding(dp(a,16),dp(a,8),dp(a,16),dp(a,12));
        ScrollView scroll=new ScrollView(a);scroll.setFillViewport(true);scroll.addView(body);
        LinearLayout header=new LinearLayout(a);header.setOrientation(LinearLayout.HORIZONTAL);
        TextView summary=new TextView(a);summary.setTextSize(15);summary.setTextColor(Color.rgb(230,226,202));summary.setPadding(dp(a,12),dp(a,12),dp(a,8),dp(a,8));
        View preview=new View(a){@Override protected void onDraw(Canvas c){super.onDraw(c);c.drawColor(0xFF183235);a.world.drawPortrait(c,getWidth()/2f,getHeight()*.78f,Math.min(getWidth()/64f,getHeight()/60f),draft);}};
        header.addView(preview,new LinearLayout.LayoutParams(dp(a,100),dp(a,120)));header.addView(summary,new LinearLayout.LayoutParams(0,dp(a,120),1));header.setBackgroundColor(0xFF183235);body.addView(header);
        Runnable refresh=()->{ActionGame sample=new ActionGame();sample.profiles[0]=draft.copy();sample.selectCustom(0);
            summary.setText(draft.name+"\n基础生命 "+(int)sample.maxHp()+" · 伤害 "+(int)sample.damage()+" · 移速 "+(int)sample.speed()+"\n技能 "+String.format(java.util.Locale.ROOT,"%.1f",sample.skillPeriod())+"秒 · 闪避 "+String.format(java.util.Locale.ROOT,"%.2f",sample.dodgePeriod())+"秒\n预览含角色等级；不含营地与天赋加成");preview.invalidate();};
        label(a,body,"名字 · 1至10个字符");EditText name=new EditText(a);name.setSingleLine(true);name.setText(draft.name);name.setContentDescription("自定义角色名字");name.setTag("profile-name");body.addView(name);
        name.addTextChangedListener(new TextWatcher(){public void beforeTextChanged(CharSequence s,int st,int count,int after){}public void onTextChanged(CharSequence s,int st,int before,int count){draft.name=s.toString();refresh.run();}public void afterTextChanged(Editable e){}});
        choose(a,body,"武器 · 决定普攻",BuildSystem.WEAPONS,draft.weapon,"profile-weapon",v->{draft.weapon=v;refresh.run();});
        choose(a,body,"主动技能 · 可跨武器搭配",BuildSystem.SKILLS,draft.skill,"profile-skill",v->{draft.skill=v;refresh.run();});
        choose(a,body,"出身",BuildSystem.ORIGINS,draft.origin,"profile-origin",v->{draft.origin=v;refresh.run();});
        choose(a,body,"特质",BuildSystem.TRAITS,draft.trait,"profile-trait",v->{draft.trait=v;refresh.run();});
        choose(a,body,"衣装颜色",BuildSystem.COLORS,draft.color,"profile-color",v->{draft.color=v;refresh.run();});
        choose(a,body,"肤色",new String[]{"浅肤色","棕肤色","深肤色"},draft.skin,"profile-skin",v->{draft.skin=v;refresh.run();});
        choose(a,body,"发型",BuildSystem.HAIR,draft.hair,"profile-hair",v->{draft.hair=v;refresh.run();});
        label(a,body,"改造免费，保留本角色经验与天赋。只能在营地编辑。取消不会改变角色。");
        AlertDialog dialog=new AlertDialog.Builder(a).setTitle("角色工坊 · 槽位 "+(slot+1)).setView(scroll).setPositiveButton("保存并选用",null).setNegativeButton("取消",null).create();
        dialog.setOnShowListener(d->{dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v->{
            try{g.customize(slot,draft);a.world.customRoster=true;a.save();a.world.invalidate();dialog.dismiss();}
            catch(IllegalArgumentException e){name.setError(e.getMessage());name.requestFocus();}
        });Window w=dialog.getWindow();if(w!=null){w.setLayout((int)(a.getResources().getDisplayMetrics().widthPixels*.88f),(int)(a.getResources().getDisplayMetrics().heightPixels*.94f));w.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);}refresh.run();});
        dialog.show();return dialog;
    }
    private static int dp(MainActivity a,int value){return Math.round(value*a.getResources().getDisplayMetrics().density);}
    private interface Pick {void set(int value);}
    private static void label(MainActivity a,LinearLayout body,String text){TextView t=new TextView(a);t.setText(text);t.setTextSize(15);t.setPadding(0,16,0,4);body.addView(t);}
    private static void choose(MainActivity a,LinearLayout body,String title,String[] options,int selected,String tag,Pick callback){
        label(a,body,title);Spinner s=new Spinner(a);s.setTag(tag);s.setContentDescription(title);
        ArrayAdapter<String> adapter=new ArrayAdapter<>(a,android.R.layout.simple_spinner_item,options);adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);s.setAdapter(adapter);s.setSelection(selected);body.addView(s,new LinearLayout.LayoutParams(-1,dp(a,52)));
        s.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener(){public void onItemSelected(AdapterView<?> parent,View view,int pos,long id){callback.set(pos);}public void onNothingSelected(AdapterView<?> parent){}});
    }
}
