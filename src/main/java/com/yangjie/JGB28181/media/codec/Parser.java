package com.yangjie.JGB28181.media.codec;

import java.util.Map;

public interface Parser {

	/**
	 * TCP包长字节
	 * 2个字节长
	 */
	int TCP_PACKET_LENGTH = 2;
	/**
	 * 有扩展字段，但是没有遇到过。
	 * 基本都为12字节
	 */
	int RTP_HEADER_LENGTH = 12;

	/**
	 * UDP模式，除去rtp头的起始字节
	 */
	int UDP_START_INDEX = RTP_HEADER_LENGTH;
	/**
	 * TCP模式，比UDP模式多2个字节
	 */
	int TCP_START_INDEX = TCP_PACKET_LENGTH +RTP_HEADER_LENGTH;

	/**
	 * UDP模式
	 * ps扩展内容字段索引
	 * rtp(12)+ 00 00 01 ba(4)+10字节(长度固定，最后一个字节低3位，为扩展内容长度)
	 */
	int UDP_PS_HEADER_STUFFING_LENGTH_INDEX = 25;

	/**
	 * TCP模式，依次延长2字节
	 */
	int TCP_PS_HEADER_STUFFING_LENGTH_INDEX = UDP_PS_HEADER_STUFFING_LENGTH_INDEX+2;
	/**
	 * crc32校验
	 * 固定4字节长度
	 */
	int CRC_32_LENGTH = 4;
	
	void parseUdp(Map<Integer,Packet> packetMap,int firstSeq,int endSeq) throws Exception;

	void parseTcp(Frame frame) throws Exception;
}
