package main.java.pt.tecnico.a01.cryptography;
import java.io.*;
import java.util.*;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.MessageDigest;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import javax.crypto.spec.SecretKeySpec;

import javax.crypto.Cipher;
import java.security.Key;
import java.security.KeyFactory;

import javax.crypto.KeyGenerator;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;


public class CryptoLibrary {

	// mapping of cryptographic algorithms and correspondent MediTrack field
	public static final String[] AES_FIELDS = {"name", "sex", "consultationRecords"};
	public static final String[] RSA_FIELDS = {"dateOfBirth", "bloodType", "knownAllergies"};

    // --------------------------------------------------------------------------------------------
    //  Main operations
    // --------------------------------------------------------------------------------------------

    public static void protect(String inputFile, String outputFile, Key serverPrivate, Key userPulic) throws Exception {

        // Read  MediTrack JSON object from file, and print its contents
        try (FileReader fileReader = new FileReader(inputFile)) {
            Gson gson = new Gson();
            JsonObject rootJson = gson.fromJson(fileReader, JsonObject.class);
            System.out.println("JSON object: " + rootJson);
            JsonObject patientObject = rootJson.get("patient").getAsJsonObject();
            JsonObject finalFileObject = new JsonObject();
            JsonObject encryptedFileObject = new JsonObject();

            // Add management of keys
            KeyGenerator keyGen = KeyGenerator.getInstance("AES");
		    		keyGen.init(128);
		    		Key key = keyGen.generateKey();
            for (String field: AES_FIELDS)
            {
                byte[] bytes = null;
                if (field.equals("consultationRecords")) {
                    bytes = gson.toJson(patientObject.get(field)).getBytes();
                }
                else {
                    bytes = patientObject.get(field).getAsString().getBytes();
                }
                byte[] encryptedBytes = aes_encrypt(bytes,key);
                String encryptedBase64 = Base64.getEncoder().encodeToString(encryptedBytes);
                encryptedFileObject.addProperty(field, encryptedBase64);
            }
            for (String field: RSA_FIELDS)
            {
                byte[] bytes = patientObject.get(field).getAsString().getBytes();
                byte[] encryptedBytes = rsa_encrypt_public(bytes,userPulic);
                String encryptedBase64 = Base64.getEncoder().encodeToString(encryptedBytes);
                encryptedFileObject.addProperty(field, encryptedBase64);
            }

            byte[] bytes = key.getEncoded();
            byte[] encryptedBytes = rsa_encrypt_public(bytes,userPulic);
            String encryptedBase64 = Base64.getEncoder().encodeToString(encryptedBytes);
            finalFileObject.addProperty("key", encryptedBase64);

            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(gson.toJson(encryptedFileObject).getBytes("UTF-8"));
            byte[] encryptedHash = rsa_encrypt_private(hash, serverPrivate);
            String hashBase64 = Base64.getEncoder().encodeToString(encryptedHash);
            finalFileObject.addProperty("hash", hashBase64);
            finalFileObject.add("payload", encryptedFileObject);

            try (FileWriter fileWriter = new FileWriter(outputFile)) {
                gson = new GsonBuilder().setPrettyPrinting().create();
                gson.toJson(finalFileObject, fileWriter);
            }
            // Save symmetric key as well
        }
    }


    public static void check(String inputFile, Key serverPublic, Key userPrivate) {

    }

    public static void unprotect(String inputFile, String outputFile, Key serverPublic, Key userPrivate) throws Exception{
        // Read  MediTrack JSON object from file, and print its contents
        try (FileReader fileReader = new FileReader(inputFile)) {
            Gson gson = new Gson();
            JsonObject rootJson = gson.fromJson(fileReader, JsonObject.class);
            System.out.println("JSON object: " + rootJson);
            JsonObject payloadObject = rootJson.get("payload").getAsJsonObject();
            JsonObject decryptedFileObject = new JsonObject();

            String base64 = rootJson.get("key").getAsString();
            byte[] encryptedKey = Base64.getDecoder().decode(base64);
            byte[] decryptedKey = rsa_decrypt(encryptedKey, userPrivate);
            Key key = new SecretKeySpec(decryptedKey, 0, decryptedKey.length, "AES");
            // Add management of keys
            for (String field: AES_FIELDS)
            {
                byte[] bytes = null;
                if (field.equals("consultationRecords")) {
                    bytes = gson.toJson(payloadObject.get(field)).getBytes();
                }
                else {
                    bytes = payloadObject.get(field).getAsString().getBytes();
                }
                byte[] decryptedBase64 = Base64.getDecoder().decode(bytes);
                byte[] decryptedBytes = aes_decrypt(decryptedBase64,key);
                decryptedFileObject.addProperty(field, new String(decryptedBytes));
            }
            for (String field: RSA_FIELDS)
            {
                byte[] bytes = payloadObject.get(field).getAsString().getBytes();
                byte[] decryptedBase64 = Base64.getDecoder().decode(bytes);
                byte[] decryptedBytes = rsa_decrypt(decryptedBase64, userPrivate);
                decryptedFileObject.addProperty(field, new String (decryptedBytes));
            }
        }
    }


    // --------------------------------------------------------------------------------------------
    //  Utilities
    // --------------------------------------------------------------------------------------------
    public static byte[] aes_encrypt(byte[] bytes, Key key) throws Exception{
        // cipher data
        final String CIPHER_ALGO = "AES/ECB/PKCS5Padding";
        System.out.println("Ciphering with " + CIPHER_ALGO + "...");
        Cipher cipher = Cipher.getInstance(CIPHER_ALGO);
        cipher.init(Cipher.ENCRYPT_MODE, key);
        byte[] cipherBytes = cipher.doFinal(bytes);
        return cipherBytes;
    }

    public static byte[] aes_decrypt(byte[] bytes, Key key) throws Exception{
        // cipher data
        final String CIPHER_ALGO = "AES/ECB/PKCS5Padding";
        System.out.println("Ciphering with " + CIPHER_ALGO + "...");
        Cipher cipher = Cipher.getInstance(CIPHER_ALGO);
        cipher.init(Cipher.DECRYPT_MODE, key);
        byte[] decipheredBytes = cipher.doFinal(bytes);
        return decipheredBytes;
    }

    public static byte[] rsa_encrypt_public(byte[] bytes, Key key) throws Exception{
        // cipher data
        final String CIPHER_ALGO = "RSA/ECB/PKCS1Padding";
        System.out.println("Ciphering with " + CIPHER_ALGO + "...");
        Cipher cipher = Cipher.getInstance(CIPHER_ALGO);
        cipher.init(Cipher.ENCRYPT_MODE, key);
        byte[] cipherBytes = cipher.doFinal(bytes);
        return cipherBytes;
    }

    public static byte[] rsa_encrypt_private(byte[] bytes, Key key) throws Exception{
        // cipher data
        final String CIPHER_ALGO = "RSA/ECB/PKCS1Padding";
        System.out.println("Ciphering with " + CIPHER_ALGO + "...");
        Cipher cipher = Cipher.getInstance(CIPHER_ALGO);
        cipher.init(Cipher.ENCRYPT_MODE, key);
        byte[] cipherBytes = cipher.doFinal(bytes);
        return cipherBytes;
    }

    public static byte[] rsa_decrypt(byte[] bytes, Key key) throws Exception{
        // cipher data
        final String CIPHER_ALGO = "RSA/ECB/PKCS1Padding";
        System.out.println("Ciphering with " + CIPHER_ALGO + "...");
        Cipher cipher = Cipher.getInstance(CIPHER_ALGO);
        cipher.init(Cipher.DECRYPT_MODE, key);
        byte[] decipheredBytes = cipher.doFinal(bytes);
        return decipheredBytes;
    }

    public static byte[] readFile(String filename) throws Exception {
        File file = new File(filename);
        FileInputStream fis = new FileInputStream(file);
        byte[] fileBytes = new byte[(int)file.length()];
        fis.read(fileBytes);
        fis.close();
        return fileBytes;
    }

    public static Key readPrivateKey(String filename) throws Exception {
        byte[] privEncoded = readFile(filename);
        PKCS8EncodedKeySpec privSpec = new PKCS8EncodedKeySpec(privEncoded);
        KeyFactory keyFacPriv = KeyFactory.getInstance("RSA");
        PrivateKey priv = keyFacPriv.generatePrivate(privSpec);
        return priv;
    }

    public static Key readPublicKey(String filename) throws Exception {
        System.out.println("Reading public key from file " + filename + " ...");
        byte[] pubEncoded = readFile(filename);
        X509EncodedKeySpec pubSpec = new X509EncodedKeySpec(pubEncoded);
        KeyFactory keyFacPub = KeyFactory.getInstance("RSA");
        PublicKey pub = keyFacPub.generatePublic(pubSpec);
        return pub;
    }

}
