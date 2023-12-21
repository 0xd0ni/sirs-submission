package main.java.pt.tecnico.a01.server;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoDatabase;
import com.mongodb.connection.SslSettings;
import com.mongodb.MongoClientSettings;
import java.security.NoSuchAlgorithmException;
import javax.net.ssl.SSLContext;
import com.mongodb.ConnectionString;
import com.mongodb.client.model.Updates;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.result.UpdateResult;
import com.mongodb.client.model.Filters;
import java.util.ArrayList;
import java.util.Map;
import java.util.Iterator;
import java.util.stream.StreamSupport;
import java.util.Optional;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

import org.bson.Document;
import org.bson.conversions.Bson;

public class MedicalRecordRepository{
    private MongoClient mongoClient;

    private MongoDatabase database;
    private String databaseName;

    public MedicalRecordRepository(String url, String databaseName) throws NoSuchAlgorithmException {
        super();
        //System.setProperty("javax.net.ssl.trustStore", "/home/vagrant/project/scripts/truststore.ts");
        //System.setProperty("javax.net.ssl.trustStorePassword", "mypasswd");
        //MongoClientSettings settings = MongoClientSettings.builder().applyConnectionString(new ConnectionString(url))
        //.applyToSslSettings(builder -> {
        //         builder.enabled(true);
        //     })
        //.build();'''
        this.mongoClient = MongoClients.create(url);
        this.databaseName = databaseName;
        this.database = this.mongoClient.getDatabase(this.databaseName);
    }
    
    public long count() {
        return this.database.getCollection("patients").countDocuments();
    }
    
    public void delete(String record) {
        this.database.getCollection("patients").deleteOne(Document.parse(record));
    }
    
    public void deleteAll() {
        this.database.getCollection("patients").deleteMany(new Document());
    }
    
    public void deleteAll(Iterable<? extends String> records) {
        records.forEach(record -> {this.database.getCollection("patients").deleteOne(Document.parse(record));});
    }
    
    public void deleteByName(String patientName) {
        this.database.getCollection("patients").deleteOne(new Document("name", patientName));
    }

    public Optional<String> find(String patientName) {
        Document record = this.database.getCollection("patients").find(new Document("name", patientName)).first();
        if (record == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(record.toJson());
    }

    public Iterable<String> findAll() {
        return () -> StreamSupport.stream(this.database.getCollection("patients").find().spliterator(), false).map(document -> document.toJson()).iterator();
    }

    // Verificar se o registo é substituido
    public String save(String record) {
        if (this.find(new Gson().fromJson(record, JsonObject.class).get("name").getAsString()).isPresent()) {
            this.database.getCollection("patients").replaceOne(new Document("name", new Gson().fromJson(record, JsonObject.class).get("name").getAsString()), Document.parse(record));
        }
        else {
            this.database
            .getCollection("patients")
            .insertOne(Document.parse(record));
        }
        return record;
    }

    public Iterable<String> saveAll(Iterable<String> records) {
        ArrayList<Document> documents = new ArrayList<Document>();
        records.forEach(record -> {documents.add(Document.parse(record));});
        this.database
        .getCollection("patients")
        .insertMany(documents);
        return records;
    }

    /**
     * 
     * @param doctorName
     * @param patientName
     * @return string of the form {"field": "value1", "field": "value2"}
     */
    public String findKeys(String doctorName, String patientName) {
        // Testar gets com coleções vazias
        Document patientKeys = this.database.getCollection(doctorName).find(new Document("name", patientName)).first();
        if (patientKeys == null) {
            return null;
        }
        return patientKeys.toJson();
    }

    /**
     * 
     * @param doctorName
     * @param patientName
     * @param keys string of the form {"field": "value1", "field": "value2"}
     * @return keys
     */
    public String addKeys(String doctorName, String patientName, String keys) {
        String currentKeys = findKeys(doctorName, patientName);
        if (currentKeys == null) {
            this.database.getCollection(doctorName).insertOne(new Document().append("name", patientName).append("keys", Document.parse(keys)));
            return keys;
        }
        currentKeys = new Gson().toJson(new Gson().fromJson(currentKeys, JsonObject.class).get("keys"));
        Document keysDocument = Document.parse(keys);
        Document newKeysDocument = Document.parse(currentKeys);
        for (String key : keysDocument.keySet()) {
            newKeysDocument.put(key, keysDocument.get(key));
        }
        System.out.println("The new keys ares:" + newKeysDocument.toJson());
        System.out.println("-_-_-_-_-_-_-_-_-_-_---_-_-_-_-_-_-_-_-_-_-_-_-_-_-----_-_-_-_---_-_-_-_-_-_");
        Bson updates = Updates.set("keys", newKeysDocument);
        UpdateResult res = this.database.getCollection(doctorName).updateOne(new Document("name", patientName), updates);
        System.out.println("The result of the update is: " + res.toString());
        System.out.println("The update did:" + res.wasAcknowledged());
        return keys;
    }
}