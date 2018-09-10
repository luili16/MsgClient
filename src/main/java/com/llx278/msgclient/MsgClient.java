package com.llx278.msgclient;

import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.util.CharsetUtil;
import io.reactivex.*;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetSocketAddress;
import java.nio.charset.Charset;

public class MsgClient {

    private final String host;
    private final int port;
    private final int uid;
    private SocketChannel socketChannel;

    private Disposable registerDisposable;


    public MsgClient(String host, int port, int uid) {
        this.host = host;
        this.port = port;
        this.uid = uid;

    }

    public void register() {
        Observable<SocketChannel> observable = Observable.create(emitter -> {

            // 在这里启动MsgClient客户端
            EventLoopGroup worker = new NioEventLoopGroup();
            try {
                Bootstrap b = new Bootstrap();
                b.group(worker).
                        channel(NioSocketChannel.class).
                        remoteAddress(new InetSocketAddress(this.host, this.port)).
                        handler(new ChannelInitializer<SocketChannel>() {
                            @Override
                            protected void initChannel(SocketChannel ch) throws Exception {
                                ch.pipeline().addLast("InHandler", new InHandler());
                                ch.pipeline().addLast("OutHandler", new OutHandler());
                                emitter.onNext(ch);
                            }
                        });
                ChannelFuture f = b.connect().sync();
                f.channel().closeFuture().sync();
            } finally {
                worker.shutdownGracefully();
                if (registerDisposable != null) {
                    registerDisposable.dispose();
                }
            }


        });

        observable.
                subscribeOn(Schedulers.newThread()).
                subscribe(new Observer<SocketChannel>() {
                    @Override
                    public void onSubscribe(Disposable d) {
                        MsgClient.this.registerDisposable = d;
                    }

                    @Override
                    public void onNext(SocketChannel socketChannel) {
                        MsgClient.this.socketChannel = socketChannel;
                    }

                    @Override
                    public void onError(Throwable e) {
                    }

                    @Override
                    public void onComplete() {
                    }
                });
    }

    public void write(String msg) {
        /*InHandler inHandler = (InHandler) this.socketChannel.pipeline().get("InHandler");
        if (inHandler == null) {
            System.out.println("null");
            return;
        }
        inHandler.getChannelHandlerContext().writeAndFlush(Unpooled.copiedBuffer(msg, CharsetUtil.UTF_8));*/

        this.socketChannel.writeAndFlush(Unpooled.copiedBuffer(msg, CharsetUtil.UTF_8));
    }



    public static void main(String[] args) {

        MsgClient client = new MsgClient("172.20.151.106", 12306, 1);

        client.register();

        BufferedReader bf = new BufferedReader(new InputStreamReader(System.in));
        while (true) {
            String msg;
            try {
                msg = bf.readLine();
                System.out.println("msg is : " + msg);
                client.write(msg);
            } catch (IOException e) {
                e.printStackTrace();
                break;
            }
        }
    }
}
