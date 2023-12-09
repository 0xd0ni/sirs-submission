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
    
    // we can share an instance and let methods reuse it
    public static Gson gson = new Gson();

    // 1 minute 
    private static final long FRESHNESS_RANGE = 60000;  

    private static final String PATIENT = "patient";
    private static final String CONSULTATION_RECORDS = "consultationRecords";
    
    
    private static final String RECORD = "record";
    private static final String METADATA = "metadata";
    private static final String HASH = "hash";
    private static final String KEY = "key";
    private static final String REFRESH_TOKEN = "refreshToken";
    private static final String KNOWN_ALLERGIES = "knownAllergies";
    

    private static final String MESSAGE_PREFIX_CHECK = "[MediTrack (check)]: ";
    private static final String UNALTERED = "unaltered";
    private static final String ALTERED = "altered";
    private static final String FRESH = "fresh";
    private static final String STALE = "stale";

    // --------------------------------------------------------------------------------------------
    //  Main operations
    // --------------------------------------------------------------------------------------------

    /**
     * Protects MediTrack records (sensitive data) by encrypting it using a combination of symmetric 
     * and asymmetric encryption techniques.
     * 
     * This method reads a JSON object from the specified input file and encrypts its contents. It generates a symmetric
     * encryption key (AES) to encrypt the core data. Additionally, it encrypts metadata related to the patient's record
     * using the provided public key. The encrypted data is then written to the specified output file.
     *
     * @param inputFile     The path of the input file containing the JSON object to be encrypted.
     * @param outputFile    The path of the output file where the encrypted JSON object will be saved.
     * @param serverPrivate The private key of the server, used in the encryption process.
     * @param userPublic    The public key of the user, used to encrypt the metadata.
     * @throws Exception    If any error occurs during file reading/writing or encryption processes.
     */
    public static void protect(String inputFile, String outputFile, Key serverPrivate, Key userPublic) throws Exception {

        JsonObject rootJson = readFileToJsonObject(inputFile);
        System.out.println("JSON object: " + rootJson);
        
        JsonObject protectedRecord = new JsonObject();

        // generates a symmetric encryption key 
        Key key = generateKeyAES();

        // encrypts the core data format
        JsonObject record = encryptRecord(rootJson.get(PATIENT).getAsJsonObject(), key, userPublic);
        // computes and encrypts the metadata linked to the patient's record - (core data format)
        JsonObject metadata = encryptMetadata(key, userPublic, serverPrivate, record);
     
        protectedRecord.add(RECORD,record);
        protectedRecord.add(METADATA,metadata);
        
        writeJsonObjectToFile(protectedRecord,outputFile);
        
    }


    public static void unprotect(String inputFile, String outputFile, Key userPrivate) throws Exception {
        
        JsonObject rootJson = readFileToJsonObject(inputFile);
        System.out.println("JSON object: " + rootJson);

        JsonObject patientObject = new JsonObject();
        JsonObject recordObject = rootJson.get(RECORD).getAsJsonObject();

        // explain:
        String keyBase64 =  rootJson.get(METADATA).getAsJsonObject().get(KEY).getAsString();

        // get the core data format decrypted 
        JsonObject decryptedRecord = getDecryptedMediTrackRecord(keyBase64,recordObject,userPrivate);
        patientObject.add(PATIENT, decryptedRecord);

        writeJsonObjectToFile(patientObject, outputFile);
    }


    public static void check(String inputFile, Key serverPrivate, Key userPrivate) throws Exception {

        JsonObject rootJson = readFileToJsonObject(inputFile);

        JsonObject recordObject = rootJson.get(RECORD).getAsJsonObject();

        String storedHashBase64 = rootJson.get(METADATA).getAsJsonObject().get(HASH).getAsString();
        String computedHashBase64 = digestAndBase64(recordObject, serverPrivate);
        String refreshTokenBase64 = rootJson.get(METADATA).getAsJsonObject().get(REFRESH_TOKEN).getAsString();
        String refreshToken =  getRefreshToken(refreshTokenBase64, userPrivate);
        
        boolean integrityStatus = compareBase64Hashes(storedHashBase64, computedHashBase64);
        boolean freshnessStatus = compareRefreshTokenInterval(refreshToken,FRESHNESS_RANGE);

        String statusMessage = String.format("%sstatus= `%s` - `%s`",
            MESSAGE_PREFIX_CHECK, integrityStatus ? UNALTERED : ALTERED, freshnessStatus ? FRESH : STALE);

        System.out.println(statusMessage);  
    
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

    /**
     * Generate a symmetric encryption key using the AES algorithm.
     * 
     * This method creates a 128-bit AES key. AES (Advanced Encryption Standard) 
     *  The generated key can be used for encrypting and decrypting data.
     *
     * @return             An AES encryption key
     * @throws Exception   If a key generation error occurs.
     */
    public static Key generateKeyAES() throws Exception {
        // Create a KeyGenerator instance for the AES encryption algorithm
        KeyGenerator keyGen = KeyGenerator.getInstance("AES");

        // Initialize the KeyGenerator with a key size of 128 bits
        keyGen.init(128);

        // Generate and return the AES key
        Key key = keyGen.generateKey();
        return key;
    }

    // --------------------------------------------------------------------------------------------
    //  Utilities - freshness
    // --------------------------------------------------------------------------------------------

    public static boolean compareRefreshTokenInterval(String refreshToken, long range) {
        Instant refreshTokenInstant = Instant.parse(refreshToken);
        Instant current = Instant.now();
       
        if(refreshTokenInstant.equals(current)) {
            return true;
        } 
        boolean isWithinBeforeRange = (refreshTokenInstant.isAfter(current.minusMillis(range))
                                       && refreshTokenInstant.isBefore(current));
        boolean isWithinAfterRange = (refreshTokenInstant.isAfter(current) 
                                      && refreshTokenInstant.isBefore(current.plusMillis(range)));
        
        return isWithinBeforeRange || isWithinAfterRange;
    }
 
    
    public static String getRefreshToken(String freshnessEncoded,Key userPrivate) throws Exception {
        byte[] decodedRefreshToken = Base64.getDecoder().decode(freshnessEncoded); 
        byte[] unencryptedRefreshToken = rsa_decrypt(decodedRefreshToken, userPrivate);
        String refreshToken = new String(unencryptedRefreshToken);

        return refreshToken;

    }

    // --------------------------------------------------------------------------------------------
    //  Utilities - hashes
    // --------------------------------------------------------------------------------------------

    public static boolean compareBase64Hashes(String base64Hash1, String base64Hash2) {
        byte[] decodedHash1 = Base64.getDecoder().decode(base64Hash1);
        byte[] decodedHash2 = Base64.getDecoder().decode(base64Hash2);

        return MessageDigest.isEqual(decodedHash1, decodedHash2);
    }


    public static String digestAndBase64(JsonObject recordObject,Key serverPrivate) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] hash = digest.digest(gson.toJson(recordObject).getBytes("UTF-8"));
        byte[] encryptedHash = rsa_encrypt_private(hash, serverPrivate);
        String hashBase64 = Base64.getEncoder().encodeToString(encryptedHash);

        return hashBase64;
    }

    // --------------------------------------------------------------------------------------------
    //  Utilities - misc
    // --------------------------------------------------------------------------------------------

    public static void writeJsonObjectToFile(JsonObject jsonObject, String outputFile) throws Exception {

        try (FileWriter fileWriter = new FileWriter(outputFile)) {
            gson = new GsonBuilder().setPrettyPrinting().create();
            gson.toJson(jsonObject, fileWriter);
        }
    }
    public static JsonObject readFileToJsonObject(String inputFile) throws Exception  {

        try (FileReader fileReader = new FileReader(inputFile)) {
            return gson.fromJson(fileReader, JsonObject.class);
        }
    }

    /**
     * Encrypts specified fields of a patient's record using AES and RSA encryption.
     *
     * @param patient     The patient's record (the core data handled) in JSON format.
     * @param key         The AES key for encryption.
     * @param userPublic  The public RSA key for encryption.
     * @return            The encrypted patient record.
     * @throws Exception  If an encryption error occurs.
     */
    public static JsonObject encryptRecord(JsonObject patient, Key key, Key userPublic) throws Exception {

        JsonObject encryptedRecord = new JsonObject();
        // Encrypt fields using AES
        encryptFields(patient, encryptedRecord, AES_FIELDS, key, true);
    
        // Encrypt fields using RSA
        encryptFields(patient, encryptedRecord, RSA_FIELDS, userPublic, false);

        return encryptedRecord;
    }

    /**
     * Encrypts fields of a JSON object using AES or RSA (when appropriate).
     *
     * @param patientObject   JsonObject containing data to encrypt.
     * @param encryptedRecord JsonObject to store encrypted data.
     * @param fields          Array of field names to be encrypted.
     * @param key             Encryption key.
     * @param useAes          Flag to determine encryption type (AES if true, RSA if false).
     * @throws Exception      If an encryption error occurs.
     */
    private static void encryptFields(JsonObject patientObject, JsonObject encryptedRecord, String[] fields, Key key, boolean useAes) throws Exception {

        for (String field : fields) 
        {
            byte[] bytes;
            if (field.equals(CONSULTATION_RECORDS) || field.equals(KNOWN_ALLERGIES)) {
                JsonArray jsonArray = patientObject.get(field).getAsJsonArray();
                bytes = jsonArray.toString().getBytes();
            } else {
                bytes = patientObject.get(field).getAsString().getBytes();
            }
            byte[] encryptedBytes = useAes ? aes_encrypt(bytes, key) : rsa_encrypt_public(bytes, key);
            String encryptedBase64 = Base64.getEncoder().encodeToString(encryptedBytes);
            encryptedRecord.addProperty(field, encryptedBase64);
        }
    }

    /**
     * Computes and Encrypts Metadata Linked to the Patient's Record
     *
     * @param key            Encryption key.
     * @param userPublic     The public RSA key for encryption.
     * @param useAes         Flag to determine encryption type (AES if true, RSA if false).
     * @return               The encrypted metadata.
     * @throws Exception     If an encryption error occurs.
     */
    public static JsonObject encryptMetadata(Key key, Key userPublic, Key serverPrivate, JsonObject encryptedRecord) throws Exception {

        JsonObject metadata = new JsonObject();
        
        byte[] bytes = key.getEncoded();
        byte[] encryptedBytes = rsa_encrypt_public(bytes,userPublic);
        String encryptedBase64 = Base64.getEncoder().encodeToString(encryptedBytes);
        metadata.addProperty(KEY, encryptedBase64);

        byte[] freshnessBytes = Instant.now().toString().getBytes();
        byte[] encryptedFreshness = rsa_encrypt_public(freshnessBytes, userPublic);
        String freshnessEncoded = Base64.getEncoder().encodeToString(encryptedFreshness);
        metadata.addProperty(REFRESH_TOKEN, freshnessEncoded);

        String hashBase64 = digestAndBase64(encryptedRecord, serverPrivate);
        metadata.addProperty(HASH, hashBase64);

        return metadata;
    }

    public static JsonObject getDecryptedMediTrackRecord(String keyBase64, JsonObject recordObject, Key userPrivate) throws Exception {

            JsonObject decryptedMediTRackRecordObject = new JsonObject();

            System.out.println("[MediTrack]: getDecryptedMediTrackRecord");
            System.out.println("metadata - key " + keyBase64);
            byte[] encryptedKey = Base64.getDecoder().decode(keyBase64);
            byte[] decryptedKey = rsa_decrypt(encryptedKey, userPrivate);
            Key key = new SecretKeySpec(decryptedKey, 0, decryptedKey.length, "AES");

            for (String field: AES_FIELDS_S)
            {
                byte[] bytes = recordObject.get(field).getAsString().getBytes();
                byte[] decryptedBase64 = Base64.getDecoder().decode(bytes);
                byte[] decryptedBytes = aes_decrypt(decryptedBase64,key);
                decryptedMediTRackRecordObject.addProperty(field, new String(decryptedBytes));

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
                    decryptedMediTRackRecordObject.add(field,gson.toJsonTree(knownAllergies));

                } else {
                    decryptedMediTRackRecordObject.addProperty(field, new String (decryptedBytes));
                }
            }

            // workaround such that the order of the unprotected record matches the original
            // TODO: simplify later ; or find a better approach
            byte[] bytes = recordObject.get("consultationRecords").getAsString().getBytes();
            byte[] decryptedBase64 = Base64.getDecoder().decode(bytes);
            byte[] decryptedBytes = aes_decrypt(decryptedBase64,key);
            Type listType = new TypeToken<List<JsonObject>>() {}.getType();
            List<String> consultationRecords = gson.fromJson(new String(decryptedBytes), listType);
            decryptedMediTRackRecordObject.add("consultationRecords",gson.toJsonTree(consultationRecords));

            return decryptedMediTRackRecordObject;

    }
    

}
