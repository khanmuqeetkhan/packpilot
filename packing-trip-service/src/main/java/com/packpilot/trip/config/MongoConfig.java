package com.packpilot.trip.config;

import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.MongoCredential;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.MongoDatabaseFactory;
import org.springframework.data.mongodb.core.SimpleMongoClientDatabaseFactory;

@Configuration
@EnableConfigurationProperties(MongoProperties.class)
public class MongoConfig {

    @Bean
    public MongoClient mongoClient(MongoProperties properties) {
        String connectionString = "mongodb://" + properties.getHost() + ":" + properties.getPort();
        MongoClientSettings.Builder settings = MongoClientSettings.builder()
                .applyConnectionString(new ConnectionString(connectionString));

        if (properties.hasCredentials()) {
            MongoCredential credential = MongoCredential.createCredential(
                    properties.getUsername(), properties.getAuthDatabase(), properties.getPassword().toCharArray());
            settings.credential(credential);
        }

        return MongoClients.create(settings.build());
    }

    @Bean
    public MongoDatabaseFactory mongoDatabaseFactory(MongoClient mongoClient, MongoProperties properties) {
        return new SimpleMongoClientDatabaseFactory(mongoClient, properties.getDatabase());
    }
}
