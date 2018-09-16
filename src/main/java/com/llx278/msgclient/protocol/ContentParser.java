package com.llx278.msgclient.protocol;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.util.CharsetUtil;

import java.util.Map;
import java.util.Set;

public class ContentParser {

    public static void parse(Map<String,String> headerMap, ByteBuf contentBuf) throws IllegalStateException {

        if (headerMap == null || contentBuf == null) {
            throw new IllegalArgumentException("parameter can not null");
        }
        ByteBuf buf = Unpooled.buffer();
        while (contentBuf.isReadable()) {
            byte ch = contentBuf.readByte();
            if (ch == '\r') {
                break;
            }
            buf.writeByte(ch);
        }

        String header = buf.toString(CharsetUtil.UTF_8);

        if (header != null) {
            String[] params = header.split("\n");
            for (String param : params) {
                String[] keyAndVal = param.split(":");
                headerMap.put(keyAndVal[0],keyAndVal[1]);
            }
        }
    }

    public static String toHeader(Map<String,String> headerMap) {

        if (headerMap == null || headerMap.isEmpty()) {
            return null;
        }

        StringBuilder sb = new StringBuilder();
        Set<Map.Entry<String, String>> entries = headerMap.entrySet();
        for (Map.Entry<String,String> entry : entries) {
            String key = entry.getKey();
            String value = entry.getValue();
            sb.append(key).append(":").append(value).append("\n");
        }
        return sb.deleteCharAt(sb.length() -1 ).append('\r').toString();
    }
}
