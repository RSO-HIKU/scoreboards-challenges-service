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
        LocalDate now = LocalDate.now();
        return getChallengesForMonth(now.getMonthValue(), now.getYear(), userId);
    }

    @GET
    @Path("/challenges/{month}/{year}")
    public Response getChallengesForMonth(
            @PathParam("month") int month,
            @PathParam("year") int year,
            @QueryParam("userId") String userId) {
        
        List<Challenge> challenges = service.getChallengesForMonth(month, year);
        List<Long> completedIds = userId != null 
            ? service.getUserCompletedChallengeIds(userId, month, year)
            : List.of();

        List<ChallengeDto> dtos = challenges.stream()
                .map(c -> {
                    ChallengeDto dto = new ChallengeDto();
                    dto.id = c.getId();
                    dto.title = c.getTitle();
                    dto.description = c.getDescription();
                    dto.month = c.getMonth();
                    dto.year = c.getYear();
                    dto.completed = completedIds.contains(c.getId());
                    return dto;
                })
                .collect(Collectors.toList());

        return Response.ok(dtos).build();
    }

    @POST
    @Path("/challenges/complete")
    public Response completeChallenge(CompleteChallengeRequest request) {
        if (request.userId == null || request.challengeId == null) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity("{\"error\":\"userId and challengeId are required\"}")
                    .build();
        }

        try {
            UserChallengeCompletion completion = service.completeChallenge(request.userId, request.challengeId);
            return Response.ok(Map.of(
                "id", completion.getId(),
                "userId", completion.getUserId(),
                "challengeId", completion.getChallenge().getId(),
                "completedAt", completion.getCompletedAt().toString()
            )).build();
        } catch (IllegalArgumentException e) {
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
        
        if (month == null || year == null) {
            // Use current month
            LocalDate now = LocalDate.now();
            month = now.getMonthValue();
            year = now.getYear();
        }

        List<Map<String, Object>> scoreboard = service.getChallengeScoreboard(month, year, limit);
        return Response.ok(scoreboard).build();
    }

    @POST
    @Path("/scoreboard/badges")
    public Response getBadgeScoreboard(BadgeScoreboardRequest request) {
        if (request.userIds == null || request.userIds.isEmpty()) {
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
        }

        List<Map<String, Object>> scoreboard = service.getBadgeScoreboard(request.userIds, month, year);
        return Response.ok(scoreboard).build();
    }
}
