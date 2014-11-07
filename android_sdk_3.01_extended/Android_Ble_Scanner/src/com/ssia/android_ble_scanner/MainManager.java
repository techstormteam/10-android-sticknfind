package com.ssia.android_ble_scanner;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Hashtable;

import com.ssia.sticknfind.sdk.LeDevice;
import com.ssia.sticknfind.sdk.LeDeviceManager;
import com.ssia.sticknfind.sdk.LeDeviceManagerDelegate;
import com.ssia.sticknfind.sdk.LeSnfDevice;
import com.ssia.sticknfind.sdk.LeSnfDeviceDelegate;
import com.ssia.sticknfind.sdk.LogDelegate;
import com.ssia.sticknfind.sdk.NexusDeviceManager;
import com.ssia.sticknfind.sdk.NexusSnfDevice;

import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothProfile;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.util.Log;
import android.widget.Toast;

public class MainManager extends Service{
	private static String TAG="MainHandler";
	
	static MainManager context;
	static LeDeviceManager leDeviceManager = null;
	
	static protected Hashtable<Object, BeaconData> mBeaconMap = new Hashtable<Object, BeaconData>();

	public MainManager(){
		super();
	}
	
    ///////////////////////////////////////////////////////////////////////////////////
	
	LogDelegate logDelegate = new LogDelegate(LogDelegate.VERBOSE_ALL){
 
		@Override
		public void log(long currentTime, String dateFormat, int pid,
				String tag, String content) {}
		
	};

    LeDeviceManagerDelegate leDeviceManagerDelegate = new LeDeviceManagerDelegate(){

		@Override
		public void didAddNewDevice(LeDeviceManager mgr, LeDevice dev) {
			if (dev.mName==null || dev.mName.length() == 0){
				mgr.mDevList.add(dev);
				dev.mName = findGoodName(mgr);
				try
				{
					((LeSnfDevice)dev).setAuthenticationKey(new byte [] {0,1,2,3,4,5,6,7,8,9,10,11,12,13,14,15});
				}catch (Exception e){};
			}
		}

		@Override
		public void didStartService(LeDeviceManager mgr) {
			try 
			{
				Thread.sleep(3000);				
			}catch (Exception e){}
			mgr.startScan();
		}
    };
    
    LeSnfDeviceDelegate leSnfDeviceDelegate = new LeSnfDeviceDelegate(){

    	@Override
    	public void didReceiveRemoteRssi(LeDevice dev, int rssi, int channel){
    		send(Message.obtain(null, Singleton.MH_MSG_BEACON_UPDATE, dev));
    	}
    	
		@Override
		public void didUpdateBroadcastData(LeDevice dev, int rssi, byte[] scanRecord) {

			BeaconData.FrameInfo frameInfo = BeaconData.searchMdScanRecord(scanRecord);
			if (dev.mRssi != rssi){
				dev.mRssi = rssi;
			}
			if (frameInfo != null){
				if (frameInfo.mType == BeaconData.mAdName){
					if (frameInfo.mOffset == -1)
						((LeSnfDevice)dev).mBroadcastAdTypeRequest = true;
				}else{
					if (mBeaconMap.containsKey(dev)){
						Log.d(TAG, "Found: " + dev.mName + " :: " + dev.getBtDevice().getName());
						((BeaconData)mBeaconMap.get(dev)).updateData(rssi, scanRecord, frameInfo);
					}else{
						Log.d(TAG, "Add: " + dev.mName + " :: " + dev.getBtDevice().getName());
						mBeaconMap.put(dev, new BeaconData(rssi, scanRecord, frameInfo, new byte [0]));
					}
					send(Message.obtain(null, Singleton.MH_MSG_BEACON_UPDATE, dev));
				}
			}else{
				send(Message.obtain(null, Singleton.MH_MSG_STICKER_UPDATE, dev));
			}
		}
		
		@Override
		public void didReadTemperature(LeDevice dev){
			Log.i(TAG, "didReadTemperature: " + dev.mName + " :: " + ((LeSnfDevice)dev).mTemperature);
		}
		
		@Override
		public void didReadBattery(LeDevice dev){
			Log.i(TAG, "didReadBattery: " + dev.mName + " :: " + ((LeSnfDevice)dev).mBatteryLevel);
		}
		
		@Override
		public void didSetAuthenticationKey(LeDevice dev){
			Log.i(TAG, "didSetAuthenticationKey:" + dev.mName);
		}
		
		@Override
		public void didReadRemoteRevision(LeDevice dev){
			Log.i(TAG, "didReadRemoteRevision: " + dev.mName + " :: " + ((LeSnfDevice)dev).mRemoteRevision);
		}
		
		@Override
		public void didReadBroadcastRate(LeDevice dev){
			Log.i(TAG, "didReadBroadcastRate: " + dev.mName + " :: " + ((LeSnfDevice)dev).mBroadcastRate);
		}

		@Override
		public void didDiscoverLeSnfDevice(LeDevice dev) {
			Log.i(TAG, "didDiscoverLeSnfDevice: " + dev.mName);
		}
		
		@Override
		public void didDiscoverLeSnfDevice(LeDevice dev, Object [] list) {
			Log.i(TAG, "didDiscoverLeSnfDevice: " + dev.mName + " :: " + list.toString());
		}

		@Override
		public void didChangeState(LeSnfDevice dev, int state) {
			Log.i(TAG, "didChangeState: " + dev.mName + " :: " + state);
			send(Message.obtain(null, Singleton.MH_MSG_CONNECTIONSTATE, dev));
			if (state == BluetoothProfile.STATE_DISCONNECTED)
				Singleton.uiContext.setProgressUpdateDialog((LeSnfDevice) dev, -2);
		}

		@Override
		public void didSetTemperatureCalibrationForLeSnfDevice(LeDevice dev,
				boolean success) {
			// TODO Auto-generated method stub
			
		}

		@Override
		public void didSetPairingRssiForLeSnfDevice(LeDevice dev,
				boolean success) {
			// TODO Auto-generated method stub
			
		}

		@Override
		public void didSetBroadcastRateForLeSnfDevice(LeDevice dev,
				boolean success) {
			Log.i(TAG, "didSetBroadcastRateForLeSnfDevice: " + dev.mName);
		}

		@Override
		public void didSetBroadcastKeyForLeSnfDevice(LeDevice dev,
				boolean success) {
			Log.i(TAG, "didSetBroadcastKeyForLeSnfDevice: " + dev.mName);	
			if (success){
				byte [] uId = {0x10, 0x11, 0x12, 0x13, 0x14, 0x15, 0x16, 0x23};
				byte [] data = {0x02,0x01,0x06,
						27,0x09,	// the raw packet data  (length, type)
						's','L',	// text starts here
						0x03,0x02,0x24,0x3C,0x28,0x29,0x2A,0x2B,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,
						(byte) 0xAA,0x55, // trailer bytes
						0x00,0x00,0x00,0x00,0x00,0x00};
				System.arraycopy(uId, 0, data, 15, 8);
				Log.e(TAG, "Data: " + ((LeSnfDevice) dev).setAdvertisementData_v2(data, 5, 1, null, false, LeSnfDevice.ADV_TYPE.ADV_CONNECTABLE, 0x76, 7, 8, 5, 16, 7, 0));
			}
		}

		@Override
		public void didSetBroadcastDataForLeSnfDevice(LeDevice dev, int index,
				boolean success) {
			Log.i(TAG, "didSetBroadcastDataForLeSnfDevice: " + dev.mName + " :: " + index);
		}

		@Override
		public void didReadTemperateLog(int[] log, LeSnfDevice dev) {
			// TODO Auto-generated method stub
			
		}

		@Override
		public void didEnableConnectionLossAlertForLeSnfDevice(LeSnfDevice dev,
				boolean success) {
			// TODO Auto-generated method stub
			
		}

		@Override
		public void didEnableAlertForLeSnfDevice(LeDevice dev,
				boolean success) {
			// TODO Auto-generated method stub
			
		}
		
		@Override
		public void didStartFirmwareUpdate(LeDevice dev) {
			Singleton.uiContext.showUpdateDialog((LeSnfDevice) dev, true);
			Singleton.uiContext.setProgressUpdateDialog((LeSnfDevice) dev, 0);
			Singleton.uiContext.keepScreenOn(true);
		}
		
		@Override
		public void didChangeFirmwareUpdateProgress(LeDevice dev, float mFirmwareProgress){
			Singleton.uiContext.setProgressUpdateDialog((LeSnfDevice) dev, mFirmwareProgress);
		}
		
		@Override
		public void didFinishFirmwareUpdate(LeDevice dev) {
			Singleton.uiContext.setProgressUpdateDialog((LeSnfDevice) dev, -1);
			if (dev.mOldSticker)
				dev.mRequestBond = true;
			dev.mOldSticker = false;
			Singleton.uiContext.keepScreenOn(false);
		}

		@Override
		public void didRequestFirmwareUpdate(LeDevice dev) {
			if (0 != (((LeSnfDevice)dev).mDeviceFlags & 0x40)) dev.setPowerOffTimer(0);
			if (dev.mOldSticker)
				dev.startFirmwareUpdate();
			else
				Singleton.uiContext.showFirmwareUpdateQuestion((LeSnfDevice) dev);
		}

		@Override
		public void didTimeoutFirmwareUpdate(LeDevice dev) {
			//NOT used at the moment
		}


		@Override
		public void didReadDeviceFlags(LeDevice dev) {
			// TODO Auto-generated method stub
			
		}

		@Override
		public void didFailAuthentication(LeDevice dev, byte [] key) {
			// TODO Auto-generated method stub
			
		}
		
		@Override 
		public void requireFirmwareUpdate(LeDevice dev){
			Log.e(TAG, "Device: " + dev.mName + " needs firmware update");
			dev.mOldSticker = true;

		}

		@Override
		public void internalError(LeDevice dev, int verbose, final String err) {
			Singleton.uiContext.runOnUiThread(new Runnable(){
				
				@Override
				public void run() {
					Toast.makeText(context, err, Toast.LENGTH_LONG).show();
				}
			});
		}

		@Override
		public void didScanForDevices(boolean start) {
			// TODO Auto-generated method stub
			
		}

		@Override
		public void didReadDeviceId(LeDevice dev, byte[] deviceId) {
			int interval = 1800;
			((LeSnfDevice) dev).setAdvertisementSettings(interval, 3000, interval + 1, 0, interval + 2, 0x76, 0x76);
			/*
			if (((LeSnfDevice)dev).mBroadcastAdTypeRequest){
				byte [] key = {0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15};
				Log.e(TAG, "BroadCast: " + dev.writeAdvertisementKey(key, (byte) 0x80));
				
				dev.readAdvertisementKey();
			}
			*/
		}

		@Override
		public void didReadBroadcastData(LeDevice dev, byte[] data) {
			// TODO Auto-generated method stub
			
		}

		@Override
		public void didSetDeviceAuthenticationKey(LeDevice dev,
				byte[] mLastAuthKey) {
			// TODO Auto-generated method stub
			
		}

		@Override
		public void didGrantAuthentication(LeSnfDevice mDevice,
				byte[] mLastAuthKey) {
			// TODO Auto-generated method stub
			
		}

		@Override
		public byte[] didSetInvalidKey(LeSnfDevice mDevice, byte[] invalidKey) {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public void didReadUserTemperatureCal(LeSnfDevice mDevice,
				byte[] tempCal) {
			// TODO Auto-generated method stub
			
		}

		@Override
		public void didWriteUserTemperatureCalChar(LeSnfDevice mDevice) {
			// TODO Auto-generated method stub
			
		}

		@Override
		public void didReadTemperatureLog(LeSnfDevice mDevice,
				HashMap<String, Object>[] tempLog) {
			Log.e(TAG, "didReadTemperatureLog");
			Log.e(TAG, "Values: " + tempLog.toString());
			
		}
		
		@Override
		public void didUpdateTemperatureLog(LeSnfDevice mDevice, HashMap<String, Object> [] tempLog){
			Log.e(TAG, "didUpdateTemperatureLog");
		}

		@Override
		public void didReadAdvertisementSettings(LeSnfDevice dev,
				int interval_fast, int timeout_fast, int interval,
				int timeout_slow, int interval_slow,
				int channel_txpower_noconn, int channel_txpower_conn) {
			Log.e(TAG, "didReadAdvertisementSettings: " + interval_fast + " :: " + timeout_fast + " :: " + interval + " :: " + 
				  timeout_slow + " :: " + interval_slow + " :: " + channel_txpower_noconn + " :: " + channel_txpower_conn);
		}

		@Override
		public void didReadDeviceTime(LeSnfDevice mDevice, int time) {
			// TODO Auto-generated method stub
			
		}

		@Override
		public void didReadCSVCValue(LeSnfDevice dev, byte[] cmd) {
			// TODO Auto-generated method stub
			
		}
    };
    
	///////////////////////////////////////////////////////////////////////////////////

    private String findGoodName(LeDeviceManager mgr) {
    	return "SNF " + mgr.mDevList.size();
	}
	
	///////////////////////////////////////////////////////////////////////////////////
	static ArrayList<Messenger> mClients = new ArrayList<Messenger>();
    final Messenger mMessenger = new Messenger(new IncomingHandler());

	protected boolean mDiscoveryRunning = false;
	private static boolean mSdkFailed = false;
    
    private static class IncomingHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
            case Singleton.MSG_REGISTER_CLIENT:
                Log.i(TAG, "Client registered: "+msg.replyTo);
                mClients.add(msg.replyTo);
                send(Message.obtain(null, Singleton.MSG_REGISTER_SUCCESS));
                if (mSdkFailed)
                	send(Message.obtain(null, Singleton.MH_MSG_PHONE_NOT_SUPPORTED));
                break;
            case Singleton.MSG_UNREGISTER_CLIENT:
                Log.i(TAG, "Client un-registered: "+msg.replyTo);
                mClients.remove(msg.replyTo);
                break;
            case Singleton.MH_MSG_MSG_DESTROY:
            	leDeviceManager.destroy();
            	if (MainManager.context != null)
            		MainManager.context.stopSelf();
            	android.os.Process.killProcess(android.os.Process.myPid());
            	break;
            default:
                onReceiveMessage(msg);
            }
        }
    }

    ///////////////////////////////////////////////////////////////////////////////////
 	
	protected BroadcastReceiver receiverAdapter;
	protected BroadcastReceiver receiverDevice;
	
	
	public void startConnection(){	
		
		Log.d(TAG,"Initial OnCreate");
		
		if (android.os.Build.VERSION.SDK_INT >= 18){
			//NEXUS
			leDeviceManager = new NexusDeviceManager(getApplicationContext(), leDeviceManagerDelegate, leSnfDeviceDelegate, logDelegate, LeDeviceManager.TYPE_BEACON);
			
			registerReceiver(receiverAdapter, new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED));
		}else{
			//NOT SUPPORTED
			MainActivity.showSdkSupportProblem();
		}
	}
	
	public void broadcastReceiver(){
		receiverDevice = new BroadcastReceiver() {

			@Override
			public void onReceive(Context context, Intent intent) {
	        	String action = intent.getAction();
	        	
	        	Log.d(TAG,"ActionDevice    "+action);
				if (leDeviceManager != null)
					leDeviceManager.receiveDeviceIntent(context, intent);
			}
		};
		receiverAdapter = new BroadcastReceiver() {

			@Override
			public void onReceive(Context context, Intent intent) {
				String action = intent.getAction();

				Log.d(TAG,"ActionAdapter    "+action);
				if (leDeviceManager != null)
					leDeviceManager.receiveAdapterIntent(context, intent);
			}
			
		};
    }
	
    ///////////////////////////////////////////////////////////////////////////////////
	
	@Override
	public void onDestroy(){
		super.onDestroy();
		
		leDeviceManager.stopScan();
		
		Singleton.connectionMaintainerIsRunning = false;		
		leDeviceManager.stopScan();
		leDeviceManager.destroy();
		
		if (receiverAdapter != null)
			unregisterReceiver(receiverAdapter);
		if (receiverDevice != null)
			unregisterReceiver(receiverDevice);
		
		try{
			android.os.Process.killProcess(android.os.Process.myPid());
		}catch(Exception e){}
	}
	
		@Override
	public void onCreate(){
		Log.i(TAG, "onCreate");
		
		Singleton.context=this;
		
		broadcastReceiver();
	}
	
	
	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		MainManager.context = this;
		
		startConnection();
		
	    return Service.START_STICKY;
	}

//////////////////////////////////////////////////////////////////////////////////////
	
	@Override
	public IBinder onBind(Intent arg0) {
        return mMessenger.getBinder();
	};
	
    protected static void send(Message msg) {
         for (int i=mClients.size()-1; i>=0; i--) {
        	 try {
               mClients.get(i).send(msg);
            }
            catch (RemoteException e) {
                Log.e(TAG, "Client is dead. Removing from list: "+i);
                mClients.remove(i);
            }
        }       
    }
    
    static void setBroadcastAdvname(LeDevice dev){
	    byte [] uId = {0x10, 0x11, 0x12, 0x13, 0x14, 0x15, 0x16, 0x19};
		byte [] key = {0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15};
		Log.e(TAG, "BroadCast: " + dev.writeAdvertisementKey(key, (byte) 0x80));
		
		byte [] data = {0x02,0x01,0x06,
				27,0x09,	// the raw packet data  (length, type)
				's','L',	// text starts here
				0x03,0x02,0x24,0x3C,0x28,0x29,0x2A,0x2B,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,
				(byte) 0xAA,0x55, // trailer bytes
				0x00,0x00,0x00,0x00,0x00,0x00};
		System.arraycopy(uId, 0, data, 15, 8);
		Log.e(TAG, "Data: " + ((LeSnfDevice) dev).setAdvertisementData_v2(data, 5, 1, null, false, LeSnfDevice.ADV_TYPE.ADV_CONNECTABLE, 0x76, 7, 8, 5, 16, 7, 0));
    }
    /*
     * 08-18 23:14:04.357: I/LeTagService(27688): Int[]: [189, 0, 3, 28, 69, 81, 0] :: 7
     * 08-18 23:14:04.357: I/LeTagService(27688): RSSI RX NEW: 45 :: 3
     */
	public static void onReceiveMessage(Message msg) {
		if (leDeviceManager == null) return;
		if (leDeviceManager.mBta == null) return;
		switch (msg.what){
		case Singleton.MH_MSG_AVOID_FIRMWARE_UPDATE:
			break;
		case Singleton.MH_MSG_RESET_STICKER:
			((LeDevice)msg.obj).setResetSticker(true);
			leDeviceManager.connect((LeDevice)msg.obj);
			break;
		case Singleton.MH_MSG_CONNECT_DEVICE:
			((LeDevice)msg.obj).setResetSticker(false);
//			((LeSnfDevice)msg.obj).setForceFirmwareUpdate(true);
			((LeSnfDevice)msg.obj).setBaseInterval(20);
			leDeviceManager.connect((LeDevice)msg.obj);
			break;
		case Singleton.MH_MSG_DISCONNECT_DEVICE:
			leDeviceManager.disconnect((LeDevice)msg.obj);
			break;
		case Singleton.MH_MSG_CONNECTION_SPEED:
			break;
		case Singleton.MH_MSG_REMOVEALL:
			break;
		case Singleton.MH_MSG_FORCE_DISCONNECT:
			break;
		case Singleton.MH_MSG_START_SCAN:
			leDeviceManager.mShouldScan = true;
			break;
		case Singleton.MH_MSG_STOP_SCAN:
			leDeviceManager.mShouldScan = false;
			break;
		case Singleton.MH_MSG_ALERT_DEVICE:
			((LeDevice)msg.obj).setAlertLevel(3);
			break;
		case Singleton.MG_MSG_MELODY:
			byte [] melody = {3,
							  16, 10, 32, 6, 8, 2, 0};
			((LeSnfDevice)msg.obj).writeMelody(melody);
			break;
		case Singleton.MG_MSG_READ_ADV_SETTINGS:
			((LeDevice)msg.obj).readAdvertisementSettings();
			break;
		case Singleton.MH_MSG_DEVICE_LATENCY:
			Object [] value = (Object[])msg.obj;
			((LeDevice)value[0]).setLatency((Integer)value[1]);
			break;
		case Singleton.MH_MSG_DEVICE_RBOND:
			((NexusSnfDevice)msg.obj).removeBond();
			break;
		case Singleton.MH_MSG_DEVICE_CBOND:
			Log.e(TAG, "Create BOND");
			((LeDevice)msg.obj).setResetSticker(false);
			leDeviceManager.connect((LeDevice)msg.obj);
			((LeDevice)msg.obj).mRequestBond = true;
			break;
		case Singleton.MG_MSG_TEMPERATURE_ON:
			((NexusSnfDevice)msg.obj).enableTemperatureLoggingWithInterval(2);
			((NexusSnfDevice)msg.obj).readTemperatureLog();
			break;
		case Singleton.MG_MSG_NAMEIT:
			((NexusSnfDevice)msg.obj).writeAdvRssiSettings(3);
			if (!((NexusSnfDevice)msg.obj).writeDeviceName("Example"))
				Log.e(TAG, "Error sending writeDeviceName");
			break;
		case Singleton.MG_MSG_PUTASLEEP:
			((NexusSnfDevice)msg.obj).setPowerOffTimer(45);
			break;
		default:
			Log.i(TAG, "Default Msg Received: " + msg.toString());
		}
	}

    
/////////////////////////////////////////////////////////////////////////////////////
    
    
}
