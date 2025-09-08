package com.one211.startupscript.auth;

public class AuthService {
    private final String user;
    private final String password;

    public AuthService(String user, String password) {
        this.user = user;
        this.password = password;
    }

    public boolean authenticate(String username, String password) {
        return this.user.equals(username) && this.password.equals(password);
    }
}
