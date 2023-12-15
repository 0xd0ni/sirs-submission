package main.java.pt.tecnico.a01.cryptography;

import java.io.*;
import java.util.*;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.MessageDigest;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.NoSuchAlgorithmException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.security.SecureRandom;

import java.time.Instant;
import java.lang.reflect.Type;

import javax.crypto.spec.SecretKeySpec;

import javax.crypto.Cipher;
import javax.crypto.NoSuchPaddingException;
import java.security.Key;
import java.security.KeyFactory;

import javax.crypto.spec.IvParameterSpec;
import javax.crypto.KeyGenerator;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;



public class CryptoLibrary {

	// main fields of a patient's medical record
	public static final String[] FIELDS = {"name", "sex","dateOfBirth", "bloodType", "knownAllergies",
                                           "consultationRecords"};
    public static final String[] AES_FIELDS_ORDERING = {"name", "sex"};
    public static final String[] AES_FIELD_ORDERING = {"consultationRecords"};
    
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
    
    private static final String MESSAGE_JSON_OBJECT = "JSON object: ";
    private static final String MESSAGE_READ_PUBLIC_KEY = "Reading public key from file ";
    private static final String MESSAGE_READ_PRIVATE_KEY = "Reading private key from file ";
    private static final String MESSAGE_PREFIX_CHECK = "[MediTrack (check)]: ";
    private static final String UNALTERED = "unaltered";
    private static final String ALTERED = "altered";
    private static final String FRESH = "fresh";
    private static final String STALE = "stale";

    private static final String INITIALIZATION_VECTOR = "iv";
    private static final String KEYS = "keys";
    private static final String MESSAGE_CIPHER = "Ciphering with ";
    private static final String MESSAGE_DECIPHER = "Deciphering with ";
    private static final String ALGORITHM_AES = "AES";
    private static final String CIPHER_ALGO_AES = "AES/CBC/PKCS5Padding";
    private static final String ALGORITHM_RSA = "RSA";
    private static final String CIPHER_ALGO_RSA = "RSA/ECB/PKCS1Padding";
    
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
     * @param userPublic    The public key of the user, used to encrypt the core data format and part of the metadata.
     * @throws Exception    If any error occurs during file reading/writing or encryption processes.
     */
    public static void protect(String inputFile, String outputFile, Key serverPrivate, Key userPublic, String... fields) 
        throws Exception {

        JsonObject rootJson = readFileToJsonObject(inputFile);
        System.out.println(MESSAGE_JSON_OBJECT + rootJson);
        
        JsonObject protectedRecord = new JsonObject();
        JsonObject metadata = new JsonObject();

        // encrypts the core data format
        JsonObject record = encryptRecord(rootJson.get(PATIENT).getAsJsonObject(), metadata, fields);
        // computes and encrypts the metadata linked to the patient's record - (core data format)
        encryptMetadata(metadata, userPublic, serverPrivate, record);

        protectedRecord.add(RECORD,record);
        protectedRecord.add(METADATA,metadata);
        
        writeJsonObjectToFile(protectedRecord,outputFile);
        
    }

    /**
     * Unprotects MediTrack records (sensitive data) by decrypting it using a combination of symmetric 
     * and asymmetric decryption techniques.
     * 
     * This method reads a JSON object from the specified input file and decrypts its contents.In essence, it obtains
     * the original 
     * data that was previously secured in `protect`.
     * The decrypted data is then written to the specified output file.
     *
     * @param inputFile     The path of the input file containing the JSON object to be decrypted.
     * @param outputFile    The path of the output file where the decrypted JSON object will be saved.
     * @param serverPrivate The private key of the server, used in the encryption process.
     * @param userPrivate   The private key of the user, used to decrypt the record
     * @throws Exception    If any error occurs during file reading/writing or encryption processes.
     */
    public static void unprotect(String inputFile, String outputFile, Key userPrivate, String... args) throws Exception {

        JsonObject rootJson = readFileToJsonObject(inputFile); 
        System.out.println(MESSAGE_JSON_OBJECT + rootJson);

        JsonObject patient = new JsonObject();

        // decrypts the secured document
        JsonObject unprotectedRecord = decryptRecord(rootJson.get(RECORD).getAsJsonObject(),
                                       rootJson.get(METADATA).getAsJsonObject().get(
                                       INITIALIZATION_VECTOR).getAsJsonObject(), 
                                       rootJson.get(METADATA).getAsJsonObject().get(
                                       KEYS).getAsJsonObject(), userPrivate, args);

        patient.add(PATIENT, unprotectedRecord);
        writeJsonObjectToFile(patient, outputFile);
    }

    /**
     * Checks MediTrack records (sensitive data) in order to verify the status of both the integrity and 
     * freshness protection
     * 
     * This method reads a JSON object from the specified input file and computes/compares the values associated 
     * to the digest and 
     * refreshToken.
     *
     * @param inputFile     The path of the input file containing the JSON object to be decrypted.
     * @param serverPrivate The private key of the server, used in the encryption process.
     * @param userPrivate   The private key of the user, used to decrypt the record
     * @throws Exception    If any error occurs during file reading/writing or encryption processes.
     */
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
    
    /**
     * Encrypts the given byte array using the AES encryption algorithm.
     * 
     * This method uses the AES encryption algorithm with ECB (Electronic Codebook) mode and PKCS5 padding scheme.
     * It initializes a Cipher instance with the specified Key in encryption mode and processes the input byte array
     * to produce the encrypted byte array.
     *
     *
     * @param  bytes     The byte array to be encrypted. This should not be null.
     * @param  key       The encryption key used for AES encryption. This should be a valid key for AES algorithm.
     * @param  iv        TODO:
     * @return           The encrypted byte array.
     * @throws Exception If any error occurs during the encryption process. 
     *                  
     * 
     */
    public static byte[] aesEncryptWithIV(byte[] bytes, Key key, IvParameterSpec iv) throws Exception{
        // cipher data
        System.out.println(MESSAGE_CIPHER + CIPHER_ALGO_AES + "...");
        Cipher cipher = Cipher.getInstance(CIPHER_ALGO_AES);
        cipher.init(Cipher.ENCRYPT_MODE, key, iv);
        byte[] cipherBytes = cipher.doFinal(bytes);
        return cipherBytes;
    }
    
    /**
     * Decrypts the given byte array using the AES decryption algorithm.
     * 
     * This method uses the AES decryption algorithm with ECB (Electronic Codebook) mode and PKCS5 padding scheme.
     * It initializes a Cipher instance with the specified Key in decryption mode and processes the input byte array
     * to produce the decrypted byte array.
     *
     *
     * @param  bytes     The byte array to be decrypted. This should not be null.
     * @param  key       The decryption key used for AES decryption.
     * @param iv         TODO:
     * @return           The decrypted byte array.
     * @throws Exception If any error occurs during the decryption process. 
     *                  
     * 
     */
    public static byte[] AesDecryptWithIV(byte[] bytes, Key key, byte[] iv) throws Exception{
        // decipher data
        System.out.println(MESSAGE_DECIPHER + CIPHER_ALGO_AES + "...");
        Cipher cipher = Cipher.getInstance(CIPHER_ALGO_AES);
        cipher.init(Cipher.DECRYPT_MODE, key, new IvParameterSpec(iv));
        byte[] decipheredBytes = cipher.doFinal(bytes);
        return decipheredBytes;
    }

    /**
     * Encrypts the given byte array using the RSA encryption algorithm.
     * 
     * This method uses the RSA encryption algorithm with ECB (Electronic Codebook) mode and PKCS5 padding scheme.
     * It initializes a Cipher instance with the specified Key in encryption mode and processes the input byte array
     * to produce the encrypted byte array.
     *
     *
     * @param  bytes     The byte array to be encrypted. This should not be null.
     * @param  key       The encryption key used for RSA encryption. 
     * @return           The encrypted byte array.
     * @throws Exception If any error occurs during the decryption process. 
     *                  
     * 
     */
    public static byte[] rsaEncrypt(byte[] bytes, Key key) throws Exception{
        // cipher data
        final String CIPHER_ALGO = "RSA/ECB/PKCS1Padding";
        System.out.println(MESSAGE_CIPHER + CIPHER_ALGO + "...");
        Cipher cipher = Cipher.getInstance(CIPHER_ALGO);
        cipher.init(Cipher.ENCRYPT_MODE, key);
        byte[] cipherBytes = cipher.doFinal(bytes);
        return cipherBytes;
    }

    /**
     * Decrypts the given byte array using the RSA decryption algorithm.
     * 
     * This method uses the RSA decryption algorithm with ECB (Electronic Codebook) mode and PKCS5 padding scheme.
     * It initializes a Cipher instance with the specified Key in decryption mode and processes the input byte array
     * to produce the encrypted byte array.
     *
     *
     * @param  bytes     The byte array to be decrypted. This should not be null.
     * @param  key       The decryption key used for RSA encryption. 
     * @return           The decrypted byte array.
     * @throws Exception If any error occurs during the decryption process. 
     *                  
     * 
     */
    public static byte[] rsaDecrypt(byte[] bytes, Key key) throws Exception{
        // cipher data
        System.out.println(MESSAGE_DECIPHER + CIPHER_ALGO_RSA + "...");
        Cipher cipher = Cipher.getInstance(CIPHER_ALGO_RSA);
        cipher.init(Cipher.DECRYPT_MODE, key);
        byte[] decipheredBytes = cipher.doFinal(bytes);
        return decipheredBytes;
    }

    /**
     * Encrypts the given byte array using the RSA encryption algorithm.
     * 
     * This method uses the RSA encryption algorithm with ECB (Electronic Codebook) mode and PKCS5 padding scheme.
     * It initializes a Cipher instance with the specified Key in encryption mode and processes the input byte array
     * to produce the encrypted byte array.
     *
     *
     * @param  bytes     The byte array to be encrypted. This should not be null.
     * @param  key       The encryption key used for RSA encryption. 
     * @param  iv        TODO:
     * @return           The encrypted byte array.
     * @throws Exception If any error occurs during the decryption process. 
     *                  
     * 
     */
    public static byte[] rsaEncryptWithIV(byte[] bytes, Key key, IvParameterSpec iv) throws Exception{
        // cipher data
        System.out.println(MESSAGE_CIPHER + CIPHER_ALGO_RSA + "...");
        Cipher cipher = Cipher.getInstance(CIPHER_ALGO_RSA);
        cipher.init(Cipher.ENCRYPT_MODE, key, iv);
        byte[] cipherBytes = cipher.doFinal(bytes);
        return cipherBytes;
    }

    /**
     * Decrypts the given byte array using the RSA decryption algorithm.
     * 
     * This method uses the RSA decryption algorithm with ECB (Electronic Codebook) mode and PKCS5 padding scheme.
     * It initializes a Cipher instance with the specified Key in decryption mode and processes the input byte array
     * to produce the encrypted byte array.
     *
     *
     * @param  bytes     The byte array to be decrypted. This should not be null.
     * @param  key       The decryption key used for RSA encryption. 
     * @param iv         TODO:
     * @return           The decrypted byte array.
     * @throws Exception If any error occurs during the decryption process. 
     *                  
     * 
     */
    public static byte[] rsaDecryptWithIV(byte[] bytes, Key key, byte[] iv) throws Exception{
        // cipher data
        System.out.println(MESSAGE_DECIPHER + CIPHER_ALGO_RSA + "...");
        Cipher cipher = Cipher.getInstance(CIPHER_ALGO_RSA);
        cipher.init(Cipher.DECRYPT_MODE, key, new IvParameterSpec(iv));
        byte[] decipheredBytes = cipher.doFinal(bytes);
        return decipheredBytes;
    }

     
    /** 
     * Reads the given file to a byte array.
     * 
     * @param  filename      The path of the input file to be read to a byte array       
     * @return               The byte array containing the data read
     * @throws Exception     If any I/O error occurs.
     */
    public static byte[] readFile(String filename) throws Exception {
        File file = new File(filename);
        FileInputStream fis = new FileInputStream(file);
        byte[] fileBytes = new byte[(int)file.length()];
        fis.read(fileBytes);
        fis.close();
        return fileBytes;
    }

    
    /** 
     * Reads the given file and generate the associated private key.
     * 
     * @param  filename      The path of the input file (key) to be read      
     * @return               A valid public key.
     * @throws Exception     If any I/O or key generation error occurs.
     */
    public static Key readPrivateKey(String filename) throws Exception {
        System.out.println(MESSAGE_READ_PRIVATE_KEY + filename + " ...");
        byte[] privEncoded = readFile(filename);
        PKCS8EncodedKeySpec privSpec = new PKCS8EncodedKeySpec(privEncoded);
        KeyFactory keyFacPriv = KeyFactory.getInstance(ALGORITHM_RSA);
        PrivateKey priv = keyFacPriv.generatePrivate(privSpec);
        return priv;
    }

    /** 
     * Reads the given file and generate the associated public key.
     * 
     * @param  filename      The path of the input file to be read to a byte array       
     * @return               A valid private key.
     * @throws Exception     If any I/O or key generation error occurs.
     */
    public static Key readPublicKey(String filename) throws Exception {
        System.out.println(MESSAGE_READ_PUBLIC_KEY + filename + " ...");
        byte[] pubEncoded = readFile(filename);
        X509EncodedKeySpec pubSpec = new X509EncodedKeySpec(pubEncoded);
        KeyFactory keyFacPub = KeyFactory.getInstance(ALGORITHM_RSA);
        PublicKey pub = keyFacPub.generatePublic(pubSpec);
        return pub;
    }

    /**
     * Generates a symmetric encryption key using the AES algorithm.
     * 
     * This method creates a 128-bit AES key. AES (Advanced Encryption Standard) 
     * The generated key can be used for encrypting and decrypting data.
     *
     * @return             An AES encryption key
     * @throws Exception   If a key generation error occurs.
     */
    public static Key generateKeyAES() throws Exception {
        // Create a KeyGenerator instance for the AES encryption algorithm
        KeyGenerator keyGen = KeyGenerator.getInstance(ALGORITHM_AES);


        // DEBUG: 128 bits - 16bytes I want a 16 bytes keys not a 28 bytes
        // Initialize the KeyGenerator with a key size of 128 bits
        keyGen.init(128);

        // Generate and return the AES key
        Key key = keyGen.generateKey();
        return key;
    }

    /**
     * TODO:
     * 
     * @param algorithm        
     * @return            
     * @throws Exception   
     */
    public static IvParameterSpec getIVSecureRandom(String algorithm) throws NoSuchAlgorithmException,
                                  NoSuchPaddingException {
        SecureRandom random = SecureRandom.getInstanceStrong();
        byte[] iv = new byte[Cipher.getInstance(algorithm).getBlockSize()];
        random.nextBytes(iv);
        return new IvParameterSpec(iv);
    }

    // --------------------------------------------------------------------------------------------
    //  Utilities - freshness
    // --------------------------------------------------------------------------------------------

    // TODO:
    /** Compares each byte of two separate Base64 hashes in order to verify if they both correspond to the same
     *  record
     * 
     * @param  refreshToken  
     * @param  range    
     * @return                A boolean that signals whether or not the refreshToken is within an acceptable time range.
     * @throws Exception       
     */
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
    

    /** Decodes and Decrypts a each byte of two separate Base64 hashes in order to verify if they both correspond to the 
     * same record
     * 
     * @param  freshnessEncoded 
     * @param  userPrivate     
     * @return                  
     * @throws Exception       
     */
    public static String getRefreshToken(String freshnessEncoded,Key userPrivate) throws Exception {
        byte[] decodedRefreshToken = Base64.getDecoder().decode(freshnessEncoded); 
        byte[] unencryptedRefreshToken = rsaDecrypt(decodedRefreshToken, userPrivate);
        String refreshToken = new String(unencryptedRefreshToken);

        return refreshToken;

    }

    // --------------------------------------------------------------------------------------------
    //  Utilities - hashes
    // -------------------------------------------------------------------------------------------- 

    /** Compares each byte of two separate Base64 hashes in order to verify if they both correspond to the same
     *  record
     * 
     * @param  base64Hash1   First Base64 encoded hash to be compared 
     * @param  base64Hash2   Second Base64 encoded hash to be compared
     * @param  serverPrivate The private RSA key used for encrypting the hash.    
     * @return               A boolean that signals whether or not the received Base64 encoded Hashes are equal.
     * @throws Exception     If any decoding issue occurs.
     */
    public static boolean compareBase64Hashes(String base64Hash1, String base64Hash2) {
        // NOTE THAT:
        // In order to prevent timing attacks, we're comparing each byte of the decoded hashes and 
        // ensuring the operation takes the same time for both equal and unequal hashes.
        byte[] decodedHash1 = Base64.getDecoder().decode(base64Hash1);
        byte[] decodedHash2 = Base64.getDecoder().decode(base64Hash2);

        return MessageDigest.isEqual(decodedHash1, decodedHash2);
    }

   /** Computes a SHA-256 digest of the given JsonObject and encrypts it with RSA using a private key,
     * then encodes the result in Base64.
     *      
     * @param  jsonObject    The JsonObject to be written to the file.
     * @param  serverPrivate The path of the file where the JsonObject should be written.
     * @throws Exception     If an I/O error occurs or the file cannot be written.
     */
    public static String digestAndBase64(JsonObject recordObject,Key serverPrivate) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] hash = digest.digest(gson.toJson(recordObject).getBytes("UTF-8"));
        byte[] encryptedHash = rsaEncrypt(hash, serverPrivate);
        String hashBase64 = Base64.getEncoder().encodeToString(encryptedHash);

        return hashBase64;
    }

    // --------------------------------------------------------------------------------------------
    //  Utilities - misc
    // --------------------------------------------------------------------------------------------

    /**
     * Writes the provided JsonObject to a file.
     * 
     * This method takes a JsonObject and writes it to the specified output file.
     * The file is created or overwritten if it already exists.
     *
     * @param  jsonObject The JsonObject to be written to the file.
     * @param  outputFile The path of the file where the JsonObject should be written.
     * @throws Exception  If an I/O error occurs or the file cannot be written.
     */
    public static void writeJsonObjectToFile(JsonObject jsonObject, String outputFile) throws Exception {

        try (FileWriter fileWriter = new FileWriter(outputFile)) {
            gson = new GsonBuilder().setPrettyPrinting().create();
            gson.toJson(jsonObject, fileWriter);
        }
    }
    
    /**
     * Reads JSON content from a file and converts it to a JsonObject.
     * 
     * This method reads a file from the given input path and parses it into a JsonObject.
     *
     * @param  inputFile The path of the file from which JSON content is to be read.
     * @throws Exception If an I/O error occurs, the file cannot be read, or the content is not valid JSON.
     */
    public static JsonObject readFileToJsonObject(String inputFile) throws Exception  {

        try (FileReader fileReader = new FileReader(inputFile)) {
            return gson.fromJson(fileReader, JsonObject.class);
        }
    }

    /**
     * Encrypts specified fields of a patient's record using AES and RSA encryption.
     *
     * @param  patient     The patient's record (the core data handled) in JSON format.
     * @param  key         The AES key for encryption.
     * @param  userPublic  The public RSA key for encryption.
     * @return             The encrypted patient record.
     * @throws Exception   If an encryption error occurs.
     */
    public static JsonObject encryptRecord(JsonObject patient, JsonObject metadata, String... fields) throws Exception {

        JsonObject encryptedRecord = new JsonObject();

        if(fields.length == 0) {
            // Encrypt all fields using AES
            encryptFields(patient, encryptedRecord, metadata, FIELDS);
        } else {
            encryptFields(patient, encryptedRecord, metadata, fields);
        }
        return encryptedRecord;
    }

    /**
     * Encrypts fields of a JSON object using AES or RSA (when appropriate).
     *
     * @param  patientObject    JsonObject containing data to encrypt.
     * @param  encryptedRecord  JsonObject to store encrypted data.
     * @param  fields           Array of field names to be encrypted.
     * @param  key              Encryption key.
     * @param  useAes           Flag to determine encryption type (AES if true, RSA if false).
     * @throws Exception        If an encryption error occurs.
     */
    private static void encryptFields(JsonObject patientObject, JsonObject encryptedRecord, JsonObject metadata,
                        String[] fields) throws Exception {
        // generates a symmetric encryption key                 
        JsonObject iv = new JsonObject();
        JsonObject keys = new JsonObject();
        for (String field : fields) 
        {
            byte[] bytes;
            if (field.equals(CONSULTATION_RECORDS) || field.equals(KNOWN_ALLERGIES)) {
                JsonArray jsonArray = patientObject.get(field).getAsJsonArray();
                bytes = jsonArray.toString().getBytes();
            } else {
                bytes = patientObject.get(field).getAsString().getBytes();
            }
            Key key = generateKeyAES();  
            IvParameterSpec ivRandom = getIVSecureRandom(ALGORITHM_AES);
            byte[] encryptedBytes = aesEncryptWithIV(bytes, key, ivRandom);
            String encryptedBase64 = Base64.getEncoder().encodeToString(encryptedBytes);
            String ivBase64 = Base64.getEncoder().encodeToString(ivRandom.getIV());
            encryptedRecord.addProperty(field, encryptedBase64);
            iv.addProperty(field, ivBase64);
            String keyEncoded = Base64.getEncoder().encodeToString(key.getEncoded());
            keys.addProperty(field, keyEncoded);
        }
        metadata.add(INITIALIZATION_VECTOR,iv);
        metadata.add(KEYS,keys);
    }

    /**
     * Computes and Encrypts Metadata Linked to the Patient's Record
     *
     * @param  key            Encryption key.
     * @param  userPublic     The public RSA key for encryption.
     * @param  useAes         Flag to determine encryption type (AES if true, RSA if false).
     * @return                The encrypted metadata.
     * @throws Exception      If an encryption error occurs.
     */ 
    public static void encryptMetadata(JsonObject metadata, Key userPublic, Key serverPrivate, JsonObject encryptedRecord) 
                       throws Exception {
        
        // Encrypt each patient's record field's AES symmetric key using the patient's public key.

        for (String field : FIELDS) 
        {
            if(metadata.get(KEYS).getAsJsonObject().get(field) == null) {
                continue;
            }
            byte[] decodedKeyBytes = Base64.getDecoder().decode(
                                     metadata.get(KEYS).getAsJsonObject().get(field).getAsString());
            byte[] encryptedBytes = rsaEncrypt(decodedKeyBytes, userPublic);
            String encryptedBase64 = Base64.getEncoder().encodeToString(encryptedBytes);
            metadata.get(KEYS).getAsJsonObject().addProperty(field, encryptedBase64);
      
        }
        byte[] freshnessBytes = Instant.now().toString().getBytes();
        byte[] encryptedFreshness = rsaEncrypt(freshnessBytes, userPublic);
        String freshnessEncoded = Base64.getEncoder().encodeToString(encryptedFreshness);
        metadata.addProperty(REFRESH_TOKEN, freshnessEncoded);

        String hashBase64 = digestAndBase64(encryptedRecord, serverPrivate);
        metadata.addProperty(HASH, hashBase64);
    }

    /**
     * Decrypts specified fields of a patient's record using AES and RSA decryption.
     *
     * @param  keyBase64   encoded and encrypted symmetric encryption key 
     * @param  record      JsonObject containing data to decrypt. 
     * @param  userPrivate The private RSA key for decryption.
     * @return             The decrypted patient record.
     * @throws Exception   If a decryption error occurs.
     */
    public static JsonObject decryptRecord(JsonObject record, JsonObject iv, JsonObject keys,
                             Key userPrivate, String... fields) throws Exception {

        JsonObject decryptedRecord = new JsonObject();

        
        if(fields.length == 0) {
            // Decrypt all fields using AES
            decryptFields(record, iv, keys, decryptedRecord, userPrivate, FIELDS);
        } else {
            decryptFields(record, iv, keys, decryptedRecord, userPrivate, fields);    
        }
        
        return decryptedRecord;
    }

    /**
     * Decrypts fields of a JSON object using AES or RSA (when appropriate).
     *
     * @param  recordObject    JsonObject containing data to decrypt.
     * @param  decryptedRecord JsonObject to store decrypted data.
     * @param  fields          Array of field names to be decrypted.
     * @param  userPrivate     decryption key.
     * 

     * @throws Exception       If a decryption error occurs.
     */
    private static void decryptFields(JsonObject recordObject, JsonObject iv, JsonObject keys, 
                        JsonObject decryptedRecord, Key userPrivate, String[] fields) throws Exception {
                  
        for (String field : fields) 
        {   
            byte[] encryptedKey = Base64.getDecoder().decode(keys.get(field).getAsString());
            byte[] decryptedKey = rsaDecrypt(encryptedKey, userPrivate);
            Key key = new SecretKeySpec(decryptedKey, 0, decryptedKey.length, ALGORITHM_AES);

            byte[] bytes = recordObject.get(field).getAsString().getBytes();
            byte[] decodedBytes = Base64.getDecoder().decode(bytes);
            byte[] decodedIv = Base64.getDecoder().decode(iv.get(field).getAsString().getBytes());
            byte[] decryptedBytes = AesDecryptWithIV(decodedBytes, key, decodedIv); 

            if (field.equals(CONSULTATION_RECORDS) || field.equals(KNOWN_ALLERGIES)) {
                // this is necessary since consultationRecords and KnownAllergies have different formats
                // NOTE THAT:
                // - consultationRecords comprises an array of JsonObjects, where each JsonObject represents 
                //   a consultation record. 
                // - knownAllergies, on the other hand, is an array of Strings, with each string representing 
                //   a specific allergy.
                // Example structures:
                // "consultationRecords": [
                //     {
                //         "date": "example_date",
                //         "medicalSpeciality": "example_speciality",
                //         "doctorName": "example_name",
                //         "practice": "example_practice",
                //         "treatmentSummary": "example_summary"
                //     },
                //     ... (more records)
                // ]
                //
                // "knownAllergies": ["allergy1", ... (more allergies)]
                Type listType = field.equals(CONSULTATION_RECORDS) ? new TypeToken<List<JsonObject>>() {}.getType() : 
                                new TypeToken<List<String>>() {}.getType();
                List<String> compositeRecords = gson.fromJson(new String(decryptedBytes), listType);
                decryptedRecord.add(field, gson.toJsonTree(compositeRecords));
            } else {
                decryptedRecord.addProperty(field, new String(decryptedBytes));
                
            }        
        }
    }

}
