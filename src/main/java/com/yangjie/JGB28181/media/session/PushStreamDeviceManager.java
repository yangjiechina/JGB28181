package com.yangjie.JGB28181.media.session;

import java.util.concurrent.ConcurrentHashMap;

import com.yangjie.JGB28181.bean.PushStreamDevice;

/**
 * 推流设备会话管理器
 * @author yangjie
 * 2020年3月17日
 */
public class PushStreamDeviceManager {

	/**
	 * 主关系map
	 * (deviceId+channelId+sessionName)->PushDevice
	 */
	private ConcurrentHashMap<String, PushStreamDevice> mainMap;

	/**
	 * callId关系map
	 * callId->主关系Id
	 */
	private ConcurrentHashMap<String, String> callIdMap;

	/**
	 * 推流channel id关系map
	 * channelid->主关系id
	 */
	private ConcurrentHashMap<String, String> channelIdMap;


	/**
	 * ssrc关系map
	 * ssrc->主关系id
	 */
	private ConcurrentHashMap<Integer, String> ssrcMap;


	private static PushStreamDeviceManager pushStreamDeviceManager;


	private PushStreamDeviceManager(){
		mainMap = new ConcurrentHashMap<>();
		callIdMap = new ConcurrentHashMap<>();
		channelIdMap =new ConcurrentHashMap<>();
		ssrcMap = new ConcurrentHashMap<>();
	}
	public static  PushStreamDeviceManager getInstance(){
		if(pushStreamDeviceManager==null){
			synchronized (PushStreamDeviceManager.class) {
				pushStreamDeviceManager = new PushStreamDeviceManager();
			}
		}
		return pushStreamDeviceManager;
	}

	public  void put(String mainKey,String callId,Integer ssrc,PushStreamDevice device){
		mainMap.put(mainKey, device);
		callIdMap.put(callId, mainKey);
		ssrcMap.put(ssrc, mainKey);
	}
	public void put(String channelId,String mainKey){
		channelIdMap.put(channelId, mainKey);
	}
	public PushStreamDevice get(String mainKey){
		return mainMap.get(mainKey);
	}
	public PushStreamDevice getByCallId(String callId){
		return mainMap.get(callIdMap.get(callId));
	}
	public PushStreamDevice getBySsrc(Integer ssrc){
		return mainMap.get(ssrcMap.get(ssrc));
	}
	public PushStreamDevice getByChannelId(String channelId){
		return mainMap.get(channelIdMap.get(channelId));
	}
	public PushStreamDevice remove(String mainKey){
		if(mainKey == null){
			return null;
		}
		PushStreamDevice device = mainMap.remove(mainKey);
		if(device!= null){
			Integer ssrc = device.getSsrc();
			if(ssrc != null){
				ssrcMap.remove(ssrc);
			}
			String callId = device.getCallId();
			if(callId != null){
				callIdMap.remove(callId);
			}
			String channelId = device .getChannelId();
			if(channelId != null){
				channelIdMap.remove(channelId);
			}
		}
		return device;
	}
	public PushStreamDevice removeBySsrc(Integer ssrc){
		
		return ssrc!= null?remove(ssrcMap.remove(ssrc)):null;
	}
	public PushStreamDevice removeByCallId(String callId){
		return callId!= null ? remove(callIdMap.remove(callId)):null;
	}
	public PushStreamDevice removeByChannelId(String channelId){
		return channelId != null ? remove(channelIdMap.remove(channelId)):null;
	}
}
