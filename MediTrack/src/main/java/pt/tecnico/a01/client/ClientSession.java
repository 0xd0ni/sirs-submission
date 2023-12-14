package main.java.pt.tecnico.a01.client;

import main.java.pt.tecnico.a01.cryptography.CryptoLibrary;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

import org.apache.commons.cli.*;

import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Call;
import okhttp3.OkHttpClient;
import okhttp3.Response;
import okhttp3.FormBody;


public class ClientSession {

    private OkHttpClient client = new OkHttpClient();

    private Gson gson = new Gson();

    private String serverAddress;

    private final String RUNTIME = "runtime";
    private final String PATIENT = "patient";
    private final String DOCTOR = "doctor";
    private final String ADMIN = "admin";
    private final String QUIT = "quit";

    private boolean discloseSex = false;
    private boolean discloseDateOfBirth = false;
    private boolean discloseBloodtype = false;
    private boolean discloseKnownAllergies = false;

    private JsonObject record;

    private String patientName;

    CommandLineParser parser = new DefaultParser();

    Options options = new Options();
    Options runtimeOptions = new Options();
    Options patientOptions = new Options();
    Options doctorOptions = new Options();

    public ClientSession() {

        options.addOption("h", "help", false, "Show help.");
        options.addOption("a", "address", true, "Address to connect to. ip:port");
        
        runtimeOptions.addOption("r", "register", true, "Register a new patient. Usage: -r <name>");
        runtimeOptions.addOption("s", "show", true, "Get a patient's record. Usage: -g <name>");
        runtimeOptions.addOption("q", "quit", false, "Quit the application. Usage: -q");
        
        patientOptions.addOption("r", "register", true, "Register a new patient. Usage: -r <name>");
        patientOptions.addOption("s", "show", true, "Get a patient's record. Usage: -g <name>");
        patientOptions.addOption("pf", "protectField", true, "Protect a field in the patient's record. Usage: -pf <field>");
        patientOptions.addOption("df", "discloseField", true, "Disclose a field in the patient's record. Usage: -df <field>");
        patientOptions.addOption("c", "commit", false, "Commit changes to the patient's record's protection. Usage: -c");
        patientOptions.addOption("s", "state", false, "Show the current state of the patient's record. Usage: -s");
        patientOptions.addOption("q", "quit", false, "Quit the application. Usage: -q");

    }

    public void start(String[] args){
        CommandLine cmd;
        try {
            cmd = parser.parse(options, args);
        } catch (Exception e) {
            System.err.println("Error parsing command");
            return;
        }
        if (cmd.hasOption("help")) {
            HelpFormatter formatter = new HelpFormatter();
            formatter.printHelp("MediTrack", options);
            System.out.println(" ");
            System.out.println("For runtime: ");
            formatter.printHelp("MediTrack", runtimeOptions);
            System.out.println(" ");
            System.out.println("For patient mode: ");
            formatter.printHelp("MediTrack", patientOptions);
            return;
        }
        if (!cmd.hasOption("address")) {
            System.out.println("Error: Missing address");
            return;
        }
        serverAddress = cmd.getOptionValue("address");
        String mode = RUNTIME; // modes: runtime, patient, doctor, admin
        while (true) {
            String[] command = System.console().readLine().split(" ", 0);
            if (mode.equals(RUNTIME)) {
                mode = parseRuntime(command);
            }
            else if (mode.equals(PATIENT)) {
                mode = parsePatient(command);
            }
            else if (mode.equals(DOCTOR)) {

            }
            else if (mode.equals(ADMIN)) {

            }
            else {
                System.out.println("Error: Invalid mode");
            }

            if (mode.equals(QUIT)) {
                break;
            }
            
        }
    }

    public String parseRuntime(String[] args) {CommandLine cmd;
        try { 
            cmd = parser.parse(this.runtimeOptions, args);
        } catch (Exception e) {
            System.err.println("Error parsing command");
            return QUIT;
        }
        if (cmd.hasOption("register")) {
            String patientName = cmd.getOptionValue("register");
            return PATIENT;

        } else if (cmd.hasOption("show")) {
            String patientName = cmd.getOptionValue("show");
            String record = this.getRecordAsPatient(patientName);
            // if successful set name
            return PATIENT;
        } else if (cmd.hasOption("quit")) {
            return QUIT;
        } else {
            System.out.println("Error: Invalid command");
            return RUNTIME;
        }
    }

    public String parsePatient(String[] args) {
        CommandLine cmd;
        try { 
            cmd = parser.parse(this.patientOptions, args);
        } catch (Exception e) {
            System.err.println("Error parsing command");
            return QUIT;
        }
        if (cmd.hasOption("register")) {
            String patientName = cmd.getOptionValue("register");
            // where do we get the record from?
            return PATIENT;
        } else if (cmd.hasOption("show")) {
            String patientName = cmd.getOptionValue("show");
            String record = this.getRecordAsPatient(patientName);
            // if successful set name
            printRecord(record);
            
            return PATIENT;
        } else if (cmd.hasOption("protectField")) {
            String field = cmd.getOptionValue("protectField");
            this.setPropertyToFalse(field);
            return PATIENT;
        } else if (cmd.hasOption("discloseField")) {
            String field = cmd.getOptionValue("discloseField");
            this.setPropertyToTrue(field);
            return PATIENT;
        } else if (cmd.hasOption("commit")) {
            this.sendCommitRequest();
            return PATIENT;
        } else if (cmd.hasOption("quit")) {
            return QUIT;
        } else {
            System.out.println("Error: Invalid command");
            return PATIENT;
        }
    }

    public String parseDoctor(String[] args) {
        CommandLine cmd;
        try { 
            cmd = parser.parse(this.doctorOptions, args);
        } catch (Exception e) {
            System.err.println("Error parsing command");
            return QUIT;
        }
        return "";

    }

    public void resetDiscloseStatus() {
        this.discloseSex = false;
        this.discloseDateOfBirth = false;
        this.discloseBloodtype = false;
        this.discloseKnownAllergies = false;
    }

    public boolean doesPropertyExist(String property) {
        if (property.equals("sex") || property.equals("dob") || property.equals("bloodtype") || property.equals("knownAllergies")) {
            return true;
        }
        else {
            return false;
        }
    }

    public void setPropertyToFalse(String property) {
        if (property.equals("sex")) {
            this.discloseSex = false;
        } else if (property.equals("dob")) {
            this.discloseDateOfBirth = false;
        } else if (property.equals("bloodtype")) {
            this.discloseBloodtype = false;
        } else if (property.equals("knownAllergies")) {
            this.discloseKnownAllergies = false;
        }
    }

    public void setPropertyToTrue(String property) {
        if (property.equals("sex")) {
            this.discloseSex = true;
        } else if (property.equals("dob")) {
            this.discloseDateOfBirth = true;
        } else if (property.equals("bloodtype")) {
            this.discloseBloodtype = true;
        } else if (property.equals("knownAllergies")) {
            this.discloseKnownAllergies = true;
        }
    }

    public void toggleProperty(String property) {
        if (property.equals("sex")) {
            this.discloseSex = !this.discloseSex;
        } else if (property.equals("dob")) {
            this.discloseDateOfBirth = !this.discloseDateOfBirth;
        } else if (property.equals("bloodtype")) {
            this.discloseBloodtype = !this.discloseBloodtype;
        } else if (property.equals("knownAllergies")) {
            this.discloseKnownAllergies = !this.discloseKnownAllergies;
        }
    } 

    public String getRecordAsPatient(String name) {
        try {
            JsonObject encryptedrecord = this.getRecord(name);
            //String record = CryptoLibrary.unprotect(record, CryptoLibrary.readPrivateKey("../keys/" + name + ".privkey"));
            return "";
        } catch (Exception e) {
            
            return "";            
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

    public void sendCommitRequest() {
        RequestBody formBody = new FormBody.Builder()
            .add("discloseSex", Boolean.toString(this.discloseSex))
            .add("discloseDateOfBirth", Boolean.toString(this.discloseDateOfBirth))
            .add("discloseBloodtype", Boolean.toString(this.discloseBloodtype))
            .add("discloseKnownAllergies", Boolean.toString(this.discloseKnownAllergies))
            .build();
        Request request = new Request.Builder()
            .url("http://" + this.serverAddress + "/commit/" + this.patientName)
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


    public void printRecord(String record) {
        
    }
}
