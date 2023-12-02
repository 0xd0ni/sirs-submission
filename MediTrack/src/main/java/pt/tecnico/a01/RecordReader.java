package main.java.pt.tecnico.a01;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.io.FileReader;
import java.io.IOException;

public class RecordReader {

    public static void main(String[] args) throws IOException {
        // Check arguments
        if (args.length < 1) { 
            System.err.println("Argument(s) missing!");
            System.err.printf("Usage: java %s file%n", RecordReader.class.getName());
            return;
        }
        final String filename = args[0];

        // Read  MediTrack JSON object from file, and print its contents
        try (FileReader fileReader = new FileReader(filename)) {
            Gson gson = new Gson();
            JsonObject rootJson = gson.fromJson(fileReader, JsonObject.class);
            System.out.println("JSON object: " + rootJson);

            JsonObject patientObject = rootJson.get("patient").getAsJsonObject();
            System.out.println("Patient Record:");
            System.out.println("name: " + patientObject.get("name").getAsString());
            System.out.println("sex: " + patientObject.get("sex").getAsString());
            System.out.println("dateOfBirth: " + patientObject.get("dateOfBirth").getAsString());
            System.out.println("bloodType: " + patientObject.get("bloodType").getAsString());
            
            JsonArray knownAllergiesArray = patientObject.getAsJsonArray("knownAllergies");
            System.out.println("Known allergies: ");
            for (int i = 0; i < knownAllergiesArray.size(); i++) {
                System.out.print(knownAllergiesArray.get(i).getAsString());
                if (i < knownAllergiesArray.size() - 1) {
                    System.out.print(", ");
                } else {
                    System.out.println(); 
                }
            }

            JsonArray consultationRecordJsonArray = patientObject.getAsJsonArray("consultationRecords");
            System.out.println("Consultation records: ");
            
           for (JsonElement consultationRecord : consultationRecordJsonArray) {

                JsonObject consultationRecordObject = consultationRecord.getAsJsonObject();

                System.out.println("date: " + consultationRecordObject.get("date").getAsString());
                System.out.println("medicalSpecialty " + consultationRecordObject.get("medicalSpecialty").getAsString());
                System.out.println("doctorName: " + consultationRecordObject.get("doctorName").getAsString());
                System.out.println("practice: " + consultationRecordObject.get("practice").getAsString());
                System.out.println("treatmentSummary: " + consultationRecordObject.get("treatmentSummary").getAsString());
                System.out.println();

            }
        }
 
    }
}


