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
        LinearLayout layout=new LinearLayout(a);layout.setOrientation(LinearLayout.HORIZONTAL);layout.setPadding(dp(a,12),dp(a,8),dp(a,12),dp(a,8));
        LinearLayout left=new LinearLayout(a);left.setOrientation(LinearLayout.VERTICAL);left.setPadding(dp(a,8),dp(a,6),dp(a,12),dp(a,6));
        LinearLayout body=new LinearLayout(a);body.setOrientation(LinearLayout.VERTICAL);body.setPadding(dp(a,16),0,dp(a,8),dp(a,8));
        ScrollView scroll=new ScrollView(a);scroll.setFillViewport(true);scroll.addView(body);scroll.setScrollbarFadingEnabled(false);
        ScrollView leftScroll=new ScrollView(a);leftScroll.addView(left);
        layout.addView(leftScroll,new LinearLayout.LayoutParams(0,-1,.4f));layout.addView(scroll,new LinearLayout.LayoutParams(0,-1,.6f));
        TextView summary=new TextView(a);summary.setTextSize(12);summary.setTextColor(Color.rgb(230,226,202));summary.setPadding(dp(a,8),dp(a,8),dp(a,8),dp(a,8));summary.setBackgroundColor(0xFF183235);
        View preview=new View(a){@Override protected void onDraw(Canvas c){super.onDraw(c);c.drawColor(0xFF183235);a.world.drawPortrait(c,getWidth()/2f,getHeight()*.78f,Math.min(getWidth()/64f,getHeight()/60f),draft);}};
        left.addView(preview,new LinearLayout.LayoutParams(-1,dp(a,90)));
        Runnable refresh=()->{ActionGame sample=new ActionGame();sample.profiles[0]=draft.copy();sample.selectCustom(0);
            summary.setText("生命 "+(int)sample.maxHp()+" · 伤害 "+(int)sample.damage()+" · 移速 "+(int)sample.speed()+"\n技能 "+String.format(java.util.Locale.ROOT,"%.1f",sample.skillPeriod())+"秒 · 闪避 "+String.format(java.util.Locale.ROOT,"%.2f",sample.dodgePeriod())+"秒\n"+SoulSystem.NAMES[draft.soul]+" · "+sample.skillName()+"\n含角色等级\n不含营地、天赋与圣遗物");preview.invalidate();};
        left.addView(summary);label(a,left,"名字 · 1至10个字符");EditText name=new EditText(a);name.setSingleLine(true);name.setTextSize(16);name.setFilters(new InputFilter[]{new InputFilter.LengthFilter(20)});name.setText(draft.name);name.setContentDescription("自定义角色名字");name.setTag("profile-name");left.addView(name);
        name.addTextChangedListener(new TextWatcher(){public void beforeTextChanged(CharSequence s,int st,int count,int after){}public void onTextChanged(CharSequence s,int st,int before,int count){draft.name=s.toString();refresh.run();}public void afterTextChanged(Editable e){}});
        choose(a,body,"武器 · 决定普攻",BuildSystem.WEAPONS,draft.weapon,"profile-weapon",v->{draft.weapon=v;refresh.run();});
        choose(a,body,"主动技能 · 可跨武器搭配",BuildSystem.SKILLS,draft.skill,"profile-skill",v->{draft.skill=v;refresh.run();});
        Spinner[] soulArtPicker={null};
        choose(a,body,"灵魂归属 · 祭坛可查看完整效果",SoulSystem.NAMES,draft.soul,"profile-soul",v->{
            if(draft.soul!=v){draft.soul=v;draft.soulArt=0;if(soulArtPicker[0]!=null){ArrayAdapter<String> ad=new ArrayAdapter<>(a,android.R.layout.simple_spinner_item,SoulSystem.ARTS[v]);ad.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);soulArtPicker[0].setAdapter(ad);soulArtPicker[0].setSelection(0);}}
            if(soulArtPicker[0]!=null)soulArtPicker[0].setEnabled(v!=0);refresh.run();
        });
        soulArtPicker[0]=choose(a,body,"专属技能",SoulSystem.ARTS[draft.soul],draft.soulArt,"profile-soul-art",v->{draft.soulArt=draft.soul==0?0:v;refresh.run();});soulArtPicker[0].setEnabled(draft.soul!=0);
        choose(a,body,"出身",BuildSystem.ORIGINS,draft.origin,"profile-origin",v->{draft.origin=v;refresh.run();});
        choose(a,body,"特质",BuildSystem.TRAITS,draft.trait,"profile-trait",v->{draft.trait=v;refresh.run();});
        choose(a,body,"衣装颜色",BuildSystem.COLORS,draft.color,"profile-color",v->{draft.color=v;refresh.run();});
        choose(a,body,"肤色",new String[]{"浅肤色","棕肤色","深肤色"},draft.skin,"profile-skin",v->{draft.skin=v;refresh.run();});
        choose(a,body,"发型",BuildSystem.HAIR,draft.hair,"profile-hair",v->{draft.hair=v;refresh.run();});
        label(a,body,"改造免费，保留本角色经验与天赋。只能在营地编辑。取消不会改变角色。");
        AlertDialog dialog=new AlertDialog.Builder(a).setTitle("角色工坊 · 槽位 "+(slot+1)).setView(layout).setPositiveButton("保存并选用",null).setNegativeButton("取消",null).create();
        dialog.setOnShowListener(d->{dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v->{
            try{g.customize(slot,draft);a.world.customRoster=true;a.save();a.world.invalidate();dialog.dismiss();}
            catch(IllegalArgumentException e){name.setError(e.getMessage());name.requestFocus();}
        });Window w=dialog.getWindow();if(w!=null){w.setLayout((int)(a.getResources().getDisplayMetrics().widthPixels*.88f),(int)(a.getResources().getDisplayMetrics().heightPixels*.94f));w.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);}refresh.run();});
        dialog.show();return dialog;
    }
    private static int dp(MainActivity a,int value){return Math.round(value*a.getResources().getDisplayMetrics().density);}
    private interface Pick {void set(int value);}
    private static void label(MainActivity a,LinearLayout body,String text){TextView t=new TextView(a);t.setText(text);t.setTextSize(13);t.setPadding(0,dp(a,8),0,dp(a,2));body.addView(t);}
    private static Spinner choose(MainActivity a,LinearLayout body,String title,String[] options,int selected,String tag,Pick callback){
        label(a,body,title);Spinner s=new Spinner(a);s.setTag(tag);s.setContentDescription(title);
        ArrayAdapter<String> adapter=new ArrayAdapter<String>(a,android.R.layout.simple_spinner_item,options){
            @Override public View getView(int position,View recycled,ViewGroup parent){TextView t=(TextView)super.getView(position,recycled,parent);t.setTextSize(14);t.setSingleLine(false);t.setMaxLines(2);return t;}
            @Override public View getDropDownView(int position,View recycled,ViewGroup parent){TextView t=(TextView)super.getDropDownView(position,recycled,parent);t.setTextSize(14);t.setSingleLine(false);t.setMaxLines(2);t.setPadding(dp(a,12),dp(a,10),dp(a,12),dp(a,10));return t;}
        };adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);s.setAdapter(adapter);s.setSelection(selected);body.addView(s,new LinearLayout.LayoutParams(-1,dp(a,52)));
        s.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener(){public void onItemSelected(AdapterView<?> parent,View view,int pos,long id){callback.set(pos);}public void onNothingSelected(AdapterView<?> parent){}});return s;
    }
}
