package main.java.com.hiku.scoreboardsChallengesService.service;

import main.java.com.hiku.scoreboardsChallengesService.db.dao.ChallengeDao;
import main.java.com.hiku.scoreboardsChallengesService.db.dao.UserChallengeCompletionDao;
import main.java.com.hiku.scoreboardsChallengesService.db.models.Challenge;
import main.java.com.hiku.scoreboardsChallengesService.db.models.UserChallengeCompletion;
import main.java.com.hiku.scoreboardsChallengesService.grpc.BadgeServiceClient;

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

    @Inject
    private BadgeServiceClient badgeServiceClient;

    public List<Challenge> getCurrentMonthChallenges() {
        LocalDate now = LocalDate.now();
        return challengeDao.getChallengesForMonth(now.getMonthValue(), now.getYear());
    }

    public List<Challenge> getChallengesForMonth(int month, int year) {
        return challengeDao.getChallengesForMonth(month, year);
    }

    public UserChallengeCompletion completeChallenge(String userId, Long challengeId) {
        System.out.println("[ScoreboardChallengeService] enter completeChallenge userId=" + userId + " challengeId=" + challengeId +
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
                    entry.put("completedChallenges", ((Number) row[1]).intValue());
                    return entry;
                })
                .collect(Collectors.toList());
    }

    public List<Map<String, Object>> getCurrentChallengeScoreboard(int limit) {
        LocalDate now = LocalDate.now();
        return getChallengeScoreboard(now.getMonthValue(), now.getYear(), limit);
    }

    public List<Map<String, Object>> getBadgeScoreboard(List<String> userIds, int month, int year) {
        Map<String, Integer> badgeCounts = new HashMap<>();
        
        for (String userId : userIds) {
            int count = badgeServiceClient.getBadgeCountForUser(userId, month, year);
            badgeCounts.put(userId, count);
        }

        return badgeCounts.entrySet().stream()
                .sorted((e1, e2) -> e2.getValue().compareTo(e1.getValue())) // Sort descending by badge count
                .map(entry -> {
                    Map<String, Object> scoreEntry = new HashMap<>();
                    scoreEntry.put("userId", entry.getKey());
                    scoreEntry.put("badgeCount", entry.getValue());
                    return scoreEntry;
                })
                .collect(Collectors.toList());
    }

    public List<Long> getUserCompletedChallengeIds(String userId, int month, int year) {
        List<UserChallengeCompletion> completions = completionDao.getCompletionsForUser(userId, month, year);
        return completions.stream()
                .map(c -> c.getChallenge().getId())
                .collect(Collectors.toList());
    }
}
