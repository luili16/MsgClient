package com.llx278.msgclient;

import com.llx278.msgclient.protocol.MsgFrame;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.util.Attribute;
import io.netty.util.ReferenceCountUtil;

public class InMsgHandler extends SimpleChannelInboundHandler<ByteBuf> {

    public static final String NAME = "InMsgHandler";

    private ByteBuf msg;

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, ByteBuf msg) throws Exception {
        this.msg = msg;
    }

    @Override
    public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {

        if (msg.refCnt() == 0) {
            msg = ReferenceCountUtil.retain(msg);
        }

        MsgFrame msgFrame = MsgFrame.valueOf(msg);

        Attribute<AsyncClient> attr = ctx.channel().attr(AsyncClient.sClientAttr);
        AsyncClient asyncClient = attr.get();
        asyncClient.onMsgReceived(msgFrame.getValue());
        ReferenceCountUtil.release(msg,msg.refCnt());
    }
}


