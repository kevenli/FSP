package flowy.scheduler.server;

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
	public void Run() throws SchedulerException {
		ServerSocket serverSocket = null;
		Scheduler scheduler = StdSchedulerFactory.getDefaultScheduler();
		
		scheduler.start();
		try {    
			serverSocket = new ServerSocket(DEFAULT_PORT);
			System.out.format("Start listening %s\r\n", DEFAULT_PORT);
			logger.info(String.format("Server started on %s", DEFAULT_PORT));
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		while (true){
			Socket socket;
			try {
				socket = serverSocket.accept();
				logger.info(String.format("Accepting new connection from %s", socket.getRemoteSocketAddress()));
				String version = this.acceptSocket(socket);
				Session session = new Session(scheduler, socket);
				new Thread(session).start();  
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}//阻塞等待消息
			
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
