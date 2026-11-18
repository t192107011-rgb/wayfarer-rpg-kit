package io.wayfarer.rpg.core;

import java.io.*;
import java.util.*;

/** Explicit versioned snapshot. The action game has its own app ID and save directory. */
public final class ActionSave {
    private ActionSave(){}
    public static byte[] encode(ActionGame g) throws IOException {
        Properties p=new Properties();p.setProperty("format","wayfarer-action");put(p,"version",10);
        p.setProperty("endless",Boolean.toString(g.endless));put(p,"endlessDepth",g.endlessDepth);put(p,"bestDepth",g.bestDepth);put(p,"threat",g.threat);put(p,"pressure",g.pressure);put(p,"encounterHpFloor",g.encounterHpFloor);put(p,"encounterDamageFloor",g.encounterDamageFloor);put(p,"roomDamage",g.roomDamage);put(p,"roomElapsed",g.roomElapsed);
        p.setProperty("mode",g.mode.name());p.setProperty("rng",Long.toString(g.rngState));p.setProperty("won",Boolean.toString(g.won));p.setProperty("cleared",Boolean.toString(g.roomCleared));
        String[] ik={"hero","bank","power","vitality","runs","wins","room","loot","kills","potions","damageBonus","healthBonus","speedBonus","combo","nextId"};
        int[] iv={g.hero,g.bank,g.power,g.vitality,g.runs,g.wins,g.room,g.loot,g.kills,g.potions,g.damageBonus,g.healthBonus,g.speedBonus,g.combo,g.nextId};
        for(int i=0;i<ik.length;i++)put(p,ik[i],iv[i]);for(int i=0;i<3;i++)put(p,"xp"+i,g.experience[i]);
        for(int b=0;b<6;b++)for(int slot=0;slot<3;slot++)put(p,"career"+b+"."+slot,g.careers[b][slot]);
        for(int b=0;b<6;b++)p.setProperty("careerPact"+b,Boolean.toString(g.careerPacts[b]));
        put(p,"pactCd",g.pactCd);put(p,"pactTime",g.pactTime);put(p,"pactWard",g.pactWard);
        put(p,"careerWard",g.careerWard);put(p,"careerCharge",g.careerCharge);
        put(p,"relicOwned",g.relicOwned);put(p,"relicAwards",g.relicAwards);put(p,"lastRelic",g.lastRelic);put(p,"relicEcho",g.relicEcho);p.setProperty("relicRevived",Boolean.toString(g.relicRevived));
        for(int b=0;b<6;b++)for(int slot=0;slot<2;slot++)put(p,"relic"+b+"."+slot,g.relics[b][slot]);
        put(p,"followUpWindow",g.followUpWindow);put(p,"ruptureTime",g.ruptureTime);put(p,"ruptureTick",g.ruptureTick);p.setProperty("ruptureLatched",Boolean.toString(g.ruptureLatched));
        put(p,"storyUnlocked",g.storyUnlocked);for(int j=0;j<5;j++)put(p,"storyChoice"+j,g.storyChoices[j]);
        for(int j=0;j<6;j++){put(p,"characterUnlocked"+j,g.characterUnlocked[j]);put(p,"characterJob"+j,g.characterJobs[j]);for(int k=0;k<3;k++)put(p,"characterChoice"+j+"."+k,g.characterChoices[j][k]);}
        put(p,"hazards",g.hazards.size());int hz=0;for(ActionGame.Hazard h:g.hazards)p.setProperty("hazard"+hz++,csv(h.type,h.x,h.y,h.radius,h.warning,h.life,h.tick,h.damage));
        put(p,"custom",g.custom);put(p,"shield",g.shield);put(p,"dodgeCharge",g.dodgeCharge);
        for(int i=0;i<3;i++){BuildSystem.Profile b=g.profiles[i];String k="profile"+i+".";p.setProperty(k+"name",b.name);p.setProperty(k+"data",csv(b.weapon,b.skill,b.origin,b.trait,b.color,b.skin,b.hair,b.xp));}
        for(int b=0;b<6;b++)for(int t=0;t<12;t++)put(p,"talent"+b+"."+t,g.talents[b][t]);
        for(int j=0;j<3;j++){put(p,"presetSoul"+j,g.presetSouls[j]);put(p,"presetArt"+j,g.presetArts[j]);put(p,"profile"+j+".soul",g.profiles[j].soul);put(p,"profile"+j+".art",g.profiles[j].soulArt);}
        put(p,"soulEnergy",g.soulEnergy);put(p,"soulCd",g.soulCd);put(p,"soulWard",g.soulWard);put(p,"corruption",g.corruption);put(p,"soulQuiet",g.soulQuiet);put(p,"soulBuff",g.soulBuff);
        put(p,"soulFields",g.soulFields.size());int fieldIndex=0;for(ActionGame.SoulField f:g.soulFields)p.setProperty("field"+fieldIndex++,csv(f.type,f.x,f.y,f.life,f.tick,f.power));
        String[] fk={"x","y","hp","faceX","faceY","time","attackCd","rollTime","rollCd","skillCd","invulnerable","comboWindow","rollX","rollY"};
        float[] fv={g.x,g.y,g.hp,g.faceX,g.faceY,g.time,g.attackCd,g.rollTime,g.rollCd,g.skillCd,g.invulnerable,g.comboWindow,g.rollX,g.rollY};
        for(int i=0;i<fk.length;i++)put(p,fk[i],fv[i]);
        put(p,"enemies",g.enemies.size());int i=0;
        for(ActionGame.Enemy e:g.enemies){p.setProperty("enemyTactics"+i,csv(e.guard,e.targetX,e.targetY,e.pattern));p.setProperty("pactEnemy"+i,csv(e.pactMark,e.pactTick,e.pactStacks,e.pactDebt));put(p,"hex"+i,e.hex);p.setProperty("e"+i++,csv(e.id,e.type,e.state,e.x,e.y,e.hp,e.maxHp,e.timer,e.aimX,e.aimY,e.stun,e.flash,e.struck?1:0));}
        put(p,"shots",g.shots.size());i=0;
        for(ActionGame.Shot s:g.shots){put(p,"shotEffect"+i,s.effect);put(p,"shotPierce"+i,s.pierce);StringJoiner hits=new StringJoiner(",");for(int id:s.hitIds)hits.add(Integer.toString(id));p.setProperty("shotHits"+i,hits.toString());p.setProperty("s"+i++,csv(s.x,s.y,s.vx,s.vy,s.life,s.damage,s.radius,s.friendly?1:0));}
        put(p,"blocks",g.blocks.size());i=0;
        for(ActionGame.Block b:g.blocks)p.setProperty("b"+i++,csv(b.x,b.y,b.w,b.h,b.style));
        put(p,"drops",g.drops.size());i=0;
        for(ActionGame.Drop d:g.drops){p.setProperty("dropPotion"+i,Boolean.toString(d.potion));p.setProperty("d"+i++,csv(d.x,d.y,d.amount,d.heal?1:0));}
        ByteArrayOutputStream out=new ByteArrayOutputStream();p.store(out,"Wayfarer Action save v10");return out.toByteArray();
    }
    private static void put(Properties p,String k,Number value){p.setProperty(k,value.toString());}
    private static String csv(Number... values){StringJoiner j=new StringJoiner(",");for(Number n:values)j.add(n.toString());return j.toString();}
    public static ActionGame decode(byte[] data) throws IOException {
        if(data==null||data.length==0||data.length>131072)throw new IOException("存档大小不正确");
        try{
            Properties p=new Properties();p.load(new ByteArrayInputStream(data));
            if(!"wayfarer-action".equals(p.getProperty("format")))throw new IllegalArgumentException("存档格式不兼容");int version=n(p,"version",1,10);
            ActionGame g=new ActionGame();g.mode=ActionGame.Mode.valueOf(required(p,"mode"));g.rngState=Long.parseLong(required(p,"rng"));if(g.rngState==0)throw new IllegalArgumentException("随机状态无效");
            g.won=bool(p,"won");g.roomCleared=bool(p,"cleared");g.hero=n(p,"hero",0,2);g.bank=n(p,"bank",0,999999);g.power=n(p,"power",0,10);g.vitality=n(p,"vitality",0,10);g.runs=n(p,"runs",0,999999);g.wins=n(p,"wins",0,g.runs);
            g.room=n(p,"room",0,5);g.loot=n(p,"loot",0,99999);g.kills=n(p,"kills",0,99999);g.potions=n(p,"potions",0,version>=10?ActionGame.MAX_POTIONS:2);g.damageBonus=n(p,"damageBonus",0,version>=10?40:16);g.healthBonus=n(p,"healthBonus",0,version>=10?200:80);g.speedBonus=n(p,"speedBonus",0,version>=10?6:4);g.combo=n(p,"combo",0,2);g.nextId=n(p,"nextId",1,Integer.MAX_VALUE-1);
            if(version>=10){
                g.endless=bool(p,"endless");g.endlessDepth=n(p,"endlessDepth",0,999999);g.bestDepth=n(p,"bestDepth",0,999999);
                g.threat=f(p,"threat",1,6);g.pressure=f(p,"pressure",0,.6f);g.encounterHpFloor=f(p,"encounterHpFloor",0,2000);g.encounterDamageFloor=f(p,"encounterDamageFloor",0,200);g.roomDamage=f(p,"roomDamage",0,100000);g.roomElapsed=f(p,"roomElapsed",0,10000000);
                if(g.endless&&(g.endlessDepth<1||g.mode==ActionGame.Mode.CAMP||g.won||g.room!=(g.endlessDepth-1)%5+1)||!g.endless&&g.endlessDepth!=0||(g.encounterHpFloor==0)!=(g.encounterDamageFloor==0))throw new IllegalArgumentException("无尽或敌势来源无效");
                if(g.mode==ActionGame.Mode.CAMP&&(g.threat!=1||g.pressure>0||g.encounterHpFloor>0||g.encounterDamageFloor>0||g.roomDamage>0||g.roomElapsed>0))throw new IllegalArgumentException("营地敌势状态无效");
            }
            for(int i=0;i<3;i++)g.experience[i]=n(p,"xp"+i,0,1000000);
            // v1 migration: preserve every original field; new profiles and empty trees use defaults.
            if(version>=2){
                g.custom=n(p,"custom",-1,2);g.shield=f(p,"shield",0,24);g.dodgeCharge=f(p,"dodgeCharge",0,3);
                for(int i=0;i<3;i++){BuildSystem.Profile b=g.profiles[i];String k="profile"+i+".";b.name=required(p,k+"name");float[] a=array(p,k+"data",8);
                    b.weapon=integer(a[0],0,2);b.skill=integer(a[1],0,2);b.origin=integer(a[2],0,2);b.trait=integer(a[3],0,2);b.color=integer(a[4],0,5);b.skin=integer(a[5],0,2);b.hair=integer(a[6],0,2);b.xp=integer(a[7],0,1000000);BuildSystem.validate(b);}
                for(int b=0;b<6;b++){for(int t=0;t<12;t++)g.talents[b][t]=n(p,"talent"+b+"."+t,0,BuildSystem.NODES[t].maxRank);if(version>=10)BuildSystem.validateTree(g.talents[b],b<3?g.experience[b]:g.profiles[b-3].xp);else BuildSystem.validateLegacyTree(g.talents[b],b<3?g.experience[b]:g.profiles[b-3].xp);}
                if(g.custom>=0&&g.hero!=g.profile().weapon)throw new IllegalArgumentException("角色与武器不一致");
                if(g.shield>g.rank(9)*6+(g.rank(11)>0?12:0)||g.dodgeCharge>0&&g.rank(5)==0)throw new IllegalArgumentException("天赋状态无效");
            }
            // v1/v2 migration leaves every soul unbound and adds no combat debt or resource.
            if(version>=3){
                for(int j=0;j<3;j++){
                    g.presetSouls[j]=n(p,"presetSoul"+j,0,2);g.presetArts[j]=n(p,"presetArt"+j,0,1);SoulSystem.validate(g.presetSouls[j],g.presetArts[j]);
                    g.profiles[j].soul=n(p,"profile"+j+".soul",0,2);g.profiles[j].soulArt=n(p,"profile"+j+".art",0,1);BuildSystem.validate(g.profiles[j]);
                }
                g.soulEnergy=f(p,"soulEnergy",0,100);g.soulCd=f(p,"soulCd",0,SoulSystem.COOLDOWNS[g.soulKind()][g.soulArt()]);g.soulWard=f(p,"soulWard",0,30);g.corruption=f(p,"corruption",0,100);g.soulQuiet=f(p,"soulQuiet",0,version>=9?8:3);g.soulBuff=f(p,"soulBuff",0,6);
                int fields=n(p,"soulFields",0,1);for(int j=0;j<fields;j++){float[] a=array(p,"field"+j,6);int type=integer(a[0],1,2);ActionGame.SoulField f=new ActionGame.SoulField(type,range(a[1],48,1072),range(a[2],48,712),range(a[5],1,300));f.life=range(a[3],0,type==1?5:4);f.tick=range(a[4],0,type==1?1:.5f);
                    if(type!=g.soulKind()||g.soulArt()!=0||f.life<=0)throw new IllegalArgumentException("领域与灵魂配置不符");g.soulFields.add(f);}
                if(g.soulKind()!=1&&g.soulWard>0||g.soulKind()!=2&&(g.corruption>0||g.soulQuiet>0||g.soulBuff>0)||g.soulBuff>0&&g.soulArt()!=1||g.soulKind()==0&&g.soulEnergy>0)throw new IllegalArgumentException("灵魂状态无效");
                if(g.mode==ActionGame.Mode.CAMP&&(g.soulEnergy>0||g.soulCd>0||g.soulWard>0||g.corruption>0||g.soulQuiet>0||g.soulBuff>0||fields>0))throw new IllegalArgumentException("营地灵魂状态无效");
            }
            // Older saves receive starter collection but empty loadouts: no retroactive combat bonuses.
            if(version>=4){
                g.relicOwned=n(p,"relicOwned",0,RelicSystem.ALL);
                if((g.relicOwned&~RelicSystem.ALL)!=0||(g.relicOwned&RelicSystem.STARTERS)!=RelicSystem.STARTERS)throw new IllegalArgumentException("圣遗物收藏无效");
                for(int b=0;b<6;b++){
                    for(int slot=0;slot<2;slot++)g.relics[b][slot]=n(p,"relic"+b+"."+slot,0,8);
                    String error=RelicSystem.invalid(g.relicOwned,g.relics[b][0],g.relics[b][1]);if(!error.isEmpty())throw new IllegalArgumentException(error);
                }
                g.relicAwards=n(p,"relicAwards",0,RelicSystem.DISCOVERY_ROOMS);g.lastRelic=n(p,"lastRelic",-1,8);g.relicEcho=f(p,"relicEcho",0,3);g.relicRevived=bool(p,"relicRevived");
                if((g.relicAwards&~RelicSystem.DISCOVERY_ROOMS)!=0||(g.relicAwards&~((1<<(g.endless?Math.min(5,g.endlessDepth):g.room))-1))!=0)throw new IllegalArgumentException("圣遗物发现进度无效");
                if(g.lastRelic!=0&&(g.room<1||(g.relicAwards&(1<<(g.room-1)))==0||!RelicSystem.owns(g.relicOwned,g.lastRelic<0?0:g.lastRelic)||g.lastRelic==1||g.lastRelic==2||g.lastRelic==-1&&g.relicOwned!=RelicSystem.ALL))throw new IllegalArgumentException("圣遗物发现记录无效");
                if(g.relicEcho>0&&!g.hasRelic(3)||g.relicRevived&&!g.hasRelic(8))throw new IllegalArgumentException("圣遗物触发状态无效");
                if(g.mode==ActionGame.Mode.CAMP&&(g.relicAwards!=0||g.lastRelic!=0||g.relicEcho>0||g.relicRevived))throw new IllegalArgumentException("营地圣遗物状态无效");
            }
            // v1-v4: keep all original combat values; opt in to careers in camp.
            if(version>=5){
                for(int b=0;b<6;b++){
                    g.careers[b][0]=n(p,"career"+b+".0",0,version>=6?CareerSystem.CLASSES.length-1:3);g.careers[b][1]=n(p,"career"+b+".1",0,2);g.careers[b][2]=n(p,"career"+b+".2",0,6);
                    String error=CareerSystem.invalid(g.careers[b][0],g.careers[b][1],g.careers[b][2],b<3?g.experience[b]:g.profiles[b-3].xp);if(!error.isEmpty())throw new IllegalArgumentException(error);
                }
                g.careerWard=f(p,"careerWard",0,g.careerWardLimit());g.careerCharge=f(p,"careerCharge",0,g.careerChargeLimit());
                if(g.mode==ActionGame.Mode.CAMP&&(g.careerWard>0||g.careerCharge>0))throw new IllegalArgumentException("营地职能状态无效");
                if(g.soulCd>g.soulPeriod()+.0001f)throw new IllegalArgumentException("魂技冷却与专精不符");
            }
            // v1-v6 opt out of occupational forbidden arts; existing souls remain unchanged.
            if(version>=7){
                for(int b=0;b<6;b++){g.careerPacts[b]=bool(p,"careerPact"+b);int xp=b<3?g.experience[b]:g.profiles[b-3].xp;if(g.careerPacts[b]&&(!ProfessionPacts.available(g.careers[b][0])||xp<CareerSystem.SPECIALIZATION_XP))throw new IllegalArgumentException("职业禁术归属或等级无效");}
                g.pactCd=f(p,"pactCd",0,g.pactEnabled()?ProfessionPacts.COOLDOWN[g.job()]:0);g.pactTime=f(p,"pactTime",0,g.pactEnabled()?ProfessionPacts.DURATION[g.job()]:0);
                g.pactWard=f(p,"pactWard",0,g.pactActive(CareerSystem.MINER)?35:0);
                if(g.pactTime>g.pactCd+.0001f||g.pactActive(CareerSystem.BOXER)&&g.hero==1||g.mode==ActionGame.Mode.CAMP&&(g.pactCd>0||g.pactTime>0||g.pactWard>0))throw new IllegalArgumentException("职业禁术状态无效");
            }
            if(version>=9){
                g.followUpWindow=f(p,"followUpWindow",0,version>=10?g.followUpDuration():2);g.ruptureTime=f(p,"ruptureTime",0,6);g.ruptureTick=f(p,"ruptureTick",0,1.5f);g.ruptureLatched=bool(p,"ruptureLatched");
                if(g.soulKind()!=2&&(g.ruptureTime>0||g.ruptureTick>0||g.ruptureLatched)||g.ruptureTime>0&&!g.ruptureLatched||g.ruptureTime==0&&g.ruptureTick>0)throw new IllegalArgumentException("侵蚀失控状态无效");
                g.storyUnlocked=n(p,"storyUnlocked",0,31);for(int j=0;j<5;j++){g.storyChoices[j]=n(p,"storyChoice"+j,0,2);if(g.storyChoices[j]>0&&(g.storyUnlocked&(1<<j))==0)throw new IllegalArgumentException("主线选择未解锁");}
                for(int j=0;j<6;j++){g.characterUnlocked[j]=n(p,"characterUnlocked"+j,0,7);g.characterJobs[j]=n(p,"characterJob"+j,-1,11);
                    if(g.characterUnlocked[j]!=0&&g.characterUnlocked[j]!=1&&g.characterUnlocked[j]!=3&&g.characterUnlocked[j]!=7||(g.characterUnlocked[j]==0)!=(g.characterJobs[j]==-1))throw new IllegalArgumentException("人物线来源无效");
                    for(int k=0;k<3;k++){g.characterChoices[j][k]=n(p,"characterChoice"+j+"."+k,0,2);if(g.characterChoices[j][k]>0&&(g.characterUnlocked[j]&(1<<k))==0)throw new IllegalArgumentException("人物选择未解锁");}
                }
                int zones=n(p,"hazards",0,18);for(int j=0;j<zones;j++){float[] a=array(p,"hazard"+j,8);int type=integer(a[0],0,1);ActionGame.Hazard h=new ActionGame.Hazard(type,range(a[1],64,1056),range(a[2],64,696),range(a[3],1,version>=10?300:190),range(a[4],0,1.5f),range(a[5],0,type==0?2.3f:.1f),range(a[7],1,version>=10?300:60));h.tick=range(a[6],0,.65f);if(h.life<=0)throw new IllegalArgumentException("已结束的危险区");g.hazards.add(h);}
                if(g.mode==ActionGame.Mode.CAMP&&(g.followUpWindow>0||g.ruptureTime>0||g.ruptureLatched||zones>0)||zones>0&&(g.mode==ActionGame.Mode.REWARD||g.roomCleared))throw new IllegalArgumentException("战斗扩展状态无效");
            }
            g.x=f(p,"x",48,1072);g.y=f(p,"y",48,712);g.hp=f(p,"hp",0,g.maxHp());g.faceX=f(p,"faceX",-1,1);g.faceY=f(p,"faceY",-1,1);g.time=f(p,"time",0,10000000);
            g.attackCd=f(p,"attackCd",0,1);g.rollTime=f(p,"rollTime",0,.3f);g.rollCd=f(p,"rollCd",0,1.2f);g.skillCd=f(p,"skillCd",0,7.5f);g.invulnerable=f(p,"invulnerable",0,1);g.comboWindow=f(p,"comboWindow",0,1);g.rollX=f(p,"rollX",-1,1);g.rollY=f(p,"rollY",-1,1);
            int count=n(p,"blocks",0,32);for(int i=0;i<count;i++){float[] a=array(p,"b"+i,5);g.blocks.add(new ActionGame.Block(range(a[0],48,1072),range(a[1],48,712),range(a[2],1,200),range(a[3],1,200),integer(a[4],0,1)));}
            count=n(p,"enemies",0,40);Set<Integer> ids=new HashSet<>();
            for(int i=0;i<count;i++){
                float[] a=array(p,"e"+i,13);int id=integer(a[0],1,g.nextId-1);if(!ids.add(id))throw new IllegalArgumentException("重复敌人ID");
                ActionGame.Enemy e=new ActionGame.Enemy(id,integer(a[1],0,version>=9?6:3),range(a[3],48,1072),range(a[4],48,712),range(a[6],1,version>=10?12000:1000));
                e.hp=range(a[5],0,e.maxHp);e.state=integer(a[2],0,version>=9?3:2);e.timer=range(a[7],-.1f,2);e.aimX=range(a[8],-1,1);e.aimY=range(a[9],-1,1);e.stun=range(a[10],0,1);e.flash=range(a[11],0,1);e.struck=integer(a[12],0,1)==1;
                if(version>=9){float[] t=array(p,"enemyTactics"+i,4);e.guard=range(t[0],0,40);e.targetX=range(t[1],64,1056);e.targetY=range(t[2],64,696);e.pattern=integer(t[3],0,e.type==3?2:0);if(e.state==3&&e.type!=2&&e.type!=4)throw new IllegalArgumentException("敌人恢复状态无效");}
                if(version>=3)e.hex=f(p,"hex"+i,0,3);
                if(version>=8){float[] pm=array(p,"pactEnemy"+i,4);e.pactMark=range(pm[0],0,g.pactTime);e.pactTick=range(pm[1],0,g.job()==CareerSystem.MECHANIC?.5f:1);e.pactStacks=integer(pm[2],0,3);e.pactDebt=range(pm[3],0,1000);
                    if(e.pactMark>0&&(!g.pactEnabled()||e.hp<=0)||e.pactMark==0&&(e.pactTick>0||e.pactStacks>0||e.pactDebt>0)||e.pactStacks>0&&g.job()!=CareerSystem.BOXER||e.pactDebt>0&&g.job()!=CareerSystem.REPORTER)throw new IllegalArgumentException("敌人禁术标记无效");
                }
                if(!g.free(e.x,e.y,e.radius()))throw new IllegalArgumentException("敌人位置无效");g.enemies.add(e);
            }
            count=n(p,"shots",0,160);for(int i=0;i<count;i++){
                float[] a=array(p,"s"+i,8);ActionGame.Shot s=new ActionGame.Shot(range(a[0],0,1120),range(a[1],0,760),range(a[2],-1000,1000),range(a[3],-1000,1000),range(a[5],1,version>=9?800:500),integer(a[7],0,1)==1);
                s.life=range(a[4],0,3);s.radius=range(a[6],1,10);
                if(version>=3){s.effect=n(p,"shotEffect"+i,0,2);s.pierce=n(p,"shotPierce"+i,0,2);String text=required(p,"shotHits"+i);
                    if(!text.isEmpty()){String[] hits=text.split(",",-1);if(hits.length>2)throw new IllegalArgumentException("穿透命中记录过多");for(String h:hits){int id=Integer.parseInt(h);if(id<1||id>=g.nextId||!s.hitIds.add(id))throw new IllegalArgumentException("穿透命中ID无效");}}
                    if(s.effect>0&&(!s.friendly||s.effect!=g.soulKind())||s.effect!=1&&(s.pierce!=0||!s.hitIds.isEmpty())||s.effect==1&&s.pierce+s.hitIds.size()!=2)throw new IllegalArgumentException("箭矢灵魂状态无效");
                }
                g.shots.add(s);
            }
            count=n(p,"drops",0,80);for(int i=0;i<count;i++){float[] a=array(p,"d"+i,4);ActionGame.Drop d=new ActionGame.Drop(range(a[0],0,1120),range(a[1],0,760),integer(a[2],1,100),integer(a[3],0,1)==1);if(version>=10){d.potion=bool(p,"dropPotion"+i);if(d.potion&&(d.heal||d.amount!=1))throw new IllegalArgumentException("药瓶状态无效");}g.drops.add(d);}
            if(!g.free(g.x,g.y,ActionGame.PLAYER_RADIUS))throw new IllegalArgumentException("角色位置无效");
            if((g.mode==ActionGame.Mode.CAMP&&g.room!=0)||(g.mode!=ActionGame.Mode.CAMP&&g.room<1)||(g.hp==0&&g.mode!=ActionGame.Mode.RESULT))throw new IllegalArgumentException("流程状态无效");
            if(g.roomCleared&&!g.enemies.isEmpty())throw new IllegalArgumentException("房间状态矛盾");
            if(g.mode==ActionGame.Mode.REWARD&&(!g.enemies.isEmpty()||g.room==5&&!g.endless||g.roomCleared))throw new IllegalArgumentException("奖励状态矛盾");
            if(g.won&&(g.mode!=ActionGame.Mode.RESULT&&g.mode!=ActionGame.Mode.CAMP))throw new IllegalArgumentException("结算状态无效");
            // v7 opted in to different abilities: never silently substitute the rewritten acts.
            if(version==7){Arrays.fill(g.careerPacts,false);g.pactCd=0;g.pactTime=0;g.pactWard=0;}
            g.settleRestoredAttack();g.notice=version==7?"旧档已恢复；职业禁术已重做，请回营自行选择。":"存档已恢复";return g;
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
