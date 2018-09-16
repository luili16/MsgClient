package com.llx278.msgclient.protocol;

import io.netty.buffer.ByteBuf;
import io.netty.util.CharsetUtil;

import java.util.Map;

public class MsgValue extends ValueExpand<MsgHeader,MsgBody> {
    public MsgValue(MsgHeader header, MsgBody value) {
        super(header, value);
    }



    public static MsgValue valueOf(ByteBuf valueBuf) {
        MsgHeader header = MsgHeader.valueOf(valueBuf);
        MsgBody body = MsgBody.valueOf(valueBuf);
        return new MsgValue(header,body);
    }

    /**
     * 快速写入,目的是为了高效，缺点是破坏了封装
     * @param fromUid fromUid
     * @param toUid toUid
     * @param type type
     * @param body body
     * @param dst dst
     */
    public static void quickWrite(int fromUid, int toUid, Map<String,String> type, String body, ByteBuf dst) {
        dst.writeInt(fromUid);
        dst.writeInt(toUid);
        dst.writeCharSequence(ContentParser.toHeader(type),CharsetUtil.UTF_8);
        dst.writeCharSequence(body,CharsetUtil.UTF_8);
    }
}
