package com.llx278.msgclient.protocol;

import io.netty.buffer.ByteBuf;
import io.netty.util.CharsetUtil;

import java.util.HashMap;
import java.util.Map;

public class MsgHeader extends ValueExpand.BaseHeader {
    
    private final int fromUid;
    private final int toUid;
    private final Map<String,String> type;

    public MsgHeader(int fromUid, int toUid, Map<String,String> type) {
        this.fromUid = fromUid;
        this.toUid = toUid;
        this.type = type;
    }

    public static MsgHeader valueOf(ByteBuf headerBuf) {
        int fromUid = headerBuf.readInt();
        int toUid = headerBuf.readInt();
        HashMap<String,String> type = new HashMap<>();
        ContentParser.parse(type,headerBuf);
        return new MsgHeader(fromUid,toUid,type);
    }

    @Override
    public String toString() {
        return "MsgHeader{" +
                "fromUid=" + fromUid +
                ", toUid=" + toUid +
                ", type=" + type +
                '}';
    }

    public int getFromUid() {
        return fromUid;
    }

    public int getToUid() {
        return toUid;
    }

    public Map<String, String> getType() {
        return type;
    }
    

    @Override
    public void writeTo(ByteBuf dst) {
        dst.writeInt(this.fromUid);
        dst.writeInt(this.toUid);
        dst.writeCharSequence(ContentParser.toHeader(this.type),CharsetUtil.UTF_8);
    }
}
