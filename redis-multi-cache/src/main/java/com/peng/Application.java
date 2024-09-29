package com.peng;

import com.peng.config.AppConfig;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;


/**
 * @Author: spengju
 * @Slogan: Day day no bug.
 * @Date: 2024/9/30 00:59
 * $description
 */
@SpringBootApplication
public class Application {

    public static void main(String[] args) {
        SpringApplication.run(AppConfig.class, args);
    }

}

