package main.java.pt.tecnico.a01;

import main.java.pt.tecnico.a01.cryptography.CryptoLibrary;
import java.security.Key;
import java.util.Arrays;


public class MediTrack
{
    // Paths relative to pom.xml
    private static String serverPrivateKeyPath = "../keys/server.privkey";
    private static String serverPublicKeyPath = "../keys/server.pubkey";
    private static String userPrivateKeyPath = "../keys/user.privkey";
    private static String userPublicKeyPath = "../keys/user.pubkey";
    private static String sosPublicKeyPath = "../keys/sospub.key";
    private static String sosPrivateKeyPath = "../keys/sospriv.key";

    private static String MESSAGE_HELP = "[MediTrack - help]: Usage info: ";
    private static String MESSAGE_USAGE = " protect (input-file) (output-file) ...\n" +
                                            "unprotect (input-file) (output-file) ...\n" +
                                            "check (input-file) \n" +
                                            "sign (input-file) (output-file) (physician-private-key)\n" +
                                            "verify-sign (input-file) (physician-public-key)\n"+
                                            "note that: `...` denotes 0 or more arguments are expected.\n" +
                                            "these arguments can range from:\n" +
                                            "name\n" +
                                            "sex\n" +
                                            "dateOfBirth\n" +
                                            "bloodType\n" +
                                            "KnownAllergies\n" +
                                            "ConsultationRecords\n";

    private static String MESSAGE_PROTECT= "[MediTrack - protect]: Protecting file ";
    private static String MESSAGE_UNPROTECT = "[MediTrack - unprotect]: Unprotecting file ";
    private static String MESSAGE_CHECK = "[MediTrack - check]: Verifying the integrity and status of the document.";
    private static String MESSAGE_SIGN = "[MediTrack - sign]: Signing a consultation record ";
    private static String MESSAGE_VERIFY_SIGN = "[MediTrack - verify-sign]: Verifying a consultationRecord ";
    private static String MESSAGE_TO = " to ";
    private static String ERROR_PROTECT = "Error protecting file: ";
    private static String ERROR_UNPROTECT = "Error unprotecting file ";
    private static String ERROR_CHECK = "Error checking file: ";
    private static String ERROR_VERIFY_SIGN = "Error veryfying the signature a consultation record ";
    private static String ERROR_SIGN = "Error veryfying the signature a consultation record ";
 
    public static void main(String[] args )
    {
        String inputFile = args[1];
        String outputFile = args.length >= 3 ? args[2] : null;
     
        switch(args[0]) {

            case "protect":
                if(args.length >= 3) {
                    System.out.println(MESSAGE_PROTECT + inputFile + MESSAGE_TO + outputFile);
                    String[] fields = args.length >= 4 ? Arrays.copyOfRange(args, 3, args.length) : new String[0];
                    try {
                        Key serverPrivate = CryptoLibrary.readPrivateKey(serverPrivateKeyPath);
                        Key userPublic = CryptoLibrary.readPublicKey(userPublicKeyPath);
                        Key sosPublic = CryptoLibrary.readPublicKey(sosPublicKeyPath);
                        CryptoLibrary.protect(inputFile, outputFile, serverPrivate, userPublic, sosPublic, fields);
                    } catch (Exception e) {
                        System.out.println(ERROR_PROTECT + e);
                    }     
                } else {
                    printUsage();
                }
                break;

            case "unprotect":
                if(args.length >= 3) {
                    System.out.println(MESSAGE_UNPROTECT + inputFile + MESSAGE_TO + outputFile);
                    String[] fields = args.length >= 4 ? Arrays.copyOfRange(args, 3, args.length) : new String[0];
                    try {
                        Key userPrivate = CryptoLibrary.readPrivateKey(userPrivateKeyPath);
                        Key sosPrivate = CryptoLibrary.readPrivateKey(sosPrivateKeyPath);
                        CryptoLibrary.unprotect(inputFile, outputFile, userPrivate, fields);
                    } catch(Exception e) {
                        System.out.println(ERROR_UNPROTECT + e);
                    }
                } else {
                    printUsage();
                }
                break;
            
            case "sign":
                if(args.length >= 3) {
                    System.out.println(MESSAGE_SIGN +  inputFile + MESSAGE_TO + outputFile);

                    String physicianPrivateKeyPath = args[3];
                    try {
                        Key physicianPrivate = CryptoLibrary.readPrivateKey(physicianPrivateKeyPath);
                        CryptoLibrary.signConsultationRecord(inputFile, outputFile, physicianPrivate);
                    } catch(Exception e) {
                        System.out.println(ERROR_SIGN + e);
                    }
                } else {
                    printUsage();
                }
                break;
            
            case "verify-sign":
                if(args.length == 3) {
                    System.out.println(MESSAGE_VERIFY_SIGN + inputFile);
                    String physicianPublicKeyPath = args[2];
                    try {
                        Key physicianPublic = CryptoLibrary.readPublicKey(physicianPublicKeyPath);
                        CryptoLibrary.verifyConsultationRecord(inputFile, physicianPublic);
                    } catch(Exception e) {
                        System.out.println(ERROR_VERIFY_SIGN + e);
                    }
                } else {
                    printUsage();
                }
                break;
     
            case "check":
                if(args.length == 2) {
                    System.out.println(MESSAGE_CHECK);
                    try {
                        Key userPrivate = CryptoLibrary.readPrivateKey(userPrivateKeyPath);
                        CryptoLibrary.check(inputFile, userPrivate);

                    } catch(Exception e) {
                        System.out.println(ERROR_CHECK + e);
                    }    
                } else  {
                    printUsage();
                }
                break;      
            case "help":
                printUsage();
                break;
            
            default:
                printUsage();
                break;
        }
    }

    public static void printUsage() {
        System.out.println(MESSAGE_HELP);
        System.out.println(MESSAGE_USAGE);
    }
}
