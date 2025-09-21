package com.bankaccountmanagement.bank_account_management.Controller;

import java.util.HashMap;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.bankaccountmanagement.bank_account_management.Config.JwtService;
import com.bankaccountmanagement.bank_account_management.DTO.LoginDTO;
import com.bankaccountmanagement.bank_account_management.DTO.RegistrationDTO;
import com.bankaccountmanagement.bank_account_management.DTO.UserResponseDto;
import com.bankaccountmanagement.bank_account_management.Model.UserModel;
import com.bankaccountmanagement.bank_account_management.Services.UserService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserService userService;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;

    @PostMapping("/register")
    public ResponseEntity<Map<String, String>> register(@RequestBody RegistrationDTO request) {
        userService.registerNewUser(
                request.getUsername(),
                request.getPassword(),
                request.getEmail(),
                request.getFirstName(),
                request.getLastName(),
                request.getAddress(),
                request.getAccountNumber(),
                request.getPhoneNumber(),
                request.getBalance()
        );

        Map<String, String> response = new HashMap<>();
        try {
            response.put("Message", "User registered successfully.");
            response.put("Status", String.valueOf(HttpStatus.CREATED.value()));
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (IllegalArgumentException e) {
            response.put("Error", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }
        // return ResponseEntity.status(HttpStatus.CREATED)
        //         .body({"Message": "User registered successfully", "Username": request.getUsername()});
    }

    @PostMapping("/login")
    public ResponseEntity<Map<String, String>> login(@RequestBody LoginDTO request) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword())
        );
        Map<String, String> response = new HashMap<>();
        try {
            if (authentication.isAuthenticated()) {
                UserModel userDetails = (UserModel) authentication.getPrincipal();
                String token = jwtService.generateToken(userDetails);
                
                response.put("Status", String.valueOf(HttpStatus.OK.value()));
                response.put("token", token);
                response.put("Message", "Login successful");
                response.put("Username", userDetails.getUsername());
                return ResponseEntity.ok(response);
            }
            else {
                response.put("Error", "Invalid credentials");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
            }
        } catch (Exception e) {
            response.put("Error", e.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
        }
    }

   @GetMapping("/account-details")
    public ResponseEntity<Map<String, UserResponseDto>> getAccountDetails(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        
        UserModel user = (UserModel) authentication.getPrincipal();
        UserResponseDto userDto = new UserResponseDto(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getFirstName(),
                user.getLastName(),
                user.getAddress(),
                user.getAccountNumber(),
                user.getPhoneNumber(),
                user.getBalance()
        );
        Map<String, UserResponseDto> response = new HashMap<>();
        response.put("User Details", userDto);
        return ResponseEntity.ok(response);
    }
}

