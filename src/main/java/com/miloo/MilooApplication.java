package com.miloo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class MilooApplication {

    public static void main(String[] args) {
        SpringApplication.run(MilooApplication.class, args);
    }
}
