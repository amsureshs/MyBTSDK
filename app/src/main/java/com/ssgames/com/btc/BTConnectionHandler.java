package com.ssgames.com.btc;

import java.io.IOException;
import java.util.ArrayList;
import java.util.UUID;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.util.Log;

public class BTConnectionHandler implements BTConnectThreadListener,
		BTConnectedThreadListener {

	private static BTConnectionHandler btConnectionHandler = null;
	public static final UUID MY_UUID = UUID.fromString("fa87c0d0-afac-11de-8a39-0800200c9a66"); //this value may read from app config

	private int maxConnections = 3;
	private int connectedCount = 0;
	private boolean workAsHost = false;
	private AcceptThread acceptThread = null;

	private ArrayList<BTConnection> connectionList = null;
	private ArrayList<BTConnectionListener> connectionsListeners = null;

	public BTConnectionHandler() {
		connectionList = new ArrayList<BTConnection>();
		connectionsListeners = new ArrayList<BTConnectionListener>();
	}

	public static BTConnectionHandler getSharedInstance() {
		if (btConnectionHandler == null) {
			btConnectionHandler = new BTConnectionHandler();
		}
		return btConnectionHandler;
	}

	public void addConnectionListener(BTConnectionListener listener) {
		if (listener == null) return;
		if (!connectionsListeners.contains(listener)) {
			connectionsListeners.add(listener);
		}
	}

	public void removeConnectionListener(BTConnectionListener listener) {
		if (listener == null) return;
		if (connectionsListeners.contains(listener)) {
			connectionsListeners.remove(listener);
		}
	}

	public int getMaxConnections() {
		return maxConnections;
	}

	public void setMaxConnections(int maxConnections) {
        if (maxConnections > 6) maxConnections = 6;
		this.maxConnections = maxConnections;
	}

	public int getConnectedCount() {
		return connectedCount;
	}

	public ArrayList<BTConnection> getConnectionList() {
		return connectionList;
	}

	//This writes data to given BTConnection
	public void writeDataToConnection(BTConnection connection, byte[] buffer) {
		BTConnectedThread cThread = connection.getBtConnectedThread();
		if (cThread != null && cThread.isAlive()) {
			cThread.write(buffer);
		}
	}

    /*
    This connects BluetoothDevice device
     */
	public synchronized void connectDevice(BluetoothDevice device) {
		if (device == null) {
			deviceNotConnected(device);
			return;
		}
		BTConnectThread connectThread = new BTConnectThread(device, this);
		connectThread.start();
	}

    /*
    This disconnects BluetoothDevice device
     */
	public synchronized void disconnectDevice(BluetoothDevice device) {
		changeConnectedCount(-1);
		BTConnection tmpConn = getConnectionOfDevice(device);
		tmpConn.disconnect();
		connectionDisconnected(tmpConn);
		connectionEnd(device);
	}

	//This adds connection to connection list
	private synchronized void addNewConnection(BTConnection connection) {
		if (connection != null) {
			synchronized (BTConnectionHandler.this) {
				connectionList.add(connection);
			}
		}
	}

	//This remove connection from connection list
	private synchronized void connectionDisconnected(BTConnection connection) {
		if (connection != null && connectionList.size() > 0) {
			synchronized (BTConnectionHandler.this) {
				connectionList.remove(connection);
			}
		}
	}

	//This closes all connections
	private void closeAllConnections() {

		synchronized (BTConnectionHandler.this) {

			changeConnectedCount(-10);

			for (BTConnection connection : connectionList) {
				try {
					connection.getBtConnectedThread().cancel();
					if (connection.getBtConnectThread() != null) {
						connection.setBtConnectThread(null);
					}
				} catch (Exception e) {

				}
			}

			connectionList.clear();
		}
	}

	/*
	Bluetooth broadcast receiver callbacks
	 */
	//This is called by Bluetooth broadcast receiver when a device disconnected
	public synchronized void removeDisconnectedRemoteConnection(
			BluetoothDevice device) {
		BTConnection tmpConn = getConnectionOfDevice(device);
		try {
			tmpConn.getBtConnectedThread().cancel();
			if (tmpConn.getBtConnectThread() != null) {
				tmpConn.setBtConnectThread(null);
			}
		} catch (Exception e) {

		}
		changeConnectedCount(-1);
		connectionDisconnected(tmpConn);
		connectionEnd(device);
	}

	//This is called by Bluetooth broadcast receiver when a device connected
	public synchronized void addRemoteDeviceIfNotExist(BluetoothDevice device) {
		//Since a connection is established in BTConnection handler this device should have a BTConnection
		BTConnection tmpConn = getConnectionOfDevice(device);
		if (tmpConn == null) {
			Log.d("D_TAG", "null connection");
		}
	}

	//This clears used data
	public void clear() {
		closeAllConnections();

		if (connectionsListeners != null)
			connectionsListeners.clear();

		if (acceptThread != null) {
			acceptThread.cancel();
			acceptThread = null;
		}
	}

	//This listen incoming connections when the device work as a BT host device
	public void startListenIncomingConnections() {
		if (acceptThread != null && acceptThread.isAlive()) {
			return;
		}

		if (acceptThread != null) {
			acceptThread.cancel();
			acceptThread = null;
		}

		acceptThread = new AcceptThread();
		acceptThread.start();
	}

	//Stop listening incoming connections
	public void stopListenIncomingConnections() {
		if (acceptThread != null && acceptThread.isAlive()) {
			acceptThread.cancel();
		}
	}

	//connection established from accept thread
	private synchronized void newConnectionEstablished(BluetoothSocket socket) {
		changeConnectedCount(1);

		BTConnection connection = new BTConnection();
		connection.setBtSocket(socket);
		BTConnectedThread connectedThread = new BTConnectedThread(socket, this);
		connection.setBtConnectedThread(connectedThread);
		connectedThread.start();
		addNewConnection(connection);

		newConnectionAdded(connection);
	}

	//connection established from Connect thread
	private synchronized void newConnectionEstablished(BluetoothSocket socket,
			BTConnectThread connectThread) {
		changeConnectedCount(1);

		BTConnection connection = new BTConnection();
		connection.setBtSocket(socket);
		connection.setBtConnectThread(connectThread);
		BTConnectedThread connectedThread = new BTConnectedThread(socket, this);
		connection.setBtConnectedThread(connectedThread);
		connectedThread.start();
		addNewConnection(connection);

		newConnectionAdded(connection);
	}

	//This disconnects BluetoothSocket when connection lost without prior notice
	private synchronized void disconnectBluetoothSocket(BluetoothSocket socket) {
		synchronized (BTConnectionHandler.this) {
			BluetoothDevice device = socket.getRemoteDevice();
			BTConnection tmpConn = getConnectionOfSocket(socket);
			if (tmpConn != null) {
				changeConnectedCount(-1);
				connectionDisconnected(tmpConn);
				connectionEnd(device);
			}
		}
	}

	private synchronized void changeConnectedCount(int change) {
		connectedCount += change;
		if (connectedCount < 0) {
			connectedCount = 0;
		}

		if (workAsHost && (connectedCount < maxConnections)) {
			startListenIncomingConnections();
		}
	}

	//This returns the BTConnection of a BluetoothSocket
	private synchronized BTConnection getConnectionOfSocket(
			BluetoothSocket socket) {
		BTConnection tmpConn = null;
		synchronized (BTConnectionHandler.this) {

			for (BTConnection connection : connectionList) {
				if (connection.getBtSocket().equals(socket)) {
					tmpConn = connection;
					break;
				}
			}
		}
		return tmpConn;
	}

	//This returns the BTConnection of a BluetoothDevice
	private synchronized BTConnection getConnectionOfDevice(
			BluetoothDevice device) {
		BTConnection tmpConn = null;
		synchronized (BTConnectionHandler.this) {
			for (BTConnection connection : connectionList) {
				if (connection.getBtDevice().equals(device)) {
					tmpConn = connection;
					break;
				}
			}
		}
		return tmpConn;
	}

	/*
	BTConnectionListeners update section
	 */
	//When a new connection established
	private synchronized void newConnectionAdded(BTConnection connection) {
		for (BTConnectionListener listener : connectionsListeners) {
			listener.connectionEstablished(connection);
		}
	}

	//When a connection disconnected
	private synchronized void connectionEnd(BluetoothDevice device) {
		for (BTConnectionListener listener : connectionsListeners) {
			listener.connectionDisconnected(device);
		}
	}

	//When data available on a connection
	private synchronized void dataAvailableOnSocket(BluetoothSocket socket,
			byte[] buffer) {
		for (BTConnectionListener listener : connectionsListeners) {
			listener.dataReceived(getConnectionOfSocket(socket), buffer);
		}
	}

	//When sending data failed
	private synchronized void dataNotSendOnSocket(BluetoothSocket socket,
			byte[] buffer) {
		for (BTConnectionListener listener : connectionsListeners) {
			listener.dataDidNotSend(getConnectionOfSocket(socket), buffer);
		}
	}

	//When data sent
	private synchronized void dataDidSendOnSocket(BluetoothSocket socket) {
		for (BTConnectionListener listener : connectionsListeners) {
			listener.dataDidSend(getConnectionOfSocket(socket));
		}
	}

	//When connection failed after try to connect
	private synchronized void deviceNotConnected(BluetoothDevice device) {
		for (BTConnectionListener listener : connectionsListeners) {
			listener.deviceNotConnected(device);
		}
	}

	/*
	 * Accept thread
	 * This thread run in a hosted device
	 */

	private class AcceptThread extends Thread {
		private final BluetoothServerSocket btServerSocket;

		public AcceptThread() {
			// Use a temporary object that is later assigned to mmServerSocket,
			// because mmServerSocket is final
			BluetoothServerSocket tmp = null;
			try {
				// MY_UUID is the app's UUID string, also used by the client
				// code
				tmp = BluetoothAdapter.getDefaultAdapter()
						.listenUsingRfcommWithServiceRecord("BTT",
								BTConnectionHandler.MY_UUID);
			} catch (IOException e) {
				e.printStackTrace();
			}
			btServerSocket = tmp;
			if (btServerSocket != null) {
				Log.d("D_TAG", "btServerSocket created");
			}
		}

		public void run() {
			BluetoothSocket socket = null;
			// Keep listening until exception occurs or a socket is returned
			while (true && connectedCount < maxConnections) {
				try {
					socket = btServerSocket.accept();
				} catch (IOException e) {
					e.printStackTrace();
					break;
				}
				// If a connection was accepted
				if (socket != null) {
					// Do work to manage the connection (in a separate thread)

					newConnectionEstablished(socket);

					try {
						if (connectedCount >= maxConnections) {
							btServerSocket.close();
							break;
						}
					} catch (IOException e) {
					}

					if (connectedCount >= maxConnections) {
						break;
					}

				}
			}
		}

		/** Will cancel the listening socket, and cause the thread to finish */
		public void cancel() {
			try {
				btServerSocket.close();
			} catch (IOException e) {
			}
		}
	}

	/*
	 * Connection thread delegates
	 */

	@Override
	public void btConnectedNewDataReceived(BluetoothSocket btSocket,
			byte[] buffer) {
		dataAvailableOnSocket(btSocket, buffer);
	}

	@Override
	public void btConnectedNewDataReadFailed(BluetoothSocket btSocket) {
		if (btSocket != null) {
			disconnectBluetoothSocket(btSocket);
		}
	}

	@Override
	public void btConnectedDataWriteSucceeded(BluetoothSocket btSocket) {
		dataDidSendOnSocket(btSocket);
	}

	@Override
	public void btConnectedDataWriteFailed(BluetoothSocket btSocket,
			byte[] buffer) {
		if (btSocket != null) {
			disconnectBluetoothSocket(btSocket);
		} else {
			dataNotSendOnSocket(btSocket, buffer);
		}
	}

	@Override
	public void btConnectConnected(BTConnectThread connectThread,
			BluetoothSocket btSocket) {
		newConnectionEstablished(btSocket, connectThread);
	}

	@Override
	public void btConnectFailed(BluetoothDevice btDevice) {
		deviceNotConnected(btDevice);
	}
}
