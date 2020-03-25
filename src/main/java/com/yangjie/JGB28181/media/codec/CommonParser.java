package com.yangjie.JGB28181.media.codec;

import java.io.IOException;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.yangjie.JGB28181.common.utils.BitUtils;

/**
 * 通用解析器
 * @author yangjie
 * 2020年3月23日
 */
public  class CommonParser implements Parser{
	private Logger log = LoggerFactory.getLogger(getClass());
	/**
	 * 解析UDPps
	 * @param packetMap
	 * @param firstSeq
	 * @param endSeq
	 * @throws Exception
	 */
	@Override
	public void parseUdp(Map<Integer,Packet> packetMap,int firstSeq,int endSeq) throws Exception{
		int remainEsLength = 0;
		int startIndex = 0;
		for(int i = firstSeq; i<= endSeq;i++){
			Packet packet = packetMap.remove(i);
			if(packet == null){
				continue;
			}
			byte[] data = packet.getData();
			int packetType = packet.getPacketType();
			boolean hasSubPacket = true;
			int pesStartIndex = 0;
			if(packetType == Packet.I){
				pesStartIndex = getIFramePesStartIndex(data, UDP_PS_HEADER_STUFFING_LENGTH_INDEX);
				hasSubPacket = false;
			}else if(packetType == Packet.P){
				pesStartIndex = getPFramePesStartIndex(data,UDP_PS_HEADER_STUFFING_LENGTH_INDEX);
				hasSubPacket = false;
			}else if(packetType == Packet.AUDIO){
				pesStartIndex = UDP_START_INDEX; 
				hasSubPacket = false;
			}
			boolean isAudio = (packetType == Packet.AUDIO);

			//统一计算 startIndex
			if(!hasSubPacket){
				long pts = getPts(data, pesStartIndex);
				onPtsCallBack(pts,isAudio);
				//pes长度
				int pesDataLength = BitUtils.byte2ToInt(data[pesStartIndex+4],data[pesStartIndex+5]);
				//pes头长度  
				int pesHeaderDataLength = data[pesStartIndex+8] & 0xFF;

				//es起始索引
				startIndex = pesStartIndex+6+3+pesHeaderDataLength;
				remainEsLength = pesDataLength-3-pesHeaderDataLength;

			}else {
				startIndex = UDP_START_INDEX;
			}

			int packetLength= data.length; 
			int dataLength  = packetLength - startIndex;
			//如果小于,整个包都是es数据
			if(dataLength <= remainEsLength){
				remainEsLength -= dataLength;
				onMediaStreamCallBack(data, startIndex, dataLength,isAudio);
				startIndex = 0;
				continue;
			}
			//大于则说明数据里面还包含es数据
			//先将上一个es包的数据写入通道
			//再解析下一个pes
			onMediaStreamCallBack(data, startIndex, remainEsLength,isAudio);
			startIndex+=remainEsLength;
			remainEsLength = 0;
			while(true){
				//新的pes数据总长
				int newPesDataLength = BitUtils.byte2ToInt(data[startIndex + 4],data[startIndex+5]);
				//新的pes头长度
				int pesHeaderDataLength = data[startIndex+8] & 0xFF;
				//es长度
				remainEsLength = newPesDataLength - 3 - pesHeaderDataLength;

				startIndex+=8+pesHeaderDataLength+1;
				//大于等于说明剩下的包不包含es,跳出循环
				if(startIndex >= packetLength){
					break;
				}
				//当前包剩余长度
				int packetRemainLength =  packetLength - startIndex;
				//小于等于，说明剩下的包全都是es的数据
				//写入通道，跳出循环
				if(packetRemainLength <= remainEsLength){
					onMediaStreamCallBack(data, startIndex, packetRemainLength,isAudio);
					remainEsLength-= packetRemainLength;
					break;
				}
				//大于es长度，说明还有  pes
				//写入通道，继续循环
				onMediaStreamCallBack(data, startIndex, remainEsLength,isAudio);
				startIndex+=remainEsLength;
			}
			startIndex = 0;
		}
	}
	/**
	 * 解析 tcp ps
	 * @param frame
	 * @throws IOException
	 */
	@Override
	public void parseTcp(Frame frame) throws Exception{
		Map<Integer, byte[]> packetMap = frame.getPacketMap();
		int firstSeq = frame.getFirstSeq();
		int endSeq = frame.getEndSeq();
		byte[] data = packetMap.get(firstSeq);
		int frameType = frame.getFrameType();

		int remainEsLength = 0;
		int startIndex = 0;
		int pesStartIndex  = 0;
		//1.解析跳过 rtp ps ps_system ps_map 
		if(frameType == Frame.I){
			pesStartIndex = getIFramePesStartIndex(data, TCP_PS_HEADER_STUFFING_LENGTH_INDEX);
		}else if(frameType == Frame.P){
			pesStartIndex = getPFramePesStartIndex(data, TCP_PS_HEADER_STUFFING_LENGTH_INDEX);
		}else if(frameType == Frame.AUDIO){
			pesStartIndex = TCP_START_INDEX;
		}
		else {
			packetMap.clear();
			return;
		}
		boolean isAudio = frameType == Frame.AUDIO;
		long pts = getPts(data, pesStartIndex);
		onPtsCallBack(pts,isAudio);

		//pes长度
		int pesDataLength = BitUtils.byte2ToInt(data[pesStartIndex+4],data[pesStartIndex+5]);

		//pes头长度  
		int pesHeaderDataLength = data[pesStartIndex+8] & 0xFF;

		//h264起始索引
		startIndex = pesStartIndex+6+3+pesHeaderDataLength;
		remainEsLength = pesDataLength-3-pesHeaderDataLength;

		for(int i =firstSeq; i <= endSeq;i++){
			byte[] packet = packetMap.get(i);
			int packetLength= packet.length; 
			if(startIndex == 0){
				startIndex = TCP_START_INDEX;
			}
			int dataLength  = packetLength - startIndex;
			//如果小于,整个包都是es数据
			if(dataLength <= remainEsLength){
				remainEsLength -= dataLength;
				onMediaStreamCallBack(packet, startIndex, dataLength,isAudio);
				startIndex = 0;
				continue;
			}
			//大于则说明数据里面还包含es数据
			//先将上一个es包的数据写入通道
			//再解析下一个pes
			onMediaStreamCallBack(packet, startIndex, remainEsLength,isAudio);
			startIndex+=remainEsLength;
			remainEsLength = 0;
			while(true){
				//新的pes数据总长
				int newPesDataLength = BitUtils.byte2ToInt(packet[startIndex + 4],packet[startIndex+5]);
				//新的pes头长度
				int newPesHeaderDataLength = packet[startIndex+8] & 0xFF;
				//es长度
				remainEsLength = newPesDataLength - 3 - newPesHeaderDataLength;

				startIndex+=8+newPesHeaderDataLength+1;
				//大于等于说明剩下的包不包含es,跳出循环
				if(startIndex >= packetLength){
					break;
				}
				//当前包剩余长度
				int packetRemainLength =  packetLength - startIndex;
				//小于等于，说明剩下的包全都是es的数据
				//写入通道，跳出循环
				if(packetRemainLength <= remainEsLength){
					onMediaStreamCallBack(packet, startIndex, packetRemainLength,isAudio);
					remainEsLength-= packetRemainLength;
					break;
				}
				//大于es长度，说明还有  pes
				//写入通道，继续循环
				onMediaStreamCallBack(packet, startIndex, remainEsLength,isAudio);
				startIndex+=remainEsLength;
			}
			startIndex = 0;
		}
	}

	/**
	 * 计算获取i帧的pes start index
	 * @param data
	 * @param stuffingLengthIndex
	 * @return
	 */
	private int getIFramePesStartIndex(byte[] data,int stuffingLengthIndex){
		//ps头中STUFFING长度，扩充扩展字段，直接跳过。
		int stuffingLength =  data[stuffingLengthIndex] & 7;
		//psSystem头开始索引
		int psSystemHeaderStartIndex =  stuffingLengthIndex+stuffingLength+1;

		//psSystem头长度,00 00 00 bb
		int psSystemHeaderLength = BitUtils.byte2ToInt(data[psSystemHeaderStartIndex + 4],data[psSystemHeaderStartIndex+5]);

		int psMapHeaderStartIndex = psSystemHeaderStartIndex + 6 +psSystemHeaderLength;

		//psMap头长度,00 00 00 bc
		int psMapHeaderLength = BitUtils.byte2ToInt(data[psMapHeaderStartIndex+4],data[psMapHeaderStartIndex+5]);

		//pes开始索引，00 00 01 e0
		int pesStartIndex = psMapHeaderStartIndex+6+psMapHeaderLength;
		return pesStartIndex;
	}
	/**
	 * 计算获取p帧的pes start index
	 * @param data
	 * @param stuffingLengthIndex
	 * @return
	 */
	private int getPFramePesStartIndex(byte[] data,int stuffingLengthIndex){
		//ps头中STUFFING长度直接跳过，扩充扩展字段。
		int stuffingLength =  data[stuffingLengthIndex] & 7;
		return stuffingLengthIndex+stuffingLength+1;
	}
	/**
	 * 计算获取 pts
	 * @param data 源数据
	 * @param pesStartIndex pes头起始索引
	 * @return
	 */
	private long getPts(byte[] data,int pesStartIndex){
		if(data != null && data.length >= pesStartIndex){
			try{
				//检查pes头中是否包含pts、dts
				//i帧中的pes头至少需要携带pts
				//pts_dts flags
				byte ptsDtsFlags =  (byte) ((data[pesStartIndex+7]&0xff) >> 6 & 0x3);

				//`11` pts、dts都有
				//`10` pts
				//00 都没有
				if(ptsDtsFlags == 0x3 || ptsDtsFlags==0x2){

					//pts[32-30]
					int bitHight32_30 = data[pesStartIndex+10]&0xff & 0xE;
					//pts[29-15]
					int bitHight29_15 = BitUtils.byte2ToInt(data[pesStartIndex+10],data[pesStartIndex+11])&0xFFFE;
					//pts[14-0]					 
					int bitHight14_0 = BitUtils.byte2ToInt(data[pesStartIndex+12],data[pesStartIndex+13])&0xFFFE;

					long pts = (long)(bitHight32_30 << 29) + (long)(bitHight29_15<<14) + (long)(bitHight14_0>>1);
					if(pts < 0){
						//System.out.println(HexStringUtils.toHexString(data));
					}
					//log.info("解析后的pts >>> {}",pts);
					return pts;
				}
			}catch(Exception e){
				e.printStackTrace();
			}
		}
		return -1;
	}
	protected void onMediaStreamCallBack(byte[] data,int offset,int length,boolean isAudio){

	}

	protected void onPtsCallBack(long pts,boolean isAudio){

	}

}
