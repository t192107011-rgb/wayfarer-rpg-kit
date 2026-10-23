package io.wayfarer.rpg;
import android.content.Context;
import android.util.AtomicFile;
import io.wayfarer.rpg.core.*;
import java.io.*;
final class SaveStore {
    private final File dir;
    SaveStore(Context c){dir=new File(c.getFilesDir(),"action-saves-v1");}
    private AtomicFile slot(int i)throws IOException{if(i<0||i>3)throw new IOException("存档槽无效");if(!dir.isDirectory()&&!dir.mkdirs())throw new IOException("无法创建存档目录");return new AtomicFile(new File(dir,"slot-"+i+".properties"));}
    boolean exists(int i){return new File(dir,"slot-"+i+".properties").exists()||new File(dir,"slot-"+i+".properties.bak").exists();}
    void write(int i,ActionGame g)throws IOException{
        byte[] data=ActionSave.encode(g);ActionSave.decode(data);AtomicFile file=slot(i);FileOutputStream out=null;
        try{out=file.startWrite();out.write(data);file.finishWrite(out);}catch(IOException e){if(out!=null)file.failWrite(out);throw e;}
    }
    ActionGame read(int i)throws IOException{
        try(InputStream in=slot(i).openRead();ByteArrayOutputStream out=new ByteArrayOutputStream()){
            byte[] buf=new byte[2048];int n;while((n=in.read(buf))!=-1){if(out.size()+n>131072)throw new IOException("存档过大");out.write(buf,0,n);}return ActionSave.decode(out.toByteArray());
        }
    }
}
