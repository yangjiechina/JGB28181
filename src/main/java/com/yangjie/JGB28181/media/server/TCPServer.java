package com.yangjie.JGB28181.media.server;

import java.util.concurrent.ConcurrentLinkedDeque;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.yangjie.JGB28181.media.callback.OnChannelStatusListener;
import com.yangjie.JGB28181.media.codec.Frame;
import com.yangjie.JGB28181.media.server.handler.TCPHandler;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import io.netty.util.concurrent.Future;

public class TCPServer extends Server implements OnChannelStatusListener{

	private Logger log = LoggerFactory.getLogger(getClass());

	private final String TAG = this.getClass().getSimpleName();

	private EventLoopGroup bossGroup = null;
	private EventLoopGroup workerGroup = null;

	private String callId;

	public void bind(ConcurrentLinkedDeque<Frame> frameDeque,int ssrc,int port,boolean checkSsrc) throws Exception {
		EventLoopGroup group = new NioEventLoopGroup();
		ServerBootstrap serverBootstrap = new ServerBootstrap();
		serverBootstrap.group(group)//
		.channel(NioServerSocketChannel.class) //
		.childHandler(new ChannelInitializer<SocketChannel>() { //
			@Override
			public void initChannel(SocketChannel ch) throws Exception {
				//解决TCP粘包问题
				ch.pipeline().addLast(new LengthFieldBasedFrameDecoder(1024*1024,0,2));
				TCPHandler tcpHandler = new TCPHandler(frameDeque,ssrc,checkSsrc,TCPServer.this);
				tcpHandler.setOnChannelStatusListener(TCPServer.this);
				ch.pipeline().addLast(tcpHandler);
			}
		});
		log.info("TCPRealTimeMediaStreamServer服务启动完毕,port={}", port);
		ChannelFuture channelFuture = serverBootstrap.bind(port).sync();
		channelFuture.channel().closeFuture().sync();
	}

	public  void startServer(ConcurrentLinkedDeque<Frame> frameDeque,int ssrc,int port,boolean checkSsrc) {
		new Thread(() -> {
			try {
				bind(frameDeque,ssrc,port,checkSsrc);
			} catch (Exception e) {
				this.log.info("{}服务启动出错:{}", TAG,e.getMessage());
				e.printStackTrace();
			}
		}, this.TAG).start();
	}

	public  void stopServer() {

		try {
			Future<?> future = this.workerGroup .shutdownGracefully().await();
			if (!future.isSuccess()) {
				log.error("workerGroup 无法正常停止:{}", future.cause());
			}

			future = this.bossGroup.shutdownGracefully().await();
			if (!future.isSuccess()) {
				log.error("bossGroup 无法正常停止:{}", future.cause());
			}
		} catch (InterruptedException e) {
			e.printStackTrace();
		}

		this.log.info("{}服务已经停止...",TAG);
	}

	@Override
	public void onConnect() {
		
	}

	@Override
	public void onDisconnect() {
		if(onProcessListener != null){
			onProcessListener.onError(callId);
		}
	}
}
