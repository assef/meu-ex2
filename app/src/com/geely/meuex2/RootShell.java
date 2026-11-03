package com.geely.meuex2;

import android.util.Log;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;

/** Executa comandos como ROOT falando com o adbd local (127.0.0.1:5555), que roda como root.
 *  Portado do mecanismo do Sapo (adbd é auth-free neste firmware — sem chave). */
public class RootShell {
    static final String T="MeuEX2";
    static final int PORT=5555;
    // comandos ADB (little-endian)
    static final int A_CNXN=0x4e584e43, A_AUTH=0x48545541, A_OPEN=0x4e45504f,
                     A_OKAY=0x59414b4f, A_WRTE=0x45545257, A_CLSE=0x45534c43;

    static class Msg { int cmd, arg0, arg1; byte[] data; }

    static Msg read(DataInputStream in) throws IOException {
        byte[] h=new byte[24]; in.readFully(h);
        ByteBuffer b=ByteBuffer.wrap(h).order(ByteOrder.LITTLE_ENDIAN);
        int cmd=b.getInt(), a0=b.getInt(), a1=b.getInt(), len=b.getInt(), crc=b.getInt(), magic=b.getInt();
        if((~cmd)!=magic) throw new IOException("cabecalho ADB invalido");
        byte[] d=new byte[len]; if(len>0) in.readFully(d);
        int sum=0; for(int i=0;i<len;i++) sum+=d[i]&255;
        if(sum!=crc) throw new IOException("checksum ADB invalido");
        Msg m=new Msg(); m.cmd=cmd; m.arg0=a0; m.arg1=a1; m.data=d; return m;
    }
    static void write(OutputStream out,int cmd,int a0,int a1,byte[] data) throws IOException {
        ByteBuffer b=ByteBuffer.allocate(24).order(ByteOrder.LITTLE_ENDIAN);
        b.putInt(cmd); b.putInt(a0); b.putInt(a1); b.putInt(data.length);
        int sum=0; for(byte x:data) sum+=x&255;
        b.putInt(sum); b.putInt(~cmd);
        out.write(b.array()); out.write(data); out.flush();
    }
    static byte[] nz(String s){ byte[] x=s.getBytes(StandardCharsets.UTF_8); byte[] r=new byte[x.length+1]; System.arraycopy(x,0,r,0,x.length); return r; }

    /** Roda um comando shell como root. Retorna stdout+stderr. timeoutMs total. */
    public static String run(int timeoutMs, String cmd) {
        byte[] payload=nz("shell:"+cmd);
        if(payload.length>4096) return "ERRO: comando > 4096 bytes (use runScript)";
        long deadline=System.currentTimeMillis()+timeoutMs;
        Socket s=new Socket();
        try {
            s.connect(new InetSocketAddress("127.0.0.1",PORT),timeoutMs);
            s.setSoTimeout(timeoutMs);
            DataInputStream in=new DataInputStream(s.getInputStream());
            OutputStream out=s.getOutputStream();
            write(out,A_CNXN,0x01000000,4096,nz("host::central-geely"));
            int c;
            do {
                long left=deadline-System.currentTimeMillis(); if(left<=0) throw new IOException("timeout handshake");
                s.setSoTimeout((int)Math.min(left,Integer.MAX_VALUE));
                c=read(in).cmd;
                if(c==A_AUTH) throw new IOException("adbd pediu autenticacao (inesperado)");
            } while(c!=A_CNXN);
            write(out,A_OPEN,1,0,payload);
            ByteArrayOutputStream bos=new ByteArrayOutputStream();
            int remote=0; boolean have=false;
            while(true){
                long left=deadline-System.currentTimeMillis(); if(left<=0) throw new IOException("timeout comando");
                s.setSoTimeout((int)Math.min(left,Integer.MAX_VALUE));
                Msg m=read(in);
                if(m.cmd==A_AUTH) throw new IOException("adbd pediu autenticacao");
                if(m.cmd==A_OKAY){ if(m.arg1==1){ have=true; remote=m.arg0; } }
                else if(m.cmd==A_WRTE){ if(!have){ have=true; remote=m.arg0; } bos.write(m.data); write(out,A_OKAY,1,m.arg0,new byte[0]); }
                else if(m.cmd==A_CLSE){ write(out,A_CLSE,1,remote,new byte[0]); s.close(); return bos.toString("UTF-8").trim(); }
                else throw new IOException("msg ADB inesperada 0x"+Integer.toHexString(m.cmd));
            }
        } catch(Throwable t){ Log.w(T,"RootShell: "+t.getMessage()); try{s.close();}catch(Throwable e){} return "ERRO: "+t.getMessage(); }
    }

    /** true se o adbd local responde e temos root (uid=0). */
    public static boolean available(){
        String r=run(4000,"id -u"); return "0".equals(r.trim());
    }
}
