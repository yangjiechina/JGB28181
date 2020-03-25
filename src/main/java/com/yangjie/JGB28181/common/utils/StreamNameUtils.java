package com.yangjie.JGB28181.common.utils;

/**
 * 生成推流名称的工具类
 * @author yangjie
 * 2020年3月18日
 */
public class StreamNameUtils {

	private static StringBuffer prefix(String deviceId,String channelId){
		StringBuffer buffer =new StringBuffer(50);
		buffer.append(deviceId);
		buffer.append("_");
		buffer.append(channelId);
		return buffer;
	}
	public static String play(String deviceId,String channelId){
		return prefix(deviceId,channelId).append("_Play").toString();
	}
	public static String playBack(String deviceId,String channelId,String startTime){
		return prefix(deviceId,channelId).append("_Playback").append("_").append(startTime).toString();
	}
	public static String download(String deviceId,String channelId,String startTime){
		return prefix(deviceId,channelId).append("_Download").append("_").append(startTime).toString();
	}
	public static String talk(String deviceId,String channelId){
		return prefix(deviceId,channelId).append("_Talk").toString();
	}
}
