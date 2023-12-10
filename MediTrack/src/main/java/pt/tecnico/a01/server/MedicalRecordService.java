package main.java.pt.tecnico.a01.server;

public class MedicalRecordService {
    
    private MedicalRecordRepository medicalRecordRepository;

    public MedicalRecordService() {
        this.medicalRecordRepository = new MedicalRecordRepository("mongodb://addr:","meditrack");
        // load Keys
    }

    public String getMedicalRecord(String patientName) {
        // maybe add Status
        String medicalRecord = medicalRecordRepository.find(patientName).orElse("{}");
        return medicalRecord;
    }

    public String saveMedicalRecord(String medicalRecord) {
        // todo: decrypt name
        //medicalRecord.addProperty("name", medicalRecord.get());
        return medicalRecordRepository.save(medicalRecord);
    }
}
