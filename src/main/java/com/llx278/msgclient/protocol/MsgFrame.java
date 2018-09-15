package com.llx278.msgclient.protocol;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.util.CharsetUtil;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class MsgFrame extends BaseFrame<MsgFrame.Value> {
    public MsgFrame(int type, Value value) {
        super(type, value);
    }

    @Override
    public void writeToByteBuf(ByteBuf buf) {
        buf.writeInt(type);
        buf.writeInt(value.length());
        value.writeTo(buf);
    }

    @Override
    public String toString() {
        return "MsgFrame{" +
                "type=" + type +
                ", value=" + value +
                '}';
    }

    public static MsgFrame valueOf(ByteBuf buf) {
        int type = buf.readInt();
        int len = buf.readInt();
        Value value = Value.valueOf(buf);
        if (len != value.length()) {
            System.out.println("--------- invalid tlv frame - exp : " + len + " actual : " + value.length() + " type is " + type);
        }
        return new MsgFrame(type, value);
    }

    public static boolean isMsgFrame(ByteBuf buf) {
        int type = buf.getInt(0);
        return type == Type.FRAME_MSG;
    }

    public static class Value extends BaseFrame.BaseValue {

        /**
         * 一条消息最大的长度 1MB
         */
        public static final int MAX_LENGTH = 1024 * 1024;

        private final int mFromUid;
        private final int mToUid;
        private final byte[] mHeader;
        private final byte[] mBody;

        public Value(int fromUid, int toUid, Map<String, String> header, byte[] body) {
            mFromUid = fromUid;
            mToUid = toUid;
            mHeader = ContentParser.toHeader(header).getBytes(CharsetUtil.UTF_8);
            mBody = body;
        }

        public int getFromUid() {
            return mFromUid;
        }

        public int getToUid() {
            return mToUid;
        }

        public String getHeader() {
            return new String(mHeader,CharsetUtil.UTF_8);
        }

        public byte[] getBody() {
            return mBody;
        }

        public ByteBuf toByteBuf() {
            ByteBuf buf = Unpooled.buffer();
            buf.writeInt(mFromUid);
            buf.writeInt(mToUid);
            buf.writeBytes(mHeader);
            buf.writeByte('\r');
            buf.writeBytes(mBody);
            return buf;
        }

        @Override
        public int length() {
            return 4 + 4 + mHeader.length + 1 + mBody.length;
        }

        @Override
        public void writeTo(ByteBuf buf) {
            buf.writeInt(mFromUid);
            buf.writeInt(mToUid);
            buf.writeBytes(mHeader);
            buf.writeByte('\r');
            buf.writeBytes(mBody);
        }

        public static Value valueOf(ByteBuf buf) {
            int fromUid = buf.readInt();
            int toUid = buf.readInt();
            Map<String, String> header = new HashMap<>();
            ContentParser.parse(header, buf);
            byte[] body = new byte[buf.readableBytes()];
            buf.readBytes(body);
            return new Value(fromUid, toUid, header, body);
        }

        @Override
        public String toString() {
            return "Value{" +
                    "mFromUid=" + mFromUid +
                    ", mToUid=" + mToUid +
                    ", mHeader=" + new String(mHeader, CharsetUtil.UTF_8) +
                    ", mBody=" + new String(mBody, CharsetUtil.UTF_8) +
                    '}';
        }
    }

    public static class ContentParser {

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
            return sb.deleteCharAt(sb.length() -1 ).toString();
        }
    }
}
