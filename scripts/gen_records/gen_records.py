"""Script that generates MediTrack Records with varying number of consultation records"""
import json
import random
from datetime import datetime, timedelta



def generate_date_of_birth():

    start_date = datetime(1940, 1, 1)
    end_date = datetime(2020, 12, 31)
    time_between_dates = end_date - start_date
    days_between_dates = time_between_dates.days
    random_number_of_days = random.randrange(days_between_dates)
    return (start_date + timedelta(days=random_number_of_days)).strftime("%Y-%m-%d")


def generate_consultation_records():
    
    specialties = ["Cardiology", "Neurology", "Pediatrics", "Ophthalmology", "Endocrinology", 
                   "Gynecology", "Urology", "Psychiatry", "Oncology", "Dentistry","Pediatrics"]
    treatments = ["Routine Check-up", "Minor Surgery", "Prescribed Medication", "Therapy Sessions", 
                  "Emergency Procedure", "Vaccination", "Health Counseling", "Diagnostic Tests", 
                  "Physical Therapy", "Dental Cleaning"]
    record_count = random.randint(1, 15) 
    records = []
    for _ in range(record_count):
        record_date = generate_date_of_birth()  
        specialty = random.choice(specialties)
        doctor_name = f"Dr. {''.join(random.sample('ABCDEFGHIJKLMNOPQRSTUVWXYZ', 2))}."
        practice = f"{specialty} {''.join(random.choice(['Health Center','Clinic']))}"
        treatment_summary = random.choice(treatments)
        records.append({
            "date": record_date,
            "medicalSpecialty": specialty,
            "doctorName": doctor_name,
            "practice": practice,
            "treatmentSummary": treatment_summary
        })
    return records


def generate_patient_data():

    forenames = read_data_record("data/forenames-pt.txt")
    surnames = read_data_record("data/surnames-pt.txt")
    blood_types = read_data_record("data/blood-types.txt")
    allergies = read_data_record("data/allergies.txt")
    

    patients = []
    for _ in range(10):
        name = f"{random.choice(forenames)} {random.choice(surnames)}"
        sex = random.choice(["Male", "Female","Nonconforming"])
        dob = generate_date_of_birth()
        blood_type = random.choice(blood_types)
        known_allergies = random.sample(allergies, random.randint(0, 5))
        consultation_records = generate_consultation_records()

        patient_data = {
            "patient": {
                "name": name,
                "sex": sex,
                "dateOfBirth": dob,
                "bloodType": blood_type,
                "knownAllergies": known_allergies,
                "consultationRecords": consultation_records
            }
        }
        patients.append(patient_data)

    return patients

def read_data_record(path):

    with open(path, "r", encoding="utf-8") as f:
        return f.read().splitlines()


def write_patients_to_file(patients):
    for index, patient in enumerate(patients):
        with open(f"records/MediTrackRecord{index}.json", "w", encoding="utf-8") as patient_data:
            json.dump(patient, patient_data, indent=2)


if __name__ == "__main__":
    patients = generate_patient_data()
    write_patients_to_file(patients=patients)


