package com.bankaccountmanagement.bank_account_management.Controller;

import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
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
import com.bankaccountmanagement.bank_account_management.Repository.UserRepository;
import com.bankaccountmanagement.bank_account_management.Services.AuditService;
import com.bankaccountmanagement.bank_account_management.Services.BankingRateLimitService;
import com.bankaccountmanagement.bank_account_management.Services.UserService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private static final Logger logger = LoggerFactory.getLogger(AuthController.class);

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

    /* @PostMapping("/login")
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
 */
@Autowired
private UserRepository userRepository;
@Autowired
private BankingRateLimitService rateLimitService;

@PostMapping("/login")
public ResponseEntity<?> login(@Valid @RequestBody LoginDTO loginRequest, HttpServletRequest request) {
    
    String ipAddress = getClientIp(request);
    String userAgent = request.getHeader("User-Agent");
    String username = loginRequest.getUsername();
    
    logger.info("Login attempt for user: {} from IP: {}", username, ipAddress);
    
    // Check all rate limits (3-layer protection)
    BankingRateLimitService.RateLimitResult rateLimitResult = 
        rateLimitService.checkLoginAllowed(username, ipAddress);
    
    if (!rateLimitResult.isAllowed()) {
        logger.warn("Rate limit exceeded for user: {} from IP: {} - Type: {}", 
            username, ipAddress, rateLimitResult.getLimitType());
        
        auditService.logFailedLogin(
            username,
            ipAddress,
            userAgent,
            "Rate limit exceeded: " + rateLimitResult.getLimitType()
        );
        
        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("error", "Too many attempts");
        errorResponse.put("message", rateLimitResult.getMessage());
        errorResponse.put("retryAfter", rateLimitResult.getRetryAfterMinutes() + " minutes");
        errorResponse.put("limitType", rateLimitResult.getLimitType());
        
        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).body(errorResponse);
    }

    try {
        // Authenticate user
        Authentication authentication = authenticationManager.authenticate(
            new UsernamePasswordAuthenticationToken(
                username,
                loginRequest.getPassword()
            )
        );

        SecurityContextHolder.getContext().setAuthentication(authentication);

        UserModel userDetails = (UserModel) authentication.getPrincipal();
        String jwt = jwtService.generateToken(userDetails);

        UserModel user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Reset failed login attempts in database
        user.setFailedLoginAttempts(0);
        user.setAccountLocked(false);
        userRepository.save(user);
        
        // Reset all rate limits for this user
        rateLimitService.resetUserLimits(username, ipAddress);

        // Log successful login
        auditService.logSuccessfulLogin(
                user.getId(),
                username,
                ipAddress,
                userAgent
        );

        logger.info("Successful login for user: {} from IP: {}", username, ipAddress);

        // Create response map
        Map<String, Object> response = new HashMap<>();
        response.put("status", String.valueOf(HttpStatus.OK.value()));
        response.put("token", jwt);
        response.put("message", "Login successful");
        response.put("id", user.getId());
        response.put("username", user.getUsername());
        response.put("email", user.getEmail());
        response.put("role", user.getRole().name());

return ResponseEntity.ok(response);

    } catch (LockedException e) {
        logger.warn("Login attempt for locked account: {}", username);
        
        auditService.logFailedLogin(
                username,
                ipAddress,
                userAgent,
                "Account locked due to multiple failed attempts"
        );

        Map<String, String> errorResponse = new HashMap<>();
        errorResponse.put("error", "Account locked");
        errorResponse.put("message", "Your account has been locked due to multiple failed login attempts. Please contact support or try 'Forgot Password'.");
        
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(errorResponse);

    } catch (BadCredentialsException e) {
        logger.warn("Invalid credentials for user: {} from IP: {}", username, ipAddress);

        // Increment failed attempt counter
        int failedAttempts = rateLimitService.incrementFailedAttempts(username);

        // Update user's failed login count in database
        userRepository.findByUsername(username).ifPresent(user -> {
            user.setFailedLoginAttempts(user.getFailedLoginAttempts() + 1);
            
            // Lock account after 5 failed attempts
            if (user.getFailedLoginAttempts() >= 5) {
                user.setAccountLocked(true);
                logger.warn("Account locked for user: {} after {} failed attempts", 
                    username, user.getFailedLoginAttempts());
            }
            
            userRepository.save(user);
        });

        // Send email alert after 3 failed attempts
        if (rateLimitService.shouldSendEmailAlert(username)) {
            logger.info("Sending security alert email for user: {}", username);
            // TODO: Implement email service
            // emailService.sendSecurityAlert(user.getEmail(), ipAddress);
        }

        // Log failed login
        auditService.logFailedLogin(
                username,
                ipAddress,
                userAgent,
                "Invalid credentials - Attempt #" + failedAttempts
        );

        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("error", "Invalid credentials");
        errorResponse.put("message", "Invalid username or password");
        errorResponse.put("attemptsRemaining", Math.max(0, 5 - failedAttempts));
        errorResponse.put("requiresCaptcha", rateLimitService.requiresCaptcha(username));
        
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(errorResponse);
    }
}

/**
 * Extract client IP address (handles proxy headers)
 */
private String getClientIp(HttpServletRequest request) {
    String xForwardedFor = request.getHeader("X-Forwarded-For");
    if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
        return xForwardedFor.split(",")[0].trim();
    }
    
    String xRealIp = request.getHeader("X-Real-IP");
    if (xRealIp != null && !xRealIp.isEmpty()) {
        return xRealIp;
    }
    
    return request.getRemoteAddr();
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