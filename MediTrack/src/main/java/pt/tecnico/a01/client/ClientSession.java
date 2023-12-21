package main.java.pt.tecnico.a01.client;

import main.java.pt.tecnico.a01.cryptography.CryptoLibrary;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

import org.apache.commons.cli.*;

import java.security.Key;

import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Call;
import okhttp3.OkHttpClient;
import okhttp3.Response;
import okhttp3.FormBody;


public class ClientSession {

    private Gson gson = new Gson();

    private String serverAddress;

    private ClientHttp clientHttp;

    private Key userPublic;

    private Key userPrivate;

    private Key sosPublic;

    private Key serverPublic;

    // If we wish to have different users the key will need to be changed on login


    private final String RUNTIME = "runtime";
    private final String PATIENT = "patient";
    private final String DOCTOR = "doctor";
    private final String ADMIN = "admin";
    private final String QUIT = "quit";

    private JsonObject record;

    private String userName;

    CommandLineParser parser = new DefaultParser();

    Options options = new Options();
    Options runtimeOptions = new Options();
    Options patientOptions = new Options();
    Options doctorOptions = new Options();

    public ClientSession() {

        options.addOption("h", "help", false, "Show help.");
        options.addOption("a", "address", true, "Address to connect to. ip:port");
        
        runtimeOptions.addOption("p", "patient", true, "Sign in as a patient. Usage: -u <name>");
        runtimeOptions.addOption("d", "doctor", true, "Sign in as a doctor. Usage: -d <name>");
        runtimeOptions.addOption("q", "quit", false, "Quit the application. Usage: -q");
        
        patientOptions.addOption("p", "patient", true, "Sign in as a patient. Usage: -u <name>");
        patientOptions.addOption("d", "doctor", true, "Sign in as a doctor. Usage: -d <name>");
        patientOptions.addOption("r", "register", true, "Register a new patient and add record. Usage: -r <path to file>");
        patientOptions.addOption("s", "show", false, "Get a patient's record. Usage: -s");
        Option share = new Option("a", "share", true, "Share a patient's record with a doctor. Usage: -a <doctorName> <list of fields, space separated>");
        share.setArgs(Option.UNLIMITED_VALUES);
        patientOptions.addOption(share);
        patientOptions.addOption("q", "quit", false, "Quit the application. Usage: -q");

        doctorOptions.addOption("p", "patient", true, "Sign in as a patient. Usage: -u <name>");
        doctorOptions.addOption("d", "doctor", true, "Sign in as a doctor. Usage: -d <name>");
        doctorOptions.addOption("s", "show", true, "Get a patient's record. Usage: -g <name>");
        doctorOptions.addOption("e", "emergency", true, "SOS. Get a patient's record in case of emergency. Usage: -e <name>");
        doctorOptions.addOption("a", "add", true, "Add a consultation record to a patient's record. Usage: -a <name> <path to file>");
        doctorOptions.addOption("q", "quit", false, "Quit the application. Usage: -q");
    }

    public void start(String[] args){
        CommandLine cmd;
        try {
            cmd = parser.parse(options, args);
            this.sosPublic = CryptoLibrary.readPublicKey("../keys/sospub.key");
            this.serverPublic = CryptoLibrary.readPublicKey("../keys/server.pubkey");
        } catch (Exception e) {
            System.err.println("Error parsing command and reading keys: " + e.getMessage());
            return;
        }
        if (cmd.hasOption("help")) {
            HelpFormatter formatter = new HelpFormatter();
            formatter.printHelp("MediTrack", options);
            System.out.println();
            System.out.println("For runtime: ");
            formatter.printHelp("-", runtimeOptions);
            System.out.println();
            System.out.println("For patient mode: ");
            formatter.printHelp("-", patientOptions);
            return;
        }
        if (!cmd.hasOption("address")) {
            System.out.println("Error: Missing address");
            return;
        }
        this.serverAddress = cmd.getOptionValue("address");
        this.clientHttp = new ClientHttp(serverAddress);

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
                mode = parseDoctor(command);
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
            return RUNTIME;
        }
        if (cmd.hasOption("patient")) {
            this.userName = cmd.getOptionValue("patient");
            try {
                this.userPublic = CryptoLibrary.readPublicKey("../keys/user.pubkey");
                this.userPrivate = CryptoLibrary.readPrivateKey("../keys/user.privkey");
            } catch (Exception e) {
                System.out.println("Error reading user key: " + e.getMessage());
                return RUNTIME;
            }
            return PATIENT;

        } else if (cmd.hasOption("doctor")) {
            this.userName = cmd.getOptionValue("doctor");
            try {
                this.userPublic = this.getDoctorPublicKey(this.userName);
                this.userPrivate = this.getDoctorPrivateKey(this.userName);
            } catch (Exception e) {
                System.out.println("Error reading user key: " + e.getMessage());
                return RUNTIME;
            }
            return DOCTOR;
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
        if (cmd.hasOption("patient")) {
            this.userName = cmd.getOptionValue("patient");
            try {
                this.userPublic = CryptoLibrary.readPublicKey("../keys/user.pubkey");
                this.userPrivate = CryptoLibrary.readPrivateKey("../keys/user.privkey");
            } catch (Exception e) {
                System.out.println("Error reading user key: " + e.getMessage());
                return RUNTIME;
            }
            return PATIENT;
        } else if (cmd.hasOption("doctor")) {
            this.userName = cmd.getOptionValue("doctor");
            try {
                this.userPublic = this.getDoctorPublicKey(this.userName);
                this.userPrivate = this.getDoctorPrivateKey(this.userName);
            } catch (Exception e) {
                System.out.println("Error reading user key: " + e.getMessage());
                return RUNTIME;
            }
            return DOCTOR;
        } else if (cmd.hasOption("register")) {
            String filePath = cmd.getOptionValue("register");
            try {
                JsonObject record = CryptoLibrary.readFileToJsonObject(filePath);
                this.clientHttp.saveRecordAsPatient(record, userPublic, sosPublic);
            } catch (Exception e) {
                System.out.println("Error: " + e.getMessage());
                return PATIENT;
            }
            return PATIENT;
        } else if (cmd.hasOption("show")) {
            JsonObject record = this.clientHttp.getRecordAsPatient(this.userName, userPrivate, serverPublic);
            if (record == null) {
                return PATIENT;
            }
            this.clientHttp.printRecord(record);
            return PATIENT;
        } else if (cmd.hasOption("share")) {
            String[] shareArgs = cmd.getOptionValues("share");
            if (shareArgs.length == 0) {
                System.out.println("Error: Invalid number of arguments. Expected <doctorName> <list of fields, space separated>");
                return PATIENT;
            }
            String doctorName = shareArgs[0];
            String[] fields = new String[shareArgs.length - 1];
            for (int i = 1; i < shareArgs.length; i++) {
                fields[i - 1] = shareArgs[i];
            }
            try {
                Key doctorPublic = this.getDoctorPublicKey(doctorName);
                this.clientHttp.shareKeys(this.userName, fields, doctorName, userPrivate, doctorPublic);
            } catch (Exception e) {
                System.out.println("Error: " + e.getMessage());
            }
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
        if (cmd.hasOption("patient")) {
        this.userName = cmd.getOptionValue("patient");
            try {
                this.userPublic = CryptoLibrary.readPublicKey("../keys/user.pubkey");
                this.userPrivate = CryptoLibrary.readPrivateKey("../keys/user.privkey");
            } catch (Exception e) {
                System.out.println("Error reading user key: " + e.getMessage());
                return RUNTIME;
            }
            return PATIENT;
        } else if (cmd.hasOption("doctor")) {
            this.userName = cmd.getOptionValue("doctor");
            try {
                this.userPublic = this.getDoctorPublicKey(this.userName);
                this.userPrivate = this.getDoctorPrivateKey(this.userName);
            } catch (Exception e) {
                System.out.println("Error reading user key: " + e.getMessage());
                return RUNTIME;
            }
            return DOCTOR;
        } else if (cmd.hasOption("show")) {
            String patientName = cmd.getOptionValue("show");
            try {
                this.clientHttp.getRecordAsDoctor(patientName, userName, userPrivate, serverPublic);
                if (record == null) {
                    return DOCTOR;
                }
            } catch (Exception e) {
                System.out.println("Error: " + e.getMessage());
                return DOCTOR;
            }
        } else if (cmd.hasOption("emergency")) {
            String patientName = cmd.getOptionValue("emergency");
            try {
                String keyfile = this.getDoctorKeyFile(this.userName);
                if (keyfile == null) {
                    throw new Exception("Doctor does not exist");
                }
                this.clientHttp.getRecordInSos(patientName, userName, userPrivate, keyfile);
                if (record == null) {
                    return DOCTOR;
                }
            } catch (Exception e) {
                System.out.println("Error: " + e.getMessage());
                return DOCTOR;
            }
        } else if (cmd.hasOption("add")) {
            String[] addArgs = cmd.getOptionValues("add");
            if (addArgs.length != 2) {
                System.out.println("Error: Invalid number of arguments. Expected <patientName> <filePath>");
                return DOCTOR;
            }
            String patientName = addArgs[0];
            String filePath = addArgs[1];
            try {
                this.clientHttp.addConsultationRecordAsDoctor(CryptoLibrary.readFileToJsonObject(filePath), patientName, userPrivate);
            } catch (Exception e) {
                System.out.println("Error: " + e.getMessage());
                return DOCTOR;
            }
        } else if (cmd.hasOption("quit")) {
            return QUIT;
        } else {
            System.out.println("Error: Invalid command");
            return DOCTOR;
        }

        return DOCTOR;

    }

    public boolean doesPropertyExist(String property) {
        if (property.equals("sex") || property.equals("dateOfBirth") || property.equals("bloodType") || property.equals("knownAllergies")) {
            return true;
        }
        else {
            return false;
        }
    }

    public Key getDoctorPrivateKey(String name) throws Exception {
        if (name.equals("Smith") || name.equals("Johnson") || name.equals("Martins")) {
            return CryptoLibrary.readPrivateKey("../keys/dr" + name + "priv.key");
        }
        else {
            throw new Exception("Doctor does not exist. ");
        }
    }

    public Key getDoctorPublicKey(String name) throws Exception {
        if (name.equals("Smith") || name.equals("Johnson") || name.equals("Martins")) {
            return CryptoLibrary.readPublicKey("../keys/dr" + name + "pub.key");
        }
        else {
            throw new Exception("Doctor does not exist. ");
        }
    }

    public String getDoctorKeyFile(String name) {
        if (name.equals("Smith") || name.equals("Johnson") || name.equals("Martins")) {
            return "../keys/dr" + name + "pub.key";
        }
        else {
            return null;
        }
    }

    
}
