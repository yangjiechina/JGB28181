package com.yangjie.JGB28181.media.server.remux;

import java.io.IOException;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.util.concurrent.ConcurrentLinkedDeque;

import org.bytedeco.javacpp.avcodec;
import org.bytedeco.javacpp.avcodec.AVPacket;
import org.bytedeco.javacv.FFmpegFrameGrabber;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * rtmp 推流器
 * @author yangjie
 * 2020年3月23日
 */
public class RtmpPusher extends Observer{

	private Logger log = LoggerFactory.getLogger(getClass());

	private PipedInputStream pis;

	private PipedOutputStream pos = new PipedOutputStream();;

	private boolean mRunning = true;

	private String address;

	private String callId;

	private long mLastPts;

	private boolean mIs90000TimeBase = false;


	private ConcurrentLinkedDeque<Long> mPtsQueue = new ConcurrentLinkedDeque<>();

	public RtmpPusher(String address,String callId){
		this.address = address;
		this.callId = callId;
	}
	@Override
	public void onMediaStream(byte[] data, int offset,int length,boolean isAudio){
		try {
			if(!isAudio){
				pos.write(data,offset,length);
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * 有的设备 timebase 90000，有的直接 1000
	 */
	@Override
	public void onPts(long pts,boolean isAudio) {
		if(isAudio){
			return;
		}
		//分辨timebase是 90000 还是 1000
		//如果是90000 pts/90
		if(mLastPts == 0 && pts != 0){
			mIs90000TimeBase = (pts >= 3000);
		}
		if(mIs90000TimeBase){
			pts = pts / 90;
		}
		//如果当前pts小于之前的pts
		//推流会崩溃
		//av_write_frame() error -22 while writing video packet.
		if(mLastPts != 0 && pts < mLastPts){
			pts = mLastPts + 40;
		}
		mPtsQueue.add(pts);
		mLastPts = pts;
		//log.info("pts >>> {}",pts);
	}
	@Override
	public void run() {
		FFmpegFrameGrabber grabber = null;
		CustomFFmpegFrameRecorder recorder = null;
		Long pts  = 0L;
		try{
			//pis = new PipedInputStream(pos,1024*1024);
			pis = new PipedInputStream(pos);
			grabber = new FFmpegFrameGrabber(pis);
			//阻塞式，直到通道有数据
			grabber.start();
			recorder = new CustomFFmpegFrameRecorder(address,1280,720,0);
			recorder.setInterleaved(true);
			recorder.setVideoCodec(avcodec.AV_CODEC_ID_H264);
			recorder.setFormat("flv"); 
			recorder.setFrameRate(25);
			recorder.start(grabber.getFormatContext());
			AVPacket avPacket;


			while(mRunning && (avPacket=grabber.grabPacket()) != null && avPacket.size() >0 && avPacket.data() != null){
				pts = mPtsQueue.pop();
				//pts+=40;
				recorder.recordPacket(avPacket,pts,pts);
			}

		}catch(Exception e){
			e.printStackTrace();
			log.error("推流发生异常 >>> {} pts== {}" ,e,pts);
			if(onProcessListener != null){
				onProcessListener.onError(callId);
			}
		}finally{
			try{
				if(recorder != null){
					recorder.close();
				}
				if(grabber != null){
					grabber.close();
				}
			}catch(Exception e){
				log.info(e.getMessage());
				e.printStackTrace();
			}
		}
		log.error("推流结束");
	}

	@Override
	public void stopRemux() {
		this.mRunning = false;
	}
	@Override
	public void startRemux() {
		this.start();
	}

}
