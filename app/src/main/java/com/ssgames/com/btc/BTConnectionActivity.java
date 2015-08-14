package com.ssgames.com.btc;

import java.util.ArrayList;

import android.app.Activity;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothClass;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

public class BTConnectionActivity extends Activity implements OnClickListener,
		BTConnectionListener {

	private BluetoothAdapter btAdapter = BluetoothAdapter.getDefaultAdapter();

	private ArrayList<BTConnection> connectionList = null;
	private ArrayList<BluetoothDevice> availableList = null;

	private BTConnectionHandler btConnectionHandler = null;
	private BluetoothDevice connectingDevice = null;

	private ImageButton searchButton;
	private ImageButton visibleButton;
	private LinearLayout conneLayout;
	private TextView txtNoConn;
	private TextView txtSearch;

	/*
	 * Blue tooth broadcast receiver
	 */
	private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
		public void onReceive(Context context, Intent intent) {
			String action = intent.getAction();
			if (BluetoothDevice.ACTION_FOUND.equals(action)) {
				BluetoothDevice device = intent
						.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
				if (device.getBluetoothClass().getDeviceClass() == BluetoothClass.Device.PHONE_SMART) {
					bluetoothDeviceFound(device);
				}
			} else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED
					.equals(action)) {
				txtSearch.setText("SEARCH FOR DEVICES");
				refreshViews();
			} else if (BluetoothDevice.ACTION_ACL_DISCONNECTED.equals(action)) {
				BluetoothDevice device = intent
						.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
				btConnectionHandler.removeDisconnectedRemoteConnection(device);
				connectionList = btConnectionHandler.getConnectionList();
				refreshViews();
			} else if (BluetoothDevice.ACTION_ACL_CONNECTED.equals(action)) {
				BluetoothDevice device = intent
						.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
				btConnectionHandler.addRemoteDeviceIfNotExist(device);
				connectionList = btConnectionHandler.getConnectionList();
				refreshViews();
			}
		}
	};

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.connection_screen);

		btConnectionHandler = BTConnectionHandler.getSharedInstance();

		searchButton = (ImageButton) findViewById(R.id.btnSearch);
		visibleButton = (ImageButton) findViewById(R.id.btnVisible);
		conneLayout = (LinearLayout) findViewById(R.id.liConnections);
		txtNoConn = (TextView) findViewById(R.id.txtNoConn);
		txtSearch = (TextView) findViewById(R.id.txtSearch);

		txtNoConn.setVisibility(View.VISIBLE);

		searchButton.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				searchButtonClicked();
			}
		});

		visibleButton.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				visibleButtonClicked();
			}
		});

		availableList = new ArrayList<BluetoothDevice>();
		connectionList = BTConnectionHandler.getSharedInstance()
				.getConnectionList();

		// Register for broadcasts when a device is discovered
		IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
		this.registerReceiver(mReceiver, filter);

		// Register for broadcasts when discovery has finished
		filter = new IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
		this.registerReceiver(mReceiver, filter);

		// Register for broadcasts when device is disconnect
		filter = new IntentFilter(BluetoothDevice.ACTION_ACL_DISCONNECTED);
		this.registerReceiver(mReceiver, filter);

		// Register for broadcasts when device is connect
		filter = new IntentFilter(BluetoothDevice.ACTION_ACL_CONNECTED);
		this.registerReceiver(mReceiver, filter);

		refreshViews();

		btConnectionHandler.addConnectionListener(this);
		btConnectionHandler.startListenIncomingConnections();
		
		String nickName = SettingsManager.getSetting(UserKey.NICK_NAME, null, getApplicationContext());
		if (nickName == null || nickName.length() == 0 || nickName.equalsIgnoreCase("you")) {
			getPlayerNickName();
		}else {
			renameBluetoothName(nickName);
			searchButtonClicked();
		}
	}
	
	private void renameBluetoothName(String name) {
		//TODO
	}
	
	private void getPlayerNickName() {
		AlertDialog.Builder alert = new AlertDialog.Builder(this);

		alert.setTitle(R.string.msg_get_player_name);
 
		final EditText input = new EditText(this);
		alert.setView(input);

		alert.setPositiveButton(R.string.msg_btn_ok, new DialogInterface.OnClickListener() {
		public void onClick(DialogInterface dialog, int whichButton) {
			String name = input.getText().toString().trim();
			saveNickName(name);
		}
		});

		alert.show();
	}
	
	private void saveNickName(String name) {
		if (name.length() == 0) {
			getPlayerNickName();
		}else {
			SettingsManager.addSetting(UserKey.NICK_NAME, name, getApplicationContext());
			renameBluetoothName(name);
			searchButtonClicked();
		}
	}
	
	private void startGame() {
		//TODO
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();

		btConnectionHandler.removeConnectionListener(this);

		if (btAdapter != null) {
			btAdapter.cancelDiscovery();
		}

		this.unregisterReceiver(mReceiver);
	}

	private void connectionSelected(BluetoothDevice btDevice) {
		connectionList = btConnectionHandler.getConnectionList();
		if (connectionList.size() > 1) {
			for (BTConnection connection : connectionList) {
				BluetoothDevice device = connection.getBtDevice();
				if (!btDevice.getAddress().equals(device.getAddress())) {
					disconnectBluetoothDevice(device);
					break;
				}
			}
		}
		
//		Intent intent = new Intent();
//		setResult(RESULT_OK, intent);

		startGame();
		
		finish();
	}

	/*
	 * UI updates
	 */

	private void refreshViews() {
		separateDeviceLists();

		conneLayout.removeAllViews();
		conneLayout.addView(txtNoConn);

		if (availableList.size() == 0 && connectionList.size() == 0) {
			txtNoConn.setVisibility(View.VISIBLE);
		} else {
			txtNoConn.setVisibility(View.GONE);
		}

		for (BTConnection connection : connectionList) {
			BluetoothDevice device = connection.getBtDevice();
			if (device != null) {
				TTTDeviceCellView deviceCell = new TTTDeviceCellView(this, null);
				deviceCell.setBtDevice(device);
				deviceCell.setCellOnClickListner(this);
				deviceCell.setConnection(true);
				deviceCell.setDeviceName(device.getName());
				conneLayout.addView(deviceCell);
			}
		}

		for (BluetoothDevice device : availableList) {
			TTTDeviceCellView deviceCell = new TTTDeviceCellView(this, null);
			deviceCell.setBtDevice(device);
			deviceCell.setCellOnClickListner(this);
			deviceCell.setConnection(false);
			deviceCell.setDeviceName(device.getName());
			conneLayout.addView(deviceCell);
		}
	}

	private void searchButtonClicked() {
		startDiscovery();
		txtSearch.setText("SEARCHING...");
	}

	private void visibleButtonClicked() {
		enableDiscoverability();
	}

	private void enableDiscoverability() {
		Intent discoverableIntent = new Intent(
				BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
		discoverableIntent.putExtra(
				BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 300);
		startActivity(discoverableIntent);
	}

	/*
	 * BluetoothDevice handling methods separate
	 */

	private void separateDeviceLists() {
		for (BTConnection connection : connectionList) {
			if (availableList.contains(connection.getBtDevice())) {
				availableList.remove(connection.getBtDevice());
				break;
			}
		}
	}

	private void startDiscovery() {

		if (btAdapter.isDiscovering()) {
			btAdapter.cancelDiscovery();
		}

		btAdapter.startDiscovery();
		Log.d("D_TAG", "Discovering...");
	}

	private void bluetoothDeviceFound(BluetoothDevice device) {

		boolean exist = false;

		for (BTConnection connection : connectionList) {
			if (connection.getBtDevice().getAddress()
					.equalsIgnoreCase(device.getAddress())) {
				exist = true;
			}
		}

		if (!exist && !availableList.contains(device)) {
			availableList.add(device);
			showFoundDevice(device);
		}
	}

	private void showFoundDevice(BluetoothDevice device) {

		TTTDeviceCellView deviceCell = new TTTDeviceCellView(this, null);
		deviceCell.setBtDevice(device);
		deviceCell.setCellOnClickListner(this);
		deviceCell.setConnection(false);
		deviceCell.setDeviceName(device.getName());
		conneLayout.addView(deviceCell);

	}

	private void connectBluetoothDevice(BluetoothDevice device) {
		btConnectionHandler.connectDevice(device);
	}

	private void disconnectBluetoothDevice(BluetoothDevice device) {
		btConnectionHandler.disconnectDevice(device);
	}

	private void connectingFailed() {
		AlertDialog.Builder alert = new AlertDialog.Builder(this);
		alert.setTitle("Device not connected!");
		alert.setPositiveButton("Ok", null);
		alert.show();
	}

	@Override
	public void onClick(View v) {
		if (v instanceof TTTDeviceCellView) {
			final TTTDeviceCellView deviceCell = (TTTDeviceCellView) v;

			if (deviceCell.isConnection()) {
				connectionSelected(deviceCell.getBtDevice());
			} else {
				connectingDevice = deviceCell.getBtDevice();
				connectBluetoothDevice(connectingDevice);
			}

		}
	}

	@Override
	public void connectionEstablished(BTConnection connection) {

		final BluetoothDevice btDevice = connection.getBtDevice();

		runOnUiThread(new Runnable() {

			@Override
			public void run() {
				connectionList = btConnectionHandler.getConnectionList();
				refreshViews();
				connectionSelected(btDevice);
			}
		});

	}

	@Override
	public void connectionDisconnected(BluetoothDevice device) {
		runOnUiThread(new Runnable() {

			@Override
			public void run() {
				connectionList = btConnectionHandler.getConnectionList();
				refreshViews();
			}
		});

	}

	@Override
	public void deviceNotConnected(BluetoothDevice device) {
		if (device != null && connectingDevice.equals(device)) {

			runOnUiThread(new Runnable() {

				@Override
				public void run() {
					connectingFailed();
				}
			});

			connectingDevice = null;
		}
	}

	@Override
	public void dataReceived(BTConnection connection, byte[] buffer) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void dataDidSend(BTConnection connection) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void dataDidNotSend(BTConnection connection, byte[] buffer) {
		// TODO Auto-generated method stub
		
	}
}
