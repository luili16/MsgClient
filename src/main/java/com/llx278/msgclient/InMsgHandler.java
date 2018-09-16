package com.llx278.msgclient;

import com.llx278.msgclient.protocol.MsgValue;
import com.llx278.msgclient.protocol.TLV;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.util.Attribute;


public class InMsgHandler extends ChannelInboundHandlerAdapter {

    public static final String NAME = "InMsgHandler";

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {

        ByteBuf buf = (ByteBuf) msg;

        TLV tlv = TLV.unCompositeTlvFrame(buf);
        if (tlv == null) {
            return;
        }

        MsgValue value = MsgValue.valueOf(tlv.getValue());
        tlv.getValue().release();
        Attribute<AsyncClient> attr = ctx.channel().attr(AsyncClient.sClientAttr);
        AsyncClient asyncClient = attr.get();
        asyncClient.onMsgReceived(value);
    }

}


