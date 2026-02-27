package com.bytecodes.ms_accounts.handler.exceptions;

import org.springframework.security.core.userdetails.UsernameNotFoundException;

public class UserNotFoundException extends UsernameNotFoundException {

	public UserNotFoundException() {
		super("No existe el usuario");
	}
}
