package io.wayfarer.rpg;

import android.content.Context;
import android.graphics.*;
import android.view.*;
import io.wayfarer.rpg.core.*;
import java.util.*;

/** Native landscape canvas with fixed-step simulation and independent multi-touch pointers. */
public final class WorldView extends View implements Choreographer.FrameCallback {
    public interface Host {void save();void slots();void help();void confirmAbandon();void sound(String event);void editCharacter();void talent(int id);void resetTalents();}
    private final Host host;
    public ActionGame game;
    private final ActionGame.Input input=new ActionGame.Input();
    private final Paint p=new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF rect=new RectF();
    private final List<Fx> effects=new ArrayList<>();
    private final Random cosmetic=new Random(119);
    private int movePointer=-1,attackPointer=-1;
    private float joyX=0,joyY=0,scale=1,offsetX=0,offsetY=0,cameraX=0,cameraY=0;
    private long lastFrame=0;
    private float accumulator=0,saveElapsed=0,visualTime=0,shake=0;
    private boolean running=false;
    public boolean soundOn=true,showTalents=false,customRoster=false;
    private int bg=Color.rgb(13,24,29),cream=Color.rgb(232,228,202),muted=Color.rgb(155,180,171),gold=Color.rgb(222,186,111);
    private static final class Fx {String kind;float x,y,life,max,vx,vy;int value,color;Fx(String k,float x,float y,float life,int value,int color){kind=k;this.x=x;this.y=y;this.life=life;max=life;this.value=value;this.color=color;}}
    public WorldView(Context c,ActionGame game,Host host){super(c);this.game=game;this.host=host;customRoster=game.custom>=0;setFocusable(true);setFocusableInTouchMode(true);setContentDescription("灯下归途：横屏即时动作地牢。左侧摇杆移动，右侧攻击、闪避、技能和药水。暂停键位于右上角。");}
    public void startFrames(){if(!running){running=true;lastFrame=0;Choreographer.getInstance().postFrameCallback(this);}}
    public void stopFrames(){running=false;lastFrame=0;resetInput();Choreographer.getInstance().removeFrameCallback(this);}
    public void replace(ActionGame restored){game=restored;showTalents=false;customRoster=game.custom>=0;effects.clear();resetInput();if(game.mode==ActionGame.Mode.PLAY)game.pause();invalidate();}
    public void resetInput(){movePointer=-1;attackPointer=-1;joyX=0;joyY=0;input.mx=0;input.my=0;input.attack=false;}
    @Override public void onWindowFocusChanged(boolean hasFocus){super.onWindowFocusChanged(hasFocus);if(!hasFocus){resetInput();game.pause();host.save();}lastFrame=0;}
    @Override public void doFrame(long now){
        if(!running)return;
        float elapsed=lastFrame==0?0:Math.min(.0833f,(now-lastFrame)/1_000_000_000f);lastFrame=now;visualTime+=elapsed;
        ActionGame.Mode old=game.mode;int room=game.room;
        accumulator+=elapsed;int steps=0;
        while(accumulator>=1f/60f&&steps++<5){game.update(1f/60f,input);accumulator-=1f/60f;}
        if(accumulator>.1f)accumulator=0;
        if(old!=game.mode||room!=game.room){resetInput();host.save();}
        if(game.mode==ActionGame.Mode.PLAY){saveElapsed+=elapsed;if(saveElapsed>=15){saveElapsed=0;host.save();}}
        consumeSignals();
        if(game.mode==ActionGame.Mode.PLAY||game.mode==ActionGame.Mode.CAMP||game.mode==ActionGame.Mode.RESULT){
            Iterator<Fx> it=effects.iterator();while(it.hasNext()){Fx f=it.next();f.life-=elapsed;f.x+=f.vx*elapsed;f.y+=f.vy*elapsed;if(f.life<=0)it.remove();}shake=Math.max(0,shake-elapsed*18);
        }
        invalidate();Choreographer.getInstance().postFrameCallback(this);
    }
    private void consumeSignals(){
        for(ActionGame.Signal s:game.signals){
            if(soundOn)host.sound(s.kind);
            if(s.kind.equals("hit")||s.kind.equals("hurt")||s.kind.equals("heal")){
                Fx f=new Fx("number",s.x,s.y-30,.7f,s.value,s.kind.equals("hurt")?0xFFFF917E:s.kind.equals("heal")?0xFFB4E9BA:cream);f.vy=-35;effects.add(f);
                if(s.kind.equals("hurt"))shake=5;
                for(int i=0;i<6;i++){Fx spark=new Fx("spark",s.x,s.y,.2f+cosmetic.nextFloat()*.2f,0,s.kind.equals("hurt")?0xFFCE705C:gold);spark.vx=(cosmetic.nextFloat()-.5f)*180;spark.vy=(cosmetic.nextFloat()-.5f)*180;effects.add(spark);}
            }else if(s.kind.equals("skill")||s.kind.equals("slam")||s.kind.equals("clear")||s.kind.equals("echo")||s.kind.equals("shield"))effects.add(new Fx("ring",s.x,s.y,.45f,s.kind.equals("slam")?185:s.kind.equals("echo")?s.value:s.kind.equals("shield")?45:140,s.kind.equals("slam")?0xFFDC7053:0xFFBCE8C5));
            else if(s.kind.equals("coin"))effects.add(new Fx("spark",s.x,s.y,.25f,0,gold));
        }
        game.signals.clear();while(effects.size()>140)effects.remove(0);
    }
    private void color(int v){p.setColor(v);p.setAlpha(255);p.setShader(null);p.setStyle(Paint.Style.FILL);}
    private void box(Canvas c,float x,float y,float w,float h,int color){color(color);c.drawRect(x,y,x+w,y+h,p);}
    private void rounded(Canvas c,float x,float y,float w,float h,float radius,int color){color(color);rect.set(x,y,x+w,y+h);c.drawRoundRect(rect,radius,radius,p);}
    private void circle(Canvas c,float x,float y,float r,int color){color(color);c.drawCircle(x,y,r,p);}
    private void text(Canvas c,String s,float x,float y,float size,int color,boolean center){color(color);p.setTypeface(Typeface.create("sans-serif",Typeface.NORMAL));p.setTextSize(size);p.setTextAlign(center?Paint.Align.CENTER:Paint.Align.LEFT);c.drawText(s,x,y,p);}
    private void line(Canvas c,float x,float y,float x2,float y2,int color,float width){color(color);p.setStrokeWidth(width);c.drawLine(x,y,x2,y2,p);}
    private void glow(Canvas c,float x,float y,float radius,int color){p.setStyle(Paint.Style.FILL);p.setAlpha(255);p.setShader(new RadialGradient(x,y,radius,color,Color.TRANSPARENT,Shader.TileMode.CLAMP));c.drawCircle(x,y,radius,p);p.setShader(null);}
    private void button(Canvas c,float x,float y,float w,float h,String title,boolean accent){rounded(c,x,y,w,h,10,accent?0xFFB6D49E:0xFF293C40);text(c,title,x+w/2,y+h/2+6,18,accent?0xFF152B28:cream,true);}
    @Override protected void onDraw(Canvas canvas){
        super.onDraw(canvas);canvas.drawColor(bg);scale=Math.min(getWidth()/960f,getHeight()/540f);offsetX=(getWidth()-960*scale)/2;offsetY=(getHeight()-540*scale)/2;
        canvas.save();canvas.translate(offsetX,offsetY);canvas.scale(scale,scale);canvas.clipRect(0,0,960,540);
        if(game.mode==ActionGame.Mode.CAMP){if(showTalents)talentTree(canvas);else camp(canvas);}else{
            dungeon(canvas);hud(canvas);
            if(game.mode==ActionGame.Mode.PLAY)controls(canvas);
            else if(game.mode==ActionGame.Mode.REWARD)rewards(canvas);
            else if(game.mode==ActionGame.Mode.PAUSED)pausePanel(canvas);
            else result(canvas);
        }
        canvas.restore();
    }
    private void camp(Canvas c){
        box(c,0,0,960,540,0xFF12282D);
        for(int i=0;i<30;i++){float px=(i*97)%960,py=100+(i*53)%440;tree(c,px,py,.9f+(i%3)*.25f,0xFF193637);}
        glow(c,480,320,250,0x554C8B77);glow(c,480,320,100,0x66EDB16B);
        for(int i=0;i<7;i++){float a=visualTime*1.5f+i*2;circle(c,470+i*4,310-(visualTime*25+i*13)%80,2,0xFFDEAF76);}
        text(c,"灯 下 归 途",44,62,33,cream,false);text(c,"WAYFARER  /  ACTION RPG KIT",46,88,12,muted,false);
        text(c,"选择守灯者，穿过五重庭院。每次归来，都让灯火更亮。",46,112,14,muted,false);
        text(c,"共享余烬  "+game.bank,820,51,19,gold,true);text(c,"远征 "+game.runs+"  ·  通关 "+game.wins,770,83,13,muted,false);
        button(c,40,126,130,32,"预设角色",!customRoster);button(c,185,126,130,32,"我的角色",customRoster);
        text(c,"当前："+game.heroName()+"  ·  可用天赋点 "+game.availablePoints(),350,148,14,gold,false);
        String[] detail={"弧形三连斩 · 震地技能","自动瞄准射击 · 扇形箭雨","高速连击 · 环刃与无敌"};
        for(int i=0;i<3;i++){
            float bx=40+i*300;BuildSystem.Profile look=customRoster?game.profiles[i]:null;
            boolean selected=customRoster?game.custom==i:game.custom<0&&game.hero==i;
            rounded(c,bx,170,280,165,12,selected?0xFF38534E:0xE0203438);
            if(selected)box(c,bx,185,3,135,0xFFBED9A3);
            sprite(c,bx+40,252,look==null?i:look.weapon,0,1,false,1.7f,0,look);
            text(c,look==null?ActionGame.HERO_NAMES[i]:look.name,bx+90,211,look==null?22:17,cream,false);
            text(c,"Lv. "+(1+Math.min(19,(look==null?game.experience[i]:look.xp)/80)),bx+92,239,14,gold,false);
            text(c,look==null?detail[i]:BuildSystem.WEAPONS[look.weapon],bx+20,283,14,muted,false);
            if(look!=null)text(c,"技能："+BuildSystem.SKILLS[look.skill].split(" · ")[0],bx+20,305,13,gold,false);
            text(c,selected?"已选择":"点击选择",bx+235,320,12,selected?gold:muted,true);
        }
        button(c,40,365,280,57,"磨刃 +2  ·  "+(game.power>=10?"MAX":game.upgradeCost(game.power)+" 余烬"),false);
        button(c,340,365,280,57,"护火 +10 HP  ·  "+(game.vitality>=10?"MAX":game.upgradeCost(game.vitality)+" 余烬"),false);
        text(c,"永久攻击 "+game.power+"/10     永久生命 "+game.vitality+"/10",40,445,13,muted,false);
        button(c,660,365,260,80,"踏入长夜  →",true);
        text(c,game.notice.length()>54?game.notice.substring(0,54):game.notice,40,465,12,muted,false);
        button(c,40,480,100,42,"存档",false);button(c,155,480,110,42,"操作说明",false);
        button(c,280,480,155,42,"角色工坊",false);button(c,450,480,175,42,"技能树 · "+game.availablePoints(),true);
        text(c,"营地可免费改造与重置天赋",650,506,13,muted,false);
    }
    public boolean closeWorkshop(){if(showTalents){showTalents=false;invalidate();return true;}return false;}
    public void drawPortrait(Canvas c,float x,float y,float size,BuildSystem.Profile profile){sprite(c,x,y,profile.weapon,0,1,false,size,0,profile);}
    private float[] nodeRect(int id){float bx=50+(id/4)*305;int tier=id%4;return new float[]{bx+(tier==3?130:0),tier==0?135:tier==1?238:350,tier<2?250:120,tier<2?67:83};}
    private void talentTree(Canvas c){
        box(c,0,0,960,540,0xFF12282D);glow(c,480,270,370,0x223E8976);
        text(c,game.heroName()+" · 天赋星图",40,48,27,cream,false);
        text(c,"Lv."+game.heroLevel()+"  ·  经验 "+game.heroXp()+"  ·  可用 "+game.availablePoints()+" / 总点数 "+BuildSystem.points(game.heroXp()),40,80,15,gold,false);
        button(c,855,15,88,45,"返回",false);
        for(int b=0;b<3;b++){
            float x=50+b*305;int tint=new int[]{0xFFE3B17D,0xFF99CEC8,0xFFC4B5DC}[b];
            text(c,BuildSystem.BRANCHES[b],x+125,119,17,tint,true);
            line(c,x+125,202,x+125,238,0xFF486561,3);
            line(c,x+125,305,x+125,326,0xFF486561,3);line(c,x+60,326,x+190,326,0xFF486561,3);
            line(c,x+60,326,x+60,350,0xFF486561,3);line(c,x+190,326,x+190,350,0xFF486561,3);
            for(int k=0;k<4;k++){
                int id=b*4+k,r=game.rank(id);BuildSystem.Node n=BuildSystem.NODES[id];float[] a=nodeRect(id);
                String reason=BuildSystem.lockReason(game.talents[game.buildId()],game.heroXp(),id);
                rounded(c,a[0],a[1],a[2],a[3],9,r>0?0xFF36554F:reason.isEmpty()?0xFF2F4148:0xFF1D3037);
                if(r>0)box(c,a[0],a[1]+9,3,a[3]-18,tint);
                text(c,n.name,a[0]+a[2]/2,a[1]+27,k<2?20:17,r>0?tint:cream,true);
                text(c,r+" / "+n.maxRank+(k<2?"  ·  "+(reason.isEmpty()?"可学习":r==n.maxRank?"已满级":"需前置或点数"):""),a[0]+a[2]/2,a[1]+50,12,muted,true);
                if(k>=2)text(c,r>0?"已定型":"终阶 · 择一",a[0]+a[2]/2,a[1]+70,12,r>0?tint:muted,true);
            }
        }
        text(c,"点击节点查看效果与前置。六个终阶天赋只能选一个。",50,461,14,muted,false);
        text(c,"初始2点，每80经验+1点，最多16点。各角色独立成长。",50,501,13,gold,false);
        button(c,700,475,210,42,"免费重置天赋",false);
    }
    private void dungeon(Canvas c){
        cameraX=Math.max(0,Math.min(ActionGame.WIDTH-960,game.x-480));cameraY=Math.max(0,Math.min(ActionGame.HEIGHT-540,game.y-275));
        c.save();c.translate(-cameraX+(float)Math.sin(visualTime*81)*shake,-cameraY+(float)Math.cos(visualTime*67)*shake);
        int floor=game.room<3?0xFF293E3B:game.room==5?0xFF30383E:0xFF303E41;
        box(c,0,0,1120,760,0xFF102328);
        for(int yy=48;yy<712;yy+=32)for(int xx=48;xx<1072;xx+=32){
            int hash=(xx*17+yy*31+game.room*7)&15;
            box(c,xx,yy,31,31,hash<3?floor+0x030403:hash>12?floor-0x020202:floor);
            if(hash==2)line(c,xx+5,yy+19,xx+15,yy+22,0xFF49605A,1);
        }
        // Stone perimeter, repeated pillars and two distinct exit gates.
        for(int xx=16;xx<1100;xx+=48){box(c,xx,22,46,27,0xFF47565A);box(c,xx,708,46,30,0xFF1D3036);}
        for(int yy=48;yy<720;yy+=48){box(c,18,yy,30,46,0xFF3D5053);box(c,1072,yy,30,46,0xFF253A41);}
        gate(c,280,70,game.roomCleared,"泉径 · 恢复",0xFF83BFC1);gate(c,840,70,game.roomCleared,"余烬 · 收获",gold);
        for(ActionGame.Block b:game.blocks){
            box(c,b.x+6,b.y+8,b.w,b.h,0x6610171B);
            if(b.style==0){box(c,b.x,b.y,b.w,b.h,0xFF40524D);box(c,b.x+5,b.y+3,b.w-10,10,0xFF657563);for(int i=0;i<3;i++)box(c,b.x+i*20,b.y-5,16,9,0xFF6C7960);}
            else {box(c,b.x,b.y,b.w,b.h,0xFF536063);box(c,b.x+4,b.y+3,b.w-8,8,0xFF84908A);line(c,b.x+2,b.y+b.h/2,b.x+b.w,b.y+b.h/2,0xFF33464B,2);}
        }
        for(int i=0;i<4;i++){float tx=i%2==0?80:1040,ty=160+(i/2)*380;glow(c,tx,ty,100,0x32FFC56D);box(c,tx-3,ty-3,6,16,0xFF5C5041);circle(c,tx,ty-7,4+(float)Math.sin(visualTime*5+i),0xFFFFD18B);}
        for(ActionGame.Enemy e:game.enemies){
            if(e.state==1){float alpha=.6f+.3f*(float)Math.sin(visualTime*12);
                if(e.type==3){circle(c,e.x,e.y,185,((int)(alpha*48)<<24)|0xD86E51);ring(c,e.x,e.y,185,0xCCEC9A75,3);}
                else if(e.type==2){line(c,e.x,e.y,e.x+e.aimX*240,e.y+e.aimY*240,0x99E5A46F,28);line(c,e.x,e.y,e.x+e.aimX*240,e.y+e.aimY*240,0xFFECD0A1,2);}
                else if(e.type==1)line(c,e.x,e.y,e.x+e.aimX*350,e.y+e.aimY*350,0x88E5A46F,2);
                else {circle(c,e.x,e.y,62,0x33EB936D);ring(c,e.x,e.y,62,0xCCEB936D,2);}
            }
        }
        for(ActionGame.Drop d:game.drops){glow(c,d.x,d.y,18,d.heal?0x556BCAB7:0x55EFC16A);box(c,d.x-4,d.y-5,8,8,d.heal?0xFFAAE1B3:gold);}
        ArrayList<ActionGame.Enemy> sorted=new ArrayList<>(game.enemies);sorted.sort(Comparator.comparingDouble(a->a.y));boolean drawn=false;
        for(ActionGame.Enemy e:sorted){if(!drawn&&e.y>game.y){player(c);drawn=true;}enemy(c,e);}if(!drawn)player(c);
        for(ActionGame.Shot s:game.shots){line(c,s.x-s.vx*.025f,s.y-s.vy*.025f,s.x,s.y,s.friendly?0xFFF3DDA5:0xFFDB8273,s.friendly?3:5);circle(c,s.x,s.y,s.radius,s.friendly?0xFFFFEBC3:0xFFF5BA8B);}
        for(Fx f:effects){color(f.color);p.setAlpha((int)(255*f.life/f.max));if(f.kind.equals("number")){p.setTextSize(18);p.setTextAlign(Paint.Align.CENTER);c.drawText(Integer.toString(f.value),f.x,f.y,p);}else if(f.kind.equals("ring")){p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(3);c.drawCircle(f.x,f.y,f.value*(1-f.life/f.max),p);p.setStyle(Paint.Style.FILL);}else c.drawRect(f.x-2,f.y-2,f.x+2,f.y+2,p);}
        p.setAlpha(255);c.restore();
        // Restrained vignette keeps combat silhouettes and warnings readable.
        p.setShader(new RadialGradient(480,270,570,new int[]{0x000B1720,0x000B1720,0xBB0B1720},new float[]{0,.58f,1},Shader.TileMode.CLAMP));c.drawRect(0,0,960,540,p);p.setShader(null);
    }
    private void ring(Canvas c,float x,float y,float radius,int color,float width){color(color);p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(width);c.drawCircle(x,y,radius,p);p.setStyle(Paint.Style.FILL);}
    private void gate(Canvas c,float x,float y,boolean open,String name,int tint){
        box(c,x-48,y-28,14,62,0xFF657577);box(c,x+34,y-28,14,62,0xFF657577);box(c,x-52,y-35,104,12,0xFF81908B);
        box(c,x-32,y-18,64,46,open?0x885AA899:0xFF15262C);if(open){glow(c,x,y,80,0x554DCEAB);text(c,name,x,y+57,14,tint,true);}else for(int i=-2;i<=2;i++)box(c,x+i*12-2,y-18,4,46,0xFF687575);
    }
    private void player(Canvas c){
        if(game.shield>0)ring(c,game.x,game.y-10,27,0xCC8ACEDD,2);
        if(game.dodgeCharge>0)ring(c,game.x,game.y,21,0xCCEACB7E,2);
        if(game.rollTime>0)for(int i=1;i<=3;i++){circle(c,game.x-game.rollX*i*12,game.y-game.rollY*i*12,13,0x223FCAA7);}
        sprite(c,game.x,game.y,game.hero,game.faceX,game.faceY,game.invulnerable>0&&((int)(visualTime*18)%2==0),1,game.mode==ActionGame.Mode.PLAY?visualTime:0,game.profile());
        if(game.attackAnim>0&&game.hero!=1){
            float angle=(float)Math.toDegrees(Math.atan2(game.faceY,game.faceX));float radius=game.hero==0?83:67;
            color(0xFFEDD6A4);p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(game.combo==2?9:6);rect.set(game.x-radius,game.y-radius,game.x+radius,game.y+radius);
            c.drawArc(rect,angle-80+(1-game.attackAnim/.24f)*45,105,false,p);p.setStyle(Paint.Style.FILL);
        }
    }
    private void sprite(Canvas c,float x,float y,int kind,float dx,float dy,boolean flash,float size,float walk,BuildSystem.Profile look){
        c.save();c.translate(x,y);c.scale(size,size);float step=(float)Math.sin(walk*11)*(Math.abs(input.mx)+Math.abs(input.my)>.1?2:0);
        color(0x7708131A);c.drawOval(-19,-3,19,9,p);
        int cloth=kind==0?0xFFA9C59A:kind==1?0xFFC39C70:0xFF9A9FCE;if(look!=null)cloth=BuildSystem.PALETTE[look.color];if(flash)cloth=0xFFEBF5CD;
        box(c,-10,-19,20,21,0xFF172B31);box(c,-8,-22,16,21,cloth);box(c,-11,-12,6,16,cloth-0x202020);box(c,5,-12,6,16,cloth-0x202020);
        box(c,-7,-34,14,13,0xFF1D2A32);box(c,-5,-32,10,11,look==null?0xFFD6B48E:BuildSystem.SKIN[look.skin]);box(c,-7,-36,14,6,kind==1?0xFF684A3D:0xFF36484D);
        if(look!=null&&look.hair==1){box(c,-8,-33,3,19,0xFF684A3D);box(c,5,-33,3,19,0xFF684A3D);}
        if(look!=null&&look.hair==2){box(c,-8,-37,16,5,cloth);box(c,-8,-34,3,14,cloth);box(c,5,-34,3,14,cloth);}
        box(c,-8,0+step,6,8,0xFF252B30);box(c,2,0-step,6,8,0xFF252B30);
        box(c,-6,-10,12,3,0xFF6B5741);box(c,-1,-11,3,4,gold);
        if(kind==0){box(c,13,-27,4,27,0xFFC6D5CD);box(c,9,-5,12,3,0xFF947652);box(c,-17,-17,8,19,0xFF678281);}
        else if(kind==1){ring(c,13,-14,12,0xFFB99A67,2);line(c,13,-26,13,-2,0xFFD4D0AF,1);}
        else {box(c,-17,-19,3,17,0xFFD1DCE0);box(c,14,-19,3,17,0xFFD1DCE0);}
        if(dy>=-.3f){box(c,-3+(dx>0?2:0),-27,2,2,0xFF253039);box(c,2+(dx>0?1:0),-27,2,2,0xFF253039);}
        c.restore();
    }
    private void enemy(Canvas c,ActionGame.Enemy e){
        c.save();c.translate(e.x,e.y);float s=e.type==3?2.1f:1;c.scale(s,s);color(0x880B151B);c.drawOval(-19,-2,19,11,p);
        int tint=e.flash>0?0xFFF4E4BD:e.type==0?0xFF936C65:e.type==1?0xFF6F8B91:e.type==2?0xFFA18A60:0xFF967967;
        box(c,-14,-27,28,30,0xFF1C292E);box(c,-11,-25,22,26,tint);box(c,-8,-37,16,16,tint-0x101010);
        box(c,-14,-38,6,9,tint);box(c,8,-38,6,9,tint);box(c,-5,-31,4,3,0xFFEDB781);box(c,3,-31,4,3,0xFFEDB781);
        box(c,-9,0,6,8,0xFF253239);box(c,3,0,6,8,0xFF253239);
        if(e.type==1){box(c,15,-37,3,40,0xFF819293);circle(c,16,-39,5,0xFFE4AA79);}else if(e.type==2)box(c,13,-22,10,16,0xFFC4B58A);
        c.restore();
        if(e.type!=3&&e.hp<e.maxHp){box(c,e.x-20,e.y-47,40,3,0xFF172529);box(c,e.x-20,e.y-47,40*e.hp/e.maxHp,3,0xFFB98672);}
    }
    private void tree(Canvas c,float x,float y,float s,int color){box(c,x-5*s,y-40*s,10*s,50*s,0xFF172B30);circle(c,x,y-55*s,34*s,color);circle(c,x-23*s,y-35*s,24*s,color);circle(c,x+20*s,y-38*s,28*s,color);}
    private void hud(Canvas c){
        rounded(c,17,13,308,66,10,0xDC12242B);text(c,game.heroName()+"  Lv."+game.heroLevel(),30,36,15,cream,false);
        box(c,30,47,220,10,0xFF37464A);box(c,30,47,220*Math.max(0,game.hp/game.maxHp()),10,0xFFBA705D);text(c,(int)game.hp+" / "+(int)game.maxHp(),263,57,12,cream,false);
        text(c,"第 "+game.room+" / 5 庭院  ·  "+(game.room>0?ActionGame.ROOM_NAMES[game.room-1]:""),480,28,16,cream,true);
        text(c,"余烬 "+game.loot+"   ·   敌人 "+game.enemies.size(),480,50,13,gold,true);
        if(game.shield>0||game.dodgeCharge>0)text(c,"护盾 "+(int)game.shield+(game.dodgeCharge>0?"  · 借势就绪":""),30,98,13,gold,false);
        button(c,855,15,88,45,"Ⅱ 暂停",false);
        if(game.roomCleared){rounded(c,280,82,400,35,8,0xDF18372F);text(c,"出口已开启：向上走，选择泉径或余烬之门",480,105,14,0xFFCADEB0,true);}
        for(ActionGame.Enemy e:game.enemies)if(e.type==3){text(c,e.hp<e.maxHp*.5f?"失灯守卫 · 狂怒":"失灯守卫",480,87,14,gold,true);box(c,340,97,280,7,0xFF273239);box(c,340,97,280*e.hp/e.maxHp,7,0xFFB36C5B);}
    }
    private void control(Canvas c,float x,float y,float r,String label,float cooldown,boolean held){circle(c,x,y,r,held?0xCC6C8271:0xBB192D36);ring(c,x,y,r,0x998AA598,2);text(c,cooldown>.05f?String.format(java.util.Locale.ROOT,"%.1f",cooldown):label,x,y+6,18,cooldown>.05f?muted:cream,true);}
    private void controls(Canvas c){
        circle(c,117,423,69,0x8814252D);ring(c,117,423,69,0x7789A39A,2);circle(c,117+joyX*48,423+joyY*48,27,0xAA79968A);
        text(c,"移动",117,519,12,muted,true);
        control(c,850,420,58,"攻击",0,input.attack);control(c,737,456,37,"闪避",game.rollCd,false);control(c,855,298,38,"技能",game.skillCd,false);
        control(c,648,456,29,"药×"+game.potions,0,false);
    }
    private void shade(Canvas c){box(c,0,0,960,540,0xD50C1820);}
    private void rewards(Canvas c){
        shade(c);text(c,"余 火 的 馈 赠",480,126,29,cream,true);text(c,"选择一项本轮增益，再选择出口继续前进",480,160,16,muted,true);
        String[] titles={"锋芒","生息","轻羽"};String[] body={"所有攻击伤害 +4","生命上限 +20，恢复 35","移动速度 +8%，技能就绪"};
        for(int i=0;i<3;i++){rounded(c,45+i*300,211,270,165,12,0xFF2D4545);text(c,titles[i],180+i*300,266,26,gold,true);text(c,body[i],180+i*300,311,15,cream,true);text(c,"点击获取",180+i*300,351,13,muted,true);}
        text(c,"局内增益在下一次远征时重置",480,421,14,muted,true);
    }
    private void pausePanel(Canvas c){shade(c);text(c,"灯火暂歇",480,125,30,cream,true);text(c,"暂停期间，战斗与冷却均停止",480,162,15,muted,true);
        button(c,330,205,300,53,"继续冒险",true);button(c,330,272,140,50,"存档",false);button(c,490,272,140,50,soundOn?"音效：开":"音效：关",false);
        button(c,330,337,300,50,"结束本轮并返回营地",false);button(c,330,403,300,45,"操作说明",false);
    }
    private void result(Canvas c){shade(c);text(c,game.won?"长 夜 已 尽":"灯 火 尚 存",480,155,34,game.won?gold:cream,true);
        text(c,game.notice,480,212,20,cream,true);text(c,"击败 "+game.kills+" 名敌人 · 抵达第 "+game.room+" 庭院",480,258,17,muted,true);
        text(c,game.won?"带回全部余烬，并获得通关奖励。":"带回已拾取余烬的 70%，角色经验仍然保留。",480,298,16,muted,true);
        button(c,340,359,280,65,"返回营地",true);
    }
    private boolean inside(float x,float y,float left,float top,float width,float height){return x>=left&&x<=left+width&&y>=top&&y<=top+height;}
    private void uiTap(float x,float y){
        if(game.mode==ActionGame.Mode.CAMP){
            if(showTalents){
                if(inside(x,y,855,15,88,45)){showTalents=false;return;}
                if(inside(x,y,700,475,210,42)){host.resetTalents();return;}
                for(int i=0;i<12;i++){float[] a=nodeRect(i);if(inside(x,y,a[0],a[1],a[2],a[3])){host.talent(i);return;}}return;
            }
            if(inside(x,y,40,126,130,32)){customRoster=false;return;}
            if(inside(x,y,185,126,130,32)){customRoster=true;return;}
            for(int i=0;i<3;i++)if(inside(x,y,40+i*300,170,280,165)){if(customRoster)game.selectCustom(i);else game.selectHero(i);host.save();return;}
            if(inside(x,y,40,365,280,57)){game.upgrade(false);host.save();}
            else if(inside(x,y,340,365,280,57)){game.upgrade(true);host.save();}
            else if(inside(x,y,660,365,260,80)){game.start(System.nanoTime());resetInput();host.save();}
            else if(inside(x,y,40,480,100,42))host.slots();else if(inside(x,y,155,480,110,42))host.help();
            else if(inside(x,y,280,480,155,42))host.editCharacter();else if(inside(x,y,450,480,175,42)){showTalents=true;resetInput();}
        }else if(game.mode==ActionGame.Mode.REWARD){for(int i=0;i<3;i++)if(inside(x,y,45+i*300,211,270,165)){game.chooseReward(i);host.save();break;}}
        else if(game.mode==ActionGame.Mode.PAUSED){
            if(inside(x,y,330,205,300,53)){game.resume();resetInput();}
            else if(inside(x,y,330,272,140,50))host.slots();else if(inside(x,y,490,272,140,50))soundOn=!soundOn;
            else if(inside(x,y,330,337,300,50))host.confirmAbandon();else if(inside(x,y,330,403,300,45))host.help();
        }else if(game.mode==ActionGame.Mode.RESULT&&inside(x,y,340,359,280,65)){game.returnCamp();host.save();}
    }
    @Override public boolean onTouchEvent(android.view.MotionEvent event){
        if(!isEnabled())return true;
        int action=event.getActionMasked(),index=event.getActionIndex(),id=event.getPointerId(index);
        float x=(event.getX(index)-offsetX)/scale,y=(event.getY(index)-offsetY)/scale;
        if(action==MotionEvent.ACTION_CANCEL){resetInput();return true;}
        if(action==MotionEvent.ACTION_DOWN||action==MotionEvent.ACTION_POINTER_DOWN){
            if(game.mode!=ActionGame.Mode.PLAY){uiTap(x,y);performClick();return true;}
            if(inside(x,y,855,15,88,45)){game.pause();resetInput();host.save();return true;}
            if(x<300&&y>300&&movePointer<0){movePointer=id;joystick(x,y);}
            else if(ActionGame.len(x-850,y-420)<67){attackPointer=id;input.attack=true;}
            else if(ActionGame.len(x-737,y-456)<45)game.dodge(input.mx,input.my);
            else if(ActionGame.len(x-855,y-298)<46)game.skill();
            else if(ActionGame.len(x-648,y-456)<35)game.potion();
        }else if(action==MotionEvent.ACTION_MOVE){
            int mi=event.findPointerIndex(movePointer);if(mi>=0)joystick((event.getX(mi)-offsetX)/scale,(event.getY(mi)-offsetY)/scale);
            int ai=event.findPointerIndex(attackPointer);if(ai>=0)input.attack=ActionGame.len((event.getX(ai)-offsetX)/scale-850,(event.getY(ai)-offsetY)/scale-420)<95;
        }else if(action==MotionEvent.ACTION_UP||action==MotionEvent.ACTION_POINTER_UP){
            if(id==movePointer){movePointer=-1;joyX=0;joyY=0;input.mx=0;input.my=0;}if(id==attackPointer){attackPointer=-1;input.attack=false;}performClick();
        }
        return true;
    }
    private void joystick(float x,float y){float dx=(x-117)/55,dy=(y-423)/55,d=ActionGame.len(dx,dy);if(d<.12f){dx=0;dy=0;}else if(d>1){dx/=d;dy/=d;}joyX=dx;joyY=dy;input.mx=dx;input.my=dy;}
    @Override public boolean performClick(){super.performClick();return true;}
    public float screenX(float x){return offsetX+x*scale;}public float screenY(float y){return offsetY+y*scale;}
}
