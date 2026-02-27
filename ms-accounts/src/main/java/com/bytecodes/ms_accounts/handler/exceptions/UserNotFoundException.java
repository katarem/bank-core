package com.bytecodes.ms_accounts.handler.exceptions;

<<<<<<< HEAD
import org.springframework.security.core.userdetails.UsernameNotFoundException;

public class UserNotFoundException extends UsernameNotFoundException {
=======
public class UserNotFoundException extends RuntimeException {
>>>>>>> 713e688cab228ea9621479719556596cb7f153dc

	public UserNotFoundException() {
		super("No existe el usuario");
	}
}
