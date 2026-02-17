package com.bankaccountmanagement.bank_account_management.Model;

// import java.time.LocalDateTime;
// import java.util.Collection;
// import java.util.List;

// import org.hibernate.annotations.CreationTimestamp;
// import org.springframework.security.core.GrantedAuthority;
// import org.springframework.security.core.userdetails.UserDetails;

// import jakarta.persistence.Column;
// import jakarta.persistence.Entity;
// import jakarta.persistence.GeneratedValue;
// import jakarta.persistence.GenerationType;
// import jakarta.persistence.Id;
// import jakarta.persistence.Table;
// import lombok.Data;
// import lombok.NoArgsConstructor;

// @Entity
// @Table(name = "users")
// @Data
// @NoArgsConstructor
// public class UserModel implements UserDetails {

//     @Id
//     @GeneratedValue(strategy = GenerationType.IDENTITY)

//     @Column(name = "id")
//     private Long id;
    
//     @Column(name = "username", nullable = false, unique = true, length = 50)
//     private String username;

//     @Column(name = "password", nullable = false, length = 100)
//     private String password;

//     @Column(name = "email", nullable = false, unique = true, length = 100)
//     private String email;

//     @Column(name = "first_name", nullable = false, length = 50)
//     private String firstName;

//     @Column(name = "last_name", nullable = false, length = 50)
//     private String lastName;

//     @Column(name = "address", nullable = false, length = 255)
//     private String address;

//     @Column(name = "phone_number", nullable = false, unique = true, length = 15)
//     private String phoneNumber;

//     @Column(name = "account_number", nullable = false, unique = true, length = 20)
//     private String accountNumber;

//     @Column(name = "balance", nullable = false )
//     private Double balance;

//     @CreationTimestamp //Automatically set on creation
//     @Column(name = "registration_date", nullable = false, updatable = false)
//     private LocalDateTime registrationDate;

//     @Override
//     public Collection<? extends GrantedAuthority> getAuthorities() {
//         return List.of(); // Define roles if needed
//     }

//     @Override
//     public boolean isAccountNonExpired() {
//         return true;
//     }

//     @Override
//     public boolean isAccountNonLocked() {
//         return true;
//     }

//     @Override
//     public boolean isCredentialsNonExpired() {
//         return true;
//     }

//     @Override
//     public boolean isEnabled() {
//         return true;
//     }
// }



import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

import org.hibernate.annotations.CreationTimestamp;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "users")
@Data
@NoArgsConstructor
public class UserModel implements UserDetails {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;
    
    @Column(name = "username", nullable = false, unique = true, length = 50)
    private String username;

    @JsonIgnore // Never expose password in JSON responses
    @Column(name = "password", nullable = false, length = 100)
    private String password;

    @Column(name = "email", nullable = false, unique = true, length = 100)
    private String email;

    @Column(name = "first_name", nullable = false, length = 50)
    private String firstName;

    @Column(name = "last_name", nullable = false, length = 50)
    private String lastName;

    @Column(name = "address", nullable = false, length = 255)
    private String address;

    @Column(name = "phone_number", nullable = false, unique = true, length = 15)
    private String phoneNumber;

    @Column(name = "account_number", nullable = false, unique = true, length = 20)
    private String accountNumber;

    @Column(name = "balance", nullable = false)
    private Double balance;

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false, length = 20)
    private Role role = Role.USER; // Default role

    @Column(name = "account_locked", nullable = false)
    private boolean accountLocked = false;

    @Column(name = "failed_login_attempts", nullable = false)
    private int failedLoginAttempts = 0;

    @CreationTimestamp
    @Column(name = "registration_date", nullable = false, updatable = false)
    private LocalDateTime registrationDate;

    // Data masking methods for sensitive information
    @JsonProperty("maskedAccountNumber")
    public String getMaskedAccountNumber() {
        if (accountNumber == null || accountNumber.length() < 4) {
            return "****";
        }
        return "****" + accountNumber.substring(accountNumber.length() - 4);
    }

    @JsonProperty("maskedPhoneNumber")
    public String getMaskedPhoneNumber() {
        if (phoneNumber == null || phoneNumber.length() < 4) {
            return "***-***-****";
        }
        return "***-***-" + phoneNumber.substring(phoneNumber.length() - 4);
    }

    @JsonProperty("maskedEmail")
    public String getMaskedEmail() {
        if (email == null || !email.contains("@")) {
            return "***@***.com";
        }
        String[] parts = email.split("@");
        String localPart = parts[0];
        if (localPart.length() <= 2) {
            return "***@" + parts[1];
        }
        return localPart.charAt(0) + "***@" + parts[1];
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + role.name()));
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return !accountLocked;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return !accountLocked;
    }

    // Method to increment failed login attempts
    public void incrementFailedAttempts() {
        this.failedLoginAttempts++;
        if (this.failedLoginAttempts >= 5) {
            this.accountLocked = true;
        }
    }

    // Method to reset failed login attempts on successful login
    public void resetFailedAttempts() {
        this.failedLoginAttempts = 0;
    }
}