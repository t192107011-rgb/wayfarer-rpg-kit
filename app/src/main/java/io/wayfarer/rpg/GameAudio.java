package io.wayfarer.rpg;
import android.content.Context;
import android.media.*;
import java.util.*;
/** Short original generated PCM effects; no microphone or network access. */
final class GameAudio {
    private final SoundPool pool;
    private final Map<String,Integer> sounds=new HashMap<>();
    private final Set<Integer> ready=new HashSet<>();
    private long lastHit=0;
    GameAudio(Context c){
        pool=new SoundPool.Builder().setMaxStreams(6).setAudioAttributes(new AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_GAME).setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build()).build();
        pool.setOnLoadCompleteListener((p,id,status)->{if(status==0)ready.add(id);});
        sounds.put("swing",pool.load(c,R.raw.swing,1));sounds.put("bow",pool.load(c,R.raw.bow,1));sounds.put("hit",pool.load(c,R.raw.hit,1));sounds.put("hurt",pool.load(c,R.raw.hurt,1));sounds.put("roll",pool.load(c,R.raw.roll,1));sounds.put("clear",pool.load(c,R.raw.clear,1));
    }
    void play(String event){
        float rate=event.equals("radiant")?1.25f:event.equals("forbidden")?.7f:1;
        if(event.equals("radiant")||event.equals("lance"))event="clear";else if(event.equals("forbidden"))event="swing";
        if(event.equals("skill"))event="swing";else if(event.equals("victory"))event="clear";
        if(event.equals("hit")){long now=android.os.SystemClock.uptimeMillis();if(now-lastHit<45)return;lastHit=now;}
        Integer id=sounds.get(event);if(id!=null&&ready.contains(id))pool.play(id,.45f,.45f,1,0,rate);
    }
    void close(){pool.release();ready.clear();}
}
