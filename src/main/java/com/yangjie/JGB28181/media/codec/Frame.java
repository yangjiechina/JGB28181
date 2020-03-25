package com.yangjie.JGB28181.media.codec;

import java.util.HashMap;
import java.util.Map;

public class Frame {


	public  static final int I = 0;
	public 	static final int P = 1;
	public 	static final int AUDIO = 2;
	public  static final int SUB_PACKET = 3;
	private static final int I_INITIAL_CAPACITY = 30;
	private static final int P_INITIAL_CAPACITY = 10;
	private static final int AUDIO_INITIAL_CAPACITY = 3;

	Map<Integer,byte[]> packetMap = new HashMap<Integer, byte[]>();

	/**
	 * 第一个包序列号
	 */
	private int firstSeq;

	/**
	 * 最后一个包序列号
	 */
	private int endSeq;


	private int frameType ;

	public Frame(int frameType){
		this.frameType = frameType; 
		packetMap = new HashMap<Integer, byte[]>(frameType == I ? I_INITIAL_CAPACITY : frameType == P ?P_INITIAL_CAPACITY:AUDIO_INITIAL_CAPACITY); 
	}
	public void addPacket(Integer order,byte[] data){
		packetMap.put(order, data);
	}

	public Map<Integer,byte[]> getPacketMap(){
		return packetMap;
	}

	public int getFrameType(){
		return frameType;
	}
	public void clear(){
		if(packetMap != null){
			packetMap.clear();
		}
	}
	public int getFirstSeq() {
		return firstSeq;
	}
	public void setFirstSeq(int firstSeq) {
		this.firstSeq = firstSeq;
	}
	public int getEndSeq() {
		return endSeq;
	}
	public void setEndSeq(int endSeq) {
		this.endSeq = endSeq;
	}
}
