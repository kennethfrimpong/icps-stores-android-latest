package com.icpsltd.stores.utils;

import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class Security {

    public String createMD5Hash(String stringToHash) throws NoSuchAlgorithmException {
        MessageDigest messageDigest = MessageDigest.getInstance("MD5");
        messageDigest.update(stringToHash.getBytes());
        byte[] bytes = messageDigest.digest();
        BigInteger bigInteger = new BigInteger(1,bytes);
        return bigInteger.toString(16);

    }
}
