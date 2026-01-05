package main.java.com.hiku.scoreboardsChallengesService.grpc;

import com.hiku.grpc.badge.BadgeRequest;
import com.hiku.grpc.badge.BadgeResponse;
import com.hiku.grpc.badge.BadgeServiceGrpc;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;

import javax.annotation.PreDestroy;
import javax.enterprise.context.ApplicationScoped;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

@ApplicationScoped
public class BadgeServiceClient {
    private static final Logger logger = Logger.getLogger(BadgeServiceClient.class.getName());
    private final ManagedChannel channel;
    private final BadgeServiceGrpc.BadgeServiceBlockingStub blockingStub;

    public BadgeServiceClient() {
        String host = System.getenv().getOrDefault("BADGE_SERVICE_HOST", "localhost");
        int port = Integer.parseInt(System.getenv().getOrDefault("BADGE_SERVICE_GRPC_PORT", "9090"));
        
        logger.info("Connecting to badge service at " + host + ":" + port);
        
        this.channel = ManagedChannelBuilder.forAddress(host, port)
                .usePlaintext()
                .build();
        this.blockingStub = BadgeServiceGrpc.newBlockingStub(channel);
    }

    public int getBadgeCountForUser(String userId, int month, int year) {
        try {
            int userIdInt = Integer.parseInt(userId);
            BadgeRequest request = BadgeRequest.newBuilder()
                    .setUserId(userIdInt)
                    .setMonth(month)
                    .setYear(year)
                    .build();

            BadgeResponse response = blockingStub.getUserBadgeCount(request);
            
            if (response.getFound()) {
                logger.info("Found " + response.getBadgeCount() + " badges for user " + userId);
                return response.getBadgeCount();
            } else {
                logger.warning("User " + userId + " not found");
                return 0;
            }
        } catch (Exception e) {
            logger.severe("gRPC call failed: " + e.getMessage());
            return 0;
        }
    }

    @PreDestroy
    public void shutdown() {
        try {
            channel.shutdown().awaitTermination(5, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            logger.warning("Channel shutdown interrupted");
            Thread.currentThread().interrupt();
        }
    }
}
