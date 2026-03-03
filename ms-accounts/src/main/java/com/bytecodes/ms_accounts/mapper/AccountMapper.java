package com.bytecodes.ms_accounts.mapper;

import com.bytecodes.ms_accounts.dto.request.RegisterAccountRequest;
import com.bytecodes.ms_accounts.dto.response.GetAccountResponse;
import com.bytecodes.ms_accounts.dto.response.RegisterAccountResponse;
import com.bytecodes.ms_accounts.entity.AccountEntity;
import com.bytecodes.ms_accounts.model.Account;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

@Mapper(componentModel = "spring")
public interface AccountMapper {

    Account toModel(AccountEntity entity);

    AccountEntity toEntity(Account model);

    RegisterAccountResponse toRegisterResponse(Account model);

    GetAccountResponse toGetAccountResponse(Account model);

    Account toModel(RegisterAccountRequest request);

}
