package com.livepoll.backendsrc.API.service;

import com.livepoll.backendsrc.API.entity.jpa.UserEntity;
import com.livepoll.backendsrc.API.exception.customExceptions.DuplicateUserException;
import com.livepoll.backendsrc.API.exception.customExceptions.GenerateTokenException;
import com.livepoll.backendsrc.API.exception.customExceptions.InvalidUserException;
import com.livepoll.backendsrc.API.records.UserRecords;
import com.livepoll.backendsrc.API.repository.jpa.UserRepository;
import com.livepoll.backendsrc.API.service.tokens.TokenService;
import com.livepoll.backendsrc.Util.RetrievalUtil;
import com.nimbusds.jose.JOSEException;
import jakarta.transaction.Transactional;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final TokenService tokenService;
    private final AuthenticationManager authenticationManager;
    private final RetrievalUtil retrievalUtil;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder, TokenService tokenService, AuthenticationManager authenticationManager, RetrievalUtil retrievalUtil) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.tokenService = tokenService;
        this.authenticationManager = authenticationManager;
        this.retrievalUtil = retrievalUtil;
    }

    //Register Logic
    @Transactional
    public UserRecords.ApiResponse<UserRecords.SuccesfullAuthResponseRecord> RegisterUserWithCredentials(UserRecords.CredentialsSubmitRequestRecord credentialsSubmitRequestRecord) {
        UserEntity user = new UserEntity();

        user.setEmail(credentialsSubmitRequestRecord.email());
        user.setPassword(passwordEncoder.encode(credentialsSubmitRequestRecord.password()));

        try {
            userRepository.save(user);
        } catch (Exception e) {
            if (e.getMessage().toUpperCase().contains("DUPLICATE KEY VALUE")){
                throw new DuplicateUserException("User with Email: '" + credentialsSubmitRequestRecord.email() + "' already exists", "DUPLICATE_USER");
            }else {
                throw new InvalidUserException("Could not create user: " + credentialsSubmitRequestRecord.email(), "INVALID_USER");
            }
        }

        String JWT;
        try{
            JWT = tokenService.generateJWTToken(user);
        } catch (JOSEException e){
            throw new GenerateTokenException("Error login User in. Please try again.", "GENERATE_TOKEN_ERROR");
        }

        return UserRecords.ApiResponse.success(
                "User with Email: '" + credentialsSubmitRequestRecord.email() + "' registered successfully",
                new UserRecords.SuccesfullAuthResponseRecord(
                        JWT,
                        user.getEmail()
                )
        );
    }

    //Login Logic
    @Transactional
    public UserRecords.SuccesfullAuthResponseRecord LoginUserWithCredentials(UserRecords.CredentialsSubmitRequestRecord credentialsSubmitRequestRecord) {

            Authentication authenticationRequest = UsernamePasswordAuthenticationToken.unauthenticated(
                    credentialsSubmitRequestRecord.email(),
                    credentialsSubmitRequestRecord.password()
            );
            authenticationManager.authenticate(authenticationRequest);

            UserEntity user = retrievalUtil.getUserEntityFromEmail(credentialsSubmitRequestRecord.email());

                String JWT;
                try{
                    JWT = tokenService.generateJWTToken(user);
                } catch (JOSEException e){
                    throw new GenerateTokenException("Error login User in. Please try again.", "GENERATE_TOKEN_ERROR");
                }

                return new UserRecords.SuccesfullAuthResponseRecord(
                        JWT,
                        user.getEmail()
                );
    }
}