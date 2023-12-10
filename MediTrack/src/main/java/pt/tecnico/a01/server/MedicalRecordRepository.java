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
    
    public void deleteAllById(Iterable<? extends Long> ids) {
        this.database.getCollection("patients").deleteMany(new Document("_id", new Document("$in", ids)));
    }
    
    public void deleteById(Long id) {
        this.database.getCollection("patients").deleteOne(new Document("_id", id));
    }

    public boolean existsById(Long id) {
        return this.database.getCollection("patients").find(new Document("_id", id)).first() != null;
    }

    public Iterable<String> findAll() {
        return () -> StreamSupport.stream(this.database.getCollection("patients").find().spliterator(), false).map(document -> document.toJson()).iterator();
    }

    public Iterable<String> findAllById(Iterable<Long> ids) {
        ArrayList<String> records = new ArrayList<String>();
        ids.forEach(id -> {
            Document document = this.database
            .getCollection("patients")
            .find(new Document("_id", id))
            .first();
            if (document != null) {
                records.add(document.toJson());
            }
        });
        return records;
    }

    public Optional<String> findById(Long id) {
        return Optional.ofNullable(this.database.getCollection("patients").find(new Document("_id", id)).first().toJson());
    }

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

    public Optional<String> find(String patientName) {
        return Optional.ofNullable(this.database.getCollection("patients").find(new Document("name", patientName)).first().toJson());
    }
}