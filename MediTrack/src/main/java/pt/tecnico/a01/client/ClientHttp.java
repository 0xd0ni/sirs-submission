package main.java.pt.tecnico.a01.client;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

import java.security.Key;
import main.java.pt.tecnico.a01.cryptography.CryptoLibrary;
import okhttp3.Call;
import okhttp3.FormBody;
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
    }

    public JsonObject getRecordAsPatient(String name, Key userPrivate) {
        try {
            JsonObject encryptedrecord = getRecord(name);
            return CryptoLibrary.unprotect(encryptedrecord, userPrivate);
        } catch (Exception e) {
            
            return null;            
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

    public void saveRecord(JsonObject record) {
        RequestBody formBody = new FormBody.Builder()
            .add("record", gson.toJson(record)).build();
        Request request = new Request.Builder()
            .url("http://" + this.serverAddress + "/")
            .put(formBody)
            .build();

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

    public void sendCommitRequest(String patientName, boolean discloseName, boolean discloseSex,
    boolean discloseDateOfBirth, boolean discloseBloodType, boolean discloseKnownAllergies,
    boolean discloseConsultationRecords) {
        RequestBody formBody = new FormBody.Builder()
            .add("name", Boolean.toString(discloseName))
            .add("sex", Boolean.toString(discloseSex))
            .add("dateOfBirth", Boolean.toString(discloseDateOfBirth))
            .add("bloodType", Boolean.toString(discloseBloodType))
            .add("knownAllergies", Boolean.toString(discloseKnownAllergies))
            .add("consultationRecords", Boolean.toString(discloseConsultationRecords))
            .build();
        Request request = new Request.Builder()
            .url("http://" + this.serverAddress + "/commit/" + patientName)
            .post(formBody)
            .build();

        Call call = client.newCall(request);
        try {
            Response response = call.execute();
            if (response.code() != 200) {
                System.out.println("Error sending commit request: " + response.body().string());
            }
            else {
                System.out.println("Commit request sent successfully");
            }
        } catch (Exception e) {
            System.out.println("Error sending commit request: " + e.getMessage());
        }
    }

    public void printRecord(JsonObject record) {
        JsonObject patient = record.get("patient").getAsJsonObject();
        for (String field : CryptoLibrary.FIELDS) {
            if (patient.get(field) != null) { // How to check if a field was protected?
                if (field.equals("knownAllergies")) {
                    System.out.println(field + ": " + gson.toJson(patient.get(field).getAsJsonArray()) + ";");
                }
                else if (field.equals("consultationRecords")) {
                    System.out.println();
                    System.out.println(field + ": " + gson.toJson(patient.get(field).getAsJsonArray()));
                }
                System.out.println(field + ": " + patient.get(field).getAsString() + ";");
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
