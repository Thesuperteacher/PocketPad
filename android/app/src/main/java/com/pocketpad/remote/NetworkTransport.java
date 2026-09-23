package com.pocketpad.remote;

import android.net.Uri;
import org.json.JSONObject;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.security.cert.X509Certificate;
import java.util.concurrent.*;
import javax.net.ssl.*;

final class NetworkTransport implements Transport {
    interface Listener { void status(String text); default void focus(boolean editable){} }
    private final ArrayBlockingQueue<String> queue=new ArrayBlockingQueue<>(256);
    private volatile boolean open=true,ready=false;
    private volatile SSLSocket socket;
    private final Listener listener;
    NetworkTransport(String pairing,boolean usb,Listener listener) {
        this.listener=listener;
        new Thread(()->run(pairing,usb),"PocketPad connection").start();
    }
    private void run(String pairing,boolean usb) {
        try {
            Uri uri=Uri.parse(pairing.trim());
            String fp=uri.getQueryParameter("fp"),token=uri.getQueryParameter("key"),host=uri.getHost();
            if(!"pocketpad".equals(uri.getScheme())||host==null||fp==null||!fp.matches("[0-9a-f]{64}")||token==null||!token.matches("[0-9a-f]{48}")||uri.getPort()!=19876) throw new IOException("Scan the QR code from the PC companion.");
            final String expected=fp;
            TrustManager[] managers={new X509TrustManager(){
                public X509Certificate[] getAcceptedIssuers(){return new X509Certificate[0];}
                public void checkClientTrusted(X509Certificate[] chain,String auth) throws java.security.cert.CertificateException {throw new java.security.cert.CertificateException("Not a server");}
                public void checkServerTrusted(X509Certificate[] chain,String auth) throws java.security.cert.CertificateException {
                    try { if(chain.length==0||!MessageDigest.isEqual(hex(MessageDigest.getInstance("SHA-256").digest(chain[0].getEncoded())).getBytes(StandardCharsets.US_ASCII),expected.getBytes(StandardCharsets.US_ASCII))) throw new java.security.cert.CertificateException("PC identity changed. Scan its new code."); }
                    catch(GeneralSecurityException e){throw new java.security.cert.CertificateException(e);}
                }
            }};
            SSLContext context=SSLContext.getInstance("TLS");context.init(null,managers,new SecureRandom());
            socket=(SSLSocket)context.getSocketFactory().createSocket();
            socket.setTcpNoDelay(true);socket.setSoTimeout(8000);
            socket.connect(new InetSocketAddress(usb?"127.0.0.1":host,19876),6000);socket.startHandshake();
            if(!open)return;
            BufferedWriter writer=new BufferedWriter(new OutputStreamWriter(socket.getOutputStream(),StandardCharsets.UTF_8));
            writer.write(new JSONObject().put("type","auth").put("version",1).put("token",token).toString()+"\n");writer.flush();
            String reply=readLine(socket.getInputStream());JSONObject result=new JSONObject(reply);
            if(!result.optBoolean("ok"))throw new IOException(result.optString("error","PC refused connection"));
            ready=true;listener.status("Connected · "+((usb||"127.0.0.1".equals(host))?"USB cable":"Wi-Fi / tethering"));
            // Reader observes a stopped host immediately, even while the touchpad is idle.
            SSLSocket active=socket;active.setSoTimeout(0);
            new Thread(()->{try{while(open){JSONObject message=new JSONObject(readLine(active.getInputStream()));if("focus".equals(message.optString("type")))listener.focus(message.optBoolean("editable",false));}}catch(Exception ignored){}finally{boolean wasConnected=connected();close();if(wasConnected)listener.status("Disconnected · connect again");}},"PocketPad disconnect watcher").start();
            while(open){String line=queue.poll(1,TimeUnit.SECONDS);if(line==null)line="{\"type\":\"ping\"}";writer.write(line);writer.write('\n');writer.flush();}
        } catch(Exception e){if(open)listener.status("Connection failed: "+(e.getMessage()==null?e.getClass().getSimpleName():e.getMessage()));}
        finally {boolean wasReady=ready;close();if(wasReady)listener.status("Disconnected · connect again");}
    }
    static String hex(byte[] bytes){StringBuilder s=new StringBuilder();for(byte b:bytes)s.append(String.format(java.util.Locale.ROOT,"%02x",b&255));return s.toString();}
    static String readLine(InputStream in)throws IOException{ByteArrayOutputStream out=new ByteArrayOutputStream();for(int i=0;i<4096;i++){int b=in.read();if(b<0)throw new EOFException("PC closed connection");if(b==10)return out.toString("UTF-8");out.write(b);}throw new IOException("Invalid PC response");}
    public boolean connected(){return ready&&open;}
    public void send(JSONObject event){if(connected()&&!queue.offer(event.toString())){listener.status("Connection too slow · reconnect");close();}}
    public void close(){open=false;ready=false;queue.clear();SSLSocket s=socket;if(s!=null)try{s.close();}catch(IOException ignored){}}
}
