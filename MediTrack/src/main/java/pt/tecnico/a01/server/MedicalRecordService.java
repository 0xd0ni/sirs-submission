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
    public String getMedicalRecord(String patientName) {
        // maybe add Status
        String medicalRecord = medicalRecordRepository.find(patientName).orElse("{}");
        return medicalRecord;
    }
    public String saveMedicalRecord(String medicalRecord) {
        JsonObject medicalRecordJson = gson.fromJson(medicalRecord, JsonObject.class);
        medicalRecordJson.addProperty("name", gson.toJson(medicalRecordJson.get("metadata").getAsJsonObject().get("name")));
        return medicalRecordRepository.save(medicalRecord);
    }

    public String changeProtections(String patientName, String fieldProperties) throws Exception {
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
            if (fieldPropertiesJson.get(field).getAsBoolean()) {
                fields.add(field);
            }
        }

        JsonObject newlyProtectedObject = CryptoLibrary.protect(unprotectedObject, fields.toArray(new String[fields.size()]));
        JsonObject metadata = newlyProtectedObject.get("metadata").getAsJsonObject();
        CryptoLibrary.encryptMetadata(
            metadata, 
            userPublic, serverPrivate,
            newlyProtectedObject.get("record").getAsJsonObject());
        newlyProtectedObject.add("metadata", metadata);
        return medicalRecordRepository.save(gson.toJson(newlyProtectedObject));
    }
}
