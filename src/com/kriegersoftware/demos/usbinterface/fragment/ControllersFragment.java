package com.kriegersoftware.demos.usbinterface.fragment;

import com.kriegersoftware.demos.usbinterface.R;

import android.app.Fragment;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

public class ControllersFragment extends Fragment implements OnClickListener{
	private int deviceNum;
	private int currentDevice=-1;
	private ViewGroup deviceContainer;
	private final static String CURRENT_DEVICE_INDICATOR = " [Current]";
	private View actionsContainer;
	private boolean deviceListVisible=true;
	private ImageView img_toggleButton;
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		View view = inflater.inflate(R.layout.fragment_controllers, container);
		actionsContainer = view.findViewById(R.id.frag_controllers_container_actions);
		deviceContainer = (ViewGroup) view.findViewById(R.id.frag_controllers_container_devices);
		img_toggleButton = (ImageView)view.findViewById(R.id.frag_controllers_img_toggle);
		img_toggleButton.setOnClickListener(new FragmentActionListener());
		
		deviceNum=5;
		for(int i=0; i<deviceNum; i++){
			View deviceRow = inflater.inflate(R.layout.row_controllers_device, null);
			((TextView)deviceRow.findViewById(R.id.row_devices_txt_top)).setText("PIC18F4550");
			((TextView)deviceRow.findViewById(R.id.row_devices_txt_bottom)).setText("Device ID: 0x56873434");
			deviceRow.setTag(i);
			deviceRow.setOnClickListener(this);
			deviceContainer.addView(deviceRow);
		}
		return view;
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

	@Override
	public void onClick(View v) {
		int tag = (Integer) v.getTag();
		setCurrentDevice(tag);
	}
	
	private class FragmentActionListener implements OnClickListener{
		@Override
		public void onClick(View v) {
			switch (v.getId()) {
			case R.id.frag_controllers_img_toggle:
				toggleDeviceListVisibility();
				break;
			default:
				break;
			}
		}
	}
}
