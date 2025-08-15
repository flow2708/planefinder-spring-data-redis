package com.example.demo;

import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisOperations;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

@EnableScheduling
@Component
public class PlaneFinderPoller {
    private WebClient client =
            WebClient.create("http://localhost:7634/api/aircraft"); //planefinder-api
    private final RedisConnectionFactory connectionFactory;
    private final AircraftRepository repository;
    //private final RedisOperations<String, Aircraft> redisOperations;

    PlaneFinderPoller(RedisConnectionFactory connectionFactory,
                      AircraftRepository repository) {
        this.connectionFactory = connectionFactory;
        this.repository = repository;
        //this.redisOperations = redisOperations;
    }

    @Scheduled(fixedRate = 1000)
    private void pollPlanes() {
        connectionFactory.getConnection().serverCommands().flushDb();
        try {
            client.get()
                    .retrieve()
                    .bodyToFlux(Aircraft.class)
                    .filter(plane -> plane != null && !plane.getReg().isEmpty())
                    .doOnError(e -> System.err.println("Error fetching aircraft data: " + e.getMessage()))
                    .toStream()
                    .forEach(repository::save);
                    //.forEach(ac -> redisOperations.opsForValue().set(ac.getReg(), ac));

            //redisOperations.opsForValue()
            repository.findAll().forEach(System.out::println);
        } catch (Exception e) {
            System.err.println("Error in pollPlanes: " + e.getMessage());
        }
    }
}
