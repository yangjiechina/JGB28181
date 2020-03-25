package com.yangjie.JGB28181.media.codec;

public  class Packet {

	public  static final int I = 0;
	public 	static final int P = 1;
	public static final  int AUDIO = 2;
	public  static final int SUB_PACKET = 3;

	private int timeStamp;   

	private int seq;

	private byte[] data;

	private int packetType;

	public  int getPacketType(){

		return packetType;
	}


	public Packet(int seq, byte[] data, int packetType) {
		this.seq = seq;
		this.data = data;
		this.packetType = packetType;
	}


	public byte[] getData(){
		return data;
	}

	public int getTimeStamp() {
		return timeStamp;
	}

	public void setTimeStamp(int timeStamp) {
		this.timeStamp = timeStamp;
	}

	public int getSeq() {
		return seq;
	}

	public void setSeq(int seq) {
		this.seq = seq;
	}

	public void setData(byte[] data) {
		this.data = data;
	}
}
