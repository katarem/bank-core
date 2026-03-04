package com.bytecodes.ms_accounts.config;

import com.bytecodes.ms_accounts.mapper.AccountMapper;
import com.bytecodes.ms_accounts.mapper.TransactionMapper;
import org.mapstruct.factory.Mappers;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;

@TestConfiguration
public class TestConfig {

    @Bean
    AccountMapper accountMapper(){
        return Mappers.getMapper(AccountMapper.class);
    }

    @Bean
    TransactionMapper transactionMapper(){
        return Mappers.getMapper(TransactionMapper.class);
    }

}
