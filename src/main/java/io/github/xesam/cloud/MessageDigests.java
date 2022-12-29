package io.github.xesam.cloud;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public final class MessageDigests {
    public static String SHA1(String message) {
        if (null == message || 0 == message.length()) {
            return null;
        }
        char[] hexDigits = {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f'};
        try {
            MessageDigest mdTemp = MessageDigest.getInstance("SHA1");
            mdTemp.update(message.getBytes(StandardCharsets.UTF_8));
            byte[] md = mdTemp.digest();
            int j = md.length;
            char[] buf = new char[j * 2];
            int k = 0;
            for (byte byte0 : md) {
                buf[k++] = hexDigits[byte0 >>> 4 & 0xf];
                buf[k++] = hexDigits[byte0 & 0xf];
            }
            return new String(buf);
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        return "";
    }

    public static String MD5(String message) {
        String md5Str = "";
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] md5Bytes = md.digest(message.getBytes());
            md5Str = bytes2Hex(md5Bytes);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return md5Str.toUpperCase();
    }

    private static String bytes2Hex(byte[] bytes) {
        StringBuilder result = new StringBuilder();
        int temp;
        try {
            for (byte aByte : bytes) {
                temp = aByte;
                if (temp < 0) {
                    temp += 256;
                }
                if (temp < 16) {
                    result.append("0");
                }
                result.append(Integer.toHexString(temp));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return result.toString();
    }
}
