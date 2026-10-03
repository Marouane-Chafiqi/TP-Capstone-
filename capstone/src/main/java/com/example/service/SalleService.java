package com.example.service;

import com.example.model.Salle;
import com.example.model.StatutReservation;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.TypedQuery;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SalleService {

    private final EntityManagerFactory emf;

    public SalleService(EntityManagerFactory emf) {
        this.emf = emf;
    }

    public List<Salle> findAvailableRooms(LocalDateTime start, LocalDateTime end) {
        EntityManager em = emf.createEntityManager();
        try {
            return em.createQuery(
                    "SELECT s FROM Salle s WHERE s.id NOT IN " +
                    "(SELECT r.salle.id FROM Reservation r " +
                    "WHERE r.statut <> :annulee AND r.dateDebut < :end AND r.dateFin > :start) " +
                    "ORDER BY s.id", Salle.class)
                    .setParameter("annulee", StatutReservation.ANNULEE)
                    .setParameter("start", start)
                    .setParameter("end", end)
                    .getResultList();
        } finally {
            em.close();
        }
    }

    public List<Salle> searchRooms(Map<String, Object> criteres) {
        StringBuilder jpql = new StringBuilder("SELECT DISTINCT s FROM Salle s");
        Map<String, Object> params = new HashMap<>();

        if (criteres.containsKey("equipement")) {
            jpql.append(" JOIN s.equipements e");
        }
        jpql.append(" WHERE 1=1");

        if (criteres.containsKey("nom")) {
            jpql.append(" AND s.nom LIKE :nom");
            params.put("nom", "%" + criteres.get("nom") + "%");
        }
        if (criteres.containsKey("capaciteMin")) {
            jpql.append(" AND s.capacite >= :capaciteMin");
            params.put("capaciteMin", criteres.get("capaciteMin"));
        }
        if (criteres.containsKey("capaciteMax")) {
            jpql.append(" AND s.capacite <= :capaciteMax");
            params.put("capaciteMax", criteres.get("capaciteMax"));
        }
        if (criteres.containsKey("batiment")) {
            jpql.append(" AND s.batiment = :batiment");
            params.put("batiment", criteres.get("batiment"));
        }
        if (criteres.containsKey("etage")) {
            jpql.append(" AND s.etage = :etage");
            params.put("etage", criteres.get("etage"));
        }
        if (criteres.containsKey("equipement")) {
            jpql.append(" AND e.id = :equipement");
            params.put("equipement", criteres.get("equipement"));
        }
        jpql.append(" ORDER BY s.id");

        EntityManager em = emf.createEntityManager();
        try {
            TypedQuery<Salle> query = em.createQuery(jpql.toString(), Salle.class);
            for (Map.Entry<String, Object> p : params.entrySet()) {
                query.setParameter(p.getKey(), p.getValue());
            }
            return query.getResultList();
        } finally {
            em.close();
        }
    }

    public List<Salle> getPaginatedRooms(int page, int size) {
        EntityManager em = emf.createEntityManager();
        try {
            return em.createQuery("SELECT s FROM Salle s ORDER BY s.id", Salle.class)
                    .setFirstResult((page - 1) * size)
                    .setMaxResults(size)
                    .getResultList();
        } finally {
            em.close();
        }
    }

    public long countRooms() {
        EntityManager em = emf.createEntityManager();
        try {
            return em.createQuery("SELECT COUNT(s) FROM Salle s", Long.class).getSingleResult();
        } finally {
            em.close();
        }
    }

    public int getTotalPages(int size) {
        return (int) Math.ceil((double) countRooms() / size);
    }
}
