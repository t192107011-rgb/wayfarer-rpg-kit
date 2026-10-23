package io.wayfarer.rpg.core;

import java.util.*;

/** Fixed-step, Android-free action simulation. World units are pixels; time is seconds. */
public final class ActionGame {
    public static final float WIDTH=1120, HEIGHT=760, PLAYER_RADIUS=15;
    public enum Mode { CAMP, PLAY, REWARD, PAUSED, RESULT }
    public Mode mode=Mode.CAMP;
    public int hero=0,bank=0,power=0,vitality=0,runs=0,wins=0;
    public final int[] experience=new int[3];
    public int custom=-1;
    public final BuildSystem.Profile[] profiles=new BuildSystem.Profile[3];
    public final int[][] talents=new int[6][12];
    public float shield=0,dodgeCharge=0;
    private float swingBoost=1;
    public ActionGame(){for(int i=0;i<3;i++){profiles[i]=new BuildSystem.Profile();profiles[i].name=new String[]{"余烬旅人","雾林信使","夜渡客"}[i];profiles[i].weapon=i;profiles[i].skill=(i+1)%3;profiles[i].origin=i;profiles[i].trait=i;profiles[i].color=i;}}
    public int buildId(){return custom<0?hero:3+custom;}
    public BuildSystem.Profile profile(){return custom<0?null:profiles[custom];}
    public String heroName(){return custom<0?HERO_NAMES[hero]:profile().name;}
    public int heroXp(){return custom<0?experience[hero]:profile().xp;}
    public int rank(int node){return talents[buildId()][node];}
    public int skillStyle(){return custom<0?hero:profile().skill;}
    public int availablePoints(){return BuildSystem.points(heroXp())-BuildSystem.spent(talents[buildId()]);}
    public boolean learn(int node){if(mode!=Mode.CAMP)return false;String reason=BuildSystem.lockReason(talents[buildId()],heroXp(),node);if(!reason.isEmpty()){notice=reason;return false;}talents[buildId()][node]++;hp=maxHp();notice="习得「"+BuildSystem.NODES[node].name+"」";return true;}
    public void resetTalents(){if(mode==Mode.CAMP){Arrays.fill(talents[buildId()],0);hp=maxHp();shield=0;dodgeCharge=0;notice="天赋已重置，全部点数返还。";}}
    public void selectCustom(int slot){if(mode==Mode.CAMP&&slot>=0&&slot<3){custom=slot;hero=profiles[slot].weapon;hp=maxHp();notice="已选择 "+heroName();}}
    public void customize(int slot,BuildSystem.Profile draft){
        if(mode!=Mode.CAMP||slot<0||slot>=3)throw new IllegalArgumentException("仅能在营地编辑角色");
        BuildSystem.Profile copy=draft.copy();copy.name=BuildSystem.cleanName(copy.name);copy.xp=profiles[slot].xp;BuildSystem.validate(copy);profiles[slot]=copy;selectCustom(slot);
    }
    public float skillPeriod(){return 7-rank(9)*.4f-(custom>=0&&profile().trait==2?.8f:0);}
    public float dodgePeriod(){return 1.15f-rank(4)*.07f-(custom>=0&&profile().trait==1?.12f:0);}
    public float potionRatio(){return custom>=0&&profile().trait==0?.55f:.45f;}
    private float originHp(){return custom<0?0:profile().origin==0?18:profile().origin==1?-10:-8;}
    public int room=0,loot=0,kills=0,potions=2,damageBonus=0,healthBonus=0,speedBonus=0,combo=0;
    public float x=560,y=620,hp=110,faceX=0,faceY=-1,time=0;
    public float attackCd=0,attackAnim=0,rollTime=0,rollCd=0,skillCd=0,invulnerable=0,comboWindow=0;
    public float rollX=0,rollY=-1,hitStop=0;
    public boolean roomCleared=false,won=false;
    public String notice="守住灯火，走进长夜。";
    public long rngState=123456789;
    public int nextId=1;
    public final List<Enemy> enemies=new ArrayList<>();
    public final List<Shot> shots=new ArrayList<>();
    public final List<Block> blocks=new ArrayList<>();
    public final List<Drop> drops=new ArrayList<>();
    public final List<Signal> signals=new ArrayList<>();
    private final Set<Integer> swingHits=new HashSet<>();
    public static final String[] HERO_NAMES={"守灯剑士","林间弓手","夜行双刃"};
    public static final String[] ROOM_NAMES={"苔石前庭","沉眠林道","残灯回廊","暮色圣所","守门者之庭"};
    public static final class Input {public float mx,my;public boolean attack;}
    public static final class Enemy {
        public int id,type,state=0; // 0 approach, 1 windup, 2 recovery/rush
        public float x,y,hp,maxHp,timer=0,aimX=0,aimY=1,stun=0,flash=0;
        public boolean struck=false;
        public Enemy(int id,int type,float x,float y,float hp){this.id=id;this.type=type;this.x=x;this.y=y;this.hp=hp;maxHp=hp;}
        public float radius(){return type==3?30:16;}
    }
    public static final class Shot {
        public float x,y,vx,vy,life,damage,radius;
        public boolean friendly;
        public Shot(float x,float y,float vx,float vy,float damage,boolean friendly){this.x=x;this.y=y;this.vx=vx;this.vy=vy;this.damage=damage;this.friendly=friendly;life=3;radius=friendly?5:8;}
    }
    public static final class Block {
        public float x,y,w,h;public int style;
        public Block(float x,float y,float w,float h,int style){this.x=x;this.y=y;this.w=w;this.h=h;this.style=style;}
    }
    public static final class Drop {
        public float x,y;public int amount;public boolean heal;
        public Drop(float x,float y,int amount,boolean heal){this.x=x;this.y=y;this.amount=amount;this.heal=heal;}
    }
    public static final class Signal {
        public String kind;public float x,y;public int value;
        public Signal(String kind,float x,float y,int value){this.kind=kind;this.x=x;this.y=y;this.value=value;}
    }
    public int heroLevel(){return 1+Math.min(19,heroXp()/80);}
    public float maxHp(){return (hero==0?110:hero==1?85:90)+vitality*10+healthBonus+(heroLevel()-1)*3+rank(8)*8+originHp()-(custom>=0&&profile().trait==2?8:0);}
    public float damage(){float value=(hero==0?22:hero==1?18:15)+power*2+damageBonus+(heroLevel()-1)+rank(0)*2+(custom>=0&&profile().origin==2?2:0);return value*(rank(3)>0&&hp<maxHp()*.4f?1.4f:1);}
    public float speed(){return (hero==2?240:hero==1?215:205)*(1+speedBonus*.08f+rank(4)*.03f)*(custom<0?1:profile().origin==0?.95f:profile().origin==1?1.07f:1);}
    public float attackPeriod(){return (hero==0?.48f:hero==1?.4f:.29f)*(custom>=0&&profile().trait==0?1.06f:1);}
    public int upgradeCost(int rank){return 30+rank*25;}
    public int random(int bound){rngState^=rngState<<13;rngState^=rngState>>>7;rngState^=rngState<<17;return (int)Long.remainderUnsigned(rngState,bound);}
    public void signal(String kind,float x,float y,int amount){if(signals.size()<64)signals.add(new Signal(kind,x,y,amount));}
    public void selectHero(int value){if(mode==Mode.CAMP && value>=0&&value<3){custom=-1;hero=value;hp=maxHp();}}
    public void upgrade(boolean health){
        if(mode!=Mode.CAMP)return;int rank=health?vitality:power;
        if(rank>=10){notice="这项营地升级已满级。";return;}
        int cost=upgradeCost(rank);if(bank<cost){notice="还需要 "+(cost-bank)+" 枚余烬。";return;}
        bank-=cost;if(health)vitality++;else power++;hp=maxHp();notice="升级完成，所有角色共同受益。";
    }
    public void start(long seed){
        if(mode!=Mode.CAMP)return;rngState=seed==0?1:seed;runs=Math.min(999999,runs+1);
        loot=0;kills=0;nextId=1;room=1;potions=2;damageBonus=0;healthBonus=0;speedBonus=0;combo=0;time=0;won=false;hp=maxHp();
        generateRoom();
    }
    public void generateRoom(){
        mode=Mode.PLAY;roomCleared=false;x=WIDTH/2;y=HEIGHT-100;faceX=0;faceY=-1;
        attackCd=0;attackAnim=0;rollTime=0;rollCd=0;skillCd=0;shield=0;dodgeCharge=0;swingBoost=1;invulnerable=.8f;comboWindow=0;hitStop=0;
        enemies.clear();shots.clear();blocks.clear();drops.clear();signals.clear();swingHits.clear();
        // Keep the central route and both exits connected in every layout.
        for(int i=0;i<6;i++) {
            float bx=(i%2==0?150:730)+random(120),by=180+(i/2)*140+random(25);
            blocks.add(new Block(bx,by,55+random(35),45+random(25),room<3?0:1));
        }
        if(room==5)enemies.add(new Enemy(nextId++,3,560,230,520));
        else {
            int count=3+room;
            for(int i=0;i<count;i++) {
                int type=room==1?0:random(Math.min(3,room));float ex,ey;int tries=0;
                do{ex=130+random(860);ey=150+random(340);tries++;}while(!free(ex,ey,24)&&tries<100);
                if(!free(ex,ey,24)){ex=560;ey=150+i*35;}
                enemies.add(new Enemy(nextId++,type,ex,ey,40+room*10+(type==2?12:0)));
            }
        }
        notice=ROOM_NAMES[room-1]+" · 清除敌人开启出口";signal("enter",x,y,room);
    }
    public void chooseReward(int which){
        if(mode!=Mode.REWARD || which<0||which>2)return;
        if(which==0){damageBonus+=4;notice="锋芒：本轮伤害 +4";}
        if(which==1){healthBonus+=20;hp=Math.min(maxHp(),hp+35);notice="生息：本轮生命上限 +20，恢复 35";}
        if(which==2){speedBonus++;skillCd=0;notice="轻羽：本轮移速 +8%，技能立即就绪";}
        roomCleared=true;mode=Mode.PLAY;signal("clear",x,y,0);
    }
    public void pause(){if(mode==Mode.PLAY){mode=Mode.PAUSED;notice="已暂停";}}
    public void resume(){if(mode==Mode.PAUSED)mode=Mode.PLAY;}
    public void returnCamp(){if(mode==Mode.RESULT){mode=Mode.CAMP;room=0;damageBonus=0;healthBonus=0;speedBonus=0;roomCleared=false;shield=0;dodgeCharge=0;enemies.clear();shots.clear();blocks.clear();drops.clear();hp=maxHp();notice="冒险所得已结算。选择角色，再次出发。";}}
    public void abandon(){if(mode==Mode.PAUSED)finish(false);}
    public void dodge(float dx,float dy){
        if(mode!=Mode.PLAY||rollCd>0||rollTime>0)return;
        float len=len(dx,dy);if(len<.1f){dx=faceX;dy=faceY;len=1;}
        rollX=dx/len;rollY=dy/len;rollTime=.28f;rollCd=dodgePeriod();invulnerable=Math.max(invulnerable,.3f);attackAnim=0;
        dodgeCharge=rank(5)>0?3:0;
        if(rank(6)>0){for(Enemy e:enemies)if(len(e.x-x,e.y-y)<85){hit(e,damage()*.75f);e.stun=Math.max(e.stun,.45f);}signal("echo",x,y,85);}
        if(rank(7)>0)skillCd=Math.max(0,skillCd-2.5f);
        signal("roll",x,y,0);
    }
    public void potion(){
        if(mode!=Mode.PLAY||potions<=0||hp>=maxHp())return;
        potions--;float amount=maxHp()*potionRatio();hp=Math.min(maxHp(),hp+amount);signal("heal",x,y,(int)amount);
    }
    public void skill(){
        if(mode!=Mode.PLAY||skillCd>0||rollTime>0)return;
        aimAtNearest(440);skillCd=skillPeriod();int style=skillStyle();signal("skill",x,y,style);
        shield=Math.max(shield,rank(9)*6+(rank(11)>0?12:0));
        if(rank(10)>0){float amount=Math.min(maxHp()-hp,maxHp()*.08f);hp+=amount;signal("heal",x,y,(int)amount);}
        if(style==1){double base=Math.atan2(faceY,faceX);for(int i=-2;i<=2;i++)fire((float)Math.cos(base+i*.14),(float)Math.sin(base+i*.14),damage()*1.3f);}
        else {
            float range=style==0?160:125;
            for(Enemy e:enemies)if(len(e.x-x,e.y-y)<range){hit(e,damage()*(style==0?1.8f:2.2f));e.stun=.7f;}
            if(style==2)invulnerable=Math.max(invulnerable,.65f);
            hitStop=.045f;
        }
    }
    private void beginAttack(){
        aimAtNearest(hero==1?500:150);attackCd=attackPeriod();attackAnim=hero==1?.18f:.24f;
        combo=comboWindow>0?(combo+1)%3:0;comboWindow=.85f;swingHits.clear();
        swingBoost=(dodgeCharge>0?1+rank(5)*.15f:1)*(combo==2?1.35f+rank(1)*.12f:1);dodgeCharge=0;
        if(combo==2&&rank(2)>0){for(Enemy e:enemies)if(len(e.x-x,e.y-y)<140)hit(e,damage()*.6f);signal("echo",x,y,140);}
        if(hero==1)fire(faceX,faceY,damage()*swingBoost);signal(hero==1?"bow":"swing",x,y,combo);
    }
    private void fire(float dx,float dy,float value){if(shots.size()<160)shots.add(new Shot(x+dx*20,y+dy*20,dx*640,dy*640,value,true));}
    private void aimAtNearest(float range){
        Enemy nearest=null;float best=range;
        for(Enemy e:enemies){float distance=len(e.x-x,e.y-y);if(e.hp>0&&distance<best&&lineClear(x,y,e.x,e.y)){best=distance;nearest=e;}}
        if(nearest!=null&&best>.01f){faceX=(nearest.x-x)/best;faceY=(nearest.y-y)/best;}
    }
    public void update(float dt,Input in){
        if(mode!=Mode.PLAY)return;
        if(!Float.isFinite(dt)||dt<=0)return;dt=Math.min(dt,.05f);
        if(hitStop>0){hitStop=Math.max(0,hitStop-dt);return;}
        time+=dt;attackCd=dec(attackCd,dt);rollCd=dec(rollCd,dt);skillCd=dec(skillCd,dt);invulnerable=dec(invulnerable,dt);comboWindow=dec(comboWindow,dt);dodgeCharge=dec(dodgeCharge,dt);
        float mx=Float.isFinite(in.mx)?in.mx:0,my=Float.isFinite(in.my)?in.my:0,mag=len(mx,my);
        if(mag>1){mx/=mag;my/=mag;mag=1;}
        if(rollTime>0){movePlayer(rollX*530*dt,rollY*530*dt);rollTime=dec(rollTime,dt);}
        else {
            if(mag>.1f){if(attackAnim<=0){faceX=mx/mag;faceY=my/mag;}float slow=attackAnim>0?.65f:1;movePlayer(mx*speed()*slow*dt,my*speed()*slow*dt);}
            if(in.attack&&attackCd<=0)beginAttack();
        }
        if(attackAnim>0){
            attackAnim=dec(attackAnim,dt);
            if(hero!=1&&attackAnim<.18f&&attackAnim>.04f){
                float range=hero==0?90:72,cos=hero==0?-.05f:.1f;
                for(Enemy e:enemies){float dx=e.x-x,dy=e.y-y,d=len(dx,dy);
                    if(e.hp>0&&!swingHits.contains(e.id)&&d<range+e.radius()&&(d<.01f||(dx*faceX+dy*faceY)/d>cos)&&lineClear(x,y,e.x,e.y)){
                        swingHits.add(e.id);hit(e,damage()*swingBoost);hitStop=.025f;
                    }
                }
            }
        }
        for(Enemy e:enemies)if(e.hp>0)updateEnemy(e,dt);
        if(mode!=Mode.PLAY)return;
        updateShots(dt);if(mode!=Mode.PLAY)return;
        Iterator<Enemy> it=enemies.iterator();while(it.hasNext()){
            Enemy e=it.next();if(e.hp<=0){it.remove();kills=Math.min(99999,kills+1);drops.add(new Drop(e.x,e.y,e.type==3?55:5+room,false));if(random(6)==0)drops.add(new Drop(e.x+15,e.y,15,true));signal("defeat",e.x,e.y,e.type);}
        }
        Iterator<Drop> d=drops.iterator();while(d.hasNext()){
            Drop p=d.next();float dist=len(p.x-x,p.y-y);if(dist<100&&dist>1){float speed=270*dt;p.x+=(x-p.x)/dist*Math.min(speed,dist);p.y+=(y-p.y)/dist*Math.min(speed,dist);}
            if(dist<24){if(p.heal)hp=Math.min(maxHp(),hp+p.amount);else loot=Math.min(99999,loot+p.amount);signal(p.heal?"heal":"coin",p.x,p.y,p.amount);d.remove();}
        }
        if(enemies.isEmpty()&&!roomCleared){
            for(Drop p:drops){if(p.heal)hp=Math.min(maxHp(),hp+p.amount);else loot=Math.min(99999,loot+p.amount);}drops.clear();shots.clear();
            if(room==5)finish(true);else mode=Mode.REWARD;
        }
        if(roomCleared&&y<100){
            if(x<430){hp=Math.min(maxHp(),hp+maxHp()*.18f);room++;generateRoom();notice="穿过泉径，恢复 18% 生命。";}
            else if(x>690){loot=Math.min(99999,loot+12);room++;generateRoom();notice="穿过余烬之门，额外获得 12 余烬。";}
        }
    }
    private void updateEnemy(Enemy e,float dt){
        e.flash=dec(e.flash,dt);if(e.stun>0){e.stun=dec(e.stun,dt);return;}
        float dx=x-e.x,dy=y-e.y,distance=Math.max(.01f,len(dx,dy));
        if(e.state==0){
            float reach=e.type==0?56:e.type==1?330:e.type==2?260:210;
            if(distance<reach&&lineClear(e.x,e.y,x,y)){
                e.state=1;e.timer=e.type==0?.58f:e.type==1?.9f:e.type==2?.85f:1.2f;e.aimX=dx/distance;e.aimY=dy/distance;e.struck=false;
            }else moveEnemy(e,dx/distance*(e.type==3?65:90+room*3)*dt,dy/distance*(e.type==3?65:90+room*3)*dt);
        } else if(e.state==1){
            e.timer-=dt;
            if(e.timer<=0){
                e.state=2;
                if(e.type==0){e.timer=.7f;if(distance<72)hurt(10+room*2);signal("enemySwing",e.x,e.y,0);}
                else if(e.type==1){e.timer=1.1f;enemyShot(e,e.aimX,e.aimY,220,10+room*2);}
                else if(e.type==2)e.timer=.6f;
                else {
                    e.timer=1.3f;signal("slam",e.x,e.y,0);if(distance<185)hurt(24);
                    int count=e.hp<e.maxHp*.5f?14:10;
                    for(int i=0;i<count;i++){double a=i*Math.PI*2/count+time*.1;enemyShot(e,(float)Math.cos(a),(float)Math.sin(a),170,15);}
                }
            }
        }else{
            e.timer-=dt;
            if(e.type==2){moveEnemy(e,e.aimX*380*dt,e.aimY*380*dt);if(!e.struck&&len(x-e.x,y-e.y)<35){e.struck=true;hurt(16+room*2);}}
            if(e.timer<=0)e.state=0;
        }
        // Gentle separation avoids enemies stacking into an unreadable single hitbox.
        for(Enemy other:enemies)if(other!=e&&other.hp>0){float sx=e.x-other.x,sy=e.y-other.y,d=len(sx,sy),min=e.radius()+other.radius();if(d>.1&&d<min)moveEnemy(e,sx/d*22*dt,sy/d*22*dt);}
    }
    private void enemyShot(Enemy e,float dx,float dy,float speed,float damage){if(shots.size()<160)shots.add(new Shot(e.x+dx*24,e.y+dy*24,dx*speed,dy*speed,damage,false));}
    private void updateShots(float dt){
        Iterator<Shot> it=shots.iterator();while(it.hasNext()){
            Shot p=it.next();p.life-=dt;float ox=p.x,oy=p.y,nx=p.x+p.vx*dt,ny=p.y+p.vy*dt;
            boolean gone=p.life<=0||!free(nx,ny,p.radius)||!lineClear(ox,oy,nx,ny);
            if(!gone&&p.friendly){for(Enemy e:enemies)if(e.hp>0&&segmentDistance(e.x,e.y,ox,oy,nx,ny)<e.radius()+p.radius){hit(e,p.damage);gone=true;break;}}
            else if(!gone&&segmentDistance(x,y,ox,oy,nx,ny)<PLAYER_RADIUS+p.radius){hurt(p.damage);gone=true;}
            p.x=nx;p.y=ny;if(gone)it.remove();
        }
    }
    private void hit(Enemy e,float value){if(e.hp<=0)return;e.hp=Math.max(0,e.hp-value);e.flash=.12f;e.stun=Math.max(e.stun,e.type==3?.03f:.12f);signal("hit",e.x,e.y,(int)value);}
    private void hurt(float value){
        if(invulnerable>0||mode!=Mode.PLAY)return;
        value*= (custom>=0&&profile().trait==1?1.12f:1)*(rank(3)>0?1.15f:1);
        float absorbed=Math.min(shield,value);shield-=absorbed;value-=absorbed;
        if(absorbed>0){signal("shield",x,y,(int)absorbed);if(rank(11)>0)for(Enemy e:enemies)if(len(e.x-x,e.y-y)<100)hit(e,absorbed*.5f);}
        hp=Math.max(0,hp-value);invulnerable=.65f;signal("hurt",x,y,(int)value);
        if(hp<=0)finish(false);
    }
    private void finish(boolean victory){
        if(mode==Mode.RESULT||mode==Mode.CAMP)return;
        won=victory;int reward=victory?loot+40:(int)(loot*.7f);bank=Math.min(999999,bank+reward);
        int gained=kills*6+(victory?50:10);
        if(custom<0)experience[hero]=Math.min(1000000,experience[hero]+gained);else profile().xp=Math.min(1000000,profile().xp+gained);
        if(victory)wins=Math.min(999999,wins+1);mode=Mode.RESULT;notice=(victory?"长夜已尽":"灯火尚存")+" · 带回 "+reward+" 余烬";signal(victory?"victory":"death",x,y,reward);
    }
    private void movePlayer(float dx,float dy){if(free(x+dx,y,PLAYER_RADIUS))x+=dx;if(free(x,y+dy,PLAYER_RADIUS))y+=dy;}
    private void moveEnemy(Enemy e,float dx,float dy){
        float ox=e.x,oy=e.y;
        if(free(e.x+dx,e.y,e.radius()))e.x+=dx;if(free(e.x,e.y+dy,e.radius()))e.y+=dy;
        if(e.state!=0||len(e.x-ox,e.y-oy)>.1f)return;
        // Sidestep the nearest blocking edge rather than pushing forever into a pillar.
        float step=Math.max(Math.abs(dx),Math.abs(dy));
        for(Block b:blocks){
            if(e.x>=b.x-e.radius()-5&&e.x<=b.x+b.w+e.radius()+5&&e.y>=b.y-e.radius()-5&&e.y<=b.y+b.h+e.radius()+5){
                if(Math.abs(dx)>=Math.abs(dy)){float sy=e.y<b.y+b.h/2?-step:step;if(free(e.x,e.y+sy,e.radius()))e.y+=sy;}
                else {float sx=e.x<b.x+b.w/2?-step:step;if(free(e.x+sx,e.y,e.radius()))e.x+=sx;}
                break;
            }
        }
    }
    public boolean free(float px,float py,float radius){
        if(px-radius<48||py-radius<48||px+radius>WIDTH-48||py+radius>HEIGHT-48)return false;
        for(Block b:blocks){float cx=Math.max(b.x,Math.min(px,b.x+b.w)),cy=Math.max(b.y,Math.min(py,b.y+b.h));if(len(px-cx,py-cy)<radius)return false;}
        return true;
    }
    public boolean lineClear(float ax,float ay,float bx,float by){
        int steps=Math.max(1,(int)(len(bx-ax,by-ay)/8));for(int i=0;i<=steps;i++){float t=i/(float)steps;if(!free(ax+(bx-ax)*t,ay+(by-ay)*t,1))return false;}return true;
    }
    public static float segmentDistance(float px,float py,float ax,float ay,float bx,float by){float dx=bx-ax,dy=by-ay,q=dx*dx+dy*dy;if(q<.0001f)return len(px-ax,py-ay);float t=Math.max(0,Math.min(1,((px-ax)*dx+(py-ay)*dy)/q));return len(px-ax-t*dx,py-ay-t*dy);}
    public static float len(float x,float y){return (float)Math.sqrt(x*x+y*y);}
    private static float dec(float value,float dt){return Math.max(0,value-dt);}
    /** A restored partial swing is cancelled without granting a second hit. */
    public void settleRestoredAttack(){attackAnim=0;swingHits.clear();}
}
