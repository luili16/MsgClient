package com.llx278.msgclient.protocol;

import io.netty.buffer.ByteBuf;
import io.netty.util.CharsetUtil;

import java.util.HashMap;

public class Debug {

    public static void print(ByteBuf buf) {

        int type = buf.readInt();
        if (type == Type.FRAME_REGISTER) {

            System.out.println("---- 注册帧 开始----");
            int len = buf.readInt();
            System.out.println("预期长度 : " + len + " 真实长度 : " + buf.readableBytes());
            int uid = buf.readInt();
            System.out.println("uid : " + uid);
            System.out.println("---- 注册帧 结束----");

        } else if (type == Type.FRAME_MSG) {
            System.out.println("--- 消息帧 开始-----");
            int len = buf.readInt();
            System.out.println("预期长度 : " + len + " 真实长度 : " + buf.readableBytes());
            int fromUid = buf.readInt();
            int toUid = buf.readInt();
            HashMap<String,String> header = new HashMap<>();
            MsgFrame.ContentParser.parse(header,buf);
            String body = buf.readCharSequence(buf.readableBytes(),CharsetUtil.UTF_8).toString();
            System.out.println("fromUid : " + fromUid + " toUid : " + toUid);
            System.out.println("header : " + header.toString());
            System.out.println("body : " + body);
            System.out.println("----- 消息帧 结束 -------");
        } else if (type == Type.FRAME_HEART) {
            System.out.println("------ 心跳帧 开始-----");
            int len = buf.readInt();
            System.out.println("预期长度 : " + len + " 真实长度 : " + buf.readableBytes());
            int uid = buf.readInt();
            System.out.println("uid : " + uid);
            System.out.println("------ 心跳帧 结束-----");
        }

    }

}
