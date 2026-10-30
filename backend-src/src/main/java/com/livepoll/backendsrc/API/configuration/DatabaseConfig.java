package com.livepoll.backendsrc.API.configuration;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;

@Configuration
public class DatabaseConfig {

    @Configuration
    @EnableJpaRepositories(
            basePackages = "com.livepoll.backendsrc.API.repository.jpa"
    )
    static class JpaConfig {}

    @Configuration
    @EnableMongoRepositories(
            basePackages = "com.livepoll.backendsrc.API.repository.mongo"
    )
    static class MongoConfig {}
}