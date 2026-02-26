package com.bytecodes.ms_accounts.mapper;

import com.bytecodes.ms_accounts.entity.AccountEntity;
import com.bytecodes.ms_accounts.model.Account;
import com.bytecodes.ms_accounts.response.AccountSummary;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

@Mapper
public interface AccountMapper {

    AccountMapper INSTANCE = Mappers.getMapper(AccountMapper.class);

    Account toModel(AccountEntity entity);

    AccountSummary toSummary(AccountEntity entity);

    AccountEntity toEntity(Account model);

}
