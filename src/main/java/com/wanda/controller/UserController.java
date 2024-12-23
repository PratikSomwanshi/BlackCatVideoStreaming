package com.wanda.controller;

import com.wanda.dto.AccessToken;
import com.wanda.entity.Users;
import com.wanda.service.GoogleOAuthService;
import com.wanda.service.UserService;
import com.wanda.utils.exceptions.CustomException;
import com.wanda.utils.exceptions.enums.ErrorCode;
import com.wanda.utils.exceptions.enums.SuccessCode;
import com.wanda.utils.exceptions.response.LoginResponse;
import com.wanda.utils.exceptions.response.SuccessResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
public class UserController {

    private UserService userService;
    private GoogleOAuthService googleOAuthService;

    public UserController(UserService userService, GoogleOAuthService googleOAuthService) {
        this.userService = userService;
        this.googleOAuthService = googleOAuthService;
    }


    @PostMapping("/user")
    public ResponseEntity<?> getUser(@RequestBody Users user) {

        try {
            var existingUser = userService.getUserByEmail(user.getEmail());



            var success = new SuccessResponse<>(
                    "Successfully saved the user",
                    SuccessCode.GENERAL_SUCCESS,
                    existingUser
            );

            return ResponseEntity.ok(success);
        }catch(Exception e) {
            throw new CustomException(e.getMessage(), HttpStatus.NOT_FOUND, ErrorCode.USER_NOT_FOUND);
        }
    }


    @PostMapping("/register")
    public ResponseEntity<SuccessResponse<Users>> register(@RequestBody Users user){
        Users savedUser = this.userService.saveUser(user);

        var success = new SuccessResponse<Users>(
                "Successfully saved the user",
                SuccessCode.GENERAL_SUCCESS,
                savedUser
        );

        return ResponseEntity.ok(success);
    }

    @PostMapping("/login")
    public ResponseEntity<SuccessResponse<LoginResponse>> login(@RequestBody Users user){
        LoginResponse loginResponse = this.userService.verify(user);

        SuccessResponse<LoginResponse> success = new SuccessResponse<>(
                "Successfully generated the token",
                SuccessCode.GENERAL_SUCCESS,
                loginResponse
        );

        return ResponseEntity.ok(success);
    }


    @PostMapping("/google/login")
    public ResponseEntity<SuccessResponse<?>> googleLogin(@RequestBody AccessToken accessToken){

        var b = this.googleOAuthService.validateGoogleOAuthToken(accessToken.getAccessToken());


        SuccessResponse<?> success = new SuccessResponse<>(
                "Successfully generated the token",
                SuccessCode.GENERAL_SUCCESS,
                b
        );

        return ResponseEntity.ok(success);
    }
}