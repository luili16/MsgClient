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
        System.out.println("channelRead");
        ByteBuf buf = (ByteBuf) msg;
        bufList.add(buf);
    }


    @Override
    public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {
        System.out.println("channelReadComplete");

        ByteBuf tlvBuf = ctx.alloc().buffer();
        for (ByteBuf buf : bufList) {
            tlvBuf.writeBytes(buf);
        }

        if (tlvBuf.readableBytes() == 0) {
            System.out.println("-------- EOF --------- 与服务器的连接断开");
            ctx.close();
            tlvBuf.release();
            bufList.clear();
            releaseBuffList();
            return;
        }


        if (tlvBuf.readableBytes() < 8) {
            System.out.println("没有获取到消息的长度，继续读!!");
            ctx.read();
            tlvBuf.release();
            return;
        }

        int len = tlvBuf.getInt(4);
        if (len != tlvBuf.readableBytes() - 8) {
            System.out.println("没有读完，继续读!! 期待的长度 : " + len + " 读取到的长度 : " + (tlvBuf.readableBytes() - 8));
            ctx.read();
            tlvBuf.release();
            return;
        }
        releaseBuffList();
        bufList.clear();

        ctx.fireChannelRead(tlvBuf);
        ctx.fireChannelReadComplete();
    }

    private void releaseBuffList() {
        for (ByteBuf buf : bufList) {
            if (buf.refCnt() != 0) {
                buf.release();
            }
        }
    }
}
