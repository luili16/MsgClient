package com.llx278.msgclient;

import com.llx278.msgclient.protocol.Debug;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelOutboundHandlerAdapter;
import io.netty.channel.ChannelPromise;
import io.netty.util.ReferenceCountUtil;

public class OutMsgHandler extends ChannelOutboundHandlerAdapter {

    public static final String NAME = "OutMsgHandler";

    @Override
    public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
        if (msg instanceof ByteBuf) {
            ByteBuf buf = (ByteBuf) msg;

            if (buf.refCnt() == 0) {
                buf = ReferenceCountUtil.retain(buf);
            }
            ByteBuf temp = buf.copy();
            Debug.print(temp);
            ctx.write(buf);

            if (buf.refCnt() != 0) {
                ReferenceCountUtil.release(buf);
            }
        }
    }

    @Override
    public void flush(ChannelHandlerContext ctx) throws Exception {
        ctx.flush();
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        System.out.println("----------------- out ---------------------");
        cause.printStackTrace();
        ctx.close();
    }
}
