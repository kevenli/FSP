package flowy.scheduler.server.rest.actions;

import java.util.List;

import org.apache.log4j.Logger;

import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.DefaultFullHttpRequest;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.multipart.DefaultHttpDataFactory;
import io.netty.handler.codec.http.multipart.HttpPostRequestDecoder;
import io.netty.handler.codec.http.multipart.InterfaceHttpData;
import flowy.scheduler.server.FSPServer;
import flowy.scheduler.server.rest.IAction;

public class TaskTypeInstanceAction implements IAction {

	private static Logger logger = Logger.getLogger(TaskTypeInstanceAction.class);
	@Override
	public String processRequest(HttpRequest request, String content,
			ChannelHandlerContext ctx) {
		if(request.method() == HttpMethod.POST)
		{
			return this.saveObj(request, content, ctx);
			
		}
		else if (request.method() == HttpMethod.GET){
			return this.getObj(request, content, ctx);
		}
		return null;
	}
	
	private String saveObj(HttpRequest request, String content,
			ChannelHandlerContext ctx)
	{
		
		DefaultFullHttpRequest postRequest = new DefaultFullHttpRequest(request.protocolVersion(), 
				request.method(), request.uri(), Unpooled.copiedBuffer(content.getBytes()));
		logger.debug(content);
		//postRequest.
		HttpPostRequestDecoder decoder = new HttpPostRequestDecoder(new DefaultHttpDataFactory(false), postRequest);
		//decoder.offer(new HttpContent())
		List<InterfaceHttpData> postList = decoder.getBodyHttpDatas();
		logger.debug(decoder.getBodyHttpData("name"));
		return null;
		
	}
	
	private String getObj(HttpRequest request, String content,
			ChannelHandlerContext ctx)
	{
		return null;
		
	}

}
