package main.java.com.hiku.scoreboardsChallengesService.util;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Disposes;
import javax.enterprise.inject.Produces;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;

@ApplicationScoped
public class EntityManagerProducer {
    private static EntityManagerFactory emf = Persistence.createEntityManagerFactory("scoreboards-pu");

    @Produces
    @ApplicationScoped
    public EntityManager createEntityManager() {
        return emf.createEntityManager();
    }

    public void close(@Disposes EntityManager em) {
        if (em.isOpen()) {
            em.close();
        }
    }
}
