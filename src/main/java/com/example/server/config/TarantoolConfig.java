package com.example.server.config;

import io.tarantool.client.crud.TarantoolCrudClient;
import io.tarantool.client.factory.TarantoolFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class TarantoolConfig {

    @Value("${tarantool.host:127.0.0.1}")
    private String host;

    @Value("${tarantool.port:3301}")
    private int port;

    @Value("${tarantool.user}")
    private String user;

    @Value("${tarantool.password}")
    private String password;

    @Bean
    public TarantoolCrudClient tarantoolClient() throws Exception {
        return TarantoolFactory.crud()
                .withHost(host)
                .withPort(port)
                .withUser(user)
                .withPassword(password)
                .build();
    }
}
