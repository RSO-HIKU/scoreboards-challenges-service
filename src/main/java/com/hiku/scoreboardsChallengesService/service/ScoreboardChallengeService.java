package com.hiku.scoreboardsChallengesService.service;

import com.hiku.scoreboardsChallengesService.db.dao.ChallengeDao;
import com.hiku.scoreboardsChallengesService.db.dao.UserChallengeCompletionDao;
import com.hiku.scoreboardsChallengesService.db.models.Challenge;
import com.hiku.scoreboardsChallengesService.db.models.UserChallengeCompletion;


import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@ApplicationScoped
public class ScoreboardChallengeService {

    @Inject
    private ChallengeDao challengeDao;

    @Inject
    private UserChallengeCompletionDao completionDao;


    public List<Challenge> getCurrentMonthChallenges() {
        LocalDate now = LocalDate.now();
        return challengeDao.getChallengesForMonth(now.getMonthValue(), now.getYear());
    }

    public List<Challenge> getChallengesForMonth(int month, int year) {
        return challengeDao.getChallengesForMonth(month, year);
    }

    public UserChallengeCompletion completeChallenge(String userId, Long challengeId, String username) {
        System.out.println("[ScoreboardChallengeService] enter completeChallenge userId=" + userId + " challengeId=" + challengeId + " username=" + username +
                " (userIdClass=" + (userId==null ? "null" : userId.getClass().getName()) +
                ", challengeIdClass=" + (challengeId==null ? "null" : challengeId.getClass().getName()) + ")");

        try {
            Challenge challenge = challengeDao.findById(challengeId);
            if (challenge == null) {
                throw new IllegalArgumentException("Challenge not found");
            }

            // Check if already completed
            UserChallengeCompletion existing = completionDao.findByUserAndChallenge(userId, challengeId);
            if (existing != null) {
                return existing; // Already completed
            }

            UserChallengeCompletion completion = new UserChallengeCompletion();
            completion.setUserId(userId);
            completion.setUsername(username);
            completion.setChallenge(challenge);
            System.out.println("[ScoreboardChallengeService] before DB check - userId=" + userId + " challengeId=" + challengeId);
            completion = completionDao.create(completion);
            System.out.println("[ScoreboardChallengeService] completion created id=" + (completion==null ? "null" : completion.getId()));
            return completion;
        } catch (NumberFormatException e) {
            System.out.println("[ScoreboardChallengeService] NumberFormatException parsing id: " + e.getMessage());
            throw new IllegalArgumentException("Invalid numeric id: " + e.getMessage(), e);
        } catch (IllegalArgumentException e) {
            System.out.println("[ScoreboardChallengeService] IllegalArgumentException: " + e.getMessage());
            throw e;
        } catch (Exception e) {
            System.out.println("[ScoreboardChallengeService] unexpected error: " + e.getClass().getName() + " - " + e.getMessage());
            throw e;
        }
    }

    public List<Map<String, Object>> getChallengeScoreboard(int month, int year, int limit) {
        List<Object[]> results = completionDao.getChallengeScoreboard(month, year, limit);
        
        return results.stream()
                .map(row -> {
                    Map<String, Object> entry = new HashMap<>();
                    entry.put("userId", row[0]);
                    entry.put("username", row[1]); // Include username
                    entry.put("completedChallenges", ((Number) row[2]).intValue());
                    return entry;
                })
                .collect(Collectors.toList());
    }

    public List<Map<String, Object>> getCurrentChallengeScoreboard(int limit) {
        LocalDate now = LocalDate.now();
        return getChallengeScoreboard(now.getMonthValue(), now.getYear(), limit);
    }


    public List<Long> getUserCompletedChallengeIds(String userId, int month, int year) {
        List<UserChallengeCompletion> completions = completionDao.getCompletionsForUser(userId, month, year);
        return completions.stream()
                .map(c -> c.getChallenge().getId())
                .collect(Collectors.toList());
    }
}
