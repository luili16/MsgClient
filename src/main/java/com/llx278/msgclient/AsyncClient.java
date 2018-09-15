package com.llx278.msgclient;

import com.llx278.msgclient.protocol.MsgFrame;
import com.llx278.msgclient.protocol.Type;
import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
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
import io.netty.util.concurrent.GenericFutureListener;
import io.reactivex.Observable;
import io.reactivex.ObservableSource;
import io.reactivex.Observer;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Function;
import io.reactivex.schedulers.Schedulers;

import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class AsyncClient {

    public static final AttributeKey<Integer> sUidAttr = AttributeKey.valueOf("uid");
    public static final AttributeKey<AsyncClient> sClientAttr = AttributeKey.valueOf("client");

    // 写通道超时时间
    private static final int WRITE_IDLE_TIME = 30;
    private static final TimeUnit WRITE_IDLE_TIME_UNIT = TimeUnit.MINUTES;

    private static final int DELAY = 10;
    private static final TimeUnit TIME_UNIT = TimeUnit.SECONDS;


    private final String mHost;
    private final int mPort;
    private final int mUid;
    private SocketChannel mSocketChannel;
    private Bootstrap mBootStrap;

    private Disposable mDisposable;

    private int mDelay;
    private TimeUnit mTimeUnit;

    private MessageReceiveListener mListener;

    private AsyncClient(String host, int port, int uid) {
        this(host, port, uid, DELAY, TIME_UNIT);
        init();
    }

    public AsyncClient(String host, int port, int uid, int delay, TimeUnit timeUnit) {
        this.mHost = host;
        this.mPort = port;
        this.mUid = uid;
        mDelay = delay;
        mTimeUnit = timeUnit;
    }

    private void init() {
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

    public void onMsgReceived(MsgFrame.Value msg) {
        if (mListener != null) {
            mListener.onMessageReceive(msg);
        }
    }

    public void connect() {
        Observable<ChannelFuture> connect = Observable.create(emitter -> {
            ChannelFuture f = mBootStrap.connect();
            f.addListener(future -> {
                if (future.isDone() && future.isSuccess()) {
                    emitter.onNext(f);
                }

                if (future.isDone() && future.cause() != null) {
                    emitter.onError(future.cause());
                }

                emitter.onComplete();
            });
        });
        connect.subscribeOn(Schedulers.io()).
                observeOn(Schedulers.io()).retryWhen(error -> error.
                flatMap(throwable -> Observable.timer(mDelay, mTimeUnit))).
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
                    }

                    @Override
                    public void onError(Throwable e) {
                    }

                    @Override
                    public void onComplete() {
                        if (mDisposable != null) {
                            mDisposable.dispose();
                            mDisposable = null;
                        }
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

    public Observable<ChannelFuture> writeMsg(int toUid, byte[] body, Map<String, String> header) {
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

    public void writeMsg(int toUid, byte[] body, Map<String, String> header, GenericFutureListener<ChannelFuture> listener) {

        System.out.println("准备发送消息 到 uid : " + toUid);
        if (body.length > MsgFrame.Value.MAX_LENGTH) {
            System.out.println("超出了value限制的长度 此时body的长度 : " + body.length);
            return;
        }
        MsgFrame.Value value = new MsgFrame.Value(this.mUid,toUid,header,body);
        MsgFrame mf = new MsgFrame(Type.FRAME_MSG,value);
        System.out.println("len : " + value.length());
        ByteBuf bf = Unpooled.buffer();
        mf.writeToByteBuf(bf);

        ChannelFuture f = mSocketChannel.writeAndFlush(bf);
        if (listener != null) {
            f.addListener(listener);
        }
    }

    public void writeMsgQuietly(int toUid, byte[] body, Map<String, String> header) {
        writeMsg(toUid, body, header, null);
    }

    public interface MessageReceiveListener {
        void onMessageReceive(MsgFrame.Value msg);
    }

    public static void main(String[] args) {

        String host = "172.18.8.119";


        int uid ;
        if (args.length != 0) {
           uid = Integer.parseInt(args[0]);
        } else {
            uid = 234;
        }
        //String mHost = "172.20.151.106";

        /*AsyncClient client = new AsyncClient(host, 12306, uid);
        client.connect();
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
                Map<String,String> header = new HashMap<>();
                header.put("Content-Type", "txt");
                header.put("Expanded-Name", ".txt");
                client.writeMsgQuietly(toUid,body,header);

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
                try {
                    Thread.sleep(random.nextInt(2000) + 200);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                AsyncClient client = new AsyncClient(host, 12306, fromUid);
                client.connect();
                client.setOnMesageReceivedListener(msg -> {
                    System.out.println("msg : " + msg.toString());
                });
                while (true) {
                    try {
                        Thread.sleep(5000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    String txt = getRandomString(random.nextInt(1000));
                    int toUid = random.nextInt(maxCount);

                    byte[] body = txt.getBytes(CharsetUtil.UTF_8);
                    Map<String, String> header = new HashMap<>();
                    header.put("Content-Type", "txt");
                    header.put("Expanded-Name", ".txt");
                    client.writeMsgQuietly(toUid,body,header);
                    System.out.println("写msg 结束");
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
