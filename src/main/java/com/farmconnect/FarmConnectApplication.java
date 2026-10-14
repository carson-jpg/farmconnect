package com.farmconnect;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class FarmConnectApplication {
    public static void main(String[] args) {
        SpringApplication.run(FarmConnectApplication.class, args);
    }
}
