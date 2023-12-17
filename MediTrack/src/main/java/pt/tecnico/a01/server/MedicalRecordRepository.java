package main.java.pt.tecnico.a01.server;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoDatabase;

import java.util.ArrayList;
import java.util.stream.StreamSupport;
import java.util.Optional;

import org.bson.Document;

public class MedicalRecordRepository{
    private MongoClient mongoClient;

    private MongoDatabase database;

    private String databaseName;

    public MedicalRecordRepository(String url, String databaseName) {
        super();
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
        this.database
        .getCollection("patients")
        .insertOne(Document.parse(record));
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
}