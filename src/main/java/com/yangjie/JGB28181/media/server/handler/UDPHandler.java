package com.yangjie.JGB28181.media.server.handler;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentLinkedDeque;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.yangjie.JGB28181.common.utils.BitUtils;
import com.yangjie.JGB28181.common.utils.HexStringUtils;
import com.yangjie.JGB28181.media.codec.Packet;
import com.yangjie.JGB28181.media.codec.Parser;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.socket.DatagramPacket;

/*
 * UDP 由于在外网环境下(特别是4G)，乱序 丢包情况较为严重
 * 所以处理方式和TCP有所区别
 * 先将每个包按照rtp头中的seq来保存，再做组包、解析、推流
 * 组包策略:
 * 比如收到第三个I帧，开始解析第一个I帧
 * 缓存长度字段：CACHE_FRAME_LENGTH
 */
public class UDPHandler  extends SimpleChannelInboundHandler<DatagramPacket>  {

	private Logger log = LoggerFactory.getLogger(getClass());

	/**
	 * 存储关键数据包(i/p/audio)seq的map
	 */
	private ConcurrentLinkedDeque<Integer> mSeqMap = new ConcurrentLinkedDeque<>();
	
	private Map<Integer,Packet> mPacketMap = new HashMap<>(60);
	
	private int mSsrc;
	private boolean mIsCheckSsrc = false;

	/**
	 * 第一帧是否为I帧
	 * 不为I帧，直接丢弃
	 */
	private boolean mIsFirstI;

	private int CACHE_FRAME_LENGTH= 10;

	private byte[] preData;

	private Parser mParser;

	public UDPHandler(int mSsrc,boolean mIsCheckSsrc,Parser parser) {
		this.mSsrc = mSsrc;
		this.mIsCheckSsrc = mIsCheckSsrc;
		this.mParser = parser;
	}

	@Override
	protected void channelRead0(ChannelHandlerContext ctx, DatagramPacket msg) throws Exception {

		ByteBuf byteBuf =  msg.content();
		int readableBytes = byteBuf.readableBytes();
		if(readableBytes <=0){
			return;
		}
		byte[] copyData = new byte[readableBytes];
		byteBuf.readBytes(copyData);
		//log.info("UDP接受到帧数据>>> {}",HexStringUtils.toHexString(copyData));

		int seq = BitUtils.byte2ToInt(copyData[2],copyData[3]);
		int length = copyData.length;
		//检查是否有rtp头
		//有的终端会先发rtp头一个包，再发ps包
		//如果当前包没有 rtp头，写入前一个数据包
		if(length <= 12 && (copyData[0] & 0xff) == 0x80 && ((copyData[1] & 0xff) != 0x60 || (copyData[1] & 0xff) != 0xe0 )){
			preData = copyData;
			return ;
		}
		if((copyData[0] & 0xff) != 0x80 && ((copyData[1] & 0xff) != 0x60 || (copyData[1] & 0xff) != 0xe0 || (copyData[1] & 0xff) != 0x88 )){
			int newLength = preData.length + length;
			byte[] buffer =new byte[newLength];
			System.arraycopy(preData, 0, buffer, 0,  preData.length);
			System.arraycopy(copyData, 0, buffer, preData.length, length);
			copyData = buffer;
			length = newLength;
			preData = null;
		}
		if(mIsCheckSsrc){
			int uploadSsrc = BitUtils.byte4ToInt(copyData[8],copyData[9],copyData[10],copyData[11]);
			if(uploadSsrc != mSsrc){
				return;
			}
		}
		try{
			Packet packet;
			if(length > 16 && copyData[12] == 0 &&copyData[13] ==0 &&copyData[14] ==01 && (copyData[15]&0xff) == 0xba){
				int stuffingLength =  copyData[25] & 7;
				int startIndex = 25+stuffingLength+1;
				//i帧
				if(copyData[startIndex] == 0 && copyData[startIndex+1] == 0&&copyData[startIndex+2] == 01&&(copyData[startIndex+3]&0xff) == 0xbb )
				{
					packet = new Packet(seq,copyData,Packet.I);
					if(!mIsFirstI){
						mIsFirstI = true;
					}
				}
				//p帧
				else{
					if(!mIsFirstI){
						return;
					}
					packet = new Packet(seq,copyData,Packet.P);
				}
				mSeqMap.add(seq);
			}
			//音频数据
			else if( length > 16 &&  copyData[12] == 0 &&copyData[13] ==0 &&copyData[14] ==01 && (copyData[15]&0xff) == 0xc0){
				if(!mIsFirstI){
					return;
				}
				mSeqMap.add(seq);
				packet = new Packet(seq,copyData,Packet.AUDIO);
			}else {
				if(!mIsFirstI){
					return ;
				}
				packet = new Packet(seq,copyData,Packet.SUB_PACKET);
			}
			mPacketMap.put(seq, packet);
			if(mSeqMap.size() >= CACHE_FRAME_LENGTH){
				Integer firstSeq = mSeqMap.pop();
				Integer endSeq = mSeqMap.getFirst()-1;
				mParser.parseUdp(mPacketMap,firstSeq,endSeq);
			}
		}catch (Exception e){
			e.printStackTrace();
			log.error("UDPHandler 异常 >>> {}",HexStringUtils.toHexString(copyData));
		}finally {
			//release(msg);
		}
	}
}
