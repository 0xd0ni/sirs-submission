package main.java.pt.tecnico.a01.cryptography;
import java.io.*;
import java.util.*;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.MessageDigest;
import javax.crypto.Cipher;
import java.security.Key;
import javax.crypto.KeyGenerator;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;


public class CryptoLibrary {


    // --------------------------------------------------------------------------------------------
    //  Main operations
    // --------------------------------------------------------------------------------------------
    
    public static void protect(String file) throws Exception{
        final String filename = file;
        // Read  MediTrack JSON object from file, and print its contents
        try (FileReader fileReader = new FileReader(filename)) {
            Gson gson = new Gson();
            JsonObject rootJson = gson.fromJson(fileReader, JsonObject.class);
            System.out.println("JSON object: " + rootJson);
            JsonObject patientObject = rootJson.get("patient").getAsJsonObject();
            JsonObject finalFileObject = new JsonObject();
            JsonObject encryptedFileObject = new JsonObject();
            String[] aes_fields = {"name","sex", "consultationRecords"};
            String[] rsa_fields = {"dateOfBirth","bloodType","knownAllergies"};
            // Add management of keys
            for (String field: aes_fields)
            {   
                byte[] bytes = null;
                if (field.equals("consultationRecords")) {
                    bytes = gson.toJson(patientObject.get(field)).getBytes();
                }
                else {
                    bytes = patientObject.get(field).getAsString().getBytes();
                }
                byte[] encryptedBytes = aes_encrypt(bytes);
                String encryptedBase64 = Base64.getEncoder().encodeToString(encryptedBytes);
                encryptedFileObject.addProperty(field, encryptedBase64);
            }
            for (String field: rsa_fields)
            {   
                byte[] bytes = patientObject.get(field).getAsString().getBytes();
                byte[] encryptedBytes = rsa_encrypt(bytes);
                String encryptedBase64 = Base64.getEncoder().encodeToString(encryptedBytes);
                encryptedFileObject.addProperty(field, encryptedBase64);
            }
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(gson.toJson(encryptedFileObject).getBytes("UTF-8"));

            KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
            keyGen.initialize(2048);
            // Server's key
            KeyPair keyPair = keyGen.generateKeyPair();
            byte[] encryptedHash = rsa_encrypt(hash, keyPair.getPrivate());
            String hashBase64 = Base64.getEncoder().encodeToString(encryptedHash);
            finalFileObject.addProperty("hash", hashBase64);
            // Save symmetric key as well
            System.out.println(gson.toJson(finalFileObject));
        }
    }


    public static void check() {

    }

    public static void unprotect() {

    }


    // --------------------------------------------------------------------------------------------
    //  Utilities
    // --------------------------------------------------------------------------------------------
    public static byte[] aes_encrypt(byte[] bytes) throws Exception{
        KeyGenerator keyGen = KeyGenerator.getInstance("AES");
		keyGen.init(128);
		Key key = keyGen.generateKey();
        // cipher data
        final String CIPHER_ALGO = "AES/ECB/PKCS5Padding";
        System.out.println("Ciphering with " + CIPHER_ALGO + "...");
        Cipher cipher = Cipher.getInstance(CIPHER_ALGO);
        cipher.init(Cipher.ENCRYPT_MODE, key);
        byte[] cipherBytes = cipher.doFinal(bytes);
        return cipherBytes;
    }

    public static byte[] rsa_encrypt(byte[] bytes) throws Exception{
        KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
		keyGen.initialize(2048);
		KeyPair keyPair = keyGen.generateKeyPair();
        // cipher data
        final String CIPHER_ALGO = "RSA/ECB/PKCS1Padding";
        System.out.println("Ciphering with " + CIPHER_ALGO + "...");
        Cipher cipher = Cipher.getInstance(CIPHER_ALGO);
        cipher.init(Cipher.ENCRYPT_MODE, keyPair.getPublic());
        byte[] cipherBytes = cipher.doFinal(bytes);
        return cipherBytes;
    }

    public static byte[] rsa_encrypt(byte[] bytes, Key key) throws Exception{
        // cipher data
        final String CIPHER_ALGO = "RSA/ECB/PKCS1Padding";
        System.out.println("Ciphering with " + CIPHER_ALGO + "...");
        Cipher cipher = Cipher.getInstance(CIPHER_ALGO);
        cipher.init(Cipher.ENCRYPT_MODE, key);
        byte[] cipherBytes = cipher.doFinal(bytes);
        return cipherBytes;
    }

























}