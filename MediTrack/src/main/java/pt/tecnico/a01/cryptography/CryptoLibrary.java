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
import java.time.Instant;
import java.lang.reflect.Type;

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
import com.google.gson.reflect.TypeToken;



public class CryptoLibrary {



	// mapping of cryptographic algorithms and correspondent MediTrack field
	public static final String[] AES_FIELDS = {"name", "sex", "consultationRecords"};
	public static final String[] RSA_FIELDS = {"dateOfBirth", "bloodType", "knownAllergies"};
    
    // TODO:
    // to remove later (a rather small workaround)
    public static final String[] AES_FIELDS_S = {"name", "sex"};

    // it is not necessary to create a new Gson instance for each operation
    // we can share an instance and let methods reuse it  
    public static Gson gson = new Gson();

    // --------------------------------------------------------------------------------------------
    //  Main operations
    // --------------------------------------------------------------------------------------------

    public static void protect(String inputFile, String outputFile, Key serverPrivate, Key userPulic) throws Exception {

        try (FileReader fileReader = new FileReader(inputFile)) {
            JsonObject rootJson = gson.fromJson(fileReader, JsonObject.class);
            System.out.println("JSON object: " + rootJson);
            JsonObject patientObject = rootJson.get("patient").getAsJsonObject();
            JsonObject finalFileObject = new JsonObject();
            JsonObject encryptedFileObject = new JsonObject();
            JsonObject metadataObject = new JsonObject();
            
            KeyGenerator keyGen = KeyGenerator.getInstance("AES");
		    keyGen.init(128);
		    Key key = keyGen.generateKey();
            for (String field: AES_FIELDS)
            {
                byte[] bytes = null;
                if (field.equals("consultationRecords")) {
                    JsonArray consultationRecordsArray = patientObject.get(field).getAsJsonArray();
                    bytes = consultationRecordsArray.toString().getBytes();
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
                byte[] bytes = null;

                if(field.equals("knownAllergies")) {
                    JsonArray knownAllergiesArray = patientObject.get(field).getAsJsonArray();
                    bytes = knownAllergiesArray.toString().getBytes();
            
                }
                else {
                    bytes = patientObject.get(field).getAsString().getBytes();
                }
                byte[] encryptedBytes = rsa_encrypt_public(bytes,userPulic);
                String encryptedBase64 = Base64.getEncoder().encodeToString(encryptedBytes);
                encryptedFileObject.addProperty(field, encryptedBase64);
                    
            }
            
            byte[] bytes = key.getEncoded();
            byte[] encryptedBytes = rsa_encrypt_public(bytes,userPulic);
            String encryptedBase64 = Base64.getEncoder().encodeToString(encryptedBytes);
            metadataObject.addProperty("key", encryptedBase64);

            byte[] freshnessBytes = Instant.now().toString().getBytes();
            byte[] encryptedFreshness = rsa_encrypt_private(freshnessBytes, serverPrivate);
            String freshnessEncoded = Base64.getEncoder().encodeToString(encryptedFreshness);
            metadataObject.addProperty("refreshToken", freshnessEncoded);

            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(gson.toJson(encryptedFileObject).getBytes("UTF-8"));
            byte[] encryptedHash = rsa_encrypt_private(hash, serverPrivate);
            String hashBase64 = Base64.getEncoder().encodeToString(encryptedHash);
            metadataObject.addProperty("hash", hashBase64);


            finalFileObject.add("record", encryptedFileObject);
            finalFileObject.add("metadata", metadataObject);

            try (FileWriter fileWriter = new FileWriter(outputFile)) {
                gson = new GsonBuilder().setPrettyPrinting().create();
                gson.toJson(finalFileObject, fileWriter);
            }
            // Save symmetric key as well
        }
    }


    public static void unprotect(String inputFile, String outputFile, Key serverPublic, Key userPrivate) throws Exception{
        
        try (FileReader fileReader = new FileReader(inputFile)) {
            JsonObject rootJson = gson.fromJson(fileReader, JsonObject.class);
            System.out.println("JSON object: " + rootJson);

            // get both the patient record and metadata as json objects
            JsonObject recordObject = rootJson.get("record").getAsJsonObject();
            JsonObject metadataObject = rootJson.get("metadata").getAsJsonObject();
            

            JsonObject decryptedFileObject = new JsonObject();
            JsonObject patientObject = new JsonObject();
          
            String base64 = metadataObject.get("key").getAsString();
            System.out.println("metadata - key " + base64 );
            byte[] encryptedKey = Base64.getDecoder().decode(base64);
            byte[] decryptedKey = rsa_decrypt(encryptedKey, userPrivate);
            Key key = new SecretKeySpec(decryptedKey, 0, decryptedKey.length, "AES");
           
        
            for (String field: AES_FIELDS_S)
            {
                byte[] bytes = recordObject.get(field).getAsString().getBytes();
                byte[] decryptedBase64 = Base64.getDecoder().decode(bytes);
                byte[] decryptedBytes = aes_decrypt(decryptedBase64,key);
                decryptedFileObject.addProperty(field, new String(decryptedBytes));
                 
            } 
            for (String field: RSA_FIELDS)
            {
                byte[] bytes = recordObject.get(field).getAsString().getBytes();
                byte[] decryptedBase64 = Base64.getDecoder().decode(bytes);
                byte[] decryptedBytes = rsa_decrypt(decryptedBase64, userPrivate);
                if (field.equals("knownAllergies")) {
                    // TODO: wrap in a function call ?
                    Type listType = new TypeToken<List<String>>() {}.getType();
                    List<String> knownAllergies = gson.fromJson(new String(decryptedBytes), listType);
                    decryptedFileObject.add(field,gson.toJsonTree(knownAllergies));

                } else {
                    decryptedFileObject.addProperty(field, new String (decryptedBytes));
                }    
            }

            // workaround such that the order of the unprotected record matches the original
            // TODO: simplify later ; or find a better approach
            byte[] bytes = recordObject.get("consultationRecords").getAsString().getBytes();
            byte[] decryptedBase64 = Base64.getDecoder().decode(bytes);
            byte[] decryptedBytes = aes_decrypt(decryptedBase64,key);
            Type listType = new TypeToken<List<JsonObject>>() {}.getType();
            List<String> consultationRecords = gson.fromJson(new String(decryptedBytes), listType);
            decryptedFileObject.add("consultationRecords",gson.toJsonTree(consultationRecords));
              
            patientObject.add("patient",decryptedFileObject);
            try (FileWriter fileWriter = new FileWriter(outputFile)) {
                gson = new GsonBuilder().setPrettyPrinting().create();
                gson.toJson(patientObject, fileWriter);
            }
            
        }
    }



    public static void check(String inputFile, Key serverPublic, Key userPrivate) throws Exception {

        try (FileReader fileReader = new FileReader(inputFile)) {


            JsonObject rootJson = gson.fromJson(fileReader, JsonObject.class);
            System.out.println("JSON object: " + rootJson);

            JsonObject recordObject = rootJson.get("record").getAsJsonObject();
            String hash = rootJson.get("metadata").getAsJsonObject().get("hash").getAsString();


            System.out.println("Record Object - " +  recordObject);
            System.out.println("Encrypted Hash - " + hash);

            // aNOTE:
            // We're assuming the inputFile is a secured MediTrack record

            /* {
            "record": {
                "name": "",
                "sex": "",
                "dateOfBirth": "",
                "bloodType": "",
                "knownAllergies": ""
            },
            "metadata": {
                "key": "",
                "refreshToken": "",x0
                "hash" : ""
            }
            } */
            

            // we need a function that verifies the hash in order to guarantee data integrity
            // we want to make sure that the MediTrack patient record was not altered by a non-authorized 
            // party
            
            // - first approach ?
            // how can we do that 
            // we need the hash 
            // we need to get the record to its original state and calculate the hash 
            // compare the stored hash with the existing hash



             // we need a function that verifies whether or not the refreshToken is fresh 
             // let's say that we want to compare the refreshToken with a pre determined 
             // time range
             // if the refreshToken is within the pre determined time range
             // the record is fresh 
             // -- ? 
             // the record is not fresh

        
        }

    }


    // --------------------------------------------------------------------------------------------
    //  Utilities
    // --------------------------------------------------------------------------------------------

    // TODO: freshness Utilities

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
