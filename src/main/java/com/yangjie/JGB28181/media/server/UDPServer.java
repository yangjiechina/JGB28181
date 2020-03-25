package com.yangjie.JGB28181.media.server;

import java.util.concurrent.ConcurrentLinkedDeque;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.yangjie.JGB28181.media.codec.Frame;
import com.yangjie.JGB28181.media.server.handler.UDPHandler;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioDatagramChannel;
import io.netty.util.concurrent.Future;

/*
 * UDP 由于在外网环境下(特别是4G)，乱序 丢包情况较为严重
 * 所以处理方式和TCP有所区别，不在Handler中直接判断是I/P/分包数据/
 * 先将每个包按照rtp头中的seq来缓存，再做组包、解析、推流
 */
public class UDPServer extends Server{

	private String TAG = this.getClass().getSimpleName();
	private Logger log = LoggerFactory.getLogger(getClass());
	private volatile boolean isRunning = false;
	private Bootstrap bootstrap = null;
	private EventLoopGroup workerGroup = null;

	private void bind(int port,int ssrc,boolean checkSsrc) throws Exception {
		workerGroup= new NioEventLoopGroup();
		try {
			bootstrap = new Bootstrap();
			bootstrap.group(workerGroup)//
			.channel(NioDatagramChannel.class) //
			.option(ChannelOption.SO_RCVBUF,1024*1024)
			.handler(new ChannelInitializer<NioDatagramChannel>() { //
				@Override
				public void initChannel(NioDatagramChannel ch) throws Exception {
					ch.pipeline().addLast(new UDPHandler(ssrc,checkSsrc,UDPServer.this));
				}
			});
			this.log.info("UDP服务启动成功port:{}", port);
			bootstrap.bind(port).sync().channel().closeFuture().sync();

		} catch (InterruptedException e) {
			e.printStackTrace();
		} finally {
			workerGroup.shutdownGracefully();
		}
	}
	@Override
	public  void startServer(ConcurrentLinkedDeque<Frame> frameDeque,int ssrc,int port,boolean checkSsrc) {
		if (this.isRunning) {
			throw new IllegalStateException(TAG+ " is already started .");
		}
		this.isRunning = true;

		new Thread(() -> {
			try {
				this.bind(port,ssrc,checkSsrc);
			} catch (Exception e) {
				this.log.info("{}服务启动出错:{}", TAG,e.getMessage());
				e.printStackTrace();
			}
		}, TAG).start();
	}
	@Override
	public  void stopServer() {
		if (!this.isRunning) {
			throw new IllegalStateException(TAG + " is not yet started .");
		}
		this.isRunning = false;
		try {
			Future<?> future = this.workerGroup.shutdownGracefully().await();
			if (!future.isSuccess()) {
				log.error("workerGroup 无法正常停止:{}", future.cause());
			}
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		this.log.info("UDPServer服务已经停止...");
	}
}
