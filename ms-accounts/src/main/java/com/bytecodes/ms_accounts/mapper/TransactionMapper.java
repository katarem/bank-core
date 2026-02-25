package com.bytecodes.ms_accounts.mapper;

import com.bytecodes.ms_accounts.entity.TransactionEntity;
import com.bytecodes.ms_accounts.model.Transaction;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

public interface TransactionMapper {

    TransactionMapper INSTANCE = Mappers.getMapper(TransactionMapper.class);

    Transaction toModel(TransactionEntity entity);

    TransactionEntity toEntity(Transaction model);

}
