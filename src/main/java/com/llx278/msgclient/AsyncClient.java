package com.llx278.msgclient;

import com.llx278.msgclient.protocol.*;
import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.CompositeByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.timeout.IdleStateHandler;
import io.netty.util.Attribute;
import io.netty.util.AttributeKey;
import io.netty.util.CharsetUtil;
import io.netty.util.ResourceLeakDetector;
import io.netty.util.concurrent.GenericFutureListener;
import io.reactivex.Observable;
import io.reactivex.ObservableSource;
import io.reactivex.Observer;
import io.reactivex.disposables.Disposable;
import io.reactivex.exceptions.UndeliverableException;
import io.reactivex.functions.Function;
import io.reactivex.plugins.RxJavaPlugins;
import io.reactivex.schedulers.Schedulers;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.*;

public class AsyncClient {

    public static final AttributeKey<Integer> sUidAttr = AttributeKey.valueOf("uid");
    public static final AttributeKey<AsyncClient> sClientAttr = AttributeKey.valueOf("client");

    // 写通道超时时间(心跳)
    private static final int WRITE_IDLE_TIME = 30;
    private static final TimeUnit WRITE_IDLE_TIME_UNIT = TimeUnit.MINUTES;

    public static final int COUNT = 5;
    public static final int DELAY = 10;
    public static final TimeUnit TIME_UNIT = TimeUnit.SECONDS;


    private final String mHost;
    private final int mPort;
    private final int mUid;
    private SocketChannel mSocketChannel;
    private Bootstrap mBootStrap;

    private Disposable mDisposable;

    private MessageReceiveListener mListener;

    public AsyncClient(String host, int port, int uid) {
        mHost = host;
        mPort = port;
        mUid = uid;
        init();
    }

    private void init() {
        ResourceLeakDetector.setLevel(ResourceLeakDetector.Level.PARANOID);
        EventLoopGroup mWorker = new NioEventLoopGroup();
        mBootStrap = new Bootstrap();
        mBootStrap.channel(NioSocketChannel.class);
        mBootStrap.group(mWorker);
        mBootStrap.remoteAddress(new InetSocketAddress(AsyncClient.this.mHost, AsyncClient.this.mPort));
        mBootStrap.handler(new ChannelInitializer<SocketChannel>() {
            @Override
            protected void initChannel(SocketChannel ch) throws Exception {
                mSocketChannel = ch;
                Attribute<Integer> uidAttr = ch.attr(sUidAttr);
                Integer uid = uidAttr.get();
                if (uid == null) {
                    uidAttr.set(mUid);
                }

                Attribute<AsyncClient> clientAttr = ch.attr(sClientAttr);
                AsyncClient client = clientAttr.get();
                if (client == null) {
                    clientAttr.set(AsyncClient.this);
                }
                ch.pipeline().addLast("idleHandler", new IdleStateHandler(0, WRITE_IDLE_TIME, 0, WRITE_IDLE_TIME_UNIT));
                ch.pipeline().addLast(InRegisterHandler.NAME, new InRegisterHandler());
                ch.pipeline().addLast(InHeartBeatHandler.NAME, new InHeartBeatHandler());
                ch.pipeline().addLast(InBufferHandler.NAME, new InBufferHandler());
                ch.pipeline().addLast(InMsgHandler.NAME, new InMsgHandler());
                ch.pipeline().addLast(OutMsgHandler.NAME, new OutMsgHandler());
            }
        });
    }

    public void setOnMesageReceivedListener(MessageReceiveListener l) {
        mListener = l;
    }

    public void onMsgReceived(MsgValue msg) {
        if (mListener != null) {
            mListener.onMessageReceive(msg);
        }
    }

    public void connect(int retryCount, int delay, TimeUnit unit) {
        System.out.println("准备连接");
        Observable<ChannelFuture> connect = Observable.create(emitter -> {
            ChannelFuture f = mBootStrap.connect();
            f.addListener(future -> {

                if (future == null) {
                    emitter.onError(new RuntimeException("connection failed!"));
                    return;
                }

                if (future.isDone() && future.isSuccess()) {
                    emitter.onNext(f);
                    emitter.onComplete();
                    return;
                }

                if (future.isDone() && future.cause() != null) {
                    emitter.onError(future.cause());
                }
            });
        });
        connect.subscribeOn(Schedulers.io()).
                observeOn(Schedulers.io()).
                retryWhen(exceptionObservable -> exceptionObservable.zipWith(Observable.range(0, retryCount), (exception, hasRetried) -> {
                    System.out.println(exception.getMessage());
                    return hasRetried;
                }).flatMap(hasRetried -> {
                    System.out.println("retry count : " + hasRetried);
                    if (hasRetried == retryCount - 1) {
                        System.out.println("connect failed cancel...");
                        if (mDisposable != null) {
                            mDisposable.dispose();
                            mDisposable = null;
                        }
                    }
                    return Observable.timer(delay, unit);
                })).
                flatMap((Function<ChannelFuture, ObservableSource<ChannelFuture>>) channelFuture -> {
                    System.out.println("连接 " + channelFuture.channel().remoteAddress() + " 成功");
                    return getCloseState(channelFuture);
                }).
                subscribe(new Observer<ChannelFuture>() {
                    @Override
                    public void onSubscribe(Disposable d) {
                        mDisposable = d;
                    }

                    @Override
                    public void onNext(ChannelFuture closeFuture) {
                        System.out.println("onNext");
                    }

                    @Override
                    public void onError(Throwable e) {
                        System.out.println(e.getMessage());
                        if (mDisposable != null) {
                            mDisposable.dispose();
                            mDisposable = null;
                        }
                    }

                    @Override
                    public void onComplete() {
                        if (mDisposable != null) {
                            mDisposable.dispose();
                            mDisposable = null;
                        }
                    }
                });
        RxJavaPlugins.setErrorHandler(e -> {
            if (e instanceof UndeliverableException) {
                e = e.getCause();
                System.out.println("e is : " + e.getMessage());
            }
        });
    }

    private Observable<ChannelFuture> getCloseState(ChannelFuture f) {
        return Observable.create(emitter -> {
            Channel c = f.channel();
            ChannelFuture closeFuture = c.closeFuture();
            closeFuture.addListener(future -> {
                // 此socketChannel已经退出了，在这种情况下意味着与
                // 服务器的连接已经断开
                emitter.onComplete();
            });
        });
    }

    public Observable<ChannelFuture> writeMsg(int toUid, String body, Map<String, String> header) {
        return Observable.create(emitter -> writeMsg(toUid, body, header, future -> {
            if (future.isDone() && future.isSuccess()) {
                emitter.onNext(future);
            }

            if (future.isDone() && future.cause() != null) {
                emitter.onError(future.cause());
            }

            emitter.onComplete();
        }));
    }

    public void writeMsg(int toUid, String body, Map<String, String> header, GenericFutureListener<ChannelFuture> listener) {

        if (body.length() > BaseValue.MAX_LENGTH) {
            System.out.println("超出了value限制的长度 此时body的长度 : " + body.length());
            return;
        }

        ByteBuf v = Unpooled.buffer();
        // 出于性能的要求，这个方法里面避免直接new对象和发生内存拷贝
        MsgValue.quickWrite(mUid, toUid, header, body, v);
        ByteBuf tl = Unpooled.buffer();
        CompositeByteBuf tlv = Unpooled.compositeBuffer();
        TLV.quickCompositeTlvFrame(Type.FRAME_MSG, tlv, tl, v);

        ChannelFuture f = mSocketChannel.writeAndFlush(tlv);
        if (listener != null) {
            f.addListener(listener);
        }
    }

    public void writeMsgQuietly(int toUid, String body, Map<String, String> header) {
        writeMsg(toUid, body, header, null);
    }

    public interface MessageReceiveListener {
        void onMessageReceive(MsgValue msg);
    }

    public static void main(String[] args) {

        //String host = "172.18.8.119";
        String host = "127.0.0.1";

        int uid;
        if (args.length != 0) {
            uid = Integer.parseInt(args[0]);
        } else {
            uid = 234;
        }
        //String mHost = "172.20.151.106";

        /*AsyncClient client = new AsyncClient(host, 12306, uid);
        client.connect(4, 5, TimeUnit.SECONDS);
        client.setOnMesageReceivedListener(msg -> {
            System.out.println("收到消息 : " + msg.toString());
        });
        BufferedReader br = new BufferedReader(new InputStreamReader(System.in));

        while (true) {
            System.out.println("等待输入");
            try {
                String msg = br.readLine();
                System.out.println("msg : " + msg);
                String[] uidAndMsg = msg.split(":");
                int toUid = Integer.parseInt(uidAndMsg[0]);
                byte[] body = uidAndMsg[1].getBytes(CharsetUtil.UTF_8);
                Map<String, String> header = new HashMap<>();
                header.put("Content-Type", "txt");
                header.put("Expanded-Name", ".txt");
                client.writeMsgQuietly(toUid, uidAndMsg[1], header);

            } catch (IOException e) {
                e.printStackTrace();
            }
        }*/

        ExecutorService executor = Executors.newFixedThreadPool(2000);
        Random random = new Random(40);
        int maxCount = 30;
        for (int i = 0; i < maxCount; i++) {
            final int fromUid = i;
            executor.execute(() -> {

                AsyncClient client = new AsyncClient(host, 12306, fromUid);
                client.connect(4, 5, TimeUnit.SECONDS);
                client.setOnMesageReceivedListener(new MessageReceiveListener() {
                    @Override
                    public void onMessageReceive(MsgValue msg) {
                        System.out.println(msg.toString());
                    }
                });
                try {
                    Thread.sleep(5000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                while (true) {
                    try {
                        Thread.sleep(random.nextInt(2000) + 200);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    String body = getRandomString(30000);
                    int toUid = random.nextInt(maxCount);
                    CountDownLatch signal = new CountDownLatch(1);
                    Map<String, String> header = new HashMap<>();
                    header.put("Content-Type", "txt");
                    header.put("Expanded-Name", ".txt");
                    if (toUid == fromUid) {
                        System.out.println("忽略自己给自己发消息");
                        continue;
                    }

                    client.writeMsgQuietly(toUid, body, header);

                }
            });
        }
    }

    public static String getRandomString(int length) {
        //定义一个字符串（A-Z，a-z，0-9）即62位；
        String str = "zxcvbnmlkjhgfdsaqwertyuiopQWERTYUIOPASDFGHJKLZXCVBNM1234567890";
        //由Random生成随机数
        Random random = new Random();
        StringBuffer sb = new StringBuffer();
        //长度为几就循环几次
        for (int i = 0; i < length; ++i) {
            //产生0-61的数字
            int number = random.nextInt(62);
            //将产生的数字通过length次承载到sb中
            sb.append(str.charAt(number));
        }
        //将承载的字符转换成字符串
        return sb.toString();
    }
}
