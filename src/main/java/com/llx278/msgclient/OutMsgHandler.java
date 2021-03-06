package com.llx278.msgclient;

import com.llx278.msgclient.protocol.TLV;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.CompositeByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelOutboundHandlerAdapter;
import io.netty.channel.ChannelPromise;
import org.apache.log4j.Level;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

public class OutMsgHandler extends ChannelOutboundHandlerAdapter {
    private static final Logger sLogger = LogManager.getLogger(OutMsgHandler.class);
    public static final String NAME = "OutMsgHandler";

    @Override
    public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
        ByteBuf buf = (ByteBuf) msg;
        ByteBuf sync = ctx.alloc().buffer();
        sync.writeBytes(TLV.SYNC_BYTES);
        ByteBuf finish = ctx.alloc().buffer();
        finish.writeBytes(TLV.FINISH_BYTES);

        CompositeByteBuf finalBuf = ctx.alloc().compositeBuffer(3);
        finalBuf.addComponents(true,sync,buf,finish);
        ctx.writeAndFlush(finalBuf);
    }

    @Override
    public void flush(ChannelHandlerContext ctx) throws Exception {
        ctx.flush();
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        sLogger.log(Level.ERROR,cause);
        ctx.close();
    }
}
