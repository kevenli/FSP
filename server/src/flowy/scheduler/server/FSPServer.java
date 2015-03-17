package flowy.scheduler.server;

import flowy.scheduler.server.data.DaoFactory;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;

import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.impl.StdSchedulerFactory;

public class FSPServer {
	
	public static final int DEFAULT_PORT = 3092;
	
	private static Logger logger = Logger.getLogger(FSPServer.class);
	
	public FSPServer() throws Exception{
		if (!DaoFactory.testDatabaseConnection()){
			throw new Exception("Cannot connect to database");
		}
	}
	
	public void Run() throws SchedulerException, InterruptedException {
		// init global scheduler
		Scheduler scheduler = StdSchedulerFactory.getDefaultScheduler();
		scheduler.start();
		
		// thread pools
		EventLoopGroup bossGroup = new NioEventLoopGroup();
        EventLoopGroup workerGroup = new NioEventLoopGroup();
        
        try {
            ServerBootstrap b = new ServerBootstrap();
            b.group(bossGroup, workerGroup)
             .channel(NioServerSocketChannel.class)
             .childHandler(new SessionHandlerInitializer())
             .option(ChannelOption.SO_BACKLOG, 128)
             .childOption(ChannelOption.SO_KEEPALIVE, true);

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
		PropertyConfigurator.configure("../conf/log4j.properties");
		
		// start server
		FSPServer server = new FSPServer();
		server.Run();
	}

}
