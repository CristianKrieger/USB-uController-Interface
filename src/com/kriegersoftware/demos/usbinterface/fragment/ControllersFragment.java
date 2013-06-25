package com.kriegersoftware.demos.usbinterface.fragment;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

import android.app.Fragment;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbInterface;
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
import com.kriegersoftware.demos.usbinterface.entities.USBDevice;
import com.kriegersoftware.demos.usbinterface.sysusb.SysUsbDevice;
import com.kriegersoftware.demos.usbinterface.sysusb.SysUsbManager;
import com.kriegersoftware.demos.usbinterface.util.UsbConstants;

public class ControllersFragment extends Fragment implements OnClickListener{
	private int deviceNum;
	private int currentDevice=-1;
	private ViewGroup deviceContainer;
	private final static String CURRENT_DEVICE_INDICATOR = " [Current]";
	private View actionsContainer;
	private boolean deviceListVisible=true;
	private ImageView img_toggleButton;
	private ImageView img_refreshButton;
	private ArrayList<USBDevice> devicesToShow;
	private LayoutInflater lInflater;
	public TerminalFragment terminal;
	private final static String NO_ANDROID_DEVICES  = "No Android devices are connected.";
	private final static String NO_SYSTEM_DEVICES  = "No System devices are connected.";
	private final static String DEVICES_FOUND  = " devices were found.";
	private final static String REFRESHING  = "Refreshing device list...";
	private final static String DEVICE_SELECTED  = "Selected device: ";
	
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
		
		new DeviceFinderAsyncTask().execute();
		return view;
	}
	
	private void populateDeviceList(){
		deviceContainer.removeAllViews();
		if(!deviceListVisible)
			deviceListVisible^=true;
		currentDevice=-1;
		actionsContainer.setVisibility(View.GONE);
		
		deviceNum=devicesToShow.size();
		for(int i=0; i<deviceNum; i++){
			USBDevice current = devicesToShow.get(i);
			View deviceRow = lInflater.inflate(R.layout.row_controllers_device, null);
			String top = current.getName();
			if(current.isSystemDevice())
				top+=" [System]";
			((TextView)deviceRow.findViewById(R.id.row_devices_txt_top)).setText(top);
			((TextView)deviceRow.findViewById(R.id.row_devices_txt_bottom)).setText(current.getDevicePath());
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
	
	private class DeviceFinderAsyncTask extends AsyncTask<Void, Void, Void>{
		private HashMap<String, UsbDevice> deviceList;
		private SysUsbManager mUsbManagerLinux;
		private HashMap<String, SysUsbDevice> mLinuxUsbDeviceList;
		
		@Override
		protected Void doInBackground(Void... params) {
			@SuppressWarnings("static-access")
			UsbManager manager = (UsbManager)
					getActivity().getSystemService(getActivity().USB_SERVICE);
			deviceList = manager.getDeviceList();
			mUsbManagerLinux = new SysUsbManager();
			return null;
		}
		@Override
		protected void onPostExecute(Void result) {
			devicesToShow = new ArrayList<USBDevice>();
			Iterator<UsbDevice> deviceIterator = deviceList.values().iterator();
			
			terminal = (TerminalFragment) getActivity()
					.getFragmentManager().findFragmentById(R.id.home_fragment_terminal);
			
			if(!deviceIterator.hasNext())
				terminal.addTextToTerminal(terminal.formatTerminalText(null,
						TerminalFragment.ORIGIN_APP, NO_ANDROID_DEVICES));
				//Toast.makeText(getActivity(), "No Android devices were found!", Toast.LENGTH_LONG).show();
			else{
				String[] paths = deviceList.keySet().toArray(new String[deviceList.keySet().size()]);
				int i=0;
				while(deviceIterator.hasNext()){					
				    UsbDevice device = deviceIterator.next();
				    
				    String dClass=null;
				    UsbInterface iface;
					for(int j = 0 ; j < device.getInterfaceCount() ; j++){
						iface = device.getInterface(j);
						if(iface != null){
							dClass=UsbConstants.resolveUsbClass(iface.getInterfaceClass());
						}
					}
				    
				    devicesToShow.add(new USBDevice(device.getDeviceName(),
				    		Integer.toString(device.getDeviceId()),
				    		false,
				    		null,
				    		Integer.toString(device.getVendorId()),
				    		dClass,
				    		paths[i++],
				    		null,
				    		null));
				    //Toast.makeText(getActivity(), device.getDeviceName(), Toast.LENGTH_LONG).show();
				}
			}
			
			mLinuxUsbDeviceList = mUsbManagerLinux.getUsbDevices();
			Iterator<SysUsbDevice> systemDeviceIterator = mLinuxUsbDeviceList.values().iterator();
			if(!systemDeviceIterator.hasNext())
				terminal.addTextToTerminal(terminal.formatTerminalText(null,
						TerminalFragment.ORIGIN_APP, NO_SYSTEM_DEVICES));
				//Toast.makeText(getActivity(), "No Linux devices were found!", Toast.LENGTH_LONG).show();
			else
				while(systemDeviceIterator.hasNext()){
				    SysUsbDevice device = systemDeviceIterator.next();
				    devicesToShow.add(new USBDevice(device.getReportedProductName(),
				    		device.getPID(),
				    		true,
				    		device.getReportedVendorName(),
				    		device.getVID(),
				    		device.getDeviceClass(),
				    		device.getDevicePath(),
				    		device.getSerialNumber(),
				    		device.getMaxPower()));
				    //Toast.makeText(getActivity(), device.getReportedProductName(), Toast.LENGTH_LONG).show();
				}
			terminal.addTextToTerminal(terminal.formatTerminalText(null,
					TerminalFragment.ORIGIN_APP, Integer.toString(devicesToShow.size())+DEVICES_FOUND));
			populateDeviceList();
			super.onPostExecute(result);
		}
	}

	@Override
	public void onClick(View v) {
		int tag = (Integer) v.getTag();
		setCurrentDevice(tag);
		terminal.addTextToTerminal(terminal.formatTerminalText(null,
				TerminalFragment.ORIGIN_APP, DEVICE_SELECTED+devicesToShow.get(tag).getName()));
	}
	
	private class FragmentActionListener implements OnClickListener{
		@Override
		public void onClick(View v) {
			switch (v.getId()) {
			case R.id.frag_controllers_img_toggle:
				toggleDeviceListVisibility();
				break;
			case R.id.frag_controllers_img_refresh:
				terminal.addTextToTerminal(terminal.formatTerminalText(null,
						TerminalFragment.ORIGIN_APP, REFRESHING));
				new DeviceFinderAsyncTask().execute();
				break;
			default:
				break;
			}
		}
	}
}
