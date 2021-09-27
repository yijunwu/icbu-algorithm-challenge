package cn.wuyijun;

/**
 * @(#)ByteTokenizerTest.java Sep 24, 2008
 * Copyright (C) 2008 Duy Do. All Rights Reserved.
 */

import java.nio.ByteBuffer;

/**
 * The class description here.
 *
 * @author <a href = "mailto:doquocduy@gmail.com">Duy Do</a>
 * @version Sep 24, 2008 2:21:54 PM
 */
public class ByteTokenizerTest {

    /**
     * @param args
     */
    public static void main(String[] args) {
        // bytes array to parse
        final byte[] bytes = new byte[] {
                (byte) 0x31, (byte) 0xC0, (byte) 0x80,
                (byte) 0x64, (byte) 0x75, (byte) 0x79, (byte) 0x64, (byte) 0xC0, (byte) 0x80,
                (byte) 0x39
        };
        // delimiters
        final byte[] delimiters = new byte[]{(byte) 0xC0, (byte) 0x80};

        System.out.println("Byte array to parse: " + getHexDump(bytes));

        // Create an instance of ByteTokenizer
        final ByteTokenizer tokenizer = new ByteTokenizer(bytes, (byte)0x80);

        // Count tokens
        final int tonkenNums = tokenizer.countTokens();
        System.out.println("Token numbers: " + tonkenNums);

        // Print all tokens
        ByteBuffer token;
        while(tokenizer.hasMoreTokens()) {
            token = tokenizer.nextToken();
            System.out.println("Byte token: " + getHexDump(token.array()));
        }
    }

    private static String getHexDump(byte[] bytes) {
        final StringBuffer sb = new StringBuffer();
        for(byte b : bytes){
            sb.append(Integer.toHexString(((int)b & 0xff))).append(" ");
        }
        return sb.toString();
    }
}