package com.kriegersoftware.demos.usbinterface.activity;

import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.UsbConstants;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbEndpoint;
import android.hardware.usb.UsbInterface;
import android.hardware.usb.UsbManager;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.util.Log;
import android.view.Menu;

import com.kriegersoftware.demos.usbinterface.R;
import com.kriegersoftware.demos.usbinterface.driver.CDCACMSerialDriver;
import com.kriegersoftware.demos.usbinterface.driver.UsbSerialDriver;
import com.kriegersoftware.demos.usbinterface.fragment.ControllersFragment;
import com.kriegersoftware.demos.usbinterface.fragment.TerminalFragment;
import com.kriegersoftware.demos.usbinterface.util.SerialInputOutputManager;

public class HomeActivity extends Activity {
	
	private static final String ACTION_USB_PERMISSION = "com.android.example.USB_PERMISSION";
	String TAG = "USB";
	final boolean SHOW_LOGCAT = true;
	Handler mHandler = new Handler();
	private TerminalFragment terminal;
	private ControllersFragment controllers;
	protected boolean mStopped=false;
	private Thread connectionThread;
	private UsbManager mUSBManager;
	
	private static UsbSerialDriver sDriver = null;
	private final ExecutorService mExecutor = Executors.newSingleThreadExecutor();

    private SerialInputOutputManager mSerialIoManager;

    private final SerialInputOutputManager.Listener mListener =
            new SerialInputOutputManager.Listener() {

        @Override
        public void onRunError(Exception e) {
            Log.d(TAG, "Runner stopped.");
        }

        @Override
        public void onNewData(final byte[] data) {
            HomeActivity.this.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    HomeActivity.this.updateReceivedData(data);
                }
            });
        }
    };
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_home);
	
		terminal = (TerminalFragment) getFragmentManager().findFragmentById(R.id.home_fragment_terminal);
		controllers = (ControllersFragment) getFragmentManager().findFragmentById(R.id.home_fragment_controllers);
		
		String filePath = Environment.getExternalStorageDirectory() + "/Logcat/logcat.txt";
        try {
        	Runtime.getRuntime().exec(new String[]{"logcat", "-c"});
            Runtime.getRuntime().exec(new String[]{"logcat", "-f", filePath});
            terminal.addTextToTerminal(null, TerminalFragment.ORIGIN_APP, "Logcat is being saved to "+filePath);
        } catch (IOException e) {
        	terminal.addTextToTerminal(null, TerminalFragment.ORIGIN_ERROR, "Unable to write to Logcat");
        }
        
        PendingIntent mPermissionIntent = PendingIntent.getBroadcast(this,
        		0, new Intent(ACTION_USB_PERMISSION), 0);
        IntentFilter filter = new IntentFilter(ACTION_USB_PERMISSION);
//        filter.addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED);
        filter.addAction(UsbManager.ACTION_USB_DEVICE_DETACHED);
        registerReceiver(mUsbReceiver, filter);
        
        mUSBManager=(UsbManager) getSystemService(USB_SERVICE);
        controllers.setUSBParamters(mUSBManager,
        		mPermissionIntent);
        controllers.refreshDeviceList();
	}

	@Override
	protected void onDestroy() {
		unregisterReceiver(mUsbReceiver);
		super.onDestroy();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.home, menu);
		return true;
	}
	
	public void sendMessageToDevice(String data){
		if(data==null){
			terminal.addTextToTerminal(null, TerminalFragment.ORIGIN_ERROR, "Null data!");
			return;
		}
		mSerialIoManager.writeAsync(data.getBytes());
	}
	
    BroadcastReceiver mUsbReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
    		String action = intent.getAction();
	        if (ACTION_USB_PERMISSION.equals(action)) {
	            synchronized (this) {
	                final UsbDevice device = (UsbDevice)intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
	                if (intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)) {
	                    if(device != null){
	                    	connectionThread = new Thread(new Runnable() {
								public void run() {
	                                terminal.addThreadSafeTextToTerminal(null,
	                                		TerminalFragment.ORIGIN_APP,
	                                		"Access to device was granted correctly");
	                                getDeviceStatus(device);
	                                UsbDeviceConnection connection = mUSBManager.openDevice(device);
	                                sDriver = new CDCACMSerialDriver(device, connection);
	                                if (sDriver == null) {
	                                	terminal.addThreadSafeTextToTerminal(null,
		                                		TerminalFragment.ORIGIN_ERROR,
		                                		"No driver available");
	                                } else {
	                                    try {
	                                        sDriver.open();
	                                        sDriver.setParameters(115200, 8, UsbSerialDriver.STOPBITS_1, UsbSerialDriver.PARITY_NONE);
	                                    } catch (IOException e) {
	                                        terminal.addThreadSafeTextToTerminal(null,
			                                		TerminalFragment.ORIGIN_ERROR,
			                                		"Error opening device: " + e.getMessage());
	                                        try {
	                                            sDriver.close();
	                                        } catch (IOException e2) {
	                                        }
	                                        sDriver = null;
	                                        return;
	                                    }
	                                    terminal.addThreadSafeTextToTerminal(deviceName,
		                                		TerminalFragment.ORIGIN_APP,
		                                		"Connection was successful through: " + sDriver.getClass().getSimpleName());
	                                }
	                                onDeviceStateChange();
	                            }
	                        });
	                    	connectionThread.start();
	                   }
	                } 
	                else {
	                	terminal.addTextToTerminal(null, TerminalFragment.ORIGIN_ERROR, "Error! Permission was denied!");
	                }
	            }
	        }
//    		if (UsbManager.ACTION_USB_DEVICE_ATTACHED.equals(action)) {
//    			Toast.makeText(getApplicationContext(), "ATTACHED", Toast.LENGTH_LONG).show();
//    		} else
    		if (UsbManager.ACTION_USB_DEVICE_DETACHED.equals(action)) {
    			terminal.addTextToTerminal(null, TerminalFragment.ORIGIN_APP, "USB Device was detached");
    			stopIoManager();
    		}
        }
    };
	private String deviceName;
    
    /*
     * Enumerate the endpoints and interfaces on the connected device.
     * We do not need permission to do anything here, it is all "publicly available"
     * until we try to connect to an actual device.
     */
    public String readDevice(UsbDevice device) {
        StringBuilder sb = new StringBuilder();
        deviceName=device.getDeviceName();
        sb.append("Device Properties:\nDevice Name: " + deviceName + "\n");
        sb.append(String.format(
                "Device Class: %s -> Subclass: 0x%02x -> Protocol: 0x%02x\n",
                nameForClass(device.getDeviceClass()),
                device.getDeviceSubclass(), device.getDeviceProtocol()));

        for (int i = 0; i < device.getInterfaceCount(); i++) {
            UsbInterface intf = device.getInterface(i);
            sb.append(String
                    .format("+-- Interface %d Class: %s -> Subclass: 0x%02x -> Protocol: 0x%02x\n",
                            intf.getId(),
                            nameForClass(intf.getInterfaceClass()),
                            intf.getInterfaceSubclass(),
                            intf.getInterfaceProtocol()));

            for (int j = 0; j < intf.getEndpointCount(); j++) {
                UsbEndpoint endpoint = intf.getEndpoint(j);
                sb.append(String.format("   +--- Endpoint %d: %s %s\n",
                        endpoint.getEndpointNumber(),
                        nameForEndpointType(endpoint.getType()),
                        nameForDirection(endpoint.getDirection())));
            }
        }

        return sb.toString();
    }
    
    /*
     * Initiate a control transfer to request the first configuration
     * descriptor of the device.
     */
    //Type: Indicates whether this is a read or write
    // Matches USB_ENDPOINT_DIR_MASK for either IN or OUT
    private static final int REQUEST_TYPE = 0x80;
    //Request: GET_CONFIGURATION_DESCRIPTOR = 0x06
    private static final int REQUEST = 0x06;
    //Value: Descriptor Type (High) and Index (Low)
    // Configuration Descriptor = 0x2
    // Index = 0x0 (First configuration)
    private static final int REQ_VALUE = 0x200;
    private static final int REQ_INDEX = 0x00;
    private static final int LENGTH = 64;
    private void getDeviceStatus(UsbDevice device) {
        UsbDeviceConnection connection = mUSBManager.openDevice(device);
        
        //Create a sufficiently large buffer for incoming data
        byte[] buffer = new byte[LENGTH];
        connection.controlTransfer(REQUEST_TYPE, REQUEST, REQ_VALUE, REQ_INDEX,
                buffer, LENGTH, 2000);
        //Parse received data into a description
        String description = parseConfigDescriptor(buffer);
        
        deviceName=device.getDeviceName();
        terminal.addThreadSafeTextToTerminal(null,
        		TerminalFragment.ORIGIN_APP,
        		description);
        connection.close();
    }
    
    /*
     * Parse the USB configuration descriptor response per the
     * USB Specification.  Return a printable description of
     * the connected device.
     */
    private static final int DESC_SIZE_CONFIG = 9;
    private String parseConfigDescriptor(byte[] buffer) {
        StringBuilder sb = new StringBuilder();
        //Parse configuration descriptor header
        int totalLength = (buffer[3] &0xFF) << 8;
        totalLength += (buffer[2] & 0xFF);
        //Interface count
        int numInterfaces = (buffer[5] & 0xFF);
        //Configuration attributes
        int attributes = (buffer[7] & 0xFF);        
        //Power is given in 2mA increments
        int maxPower = (buffer[8] & 0xFF) * 2;
        
        sb.append("Configuration Descriptor:\n");
        sb.append("Length: " + totalLength + " bytes\n");
        sb.append(numInterfaces + " Interfaces\n");
        sb.append(String.format("Attributes:%s%s%s\n",
                (attributes & 0x80) == 0x80 ? " BusPowered" : "",
                (attributes & 0x40) == 0x40 ? " SelfPowered" : "",
                (attributes & 0x20) == 0x20 ? " RemoteWakeup" : ""));
        sb.append("Max Power: " + maxPower + "mA\n");
        
        //The rest of the descriptor is interfaces and endpoints
        int index = DESC_SIZE_CONFIG; 
        while (index < totalLength) {
            //Read length and type
            int len = (buffer[index] & 0xFF);
            int type = (buffer[index+1] & 0xFF);
            switch (type) {
            case 0x04: //Interface Descriptor
                int intfNumber = (buffer[index+2] & 0xFF);
                int numEndpoints = (buffer[index+4] & 0xFF);
                int intfClass = (buffer[index+5] & 0xFF);
                
                sb.append(String.format("+-- Interface %d, %s, %d Endpoints\n",
                        intfNumber, nameForClass(intfClass), numEndpoints));
                break;
            case 0x05: //Endpoint Descriptor
                int endpointAddr = ((buffer[index+2] & 0xFF));
                //Number is lower 4 bits
                int endpointNum = (endpointAddr & 0x0F);
                //Direction is high bit
                int direction = (endpointAddr & 0x80);
                
                int endpointAttrs = (buffer[index+3] & 0xFF);
                //Type is the lower two bits
                int endpointType = (endpointAttrs & 0x3);
                
                sb.append(String.format("   +--- Endpoint %d, %s %s\n",
                        endpointNum,
                        nameForEndpointType(endpointType),
                        nameForDirection(direction) ));
                break;
            }
            //Advance to next descriptor
            index += len;
        }
        
        return sb.toString();
    }
    
/* Helper Methods to Provide Readable Names for USB Constants */
    
    private String nameForClass(int classType) {
        switch (classType) {
        case UsbConstants.USB_CLASS_APP_SPEC:
            return String.format("Application Specific 0x%02x", classType);
        case UsbConstants.USB_CLASS_AUDIO:
            return "Audio";
        case UsbConstants.USB_CLASS_CDC_DATA:
            return "CDC Control";
        case UsbConstants.USB_CLASS_COMM:
            return "Communications";
        case UsbConstants.USB_CLASS_CONTENT_SEC:
            return "Content Security";
        case UsbConstants.USB_CLASS_CSCID:
            return "Content Smart Card";
        case UsbConstants.USB_CLASS_HID:
            return "Human Interface Device";
        case UsbConstants.USB_CLASS_HUB:
            return "Hub";
        case UsbConstants.USB_CLASS_MASS_STORAGE:
            return "Mass Storage";
        case UsbConstants.USB_CLASS_MISC:
            return "Wireless Miscellaneous";
        case UsbConstants.USB_CLASS_PER_INTERFACE:
            return "(Defined Per Interface)";
        case UsbConstants.USB_CLASS_PHYSICA:
            return "Physical";
        case UsbConstants.USB_CLASS_PRINTER:
            return "Printer";
        case UsbConstants.USB_CLASS_STILL_IMAGE:
            return "Still Image";
        case UsbConstants.USB_CLASS_VENDOR_SPEC:
            return String.format("Vendor Specific 0x%02x", classType);
        case UsbConstants.USB_CLASS_VIDEO:
            return "Video";
        case UsbConstants.USB_CLASS_WIRELESS_CONTROLLER:
            return "Wireless Controller";
        default:
            return String.format("0x%02x", classType);
        }
    }

    private String nameForEndpointType(int type) {
        switch (type) {
        case UsbConstants.USB_ENDPOINT_XFER_BULK:
            return "Bulk";
        case UsbConstants.USB_ENDPOINT_XFER_CONTROL:
            return "Control";
        case UsbConstants.USB_ENDPOINT_XFER_INT:
            return "Interrupt";
        case UsbConstants.USB_ENDPOINT_XFER_ISOC:
            return "Isochronous";
        default:
            return "Unknown Type";
        }
    }

    private String nameForDirection(int direction) {
        switch (direction) {
        case UsbConstants.USB_DIR_IN:
            return "IN";
        case UsbConstants.USB_DIR_OUT:
            return "OUT";
        default:
            return "Unknown Direction";
        }
    }

    private void stopIoManager() {
        if (mSerialIoManager != null) {
            Log.i(TAG, "Stopping io manager ..");
            terminal.addThreadSafeTextToTerminal(deviceName,
            		TerminalFragment.ORIGIN_DEVICE,
            		"Stopping IO manager...");
            mSerialIoManager.stop();
            mSerialIoManager = null;
            deviceName=null;
        }
    }

    private void startIoManager() {
        if (sDriver != null) {
            Log.i(TAG, "Starting io manager ..");
            terminal.addThreadSafeTextToTerminal(deviceName,
            		TerminalFragment.ORIGIN_DEVICE,
            		"Starting IO manager...");
            mSerialIoManager = new SerialInputOutputManager(sDriver, mListener);
            mExecutor.submit(mSerialIoManager);
        }
    }

    private void onDeviceStateChange() {
        stopIoManager();
        startIoManager();
    }

    private void updateReceivedData(byte[] data) {
        terminal.addTextToTerminal(deviceName,
        		TerminalFragment.ORIGIN_DEVICE,
        		new String(data));
    }
}
