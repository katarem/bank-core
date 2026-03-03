package com.bytecodes.ms_accounts.mapper;

import com.bytecodes.ms_accounts.dto.request.DepositRequest;
import com.bytecodes.ms_accounts.dto.response.DepositResponse;
import com.bytecodes.ms_accounts.entity.TransactionEntity;
import com.bytecodes.ms_accounts.model.Transaction;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

@Mapper
public interface TransactionMapper {

    Transaction toModel(TransactionEntity entity);

    @Mapping(source = "description", target = "concept")
    Transaction toModel(DepositRequest request);

    TransactionEntity toEntity(Transaction model);

    @Mapping(source = "id", target = "transactionId")
    @Mapping(source = "concept", target = "description")
    @Mapping(source = "createdAt", target = "timestamp")
    DepositResponse toDepositResponse(Transaction model);

}
