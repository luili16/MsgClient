package com.llx278.msgclient.protocol;

import io.netty.buffer.ByteBuf;

public abstract class BaseValue {

    public static final int MAX_LENGTH = 1024 * 1024 - 8; // 1MB - 8byte

    public abstract void writeTo(ByteBuf dst);

}
