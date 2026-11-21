package com.paravar.rest.users;

public class DuplicateEmailException extends RuntimeException {

    private final String email;

    public DuplicateEmailException(String email) {
        super("Email already registered");
        this.email = email;
    }

    public String email() {
        return email;
    }
}
