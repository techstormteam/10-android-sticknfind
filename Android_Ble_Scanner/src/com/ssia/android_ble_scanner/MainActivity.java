package com.ssia.android_ble_scanner;

import java.util.Collections;
import java.util.Vector;

import com.ssia.sticknfind.sdk.LeDevice;
import com.ssia.sticknfind.sdk.LeSnfDevice;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.Dialog;
import android.app.ActivityManager.RunningServiceInfo;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Toast;

/**
 * An example full-screen activity that shows and hides the system UI (i.e.
 * status bar and navigation/system bar) with user interaction.
 * 
 * @see SystemUiHider
 */
public class MainActivity extends Activity {

	static private String TAG = "MainActivity";

    public static boolean  mActivityIsVisible = false;
    
    ListView listView = null;
    UserArrayAdapter listAdapter = null;
	Button forceClose = null;
	
	long updateWaitTime = -1;
	
	FirmwareUpdateDialog firmwareDialog;
	private Dialog mFwQuestion = null;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.activity_main_activity);

		Singleton.uiContext=this;
		
		forceClose = (Button) this.findViewById(R.id.force_close);
		forceClose.setOnClickListener(new OnClickListener(){

			@Override
			public void onClick(View arg0) {
				send(Message.obtain(null, Singleton.MH_MSG_STOP_SCAN, null));
				new AlertDialog.Builder(arg0.getContext())
            	.setTitle("Force Stop")
         	    .setMessage("Are you sure?")
             	.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
         	        public void onClick(DialogInterface dialog, int which) {
         	        	send(Message.obtain(null, Singleton.MH_MSG_MSG_DESTROY, null));
         	        }
         	     })
        	     .setNegativeButton("No", new DialogInterface.OnClickListener() {
        	        public void onClick(DialogInterface dialog, int which) {
        	        	dialog.cancel();
        	        	send(Message.obtain(null, Singleton.MH_MSG_START_SCAN, null));
        	        }
        	     })
         	     .show();
			}
			
		});

        listView = (ListView) this.findViewById(R.id.listView);

        listAdapter= new UserArrayAdapter(this, new Vector<LeDevice>());
		listView.setAdapter(listAdapter);
		
		int retry = 1;
		int timeout = 30;
		while (!BluetoothAdapter.getDefaultAdapter().isEnabled()){
			if (--timeout == 0){
				break;
			}
			if (--retry == 0){
				retry = 10;
				BluetoothAdapter.getDefaultAdapter().enable();
			}
			try 
			{
				Thread.sleep(1000);				
			}catch (Exception e){}
		}

		if (!BluetoothAdapter.getDefaultAdapter().isEnabled())
			Toast.makeText(Singleton.uiContext, "Please enable bluetooth manual", Toast.LENGTH_LONG).show();
		
		Log.i(TAG, "Initial Services");
		mActivityIsVisible = true;
		Intent serviceClass = new Intent(this, MainManager.class);
		this.startService(serviceClass);

    	ActivityManager manager = (ActivityManager) this.getSystemService(Context.ACTIVITY_SERVICE);
        
    	for (RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (MainManager.class.getName().equals(service.service.getClassName())) {
            	this.bindService(new Intent(this, MainManager.class), mConnection, Context.BIND_AUTO_CREATE);
                mIsBound = true;
            }
        }
	}
	
	public void showFirmwareUpdateQuestion(final LeSnfDevice dev){
		try{
			Singleton.uiContext.runOnUiThread(new Runnable() {
	            public void run(){
	            	try{
		            	mFwQuestion = new AlertDialog.Builder(Singleton.uiContext)
		            	.setTitle(dev.mName + " - Update")
		         	    .setMessage("This sticker requires a firmware update that should take about 3 minutes. Updating your sticker to the latest firmware is necessary for all the features to work properly. Proceed?")
		             	.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
		         	        public void onClick(DialogInterface dialog, int which) {
		         	        	dev.startFirmwareUpdate();
		         	        	dialog.cancel();
		         	        }
		         	     })
		        	     .setNegativeButton("No", new DialogInterface.OnClickListener() {
		        	        public void onClick(DialogInterface dialog, int which) {
		        	        	dev.onCharDiscoveryDone(true);
		        	        	dialog.cancel();
		        	        }
		        	     })
		         	     .show();
	            	}catch(Exception e){}
	            }
	        });
		}catch(Exception e){}
    }
    
    public void cancelFirmwareUpdateQuestion(){
    	mFwQuestion.cancel();
    }
		
	public void showUpdateDialog(final LeSnfDevice dev, boolean show){
		try{
			if (show){
				Singleton.uiContext.runOnUiThread(new Runnable() {
		            public void run(){
		            	try{
		            		 firmwareDialog = new FirmwareUpdateDialog(Singleton.uiContext, dev);
			            	 firmwareDialog.show();
		            	}catch(Exception e){}
		            }
				});
			}
		}catch(Exception e){}
	}
	
	public void setProgressUpdateDialog(final LeSnfDevice dev, final float mFirmwareProgress){
		if (firmwareDialog == null) return;
		if (!firmwareDialog.isShowing()) return;
		try{
			Singleton.uiContext.runOnUiThread(new Runnable() {
	            public void run(){
	            	try{
	            		firmwareDialog.changeProgess(dev, mFirmwareProgress);
	            	}catch(Exception e){}
	            }
			});
		}catch(Exception e){}
	}
	
	public void keepScreenOn(final boolean enable){
		Singleton.uiContext.runOnUiThread(new Runnable() {
            public void run(){
				if (enable)
					getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
				else
					getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
            }
		});
	}

	public void updateConnectionState(final LeDevice dev){
		Log.i(TAG, "devState: " + dev.mName);
        runOnUiThread(new Runnable() {
            public void run() {
				listAdapter.notifyDataSetChanged();
            }
        });
	}
	
	public void updateBeacon1List(final LeDevice dev){
		Log.i(TAG, "Beacon: " + dev.mName + " :: " + dev.mRssi);
        runOnUiThread(new Runnable() {
            public void run() {
				if (listAdapter.getPosition(dev) >= 0){
					if (updateWaitTime > System.currentTimeMillis()) return;
					updateWaitTime = System.currentTimeMillis() + 2500;
					listAdapter.sort(new ListComparator());
					listAdapter.notifyDataSetChanged();
				}else if (MainManager.mBeaconMap.get(dev) != null){
					listAdapter.add(dev);
					if (updateWaitTime > System.currentTimeMillis()) return;
					updateWaitTime = System.currentTimeMillis() + 2500;
					listAdapter.sort(new ListComparator());
				}
            }
        });
	}
	
	public void updateStickerList(final LeDevice dev){
		Log.i(TAG, "Sticker: " + dev.mName + " :: " + dev.mRssi);
        runOnUiThread(new Runnable() {
            public void run() {
				if (listAdapter.getPosition(dev) >= 0){
					if (updateWaitTime > System.currentTimeMillis()) return;
					updateWaitTime = System.currentTimeMillis() + 2500;
					listAdapter.sort(new ListComparator());
					listAdapter.notifyDataSetChanged();
				}else{
					listAdapter.add(dev);
					if (updateWaitTime > System.currentTimeMillis()) return;
					updateWaitTime = System.currentTimeMillis() + 2500;
					listAdapter.sort(new ListComparator());
				}
            }
        });
	}
	
	static public void showBtConnectionProblem(){
		Log.i(TAG, "BtConnectionProblem");
		try{
			if (mActivityIsVisible){
				Singleton.uiContext.runOnUiThread(new Runnable() {
		            public void run(){
		            	new AlertDialog.Builder(Singleton.uiContext)
		        	    .setTitle("Bluetooth Restart")
		        	    .setMessage("Back soon")
		        	    .setNeutralButton("Close", new DialogInterface.OnClickListener() {
		        	        public void onClick(DialogInterface dialog, int which) {
		        	        	dialog.cancel();
		        	        }
		        	     })
		        	     .show();
		            }
		        });
			}
		}catch(Exception e){}
	}
	
	static public void showSdkSupportProblem(){
		Log.i(TAG, "SdkSupportProblem");
		try{
			if (mActivityIsVisible){
				Singleton.uiContext.runOnUiThread(new Runnable() {
		            public void run(){
		            	new AlertDialog.Builder(Singleton.uiContext)
		            	.setTitle("Unsupported Phone Model")
		         	    .setMessage("Unfortunately, your phone build is not supported! Build.Model: " + Build.MODEL)
		             	.setPositiveButton("Close", new DialogInterface.OnClickListener() {
		         	        public void onClick(DialogInterface dialog, int which) {
		         	        	Singleton.uiContext.finish();
		         	        	dialog.cancel();
		         	        }
		         	     })
		         	     .show();
		            }
		        });
			}
		}catch(Exception e){}
	}
	
	private static boolean mIsBound;
    private static Messenger mService = null;
    static Vector<Message> msgList=new Vector<Message>();

    private final Messenger mMessenger = new Messenger(new IncomingHandler());

    private static class IncomingHandler extends Handler {
        @Override
        public synchronized void handleMessage(Message msg) {
        	if (msg.what == Singleton.MSG_REGISTER_SUCCESS){
                Log.i(TAG, "Send missing messages");
                for (Message msgDev: msgList){
                	send(msgDev);
                }
            	msgList.removeAllElements();
        	}else{
                Singleton.incomingMessage(msg);
         	}
        }
    }
    
    public synchronized static void send(Message msg) {
    	try{
	        if ((mIsBound) && (mService != null)) {
                mService.send(msg);
	        }else if (msgList != null){
	        	msgList.add(msg);
	        }
    	}catch (RemoteException e){
    		Log.e(TAG, "Send Message: " + e.toString());
    	}
    }

    private ServiceConnection mConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName className, IBinder service) {
            mService = new Messenger(service);
            Log.i(TAG, "Attached.");
            try {
                Message msg = Message.obtain(null, Singleton.MSG_REGISTER_CLIENT);
                msg.replyTo = mMessenger;
                mService.send(msg);
            } catch (RemoteException e) {}
        }

        public void onServiceDisconnected(ComponentName className) {
            mService = null;
            Log.i(TAG, "Disconnected.");
        }
    };
    
    private void doUnbindService() {
        if (mIsBound) {
            if (mService != null) {
                try {
                    Message msg = Message.obtain(null, Singleton.MSG_UNREGISTER_CLIENT);
                    msg.replyTo = mMessenger;
                    mService.send(msg);
                } catch (RemoteException e) {}
            }

            this.unbindService(mConnection);
            mIsBound = false;
            Log.i(TAG, "Unbinding.");
        }
    }
    
    @Override
	public void onResume(){
		super.onResume();
		mActivityIsVisible = true;
	}
	
    @Override
	public void onPause(){
		super.onPause();
		mActivityIsVisible = false;
	}
	
    @Override
	public void onDestroy(){
		super.onDestroy();
		mActivityIsVisible = false;
		doUnbindService();
	}

}
