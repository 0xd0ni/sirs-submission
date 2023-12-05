package main.java.pt.tecnico.a01;

import main.java.pt.tecnico.a01.cryptography.CryptoLibrary;
import java.security.Key;


public class MediTrack
{
    // Paths relative to pom.xml
    private static String serverPrivateKeyPath = "../keys/server.privkey";
    private static String serverPublicKeyPath = "../keys/server.pubkey";
    private static String userPrivateKeyPath = "../keys/user.privkey";
    private static String userPublicKeyPath = "../keys/user.pubkey";
    
    
    public static void main(String[] args )
    {
        String inputFile = args[1];
        String outputFile = args.length == 3 ? args[2] : null;

        switch(args[0]) {

            case "protect":
                if(args.length == 3) {
                    System.out.println("[MediTrack - protect]: Protecting file " + inputFile + " to " + outputFile);
                      try {
                        Key serverPrivate = CryptoLibrary.readPrivateKey(serverPrivateKeyPath);
                        Key userPublic = CryptoLibrary.readPublicKey(userPublicKeyPath);
                        CryptoLibrary.protect(inputFile, outputFile, serverPrivate, userPublic);
                    } catch (Exception e) {
                        System.out.println("Error protecting file: " + e);
                    }     
                } else {
                    printUsage();
                }
                break;

            case "unprotect":
                if(args.length == 3) {
                    System.out.println("[MediTrack - unprotect]: Unprotecting file " + inputFile + " to " + outputFile);
                    try {
                        Key serverPublic = CryptoLibrary.readPublicKey(serverPublicKeyPath);
                        Key userPrivate = CryptoLibrary.readPrivateKey(userPrivateKeyPath);
                        CryptoLibrary.unprotect(inputFile, outputFile, serverPublic, userPrivate);
                    } catch(Exception e) {
                        System.out.println("Error unprotecting file " + e);
                    }
                } else {
                    printUsage();
                }
                break;
                
            case "check":
                if(args.length == 2) {
                    System.out.println("[MediTrack - check]: Verifying the integrity and status of the document...");
                    try {
                        Key serverPublic = CryptoLibrary.readPublicKey(serverPublicKeyPath);
                        Key userPrivate = CryptoLibrary.readPrivateKey(userPrivateKeyPath);
                        CryptoLibrary.check(inputFile, serverPublic, userPrivate);

                    } catch(Exception e) {
                        System.out.println("Error checking file: " + e);
                    }    
                } else  {
                    printUsage();
                }
                break;
            
            default:
                printUsage();
                break;
        }
    }

    public static void printUsage() {
        System.out.println("Usage:");
        System.out.println("    protect (input-file) (output-file)");
        System.out.println("    unprotect (input-file) (output-file)");
        System.out.println("    check (input-file) ");

    }
}
