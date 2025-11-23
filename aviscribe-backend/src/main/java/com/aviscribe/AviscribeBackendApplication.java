package com.aviscribe;

import com.aviscribe.security.JwtProperties;
import com.aviscribe.security.RateLimitProperties;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@MapperScan("com.aviscribe.mapper")
@EnableAsync // 关键：开启异步支持
@EnableConfigurationProperties({JwtProperties.class, RateLimitProperties.class})
public class AviscribeBackendApplication {

    public static void main(String[] args) {
        SpringApplication.run(AviscribeBackendApplication.class, args);
    }

}