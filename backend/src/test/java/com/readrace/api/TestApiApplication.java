package com.readrace.api;

import org.springframework.boot.SpringApplication;

public class TestApiApplication {

    public static void main(String[] args) {
        SpringApplication.from(ApiApplication::main)
                .with(TestcontainersConfiguration.class)
                .run(args);
    }
}
