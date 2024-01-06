package main.java.pt.tecnico.a01.client;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;

import java.util.List;
import java.security.Key;

import main.java.pt.tecnico.a01.cryptography.CryptoLibrary;
import okhttp3.Call;
import okhttp3.FormBody;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class ClientHttp {

    private OkHttpClient client = new OkHttpClient();

    private Gson gson = new Gson();

    private String serverAddress;

    public ClientHttp(String serverAddress) {
        this.serverAddress = serverAddress;
        try {
        } catch (Exception e) {
            System.out.println("Error reading server private key: " + e.getMessage());
            System.exit(1);
        }
    }
    
    public void addConsultationRecordAsDoctor(JsonObject record, String patientName, Key patientPublic) {
        // perhaps the server should check the record's integrity and authenticity before adding it to the database
        try {
            // JsonObject encryptedRecord = CryptoLibrary.protectConsultationRecord(record, patientPublic);
            
        }
        catch (Exception e) {
            System.out.println("Error adding consultation record as Doctor: " + e.getMessage());
        }
    }

    public void populate(String filename) {
        try {
            saveRecord(CryptoLibrary.readFileToJsonObject(filename));
        }
        catch (Exception e) {
            System.out.println("Error populating database: " + e.getMessage());
        }
    }

    public void saveRecordAsPatient(JsonObject record, Key patientPublic, Key sosPublic) {
        try {
            // REMOVE SERVER PRIVATE KEY
            JsonObject encryptedRecord = CryptoLibrary.protect(record, patientPublic, sosPublic);
            record.get("patient").getAsJsonObject();
            record.get("patient").getAsJsonObject().get("name").getAsString();
            encryptedRecord.addProperty("name", record.get("patient").getAsJsonObject().get("name").getAsString());
            saveRecord(encryptedRecord);
        }
        catch (Exception e) {
            System.out.println("Error saving record as Patient: " + e.getMessage());
        }
    }

    public void saveRecord(JsonObject record) {
        RequestBody formBody = FormBody.create(gson.toJson(record), MediaType.parse("application/json"));
        Request request = new Request.Builder()
            .url("http://" + this.serverAddress + "/" + record.get("name").getAsString())
            .put(formBody)
            .build();
        System.out.println("Saving record... url: " + request.url().toString());
        Call call = client.newCall(request);
        try {
            Response response = call.execute();
            if (response.code() != 200) {
                System.out.println("Error saving record: " + response.body().string());
            }
            else {
                System.out.println("Record saved successfully");
            }
        } catch (Exception e) {
            System.out.println("Error saving record: " + e.getMessage());
        }
    }

    public JsonObject getRecordAsPatient(String name, Key userPrivate, Key serverPublic) {
        try {
            JsonObject encryptedRecord = getRecord(name);
            System.out.println("Checking record");
            boolean healthy = CryptoLibrary.check(encryptedRecord, serverPublic);
            if (!healthy) {
                System.out.println("Record is not healthy");
                return null;
            }
            return CryptoLibrary.unprotect(encryptedRecord, userPrivate);
        } catch (Exception e) {
            System.out.println("Error getting record as Patient: " + e.getMessage());
            return null;            
        }
    }

    public JsonObject getRecordAsDoctor(String patientName, String doctorName, Key doctorPrivate, Key serverPublic) {
        try {
            JsonObject encryptedRecord = getRecord(patientName);
            JsonObject doctorsKeys = getKeys(doctorName, patientName);
            System.out.println("Doctors keys: " + gson.toJson(doctorsKeys));
            System.out.println("Checking record");
            boolean healthy = CryptoLibrary.check(encryptedRecord, serverPublic);
            if (!healthy) {
                System.out.println("Record is not healthy");
                return null;
            }
            JsonObject decryptedRecord = CryptoLibrary.unprotectWithCustomKeys(encryptedRecord, doctorsKeys, doctorPrivate); 
            return decryptedRecord;
        }
        catch (Exception e) {
            System.out.println("Error getting record as Doctor: " + e.getMessage());
            return null;
        }
    }

    public JsonObject getRecordInSos(String patientName, String doctorName, Key doctorPrivate, String keyfile) {
        // Add sosPublic to other functions that need it
        // sos must be visible to the server, it cannot be the same type of request as normal access
        // sos request: the doctor would authenticate himself to the server using a signature and a freshness token
        // the server would then unprotect keys on behalf of the doctor and send them, encrypted with the doctor's public key 
        // To put in report: the records should ideally be re-encrypted with new keys after an sos event. We won't do this.
        try {
            JsonObject encryptedRecord = getRecord(patientName);
            JsonObject sosKeys = getSosKeys(patientName, doctorName, keyfile);
            JsonObject decryptedRecord = CryptoLibrary.unprotectWithCustomKeys(encryptedRecord, sosKeys, doctorPrivate);
            return decryptedRecord;
        }
        catch (Exception e) {
            System.out.println("Error getting record in SOS: " + e.getMessage());
            return null;
        }
    }

    public JsonObject getRecord(String name) throws Exception {
        Request request = new Request.Builder()
            .url("http://" + this.serverAddress + "/" + name)
            .build();
        Call call = client.newCall(request);
        Response response = call.execute();
        if (response.code() != 200) {
            throw new Exception("Error getting record: " + response.body().string());
        }
        return this.gson.fromJson(response.body().string(), JsonObject.class);
    }

    public JsonObject getKeys(String doctorName, String patientName) throws Exception {
        Request request = new Request.Builder()
            .url("http://" + this.serverAddress + "/keys/" + doctorName + "/" + patientName)
            .build();
        Call call = client.newCall(request);
        Response response = call.execute();
        if (response.code() != 200) {
            throw new Exception("Error getting keys: " + response.body().string());
        }
        return this.gson.fromJson(response.body().string(), JsonObject.class);
    }

    public JsonObject getSosKeys(String patientName, String doctorName, String keyfile) throws Exception {
        Request request = new Request.Builder()
            .url("http://" + this.serverAddress + "/sos/" + patientName + "/" + doctorName + "/" + keyfile)
            .build();
        Call call = client.newCall(request);
        Response response = call.execute();
        if (response.code() != 200) {
            throw new Exception("Error getting sos keys: " + response.body().string());
        }
        return this.gson.fromJson(response.body().string(), JsonObject.class);
    }

    public void shareKeys(String patientName, String[] fields, String doctorName, Key userPrivate, Key doctorPublic) {
        System.out.println("Sharing keys...");
        System.out.println("Patient: " + patientName);
        System.out.println("Fields: " + fields.toString());
        System.out.println("Doctor: " + doctorName);
        JsonObject encryptedKeys = new JsonObject();
        try {
            JsonObject record = getRecord(patientName);
            // perhaps add a check of the record here
            JsonObject keys = CryptoLibrary.unprotectKeys(record.get("metadata").getAsJsonObject().get("keys").getAsJsonObject(), userPrivate);
            encryptedKeys = CryptoLibrary.protectKeys(keys, doctorPublic, fields);
        } catch (Exception e) {
            System.out.println("Error protecting keys: " + e.getMessage());
            return;
        }
        System.out.println("Encrypted keys: " + gson.toJson(encryptedKeys));
        RequestBody formBody = FormBody.create(gson.toJson(encryptedKeys), MediaType.parse("application/json"));
        Request request = new Request.Builder()
            .url("http://" + this.serverAddress + "/keys/" + doctorName + "/" + patientName)
            .post(formBody)
            .build();
        Call call = client.newCall(request);
        try {
            Response response = call.execute();
            if (response.code() != 200) {
                System.out.println("Error sharing keys: " + response.body().string());
            }
            else {
                System.out.println("Keys shared successfully");
            }
        } catch (Exception e) {
            System.out.println("Error sharing keys: " + e.getMessage());
        }
    }

    public void printRecord(JsonObject record) {
        Gson prettyGson = new GsonBuilder().setPrettyPrinting().create();
        JsonObject patient = record.get("patient").getAsJsonObject();
        for (String field : CryptoLibrary.FIELDS) {
            if (patient.get(field) != null) { // How to check if a field was protected?
                if (field.equals("knownAllergies")) {
                    System.out.println(field + ": " + prettyGson.toJson(patient.get(field).getAsJsonArray()) + ";");
                }
                else if (field.equals("consultationRecords")) {
                    System.out.println();
                    System.out.println(field + ": " + prettyGson.toJson(patient.get(field).getAsJsonArray()));
                }
                else {
                    System.out.println(field + ": " + patient.get(field).getAsString() + ";");
                }
            }
        }
    }

    public boolean wasFieldProtected(JsonObject record, String field) {
        JsonObject keys = record.get("patient").getAsJsonObject()
            .get("metadata").getAsJsonObject()
            .get("keys").getAsJsonObject();
        if (keys.get(field) != null) {
            return true;
        }
        else {
            return false;
        }
    }
}
