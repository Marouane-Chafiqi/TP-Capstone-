package com.example;

import com.example.service.SalleService;
import com.example.test.TestScenarios;
import com.example.util.DataInitializer;
import com.example.util.DatabaseMigrationTool;
import com.example.util.PerformanceReport;

import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;

public class App {

    public static void main(String[] args) {
        System.out.println("=== APPLICATION DE RESERVATION DE SALLES ===\n");

        EntityManagerFactory emf = Persistence.createEntityManagerFactory("capstone");

        try {
            new DataInitializer(emf).initializeData();

            SalleService salleService = new SalleService(emf);
            new TestScenarios(emf, salleService).runAllTests();

            new DatabaseMigrationTool("jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1", "sa", "").executeMigration();

            new PerformanceReport(emf).runPerformanceTests();
        } finally {
            emf.close();
        }
    }
}
