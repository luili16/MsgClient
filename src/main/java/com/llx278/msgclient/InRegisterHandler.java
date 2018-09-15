package com.llx278.msgclient;

import com.llx278.msgclient.protocol.RegisterFrame;
import com.llx278.msgclient.protocol.TLV;
import com.llx278.msgclient.protocol.Type;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.util.Attribute;

public class InRegisterHandler extends ChannelInboundHandlerAdapter {

    public static final String NAME = "InRegisterHandler";

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        System.out.println("channelactive 准备发送注册帧");
        Attribute<Integer> uidAttr = ctx.channel().attr(AsyncClient.sUidAttr);
        Integer uid = uidAttr.get();
        RegisterFrame rf = new RegisterFrame(Type.FRAME_REGISTER,new RegisterFrame.Value(uid));
        ByteBuf msg = TLV.composite(rf);

        ctx.writeAndFlush(msg);

        ctx.fireChannelActive();
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        System.out.println("channelInactive 准备重新连接");
        Attribute<AsyncClient> clientAttr = ctx.channel().attr(AsyncClient.sClientAttr);
        AsyncClient asyncClient = clientAttr.get();
        asyncClient.connect();

        ctx.fireChannelInactive();
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        System.out.println("InRegisterHandler exception caught 关闭当前的channel 准备重新连接");
        ctx.fireExceptionCaught(cause);
        ctx.close();

        Attribute<AsyncClient> clientAttr = ctx.channel().attr(AsyncClient.sClientAttr);
        AsyncClient asyncClient = clientAttr.get();
        asyncClient.connect();
    }
}
