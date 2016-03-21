package flowy.scheduler.server;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import flowy.scheduler.server.data.DaoFactory;
import flowy.scheduler.server.rest.RestServerInitializer;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;

import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.impl.StdSchedulerFactory;

public class FSPServer {

	public static final int DEFAULT_PORT = 3092;

	private static Logger logger = Logger.getLogger(FSPServer.class);

	private Properties config;

	public FSPServer() throws Exception {
		config = loadConfiguration();

		initDatabase();

	}

	private Properties loadConfiguration() throws IOException {
		Properties prop = new Properties();
		InputStream in = new FileInputStream("../conf/fspserver.conf");
		try {
			prop.load(in);
			return prop;
		} finally {
			in.close();
		}

	}

	private void initDatabase() throws Exception {
		String mysqlHost = config.getProperty("mysql.host");
		String mysqlUsername = config.getProperty("mysql.username");
		String mysqlPassword = config.getProperty("mysql.password");
		String mysqlDatabase = config.getProperty("mysql.database");
		int mysqlPort = Integer.parseInt(config.getProperty("mysql.port",
				"3306"));
		DaoFactory.init(mysqlHost, mysqlUsername, mysqlPassword, mysqlDatabase,
				mysqlPort);
	}

	public void Run() throws SchedulerException, InterruptedException {
		// init global scheduler
		Scheduler scheduler = StdSchedulerFactory.getDefaultScheduler();
		scheduler.start();

		// thread pools
		EventLoopGroup bossGroup = new NioEventLoopGroup();
		EventLoopGroup workerGroup = new NioEventLoopGroup();

		EventLoopGroup wiBossGroup = new NioEventLoopGroup(1);
		EventLoopGroup wiWorkerGroup = new NioEventLoopGroup();
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

			ServerBootstrap wib = new ServerBootstrap();
			wib.group(wiBossGroup, wiWorkerGroup)
					.channel(NioServerSocketChannel.class)
					.handler(new LoggingHandler(LogLevel.INFO))
					.childHandler(new RestServerInitializer(null));

			Channel ch = wib.bind(8080).sync().channel();

			// Wait until the server socket is closed.
			// In this example, this does not happen, but you can do that to
			// gracefully
			// shut down your server.
			f.channel().closeFuture().sync();
		} finally {
			workerGroup.shutdownGracefully();
			bossGroup.shutdownGracefully();
			
			wiBossGroup.shutdownGracefully();
			wiWorkerGroup.shutdownGracefully();
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
