package main.java.pt.tecnico.a01.server;

import static spark.Spark.*;


public class ServerApplication {

	public static void main(String[] args) {
		try {
			MedicalRecordService medicalRecordService = new MedicalRecordService();
			port(4000);
			get("/:name", (req, res) -> {
				try {
					return medicalRecordService.getMedicalRecord(req.params(":name"));
				} catch (Exception e) {
					res.status(404);
					return e.getMessage();
				}
			});

			put("/:name", (req, res) -> {
				try {
					return medicalRecordService.saveMedicalRecord(req.body());
				} catch (Exception e) {
					res.status(404);
					return e.getMessage();
				}
			});
			put("/commit/:name", (req, res) -> {
				try {
					return medicalRecordService.changeProtections(req.params(":name"), req.body());
				} catch (Exception e) {
					res.status(404);
					return e.getMessage();
				}
			});
		} catch(Exception e) {
			System.out.println("Error starting server: " + e);
		}
	}
	
}
