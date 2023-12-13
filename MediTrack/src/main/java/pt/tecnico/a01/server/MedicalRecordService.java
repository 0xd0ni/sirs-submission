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
}
