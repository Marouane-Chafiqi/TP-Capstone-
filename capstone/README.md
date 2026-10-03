# Capstone : Application "Reservation de salles"

## Objectif
Finaliser l'application : modele complet, jeu de donnees realiste, scenarios de test,
script de migration et rapport de performance (JPA / Hibernate, Ehcache, H2).

## Lancer
Ouvrir le projet (NetBeans ou IntelliJ), attendre Maven, puis executer `App` (F6 sous NetBeans).
Tout s'execute automatiquement, dans l'ordre :

1. Jeu de donnees : 10 equipements, 20 utilisateurs, 15 salles, 100 reservations
2. Scenarios de test : disponibilite, recherche multi-criteres, pagination, optimistic locking, cache
3. Script de migration (`migration_v2.sql`)
4. Rapport de performance (affiche a l'ecran et enregistre dans un fichier `performance_report_*.txt`)

## Structure
```
src/main/java/com/example
├── App.java
├── model     (Utilisateur, Salle, Reservation, Equipement, StatutReservation)
├── service   (SalleService)
├── util      (DataInitializer, DatabaseMigrationTool, PerformanceReport, PaginationResult)
└── test      (TestScenarios)
src/main/resources : persistence.xml, ehcache.xml, migration_v2.sql
```
