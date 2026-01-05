package main.java.com.hiku.scoreboardsChallengesService.controller;

import main.java.com.hiku.scoreboardsChallengesService.db.models.Challenge;
import main.java.com.hiku.scoreboardsChallengesService.db.models.UserChallengeCompletion;
import main.java.com.hiku.scoreboardsChallengesService.service.ScoreboardChallengeService;

import javax.inject.Inject;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Path("/scoreboards-challenges")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class ScoreboardChallengeController {

    @Inject
    private ScoreboardChallengeService service;

    public static class ChallengeDto {
        public Long id;
        public String title;
        public String description;
        public Integer month;
        public Integer year;
        public Boolean completed;  // Will be set based on user
    }

    public static class CompleteChallengeRequest {
        public String userId;
        public Long challengeId;
    }

    public static class BadgeScoreboardRequest {
        public List<String> userIds;
        public Integer month;
        public Integer year;
    }

    @GET
    @Path("/challenges")
    public Response getCurrentChallenges(@QueryParam("userId") String userId) {
        System.out.println("[ScoreboardChallengeController] getCurrentChallenges called userId=" + userId);
        LocalDate now = LocalDate.now();
        return getChallengesForMonth(now.getMonthValue(), now.getYear(), userId);
    }

    @GET
    @Path("/challenges/{month}/{year}")
    public Response getChallengesForMonth(
            @PathParam("month") int month,
            @PathParam("year") int year,
            @QueryParam("userId") String userId) {

        System.out.println("[ScoreboardChallengeController] getChallengesForMonth called month=" + month + " year=" + year + " userId=" + userId);

        List<Challenge> challenges = service.getChallengesForMonth(month, year);
        List<Long> completedIds = userId != null
            ? service.getUserCompletedChallengeIds(userId, month, year)
            : List.of();

        System.out.println("[ScoreboardChallengeController] fetched challenges count=" + (challenges == null ? 0 : challenges.size()) +
                " completedIds count=" + (completedIds == null ? 0 : completedIds.size()));

        List<ChallengeDto> dtos = challenges.stream()
                .map(c -> {
                    ChallengeDto dto = new ChallengeDto();
                    dto.id = c.getId();
                    dto.title = c.getTitle();
                    dto.description = c.getDescription();
                    dto.month = c.getMonth();
                    dto.year = c.getYear();
                    dto.completed = completedIds.contains(c.getId());
                    if (dto.completed) {
                        System.out.println("[ScoreboardChallengeController] challenge marked completed id=" + dto.id + " for user=" + userId);
                    }
                    return dto;
                })
                .collect(Collectors.toList());

        System.out.println("[ScoreboardChallengeController] returning " + dtos.size() + " challenge DTOs");
        return Response.ok(dtos).build();
    }

    @POST
    @Path("/challenges/complete")
    public Response completeChallenge(CompleteChallengeRequest request) {
        System.out.println("[ScoreboardChallengeController] completeChallenge called userId=" + request.userId + " challengeId=" + request.challengeId);
        if (request.userId == null || request.challengeId == null) {
            System.out.println("[ScoreboardChallengeController] completeChallenge bad request: missing userId or challengeId");
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity("{\"error\":\"userId and challengeId are required\"}")
                    .build();
        }

        try {
            UserChallengeCompletion completion = service.completeChallenge(request.userId, request.challengeId);
            System.out.println("[ScoreboardChallengeController] completeChallenge succeeded id=" + completion.getId() +
                    " userId=" + completion.getUserId() + " challengeId=" + completion.getChallenge().getId());
            return Response.ok(Map.of(
                "id", completion.getId(),
                "userId", completion.getUserId(),
                "challengeId", completion.getChallenge().getId(),
                "completedAt", completion.getCompletedAt().toString()
            )).build();
        } catch (IllegalArgumentException e) {
            System.out.println("[ScoreboardChallengeController] completeChallenge not found: " + e.getMessage());
            return Response.status(Response.Status.NOT_FOUND)
                    .entity("{\"error\":\"" + e.getMessage() + "\"}")
                    .build();
        }
    }

    @GET
    @Path("/scoreboard/challenges")
    public Response getChallengeScoreboard(
            @QueryParam("month") Integer month,
            @QueryParam("year") Integer year,
            @QueryParam("limit") @DefaultValue("10") int limit) {

        System.out.println("[ScoreboardChallengeController] getChallengeScoreboard called month=" + month + " year=" + year + " limit=" + limit);

        if (month == null || year == null) {
            // Use current month
            LocalDate now = LocalDate.now();
            month = now.getMonthValue();
            year = now.getYear();
            System.out.println("[ScoreboardChallengeController] getChallengeScoreboard using current month/year " + month + "/" + year);
        }

        List<Map<String, Object>> scoreboard = service.getChallengeScoreboard(month, year, limit);
        System.out.println("[ScoreboardChallengeController] getChallengeScoreboard returning entries=" + (scoreboard == null ? 0 : scoreboard.size()));
        return Response.ok(scoreboard).build();
    }

    @POST
    @Path("/scoreboard/badges")
    public Response getBadgeScoreboard(BadgeScoreboardRequest request) {
        System.out.println("[ScoreboardChallengeController] getBadgeScoreboard called userIdsCount=" + (request.userIds == null ? 0 : request.userIds.size()) +
                " month=" + request.month + " year=" + request.year);
        if (request.userIds == null || request.userIds.isEmpty()) {
            System.out.println("[ScoreboardChallengeController] getBadgeScoreboard bad request: userIds list required");
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity("{\"error\":\"userIds list is required\"}")
                    .build();
        }

        Integer month = request.month;
        Integer year = request.year;

        if (month == null || year == null) {
            LocalDate now = LocalDate.now();
            month = now.getMonthValue();
            year = now.getYear();
            System.out.println("[ScoreboardChallengeController] getBadgeScoreboard using current month/year " + month + "/" + year);
        }

        List<Map<String, Object>> scoreboard = service.getBadgeScoreboard(request.userIds, month, year);
        System.out.println("[ScoreboardChallengeController] getBadgeScoreboard returning entries=" + (scoreboard == null ? 0 : scoreboard.size()));
        return Response.ok(scoreboard).build();
    }
}
