package com.gout;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@SpringBootApplication
@EnableJpaAuditing
public class GoutApplication {
    public static void main(String[] args) {
        SpringApplication.run(GoutApplication.class, args);
    }
}
