package com.bytecodes.ms_accounts.mapper;

import com.bytecodes.ms_accounts.dto.request.RegisterAccountRequest;
import com.bytecodes.ms_accounts.dto.response.AccountSummary;
import com.bytecodes.ms_accounts.dto.response.GetAccountResponse;
import com.bytecodes.ms_accounts.dto.response.RegisterAccountResponse;
import com.bytecodes.ms_accounts.entity.AccountEntity;
import com.bytecodes.ms_accounts.model.Account;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface AccountMapper {

    Account toModel(AccountEntity entity);

    AccountSummary toSummary(AccountEntity entity);

    AccountEntity toEntity(Account model);

    RegisterAccountResponse toRegisterResponse(Account model);

    GetAccountResponse toGetAccountResponse(Account model);

    Account toModel(RegisterAccountRequest request);

}
