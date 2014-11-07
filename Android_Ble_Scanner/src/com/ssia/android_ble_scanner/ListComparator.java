package com.ssia.android_ble_scanner;

import java.util.Comparator;

import com.ssia.sticknfind.sdk.LeDevice;

public class ListComparator implements Comparator<LeDevice>{
	
	
	public int compare(LeDevice dev1, LeDevice dev2){
		return  dev2.mRssi - dev1.mRssi;
	}
}
