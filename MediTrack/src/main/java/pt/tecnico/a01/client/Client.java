package main.java.pt.tecnico.a01.client;

import main.java.pt.tecnico.a01.cryptography.CryptoLibrary;

import javafx.application.Application;
import javafx.stage.Stage;
import javafx.scene.Scene;
import javafx.scene.layout.VBox;
import javafx.scene.web.WebView;

import okhttp3.Request;

public class Client extends Application {

    private WebView webView;

    public static void main(String[] args) {
        launch(args);
    }

    public void start(Stage primaryStage) {

        this.webView = new WebView();

        webView.getEngine().load("./src/main/java/pt/tecnico/a01/client/patient.html");

        VBox vBox = new VBox(webView);
        Scene scene = new Scene(vBox, 960, 600);

        primaryStage.setScene(scene);
        primaryStage.show();

    }

    public void loadPatient() {
        webView.getEngine().load("./src/main/java/pt/tecnico/a01/client/patient.html");
    }

    public void loadDoctor() {
        webView.getEngine().load("./src/main/java/pt/tecnico/a01/client/doctor.html");
    }

    public void loadAdmin() {
        webView.getEngine().load("./src/main/java/pt/tecnico/a01/client/admin.html");
    }

    public void getRecordAsPatient(String name) {
        this.getRecord(name);
    }

    public void getRecord(String name) {

    }
}