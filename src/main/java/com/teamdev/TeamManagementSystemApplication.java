package com.teamdev;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("com.teamdev.mapper")
public class TeamManagementSystemApplication {

    public static void main(String[] args) {
        SpringApplication.run(TeamManagementSystemApplication.class, args);
    }

}