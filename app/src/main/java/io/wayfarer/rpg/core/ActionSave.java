package io.wayfarer.rpg.core;

import java.io.*;
import java.util.*;

/** Explicit versioned snapshot. The action game has its own app ID and save directory. */
public final class ActionSave {
    private ActionSave(){}
    public static byte[] encode(ActionGame g) throws IOException {
        Properties p=new Properties();p.setProperty("format","wayfarer-action");put(p,"version",2);
        p.setProperty("mode",g.mode.name());p.setProperty("rng",Long.toString(g.rngState));p.setProperty("won",Boolean.toString(g.won));p.setProperty("cleared",Boolean.toString(g.roomCleared));
        String[] ik={"hero","bank","power","vitality","runs","wins","room","loot","kills","potions","damageBonus","healthBonus","speedBonus","combo","nextId"};
        int[] iv={g.hero,g.bank,g.power,g.vitality,g.runs,g.wins,g.room,g.loot,g.kills,g.potions,g.damageBonus,g.healthBonus,g.speedBonus,g.combo,g.nextId};
        for(int i=0;i<ik.length;i++)put(p,ik[i],iv[i]);for(int i=0;i<3;i++)put(p,"xp"+i,g.experience[i]);
        put(p,"custom",g.custom);put(p,"shield",g.shield);put(p,"dodgeCharge",g.dodgeCharge);
        for(int i=0;i<3;i++){BuildSystem.Profile b=g.profiles[i];String k="profile"+i+".";p.setProperty(k+"name",b.name);p.setProperty(k+"data",csv(b.weapon,b.skill,b.origin,b.trait,b.color,b.skin,b.hair,b.xp));}
        for(int b=0;b<6;b++)for(int t=0;t<12;t++)put(p,"talent"+b+"."+t,g.talents[b][t]);
        String[] fk={"x","y","hp","faceX","faceY","time","attackCd","rollTime","rollCd","skillCd","invulnerable","comboWindow","rollX","rollY"};
        float[] fv={g.x,g.y,g.hp,g.faceX,g.faceY,g.time,g.attackCd,g.rollTime,g.rollCd,g.skillCd,g.invulnerable,g.comboWindow,g.rollX,g.rollY};
        for(int i=0;i<fk.length;i++)put(p,fk[i],fv[i]);
        put(p,"enemies",g.enemies.size());int i=0;
        for(ActionGame.Enemy e:g.enemies)p.setProperty("e"+i++,csv(e.id,e.type,e.state,e.x,e.y,e.hp,e.maxHp,e.timer,e.aimX,e.aimY,e.stun,e.flash,e.struck?1:0));
        put(p,"shots",g.shots.size());i=0;
        for(ActionGame.Shot s:g.shots)p.setProperty("s"+i++,csv(s.x,s.y,s.vx,s.vy,s.life,s.damage,s.radius,s.friendly?1:0));
        put(p,"blocks",g.blocks.size());i=0;
        for(ActionGame.Block b:g.blocks)p.setProperty("b"+i++,csv(b.x,b.y,b.w,b.h,b.style));
        put(p,"drops",g.drops.size());i=0;
        for(ActionGame.Drop d:g.drops)p.setProperty("d"+i++,csv(d.x,d.y,d.amount,d.heal?1:0));
        ByteArrayOutputStream out=new ByteArrayOutputStream();p.store(out,"Wayfarer Action save v2");return out.toByteArray();
    }
    private static void put(Properties p,String k,Number value){p.setProperty(k,value.toString());}
    private static String csv(Number... values){StringJoiner j=new StringJoiner(",");for(Number n:values)j.add(n.toString());return j.toString();}
    public static ActionGame decode(byte[] data) throws IOException {
        if(data==null||data.length==0||data.length>131072)throw new IOException("存档大小不正确");
        try{
            Properties p=new Properties();p.load(new ByteArrayInputStream(data));
            if(!"wayfarer-action".equals(p.getProperty("format")))throw new IllegalArgumentException("存档格式不兼容");int version=n(p,"version",1,2);
            ActionGame g=new ActionGame();g.mode=ActionGame.Mode.valueOf(required(p,"mode"));g.rngState=Long.parseLong(required(p,"rng"));if(g.rngState==0)throw new IllegalArgumentException("随机状态无效");
            g.won=bool(p,"won");g.roomCleared=bool(p,"cleared");g.hero=n(p,"hero",0,2);g.bank=n(p,"bank",0,999999);g.power=n(p,"power",0,10);g.vitality=n(p,"vitality",0,10);g.runs=n(p,"runs",0,999999);g.wins=n(p,"wins",0,g.runs);
            g.room=n(p,"room",0,5);g.loot=n(p,"loot",0,99999);g.kills=n(p,"kills",0,99999);g.potions=n(p,"potions",0,2);g.damageBonus=n(p,"damageBonus",0,16);g.healthBonus=n(p,"healthBonus",0,80);g.speedBonus=n(p,"speedBonus",0,4);g.combo=n(p,"combo",0,2);g.nextId=n(p,"nextId",1,Integer.MAX_VALUE-1);
            for(int i=0;i<3;i++)g.experience[i]=n(p,"xp"+i,0,1000000);
            // v1 migration: preserve every original field; new profiles and empty trees use defaults.
            if(version>=2){
                g.custom=n(p,"custom",-1,2);g.shield=f(p,"shield",0,24);g.dodgeCharge=f(p,"dodgeCharge",0,3);
                for(int i=0;i<3;i++){BuildSystem.Profile b=g.profiles[i];String k="profile"+i+".";b.name=required(p,k+"name");float[] a=array(p,k+"data",8);
                    b.weapon=integer(a[0],0,2);b.skill=integer(a[1],0,2);b.origin=integer(a[2],0,2);b.trait=integer(a[3],0,2);b.color=integer(a[4],0,5);b.skin=integer(a[5],0,2);b.hair=integer(a[6],0,2);b.xp=integer(a[7],0,1000000);BuildSystem.validate(b);}
                for(int b=0;b<6;b++){for(int t=0;t<12;t++)g.talents[b][t]=n(p,"talent"+b+"."+t,0,BuildSystem.NODES[t].maxRank);BuildSystem.validateTree(g.talents[b],b<3?g.experience[b]:g.profiles[b-3].xp);}
                if(g.custom>=0&&g.hero!=g.profile().weapon)throw new IllegalArgumentException("角色与武器不一致");
                if(g.shield>g.rank(9)*6+(g.rank(11)>0?12:0)||g.dodgeCharge>0&&g.rank(5)==0)throw new IllegalArgumentException("天赋状态无效");
            }
            g.x=f(p,"x",48,1072);g.y=f(p,"y",48,712);g.hp=f(p,"hp",0,g.maxHp());g.faceX=f(p,"faceX",-1,1);g.faceY=f(p,"faceY",-1,1);g.time=f(p,"time",0,10000000);
            g.attackCd=f(p,"attackCd",0,1);g.rollTime=f(p,"rollTime",0,.3f);g.rollCd=f(p,"rollCd",0,1.2f);g.skillCd=f(p,"skillCd",0,7);g.invulnerable=f(p,"invulnerable",0,1);g.comboWindow=f(p,"comboWindow",0,1);g.rollX=f(p,"rollX",-1,1);g.rollY=f(p,"rollY",-1,1);
            int count=n(p,"blocks",0,32);for(int i=0;i<count;i++){float[] a=array(p,"b"+i,5);g.blocks.add(new ActionGame.Block(range(a[0],48,1072),range(a[1],48,712),range(a[2],1,200),range(a[3],1,200),integer(a[4],0,1)));}
            count=n(p,"enemies",0,40);Set<Integer> ids=new HashSet<>();
            for(int i=0;i<count;i++){
                float[] a=array(p,"e"+i,13);int id=integer(a[0],1,g.nextId-1);if(!ids.add(id))throw new IllegalArgumentException("重复敌人ID");
                ActionGame.Enemy e=new ActionGame.Enemy(id,integer(a[1],0,3),range(a[3],48,1072),range(a[4],48,712),range(a[6],1,1000));
                e.hp=range(a[5],0,e.maxHp);e.state=integer(a[2],0,2);e.timer=range(a[7],-.1f,2);e.aimX=range(a[8],-1,1);e.aimY=range(a[9],-1,1);e.stun=range(a[10],0,1);e.flash=range(a[11],0,1);e.struck=integer(a[12],0,1)==1;
                if(!g.free(e.x,e.y,e.radius()))throw new IllegalArgumentException("敌人位置无效");g.enemies.add(e);
            }
            count=n(p,"shots",0,160);for(int i=0;i<count;i++){
                float[] a=array(p,"s"+i,8);ActionGame.Shot s=new ActionGame.Shot(range(a[0],0,1120),range(a[1],0,760),range(a[2],-1000,1000),range(a[3],-1000,1000),range(a[5],1,500),integer(a[7],0,1)==1);
                s.life=range(a[4],0,3);s.radius=range(a[6],1,10);g.shots.add(s);
            }
            count=n(p,"drops",0,80);for(int i=0;i<count;i++){float[] a=array(p,"d"+i,4);g.drops.add(new ActionGame.Drop(range(a[0],0,1120),range(a[1],0,760),integer(a[2],1,100),integer(a[3],0,1)==1));}
            if(!g.free(g.x,g.y,ActionGame.PLAYER_RADIUS))throw new IllegalArgumentException("角色位置无效");
            if((g.mode==ActionGame.Mode.CAMP&&g.room!=0)||(g.mode!=ActionGame.Mode.CAMP&&g.room<1)||(g.hp==0&&g.mode!=ActionGame.Mode.RESULT))throw new IllegalArgumentException("流程状态无效");
            if(g.roomCleared&&!g.enemies.isEmpty())throw new IllegalArgumentException("房间状态矛盾");
            if(g.mode==ActionGame.Mode.REWARD&&(!g.enemies.isEmpty()||g.room==5||g.roomCleared))throw new IllegalArgumentException("奖励状态矛盾");
            if(g.won&&(g.mode!=ActionGame.Mode.RESULT&&g.mode!=ActionGame.Mode.CAMP))throw new IllegalArgumentException("结算状态无效");
            g.settleRestoredAttack();g.notice="存档已恢复";return g;
        }catch(IllegalArgumentException|NullPointerException e){throw new IOException("存档校验失败："+e.getMessage(),e);}
    }
    private static String required(Properties p,String key){String s=p.getProperty(key);if(s==null)throw new IllegalArgumentException("缺少 "+key);return s;}
    private static int n(Properties p,String key,int lo,int hi){int v=Integer.parseInt(required(p,key));if(v<lo||v>hi)throw new IllegalArgumentException(key+" 超出范围");return v;}
    private static float f(Properties p,String key,float lo,float hi){return range(Float.parseFloat(required(p,key)),lo,hi);}
    private static float range(float v,float lo,float hi){if(!Float.isFinite(v)||v<lo-.0001f||v>hi+.0001f)throw new IllegalArgumentException("非法数值");return Math.max(lo,Math.min(v,hi));}
    private static int integer(float v,int lo,int hi){range(v,lo,hi);if(v!=(int)v)throw new IllegalArgumentException("应为整数");return (int)v;}
    private static float[] array(Properties p,String key,int count){String[] parts=required(p,key).split(",",-1);if(parts.length!=count)throw new IllegalArgumentException("记录长度错误");float[] out=new float[count];for(int i=0;i<count;i++){out[i]=Float.parseFloat(parts[i]);if(!Float.isFinite(out[i]))throw new IllegalArgumentException("无效数值");}return out;}
    private static boolean bool(Properties p,String key){String s=required(p,key);if(!s.equals("true")&&!s.equals("false"))throw new IllegalArgumentException("布尔值错误");return Boolean.parseBoolean(s);}
}
