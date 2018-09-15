package com.llx278.msgclient.protocol;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.CompositeByteBuf;
import io.netty.buffer.Unpooled;

public class TLV {

    public static ByteBuf composite(BaseFrame baseFrame) {
        ByteBuf tlv = Unpooled.buffer();
        tlv.writeInt(baseFrame.type);
        tlv.writeInt(baseFrame.value.length());
        baseFrame.value.writeTo(tlv);
        return tlv;
    }

    public static ByteBuf composite(int type, int length, ByteBuf value) {
        CompositeByteBuf buf = Unpooled.compositeBuffer();
        ByteBuf tl = Unpooled.buffer();
        tl.writeInt(type);
        tl.writeInt(length);
        buf.addComponents(true,tl,value);
        return buf;
    }

    public static ByteBuf composite(int type,int length,byte[] value) {
        ByteBuf tlv = Unpooled.buffer();
        tlv.writeInt(type);
        tlv.writeInt(length);
        tlv.writeBytes(value);
        return tlv;
    }

    public static ByteBuf composite(int type,int length,int value) {
        ByteBuf tlv = Unpooled.buffer();
        tlv.writeInt(type);
        tlv.writeInt(length);
        tlv.writeInt(value);
        return tlv;
    }
}
