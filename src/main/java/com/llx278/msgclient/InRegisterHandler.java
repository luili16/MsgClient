package com.llx278.msgclient;

import com.llx278.msgclient.protocol.RegisterValue;
import com.llx278.msgclient.protocol.TLV;
import com.llx278.msgclient.protocol.Type;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.CompositeByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.util.Attribute;
import org.apache.log4j.Level;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

public class InRegisterHandler extends ChannelInboundHandlerAdapter {

    private static final Logger sLogger = LogManager.getLogger(InRegisterHandler.class);
    public static final String NAME = "InRegisterHandler";

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        sLogger.log(Level.DEBUG, "channelactive 准备发送注册帧");
        Attribute<Integer> uidAttr = ctx.channel().attr(AsyncClient.sUidAttr);
        Integer uid = uidAttr.get();

        RegisterValue registerValue = new RegisterValue(uid);
        CompositeByteBuf dst = ctx.alloc().compositeBuffer();
        ByteBuf tl = ctx.alloc().buffer();
        ByteBuf v = ctx.alloc().buffer();
        TLV.compositeTlvFrame(Type.FRAME_REGISTER, registerValue, dst, tl, v);
        ctx.channel().writeAndFlush(dst);
        ctx.fireChannelActive();
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        sLogger.log(Level.ERROR, "channelInactive 准备重新连接");
        Attribute<AsyncClient> clientAttr = ctx.channel().attr(AsyncClient.sClientAttr);
        AsyncClient asyncClient = clientAttr.get();
        asyncClient.connect(AsyncClient.COUNT, AsyncClient.DELAY, AsyncClient.TIME_UNIT);

        ctx.fireChannelInactive();
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        sLogger.log(Level.ERROR, "InRegisterHandler exception caught 关闭当前的channel 准备重新连接", cause);
        ctx.close();
        Attribute<AsyncClient> clientAttr = ctx.channel().attr(AsyncClient.sClientAttr);
        AsyncClient asyncClient = clientAttr.get();
        asyncClient.connect(AsyncClient.COUNT, AsyncClient.DELAY, AsyncClient.TIME_UNIT);
    }
}
