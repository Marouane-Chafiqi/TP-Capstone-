package com.example.util;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.stream.Collectors;

public class DatabaseMigrationTool {

    private final String jdbcUrl;
    private final String username;
    private final String password;

    public DatabaseMigrationTool(String jdbcUrl, String username, String password) {
        this.jdbcUrl = jdbcUrl;
        this.username = username;
        this.password = password;
    }

    public void executeMigration() {
        System.out.println("Demarrage de la migration de la base de donnees...");

        try (Connection connection = DriverManager.getConnection(jdbcUrl, username, password);
             InputStream in = getClass().getClassLoader().getResourceAsStream("migration_v2.sql")) {

            if (in == null) {
                System.out.println("Script migration_v2.sql introuvable dans les ressources");
                return;
            }

            String script = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))
                    .lines()
                    .filter(ligne -> !ligne.trim().startsWith("--"))
                    .collect(Collectors.joining("\n"));

            int ok = 0;
            int erreurs = 0;
            try (Statement statement = connection.createStatement()) {
                for (String instruction : script.split(";")) {
                    String sql = instruction.trim();
                    if (sql.isEmpty()) {
                        continue;
                    }
                    try {
                        statement.execute(sql);
                        ok++;
                    } catch (SQLException e) {
                        erreurs++;
                        System.out.println("  Erreur (ignoree) : " + e.getMessage().split("\n")[0]);
                    }
                }
            }
            System.out.println("Migration terminee : " + ok + " instruction(s) reussie(s), " + erreurs + " erreur(s)");

        } catch (Exception e) {
            System.out.println("Erreur lors de la migration : " + e.getMessage());
        }
    }
}
