package flowy.scheduler.server;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.Charset;

import org.apache.log4j.Logger;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.impl.StdSchedulerFactory;

public class FSPServer {
	
	public static final int DEFAULT_PORT = 3092;
	
	private Logger logger = Logger.getLogger(FSPServer.class);
	
	public FSPServer() throws Exception{
		if (!DaoFactory.testDatabaseConnection()){
			throw new Exception("Cannot connect to database");
		}
	}
	
	@SuppressWarnings("resource")
	public void Run() throws SchedulerException, InterruptedException {
		ServerSocket serverSocket = null;
		Scheduler scheduler = StdSchedulerFactory.getDefaultScheduler();
		
		scheduler.start();
		EventLoopGroup bossGroup = new NioEventLoopGroup(); // (1)
        EventLoopGroup workerGroup = new NioEventLoopGroup();
        try {
            ServerBootstrap b = new ServerBootstrap(); // (2)
            b.group(bossGroup, workerGroup)
             .channel(NioServerSocketChannel.class) // (3)
             .childHandler(new ChannelInitializer<SocketChannel>() { // (4)
                 @Override
                 public void initChannel(SocketChannel ch) throws Exception {
                     ch.pipeline().addLast(new FSPServerHandler());
                 }
             })
             .option(ChannelOption.SO_BACKLOG, 128)          // (5)
             .childOption(ChannelOption.SO_KEEPALIVE, true); // (6)

            // Bind and start to accept incoming connections.
            ChannelFuture f = b.bind(DEFAULT_PORT).sync(); // (7)

            // Wait until the server socket is closed.
            // In this example, this does not happen, but you can do that to gracefully
            // shut down your server.
            f.channel().closeFuture().sync();
        } finally {
            workerGroup.shutdownGracefully();
            bossGroup.shutdownGracefully();
        }
	}
	
	// 开始接受链接时先检查客户端版本号
	private String acceptSocket(Socket socket) throws IOException{
		byte[] readBuffer = new byte[1];
		int readIndex = 0;
		int readLength = 1;
		
		StringBuilder versionReader = new StringBuilder();
		
		// 变长协议字符串，以0x00结尾   ： FSP_0_0_1
		do {
			socket.getInputStream().read(readBuffer, readIndex, readLength);
			versionReader.append(new String(readBuffer, Charset.forName("Ascii")));
		}
		while(readBuffer[0] != 0x00);
		
		String versionStr = versionReader.toString();
		
		
		
		System.out.format("Client connected, %s %s\r\n", socket.getInetAddress().getHostAddress(), versionStr);
		
		return versionStr;
	}

	public static void main(String[] args) throws Exception {
		FSPServer server = new FSPServer();
		server.Run();
	}

}
