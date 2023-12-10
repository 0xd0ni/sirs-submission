package main.java.pt.tecnico.a01.server;

import static spark.Spark.*;


public class ServerApplication {

	public static void main(String[] args) {
		MedicalRecordService medicalRecordService = new MedicalRecordService();
		get("/:name", (req, res) -> {
			return medicalRecordService.getMedicalRecord(req.params(":name"));
		});

		put("/:name", (req, res) -> {
			return medicalRecordService.saveMedicalRecord(req.body());
		});
	}
	
}
