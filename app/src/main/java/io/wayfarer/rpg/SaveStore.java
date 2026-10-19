package io.wayfarer.rpg;

import android.content.Context;
import android.util.AtomicFile;
import io.wayfarer.rpg.core.GameEngine;
import java.io.*;

/** Small, synchronous snapshots (under 64 KiB); AtomicFile recovers interrupted writes. */
final class SaveStore {
    private final File directory;
    SaveStore(Context context) { directory=new File(context.getFilesDir(),"saves"); }
    private AtomicFile slot(int slot) throws IOException {
        if(slot<0 || slot>3) throw new IOException("未知存档槽");
        if(!directory.isDirectory() && !directory.mkdirs()) throw new IOException("无法创建存档目录");
        return new AtomicFile(new File(directory,"slot-"+slot+".properties"));
    }
    boolean exists(int index) {
        File base=new File(directory,"slot-"+index+".properties");
        return base.exists() || new File(base.getPath()+".bak").exists();
    }
    void write(int index,GameEngine game) throws IOException {
        byte[] bytes=game.encode();
        GameEngine.decode(bytes); // Never replace a valid snapshot with invalid engine state.
        AtomicFile file=slot(index);FileOutputStream out=null;
        try {out=file.startWrite();out.write(bytes);file.finishWrite(out);}
        catch(IOException e) {if(out!=null) file.failWrite(out);throw e;}
    }
    GameEngine read(int index) throws IOException {
        try(InputStream in=slot(index).openRead();ByteArrayOutputStream out=new ByteArrayOutputStream()) {
            byte[] buffer=new byte[2048];int n;
            while((n=in.read(buffer))!=-1) {if(out.size()+n>65536) throw new IOException("存档过大");out.write(buffer,0,n);}
            return GameEngine.decode(out.toByteArray());
        }
    }
}
