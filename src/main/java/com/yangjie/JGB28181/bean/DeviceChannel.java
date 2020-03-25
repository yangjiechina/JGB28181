package com.yangjie.JGB28181.bean;

public class DeviceChannel {

	/**
	 * 通道id
	 */
	private String deviceId;
	
	/**
	 * 通道名
	 */
	private String name;
	
	/**
	 * 在线/离线
	 * 0在线，其他离线
	 * 默认在线
	 * 信令:
	 * <Status>ON</Status>
	 * <Status>OFF</Status>
	 * 遇到过NVR下的IPC下发信令可以推流， 但是 Status 响应 OFF
	 */
	private int status;

	public String getDeviceId() {
		return deviceId;
	}

	public void setDeviceId(String deviceId) {
		this.deviceId = deviceId;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public int getStatus() {
		return status;
	}

	public void setStatus(int status) {
		this.status = status;
	}
	
}
