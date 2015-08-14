package com.ssgames.com.btc;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;

public class BTConnection {

	private BluetoothSocket btSocket;
	private BTConnectedThread btConnectedThread;
	private BTConnectThread btConnectThread;
	private boolean connect;

	public BluetoothSocket getBtSocket() {
		return btSocket;
	}

	public void setBtSocket(BluetoothSocket btSocket) {
		this.btSocket = btSocket;
	}

	public BTConnectedThread getBtConnectedThread() {
		return btConnectedThread;
	}

	public void setBtConnectedThread(BTConnectedThread btConnectedThread) {
		this.btConnectedThread = btConnectedThread;
	}

	public BTConnectThread getBtConnectThread() {
		return btConnectThread;
	}

	public void setBtConnectThread(BTConnectThread btConnectThread) {
		this.btConnectThread = btConnectThread;
	}

	public boolean isConnect() {
		return connect;
	}

	public BluetoothDevice getBtDevice() {
		if (btSocket == null) {
			return null;
		}
		return btSocket.getRemoteDevice();
	}
	
	public String getName() {
		if(getBtDevice() != null)
			return getBtDevice().getName();
		
		return "";
	}
	
	public void disconnect() {
		btConnectedThread.cancel();
	}
}
