package com.ssgames.com.btc;

import android.bluetooth.BluetoothSocket;

public interface BTConnectedThreadListener {
	public void btConnectedNewDataReceived(BluetoothSocket btSocket, byte[] buffer);
	public void btConnectedNewDataReadFailed(BluetoothSocket btSocket);
	public void btConnectedDataWriteSucceeded(BluetoothSocket btSocket);
	public void btConnectedDataWriteFailed(BluetoothSocket btSocket, byte[] buffer);
}
