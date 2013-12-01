package com.kriegersoftware.demos.usbinterface.fragment;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

import android.app.Fragment;
import android.app.PendingIntent;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.kriegersoftware.demos.usbinterface.R;
import com.kriegersoftware.demos.usbinterface.activity.HomeActivity;
import com.kriegersoftware.demos.usbinterface.util.USBConstants;

public class ControllersFragment extends Fragment implements OnClickListener{
	private int deviceNum;
	public int currentDevice=-1;
	private ViewGroup deviceContainer;
	
	private View actionsContainer;
	private boolean deviceListVisible=true;
	private ImageView img_toggleButton;
	private ImageView img_refreshButton;
	private ArrayList<UsbDevice> devicesAvailable;
	private LayoutInflater lInflater;
	private TerminalFragment terminal;
	private DeviceFinderAsyncTask task;
	private PendingIntent mPermissionIntent;
	private UsbManager manager;
	
	private final static String CURRENT_DEVICE_INDICATOR = " [Current]";
	public final static String NO_ANDROID_DEVICES  = "No USB devices are connected.";
	private final static String DEVICES_FOUND  = "Devices found: ";
	private final static String REFRESHING  = "Refreshing device list...";
	private final static String DEVICE_SELECTED  = "Selected device: ";
	private final static String COM_STARTED  = "Started communication with: ";

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		lInflater=inflater;
		View view = inflater.inflate(R.layout.fragment_controllers, container);
		
		actionsContainer = view.findViewById(R.id.frag_controllers_container_actions);
		deviceContainer = (ViewGroup) view.findViewById(R.id.frag_controllers_container_devices);
		
		img_toggleButton = (ImageView)view.findViewById(R.id.frag_controllers_img_toggle);
		img_toggleButton.setOnClickListener(new FragmentActionListener());
		
		img_refreshButton = (ImageView)view.findViewById(R.id.frag_controllers_img_refresh);
		img_refreshButton.setOnClickListener(new FragmentActionListener());
		
		view.findViewById(R.id.frag_controllers_button_start).setOnClickListener(new FragmentActionListener());
		view.findViewById(R.id.frag_controllers_button_details).setOnClickListener(new FragmentActionListener());
		return view;
	}
	
	private void populateDeviceList(){
		deviceContainer.removeAllViews();
		if(!deviceListVisible)
			deviceListVisible^=true;
		currentDevice=-1;
		actionsContainer.setVisibility(View.GONE);
		
		deviceNum=devicesAvailable.size();
		for(int i=0; i<deviceNum; i++){
			UsbDevice current = devicesAvailable.get(i);
			View deviceRow = lInflater.inflate(R.layout.row_controllers_device, null);
			String top = current.getDeviceName();
			if(top==null)
				top="Unnamed Device";
			top+="\nClass: ";
			String usbClass=USBConstants.resolveUsbClass(current.getDeviceClass());
			if(usbClass==null)
				usbClass="Unknown";
			top+=usbClass;
			String bottom = Integer.toString(current.getProductId());
			if(bottom==null)
				bottom="No PID";
			else
				bottom="PID: "+bottom;
			String vendorID= Integer.toString(current.getVendorId());
			if(vendorID==null)
				vendorID="No VID";
			else
				vendorID="VID: "+vendorID;
			bottom+=(". "+vendorID);
			((TextView)deviceRow.findViewById(R.id.row_devices_txt_top)).setText(top);
			((TextView)deviceRow.findViewById(R.id.row_devices_txt_bottom)).setText(bottom);
			deviceRow.setTag(i);
			deviceRow.setOnClickListener(this);
			deviceContainer.addView(deviceRow);
		}
	}
	
	private void toggleDeviceListVisibility(){
		int visibility;
		Drawable drawable;
		if(deviceListVisible){
			visibility=View.GONE;
			drawable=getResources().getDrawable(R.drawable.selector_controllers_toggleclosed);
		}else{
			visibility=View.VISIBLE;
			drawable=getResources().getDrawable(R.drawable.selector_controllers_toggleopened);
		}
		for(int i=0; i<deviceNum; i++){
			if(i!=currentDevice){
				deviceContainer.findViewWithTag(i).setVisibility(visibility);
			}
		}
		img_toggleButton.setImageDrawable(drawable);
		deviceListVisible^=true;
	}
	
	private void setCurrentDevice(int newCurrentTag){
		if(currentDevice==newCurrentTag)
			return;
		if(currentDevice != -1){
			
			View deviceRow = deviceContainer.findViewWithTag(currentDevice);
			deviceRow.setBackgroundColor(Color.parseColor("#0033B5E5"));
			TextView tvTop = (TextView)deviceRow.findViewById(R.id.row_devices_txt_top);
			String text=tvTop.getText().toString();
			text=text.replace(CURRENT_DEVICE_INDICATOR, "");
			tvTop.setText(text);
		}else
			actionsContainer.setVisibility(View.VISIBLE);
		currentDevice=newCurrentTag;
		toggleDeviceListVisibility();
		View deviceRow = deviceContainer.findViewWithTag(currentDevice);
		deviceRow.setBackgroundColor(Color.parseColor("#8833B5E5"));
		TextView tvTop = (TextView)deviceRow.findViewById(R.id.row_devices_txt_top);
		tvTop.setText(tvTop.getText().toString()+CURRENT_DEVICE_INDICATOR);
	}
	
	public void refreshDeviceList(){
		if(task!=null){
			task.cancel(false);
		}else{
			task=new DeviceFinderAsyncTask();
			task.execute();
		}
	}
	
	public void setUSBParamters(UsbManager manager, PendingIntent mPermissionIntent){
		this.mPermissionIntent=mPermissionIntent;
		this.manager=manager;
	}
	
	private void requestUSBPermission(){
		if(currentDevice>-1)
			manager.requestPermission(devicesAvailable.get(currentDevice), mPermissionIntent);
	}
	
	public boolean deviceHasBeenSelected(){
		return currentDevice!=-1;
	}
	
	public void addDeviceDataToTerminal(String data){
		terminal.addTextToTerminal(devicesAvailable.get(currentDevice).getDeviceName(),
				TerminalFragment.ORIGIN_DEVICE, data);
	}
	
	private class DeviceFinderAsyncTask extends AsyncTask<Void, Void, Void>{
		private HashMap<String, UsbDevice> deviceList;
		
		@Override
		protected Void doInBackground(Void... params) {
			deviceList = manager.getDeviceList();
			return null;
		}
		
		@Override
		protected void onPostExecute(Void result) {
			Iterator<UsbDevice> deviceIterator = deviceList.values().iterator();
			devicesAvailable=new ArrayList<UsbDevice>();
			
			terminal = (TerminalFragment) getActivity()
					.getFragmentManager().findFragmentById(R.id.home_fragment_terminal);
			
			if(!deviceIterator.hasNext())
				terminal.addTextToTerminal(null, TerminalFragment.ORIGIN_APP,
						NO_ANDROID_DEVICES);
			else{
				while(deviceIterator.hasNext()){					
				    UsbDevice device = deviceIterator.next();
				    devicesAvailable.add(device);
				}
				terminal.addTextToTerminal(null, TerminalFragment.ORIGIN_APP,
						DEVICES_FOUND+Integer.toString(devicesAvailable.size()));
			}
			populateDeviceList();
			super.onPostExecute(result);
		}
	}

	@Override
	public void onClick(View v) {
		int tag = (Integer) v.getTag();
		setCurrentDevice(tag);
		terminal.addTextToTerminal(null, TerminalFragment.ORIGIN_APP,
				DEVICE_SELECTED+devicesAvailable.get(tag).getDeviceName());
	}
	
	private class FragmentActionListener implements OnClickListener{
		@Override
		public void onClick(View v) {
			switch (v.getId()) {
			case R.id.frag_controllers_img_toggle:
				toggleDeviceListVisibility();
				break;
			case R.id.frag_controllers_img_refresh:
				terminal.addTextToTerminal(null, TerminalFragment.ORIGIN_APP,
						REFRESHING);
				new DeviceFinderAsyncTask().execute();
				break;
			case R.id.frag_controllers_button_details:
				terminal.addTextToTerminal(null, TerminalFragment.ORIGIN_APP,
						((HomeActivity)getActivity()).readDevice(devicesAvailable.get(currentDevice)));
				break;
			case R.id.frag_controllers_button_start:
				requestUSBPermission();
				terminal.addTextToTerminal(null, TerminalFragment.ORIGIN_APP,
						COM_STARTED+devicesAvailable.get(currentDevice).getDeviceName());
				break;
			default:
				break;
			}
		}
	}
}
