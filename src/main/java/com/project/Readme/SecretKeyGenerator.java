//package com.project.Readme;
//
//import java.security.SecureRandom;
//import java.util.Base64;
//
//public class SecretKeyGenerator {
//    public static void main(String[] args) {
//        SecureRandom random = new SecureRandom();
//        byte[] key = new byte[64]; // 64 bytes = 512 bits
//        random.nextBytes(key);
//        String base64Key = Base64.getEncoder().encodeToString(key);
//        System.out.println(base64Key);
//    }
//}
//
