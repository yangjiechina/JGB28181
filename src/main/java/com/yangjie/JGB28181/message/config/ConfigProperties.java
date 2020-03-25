package com.yangjie.JGB28181.message.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.PropertySource;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "config")
@PropertySource("classpath:config.properties")
public class ConfigProperties {
	private String listenIp;
	private int listenPort;
	private String sipId;
	private String sipRealm;
	private String password;
	private String streamMediaIp;
	private String pullRtmpAddress;
	private String pushRtmpAddress;
	private boolean checkSsrc;
	
	public String getListenIp() {
		return listenIp;
	}
	public void setListenIp(String listenIp) {
		this.listenIp = listenIp;
	}
	public int getListenPort() {
		return listenPort;
	}
	public void setListenPort(int listenPort) {
		this.listenPort = listenPort;
	}
	public String getSipId() {
		return sipId;
	}
	public void setSipId(String sipId) {
		this.sipId = sipId;
	}
	public String getSipRealm() {
		return sipRealm;
	}
	public void setSipRealm(String sipRealm) {
		this.sipRealm = sipRealm;
	}
	public String getPassword() {
		return password;
	}
	public void setPassword(String password) {
		this.password = password;
	}
	public String getStreamMediaIp() {
		return streamMediaIp;
	}
	public void setStreamMediaIp(String streamMediaIp) {
		this.streamMediaIp = streamMediaIp;
	}
	public String getPullRtmpAddress() {
		return pullRtmpAddress;
	}
	public void setPullRtmpAddress(String pullRtmpAddress) {
		this.pullRtmpAddress = pullRtmpAddress;
	}
	public String getPushRtmpAddress() {
		return pushRtmpAddress;
	}
	public void setPushRtmpAddress(String pushRtmpAddress) {
		this.pushRtmpAddress = pushRtmpAddress;
	}
	public boolean isCheckSsrc() {
		return checkSsrc;
	}
	public void setCheckSsrc(boolean checkSsrc) {
		this.checkSsrc = checkSsrc;
	}

}
