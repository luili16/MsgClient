package com.llx278.msgclient;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.CompositeByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.util.ReferenceCountUtil;

import java.util.LinkedList;
import java.util.List;

public class InBufferHandler extends ChannelInboundHandlerAdapter {

    public static final String NAME = "InBufferHandler";

    private List<ByteBuf> mByteBufs = new LinkedList<>();

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        ByteBuf buf = (ByteBuf) msg;
        mByteBufs.add(buf);
    }

    @Override
    public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {


        CompositeByteBuf compositeByteBuf = ctx.alloc().compositeBuffer(mByteBufs.size());
        compositeByteBuf.addComponents(true,mByteBufs);

        if (compositeByteBuf.readableBytes() == 0) {
            System.out.println("-------- EOF --------- 与服务器的连接断开");
            ctx.close();
            return;
        }
        compositeByteBuf = ReferenceCountUtil.retain(compositeByteBuf);
        ctx.fireChannelRead(compositeByteBuf);
        ctx.fireChannelReadComplete();
        mByteBufs.clear();
    }

}
