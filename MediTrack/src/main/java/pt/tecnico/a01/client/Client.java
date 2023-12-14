package main.java.pt.tecnico.a01.client;


public class Client {

    public static void main(String[] args) {
        ClientSession client = new ClientSession();
        client.start(args);
    }
}