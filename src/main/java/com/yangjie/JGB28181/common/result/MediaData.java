package com.yangjie.JGB28181.common.result;

public class MediaData {

	private String address;
	
	private String callId;

	public MediaData(String address, String callId) {
		this.address = address;
		this.callId = callId;
	}

	public String getAddress() {
		return address;
	}

	public void setAddress(String address) {
		this.address = address;
	}

	public String getCallId() {
		return callId;
	}

	public void setCallId(String callId) {
		this.callId = callId;
	}
	
}
