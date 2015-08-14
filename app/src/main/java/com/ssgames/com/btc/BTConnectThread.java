package com.ssgames.com.btc;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.util.Log;

public class BTConnectThread extends Thread {

	private final BluetoothDevice btDevice;
	private final BluetoothSocket btSocket;
	private final BTConnectThreadListener ctListener;

	public BTConnectThread(BluetoothDevice device,
			BTConnectThreadListener listener) {
		btDevice = device;
		ctListener = listener;
		BluetoothSocket tmp = null;

		try {
			tmp = btDevice
					.createRfcommSocketToServiceRecord(BTConnectionHandler.MY_UUID);
		} catch (Exception e) {
			Log.d("D_TAG", "Creating socket failed!");
			e.printStackTrace();
		}

		btSocket = tmp;
	}

	public void run() {
		BluetoothAdapter.getDefaultAdapter().cancelDiscovery();

		try {
			btSocket.connect();
		} catch (Exception e) {

			Log.d("D_TAG", "Connecting failed!");
			e.printStackTrace();

			try {
				btSocket.close();
			} catch (Exception e2) {

			}
			connectFailed();
			return;
		}

		connected();
	}

	public void cancel() {
		try {
			btSocket.close();
		} catch (Exception e) {

		}
	}

	private void connected() {
		ctListener.btConnectConnected(this, btSocket);
	}

	private void connectFailed() {
		ctListener.btConnectFailed(btDevice);
	}

	public BluetoothSocket getBtSocket() {
		return btSocket;
	}

	public BluetoothDevice getBtDevice() {
		return btDevice;
	}

}
