package com.hiku.scoreboardsChallengesService.controller;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.json.Json;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.RuntimeMXBean;
import java.time.Instant;

/**
 * Health check endpoints for Kubernetes probes.
 * 
 * Endpoints:
 * - /api/scoreboards-challenges/health - Overall health status with detailed checks
 * - /api/scoreboards-challenges/health/live - Liveness probe (is the service running?)
 */
@Path("/health")
public class HealthController {

    private static EntityManagerFactory emf;

    static {
        try {
            emf = Persistence.createEntityManagerFactory("hikuPU");
        } catch (Exception e) {
            System.err.println("Failed to initialize EntityManagerFactory: " + e.getMessage());
        }
    }

    /**
     * Main health check endpoint - comprehensive status with multiple checks
     */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response health() {
        JsonArrayBuilder checks = Json.createArrayBuilder();
        boolean allHealthy = true;

        // Check 1: Database connectivity
        HealthCheckResult dbCheck = checkDatabase();
        checks.add(buildCheckJson("database", dbCheck));
        if (!dbCheck.isUp()) {
            allHealthy = false;
        }

        // Check 2: Memory status
        HealthCheckResult memoryCheck = checkMemory();
        checks.add(buildCheckJson("memory", memoryCheck));
        if (!memoryCheck.isUp()) {
            allHealthy = false;
        }

        // Check 3: Uptime
        RuntimeMXBean runtimeBean = ManagementFactory.getRuntimeMXBean();
        long uptime = runtimeBean.getUptime();
        checks.add(Json.createObjectBuilder()
                .add("name", "uptime")
                .add("status", "UP")
                .add("uptime_ms", uptime)
                .build());

        String status = allHealthy ? "UP" : "DOWN";
        JsonObject response = Json.createObjectBuilder()
                .add("status", status)
                .add("service", "scoreboards-challenges-service")
                .add("timestamp", Instant.now().toString())
                .add("checks", checks)
                .build();

        return Response.status(allHealthy ? Response.Status.OK : Response.Status.SERVICE_UNAVAILABLE)
                .entity(response.toString())
                .build();
    }

    /**
     * Liveness probe endpoint.
     * Returns UP if the service is running (JVM is alive).
     * Kubernetes uses this to decide if the container should be restarted.
     */
    @GET
    @Path("/live")
    @Produces(MediaType.APPLICATION_JSON)
    public Response liveness() {
        RuntimeMXBean runtimeBean = ManagementFactory.getRuntimeMXBean();
        long uptime = runtimeBean.getUptime();

        JsonObject response = Json.createObjectBuilder()
                .add("status", "UP")
                .add("service", "scoreboards-challenges-service")
                .add("timestamp", Instant.now().toString())
                .add("uptime_ms", uptime)
                .build();

        return Response.ok(response.toString()).build();
    }

    /**
     * Check database connectivity by executing a simple query.
     */
    private HealthCheckResult checkDatabase() {
        EntityManager em = null;
        try {
            if (emf == null || !emf.isOpen()) {
                return new HealthCheckResult(false, "EntityManagerFactory not initialized");
            }
            em = emf.createEntityManager();
            em.createNativeQuery("SELECT 1").getSingleResult();
            return new HealthCheckResult(true, "Database connection OK");
        } catch (Exception e) {
            return new HealthCheckResult(false, "Database connection failed: " + e.getMessage());
        } finally {
            if (em != null && em.isOpen()) {
                em.close();
            }
        }
    }

    /**
     * Check memory status - warns if heap usage is too high.
     */
    private HealthCheckResult checkMemory() {
        try {
            MemoryMXBean memoryBean = ManagementFactory.getMemoryMXBean();
            long heapUsed = memoryBean.getHeapMemoryUsage().getUsed();
            long heapMax = memoryBean.getHeapMemoryUsage().getMax();
            
            double usagePercent = (double) heapUsed / heapMax * 100;
            String message = String.format("Heap usage: %.1f%% (%d MB / %d MB)", 
                    usagePercent, heapUsed / (1024 * 1024), heapMax / (1024 * 1024));

            // Consider unhealthy if heap usage is above 90%
            boolean isHealthy = usagePercent < 90;
            return new HealthCheckResult(isHealthy, message);
        } catch (Exception e) {
            return new HealthCheckResult(false, "Memory check failed: " + e.getMessage());
        }
    }

    /**
     * Build JSON object for a health check result.
     */
    private JsonObject buildCheckJson(String name, HealthCheckResult result) {
        return Json.createObjectBuilder()
                .add("name", name)
                .add("status", result.isUp() ? "UP" : "DOWN")
                .add("message", result.getMessage())
                .build();
    }

    /**
     * Simple class to hold health check results.
     */
    private static class HealthCheckResult {
        private final boolean up;
        private final String message;

        HealthCheckResult(boolean up, String message) {
            this.up = up;
            this.message = message;
        }

        boolean isUp() {
            return up;
        }

        String getMessage() {
            return message;
        }
    }
}

