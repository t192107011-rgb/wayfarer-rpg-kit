package io.wayfarer.rpg;

import android.content.Context;
import android.graphics.*;
import android.view.View;
import io.wayfarer.rpg.core.*;

/** Original procedural art; substitute sprite drawing here without changing the rules. */
final class WorldView extends View {
    private final Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG);
    private GameEngine game;
    WorldView(Context context) {super(context);setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_YES);}
    void show(GameEngine game) {
        this.game=game;
        setContentDescription(game.inBattle()?"战斗："+game.enemy().name+"，敌方生命 "+game.enemyHp:game.map().name+"，位置 "+game.x+", "+game.y+"。使用下方方向按钮移动。");
        invalidate();
    }
    @Override protected void onMeasure(int ws,int hs) {
        int w=MeasureSpec.getSize(ws);setMeasuredDimension(w,resolveSize(Math.round(w*9f/13f),hs));
    }
    private void fill(Canvas c,int color,float l,float t,float r,float b) {paint.setColor(color);c.drawRect(l,t,r,b,paint);}
    private void label(Canvas c,String text,float x,float y,float size,int color) {
        paint.setColor(color);paint.setTextSize(size);paint.setTypeface(Typeface.create("sans-serif-medium",Typeface.NORMAL));paint.setTextAlign(Paint.Align.CENTER);c.drawText(text,x,y,paint);
    }
    @Override protected void onDraw(Canvas c) {
        super.onDraw(c);if(game==null)return;
        if(game.inBattle()) {battle(c);return;}
        GameData.Area map=game.map();float tile=getWidth()/(float)map.width();
        for(int y=0;y<map.height();y++) for(int x=0;x<map.width();x++) {
            char v=map.tiles[y].charAt(x);float l=x*tile,t=y*tile;
            int color=game.area.equals("ruins")?Color.rgb(54,65,69):Color.rgb(49,83,71);
            if(v=='#') color=Color.rgb(24,46,43);
            else if(v=='~') color=Color.rgb(42,85,104);
            else if(v==',') color=Color.rgb(139,128,97);
            else if((x+y)%2==0) color=game.area.equals("ruins")?Color.rgb(60,72,75):Color.rgb(53,89,75);
            fill(c,color,l,t,l+tile,t+tile);
            if(v=='#') {
                paint.setColor(Color.rgb(39,65,56));
                c.drawCircle(l+tile*.5f,t+tile*.45f,tile*.36f,paint);
                fill(c,Color.rgb(34,54,45),l+tile*.43f,t+tile*.5f,l+tile*.57f,t+tile*.95f);
            } else if(v=='~') fill(c,Color.rgb(71,119,133),l+tile*.2f,t+tile*.5f,l+tile*.7f,t+tile*.54f);
            else if(v=='.') fill(c,Color.rgb(76,105,83),l+tile*.22f,t+tile*.72f,l+tile*.28f,t+tile*.84f);
        }
        for(GameData.Event e:map.events) {
            if(!game.visible(e))continue;
            float cx=(e.x+.5f)*tile,cy=(e.y+.5f)*tile;
            if(e.kind.equals("enemy")) {
                paint.setColor(e.target.equals("warden")?Color.rgb(182,108,98):Color.rgb(173,121,101));
                c.drawRoundRect(cx-tile*.32f,cy-tile*.25f,cx+tile*.32f,cy+tile*.32f,tile*.15f,tile*.15f,paint);
                label(c,"!",cx,cy+tile*.16f,tile*.5f,Color.rgb(38,32,32));
            } else if(e.kind.equals("portal")) {
                paint.setColor(Color.rgb(207,223,155));c.drawCircle(cx,cy,tile*.27f,paint);
                label(c,e.id.equals("f_v")||e.id.equals("r_f")?"‹":"›",cx,cy+tile*.16f,tile*.55f,Color.rgb(33,66,59));
            } else if(e.kind.equals("chest")) {
                fill(c,Color.rgb(200,159,89),cx-tile*.32f,cy-tile*.2f,cx+tile*.32f,cy+tile*.28f);
                fill(c,Color.rgb(255,220,128),cx-tile*.05f,cy-tile*.2f,cx+tile*.05f,cy+tile*.28f);
            } else {
                paint.setColor(Color.rgb(202,185,144));c.drawCircle(cx,cy-tile*.17f,tile*.17f,paint);
                fill(c,Color.rgb(88,136,151),cx-tile*.23f,cy,cx+tile*.23f,cy+tile*.33f);
                label(c,e.kind.equals("elder")?"长":e.kind.equals("shop")?"商":"宿",cx,cy+tile*.2f,tile*.3f,Color.WHITE);
            }
        }
        float cx=(game.x+.5f)*tile,cy=(game.y+.5f)*tile;
        paint.setColor(Color.rgb(12,32,29));c.drawOval(cx-tile*.32f,cy+tile*.23f,cx+tile*.32f,cy+tile*.4f,paint);
        paint.setColor(Color.rgb(232,212,165));c.drawCircle(cx,cy-tile*.2f,tile*.2f,paint);
        Path cape=new Path();cape.moveTo(cx-tile*.18f,cy-tile*.06f);cape.lineTo(cx+tile*.2f,cy-tile*.06f);cape.lineTo(cx+tile*.33f,cy+tile*.34f);cape.lineTo(cx-tile*.3f,cy+tile*.34f);cape.close();
        paint.setColor(Color.rgb(213,232,160));c.drawPath(cape,paint);
    }
    private void battle(Canvas c) {
        float w=getWidth(),h=getHeight();
        fill(c,Color.rgb(25,44,48),0,0,w,h);
        paint.setColor(Color.rgb(35,59,61));c.drawCircle(w*.5f,h*.48f,h*.38f,paint);
        fill(c,Color.rgb(44,65,61),0,h*.8f,w,h);
        boolean boss=game.enemy().id.equals("warden");
        paint.setColor(boss?Color.rgb(161,120,105):Color.rgb(115,164,139));
        c.drawRoundRect(w*.37f,h*.32f,w*.63f,h*.7f,h*.08f,h*.08f,paint);
        fill(c,Color.rgb(226,226,161),w*.42f,h*.43f,w*.46f,h*.48f);
        fill(c,Color.rgb(226,226,161),w*.54f,h*.43f,w*.58f,h*.48f);
        label(c,game.enemy().name,w*.5f,h*.18f,h*.065f,Color.rgb(237,231,210));
        fill(c,Color.rgb(66,69,66),w*.25f,h*.77f,w*.75f,h*.8f);
        fill(c,Color.rgb(215,151,127),w*.25f,h*.77f,w*(.25f+.5f*game.enemyHp/game.enemy().hp),h*.8f);
        label(c,game.enemyHp+" / "+game.enemy().hp,w*.5f,h*.9f,h*.055f,Color.rgb(224,226,210));
        label(c,(game.turn+1)%3==0?"敌人下一回合将使用重击":"每第三回合重击 · 防御可减伤",w*.5f,h*.98f,h*.04f,Color.rgb(179,198,184));
    }
}
