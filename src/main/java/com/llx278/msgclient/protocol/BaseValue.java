package com.llx278.msgclient.protocol;

import io.netty.buffer.ByteBuf;

public abstract class BaseValue {

    public static final int MAX_LENGTH = 1024 * 1024; // 1MB

    public abstract void writeTo(ByteBuf dst);

}
