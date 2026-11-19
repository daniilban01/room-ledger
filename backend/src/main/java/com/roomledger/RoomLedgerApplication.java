package com.roomledger;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

// Users come from our own table and JWTs, so Spring Boot's default in-memory user
// (with a generated password printed in the logs) is not needed.
@SpringBootApplication(exclude = UserDetailsServiceAutoConfiguration.class)
@ConfigurationPropertiesScan
public class RoomLedgerApplication {

    public static void main(String[] args) {
        SpringApplication.run(RoomLedgerApplication.class, args);
    }
}
