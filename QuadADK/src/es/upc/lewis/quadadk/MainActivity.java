package es.upc.lewis.quadadk;

import java.io.IOException;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.ParcelFileDescriptor;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;

import com.android.future.usb.UsbAccessory;
import com.android.future.usb.UsbManager;

public class MainActivity extends Activity {
	private static final String TAG = MainActivity.class.getSimpleName();
	
	private PendingIntent mPermissionIntent;
	private static final String ACTION_USB_PERMISSION = "com.android.example.USB_PERMISSION";
	private boolean mPermissionRequestPending;
	
	private UsbManager mUsbManager;
	private UsbAccessory mAccessory;
	private ParcelFileDescriptor mFileDescriptor;
	
	// Worker thread for ADK communications
	private CommunicationsThread comms;
	
	// UI references
	private TextView connectionStatusText;
	private TextView sensor1Text;
	private TextView sensor2Text;
	private TextView sensor3Text;
	private Button connectButton;
	private Button readSensor1Button;
	private Button readSensor2Button;
	private Button readSensor3Button;
	// UI states
	private static final int CONNECTED = 1;
    private static final int DISCONNECTED = 2;

	private void openAccessory(UsbAccessory accessory) {
		mFileDescriptor = mUsbManager.openAccessory(accessory);
		if (mFileDescriptor != null) {
			mAccessory = accessory;
			
			comms = new CommunicationsThread(this, mFileDescriptor.getFileDescriptor());
			comms.start();
			
			setUi(CONNECTED);
			Log.i(TAG, "Accessory opened");
		} else {
			Log.e(TAG, "Error opening accessory");
		}
	}

	private void closeAccessory() {
		// Stop worker thread
		if (comms != null) {
			comms.interrupt();
			comms = null;
		}

		try {
			if (mFileDescriptor != null) {
				mFileDescriptor.close();
			}
		} catch (IOException e) {
		} finally {
			mFileDescriptor = null;
			mAccessory = null;

		}
		
		setUi(DISCONNECTED);
	}
	
	private OnClickListener connectButtonListener = new OnClickListener() {
		@Override
		public void onClick(View v) {
			// Connect
			if (mAccessory == null) {
				connect();
			}
			// Disconnect
			else {
				closeAccessory();				
			}
		}
	};
	
	private OnClickListener readSensorButtonListener = new OnClickListener() {
		@Override
		public void onClick(View v) {
			if (mAccessory == null) { return; }
			
			switch(v.getId()) {
			case R.id.read_sensor_1_button:
				comms.send(Commands.READ_SENSOR_1);
				break;
			case R.id.read_sensor_2_button:
				comms.send(Commands.READ_SENSOR_2);
				break;
			case R.id.read_sensor_3_button:
				comms.send(Commands.READ_SENSOR_3);
				break;
			}
		}
	};
	
	private void connect() {
		UsbAccessory[] accessories = mUsbManager.getAccessoryList();
		UsbAccessory accessory = (accessories == null ? null : accessories[0]);
		
		if (accessory != null) {
			if (mUsbManager.hasPermission(accessory)) {
				openAccessory(accessory);
			} else {
				synchronized (usbReceiver) {
					if (!mPermissionRequestPending) {
						mUsbManager.requestPermission(accessory, mPermissionIntent);
						mPermissionRequestPending = true;
					}
				}
			}
		} else {
			Log.e(TAG, "Error connecting");
		}
	}
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		setContentView(R.layout.main_activity);
		getUiReferences();
		connectButton.setOnClickListener(connectButtonListener);
		readSensor1Button.setOnClickListener(readSensorButtonListener);
		readSensor2Button.setOnClickListener(readSensorButtonListener);
		readSensor3Button.setOnClickListener(readSensorButtonListener);
		
		setUi(DISCONNECTED);
		
		mUsbManager = UsbManager.getInstance(this);
		mPermissionIntent = PendingIntent.getBroadcast(this, 0, new Intent(ACTION_USB_PERMISSION), 0);
		registerReceivers();
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		closeAccessory();
		unregisterReceivers();
	}
	
	private void getUiReferences() {
		connectionStatusText = (TextView) findViewById(R.id.connection_status);
		sensor1Text = (TextView) findViewById(R.id.sensor_1_text);
		sensor2Text = (TextView) findViewById(R.id.sensor_2_text);
		sensor3Text = (TextView) findViewById(R.id.sensor_3_text);
		connectButton = (Button) findViewById(R.id.connect_button);
		readSensor1Button = (Button) findViewById(R.id.read_sensor_1_button);
		readSensor2Button = (Button) findViewById(R.id.read_sensor_2_button);
		readSensor3Button = (Button) findViewById(R.id.read_sensor_3_button);
	}
	
	private void setUi(int type) {
    	switch (type) {
    		case CONNECTED:
    			connectionStatusText.setText("Connected");
    			connectButton.setText("Disconnect");
    			readSensor1Button.setEnabled(true);
    			readSensor2Button.setEnabled(true);
    			readSensor3Button.setEnabled(true);
    			break;
    		case DISCONNECTED:
    			connectionStatusText.setText("Disconnected");
    			connectButton.setText("Connect");
    			readSensor1Button.setEnabled(false);
    			readSensor2Button.setEnabled(false);
    			readSensor3Button.setEnabled(false);
    			break;
    	}
    }
	
	private void registerReceivers() {
		// Sensor data receiver
    	LocalBroadcastManager.getInstance(this).registerReceiver(sensorDataReceiver,sensorDataIntentFilter());
    	
    	// USB events
    	IntentFilter filter = new IntentFilter(ACTION_USB_PERMISSION);
		filter.addAction(UsbManager.ACTION_USB_ACCESSORY_DETACHED);
		registerReceiver(usbReceiver, filter);
    }
    
    private void unregisterReceivers() {
    	// Sensor data receiver
    	LocalBroadcastManager.getInstance(this).unregisterReceiver(sensorDataReceiver);
    	
    	// USB events
    	unregisterReceiver(usbReceiver);
    }
    
    private BroadcastReceiver sensorDataReceiver = new BroadcastReceiver() {
    	  @Override
    	  public void onReceive(Context context, Intent intent) {
    		  String action = intent.getAction();
    		  int value = intent.getIntExtra(CommunicationsThread.VALUE, 0);
    		  
    		  if (action.equals(CommunicationsThread.ACTION_DATA_AVAILABLE_SENSOR_1)) {
    			  sensor1Text.setText(Integer.toString(value));
    		  }
    		  else if (action.equals(CommunicationsThread.ACTION_DATA_AVAILABLE_SENSOR_2)) {
    			  sensor2Text.setText(Integer.toString(value));
    		  }
    		  else if (action.equals(CommunicationsThread.ACTION_DATA_AVAILABLE_SENSOR_3)) {
    			  sensor3Text.setText(Integer.toString(value));
    		  }
    	  }
    };
    
    private static IntentFilter sensorDataIntentFilter() {
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(CommunicationsThread.ACTION_DATA_AVAILABLE_SENSOR_1);
        intentFilter.addAction(CommunicationsThread.ACTION_DATA_AVAILABLE_SENSOR_2);
        intentFilter.addAction(CommunicationsThread.ACTION_DATA_AVAILABLE_SENSOR_3);
        return intentFilter;
    }
    
    private final BroadcastReceiver usbReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			String action = intent.getAction();
			
			// Granted USB permission
			if (ACTION_USB_PERMISSION.equals(action)) {
				synchronized (this) {
					UsbAccessory accessory = UsbManager.getAccessory(intent);
					if (intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)) {
						openAccessory(accessory);
					} else {
						Log.e(TAG, "Permission denied for accessory " + accessory);
					}
					mPermissionRequestPending = false;
				}
				
			// USB disconnected
			} else if (UsbManager.ACTION_USB_ACCESSORY_DETACHED.equals(action)) {
				UsbAccessory accessory = UsbManager.getAccessory(intent);
				if (accessory != null && accessory.equals(mAccessory)) {
					closeAccessory();
				}
			}
		}
	};
}