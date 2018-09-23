package com.llx278.msgclient;

import com.llx278.msgclient.protocol.HeartBeatValue;
import com.llx278.msgclient.protocol.TLV;
import com.llx278.msgclient.protocol.Type;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.CompositeByteBuf;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;
import io.netty.util.Attribute;

public class InHeartBeatHandler extends ChannelInboundHandlerAdapter {

    public static final String NAME = "InHeartBeatHandler";

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {

        if (evt instanceof IdleStateEvent) {
            IdleStateEvent e = (IdleStateEvent) evt;
            if (e.state() == IdleState.WRITER_IDLE) {
                Attribute<Integer> uidAttr = ctx.channel().attr(AsyncClient.sUidAttr);
                Integer uid = uidAttr.get();

                CompositeByteBuf tlv = ctx.alloc().compositeBuffer();
                ByteBuf tl = ctx.alloc().buffer();
                ByteBuf v = ctx.alloc().buffer();
                HeartBeatValue.quickWrite(uid,v);
                TLV.quickCompositeTlvFrame(Type.FRAME_HEART,tlv,tl,v);
                ChannelFuture f = ctx.channel().writeAndFlush(tlv);
                f.addListener(future -> {
                });
            }
            return;
        }

        super.userEventTriggered(ctx, evt);
    }
}
