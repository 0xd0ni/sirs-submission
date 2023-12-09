package main.java.pt.tecnico.a01.server;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import com.google.gson.JsonObject;

import org.springframework.boot.autoconfigure.*;

@RestController
@RequestMapping("/records")
public class MedicalRecordController {

    @Autowired
    private MedicalRecordService medicalRecordService;

    MedicalRecordController(MedicalRecordService medicalRecordService) {
        this.medicalRecordService = medicalRecordService;
    }

    @GetMapping("/records/{patientName}")
    public JsonObject getMedicalRecord(String patientName) {
        JsonObject medicalRecord = medicalRecordService.getMedicalRecord(patientName);
        return medicalRecord;
    }
}