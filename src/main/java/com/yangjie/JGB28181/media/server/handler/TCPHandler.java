package com.yangjie.JGB28181.media.server.handler;

import java.util.concurrent.ConcurrentLinkedDeque;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.yangjie.JGB28181.common.utils.BitUtils;
import com.yangjie.JGB28181.common.utils.HexStringUtils;
import com.yangjie.JGB28181.media.callback.OnChannelStatusListener;
import com.yangjie.JGB28181.media.codec.Frame;
import com.yangjie.JGB28181.media.codec.Parser;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.util.ReferenceCountUtil;

/**
 * 接受TCP音视频媒体流
 * @author yangjie
 * 2020年3月13日
 */
public class TCPHandler  extends ChannelInboundHandlerAdapter{

	private Logger log = LoggerFactory.getLogger(getClass());

	private ConcurrentLinkedDeque<Frame> mFrameDeque = null;

	private boolean mIsCheckSsrc = false;

	private int mSsrc;

	/**
	 * 第一帧是否为I帧
	 * 不为I帧，先丢弃
	 */
	private boolean mIsFirstI;

	private OnChannelStatusListener onChannelStatusListener;

	private Parser mParser;


	public void setOnChannelStatusListener(OnChannelStatusListener onChannelStatusListener) {
		this.onChannelStatusListener = onChannelStatusListener;
	}
	public TCPHandler(ConcurrentLinkedDeque<Frame> frameDeque,int ssrc, boolean checkSsrc,
			Parser parser) {
		this.mFrameDeque =frameDeque;
		this.mSsrc = ssrc;
		this.mParser = parser;
	}
	@Override
	public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
		if(mFrameDeque == null){
			log.error("frame deque can not null");
			return;
		}
		//log.info("channelRead");

		ByteBuf byteBuf = (ByteBuf) msg;
		int readableBytes = byteBuf.readableBytes();
		if(readableBytes <=0){
			return;
		} 
		byte[] copyData = new byte[readableBytes];
		byteBuf.readBytes(copyData);
		
		//log.error("TCPHandler channelRead >>> {}",HexStringUtils.toHexString(copyData));
		try{
			if(mIsCheckSsrc){
				int uploadSsrc = BitUtils.byte4ToInt(copyData[10],copyData[11],copyData[12],copyData[13]);
				if(uploadSsrc != mSsrc){
					return;
				}
			}
			int seq = BitUtils.byte2ToInt(copyData[4],copyData[5]);
			Frame frame;
			//有ps头,判断是否是i帧或者p帧
			if(readableBytes > 18 && copyData[14] == 0 &&copyData[15] ==0 &&copyData[16] ==01 && (copyData[17]&0xff) == 0xba){
				//pack_stuffing_length
				int stuffingLength =  copyData[27] & 7;
				int startIndex = 27+stuffingLength+1;

				//有ps系统头为i帧
				if(copyData[startIndex] == 0 && copyData[startIndex+1] == 0&&copyData[startIndex+2] == 01&&(copyData[startIndex+3]&0xff) == 0xbb ){
					frame = new Frame(Frame.I);
					mIsFirstI = true;
				}
				//p帧
				else{
					if(!mIsFirstI){
						return;
					}
					frame = new Frame(Frame.P);
				}
				frame.addPacket(seq, copyData);
				frame.setFirstSeq(seq);
				mFrameDeque.add(frame);
			}
			//音频数据
			else if(readableBytes > 18 && copyData[14] == 0 &&copyData[15] ==0 &&copyData[16] ==01 && (copyData[17]&0xff) == 0xc0){
				if(!mIsFirstI){
					return;
				}
				frame = new Frame(Frame.AUDIO);
				frame.addPacket(seq, copyData);
				frame.setFirstSeq(seq);
				mFrameDeque.add(frame);
			}
			//分包数据
			else{
				if(mFrameDeque.size() >0 && mIsFirstI){
					frame = mFrameDeque.getLast();
					if(frame != null){
						frame .addPacket(seq, copyData);
						frame.setEndSeq(seq);
					}

					if(mFrameDeque.size() >1){
						Frame pop = mFrameDeque.pop();
						mParser.parseTcp(pop);
					}
				}
			}  
		}catch (Exception e){
			e.printStackTrace();
			log.error("TCPHandler 异常 >>> {}",HexStringUtils.toHexString(copyData));
		}finally {
			release(msg);
		}
	}
	private void release(Object msg){
		try{
			ReferenceCountUtil.release(msg);
		}catch(Exception e){
			e.printStackTrace();
		}
	}

	/**
	 * TCP建立连接
	 */
	@Override
	public void channelActive(ChannelHandlerContext ctx) throws Exception {
		super.channelActive(ctx);
		if(onChannelStatusListener!= null){
			onChannelStatusListener.onConnect();
		}
	}
	/**
	 * TCP连接断开
	 */
	@Override
	public void channelInactive(ChannelHandlerContext ctx) throws Exception {
		super.channelInactive(ctx);
		if(onChannelStatusListener!= null){
			onChannelStatusListener.onDisconnect();
		}
	}
}
