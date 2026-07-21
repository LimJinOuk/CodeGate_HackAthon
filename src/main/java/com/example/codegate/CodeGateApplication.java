package com.example.codegate;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class CodeGateApplication {

    public static void main(String[] args) {
        SpringApplication.run(CodeGateApplication.class, args);
    }

}
