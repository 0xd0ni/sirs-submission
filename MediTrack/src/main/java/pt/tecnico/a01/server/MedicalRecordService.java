package main.java.pt.tecnico.a01.server;

import main.java.pt.tecnico.a01.cryptography.CryptoLibrary;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

import java.security.Key;
import java.util.ArrayList;

public class MedicalRecordService {
    
    private MedicalRecordRepository medicalRecordRepository;

    private Key userPublic;

    private Key serverPrivate;

    private Gson gson;

    public MedicalRecordService() throws Exception{
        this.medicalRecordRepository = new MedicalRecordRepository("mongodb://localhost:27017","meditrack");
        this.userPublic = CryptoLibrary.readPublicKey("../keys/user.pubkey");
        this.serverPrivate = CryptoLibrary.readPrivateKey("../keys/server.privkey");
        this.gson = new Gson();
    }

    // if we get an empty string, we should throw an exception
    public String getMedicalRecord(String patientName) throws Exception{
        // maybe add Status
        String medicalRecord = medicalRecordRepository.find(patientName).orElse(null);
        if (medicalRecord == null) {
            throw new Exception("Patient not found");
        }
        JsonObject medicalRecordObject = gson.fromJson(medicalRecord, JsonObject.class);
        JsonObject metadata = medicalRecordObject.get("metadata").getAsJsonObject();
        CryptoLibrary.encryptMetadata(
            metadata, 
            userPublic, serverPrivate,
            medicalRecordObject.get("record").getAsJsonObject());
        medicalRecordObject.add("metadata", metadata);
        return gson.toJson(medicalRecordObject);
    }
    public String saveMedicalRecord(String carrierJson) throws Exception {
        JsonObject medicalRecordJson;
        try {
            medicalRecordJson = gson.fromJson(carrierJson, JsonObject.class);
        }
        catch(Exception e){
            throw new Exception("Invalid record + " + carrierJson);
        }
        JsonObject encryptedRecordJson;
        try {
            encryptedRecordJson = CryptoLibrary.protect(medicalRecordJson);
        }
        catch(Exception e){
            throw new Exception("Failed to encrypt record");
        }
        String name = medicalRecordJson.get("patient").getAsJsonObject().get("name").getAsString();
        encryptedRecordJson.addProperty("name", name);
        // The name has to be exposed
        return medicalRecordRepository.save(gson.toJson(encryptedRecordJson));
    }

    public void changeProtections(String patientName, String fieldProperties) throws Exception {
        JsonObject fieldPropertiesJson = gson.fromJson(fieldProperties, JsonObject.class);
        String medicalRecord = medicalRecordRepository.find(patientName).orElse(null);
        if (medicalRecord == null) {
            throw new Exception("Patient not found");
        }
        JsonObject medicalRecordObject = gson.fromJson(fieldPropertiesJson, JsonObject.class);
        JsonObject unprotectedObject = CryptoLibrary.unprotect(medicalRecordObject);
        
        ArrayList<String> fields = new ArrayList<String>();
        
        // if we want name to be always public change this enum (FIELDS)
        for (String field : CryptoLibrary.FIELDS) {
            if (fieldPropertiesJson.get(field) != null && !fieldPropertiesJson.get(field).getAsBoolean()) {
                fields.add(field);
            }
        }

        JsonObject newlyProtectedObject = CryptoLibrary.protect(unprotectedObject, fields.toArray(new String[fields.size()]));
        medicalRecordRepository.save(gson.toJson(newlyProtectedObject));
    }
}
