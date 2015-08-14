package com.ssgames.com.btc;

import android.bluetooth.BluetoothDevice;

public interface BTConnectionListener {

	/*
	 * Connection manage
	 */

	void connectionEstablished(BTConnection connection);

	void connectionDisconnected(BluetoothDevice device);

	void deviceNotConnected(BluetoothDevice device);
	
	/*
	 * Data manage
	 */

	void dataReceived(BTConnection connection, byte[] buffer);

	void dataDidSend(BTConnection connection);

	void dataDidNotSend(BTConnection connection, byte[] buffer);

}
