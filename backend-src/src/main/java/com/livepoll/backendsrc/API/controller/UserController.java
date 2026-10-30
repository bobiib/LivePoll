package com.livepoll.backendsrc.API.controller;

import com.livepoll.backendsrc.API.records.UserRecords;
import com.livepoll.backendsrc.API.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/user")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    //Auth Endpoints
    @GetMapping("auth")
    public ResponseEntity<UserRecords.ApiResponse<Void>> checkAuth(){
        return ResponseEntity.ok(UserRecords.ApiResponse.success("Valid Authentication"));
    }

    @PostMapping("auth/register")
    public ResponseEntity<UserRecords.ApiResponse<UserRecords.SuccesfullAuthResponseRecord>> registerUser(@RequestBody UserRecords.CredentialsSubmitRequestRecord credentialsSubmitRequestRecord){
        return ResponseEntity.ok(userService.RegisterUserWithCredentials(credentialsSubmitRequestRecord));
    }

    @PostMapping("auth/login")
    public ResponseEntity<UserRecords.ApiResponse<UserRecords.SuccesfullAuthResponseRecord>> loginUser(@RequestBody UserRecords.CredentialsSubmitRequestRecord credentialsSubmitRequestRecord){
        return ResponseEntity.ok(UserRecords.ApiResponse.success(
                "Log in was successful.",
                userService.LoginUserWithCredentials(credentialsSubmitRequestRecord)
        ));
    }

}
