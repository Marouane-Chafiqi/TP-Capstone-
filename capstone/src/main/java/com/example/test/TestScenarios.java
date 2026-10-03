package com.example.test;

import com.example.model.Reservation;
import com.example.model.Salle;
import com.example.model.StatutReservation;
import com.example.service.SalleService;
import com.example.util.PaginationResult;
import org.hibernate.SessionFactory;
import org.hibernate.StaleStateException;
import org.hibernate.stat.Statistics;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.OptimisticLockException;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

public class TestScenarios {

    private final EntityManagerFactory emf;
    private final SalleService salleService;
    private final SessionFactory sessionFactory;
    private final Statistics stats;

    public TestScenarios(EntityManagerFactory emf, SalleService salleService) {
        this.emf = emf;
        this.salleService = salleService;
        this.sessionFactory = emf.unwrap(SessionFactory.class);
        this.stats = sessionFactory.getStatistics();
    }

    public void runAllTests() {
        System.out.println("\n=== EXECUTION DES SCENARIOS DE TEST ===");
        testRechercheDisponibilite();
        testRechercheMultiCriteres();
        testPagination();
        testOptimisticLocking();
        testCachePerformance();
        System.out.println("\n=== TOUS LES TESTS SONT TERMINES ===");
    }

    // ---------------------------------------------------------------- TEST 1
    private void testRechercheDisponibilite() {
        System.out.println("\n=== TEST 1 : RECHERCHE DE DISPONIBILITE ===");

        LocalDateTime debut = LocalDateTime.now().plusDays(1).withHour(9).withMinute(0).withSecond(0).withNano(0);
        LocalDateTime fin = debut.plusHours(3);

        System.out.println("Salles disponibles entre " + debut + " et " + fin);
        List<Salle> libres = salleService.findAvailableRooms(debut, fin);
        System.out.println("Nombre de salles disponibles : " + libres.size());
        for (int i = 0; i < Math.min(5, libres.size()); i++) {
            Salle s = libres.get(i);
            System.out.println("- " + s.getNom() + " (capacite " + s.getCapacite() + ", " + s.getBatiment() + ")");
        }
        if (libres.size() > 5) {
            System.out.println("... et " + (libres.size() - 5) + " autres salles");
        }

        // Creneau d'une reservation confirmee existante : sa salle doit etre exclue
        EntityManager em = emf.createEntityManager();
        Reservation reservation;
        try {
            reservation = em.createQuery(
                    "SELECT r FROM Reservation r JOIN FETCH r.salle WHERE r.statut = :statut ORDER BY r.id",
                    Reservation.class)
                    .setParameter("statut", StatutReservation.CONFIRMEE)
                    .setMaxResults(1)
                    .getSingleResult();
        } finally {
            em.close();
        }

        System.out.println("\nCreneau deja reserve : " + reservation.getDateDebut() + " a " + reservation.getDateFin());
        System.out.println("Salle reservee : " + reservation.getSalle().getNom());

        List<Salle> libres2 = salleService.findAvailableRooms(reservation.getDateDebut(), reservation.getDateFin());
        boolean exclue = true;
        for (Salle s : libres2) {
            if (s.getId().equals(reservation.getSalle().getId())) {
                exclue = false;
            }
        }
        System.out.println("Nombre de salles disponibles : " + libres2.size());
        System.out.println("La salle reservee est-elle exclue des resultats ? " + exclue);
    }

    // ---------------------------------------------------------------- TEST 2
    private void testRechercheMultiCriteres() {
        System.out.println("\n=== TEST 2 : RECHERCHE MULTI-CRITERES ===");

        Map<String, Object> c1 = new HashMap<>();
        c1.put("capaciteMin", 30);
        c1.put("equipement", 2L); // Ecran interactif
        System.out.println("Capacite >= 30 et equipee d'un ecran interactif :");
        afficher(salleService.searchRooms(c1));

        Map<String, Object> c2 = new HashMap<>();
        c2.put("batiment", "Batiment C");
        c2.put("etage", 2);
        System.out.println("\nBatiment C, etage 2 :");
        afficher(salleService.searchRooms(c2));

        Map<String, Object> c3 = new HashMap<>();
        c3.put("capaciteMin", 20);
        c3.put("capaciteMax", 50);
        c3.put("batiment", "Batiment B");
        c3.put("equipement", 7L); // Ordinateur fixe
        System.out.println("\nCapacite 20-50, Batiment B, avec ordinateur fixe :");
        afficher(salleService.searchRooms(c3));
    }

    private void afficher(List<Salle> salles) {
        System.out.println("Nombre de salles trouvees : " + salles.size());
        for (Salle s : salles) {
            System.out.println("- " + s.getNom() + " (capacite " + s.getCapacite()
                    + ", " + s.getBatiment() + ", etage " + s.getEtage() + ")");
        }
    }

    // ---------------------------------------------------------------- TEST 3
    private void testPagination() {
        System.out.println("\n=== TEST 3 : PAGINATION ===");

        int taillePage = 5;
        int totalPages = salleService.getTotalPages(taillePage);
        System.out.println("Nombre total de pages : " + totalPages);

        for (int page = 1; page <= totalPages; page++) {
            System.out.println("\nPage " + page + " :");
            for (Salle s : salleService.getPaginatedRooms(page, taillePage)) {
                System.out.println("- " + s.getNom() + " (capacite " + s.getCapacite() + ", " + s.getBatiment() + ")");
            }
        }

        PaginationResult<Salle> resultat = new PaginationResult<>(
                salleService.getPaginatedRooms(1, taillePage), 1, taillePage, salleService.countRooms());

        System.out.println("\nPaginationResult :");
        System.out.println("Page courante : " + resultat.getCurrentPage());
        System.out.println("Taille de la page : " + resultat.getPageSize());
        System.out.println("Nombre total de pages : " + resultat.getTotalPages());
        System.out.println("Nombre total d'elements : " + resultat.getTotalItems());
        System.out.println("Page suivante : " + resultat.hasNext());
        System.out.println("Page precedente : " + resultat.hasPrevious());
    }

    // ---------------------------------------------------------------- TEST 4
    private void testOptimisticLocking() {
        System.out.println("\n=== TEST 4 : OPTIMISTIC LOCKING ===");

        EntityManager em = emf.createEntityManager();
        Long id;
        try {
            id = em.createQuery("SELECT r.id FROM Reservation r WHERE r.statut = :statut ORDER BY r.id", Long.class)
                    .setParameter("statut", StatutReservation.CONFIRMEE)
                    .setMaxResults(1)
                    .getSingleResult();
        } finally {
            em.close();
        }
        System.out.println("Reservation choisie : id = " + id);

        Thread thread1 = new Thread(() ->
                modifier("Thread 1", id, 1000, r -> r.setMotif("Motif modifie par Thread 1")));

        Thread thread2 = new Thread(() -> {
            pause(100);
            modifier("Thread 2", id, 0, r -> r.setDateFin(r.getDateFin().plusHours(1)));
        });

        thread1.start();
        thread2.start();
        try {
            thread1.join();
            thread2.join();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        EntityManager em2 = emf.createEntityManager();
        try {
            Reservation r = em2.find(Reservation.class, id);
            System.out.println("\nEtat final de la reservation :");
            System.out.println("Motif : " + r.getMotif());
            System.out.println("Date fin : " + r.getDateFin());
            System.out.println("Version : " + r.getVersion());
        } finally {
            em2.close();
        }
    }

    private void modifier(String nom, Long id, long pauseAvantCommit, Consumer<Reservation> modification) {
        EntityManager em = emf.createEntityManager();
        try {
            em.getTransaction().begin();
            Reservation r = em.find(Reservation.class, id);
            System.out.println(nom + " : version lue = " + r.getVersion());

            pause(pauseAvantCommit);
            modification.accept(r);

            em.getTransaction().commit();
            System.out.println(nom + " : mise a jour reussie");
        } catch (RuntimeException e) {
            if (estConflit(e)) {
                System.out.println(nom + " : CONFLIT detecte (OptimisticLockException)");
            } else {
                e.printStackTrace();
            }
            if (em.getTransaction().isActive()) {
                em.getTransaction().rollback();
            }
        } finally {
            em.close();
        }
    }

    private boolean estConflit(Throwable e) {
        while (e != null) {
            if (e instanceof OptimisticLockException || e instanceof StaleStateException) {
                return true;
            }
            e = e.getCause();
        }
        return false;
    }

    // ---------------------------------------------------------------- TEST 5
    private void testCachePerformance() {
        System.out.println("\n=== TEST 5 : PERFORMANCE DU CACHE ===");
        int n = 300;

        // Sans cache : on vide le cache avant chaque lecture
        viderCache();
        stats.clear();
        long debut = System.currentTimeMillis();
        for (int i = 0; i < n; i++) {
            viderCache();
            lireSalle(i % 15 + 1);
        }
        System.out.println(n + " lectures SANS cache : " + (System.currentTimeMillis() - debut) + " ms, "
                + stats.getPrepareStatementCount() + " requetes SQL");

        // Avec cache
        viderCache();
        stats.clear();
        debut = System.currentTimeMillis();
        for (int i = 0; i < n; i++) {
            lireSalle(i % 15 + 1);
        }
        System.out.println(n + " lectures AVEC cache : " + (System.currentTimeMillis() - debut) + " ms, "
                + stats.getPrepareStatementCount() + " requetes SQL, "
                + stats.getSecondLevelCacheHitCount() + " hits");

        // Requetes sans cache de requete
        viderCache();
        stats.clear();
        debut = System.currentTimeMillis();
        for (int i = 0; i < 20; i++) {
            executerRequete(false);
        }
        System.out.println("20 requetes SANS cache de requete : " + (System.currentTimeMillis() - debut) + " ms, "
                + stats.getPrepareStatementCount() + " requetes SQL");

        // Requetes avec cache de requete
        viderCache();
        stats.clear();
        debut = System.currentTimeMillis();
        for (int i = 0; i < 20; i++) {
            executerRequete(true);
        }
        System.out.println("20 requetes AVEC cache de requete : " + (System.currentTimeMillis() - debut) + " ms, "
                + stats.getPrepareStatementCount() + " requetes SQL, "
                + stats.getQueryCacheHitCount() + " hits du cache de requete");
    }

    private void lireSalle(long id) {
        EntityManager em = emf.createEntityManager();
        try {
            em.find(Salle.class, id);
        } finally {
            em.close();
        }
    }

    private void executerRequete(boolean avecCache) {
        EntityManager em = emf.createEntityManager();
        try {
            javax.persistence.TypedQuery<Salle> query = em.createQuery(
                    "SELECT s FROM Salle s WHERE s.capacite >= :capacite", Salle.class)
                    .setParameter("capacite", 30);
            if (avecCache) {
                query.setHint("org.hibernate.cacheable", true);
            }
            query.getResultList();
        } finally {
            em.close();
        }
    }

    private void viderCache() {
        sessionFactory.getCache().evictAllRegions();
    }

    private void pause(long ms) {
        if (ms <= 0) {
            return;
        }
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
