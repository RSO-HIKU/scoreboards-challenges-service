package com.hiku.scoreboardsChallengesService.db.dao;
import com.hiku.scoreboardsChallengesService.db.models.Challenge;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.transaction.Transactional;
import java.util.List;

@ApplicationScoped
public class ChallengeDao {

    @Inject
    private EntityManager em;

    public List<Challenge> getChallengesForMonth(int month, int year) {
        return em.createQuery(
                "SELECT c FROM Challenge c WHERE c.month = :month AND c.year = :year ORDER BY c.id",
                Challenge.class)
                .setParameter("month", month)
                .setParameter("year", year)
                .getResultList();
    }

    public Challenge findById(Long id) {
        return em.find(Challenge.class, id);
    }

    public Challenge create(Challenge challenge) {
        em.getTransaction().begin();
        em.persist(challenge);
        em.getTransaction().commit();
        return challenge;
    }
}
