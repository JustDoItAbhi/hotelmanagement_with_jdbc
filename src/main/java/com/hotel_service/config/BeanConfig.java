package com.hotel_service.config;

import org.modelmapper.ModelMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;

@Configuration
public class BeanConfig {
    @Bean
    public ModelMapper mapper(){
        return new ModelMapper();
    }
    @Bean
    public JdbcTemplate jdbcTemplate(DataSource dataSource) {
        JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);

        jdbcTemplate.setFetchSize(1000);
        jdbcTemplate.setMaxRows(100000);
        jdbcTemplate.setQueryTimeout(60); // seconds

        return jdbcTemplate;
    }
    @Bean
    public RedisTemplate<String,Object> redisTemplate(RedisConnectionFactory factory){
        RedisTemplate<String, Object>template=new RedisTemplate<>();
        template.setConnectionFactory(factory);
        return template;
    }
}
