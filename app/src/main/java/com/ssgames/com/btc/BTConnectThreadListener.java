package com.ssgames.com.btc;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;

public interface BTConnectThreadListener {
	public void btConnectConnected(BTConnectThread connectThread, BluetoothSocket btSocket);
	public void btConnectFailed(BluetoothDevice btDevice);
}
