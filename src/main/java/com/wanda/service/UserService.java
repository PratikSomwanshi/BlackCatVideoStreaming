package com.wanda.service;

import com.wanda.dto.CustomUserDetails;
import com.wanda.entity.Users;
import com.wanda.repository.UsersRepository;
import com.wanda.utils.exceptions.CustomException;
import com.wanda.utils.exceptions.enums.ErrorCode;
import com.wanda.utils.exceptions.response.LoginResponse;
import com.wanda.utils.exceptions.response.TokenResponse;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class UserService {

    private final UsersRepository usersRepository;
    private final AuthenticationManager authManager;
    private final JWTService jwtService;

    public UserService(UsersRepository usersRepository, AuthenticationManager authManager, JWTService jwtService) {
        this.usersRepository = usersRepository;
        this.authManager = authManager;
        this.jwtService = jwtService;
    }

    //
    public Users getUserByEmail(String email){

        var user = this.usersRepository.findByEmail(email);

        if(user.isPresent()){
            return user.get();
        }

        throw new UsernameNotFoundException("Email Not found");
    }

    public Users saveUser(Users user) {

        Optional<Users> existingUser = this.usersRepository.findByEmail(user.getEmail());

        if(existingUser.isPresent()){
            throw new CustomException("User already exists", HttpStatus.NOT_FOUND, ErrorCode.USER_NOT_FOUND);
        }

        Users newUser = new Users();

        newUser.setEmail(user.getEmail());
        newUser.setUsername(user.getUsername());

        BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

        String hashedPassword = passwordEncoder.encode(user.getPassword());

        newUser.setPassword(hashedPassword);



        return this.usersRepository.save(newUser);
    }

    public LoginResponse verify(Users user) {
        try {
            Authentication authenticate = authManager.authenticate(
                    new UsernamePasswordAuthenticationToken(user.getEmail(), user.getPassword())
            );

            if (authenticate.isAuthenticated()) {
                CustomUserDetails fetchedUser = (CustomUserDetails) authenticate.getPrincipal();

                String token = jwtService.generate(user.getEmail());

                return new LoginResponse(fetchedUser.getUsername(),fetchedUser.getEmailUsername(), token, fetchedUser.getIsPremiumUser());

            }

            throw new CustomException("Authentication failed", HttpStatus.NOT_FOUND, ErrorCode.TOKEN_INVALID); // Should not reach here
        }  catch (BadCredentialsException e) {
            throw new CustomException("Bad Credentials", HttpStatus.NOT_FOUND, ErrorCode.BAD_CREDENTIALS); // Custom exception for password
        }

    }


    public String makeUserPremium(String email) {
        Optional<Users> existingUser = this.usersRepository.findByEmail(email);

        if(existingUser.isEmpty()){
            throw new CustomException("User not found", HttpStatus.NOT_FOUND, ErrorCode.USER_NOT_FOUND);
        }

        var user = existingUser.get();

        user.setIsPremiumUser(true);
        this.usersRepository.save(user);
        return email;
    }
}