package com.yangjie.JGB28181.bean;

import java.util.Date;
import java.util.concurrent.ConcurrentLinkedDeque;

import javax.sip.Dialog;

import com.yangjie.JGB28181.media.codec.Frame;
import com.yangjie.JGB28181.media.server.Server;
import com.yangjie.JGB28181.media.server.remux.Observer;

public class PushStreamDevice {

	/**
	 * 设备ID
	 */
	private String deviceId;
	/**
	 * ssrc
	 * invite下发时携带字段
	 * 终端摄像头推流会携带上
	 */
	private Integer ssrc;
	/**
	 * sip callId
	 */
	private String callId;
	
	private Dialog dialog;
	/**
	 * rtmp推流名称
	 */
	private String streamName;

	/**
	 * 网络通道ID
	 */
	private String channelId;
	/**
	 * 是否正在推流
	 */
	private boolean pushing; 

	/**
	 * 创建时间
	 */
	private Date createDate;

	/**
	 * 推流时间
	 */
	private Date pushStreamDate;

	/**
	 * 监听的收流端口
	 */
	private int port;

	/**
	 * 是否是 tcp传输音视频
	 */
	private boolean isTcp;

	/**
	 * 接受的视频帧队列
	 */
	private ConcurrentLinkedDeque<Frame> frameDeque = new ConcurrentLinkedDeque<Frame>();


	/**
	 * 接受流的Server
	 */
	private Server server;
	
	/**
	 * 处理封装流的监听者
	 */
	private Observer observer;

	/**
	 * 拉流地址
	 */
	private String pullRtmpAddress;


	public PushStreamDevice(String deviceId, Integer ssrc, String callId, String streamName, int port,
			boolean isTcp, Server server,Observer observer,String pullRtmpAddress) {
		this.deviceId = deviceId;
		this.ssrc = ssrc;
		this.callId = callId;
		this.streamName = streamName;
		this.port = port;
		this.isTcp = isTcp;
		this.server = server;
		this.observer = observer;
		this.pullRtmpAddress = pullRtmpAddress;
	}

	public Integer getSsrc() {
		return ssrc;
	}

	public void setSsrc(Integer ssrc) {
		this.ssrc = ssrc;
	}

	public String getDeviceId() {
		return deviceId;
	}

	public void setDeviceId(String deviceId) {
		this.deviceId = deviceId;
	}

	public String getChannelId() {
		return channelId;
	}

	public void setChannelId(String channelId) {
		this.channelId = channelId;
	}

	public boolean isPushing() {
		return pushing;
	}

	public void setPushing(boolean pushing) {
		this.pushing = pushing;
	}

	public Date getCreateDate() {
		return createDate;
	}

	public void setCreateDate(Date createDate) {
		this.createDate = createDate;
	}

	public Date getPushStreamDate() {
		return pushStreamDate;
	}

	public void setPushStreamDate(Date pushStreamDate) {
		this.pushStreamDate = pushStreamDate;
	}

	public int getPort() {
		return port;
	}

	public void setPort(int port) {
		this.port = port;
	}

	public boolean isTcp() {
		return isTcp;
	}

	public void setTcp(boolean isTcp) {
		this.isTcp = isTcp;
	}

	public ConcurrentLinkedDeque<Frame> getFrameDeque() {
		return frameDeque;
	}

	public void setFrameDeque(ConcurrentLinkedDeque<Frame> frameDeque) {
		this.frameDeque = frameDeque;
	}

	public String getCallId() {
		return callId;
	}

	public void setCallId(String callId) {
		this.callId = callId;
	}

	public String getStreamName() {
		return streamName;
	}

	public void setStreamName(String streamName) {
		this.streamName = streamName;
	}

	public Server getServer() {
		return server;
	}

	public void setServer(Server server) {
		this.server = server;
	}

	public Observer getObserver() {
		return observer;
	}

	public void setObserver(Observer observer) {
		this.observer = observer;
	}

	public String getPullRtmpAddress() {
		return pullRtmpAddress;
	}

	public void setPullRtmpAddress(String pullRtmpAddress) {
		this.pullRtmpAddress = pullRtmpAddress;
	}

	public Dialog getDialog() {
		return dialog;
	}

	public void setDialog(Dialog dialog) {
		this.dialog = dialog;
	}
}
