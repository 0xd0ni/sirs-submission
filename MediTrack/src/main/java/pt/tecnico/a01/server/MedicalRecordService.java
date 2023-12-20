package main.java.pt.tecnico.a01.server;

import main.java.pt.tecnico.a01.cryptography.CryptoLibrary;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.security.Key;
import java.util.ArrayList;

public class MedicalRecordService {
    
    private MedicalRecordRepository medicalRecordRepository;

    private Key userPublic;

    private Key sosPublic;

    private Key serverPrivate;

    private Gson gson;

    public MedicalRecordService() throws Exception{
        this.medicalRecordRepository = new MedicalRecordRepository("mongodb://192.168.56.10:27017","meditrack");
        this.userPublic = CryptoLibrary.readPublicKey("../keys/user.pubkey");
        this.sosPublic = CryptoLibrary.readPublicKey("../keys/sospub.key");
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
            userPublic, sosPublic, serverPrivate,
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
        // The name has to be exposed
        return medicalRecordRepository.save(gson.toJson(medicalRecordJson));
    }

    public void shareKeys(String doctorName, String patientName, String keys) throws Exception {
        medicalRecordRepository.addKeys(doctorName, patientName, keys);
    }

    public void addConsultationRecordAsDoctor(String patientName, String consultationRecord) {
        // For this to work consultationRecords must be split up
        medicalRecordRepository.find(patientName).ifPresent(medicalRecord -> {
            JsonObject medicalRecordJson = gson.fromJson(medicalRecord, JsonObject.class);
            JsonObject record = medicalRecordJson.get("record").getAsJsonObject();
            JsonArray consultationRecords;
            if (record.get("consultationRecords") != null) {
                consultationRecords = gson.fromJson(record.get("consultationRecords").getAsJsonArray(), JsonArray.class);
            }
            else {
                consultationRecords = new JsonArray();
            }
            // redo digest?
            consultationRecords.add(consultationRecord);
            record.add("consultations", consultationRecords.getAsJsonObject());
            medicalRecordRepository.save(gson.toJson(medicalRecordJson));
        });
    }
}
