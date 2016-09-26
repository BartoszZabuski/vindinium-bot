package dev.challenge.vindinium;

import org.springframework.boot.SpringApplication;

import dev.challenge.vindinium.config.RootApplicationConfig;

public class App {
    public static void main(String[] args) {
        SpringApplication.run(RootApplicationConfig.class, args);
    }
}
