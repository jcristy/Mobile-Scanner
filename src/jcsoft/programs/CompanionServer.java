package jcsoft.programs;

import java.io.BufferedInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.Enumeration;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;

public class CompanionServer extends Activity 
{
	 public void onCreate(Bundle savedInstanceState) 
	 {
		 super.onCreate(savedInstanceState);
		 setContentView(R.layout.companionserver);
		 Thread t = new Thread(new Runnable(){

			@Override
			public void run() {
				try {
					ServerSocket ss = new ServerSocket(8080);
					Socket s = ss.accept();
					DataOutputStream dos = new DataOutputStream(s.getOutputStream());
					
					long length = getAssets().openFd("jbluetoothscanner.zip").getLength();
					
					
					dos.write("HTTP/1.0 200 OK\r\n".getBytes());
					dos.write(("Date: Fri, 31 Dec 1999 23:59:59 GMT\r\n").getBytes());
					dos.write("Content-Type: binary/octet-stream\r\n".getBytes());
					//dos.write("Content-Type: text/txt\r\n".getBytes());
					dos.write("Content-Disposition: attachment; filename=jbluetoothscanner.zip\r\n".getBytes());
					dos.write(("Content-Length: "+length+"\r\n").getBytes());
					//dos.write(("Content-Length: "+("Good!".getBytes().length)+"\r\n").getBytes());
					dos.write("\r\n".getBytes());

					dos.flush();
					BufferedInputStream bis = new BufferedInputStream(getAssets().open("jbluetoothscanner.zip"));
					for (int i=0; i<length;i++)
						dos.write(bis.read());
					//s.close();
					//ss.close();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				
			}
			 
		 });
		 t.start();
		 TextView serverinfo = (TextView)findViewById(R.id.serverinfo);
		 String ipaddress = getIPAddress();
		 serverinfo.setText("Point browser to "+ipaddress+":8080 (must be an address reachable by your computer)"); 
		 
	 }
	 public String getIPAddress()
	 {
		 try {
             for (Enumeration<NetworkInterface> en = NetworkInterface.getNetworkInterfaces(); en.hasMoreElements();) {
                 NetworkInterface intf = en.nextElement();
                 for (Enumeration<InetAddress> enumIpAddr = intf.getInetAddresses(); enumIpAddr.hasMoreElements();) {
                     InetAddress inetAddress = enumIpAddr.nextElement();
                     if (!inetAddress.isLoopbackAddress()) {
                         return inetAddress.getHostAddress().toString();
                     }
                 }
             }
         } catch (SocketException ex) {
             Log.i("externalip", ex.toString());
         }
		 return "your phone @ port";
	 }
}
