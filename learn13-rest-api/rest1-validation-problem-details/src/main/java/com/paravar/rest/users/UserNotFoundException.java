package com.paravar.rest.users;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.ErrorResponseException;

import java.net.URI;

// ErrorResponseException carries its own ProblemDetail, so no handler method is needed
public class UserNotFoundException extends ErrorResponseException {

    public UserNotFoundException(long id) {
        super(HttpStatus.NOT_FOUND, problem(id), null);
    }

    private static ProblemDetail problem(long id) {
        ProblemDetail p = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, "No user with id " + id);
        p.setType(URI.create("https://example.test/problems/user-not-found"));
        p.setTitle("User not found");
        p.setProperty("userId", id);
        return p;
    }
}
