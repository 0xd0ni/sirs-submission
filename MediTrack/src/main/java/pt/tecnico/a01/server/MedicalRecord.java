package com.example.server;
import java.util.HashSet;
import java.util.Set;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;


@Entity
public class MedicalRecord {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;
    private String name;
    private String sex;
    private String dateOfBirth;
    private String bloodType;
    private Set<String> knownAllergies = new HashSet<>();
    //private Set<consultationRecord> consultationRecords=new HashSet<>();

    protected MedicalRecord() {}

    public MedicalRecord(String name, String sex, String dateOfBirth, String bloodType, Set<String> knownAllergies) {
        this.name = name;
        this.sex = sex;
        this.dateOfBirth = dateOfBirth;
        this.bloodType = bloodType;
        this.knownAllergies = knownAllergies;
    }


    // Getters and setters

    public Long getId() {
        return this.id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return this.name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getSex() {
        return this.sex;
    }

    public void setSex(String sex) {
        this.sex = sex;
    }

    public String getDateOfBirth() {
        return this.dateOfBirth;
    }

    public void setDateOfBirth(String dateOfBirth) {
        this.dateOfBirth = dateOfBirth;
    }

    public String getBloodType() {
        return this.bloodType;
    }

    public void setBloodType(String bloodType) {
        this.bloodType = bloodType;
    }

    public Set<String> getKnownAllergies() {
        return this.knownAllergies;
    }

    public void setKnownAllergies(Set<String> knownAllergies) {
        this.knownAllergies = knownAllergies;
    }


}