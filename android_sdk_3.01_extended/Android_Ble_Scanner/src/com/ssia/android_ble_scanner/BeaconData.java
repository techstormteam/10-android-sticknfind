package com.ssia.android_ble_scanner;

import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.SecretKeySpec;

import android.util.Base64;
import android.util.Log;

public class BeaconData {
	
	final static String TAG = "BeaconData";
	
	final public static int mAppleBeacon = 1;
	final public static int mSnfBeacon = 2;
	
	final public static int mAdName = 0x10;
	
	protected static final int ADTYPE_128BIT_MORE                 = 0x06; // Service: More 128-bit UUIDs available
	protected static final int ADTYPE_NAME						  = 0x09;
	protected static final int ADTYPE_MANUFACTURER_SPECIFIC       = 0xFF; // Manufacturer Specific Data: first 2 octets contain the Company Identifier Code followed by the additional manufacturer specific data

	static final String mSnfUUID = "8DCA-417A-944E-403C76E4B50C";	//raw
	static final int [] mIntSnfUUID = {0x0C, 0xB5, 0xE4, 0x76, 0x3C, 0x40, 
			                        0x4E, 0x94, 0x7A, 0x41, 0xCA, 0x8D};
	
	static final String mGoogleUuid = "51346E95-6858-4810-BD7D-6DD3E4EB42DD";
	static final int [] mIntGoogleUuid = {0x51, 0x34, 0x6E, 0x95,
		                                  0x68, 0x58, 0x48, 0x10};
	
	static final String mAppleUuid = "FD7B7966-9C0F-471A-83A2-46D995AE85A1";
	static final int [] mIntAppleUuid = {0xFD, 0x7B, 0x79, 0x66, 0x9C, 0x0F, 0x47, 0x1A,
				       			         0x83, 0xA2, 0x46, 0xD9, 0x95, 0xAE, 0x85, 0xA1};
	
	static class FrameInfo{
		int mType;
		int mOffset, mLength;
		
		public FrameInfo(int type, int offset, int length){
			mType = type;
			mOffset = offset;
			mLength = length;
		}
	}
	
	private int mNoBeaconCnt = 0;
	
	public int mType = -1;
	public BeaconData.Apple mApple;
	public BeaconData.Snf mSnf;
	
	public class Apple{
		public String mUuid;
		public String mSubId;
		public int mSignal;
		
		public Apple(){
			mUuid = "";
			mSignal = -1;
		}
	}
		
	public class Snf{
		int mTemp;			//1&2
		int mBatt;			//1&2
		String mSubId;
		int mRssi;
		int [] mChn;
		int [] mTxSignal;
		int mNumScan;
		
		public Snf(){
			mSubId = "";
			mTxSignal = new int[3];
			mChn = new int[3];
			Arrays.fill(mChn, -1);
		}
		
		public String toString(){
			return "mSubId: " + mSubId + " :: " + "mRssi: " + mRssi + " :: " + "Temp: " + mTemp + " :: " + "Batt: " + mBatt +
					" Chn: " + mChn[0] + ":" + mChn[1] + ":" + mChn[2]; 
		}
	}	
	
	/*
	 * Google
	 * 
	 * 2 - packet type
	 * 3 - 14 UUID MSB first
	 * 15 - 18 reserved
	 * 
	 */
	
	static void logData(String subTag, byte []rec, int offset, int length, boolean enable){
		if (!enable) return;
		StringBuffer output = new StringBuffer();
		for (int i = 0; i < length; i++){
			output.append(String.format("%02x", ((int) rec[offset + i]) & 0xFF));
			output.append(" ");
		}
		Log.e(TAG, subTag + output);
	}
	
	public BeaconData(int rssi, byte[] scanRecord, FrameInfo frameInfo, byte [] key) {
		mNoBeaconCnt = 0;
		mApple = new BeaconData.Apple();
		mSnf = new BeaconData.Snf();
		updateData(rssi, scanRecord, frameInfo);
	}
	
	byte[] broadcastChecksumForData(byte[] data)
	{
	    byte key[] = {0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0};
	    byte idata[] = {0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0};
	    byte odata[] = new byte[16];
	    
	    if (data.length > 16)
	    	System.arraycopy(data, 0, idata, 0, 16);
	    else
	    	System.arraycopy(data, 0, idata, 0, data.length);
	    
		SecretKeySpec ks = new SecretKeySpec(key, "AES/ECB/NoPadding");
	    Cipher cipher;
	    try {
	    	cipher = Cipher.getInstance("AES/ECB/NoPadding");
	    	cipher.init(Cipher.ENCRYPT_MODE, ks);	
	    	odata = cipher.doFinal(idata);
	    	
	    	return odata;
	    	
	    } catch (Exception e) {
			e.printStackTrace();
		}
	    return new byte [0];
	}
	
	byte [] decryptData(byte [] key, byte[] encrypted){
		SecretKeySpec ks = new SecretKeySpec(key, "AES/ECB/NoPadding");
	    Cipher cipher;
	    try {
	    	cipher = Cipher.getInstance("AES/ECB/NoPadding");
	    	cipher.init(Cipher.DECRYPT_MODE, ks);	
	    	byte[] auth = cipher.doFinal(encrypted);
	    	Log.d("auth key","len: " + auth.length);
	    	
	    	return auth;
	    	
	    } catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
		} catch (NoSuchPaddingException e) {
			e.printStackTrace();
		} catch (InvalidKeyException e) {
			e.printStackTrace();
		} catch (IllegalBlockSizeException e) {
			e.printStackTrace();
		} catch (BadPaddingException e) {
			e.printStackTrace();
		}
	    return new byte [0];
	}
	
	Integer[] convertToUnsigned(byte [] data){
		Integer [] result = new Integer[data.length];
		for (int i = 0; i < data.length; i++){
			result[i] = ((int)data[i]) & 0xFF;
		}
		return result;
	}
	
	public boolean incNoBeaconCount(){
		if (mNoBeaconCnt++ > 7){
			mNoBeaconCnt = 0;
			return true;
		}
		return false;
	}
	
	public void resetNoBeaconCount(){
		mNoBeaconCnt = 0;
	}
	
	public void updateData(int namerssi, byte [] scanRecord, FrameInfo frameInfo){
		Integer[] record = null;
		switch (frameInfo.mType){
		case mAppleBeacon:
			mType = BeaconData.mAppleBeacon;
			StringBuffer uuidA = new StringBuffer();
			for (int i = 6 + frameInfo.mOffset; i < 27 + frameInfo.mOffset; i++){
				uuidA.append(String.format("%02x", ((int) scanRecord[i]) & 0xFF));
			}
			mApple.mUuid = uuidA.substring(0,32);
			mApple.mSubId = uuidA.substring(32, 40);
			Log.d(TAG, "AppleBeacon Update: " + mApple.mUuid);
			mApple.mSignal = scanRecord[frameInfo.mOffset + frameInfo.mLength];
			break;
		case mSnfBeacon:
			mType = BeaconData.mSnfBeacon;
			record = convertToUnsigned(scanRecord);
			int ch = record[14 + 2 + frameInfo.mOffset] / 85;
			int txP = -((record[14 + 2 + frameInfo.mOffset] % 85) + 20);
			int index = 0;
	        for (int i = 0; i < 4; i++){
	        	if (i < 3){
		        	if (mSnf.mChn[i] == ch){
		        		mSnf.mTxSignal[i] = txP;
		        		index = i;
		        		break;
		        	}
	        	}else{
	        		index= 2;	
	        		if (mSnf.mChn[0] == -1)
	        			index = 0;
	        		else if (mSnf.mChn[1] == -1)
	        			index = 1;
	        		mSnf.mChn[index] = ch;
	        		mSnf.mTxSignal[index] = txP;
	        	}
	        }

		    byte tmp[] = new byte[16];
		    System.arraycopy(scanRecord, 3 + 2 + frameInfo.mOffset, tmp, 0, 12);
		    tmp[11] = 0;
		    byte check[] = broadcastChecksumForData(tmp);
		    byte cmp1[] = new byte[7];
		    byte cmp2[] = new byte[7];
		    System.arraycopy(check, 0, cmp1, 0, 7);
		    System.arraycopy(scanRecord, 15 + 2 + frameInfo.mOffset, cmp2, 0, 7);
		    if (Arrays.equals(cmp1, cmp2)){
		    	StringBuffer uuidS = new StringBuffer();
				for (int i = 6 + 2 + frameInfo.mOffset; i >= 3 + 2 + frameInfo.mOffset; i--){
					uuidS.append(String.format("%02x", record[i]));
				}
		    	mSnf.mSubId = uuidS.substring(0, 8);
		        mSnf.mNumScan = record[11 + 2 + frameInfo.mOffset] >> 2;
		        mSnf.mBatt = (record[12 + 2 + frameInfo.mOffset] << 2) + (record[11 + 2 + frameInfo.mOffset] & 0x3);
		        mSnf.mTemp = record[13 + 2 + frameInfo.mOffset];
		        mSnf.mBatt = (mSnf.mBatt * 3600) / 1024;
	        	mSnf.mRssi = namerssi + mSnf.mTxSignal[index] + 40;
		        Log.i(TAG, "sBeacon: " + mSnf.toString());
		    }else{
		    	Log.e(TAG, "sBeacon Checksum err");
		    }
			break;
		}
	}
	
	static FrameInfo checkManufacturerData(byte [] rec, int offset, int length){
		logData("MD: ", rec, offset, length + 1, true);
		if ((rec[offset] == 26) && (rec[offset + 2] == 0x4c) && (rec[offset + 3] == 0)){	//Apple Beacon
//			for (int i = offset + 6, x = 0; i < offset + 6 + mIntAppleUuid.length; i++){
//				if (mIntAppleUuid[x++] != (((int)rec[i]) & 0xFF)) return null;
//			}
			return new FrameInfo(mAppleBeacon, offset, length);
		}else
		if (((((int)rec[offset + 2]) & 0xFF) == 0xF9) && (rec[offset + 3] == 0x00)){
			return new FrameInfo(mSnfBeacon, offset, length);
		}else
			return null;
	}
	
	public static FrameInfo searchMdScanRecord(byte [] rec)
	{
		int i, c;
		c = 0;
		for (i = 0;i < (rec.length - 1);)
		{
			int n = rec[i];
			if (0 == n) break;
			int code = ((int)rec[i+1]) & 0xff;
			if (code == ADTYPE_MANUFACTURER_SPECIFIC){
				return checkManufacturerData(rec, i, n);
			}else if (code == ADTYPE_NAME){
				logData("Name: ", rec, i, n, true);
				String base64 = new String(Arrays.copyOfRange(rec, i + 4, i + 28));
				
				byte [] uId = {0x10, 0x11, 0x12, 0x13, 0x14, 0x15, 0x16, 0x23};
				byte [] key = {0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15};
				byte[] decodedString = Base64.decode(base64, Base64.DEFAULT);
				SecretKeySpec ks = new SecretKeySpec(key, "AES/ECB/NoPadding");
		    	try {
					Cipher cipher = Cipher.getInstance("AES/ECB/NoPadding");
			    	cipher.init(Cipher.DECRYPT_MODE, ks);	
					byte[] outb = cipher.doFinal(Arrays.copyOfRange(decodedString, 0,16));
					if (!Arrays.equals(uId, Arrays.copyOfRange(outb, 8, 8))){
						return new FrameInfo(mAdName, -1, -1);
					}
					return new FrameInfo(mAdName, i, n);
				} catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} 
			}
			c++;
			i += n + 1;
		}
		return null;
	}
	
	/////////////////////////////////////////////////////////////////////////////////
	
	static int getMajorMinorArray(byte [] rec, int [] uuid, int offset){
		logData("UUID: ", rec, offset, 16, false);
		int a;
		int [] mm = new int[4];
		for (int i = offset; i < offset+12; i++){
			a = ((int)rec[i]) & 0xFF;
			if (uuid[i - offset] != a)
				return -1;
		}
		for (int i = offset + 12, x = 3; i < offset + 16; i++){
			mm[x--] = ((int)rec[i]) & 0xFF; 
		}
		return mm[3] | (mm[2] << 8) | mm[1] << 16 | mm[0] << 24;
	}

	public static int getMajorMinorScanRecord(byte [] rec)
	{
		int i;
		for (i = 0;i < (rec.length - 1);)
		{
			int n = rec[i];
			if (0 == n) break;
			int code = ((int)rec[i+1]) & 0xff;
			
			if (code == ADTYPE_128BIT_MORE){
				return getMajorMinorArray(rec, mIntSnfUUID, i + 2);
			}
			i += n + 1;
		}
		return -1;
	}
	
	///////////////////////////////////////////////////////////////////////////////
	
	public static int getMajorMinorKey(int [] value){
		return value[3] | (value[2] << 8) | (value[1] << 16) | (value[0] << 24);
	}
}
