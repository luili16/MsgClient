package com.llx278.msgclient;

import io.netty.channel.ChannelHandlerContext;

public class ConnectionManager {

    private ChannelHandlerContext channelHandlerContext;

    public void setChannelHandlerContext(ChannelHandlerContext channelHandlerContext) {
        this.channelHandlerContext = channelHandlerContext;
    }

    public ChannelHandlerContext getChannelHandlerContext() {
        return channelHandlerContext;
    }
}
