package com.voyah.lib_base.mvp.utils;

import android.util.Base64;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Random;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;

/**
 * @Author ：xians.yang
 * @Date ：created in 2019/03/13 17:30
 * @Description ：AESUtil
 * @Modified_By ：
 * @Version : 1.0
 */
public class AESUtils {
    /**
     * 生成随机串(包含 字母+数字)
     * @param pwd_len // 随机数的长度
     * @return 字符串
     */
    public static String genRandomNum(int pwd_len) {
        final int maxNum = 10;
        int i;
        int count = 0;
        char[] str = { 'a', 'b', 'c', 'd', 'e', 'f', 'g', 'h', 'i', 'j', 'k', 'l', 'm', 'n', 'o', 'p', 'q', 'r', 's', 't', 'u', 'v', 'w', 'x', 'z', '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', };
        StringBuffer pwd = new StringBuffer();
        Random r = new Random();
        while (count < pwd_len) {
            i = Math.abs(r.nextInt(str.length-1));

            if (i >= 0 && i < str.length) {
                pwd.append(str[i]);
                count++;
            }
        }
        return pwd.toString();
    }

    /**
     * 密钥算法
     */
    private static final String ALGORITHM = "AES";
    /**
     * 加解密算法/工作模式/填充方式
     */
    private static final String ALGORITHM_STR = "AES/ECB/PKCS5Padding";

    private static final String ALGORITHM_MD5 = "md5";

    private static final String CHARSET = "UTF-8";


    /**
     * AES加密
     *
     * @param data
     * @return
     * @throws Exception
     */
    public static String encrypt(String data, String hexKey){
        try {
            Cipher cipher = Cipher.getInstance(ALGORITHM_STR);
            SecretKeySpec key = new SecretKeySpec(hexKey.getBytes(), ALGORITHM);
            cipher.init(Cipher.ENCRYPT_MODE, key);
            return Base64.encodeToString(cipher.doFinal(data.getBytes()), Base64.NO_WRAP).replaceAll("\r\n", "");
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "";
    }

    /**
     * AES解密
     *
     * @param base64Data
     * @return
     * @throws Exception
     */
    public static String decrypt(String base64Data, String hexKey){
        try {
            Cipher cipher = Cipher.getInstance(ALGORITHM_STR);
            SecretKeySpec key = new SecretKeySpec(hexKey.getBytes(), ALGORITHM);
            cipher.init(Cipher.DECRYPT_MODE, key);
            return new String(cipher.doFinal(Base64.decode(base64Data, Base64.DEFAULT)));
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "";
    }

    /**
     * 将16进制转换为二进制
     *
     * @param hexStr
     * @return
     */
    public static byte[] parseHexStr2Byte(String hexStr) {
        if (hexStr.length() < 1) {
            return null;
        }
        byte[] result = new byte[hexStr.length() / 2];
        for (int i = 0; i < hexStr.length() / 2; i++) {
            int high = Integer.parseInt(hexStr.substring(i * 2, i * 2 + 1), 16);
            int low = Integer.parseInt(hexStr.substring(i * 2 + 1, i * 2 + 2), 16);
            result[i] = (byte) (high * 16 + low);
        }
        return result;
    }

    /**
     * 2进制转化为16进制字符串
     *
     * @param buf
     * @return
     */
    public static String byteToHexString(byte[] buf) {
        StringBuffer sb = new StringBuffer();
        for (int i = 0; i < buf.length; i++) {
            String hex = Integer.toHexString(buf[i] & 0xFF);
            if (hex.length() == 1) {
                hex = '0' + hex;
            }
            sb.append(hex.toUpperCase());
        }
        return sb.toString();
    }


}
