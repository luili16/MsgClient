package com.llx278.msgclient.protocol;

import io.netty.buffer.ByteBuf;

public class ValueExpand<H extends ValueExpand.BaseHeader, B extends ValueExpand.BaseBody> extends BaseValue {

    private H header;
    private B body;

    public ValueExpand(H header, B value) {
        this.header = header;
        this.body = value;
    }

    public H getHeader() {
        return header;
    }

    public B getBody() {
        return body;
    }

    @Override
    public String toString() {
        return "ValueExpand{" +
                "header=" + header +
                ", body=" + body +
                '}';
    }

    @Override
    public void writeTo(ByteBuf dst) {
        this.header.writeTo(dst);
        this.body.writeTo(dst);
    }

    public abstract static class BaseBody {
        public abstract void writeTo(ByteBuf dst);
    }

    public abstract static class BaseHeader {
        public abstract void writeTo(ByteBuf dst);
    }
}
