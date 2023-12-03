package main.java.pt.tecnico.a01;

import main.java.pt.tecnico.a01.cryptography.CryptoLibrary;
import java.security.Key;
/**
 * Hello world!
 *
 */
public class MediTrack
{
    private static String serverPrivateKeyPath = "../keys/server.privkey";
    private static String serverPublicKeyPath = "../keys/server.pubkey";
    private static String userPrivateKeyPath = "../keys/user.privkey";
    private static String userPublicKeyPath = "../keys/user.pubkey";
    // Paths relative to pom.xml
    
    public static void main( String[] args )
    {
        if (args.length == 2 && args[0].equals("check")){
            System.out.println("Checking file " + args[1]);
            // Decode from Base64
            // Decrypt with private key
            // Recreate Digest
            // Compare Digest
        }
        else if (args.length == 3) {
            if (args[0].equals("protect")) {
                System.out.println("Protecting file " + args[1] + " to " + args[2]);
                try {
                    Key serverPrivate = CryptoLibrary.readPrivateKey(serverPrivateKeyPath);
                    Key userPublic = CryptoLibrary.readPublicKey(userPublicKeyPath);
                    CryptoLibrary.protect(args[1], args[2], serverPrivate, userPublic);
                } catch (Exception e) {
                    System.out.println("Error protecting file: " + e);
                }
            } else if (args[0].equals("unprotect")) {
                System.out.println("Unprotecting file " + args[1] + " to " + args[2]);
                try {
                    Key serverPublic = CryptoLibrary.readPublicKey(serverPublicKeyPath);
                    Key userPrivate = CryptoLibrary.readPrivateKey(userPrivateKeyPath);
                    CryptoLibrary.unprotect(args[1], args[2], serverPublic, userPrivate);
                } catch (Exception e) {
                    System.out.println("Error unprotecting file: " + e);
                }
            } else {
                printUsage();
            }
        } else {
            printUsage();
        }
    }

    public static void printUsage() {
        System.out.println("Usage:");
        System.out.println("protect *filename*");
        System.out.println("unprotect *filename*");
        System.out.println("check *filename*");
    }
}
