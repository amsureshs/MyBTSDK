package com.ssgames.com.btc;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import android.bluetooth.BluetoothSocket;
import android.util.Log;

public class BTConnectedThread extends Thread {

	private final BluetoothSocket btSocket;
	private final InputStream socInStream;
	private final OutputStream socOutStream;
	private final BTConnectedThreadListener ctListener;

	public BTConnectedThread(BluetoothSocket socket,
			BTConnectedThreadListener listener) {
		btSocket = socket;
		ctListener = listener;
		InputStream tmpIn = null;
		OutputStream tmpOut = null;

		try {
			tmpIn = btSocket.getInputStream();
			tmpOut = btSocket.getOutputStream();
		} catch (Exception e) {

		}

		socInStream = tmpIn;
		socOutStream = tmpOut;
	}

	public void run() {
		byte[] buffer = new byte[1024];
		int bytes;

		// Keep listening to the InputStream while connected
		while (true) {
			try {
				// Read from the InputStream
				bytes = socInStream.read(buffer);
				if (bytes == -1) {
					ctListener.btConnectedNewDataReceived(btSocket, buffer);
				}
			} catch (IOException e) {
				ctListener.btConnectedNewDataReadFailed(btSocket);
				// TODO on break
				break;
			}
		}
	}

	public void write(byte[] buffer) {
		try {
			socOutStream.write(buffer);
			ctListener.btConnectedDataWriteSucceeded(btSocket);
		} catch (IOException e) {
			ctListener.btConnectedDataWriteFailed(btSocket, buffer);
			Log.d("D_TAG", "Error during write");
		}
	}

	public void cancel() {
		try {
			btSocket.close();
		} catch (IOException e) {
			Log.d("D_TAG", "Socket closing error");
		}
	}

}
