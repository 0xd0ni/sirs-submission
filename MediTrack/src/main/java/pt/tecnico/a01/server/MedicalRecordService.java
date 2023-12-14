package main.java.pt.tecnico.a01.server;

import main.java.pt.tecnico.a01.cryptography.CryptoLibrary;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

import java.security.Key;

public class MedicalRecordService {
    
    private MedicalRecordRepository medicalRecordRepository;

    private Key userPublic;

    private Gson gson;

    public MedicalRecordService() throws Exception{
        this.medicalRecordRepository = new MedicalRecordRepository("mongodb://localhost:27017","meditrack");
        this.userPublic = CryptoLibrary.readPublicKey("../keys/user.pubkey");
        this.gson = new Gson();
    }

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

    public String changeProtections(String patientName, String fieldProperties) {
        JsonObject fieldPropertiesJson = gson.fromJson(fieldProperties, JsonObject.class);
        String medicalRecord = medicalRecordRepository.find(patientName).orElse(null);
        if (medicalRecord == null) {
            return null;
        }
        Boolean discloseSex = fieldPropertiesJson.get("sex").getAsBoolean();
        Boolean discloseDateOfBirth = fieldPropertiesJson.get("dateOfBirth").getAsBoolean();
        Boolean discloseBloodtype = fieldPropertiesJson.get("bloodtype").getAsBoolean();
        Boolean discloseKnownAllergies = fieldPropertiesJson.get("knownAllergies").getAsBoolean();
    ;
        JsonObject medicalRecordJson = gson.fromJson(medicalRecord, JsonObject.class);
        JsonObject metadataJson = medicalRecordJson.get("metadata").getAsJsonObject();
        metadataJson.add("fieldProperties", fieldPropertiesJson);
        medicalRecordJson.add("metadata", metadataJson);
        return medicalRecordRepository.save(medicalRecordJson.toString());
    }
}
