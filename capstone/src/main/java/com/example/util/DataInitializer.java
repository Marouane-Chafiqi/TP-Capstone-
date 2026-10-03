package com.example.util;

import com.example.model.Equipement;
import com.example.model.Reservation;
import com.example.model.Salle;
import com.example.model.StatutReservation;
import com.example.model.Utilisateur;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import java.time.LocalDateTime;
import java.util.Random;

public class DataInitializer {

    private final EntityManagerFactory emf;
    private final Random random = new Random(42);

    public DataInitializer(EntityManagerFactory emf) {
        this.emf = emf;
    }

    public void initializeData() {
        EntityManager em = emf.createEntityManager();
        try {
            em.getTransaction().begin();

            Equipement[] equipements = createEquipements(em);
            Utilisateur[] utilisateurs = createUtilisateurs(em);
            Salle[] salles = createSalles(em, equipements);
            createReservations(em, utilisateurs, salles);

            em.getTransaction().commit();
            System.out.println("Jeu de donnees initialise : 10 equipements, 20 utilisateurs, 15 salles, 100 reservations");
        } catch (RuntimeException e) {
            if (em.getTransaction().isActive()) {
                em.getTransaction().rollback();
            }
            e.printStackTrace();
        } finally {
            em.close();
        }
    }

    private Equipement[] createEquipements(EntityManager em) {
        String[][] data = {
                {"Projecteur HD", "Projecteur haute definition 4K", "PROJ-4K-001"},
                {"Ecran interactif", "Ecran tactile 65 pouces", "ECRAN-T-65"},
                {"Systeme de visioconference", "Systeme complet avec camera HD", "VISIO-HD-100"},
                {"Tableau blanc", "Tableau blanc magnetique 2m x 1m", "TB-MAG-2X1"},
                {"Systeme audio", "Systeme audio avec 4 haut-parleurs", "AUDIO-4HP"},
                {"Microphones sans fil", "Set de 4 microphones sans fil", "MIC-SF-4"},
                {"Ordinateur fixe", "PC avec Windows 11 et suite Office", "PC-W11-OFF"},
                {"Connexion WiFi haut debit", "WiFi 6 avec debit jusqu'a 1 Gbps", "WIFI-6-1G"},
                {"Systeme de climatisation", "Climatisation reglable", "CLIM-REG"},
                {"Prises electriques multiples", "10 prises electriques reparties", "PRISES-10"}
        };

        Equipement[] equipements = new Equipement[data.length];
        for (int i = 0; i < data.length; i++) {
            equipements[i] = new Equipement(data[i][0], data[i][1]);
            equipements[i].setReference(data[i][2]);
            em.persist(equipements[i]);
        }
        return equipements;
    }

    private Utilisateur[] createUtilisateurs(EntityManager em) {
        String[] noms = {"Martin", "Bernard", "Dubois", "Thomas", "Robert", "Richard", "Petit", "Durand", "Leroy", "Moreau",
                "Simon", "Laurent", "Lefebvre", "Michel", "Garcia", "David", "Bertrand", "Roux", "Vincent", "Fournier"};
        String[] prenoms = {"Jean", "Marie", "Pierre", "Sophie", "Thomas", "Catherine", "Nicolas", "Isabelle", "Philippe", "Nathalie",
                "Michel", "Francoise", "Patrick", "Monique", "Rene", "Sylvie", "Louis", "Anne", "Daniel", "Christine"};
        String[] departements = {"Ressources Humaines", "Informatique", "Finance", "Marketing", "Commercial",
                "Production", "Recherche et Developpement", "Juridique", "Communication", "Direction"};

        Utilisateur[] utilisateurs = new Utilisateur[20];
        for (int i = 0; i < 20; i++) {
            String email = prenoms[i].toLowerCase() + "." + noms[i].toLowerCase() + "@example.com";
            utilisateurs[i] = new Utilisateur(noms[i], prenoms[i], email);
            utilisateurs[i].setTelephone("06" + (10000000 + random.nextInt(90000000)));
            utilisateurs[i].setDepartement(departements[i % 10]);
            em.persist(utilisateurs[i]);
        }
        return utilisateurs;
    }

    private Salle nouvelleSalle(String nom, int capacite, String description, String batiment, int etage, String numero) {
        Salle salle = new Salle(nom, capacite);
        salle.setDescription(description);
        salle.setBatiment(batiment);
        salle.setEtage(etage);
        salle.setNumero(numero);
        return salle;
    }

    private Salle[] createSalles(EntityManager em, Equipement[] eq) {
        Salle[] salles = new Salle[15];

        // Batiment A : salles de reunion
        for (int i = 0; i < 5; i++) {
            Salle s = nouvelleSalle("Salle A" + (i + 1), 10 + i * 2, "Salle de reunion standard",
                    "Batiment A", i % 3 + 1, "A" + (i + 1));
            s.addEquipement(eq[3]);
            s.addEquipement(eq[7]);
            s.addEquipement(eq[9]);
            if (i % 2 == 0) s.addEquipement(eq[0]);
            if (i % 3 == 0) s.addEquipement(eq[4]);
            em.persist(s);
            salles[i] = s;
        }

        // Batiment B : salles de formation
        for (int i = 5; i < 10; i++) {
            Salle s = nouvelleSalle("Salle B" + (i - 4), 20 + (i - 5) * 5, "Salle de formation equipee",
                    "Batiment B", i % 4 + 1, "B" + (i - 4));
            s.addEquipement(eq[0]);
            s.addEquipement(eq[3]);
            s.addEquipement(eq[6]);
            s.addEquipement(eq[7]);
            s.addEquipement(eq[9]);
            if (i % 2 == 0) s.addEquipement(eq[1]);
            em.persist(s);
            salles[i] = s;
        }

        // Batiment C : salles de conference
        for (int i = 10; i < 15; i++) {
            Salle s = nouvelleSalle("Salle C" + (i - 9), 50 + (i - 10) * 20, "Salle de conference haut de gamme",
                    "Batiment C", i % 3 + 1, "C" + (i - 9));
            s.addEquipement(eq[0]);
            s.addEquipement(eq[2]);
            s.addEquipement(eq[4]);
            s.addEquipement(eq[5]);
            s.addEquipement(eq[7]);
            s.addEquipement(eq[8]);
            s.addEquipement(eq[9]);
            em.persist(s);
            salles[i] = s;
        }
        return salles;
    }

    private void createReservations(EntityManager em, Utilisateur[] utilisateurs, Salle[] salles) {
        String[] motifs = {"Reunion d'equipe", "Entretien", "Formation", "Presentation client",
                "Brainstorming", "Reunion de projet", "Conference", "Atelier",
                "Seminaire", "Reunion de direction", "Demonstration produit"};

        LocalDateTime now = LocalDateTime.now();

        for (int i = 0; i < 100; i++) {
            int jour = random.nextInt(90);
            int heureDebut = 8 + random.nextInt(9);
            int duree = 1 + random.nextInt(3);

            LocalDateTime debut = now.plusDays(jour).withHour(heureDebut).withMinute(0).withSecond(0).withNano(0);
            Reservation reservation = new Reservation(debut, debut.plusHours(duree), motifs[random.nextInt(motifs.length)]);

            int tirage = random.nextInt(10);
            if (tirage < 8) {
                reservation.setStatut(StatutReservation.CONFIRMEE);
            } else if (tirage < 9) {
                reservation.setStatut(StatutReservation.EN_ATTENTE);
            } else {
                reservation.setStatut(StatutReservation.ANNULEE);
            }

            utilisateurs[random.nextInt(utilisateurs.length)].addReservation(reservation);
            salles[random.nextInt(salles.length)].addReservation(reservation);
            em.persist(reservation);
        }
    }
}
