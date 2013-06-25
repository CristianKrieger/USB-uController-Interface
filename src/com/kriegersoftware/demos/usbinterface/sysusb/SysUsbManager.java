package com.kriegersoftware.demos.usbinterface.sysusb;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;

import com.kriegersoftware.demos.usbinterface.util.ExecTerminal;

public class SysUsbManager {
	//final String TAG =  this.getClass().getName();
	private HashMap<String, SysUsbDevice> myUsbDevices;
	
	private static final String PATH_SYS_BUS_USB = "/sys/bus/usb/devices/";
	private static final String DEVICE_START = "__DEV_START__";
	private static final String DEVICE_END = "__DEV_END__";
	
	private static final String COMMAND_GET_USB_INFO = "for DEVICE in /sys/bus/usb/devices/*; do " +
			" echo "+ DEVICE_START + ";" +
			" [ -f $DEVICE/idProduct ] && echo PID: $(cat $DEVICE/idProduct);" +
			" [ -f $DEVICE/idVendor ] && echo BUSNUM: $(cat $DEVICE/busnum);" +
			" [ -f $DEVICE/idVendor ] && echo DEVCLASS: $(cat $DEVICE/bDeviceClass);" +
			" [ -f $DEVICE/idVendor ] && echo DEVNUM: $(cat $DEVICE/devnum);" +
			" [ -f $DEVICE/idVendor ] && echo DEVPROTOCOL: $(cat $DEVICE/bDeviceProtocol);" +
			" [ -f $DEVICE/idVendor ] && echo DEVSUBCLASS: $(cat $DEVICE/bDeviceSubClass);" +
			" [ -f $DEVICE/idVendor ] && echo MAXPOWER: $(cat $DEVICE/bMaxPower);" +
			" [ -f $DEVICE/idVendor ] && echo SERIAL: $(cat $DEVICE/serial);" +
			" [ -f $DEVICE/idVendor ] && echo SPEED: $(cat $DEVICE/speed);" +
			" [ -f $DEVICE/idVendor ] && echo VERSION: $(cat $DEVICE/version);" +			
			" [ -f $DEVICE/idVendor ] && echo VID: $(cat $DEVICE/idVendor);" +
			" [ -f $DEVICE/product ] && echo MANUFACTURER: $(cat $DEVICE/manufacturer);"  +
			" [ -f $DEVICE/product ] && echo PRODUCT: $(cat $DEVICE/product);"  +
			" echo "+ DEVICE_END + ";" +
			" done";

	public static String getUsbInfoViaShell(){
		String res = (new ExecTerminal()).exec(COMMAND_GET_USB_INFO);
		
		res = res.replace(DEVICE_START + "\n" + DEVICE_END + "\n", "");
		return res;
	}

	public SysUsbManager(){
		myUsbDevices = new HashMap<String, SysUsbDevice>();
	}

	public HashMap<String, SysUsbDevice> getUsbDevices(){
		populateList(PATH_SYS_BUS_USB);
		
		return myUsbDevices;
	}

	private void populateList(String path){
		SysUsbDevice usb;
		
		myUsbDevices.clear();	
		
		File dir = new File(path);
		
		if (!dir.isDirectory()){return;}
		
		for (File child : dir.listFiles()) {
			
			if (".".equals(child.getName()) || "..".equals(child.getName())) {
				continue;  // Ignore the self and parent aliases.
			}
			
			String parentPath = child.getAbsolutePath() + File.separator;
			
			usb = new SysUsbDevice();
			usb.setDevicePath(parentPath);
			usb.setBusNumber(readFileContents(parentPath + "busnum"));
			usb.setDeviceClass(readFileContents(parentPath + "bDeviceClass"));
			usb.setDeviceNumber(readFileContents(parentPath + "devnum"));
			usb.setDeviceProtocol(readFileContents(parentPath + "bDeviceProtocol"));
			usb.setDeviceSubClass(readFileContents(parentPath + "bDeviceSubClass"));
			usb.setMaxPower(readFileContents(parentPath + "bMaxPower"));
			usb.setPID(readFileContents(parentPath + "idProduct"));
			usb.setReportedProductName(readFileContents(parentPath + "product"));
			usb.setReportedVendorName(readFileContents(parentPath + "manufacturer"));
			usb.setSerialNumber(readFileContents(parentPath + "serial"));
			usb.setSpeed(readFileContents(parentPath + "speed"));
			usb.setVID(readFileContents(parentPath + "idVendor"));
			usb.setUsbVersion(readFileContents(parentPath + "version"));

			if(usb.getBusNumber().length() > 0 && usb.getDeviceNumber().length() > 0){
				String key = child.getName();
				myUsbDevices.put(key, usb);
			}
		}
	}

	private String readFileContents(String filePath){
		File file = new File(filePath);
		if(!file.exists()){return "";}
		if(file.isDirectory()){return "";}
		
        StringBuffer fileData = new StringBuffer(1000);
        BufferedReader reader;
		try {
			reader = new BufferedReader(new FileReader(filePath));

        char[] buf = new char[1024];
        int numRead=0;
        
        while((numRead=reader.read(buf)) != -1){
            String readData = String.valueOf(buf, 0, numRead);
            fileData.append(readData);
            buf = new char[1024];
        }
        
        reader.close();
        
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			return "";
		} catch (IOException e) {
			e.printStackTrace();
			return "";
		}

		String res = fileData.toString();
		if(res == null){
			res = "";
		}
        return res.trim();
	}
}
