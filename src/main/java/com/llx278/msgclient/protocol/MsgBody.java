package com.llx278.msgclient.protocol;

import io.netty.buffer.ByteBuf;
import io.netty.util.CharsetUtil;

public class MsgBody extends ValueExpand.BaseBody {

    private String body;

    public MsgBody(String body) {
        this.body = body;
    }

    public String getMsg() {
        return body;
    }

    @Override
    public String toString() {
        return "MsgBody{" +
                "body='" + body + '\'' +
                '}';
    }

    @Override
    public void writeTo(ByteBuf dst) {
        dst.writeCharSequence(body,CharsetUtil.UTF_8);
    }

    public static MsgBody valueOf(ByteBuf bodyBuf) {
        String body = bodyBuf.readCharSequence(bodyBuf.readableBytes(),CharsetUtil.UTF_8).toString();
        return new MsgBody(body);
    }
}
