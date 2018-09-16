package com.llx278.msgclient.protocol;

import io.netty.buffer.ByteBuf;

public class RegisterValue extends BaseValue {

    private int uid;

    public RegisterValue(int uid) {
        this.uid = uid;
    }

    public int getUid() {
        return uid;
    }

    public static RegisterValue valueOf(ByteBuf registerBuf) {
        int uid = registerBuf.readInt();
        return new RegisterValue(uid);
    }

    @Override
    public void writeTo(ByteBuf dst) {
        dst.writeInt(uid);
    }
}
