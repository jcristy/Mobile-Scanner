package jcsoft.programs;

import java.io.DataInputStream;

import java.io.DataOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.hardware.Camera;
import android.hardware.Camera.AutoFocusCallback;
import android.hardware.Camera.PictureCallback;
import android.hardware.Camera.ShutterCallback;
import android.hardware.Camera.Size;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.widget.Toast;

public class BluetoothScannerActivity extends Activity {
    /** Called when the activity is first created. */
	BluetoothAdapter bluetoothAdapt;
	CameraPreview cp;
	public final static String TAG = "BluetoothScanner"; 
    @Override
    public void onCreate(Bundle savedInstanceState) 
    {
        super.onCreate(savedInstanceState);
        
        //cam = getCameraInstance();
    	//CameraPreview cp = new CameraPreview(this,cam);
    	//
    	//FrameLayout preview = (FrameLayout)findViewById(R.id.campreview);
        //cam.startPreview();
        //Log.d(TAG,"Started Preview");
    	//preview.addView(cp);
		
        /*((TextView)findViewById(R.id.message)).setText(bluetoothAdapt.getName()+" : "+bluetoothAdapt.getAddress());
        findViewById(R.id.take_picture).setOnClickListener(new OnClickListener(){
			@Override
			public void onClick(View v) {
				
			}
        });*/
    }
    protected void onPause()
    {
    	super.onPause();
    	if (cam!=null) {
    		
    		cam.stopPreview();
    		cam.setPreviewCallback(null);
    		cam.release();
    		cam = null;
    		Toast.makeText(this, "Camera Released", Toast.LENGTH_SHORT).show();
    	}
    }
    protected void onResume()
    {
    	super.onResume();
    	setContentView(R.layout.main); 
    	Log.d(TAG,"OnResume");
    	cam = getCameraInstance();
    	cp = new CameraPreview(this,cam);
    	FrameLayout preview = (FrameLayout)findViewById(R.id.campreview);
        cam.startPreview();
        preview.addView(cp);
        
        bluetoothAdapt = BluetoothAdapter.getDefaultAdapter();
        Log.d(TAG,"We have the bt adapter");
        
        Log.d(TAG,"Added Preview");
    	Thread connection = new Thread(new dirtyWork());
		connection.start();
		Log.d(TAG,"Started Bluetooth Thread");
		
		
    }
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.mainmenu, menu);
        return true;
    }
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle item selection
        switch (item.getItemId()) {
            case R.id.servewindowsapp:
            	Intent intent = new Intent(this, CompanionServer.class);
            	startActivity(intent);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }
    public Object WaitForRequests = new Object();
    public class dirtyWork implements Runnable
    {
    	
    	public dirtyWork()
    	{
    		
    	}
		public void run() 
		{
			if (bluetoothAdapt == null)
			{
				Toast.makeText(BluetoothScannerActivity.this, "Error: Bluetooth not supported", Toast.LENGTH_SHORT).show();
				return;
			}
			if (!bluetoothAdapt.isEnabled())
			{
			    Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
			    startActivityForResult(enableBtIntent, 5);
			}
			//Set<BluetoothDevice> devices = bluetoothAdapt.getBondedDevices();
			
			try {
//				((Button)v).setEnabled( false);
				Log.d(TAG,"Set up ServerSocket");
				BluetoothServerSocket bss = bluetoothAdapt.listenUsingRfcommWithServiceRecord("JC's Remote Scanner", UUID.fromString("a637c370-421e-11e1-b86c-0800200c9a66"));
				//BluetoothServerSocket bss2 = bluetoothAdapt.listenUsingRfcommWithServiceRecord("JC's Remote Scanner X Channel", UUID.fromString("a637c370-421e-11e1-b86c-0800200c9a67"));
				while(true)
				{
					Log.d(TAG,"Start Listening");
					bluetoothAdapt.cancelDiscovery();
					BluetoothSocket connection = bss.accept();
					//BluetoothSocket connection2= bss2.accept();
					Log.d(TAG,"Got a connection!");
					InputStream is = connection.getInputStream();
					OutputStream os = connection.getOutputStream();
					//OutputStream os2= connection2.getOutputStream();
					DataInputStream dis = new DataInputStream(is);
					String request = dis.readLine(); 
					Log.d(TAG,"we read a line: "+request);
					//TextView results = ((TextView)(BluetoothScannerActivity.this.findViewById(R.id.message)));
					DataOutputStream dos = new DataOutputStream(os);
					//DataOutputStream dos2 = new DataOutputStream(os2);
					dos.writeBytes("Taking Picture\r\n");
					if (cam==null)
					{
						
						dos.writeBytes("-1\r\n");
						
					}
					else
					{
						getImageFromCamera(dos);
						try {
							Log.d(TAG,"Waiting up here");
							synchronized(WaitForRequests){
								WaitForRequests.wait();
							}
						} catch (InterruptedException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
					}
					dis.close();
					dos.close();
					os.close();
					is.close();
					connection.close(); 
					Log.d(TAG,"Connection shut-down!");
				}	
				 
			} catch (IOException e) {
				e.printStackTrace();
			}			
		}    	
    }
    Camera cam;
    
    public void getImageFromCamera(DataOutputStream dos)
    {
    	if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA))
    	{ 
    		Toast.makeText(this, "No Camera", Toast.LENGTH_SHORT).show();
    		return;
    	}
    	Log.d(TAG,"Taking picture NOW");
    	
    	Camera.Parameters parameters = cam.getParameters();
    	
    	parameters.setJpegQuality(90);
    	parameters.setSceneMode(Camera.Parameters.SCENE_MODE_STEADYPHOTO);
    	cam.setParameters(parameters);
    	
    	cam.autoFocus(new OurAutoFocusCallback(dos)); 
    	
    }
    public class OurAutoFocusCallback implements AutoFocusCallback
    {
    	DataOutputStream dos;
    	public OurAutoFocusCallback(DataOutputStream dos)
    	{
    		super();
    		this.dos = dos;
    	}
    	@Override
		public void onAutoFocus(boolean success, Camera camera) 
    	{
    		JpegCallback jpeg = new JpegCallback(dos);
    		
    		Log.d(TAG,"AutoFocus" +(success?" successful":"failed")	+" Complete: Take Picture");
    		cam.takePicture(
        			null, 
        			new PictureCallback(){
    					public void onPictureTaken(byte[] data, Camera camera) 
    					{
    						Log.d(TAG,"Raw data!");
    					}},
        			jpeg);
		}
    }
    
    public class JpegCallback implements PictureCallback
    {
    	public DataOutputStream dos;
    	//public DataOutputStream dos2;
    	public JpegCallback(DataOutputStream writeToThis)
    	{
    		super();
    		dos = writeToThis; 
    		
    	}
    	public void onPictureTaken(byte[] data, Camera camera)
    	{
    		try {
    			Log.d(TAG,"JPEG CALLBACK");
    			Toast.makeText(BluetoothScannerActivity.this, "JPeg Returned!", Toast.LENGTH_SHORT).show();
    			Log.d(TAG,"Size of Data="+data.length);//data.length);
    			dos.write((""+data.length+"\r\n").getBytes());
    			Log.d(TAG,"Size written");
    			//FileOutputStream fos = new FileOutputStream("Johnstest.jpg");
    			Log.d(TAG,data[0]+"  "+data[5]+"  "+data[data.length-1]);
    			//for (int i=0; i<data.length;i++)
    			//for (int i=0; i<5000;i++)
    			{
    				//dos.write(data[i]);
    			
    				dos.write(data);
    			
    				//if (i%10000==0) Log.d(TAG, ""+((double)i)/data.length+"%"); 
    			}
				Log.d(TAG,"It has been written");
				 
			} catch (IOException e) { 
				e.printStackTrace();
				Log.d(TAG,e.getMessage());
			}
    		cam.startPreview();
    		synchronized(WaitForRequests){
				BluetoothScannerActivity.this.WaitForRequests.notify();}
			Log.d(TAG,"Notified WaitForRequests");
    	} 
    };
    public Camera getCameraInstance()
    {
    	Camera c = null;
    	try{
    		c = Camera.open();
    		Log.d(TAG,"We opened the camera");
    	}catch(Exception e){
    		Toast.makeText(this, "Could not get camera", Toast.LENGTH_SHORT).show();
    	}
    	if (c==null) Log.d(TAG,"camera is null still");
    	return c;
    }
}