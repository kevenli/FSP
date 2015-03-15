package flowy.scheduler.server;

import flowy.scheduler.protocal.Messages;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.protobuf.ProtobufDecoder;
import io.netty.handler.codec.protobuf.ProtobufEncoder;
import io.netty.handler.codec.protobuf.ProtobufVarint32FrameDecoder;
import io.netty.handler.codec.protobuf.ProtobufVarint32LengthFieldPrepender;
import io.netty.handler.codec.string.StringEncoder;
import io.netty.handler.timeout.IdleStateHandler;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.Charset;

import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.impl.StdSchedulerFactory;

import com.google.protobuf.ExtensionRegistry;

public class FSPServer {
	
	public static final int DEFAULT_PORT = 3092;
	
	private static Logger logger = Logger.getLogger(FSPServer.class);
	
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
        final ExtensionRegistry registry = ExtensionRegistry.newInstance();
        Messages.registerAllExtensions(registry);
        try {
            ServerBootstrap b = new ServerBootstrap(); // (2)
            b.group(bossGroup, workerGroup)
             .channel(NioServerSocketChannel.class) // (3)
             .childHandler(new ChannelInitializer<SocketChannel>() { // (4)
                 @Override
                 public void initChannel(SocketChannel ch) throws Exception {
                	 ChannelPipeline pipeline = ch.pipeline();
                	 pipeline.addLast(
                			 new IdleStateHandler(10, 10, 0),
                			 new ProtobufVarint32LengthFieldPrepender(),
                             new ProtobufEncoder(), 
                			 new ProtobufVarint32FrameDecoder(),
                			 new ProtobufDecoder(Messages.Request.getDefaultInstance(), registry),
                			 new SessionHandler());
                 }
             })
             .option(ChannelOption.SO_BACKLOG, 128)          // (5)
             .childOption(ChannelOption.SO_KEEPALIVE, true); // (6)

            // Bind and start to accept incoming connections.
            ChannelFuture f = b.bind(DEFAULT_PORT).sync(); // (7)
            logger.debug("Server started");
            // Wait until the server socket is closed.
            // In this example, this does not happen, but you can do that to gracefully
            // shut down your server.
            f.channel().closeFuture().sync();
        } finally {
            workerGroup.shutdownGracefully();
            bossGroup.shutdownGracefully();
        }
	}

	public static void main(String[] args) throws Exception {
		// load log4j configuration 
		PropertyConfigurator.configure("conf/log4j.properties");
		
		// start server
		FSPServer server = new FSPServer();
		server.Run();
	}

}
