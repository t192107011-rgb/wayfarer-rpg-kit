package io.wayfarer.rpg.core;

/** Room-entry scaling. Permanent growth and last-room performance never rewrite living enemies. */
public final class ThreatSystem {
    private ThreatSystem(){}
    public static float rating(ActionGame g){
        int relics=(g.relics[g.buildId()][0]>0?1:0)+(g.relics[g.buildId()][1]>0?1:0);
        float growth=(g.heroLevel()-1)*.035f+g.power*.04f+g.vitality*.025f+BuildSystem.spent(g.talents[g.buildId()])*.018f+relics*.07f+(g.specialization()>0?.08f:0)+(g.soulKind()>0?.1f:0)+(g.characterTrait(1)+g.characterTrait(2))*.03f;
        float depth=g.endless?Math.min(2.5f,(g.endlessDepth-1)*.065f):0;
        return Math.min(6,1.2f+g.room*.12f+growth+g.pressure+depth);
    }
    public static void enter(ActionGame g){g.threat=rating(g);g.encounterHpFloor=g.maxHp()*1.2f+g.baseDamage()*2;g.encounterDamageFloor=g.baseDamage()*1.12f;g.roomDamage=0;g.roomElapsed=0;}
    public static void clear(ActionGame g){float loss=g.roomDamage/Math.max(1,g.maxHp());if(loss<=.12f&&g.roomElapsed<=45)g.pressure=Math.min(.6f,g.pressure+.12f);else if(loss>=.55f)g.pressure=Math.max(0,g.pressure-.12f);}
    public static int potionChance(int type){return type==3?50:type>=4?18:12;}
}
