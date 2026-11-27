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
        if(job==CareerSystem.TIMEKEEPER){
            if(kind==1)return "时序裁定：消耗40魂能，冷却16秒。对全场存活敌人造成3.0倍伤害并冻结2.5秒；同时获得22点辉盾，并净化自身附近420范围内的敌方弹幕。冻结时间固定，不受天赋延长。";
            return "终末回响：消耗40魂能、最大生命的8%与30点侵蚀，冷却20秒。对全场存活敌人施加8秒诅咒，造成3.6倍伤害并冻结2.5秒；时针终阶天赋可提高技能伤害，但不延长冻结时间。";
        }
        if(kind==1)switch(job){
            case CareerSystem.HUNTER:return "曙光猎令：消耗40魂能，冷却12秒。获得14点辉盾，为全场存活敌人添加7秒线索标记，并对自动锁定的首领优先目标造成3.1倍伤害与短暂硬直。";
            case CareerSystem.ASSASSIN:return "无影圣步：消耗40魂能，冷却10秒。获得约1.1秒无敌并净化自身周围180范围的敌方弹幕；向瞄准方向突进，对路径敌人造成2.25倍伤害，按命中数量获得辉盾。";
            case CareerSystem.DETECTIVE:return "真相显影：消耗40魂能，冷却14秒。为全场敌人补全8秒线索标记，普通敌人受到约0.75秒硬直、首领受到短暂硬直；同时获得16点辉盾并净化自身周围320范围内的敌方弹幕。";
            default:return "光辉头条：消耗40魂能，冷却12秒。对当前生命值最高的敌人造成2.4倍伤害，再对其附近最近的一名敌人造成0.9倍伤害；同时获得10点辉盾并恢复3%最大生命。";
        }
        switch(job){
            case CareerSystem.HUNTER:return "血猎追魂：消耗40魂能、最大生命的8%与30点侵蚀，冷却14秒。优先锁定首领并施加9秒标记；目标生命低于35%时造成4.1倍伤害，否则造成3.3倍伤害。击杀目标额外恢复6%最大生命。";
            case CareerSystem.ASSASSIN:return "蚀影处决：消耗40魂能、最大生命的8%与30点侵蚀，冷却16秒。向瞄准方向突进，对路径敌人施加6秒标记并造成2.8倍伤害；命中后按命中数量恢复生命，最多恢复最大生命的5%，并额外增加8点侵蚀。";
            case CareerSystem.DETECTIVE:return "深渊审讯：消耗40魂能、最大生命的8%与30点侵蚀，冷却18秒。对全场已有线索标记的目标按标记剩余时间提高审判伤害，并造成硬直；若没有已标记目标，则先为敌人建立初步线索并造成0.9倍伤害。";
            default:return "血色头条：消耗40魂能、最大生命的10%与38点侵蚀，冷却20秒。为全场敌人添加6秒标记并造成1.25倍伤害；随后每名存活敌人会向其附近最近的另一名敌人传导0.35倍伤害，形成连锁传播。";
        }
    }
    public static AlertDialog show(MainActivity a){
        ActionGame g=a.world.game;int[] draft={g.soulKind(),g.soulArt()};
        LinearLayout row=new LinearLayout(a);row.setOrientation(LinearLayout.HORIZONTAL);row.setPadding(dp(a,18),0,dp(a,18),0);
        LinearLayout left=new LinearLayout(a),right=new LinearLayout(a);left.setOrientation(LinearLayout.VERTICAL);right.setOrientation(LinearLayout.VERTICAL);right.setPadding(dp(a,20),0,0,0);
        ScrollView ls=new ScrollView(a),rs=new ScrollView(a);ls.addView(left);rs.addView(right);row.addView(ls,new LinearLayout.LayoutParams(0,-1,1));row.addView(rs,new LinearLayout.LayoutParams(0,-1,1));
        text(a,left,"灵魂归属 · 当前角色独立保存",14);Spinner kind=new Spinner(a);kind.setTag("soul-kind");kind.setContentDescription("灵魂归属");kind.setAdapter(adapter(a,SoulSystem.NAMES));kind.setSelection(draft[0]);left.addView(kind,new LinearLayout.LayoutParams(-1,dp(a,48)));
        TextView passive=text(a,left,"",14),variant=text(a,left,"",14);
        text(a,right,"职业专属灵魂技能 · 独立于职业主动技能，自动匹配当前职业",14);TextView form=text(a,right,"",18),detail=text(a,right,"",14);
        text(a,right,"每轮初始40魂能，最多100。有效攻击积累魂能；光辉护盾吸收伤害也可充能。专属魂技与其伤害不能回充。\\n\\n光辉偏向防护、控制与稳定收益；禁忌以生命和侵蚀换取爆发。更换只在营地生效，取消不修改。",13);
        Runnable refresh=()->{passive.setText(SoulSystem.PASSIVES[draft[0]]);variant.setText("当前通用主动技（独立系统）\n"+SoulSystem.DETAILS[draft[0]][g.skillStyle()]);form.setText(formName(draft[0],g.job()));detail.setText(formDetail(draft[0],g.job()));};
        kind.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener(){public void onItemSelected(AdapterView<?> p,View v,int pos,long id){if(pos!=draft[0]){draft[0]=pos;draft[1]=0;}refresh.run();}public void onNothingSelected(AdapterView<?> p){}});
        AlertDialog dialog=new AlertDialog.Builder(a).setTitle(g.heroName()+" · 灵魂祭坛").setView(row).setPositiveButton("确认缔结",(d,w)->{if(g.bindSoul(draft[0],draft[1])){a.save();a.world.invalidate();}}).setNegativeButton("取消",null).create();
        dialog.setOnShowListener(d->{Window window=dialog.getWindow();if(window!=null)window.setLayout((int)(a.getResources().getDisplayMetrics().widthPixels*.9f),(int)(a.getResources().getDisplayMetrics().heightPixels*.94f));refresh.run();});dialog.show();return dialog;
    }
}
