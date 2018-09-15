package com.llx278.msgclient;

import com.llx278.msgclient.protocol.MsgFrame;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@RunWith(JUnit4.class)
public class ContentParserTest {

    @Before
    public void before() {

    }

    @After
    public void after() {

    }

    @Test
    public void parseTest() throws IOException {

        String content = "Content-Type:application/x-text\nExpanded-Name:.jped\nTestKey1:TestVal1\nTestKey2:TestVale\rThis is body";

        byte[] src = content.getBytes("utf-8");
        ByteBuf buf = Unpooled.buffer();
        buf.writeBytes(src);
        Map<String,String> headerMap = new HashMap<>();

        MsgFrame.ContentParser.parse(headerMap,buf);

        System.out.println(headerMap.toString());
    }

    @Test
    public void toHeaderTest() throws Exception  {

        Map<String,String> headerMap = new HashMap<>();
        headerMap.put("abc","def");
        headerMap.put("ddd","jkl");
        headerMap.put("mac","book pro");

        String res = MsgFrame.ContentParser.toHeader(headerMap);

        System.out.println(res);

    }

}
