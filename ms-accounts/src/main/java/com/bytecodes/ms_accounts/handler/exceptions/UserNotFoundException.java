package com.bytecodes.ms_accounts.handler.exceptions;

public class UserNotFoundException extends RuntimeException {

	public UserNotFoundException() {
		super("No existe el usuario");
	}
}
