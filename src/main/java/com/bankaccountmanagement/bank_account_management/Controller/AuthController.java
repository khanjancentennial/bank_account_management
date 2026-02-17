package com.bankaccountmanagement.bank_account_management.Controller;

// import java.util.HashMap;
// import java.util.Map;

// import org.springframework.http.HttpStatus;
// import org.springframework.http.ResponseEntity;
// import org.springframework.security.authentication.AuthenticationManager;
// import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
// import org.springframework.security.core.Authentication;
// import org.springframework.web.bind.annotation.GetMapping;
// import org.springframework.web.bind.annotation.PostMapping;
// import org.springframework.web.bind.annotation.RequestBody;
// import org.springframework.web.bind.annotation.RequestMapping;
// import org.springframework.web.bind.annotation.RestController;

// import com.bankaccountmanagement.bank_account_management.Config.JwtService;
// import com.bankaccountmanagement.bank_account_management.DTO.LoginDTO;
// import com.bankaccountmanagement.bank_account_management.DTO.RegistrationDTO;
// import com.bankaccountmanagement.bank_account_management.DTO.UserResponseDto;
// import com.bankaccountmanagement.bank_account_management.Model.UserModel;
// import com.bankaccountmanagement.bank_account_management.Services.UserService;

// import lombok.RequiredArgsConstructor;

// @RestController
// @RequestMapping("/api/auth")
// @RequiredArgsConstructor
// public class AuthController {

//     private final UserService userService;
//     private final JwtService jwtService;
//     private final AuthenticationManager authenticationManager;

//     @PostMapping("/register")
//     public ResponseEntity<Map<String, String>> register(@RequestBody RegistrationDTO request) {
//         userService.registerNewUser(
//                 request.getUsername(),
//                 request.getPassword(),
//                 request.getEmail(),
//                 request.getFirstName(),
//                 request.getLastName(),
//                 request.getAddress(),
//                 request.getAccountNumber(),
//                 request.getPhoneNumber(),
//                 request.getBalance()
//         );

//         Map<String, String> response = new HashMap<>();
//         try {
//             response.put("Message", "User registered successfully.");
//             response.put("Status", String.valueOf(HttpStatus.CREATED.value()));
//             return ResponseEntity.status(HttpStatus.CREATED).body(response);
//         } catch (IllegalArgumentException e) {
//             response.put("Error", e.getMessage());
//             return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
//         }
//         // return ResponseEntity.status(HttpStatus.CREATED)
//         //         .body({"Message": "User registered successfully", "Username": request.getUsername()});
//     }

//     @PostMapping("/login")
//     public ResponseEntity<Map<String, String>> login(@RequestBody LoginDTO request) {
//         Authentication authentication = authenticationManager.authenticate(
//                 new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword())
//         );
//         Map<String, String> response = new HashMap<>();
//         try {
//             if (authentication.isAuthenticated()) {
//                 UserModel userDetails = (UserModel) authentication.getPrincipal();
//                 String token = jwtService.generateToken(userDetails);
                
//                 response.put("Status", String.valueOf(HttpStatus.OK.value()));
//                 response.put("token", token);
//                 response.put("Message", "Login successful");
//                 response.put("Username", userDetails.getUsername());
//                 return ResponseEntity.ok(response);
//             }
//             else {
//                 response.put("Error", "Invalid credentials");
//                 return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
//             }
//         } catch (Exception e) {
//             response.put("Error", e.getMessage());
//             return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
//         }
//     }

//    @GetMapping("/account-details")
//     public ResponseEntity<Map<String, UserResponseDto>> getAccountDetails(Authentication authentication) {
//         if (authentication == null || !authentication.isAuthenticated()) {
//             return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
//         }
        
//         UserModel user = (UserModel) authentication.getPrincipal();
//         UserResponseDto userDto = new UserResponseDto(
//                 user.getId(),
//                 user.getUsername(),
//                 user.getEmail(),
//                 user.getFirstName(),
//                 user.getLastName(),
//                 user.getAddress(),
//                 user.getAccountNumber(),
//                 user.getPhoneNumber(),
//                 user.getBalance()
//         );
//         Map<String, UserResponseDto> response = new HashMap<>();
//         response.put("User Details", userDto);
//         return ResponseEntity.ok(response);
//     }
// }



import java.util.HashMap;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.LockedException;
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
import com.bankaccountmanagement.bank_account_management.Services.AuditService;
import com.bankaccountmanagement.bank_account_management.Services.UserService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserService userService;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;
    private final AuditService auditService;

    @PostMapping("/register")
    public ResponseEntity<Map<String, String>> register(@Valid @RequestBody RegistrationDTO request) {
        Map<String, String> response = new HashMap<>();
        
        try {
            UserModel user = userService.registerNewUser(
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

            response.put("message", "User registered successfully");
            response.put("username", user.getUsername());
            response.put("accountNumber", user.getMaskedAccountNumber()); // Masked
            response.put("status", String.valueOf(HttpStatus.CREATED.value()));
            
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
            
        } catch (IllegalArgumentException e) {
            response.put("error", e.getMessage());
            response.put("status", String.valueOf(HttpStatus.BAD_REQUEST.value()));
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        } catch (RuntimeException e) {
            response.put("error", e.getMessage());
            response.put("status", String.valueOf(HttpStatus.CONFLICT.value()));
            return ResponseEntity.status(HttpStatus.CONFLICT).body(response);
        }
    }

    @PostMapping("/login")
    public ResponseEntity<Map<String, String>> login(@Valid @RequestBody LoginDTO request) {
        Map<String, String> response = new HashMap<>();
        
        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword())
            );

            if (authentication.isAuthenticated()) {
                UserModel userDetails = (UserModel) authentication.getPrincipal();
                
                // Check if account is locked
                if (userDetails.isAccountLocked()) {
                    auditService.logAction(
                        userDetails.getId(),
                        userDetails.getUsername(),
                        "LOGIN_ATTEMPT_LOCKED",
                        "BLOCKED",
                        "Login attempt on locked account"
                    );
                    response.put("error", "Account is locked. Please contact support.");
                    response.put("status", String.valueOf(HttpStatus.FORBIDDEN.value()));
                    return ResponseEntity.status(HttpStatus.FORBIDDEN).body(response);
                }
                
                String token = jwtService.generateToken(userDetails);
                
                // Record successful login and reset failed attempts
                userService.recordSuccessfulLogin(userDetails.getUsername());
                
                response.put("status", String.valueOf(HttpStatus.OK.value()));
                response.put("token", token);
                response.put("message", "Login successful");
                response.put("username", userDetails.getUsername());
                response.put("role", userDetails.getRole().name());
                
                return ResponseEntity.ok(response);
            }
            
        } catch (BadCredentialsException e) {
            // Record failed login attempt
            userService.recordFailedLoginAttempt(request.getUsername());
            
            response.put("error", "Invalid username or password");
            response.put("status", String.valueOf(HttpStatus.UNAUTHORIZED.value()));
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
            
        } catch (LockedException e) {
            response.put("error", "Account is locked. Please contact support.");
            response.put("status", String.valueOf(HttpStatus.FORBIDDEN.value()));
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(response);
            
        } catch (Exception e) {
            auditService.logFailure(request.getUsername(), "LOGIN", e.getMessage());
            
            response.put("error", "Authentication failed");
            response.put("status", String.valueOf(HttpStatus.UNAUTHORIZED.value()));
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
        }
        
        response.put("error", "Authentication failed");
        response.put("status", String.valueOf(HttpStatus.UNAUTHORIZED.value()));
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
    }

    @GetMapping("/account-details")
    public ResponseEntity<Map<String, Object>> getAccountDetails(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        
        UserModel user = (UserModel) authentication.getPrincipal();
        
        // Return masked sensitive data
        UserResponseDto userDto = new UserResponseDto(
                user.getId(),
                user.getUsername(),
                user.getMaskedEmail(), // Masked
                user.getFirstName(),
                user.getLastName(),
                user.getAddress(),
                user.getMaskedAccountNumber(), // Masked
                user.getMaskedPhoneNumber(), // Masked
                user.getBalance()
        );
        
        Map<String, Object> response = new HashMap<>();
        response.put("userDetails", userDto);
        response.put("role", user.getRole().name());
        
        // Log account details access
        auditService.logAction(
            user.getId(),
            user.getUsername(),
            "VIEW_ACCOUNT_DETAILS",
            "SUCCESS",
            "User viewed their account details"
        );
        
        return ResponseEntity.ok(response);
    }

    @GetMapping("/logout")
    public ResponseEntity<Map<String, String>> logout(Authentication authentication) {
        if (authentication != null && authentication.isAuthenticated()) {
            UserModel user = (UserModel) authentication.getPrincipal();
            
            auditService.logAction(
                user.getId(),
                user.getUsername(),
                "LOGOUT",
                "SUCCESS",
                "User logged out"
            );
        }
        
        Map<String, String> response = new HashMap<>();
        response.put("message", "Logged out successfully");
        response.put("status", String.valueOf(HttpStatus.OK.value()));
        
        return ResponseEntity.ok(response);
    }
}