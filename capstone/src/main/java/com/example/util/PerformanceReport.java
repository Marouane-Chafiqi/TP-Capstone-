package com.example.util;

import com.example.model.Salle;
import com.example.model.StatutReservation;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Supplier;

public class PerformanceReport {

    private final EntityManagerFactory emf;
    private final SessionFactory sessionFactory;
    private final Statistics stats;
    private final Map<String, TestResult> results = new LinkedHashMap<>();

    public PerformanceReport(EntityManagerFactory emf) {
        this.emf = emf;
        this.sessionFactory = emf.unwrap(SessionFactory.class);
        this.stats = sessionFactory.getStatistics();
    }

    public void runPerformanceTests() {
        System.out.println("\nExecution des tests de performance...");

        testPerformance("Recherche de salles disponibles", () -> {
            EntityManager em = emf.createEntityManager();
            try {
                LocalDateTime start = LocalDateTime.now().plusDays(1);
                return em.createQuery(
                        "SELECT s FROM Salle s WHERE s.id NOT IN " +
                        "(SELECT r.salle.id FROM Reservation r " +
                        "WHERE r.statut <> :annulee AND r.dateDebut < :end AND r.dateFin > :start)", Salle.class)
                        .setParameter("annulee", StatutReservation.ANNULEE)
                        .setParameter("start", start)
                        .setParameter("end", start.plusHours(2))
                        .getResultList();
            } finally {
                em.close();
            }
        });

        testPerformance("Recherche multi-criteres", () -> {
            EntityManager em = emf.createEntityManager();
            try {
                return em.createQuery(
                        "SELECT DISTINCT s FROM Salle s JOIN s.equipements e " +
                        "WHERE s.capacite >= :capacite AND s.batiment = :batiment AND e.id = :equipementId", Salle.class)
                        .setParameter("capacite", 30)
                        .setParameter("batiment", "Batiment B")
                        .setParameter("equipementId", 1L)
                        .getResultList();
            } finally {
                em.close();
            }
        });

        testPerformance("Pagination", () -> {
            EntityManager em = emf.createEntityManager();
            try {
                return em.createQuery("SELECT s FROM Salle s ORDER BY s.id", Salle.class)
                        .setFirstResult(0)
                        .setMaxResults(10)
                        .getResultList();
            } finally {
                em.close();
            }
        });

        testPerformance("Acces repete avec cache", () -> {
            Object result = null;
            for (int i = 0; i < 100; i++) {
                EntityManager em = emf.createEntityManager();
                try {
                    result = em.find(Salle.class, 1L);
                } finally {
                    em.close();
                }
            }
            return result;
        });

        testPerformance("Requete avec JOIN FETCH", () -> {
            EntityManager em = emf.createEntityManager();
            try {
                return em.createQuery(
                        "SELECT DISTINCT s FROM Salle s LEFT JOIN FETCH s.equipements WHERE s.capacite > 20",
                        Salle.class)
                        .getResultList();
            } finally {
                em.close();
            }
        });

        generateReport();
    }

    private void testPerformance(String nom, Supplier<Object> test) {
        System.out.println("Execution du test : " + nom);

        sessionFactory.getCache().evictAllRegions();
        stats.clear();

        long debut = System.currentTimeMillis();
        Object result = test.get();
        long duree = System.currentTimeMillis() - debut;

        TestResult r = new TestResult();
        r.executionTime = duree;
        r.sqlCount = stats.getPrepareStatementCount();
        r.entityLoadCount = stats.getEntityLoadCount();
        r.cacheHitCount = stats.getSecondLevelCacheHitCount();
        r.cacheMissCount = stats.getSecondLevelCacheMissCount();
        if (result instanceof Collection) {
            r.resultSize = ((Collection<?>) result).size();
        } else {
            r.resultSize = result != null ? 1 : 0;
        }
        results.put(nom, r);
    }

    private void generateReport() {
        StringBuilder sb = new StringBuilder();
        sb.append("=== RAPPORT DE PERFORMANCE ===\n");
        sb.append("Date : ").append(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))).append("\n");
        sb.append("==============================\n\n");

        for (Map.Entry<String, TestResult> entry : results.entrySet()) {
            TestResult r = entry.getValue();
            long total = r.cacheHitCount + r.cacheMissCount;
            double ratio = total > 0 ? (double) r.cacheHitCount / total * 100 : 0;

            sb.append("Test : ").append(entry.getKey()).append("\n");
            sb.append("Temps d'execution : ").append(r.executionTime).append(" ms\n");
            sb.append("Requetes SQL : ").append(r.sqlCount).append("\n");
            sb.append("Entites chargees : ").append(r.entityLoadCount).append("\n");
            sb.append("Hits du cache : ").append(r.cacheHitCount).append("\n");
            sb.append("Miss du cache : ").append(r.cacheMissCount).append("\n");
            sb.append("Taille du resultat : ").append(r.resultSize).append("\n");
            sb.append("Ratio de hit du cache : ").append(String.format("%.2f", ratio)).append("%\n");
            sb.append("------------------------------\n\n");
        }

        boolean cacheAOptimiser = false;
        boolean requetesAOptimiser = false;
        for (TestResult r : results.values()) {
            if (r.cacheHitCount < r.cacheMissCount && r.sqlCount > 5) cacheAOptimiser = true;
            if (r.sqlCount > 10 || r.executionTime > 500) requetesAOptimiser = true;
        }

        sb.append("=== RECOMMANDATIONS ===\n");
        if (cacheAOptimiser) {
            sb.append("- Verifier la configuration du cache de second niveau\n");
            sb.append("- Utiliser le cache de requete pour les requetes repetitives\n");
        }
        if (requetesAOptimiser) {
            sb.append("- Utiliser JOIN FETCH pour eviter les problemes N+1\n");
            sb.append("- Creer des index sur les colonnes utilisees dans les WHERE\n");
        }
        if (!cacheAOptimiser && !requetesAOptimiser) {
            sb.append("- Aucun probleme majeur detecte\n");
        }
        sb.append("- Surveiller les performances en production (monitoring)\n");

        String fileName = "performance_report_" +
                LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")) + ".txt";
        try (PrintWriter writer = new PrintWriter(new FileWriter(fileName))) {
            writer.print(sb);
        } catch (IOException e) {
            System.out.println("Erreur lors de l'ecriture du rapport : " + e.getMessage());
        }

        System.out.println("\n" + sb);
        System.out.println("Rapport enregistre dans : " + fileName);
    }

    private static class TestResult {
        long executionTime;
        long sqlCount;
        long entityLoadCount;
        long cacheHitCount;
        long cacheMissCount;
        int resultSize;
    }
}
