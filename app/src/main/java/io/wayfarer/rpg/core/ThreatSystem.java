package io.wayfarer.rpg.core;

/** Room-entry scaling. Permanent character growth sets both opening pressure and encounter floors. */
public final class ThreatSystem {
    private ThreatSystem(){}
    public static float rating(ActionGame g){
        int id=g.buildId();
        int relics=(g.relics[id][0]>0?1:0)+(g.relics[id][1]>0?1:0);
        float growth=(g.heroLevel()-1)*.07f
            +g.power*.085f+g.vitality*.05f
            +g.training[id][0]*.045f+g.training[id][1]*.03f+g.training[id][2]*.02f+g.training[id][3]*.02f
            +BuildSystem.spent(g.talents[id])*.025f
            +BuildSystem.spent(g.specialties[id])*.02f
            +BuildSystem.coreSpent(g.coreTalents[id])*.018f
            +relics*.10f+(g.specialization()>0?.12f:0)+(g.soulKind()>0?.12f:0)
            +(g.characterTrait(1)+g.characterTrait(2))*.04f;
        float depth=g.endless?Math.min(98f,(g.endlessDepth-1)*.72f):0;
        return Math.min(99f,1.35f+g.room*.22f+growth+g.pressure+depth);
    }
    public static void enter(ActionGame g){
        g.threat=rating(g);
        g.encounterHpFloor=g.maxHp()*1.4f+g.baseDamage()*5f;
        g.encounterDamageFloor=g.maxHp()*.08f+g.baseDamage()*.15f;
        g.roomDamage=0;g.roomElapsed=0;
    }
    public static void clear(ActionGame g){
        float loss=g.roomDamage/Math.max(1,g.maxHp());
        if(loss<=.12f&&g.roomElapsed<=45)g.pressure=Math.min(.6f,g.pressure+.12f);
        else if(loss>=.55f)g.pressure=Math.max(0,g.pressure-.12f);
    }
    public static int potionChance(int type){return type==3?50:type>=4?18:12;}
}
