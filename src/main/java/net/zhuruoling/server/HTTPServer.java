package net.zhuruoling.server;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.*;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.util.CharsetUtil;
import net.zhuruoling.configuration.ConfigReader;
import net.zhuruoling.socket.ClientSocketHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import static io.netty.handler.codec.http.HttpUtil.is100ContinueExpected;

public class HTTPServer extends Thread{
    final private static Logger logger = LoggerFactory.getLogger("HTTPServer");
    public HTTPServer(){
        this.setName("HTTPServer#" + getId());
    }

    @Override
    public void run() {
        ServerBootstrap bootstrap = new ServerBootstrap();
        EventLoopGroup boss = new NioEventLoopGroup();
        EventLoopGroup work = new NioEventLoopGroup();
        bootstrap.group(boss,work)
                .handler(new LoggingHandler(LogLevel.INFO))
                .channel(NioServerSocketChannel.class)
                .childHandler(new HttpServerInitializer());

        try {
            ChannelFuture f = bootstrap.bind(new InetSocketAddress(Objects.requireNonNull(ConfigReader.read()).getHttpPort())).sync();
            f.channel().closeFuture().sync();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        logger.info("Started Uranium qLink socket service at "+ Objects.requireNonNull(ConfigReader.read()).getHttpPort());
    }

    class HttpServerInitializer extends ChannelInitializer<SocketChannel> {
        @Override
        protected void initChannel(SocketChannel channel) throws Exception {
            ChannelPipeline pipeline = channel.pipeline();
            pipeline.addLast(new HttpServerCodec());// http 编解码
            pipeline.addLast("httpAggregator",new HttpObjectAggregator(512*1024)); // http 消息聚合器                                                                     512*1024为接收的最大contentlength
            pipeline.addLast(new HttpRequestHandler());// 请求处理器

        }
    }

    public static class HttpRequestHandler extends SimpleChannelInboundHandler<FullHttpRequest> {

        @Override
        public void channelReadComplete(ChannelHandlerContext ctx) {
            ctx.flush();
        }

        @Override
        protected void channelRead0(ChannelHandlerContext ctx, FullHttpRequest req) throws Exception {
            //100 Continue
            if (is100ContinueExpected(req)) {
                ctx.write(new DefaultFullHttpResponse(
                        HttpVersion.HTTP_1_1,
                        HttpResponseStatus.CONTINUE));
            }
            // 获取请求的uri

            String uri = req.uri();
            Map<String,String> resMap = new HashMap<>();
            resMap.put("method",req.method().name());
            resMap.put("uri",uri);
            String msg = "<html><head><title>test</title></head><body>你请求uri为：" + uri+"</body></html>";
            // 创建http响应
            FullHttpResponse response = new DefaultFullHttpResponse(
                    HttpVersion.HTTP_1_1,
                    HttpResponseStatus.OK,
                    Unpooled.copiedBuffer(msg, CharsetUtil.UTF_8));
            // 设置头信息
            response.headers().set(HttpHeaderNames.CONTENT_TYPE, "text/html; charset=UTF-8");
            //response.headers().set(HttpHeaderNames.CONTENT_TYPE, "text/plain; charset=UTF-8");
            // 将html write到客户端
            ctx.writeAndFlush(response).addListener(ChannelFutureListener.CLOSE);
        }
    }
}
