package com.dota2.api;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.servlet.ServletComponentScan;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@EnableTransactionManagement
@MapperScan(basePackages = {"com.dota2.**.dao"})
@ServletComponentScan
@SpringBootApplication
@ComponentScan(basePackages = {"com.dota2"})
@EnableAsync
@EnableScheduling
public class Dota2Application {

    public static void main(String[] args) {
        SpringApplication.run(Dota2Application.class, args);
    }
}
