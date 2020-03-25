package com.yangjie.JGB28181.media.server;

import java.util.concurrent.ConcurrentLinkedDeque;

import com.yangjie.JGB28181.media.codec.Frame;
import com.yangjie.JGB28181.media.callback.OnProcessListener;
import com.yangjie.JGB28181.media.codec.CommonParser;
import com.yangjie.JGB28181.media.server.remux.Observer;

public  abstract class Server extends CommonParser implements Observable{

	protected Observer observer;
	
	public abstract  void startServer(ConcurrentLinkedDeque<Frame> frameDeque,int ssrc,int port,boolean checkSsrc);
	public abstract  void stopServer();
	
	public OnProcessListener onProcessListener = null;

	@Override
	public void subscribe(Observer observer) {
		this.observer = observer;
	}
	
	@Override
	protected void onMediaStreamCallBack(byte[] data,int offset,int length,boolean isAudio){
		if(observer != null){
			observer.onMediaStream(data, offset, length,isAudio);
		}
	}
	
	@Override
	protected void onPtsCallBack(long pts,boolean isAudio){
		if(observer != null){
			observer.onPts(pts,isAudio);
		}
	}
	public void setOnProcessListener(OnProcessListener onProcessListener){
		this.onProcessListener = onProcessListener;
	}
}
