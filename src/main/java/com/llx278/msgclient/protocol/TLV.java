package com.llx278.msgclient.protocol;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.CompositeByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.util.ReferenceCountUtil;

public class TLV {

    /**
     * 代表一个消息的起始
     */
    public static final int SYNC =   0xAAAAAAAA;
    /**
     * 代表一个消息的结束
     */
    public static final int FINISH = 0x55555555;

    private final int type;
    private final int len;
    private final ByteBuf value;

    public TLV(int type, int len, ByteBuf value) {
        this.type = type;
        this.len = len;
        this.value = value;
    }

    public int getType() {
        return type;
    }

    public int getLen() {
        return len;
    }

    public ByteBuf getValue() {
        return value;
    }

    public static void compositeTlvFrame(int type, BaseValue value, CompositeByteBuf dst, ByteBuf tl, ByteBuf v) {
        tl.writeInt(type);
        value.writeTo(v);
        tl.writeInt(v.readableBytes());
        dst.addComponents(true,tl,v);
    }

    /**
     * 直接写入
     * @param type type
     * @param dst dst
     * @param tl tl
     * @param v v
     */
    public static void quickCompositeTlvFrame(int type,CompositeByteBuf dst,ByteBuf tl,ByteBuf v) {
        tl.writeInt(type);
        tl.writeInt(v.readableBytes());
        dst.addComponents(true,tl,v);
    }

    public static TLV unCompositeTlvFrame(ByteBuf tlvBuf) {
        int type = tlvBuf.readInt();
        int len = tlvBuf.readInt();
        if (len != tlvBuf.readableBytes()) {
            System.out.println("invalid tlv frame! expected len is " + len + " actual len is " + tlvBuf.readableBytes());
            return null;
        }

        return new TLV(type,len,tlvBuf);
    }
}
