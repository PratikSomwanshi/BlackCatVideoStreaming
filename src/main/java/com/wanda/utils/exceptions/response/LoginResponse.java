package com.wanda.utils.exceptions.response;

import lombok.Data;

@Data
public class LoginResponse {
    private String email;
    private String username;
    private String token;
    private Boolean isPremiumUser =  false;

    public LoginResponse(String email, String username, String token, Boolean isPremiumUser) {
        this.email = email;
        this.username = username;
        this.token = token;
        this.isPremiumUser = isPremiumUser;
    }
}
