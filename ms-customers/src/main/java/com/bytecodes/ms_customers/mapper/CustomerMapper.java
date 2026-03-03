package com.bytecodes.ms_customers.mapper;

import com.bytecodes.ms_customers.dto.request.RegisterRequest;
import com.bytecodes.ms_customers.dto.response.GetCustomerResponse;
import com.bytecodes.ms_customers.dto.response.GetProfileResponse;
import com.bytecodes.ms_customers.dto.response.RegisterResponse;
import com.bytecodes.ms_customers.dto.response.UpdateProfileResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

import com.bytecodes.ms_customers.entity.CustomerEntity;
import com.bytecodes.ms_customers.model.Customer;

@Mapper
public interface CustomerMapper {

    CustomerMapper INSTANCE = Mappers.getMapper(CustomerMapper.class);
    
    Customer toModel(CustomerEntity entity);

    CustomerEntity toEntity(Customer model);

    Customer toModel(RegisterRequest request);

    @Mapping(target = "fullName", expression = "java(customer.getFirstName() + \" \" + customer.getLastName())")
    RegisterResponse toRegisterResponse(Customer customer);

    GetProfileResponse toGetProfileResponse(Customer customer);

    UpdateProfileResponse toUpdateProfileResponse(Customer customer);

    @Mapping(target = "fullName", expression = "java(customer.getFirstName() + \" \" + customer.getLastName())")
    GetCustomerResponse toGetCustomerResponse(Customer customer);

}
