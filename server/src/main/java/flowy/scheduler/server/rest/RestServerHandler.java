package flowy.scheduler.server.rest;

import flowy.scheduler.server.Session;
import flowy.scheduler.server.rest.actions.GetTaskTypesAction;
import flowy.scheduler.server.rest.actions.TaskTypeInstanceAction;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.DecoderResult;
import io.netty.handler.codec.http.Cookie;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpContent;
import io.netty.handler.codec.http.HttpHeaderUtil;
import io.netty.handler.codec.http.HttpHeaderValues;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpObject;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.LastHttpContent;
import io.netty.handler.codec.http.QueryStringDecoder;
import io.netty.handler.codec.http.ServerCookieDecoder;
import io.netty.handler.codec.http.ServerCookieEncoder;
import io.netty.util.CharsetUtil;

import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static io.netty.handler.codec.http.HttpHeaderNames.*;
import static io.netty.handler.codec.http.HttpResponseStatus.*;
import static io.netty.handler.codec.http.HttpVersion.*;

public class RestServerHandler extends SimpleChannelInboundHandler<Object> {

	private HttpRequest request;
	/** Buffer that stores the response content */
	private final StringBuilder buf = new StringBuilder();
	private final StringBuilder requestContent = new StringBuilder();
	private Hashtable<String, IAction> routing = new Hashtable<String, IAction>();

	public RestServerHandler() {
		routing.put("^/tasktypes$", new GetTaskTypesAction());
		routing.put("^/tasktypes/\\d+$", new TaskTypeInstanceAction());
	}

	@Override
	public void channelReadComplete(ChannelHandlerContext ctx) {
		ctx.flush();
	}

	@Override
	protected void messageReceived(ChannelHandlerContext ctx, Object msg) {
		if (msg instanceof HttpRequest) {
			buf.setLength(0);
			requestContent.setLength(0);
			HttpRequest request = this.request = (HttpRequest) msg;

			if (HttpHeaderUtil.is100ContinueExpected(request)) {
				send100Continue(ctx);
			}


		}

		if (msg instanceof HttpContent) {
			HttpContent httpContent = (HttpContent) msg;
			requestContent.append(httpContent.content());
			ByteBuf content = httpContent.content();
			//if (content.isReadable()) {
				//buf.append("CONTENT: ");
				//buf.append(content.toString(CharsetUtil.UTF_8));
				//buf.append("\r\n");
			//	appendDecoderResult(buf, request);
			//}

			if (msg instanceof LastHttpContent) {
				for (String route_pattern : routing.keySet()) {
					if (Pattern.matches(route_pattern, request.uri())) {
						buf.append(routing.get(route_pattern).processRequest(
									request, requestContent.toString(), ctx));
					}
				}
				LastHttpContent trailer = (LastHttpContent) msg;
				if (!trailer.trailingHeaders().isEmpty()) {
					buf.append("\r\n");
					for (CharSequence name : trailer.trailingHeaders().names()) {
						for (CharSequence value : trailer.trailingHeaders()
								.getAll(name)) {
							buf.append("TRAILING HEADER: ");
							buf.append(name).append(" = ").append(value)
									.append("\r\n");
						}
					}
					buf.append("\r\n");
				}

				if (!writeResponse(trailer, ctx)) {
					// If keep-alive is off, close the connection once the
					// content is fully written.
					ctx.writeAndFlush(Unpooled.EMPTY_BUFFER).addListener(
							ChannelFutureListener.CLOSE);
				}
			}
		}
	}

	private static void appendDecoderResult(StringBuilder buf, HttpObject o) {
		DecoderResult result = o.decoderResult();
		if (result.isSuccess()) {
			return;
		}

		buf.append(".. WITH DECODER FAILURE: ");
		buf.append(result.cause());
		buf.append("\r\n");
	}
	
	private boolean writeResponse(HttpObject currentObj,
			ChannelHandlerContext ctx) {
		// Decide whether to close the connection or not.
		boolean keepAlive = HttpHeaderUtil.isKeepAlive(request);
		// Build the response object.
		FullHttpResponse response = new DefaultFullHttpResponse(HTTP_1_1,
				currentObj.decoderResult().isSuccess() ? OK : BAD_REQUEST,
				Unpooled.copiedBuffer(buf.toString(), CharsetUtil.UTF_8));

		response.headers().set(CONTENT_TYPE, "text/plain; charset=UTF-8");

		if (keepAlive) {
			// Add 'Content-Length' header only for a keep-alive connection.
			response.headers().setInt(CONTENT_LENGTH,
					response.content().readableBytes());
			// Add keep alive header as per:
			// -
			// http://www.w3.org/Protocols/HTTP/1.1/draft-ietf-http-v11-spec-01.html#Connection
			response.headers().set(CONNECTION, HttpHeaderValues.KEEP_ALIVE);
		}

		// Encode the cookie.
		String cookieString = request.headers().getAndConvert(COOKIE);
		if (cookieString != null) {
			Set<Cookie> cookies = ServerCookieDecoder.decode(cookieString);
			if (!cookies.isEmpty()) {
				// Reset the cookies if necessary.
				for (Cookie cookie : cookies) {
					response.headers().add(SET_COOKIE,
							ServerCookieEncoder.encode(cookie));
				}
			}
		} else {
			// Browser sent no cookie. Add some.
			response.headers().add(SET_COOKIE,
					ServerCookieEncoder.encode("key1", "value1"));
			response.headers().add(SET_COOKIE,
					ServerCookieEncoder.encode("key2", "value2"));
		}

		// Write the response.
		ctx.write(response);

		return keepAlive;
	}

	private static void send100Continue(ChannelHandlerContext ctx) {
		FullHttpResponse response = new DefaultFullHttpResponse(HTTP_1_1,
				CONTINUE);
		ctx.write(response);
	}

	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
		cause.printStackTrace();
		ctx.close();
	}
}