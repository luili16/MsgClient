package com.llx278.msgclient;

import com.llx278.msgclient.protocol.Debug;
import com.llx278.msgclient.protocol.TLV;
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
        ByteBuf sync = ctx.alloc().buffer();
        ByteBuf finish = ctx.alloc().buffer();
        sync.writeInt(TLV.SYNC);
        finish.writeInt(TLV.FINISH);
        ctx.write(sync);
        ctx.write(msg);
        ctx.write(finish);
        ctx.flush();
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
