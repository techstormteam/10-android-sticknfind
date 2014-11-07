package com.ssia.android_ble_scanner;

import java.util.Comparator;
import java.util.Vector;

import com.ssia.sticknfind.sdk.LeDevice;
import com.ssia.sticknfind.sdk.LeSnfDevice;

import android.R.color;
import android.content.Context;
import android.graphics.Color;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.TextView;

public class UserArrayAdapter extends ArrayAdapter<LeDevice> {
	private Vector<LeDevice> mList;
	private Context mContext;

	public UserArrayAdapter(Context context, Vector<LeDevice> list) {
		super(context, R.layout.snf_scan_list_item, list);
		this.mList=list;
		this.mContext = context;
	}
 
	private class StickerContentItem
	{
		private Button mButton;
		private TextView mRssi, mBatt, mTemp;
	
		public StickerContentItem(LeDevice dev, Button but, TextView rssi, TextView batt, TextView temp){
			mButton = but;
			mRssi = rssi;
			mBatt = batt;
			mTemp = temp;
		}
		
	}
	
	private class BeaconContentItem
	{
		private Button mButton;
		private TextView mSID, mIUUD;
		private TextView mSRssi, mIRssi;
		private TextView mSTemp, mId;
		private TextView mSBatt;
		private TextView mSNumScan;
		private TextView mSTxPower;
		private TextView mSChn;
		
		public BeaconContentItem(LeDevice dev, Button but, TextView sID, TextView iUUID, TextView sRssi, TextView iRssi, 
						   TextView sTemp, TextView subId, TextView sBatt,
						   TextView sNumScan, TextView sTxPower, TextView sChn){
			mButton = but;
			mSID = sID;
			mIUUD = iUUID;
			mSRssi = sRssi;
			mIRssi = iRssi;
			mSTemp = sTemp;
			mId = subId;
			mSBatt = sBatt;
			mSNumScan = sNumScan;
			mSTxPower = sTxPower;
			mSChn = sChn;
		}
		
	}
	
	private void changeBgdColor(LeDevice dev, View view, Button name){
		if (dev.isConnected()){
			if (dev.isDiscoveryDone()){
				view.setBackgroundColor(Color.argb(125, 0, 0, 150));
			}else{
				view.setBackgroundColor(Color.argb(125, 0, 0, 50));
			}
		}else if (dev.isBonded()){
			view.setBackgroundColor(Color.argb(125, 0, 150, 0)); 
		}else if (dev.isBonding()){
			view.setBackgroundColor(Color.argb(125, 0, 15, 0));
		}else if (dev.isConnecting()){
			view.setBackgroundColor(Color.argb(125, 0, 0, 255));
		}else{
			view.setBackgroundColor(Color.argb(125, 30, 0, 0));
		}
		
		view.postInvalidate();
	}
	
	private void setPopupMenu(int itemId, LeDevice dev){
		switch (itemId){
		case R.id.reset:
			Singleton.updateMainHandlerDevice(Singleton.MH_MSG_RESET_STICKER, dev);
			break;
		case R.id.connect:
			Singleton.updateMainHandlerDevice(Singleton.MH_MSG_CONNECT_DEVICE, dev);
			break;
		case R.id.disconnect:
			Singleton.updateMainHandlerDevice(Singleton.MH_MSG_DISCONNECT_DEVICE, dev);
			break;
		case R.id.sleep:
			Singleton.updateMainHandlerDevice(Singleton.MG_MSG_PUTASLEEP, dev);
			break;
		case R.id.advsettings:
			Singleton.updateMainHandlerDevice(Singleton.MG_MSG_READ_ADV_SETTINGS, dev);
			break;
		case R.id.alert:
			Singleton.updateMainHandlerDevice(Singleton.MH_MSG_ALERT_DEVICE, dev);
			break;
		case R.id.melody:
			Singleton.updateMainHandlerDevice(Singleton.MG_MSG_MELODY, dev);
			break;
		case R.id.tempon:
			Singleton.updateMainHandlerDevice(Singleton.MG_MSG_TEMPERATURE_ON, dev);
			break;
		case R.id.tempoff:
			Singleton.updateMainHandlerDevice(Singleton.MG_MSG_NAMEIT, dev);
			break;
		case R.id.high:
			Singleton.updateMainHandlerDevice(Singleton.MH_MSG_DEVICE_LATENCY, dev, 0);
			break;
		case R.id.slow:
			Singleton.updateMainHandlerDevice(Singleton.MH_MSG_DEVICE_LATENCY, dev, 9);
			break;
		case R.id.cbond:
			Singleton.updateMainHandlerDevice(Singleton.MH_MSG_DEVICE_CBOND, dev);
			break;
		case R.id.rbond:
			Singleton.updateMainHandlerDevice(Singleton.MH_MSG_DEVICE_RBOND, dev);
			break;
		}
	}
	
	private View displayStickers(LeDevice dev, int position, View convertView, final ViewGroup parent) {
		Button name;
		TextView temp, batt, rssi;
		
		if (convertView == null){
			LayoutInflater inflater = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);		
			convertView = inflater.inflate(R.layout.snf_scan_list_item, parent, false);

			name = (Button) convertView.findViewById(R.id.name);
			name.setText(dev.mName + "/" + dev.getBtDevice().getAddress().substring(0,5));
						
			changeBgdColor(dev, convertView, name);
			
			name.setEnabled(true);
			name.setTag(dev);
			name.setOnClickListener(new OnClickListener(){
	
				@Override
				public void onClick(View v) {
					PopupMenu popupMenu = new PopupMenu(parent.getContext(), v);
					final LeDevice dev = (LeDevice)v.getTag();
					popupMenu.getMenuInflater().inflate(R.menu.popupmenu, popupMenu.getMenu());
					popupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
						
						@Override
						public boolean onMenuItemClick(MenuItem item) {
							setPopupMenu(item.getItemId(), dev);
							return false;
						}
					});
					popupMenu.show();
				}
				
			});
			////////////////////////////////////////////////////////////////
			temp = (TextView) convertView.findViewById(R.id.temp);
			temp.setText("T: " + ((LeSnfDevice)dev).mTemperature);

			batt = (TextView) convertView.findViewById(R.id.battery);
			batt.setText("B: " + ((LeSnfDevice)dev).mBatteryLevel);

			rssi = (TextView) convertView.findViewById(R.id.rssi);
			rssi.setText("R: " + dev.mRssi);
			convertView.setTag(new StickerContentItem(dev, name, rssi, batt, temp));
		}else if (convertView.getTag() != null){
			StickerContentItem item;
			if (convertView.getTag().getClass().equals(StickerContentItem.class))
				item = (StickerContentItem)convertView.getTag();
			else{
				return displayStickers(dev, position, null, parent);
			}

			name = item.mButton;
			////////////////////////////////////////////////////////////////
			temp = item.mTemp;
			temp.setText("T: " + ((LeSnfDevice)dev).mTemperature);
			
			batt = item.mBatt;
			batt.setText("B: " + ((LeSnfDevice)dev).mBatteryLevel);
 
			rssi = item.mRssi;
			rssi.setText("R: " + dev.mRssi);
			
			////////////////////////////////////////////////////////////////
			
			changeBgdColor(dev, convertView, name);
			name.setText(dev.mName + "/" + dev.getBtDevice().getAddress().substring(0,5));
			name.setTag(dev);
		}else{
			Log.i("Result", "Unkown");
		}

		return convertView; 
	}
		
	private View displayBeacons(final LeDevice dev, final BeaconData beaconData, final int position, View convertView, final ViewGroup parent) {
		Button name;
		TextView sId, iUUID;
		TextView sRssi, iRssi;
		TextView sTemp, subId;
		TextView sBatt, sTxPower;
		TextView sNumScan, sChn;
		
		if (convertView == null){
			LayoutInflater inflater = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);		
			convertView = inflater.inflate(R.layout.beacon_2_scan_list_item, parent, false);

			name = (Button) convertView.findViewById(R.id.name);
			name.setText(dev.mName + "/" + dev.getBtDevice().getAddress().substring(0,5));
						
			changeBgdColor(dev, convertView, name);
			
			name.setEnabled(true);
			name.setTag(dev);
			name.setOnClickListener(new OnClickListener(){
	
				@Override
				public void onClick(View v) {
					PopupMenu popupMenu = new PopupMenu(parent.getContext(), v);
					final LeDevice dev = (LeDevice)v.getTag();
					popupMenu.getMenuInflater().inflate(R.menu.popupmenu, popupMenu.getMenu());
					popupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
						
						@Override
						public boolean onMenuItemClick(MenuItem item) {
							setPopupMenu(item.getItemId(), dev);
							return false;
						}
					});
					popupMenu.show();
				}
				
			});
			////////////////////////////////////////////////////////////////
			sId = (TextView) convertView.findViewById(R.id.sId);
			sId.setText(beaconData.mSnf.mSubId);
			
			iUUID = (TextView) convertView.findViewById(R.id.iUUID);
			iUUID.setText(beaconData.mApple.mUuid);
			////////////////////////////////////////////////////////////////
			sRssi = (TextView) convertView.findViewById(R.id.sSignal);
			sRssi.setText("S: " + beaconData.mSnf.mRssi + "dBm");
			
			iRssi = (TextView) convertView.findViewById(R.id.iSignal);
			iRssi.setText("Sr: " + beaconData.mApple.mSignal);
			////////////////////////////////////////////////////////////////
			sTemp = (TextView) convertView.findViewById(R.id.sTemp);
			sTemp.setText("T: " + beaconData.mSnf.mTemp);
			
			subId = (TextView) convertView.findViewById(R.id.subid);
			subId.setText("SubId: " + beaconData.mApple.mSubId);
			////////////////////////////////////////////////////////////////
			sBatt = (TextView) convertView.findViewById(R.id.sBattery);
			sBatt.setText("B: " + beaconData.mSnf.mBatt);
			
			sTxPower = (TextView) convertView.findViewById(R.id.sTxpower);
			sTxPower.setText("TX[]: " + beaconData.mSnf.mTxSignal[0] + ":" + beaconData.mSnf.mTxSignal[1] + ":" + beaconData.mSnf.mTxSignal[2] + "dBm");
			////////////////////////////////////////////////////////////////
			sNumScan = (TextView) convertView.findViewById(R.id.sNumscan);
			sNumScan.setText("Ns: " + beaconData.mSnf.mNumScan);
			
			sChn = (TextView) convertView.findViewById(R.id.sChn);
			sChn.setText("Ch[]: " + beaconData.mSnf.mChn[0] + ":" + beaconData.mSnf.mChn[1] + ":" + beaconData.mSnf.mChn[2]);
			////////////////////////////////////////////////////////////////
			
			convertView.setTag(new BeaconContentItem(dev, name, sId, iUUID, sRssi, iRssi,
											   sTemp, subId, sBatt,
											   sNumScan, sTxPower, sChn));
		}else if (convertView.getTag() != null){
			BeaconContentItem item;
			if (convertView.getTag().getClass().equals(BeaconContentItem.class))
				item = (BeaconContentItem)convertView.getTag();
			else{
				return displayBeacons(dev, beaconData, position, null, parent);
			}
						
			name = item.mButton;
			////////////////////////////////////////////////////////////////
			sId = item.mSID;
			sId.setText(beaconData.mSnf.mSubId);
			
			iUUID = item.mIUUD;
			iUUID.setText(beaconData.mApple.mUuid);
			////////////////////////////////////////////////////////////////
			sRssi = item.mSRssi;
			sRssi.setText("S: " + beaconData.mSnf.mRssi + "dBm");
			
			iRssi = item.mIRssi;
			iRssi.setText("Sr: " + beaconData.mApple.mSignal);
			////////////////////////////////////////////////////////////////
			sTemp = item.mSTemp;
			sTemp.setText("T: " + beaconData.mSnf.mTemp);
			
			subId = item.mId;
			subId.setText("SubId: " + beaconData.mApple.mSubId);
			////////////////////////////////////////////////////////////////
			sBatt = item.mSBatt;
			sBatt.setText("B: " + beaconData.mSnf.mBatt);
			
			sTxPower = item.mSTxPower;
			sTxPower.setText("TX[]: " + beaconData.mSnf.mTxSignal[0] + ":" + beaconData.mSnf.mTxSignal[1] + ":" + beaconData.mSnf.mTxSignal[2] + "dBm");
			////////////////////////////////////////////////////////////////
			sNumScan = item.mSNumScan;
			sNumScan.setText("Ns: " + beaconData.mSnf.mNumScan);
			
			sChn = item.mSChn;
			sChn.setText("Ch[]: " + beaconData.mSnf.mChn[0] + ":" + beaconData.mSnf.mChn[1] + ":" + beaconData.mSnf.mChn[2]);
			////////////////////////////////////////////////////////////////
			
			changeBgdColor(dev, convertView, name);
			name.setText(dev.mName + "/" + dev.getBtDevice().getAddress().substring(0,5));
			name.setTag(dev);
		}else{
			Log.i("Result", "Unkown");
		}
		
		return convertView; 
	}
	
	@Override
	public View getView(final int position, final View convertView, final ViewGroup parent) {

		final LeDevice dev=mList.get(position);
				
		BeaconData beaconData = MainManager.mBeaconMap.get(dev);
		if (beaconData == null)
			return displayStickers(dev, position, convertView, parent);
		else
			return displayBeacons(dev, beaconData, position, convertView, parent);		
	}
}
