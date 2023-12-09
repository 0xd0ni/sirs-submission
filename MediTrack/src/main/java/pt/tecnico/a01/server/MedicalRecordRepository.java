package main.java.pt.tecnico.a01.server;
import org.springframework.data.repository.CrudRepository;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoDatabase;

import java.util.ArrayList;
import java.util.stream.StreamSupport;
import java.util.Optional;

import org.bson.Document;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

public class MedicalRecordRepository implements CrudRepository<JsonObject, Long> {
    private MongoClient mongoClient;

    private MongoDatabase database;

    private String databaseName;

    private Gson gson;

    
    public MedicalRecordRepository(String url, String databaseName) {
        super();
        this.mongoClient = MongoClients.create(url);
        this.databaseName = databaseName;
        this.database = this.mongoClient.getDatabase(this.databaseName);
        this.gson = new Gson();
    }

    @Override
    public long count() {
        return this.database.getCollection("patients").countDocuments();
    }

    @Override
    public void delete(JsonObject record) {
        this.database.getCollection("patients").deleteOne(Document.parse(this.gson.toJson(record)));
    }
    
    @Override
    public void deleteAll() {
        this.database.getCollection("patients").deleteMany(new Document());
    }

    @Override
    public void deleteAll(Iterable<? extends JsonObject> records) {
        this.database.getCollection("patients").deleteMany(Document.parse(this.gson.toJson(records)));
    }

    @Override
    public void deleteAllById(Iterable<? extends Long> ids) {
        this.database.getCollection("patients").deleteMany(new Document("_id", new Document("$in", ids)));
    }

    @Override
    public void deleteById(Long id) {
        this.database.getCollection("patients").deleteOne(new Document("_id", id));
    }

    @Override
    public boolean existsById(Long id) {
        return this.database.getCollection("patients").find(new Document("_id", id)).first() != null;
    }

    @Override
    public Iterable<JsonObject> findAll() {
        return () -> StreamSupport.stream(this.database.getCollection("patients").find().spliterator(), false).map(document -> this.gson.fromJson(document.toJson(), JsonObject.class)).iterator();
    }

    @Override
    public Iterable<JsonObject> findAllById(Iterable<Long> ids) {
        ArrayList<JsonObject> records = new ArrayList<JsonObject>();
        ids.forEach(id -> {
            Document document = this.database
            .getCollection("patients")
            .find(new Document("_id", id))
            .first();
            if (document != null) {
                records.add(this.gson.fromJson(document.toJson(), JsonObject.class));
            }
        });
        return records;
    }

    @Override
    public Optional<JsonObject> findById(Long id) {
        return Optional.ofNullable(this.gson.fromJson(this.database.getCollection("patients").find(new Document("_id", id)).first().toJson(), JsonObject.class));
    }

    @Override
    public <S extends JsonObject> S save(S record) {
        this.database
        .getCollection("patients")
        .insertOne(Document.parse(this.gson.toJson(record)));
        return record;
    }

    @Override
    public <S extends JsonObject> Iterable<S> saveAll(Iterable<S> records) {
        ArrayList<Document> documents = new ArrayList<Document>();
        records.forEach(record -> {documents.add(Document.parse(this.gson.toJson(record)));});
        this.database
        .getCollection("patients")
        .insertMany(documents);
        return records;
    }

    public Optional<JsonObject> find(String patientName) {
        return Optional.ofNullable(this.gson.fromJson(this.database.getCollection("patients").find(new Document("name", patientName)).first().toJson(), JsonObject.class));
    }
}