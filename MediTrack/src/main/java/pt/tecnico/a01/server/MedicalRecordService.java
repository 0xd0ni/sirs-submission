package main.java.pt.tecnico.a01.server;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.annotation.Isolation;

import java.util.List;

import com.google.gson.JsonObject;

@Service
public class MedicalRecordService {
    
    private MedicalRecordRepository medicalRecordRepository;

    public MedicalRecordService() {
        this.medicalRecordRepository = new MedicalRecordRepository("mongodb://addr:","meditrack");
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public JsonObject getMedicalRecord(String patientName) {
        JsonObject medicalRecord = medicalRecordRepository.find(patientName).orElse(null);
        return medicalRecord;
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public JsonObject saveMedicalRecord(JsonObject medicalRecord) {
        // todo: decrypt name
        //medicalRecord.addProperty("name", medicalRecord.get());
        return medicalRecordRepository.save(medicalRecord);
    }
}
