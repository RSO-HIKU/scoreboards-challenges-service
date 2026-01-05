package main.java.com.hiku.scoreboardsChallengesService.db.dao;

import main.java.com.hiku.scoreboardsChallengesService.db.models.UserChallengeCompletion;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.transaction.Transactional;
import java.util.List;

@ApplicationScoped
public class UserChallengeCompletionDao {

    @Inject
    private EntityManager em;

    public List<UserChallengeCompletion> getCompletionsForUser(String userId, int month, int year) {
        return em.createQuery(
                "SELECT ucc FROM UserChallengeCompletion ucc " +
                "JOIN ucc.challenge c " +
                "WHERE ucc.userId = :userId AND c.month = :month AND c.year = :year",
                UserChallengeCompletion.class)
                .setParameter("userId", userId)
                .setParameter("month", month)
                .setParameter("year", year)
                .getResultList();
    }

    public Long countCompletionsForChallenge(Long challengeId) {
        return em.createQuery(
                "SELECT COUNT(ucc) FROM UserChallengeCompletion ucc WHERE ucc.challenge.id = :challengeId",
                Long.class)
                .setParameter("challengeId", challengeId)
                .getSingleResult();
    }

    public UserChallengeCompletion findByUserAndChallenge(String userId, Long challengeId) {
        try {
            return em.createQuery(
                    "SELECT ucc FROM UserChallengeCompletion ucc " +
                    "WHERE ucc.userId = :userId AND ucc.challenge.id = :challengeId",
                    UserChallengeCompletion.class)
                    .setParameter("userId", userId)
                    .setParameter("challengeId", challengeId)
                    .getSingleResult();
        } catch (NoResultException e) {
            return null;
        }
    }

    public UserChallengeCompletion create(UserChallengeCompletion completion) {
        em.getTransaction().begin();
        em.persist(completion);
        em.getTransaction().commit();
        return completion;
    }

    // Get scoreboard: users with most completed challenges in a month
    public List<Object[]> getChallengeScoreboard(int month, int year, int limit) {
        return em.createQuery(
                "SELECT ucc.userId, ucc.username, COUNT(ucc) as completions " +
                "FROM UserChallengeCompletion ucc " +
                "JOIN ucc.challenge c " +
                "WHERE c.month = :month AND c.year = :year " +
                "GROUP BY ucc.userId, ucc.username " +
                "ORDER BY completions DESC",
                Object[].class)
                .setParameter("month", month)
                .setParameter("year", year)
                .setMaxResults(limit)
                .getResultList();
    }

    // Get username for a user
    public String getUsernameForUser(String userId) {
        try {
            return em.createQuery(
                    "SELECT ucc.username FROM UserChallengeCompletion ucc " +
                    "WHERE ucc.userId = :userId AND ucc.username IS NOT NULL " +
                    "LIMIT 1",
                    String.class)
                    .setParameter("userId", userId)
                    .getSingleResult();
        } catch (NoResultException e) {
            return null;
        }
    }
}
