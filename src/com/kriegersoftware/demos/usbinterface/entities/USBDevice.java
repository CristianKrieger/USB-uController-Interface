package com.kriegersoftware.demos.usbinterface.entities;

public class USBDevice {
	private String name;
	private String productID;
	private boolean isSystemDevice;
	private String vendorName;
	private String vendorID;
	private String deviceClass;
	private String devicePath;
	private String serialNumber;
	private String maxPower;
	
	public USBDevice(String name, String productID, boolean isSystemDevice,
			String vendorName, String vendorID, String deviceClass,
			String devicePath, String serialNumber, String maxPower) {
		this.name = name;
		this.productID = productID;
		this.isSystemDevice = isSystemDevice;
		this.vendorName = vendorName;
		this.vendorID = vendorID;
		this.deviceClass = deviceClass;
		this.devicePath = devicePath;
		this.serialNumber = serialNumber;
		this.maxPower = maxPower;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getProductID() {
		return productID;
	}

	public void setProductID(String productID) {
		this.productID = productID;
	}

	public boolean isSystemDevice() {
		return isSystemDevice;
	}

	public void setSystemDevice(boolean isSystemDevice) {
		this.isSystemDevice = isSystemDevice;
	}

	public String getVendorName() {
		return vendorName;
	}

	public void setVendorName(String vendorName) {
		this.vendorName = vendorName;
	}

	public String getVendorID() {
		return vendorID;
	}

	public void setVendorID(String vendorID) {
		this.vendorID = vendorID;
	}

	public String getDeviceClass() {
		return deviceClass;
	}

	public void setDeviceClass(String deviceClass) {
		this.deviceClass = deviceClass;
	}

	public String getDevicePath() {
		return devicePath;
	}

	public void setDevicePath(String devicePath) {
		this.devicePath = devicePath;
	}

	public String getSerialNumber() {
		return serialNumber;
	}

	public void setSerialNumber(String serialNumber) {
		this.serialNumber = serialNumber;
	}

	public String getMaxPower() {
		return maxPower;
	}

	public void setMaxPower(String maxPower) {
		this.maxPower = maxPower;
	}
}
