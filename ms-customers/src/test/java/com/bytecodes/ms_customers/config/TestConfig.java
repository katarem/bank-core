package com.bytecodes.ms_customers.config;

import com.bytecodes.ms_customers.mapper.CustomerMapper;
import org.mapstruct.factory.Mappers;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;

@TestConfiguration
public class TestConfig {

    @Bean
    CustomerMapper mapper() {
        return Mappers.getMapper(CustomerMapper.class);
    }

}
