package com.llx278.msgclient.protocol;

import io.netty.buffer.ByteBuf;

public class HeartBeatValue extends BaseValue {

    private int uid;

    public HeartBeatValue(int uid) {
        this.uid = uid;
    }

    public int getUid() {
        return uid;
    }

    @Override
    public void writeTo(ByteBuf dst) {
        dst.writeInt(this.uid);
    }

    /**
     * 快速写入
     * @param uid uid
     * @param dst dst
     */
    public static void quickWrite(int uid, ByteBuf dst) {
        dst.writeInt(uid);
    }

}
