package pt.tecnico.a01;

/**
 * Hello world!
 *
 */
public class MediTrack
{
    public static void main( String[] args )
    {
        if (args.length != 2) {
            printUsage();
        } else if (args.length == 2){
            if (args[0].equals("protect")) {
                System.out.println("Protecting file " + args[1]);
                // Create Digest
                // Encrypt with public key
                // Encode to Base64

            } else if (args[0].equals("unprotect")) {
                System.out.println("Unprotecting file " + args[1]);
                // Decode from Base64
                // Decrypt with private key
                // Compare Digest ?

            } else if (args[0].equals("check")) {
                System.out.println("Checking file " + args[1]);
                // Decode from Base64
                // Decrypt with private key
                // Recreate Digest
                // Compare Digest

            } else {
                printUsage();
            }
        }
    }

    public static void printUsage() {
        System.out.println("Usage:");
        System.out.println("protect *filename*");
        System.out.println("unprotect *filename*");
        System.out.println("check *filename*");
    }
}
