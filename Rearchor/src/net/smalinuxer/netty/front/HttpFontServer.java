package net.smalinuxer.netty.front;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpRequestDecoder;
import io.netty.handler.codec.http.HttpResponseEncoder;
import io.netty.handler.stream.ChunkedWriteHandler;
import net.smalinuxer.lucene.frame.RetinReaderFinder;

public class HttpFontServer implements Runnable {

	private Thread thread;
	
	private RetinReaderFinder retinReaderFinder = null;
	
	private int port;
	
	public HttpFontServer(RetinReaderFinder finder, int port) {
		this.retinReaderFinder = finder;
		this.port = port;
		thread = new Thread(this);
		thread.start();
	}
	
	public HttpFontServer(int port){
		this(RetinReaderFinder.newInstance(),port);
	} 
	
	@Override
	public void run() {
		EventLoopGroup bossGroup = null;
		EventLoopGroup workerGroup = null;
		try {
			bossGroup = new NioEventLoopGroup();
			workerGroup = new NioEventLoopGroup();
			ServerBootstrap b = new ServerBootstrap();
			b.group(bossGroup,workerGroup)
				.channel(NioServerSocketChannel.class)
				.option(ChannelOption.SO_BACKLOG, 1024)
				.childHandler(new ChannelInitializer<SocketChannel>() {

					@Override
					protected void initChannel(SocketChannel ch) throws Exception {
						ch.pipeline().addLast("http-decoder",new HttpRequestDecoder());
						ch.pipeline().addLast("http-aggregator",new HttpObjectAggregator(65536));
						ch.pipeline().addLast("http-encoder",new HttpResponseEncoder());
						ch.pipeline().addLast("http-chunked",new ChunkedWriteHandler());
						ch.pipeline().addLast("auth-chrcked",new AuthCherkHandler());
						ch.pipeline().addLast("fileServerHandler",new HttpFrontServerHandler(retinReaderFinder));
					}
					
				});
			ChannelFuture future = b.bind(port).sync();
			future.channel().closeFuture().sync();
			System.out.println("2");
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		} finally{
			if(workerGroup != null){
				workerGroup.shutdownGracefully();
			}
			if(bossGroup != null){
				bossGroup.shutdownGracefully();
			}
		}
	}
	
	/**
	 * 可能有点暴力
	 */
	private void stop() {
		thread.interrupt();
		thread = null;
	}
	
	public static void main(String[] args) {
		HttpFontServer server = new HttpFontServer(8089);
		System.out.println();
		try {
			Thread.sleep(50000);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		server.stop();
	}
	
}
