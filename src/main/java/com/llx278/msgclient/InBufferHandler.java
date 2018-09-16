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

    private List<ByteBuf> bufList = new LinkedList<>();

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        ByteBuf buf = (ByteBuf) msg;
        bufList.add(buf);
    }

    @Override
    public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {

        CompositeByteBuf cb = ctx.alloc().compositeBuffer(bufList.size());
        cb.addComponents(true,bufList);

        try {

            if (cb.readableBytes() == 0) {
                System.out.println("-------- EOF --------- 与服务器的连接断开");
                ctx.close();
                return;
            }


            if (cb.readableBytes() < 8) {
                System.out.println("没有读到长度，继续读!!");
                ctx.read();
                return;
            }

            int len = cb.getInt(4);
            if (len != cb.readableBytes() - 8) {
                System.out.println("没有读完，继续读!!");
                ctx.read();
                return;
            }

            ctx.fireChannelRead(cb);
            ctx.fireChannelReadComplete();

        } finally {
            bufList.clear();
        }






    }
}
