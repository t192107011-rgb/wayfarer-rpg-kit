package io.wayfarer.rpg.core;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/** Content definitions. Coordinates are zero based; every ID is part of the save format. */
public final class GameData {
    private GameData() {}
    public static final class Enemy {
        public final String id, name;
        public final int hp, attack, defense, xp, gold;
        public Enemy(String id, String name, int hp, int attack, int defense, int xp, int gold) {
            this.id=id; this.name=name; this.hp=hp; this.attack=attack; this.defense=defense; this.xp=xp; this.gold=gold;
        }
    }
    public static final class Event {
        public final String id, kind, target;
        public final int x,y;
        public Event(String id, String kind, int x, int y, String target) {
            this.id=id; this.kind=kind; this.x=x; this.y=y; this.target=target;
        }
    }
    public static final class Area {
        public final String id,name;
        public final String[] tiles;
        public final Event[] events;
        public Area(String id, String name, String[] tiles, Event... events) {
            this.id=id; this.name=name; this.tiles=tiles; this.events=events;
        }
        public int width() { return tiles[0].length(); }
        public int height() { return tiles.length; }
        public boolean walkable(int x,int y) {
            return y>=0 && y<height() && x>=0 && x<width() && tiles[y].charAt(x)!='#' && tiles[y].charAt(x)!='~';
        }
        public Event eventAt(int x,int y) {
            for(Event e:events) if(e.x==x && e.y==y) return e;
            return null;
        }
    }
    public static final Map<String,Enemy> ENEMIES;
    public static final Map<String,Area> AREAS;
    static {
        Map<String,Enemy> enemies=new LinkedHashMap<>();
        enemies.put("slime",new Enemy("slime","苔原软泥",24,7,1,15,12));
        enemies.put("wolf",new Enemy("wolf","灰影狼",36,10,2,24,18));
        enemies.put("warden",new Enemy("warden","古塔守卫",100,16,4,80,90));
        ENEMIES=Collections.unmodifiableMap(enemies);
        Map<String,Area> areas=new LinkedHashMap<>();
        areas.put("village",new Area("village","风栖村",new String[]{
            "#############","#....,,,....#","#....,,,....#","#....,,,....#","#,,,,,,,,,,,#",
            "#....,,,....#","#....,,,....#","#....,,,....#","#############"},
            new Event("elder","elder",3,2,""),new Event("shop","shop",9,2,""),
            new Event("inn","inn",3,6,""),new Event("v_f","portal",11,4,"forest:1:4")));
        areas.put("forest",new Area("forest","回声林地",new String[]{
            "#############","#...#.......#","#...#..~~...#","#......~~...#","#,,,,,,,,,,,#",
            "#...#.......#","#...#.......#","#...........#","#############"},
            new Event("f_v","portal",1,4,"village:10:4"),new Event("f_r","portal",11,4,"ruins:1:4"),
            new Event("slime_a","enemy",4,4,"slime"),new Event("slime_b","enemy",6,4,"slime"),
            new Event("wolf_a","enemy",9,4,"wolf"),new Event("forest_chest","chest",2,1,"")));
        areas.put("ruins",new Area("ruins","灯塔遗迹",new String[]{
            "#############","#.....#.....#","#.....#.....#","#.....#.....#","#,,,,,,,,,,,#",
            "#.....#.....#","#.....#.....#","#...........#","#############"},
            new Event("r_f","portal",1,4,"forest:10:4"),new Event("warden","enemy",9,4,"warden"),
            new Event("ruins_chest","chest",10,6,"")));
        AREAS=Collections.unmodifiableMap(areas);
        validate();
    }
    public static Event event(String id) {
        for(Area a:AREAS.values()) for(Event e:a.events) if(e.id.equals(id)) return e;
        return null;
    }
    public static void validate() {
        java.util.Set<String> ids=new java.util.HashSet<>();
        for(Area a:AREAS.values()) {
            for(String row:a.tiles) if(row.length()!=a.width()) throw new IllegalStateException("Unequal map rows: "+a.id);
            java.util.Set<String> cells=new java.util.HashSet<>();
            for(Event e:a.events) {
                if(!a.walkable(e.x,e.y) || !ids.add(e.id) || !cells.add(e.x+":"+e.y)) throw new IllegalStateException("Invalid event: "+e.id);
                if(e.kind.equals("enemy") && !ENEMIES.containsKey(e.target)) throw new IllegalStateException("Missing enemy");
                if(e.kind.equals("portal")) {
                    String[] p=e.target.split(":");
                    Area destination=AREAS.get(p[0]);
                    if(destination==null || !destination.walkable(Integer.parseInt(p[1]),Integer.parseInt(p[2]))) throw new IllegalStateException("Invalid portal");
                }
            }
        }
    }
}

