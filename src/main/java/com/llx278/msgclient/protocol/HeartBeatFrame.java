package com.llx278.msgclient.protocol;

import io.netty.buffer.ByteBuf;

public class HeartBeatFrame extends BaseFrame<HeartBeatFrame.Value> {
    public HeartBeatFrame(int type, Value value) {
        super(type, value);
    }

    @Override
    public void writeToByteBuf(ByteBuf buf) {
        buf.writeInt(type);
        buf.writeInt(value.length());
        value.writeTo(buf);
    }

    public static HeartBeatFrame valueOf(ByteBuf buf) {
        int type = buf.readInt();
        int len = buf.readInt();
        Value value = new Value(buf.readInt());
        if (len != value.length()) {
            throw new IllegalStateException("invalid tlv msg!");
        }
        return new HeartBeatFrame(type,value);
    }

    public static boolean isHeartBeatFrame(ByteBuf buf) {
        int type = buf.getInt(0);
        return type == Type.FRAME_HEART;
    }


    public static class Value extends BaseFrame.BaseValue {

        private final int uid;

        public Value(int uid) {
            this.uid = uid;
        }

        @Override
        public int length() {
            return 4;
        }

        @Override
        public void writeTo(ByteBuf outByteBuf) {
            outByteBuf.writeInt(uid);
        }


    }
}
