package com.study;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan(basePackages = {"com.study.*.mapper"})
public class Seckill2Application {

    public static void main(String[] args) {
        SpringApplication.run(Seckill2Application.class, args);
    }

}
