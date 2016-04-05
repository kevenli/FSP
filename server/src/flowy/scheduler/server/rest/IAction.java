package flowy.scheduler.server.rest;

import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.HttpContent;
import io.netty.handler.codec.http.HttpRequest;

public interface IAction {

	String processRequest(HttpRequest request, String content, ChannelHandlerContext ctx);
}
