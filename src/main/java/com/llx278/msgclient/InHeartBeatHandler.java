package com.llx278.msgclient;

import com.llx278.msgclient.protocol.HeartBeatFrame;
import com.llx278.msgclient.protocol.TLV;
import com.llx278.msgclient.protocol.Type;
import io.netty.buffer.ByteBuf;
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
                HeartBeatFrame hb = new HeartBeatFrame(Type.FRAME_HEART,new HeartBeatFrame.Value(uid));
                ByteBuf msg = TLV.composite(hb);
                ChannelFuture f = ctx.writeAndFlush(msg);
                f.addListener(future -> {
                });
            }

            return;
        }

        super.userEventTriggered(ctx, evt);
    }
}
