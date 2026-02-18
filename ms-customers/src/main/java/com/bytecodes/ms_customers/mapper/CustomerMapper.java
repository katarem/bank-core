package com.bytecodes.ms_customers.mapper;

import com.bytecodes.ms_customers.model.SafeCustomer;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

import com.bytecodes.ms_customers.entity.CustomerEntity;
import com.bytecodes.ms_customers.model.Customer;

@Mapper
public interface CustomerMapper {

    CustomerMapper INSTANCE = Mappers.getMapper(CustomerMapper.class);
    
    Customer toModel(CustomerEntity entity);

    CustomerEntity toEntity(Customer model);

    SafeCustomer toSafeModel(CustomerEntity entity);

}
