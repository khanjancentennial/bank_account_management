# 🏦 Bank Account Management Microservice

A secure banking microservice for account management with JWT authentication, comprehensive audit logging, and event-driven architecture.

![Java](https://img.shields.io/badge/Java-17-orange)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.2.0-brightgreen)
![PostgreSQL](https://img.shields.io/badge/PostgreSQL-15-blue)
![Kafka](https://img.shields.io/badge/Apache%20Kafka-3.5-black)

---

## 📋 Overview

This microservice handles user account management, authentication, and balance updates for a banking system. 
It consumes transaction events from Kafka and maintains account balances with full audit trail for compliance.

---

## 🛠️ Technologies Used

**Backend Framework:**
- Java 17
- Spring Boot 3.5.5
- Spring Security
- Spring Data JPA
- Spring Kafka

**Database:**
- PostgreSQL
- HikariCP (Connection Pooling)

**Messaging:**
- Apache Kafka 3.5

**Security:**
- JWT (JSON Web Tokens)
- BCrypt Password Encryption
- Bucket4j (Rate Limiting)

**Build Tool:**
- Maven

---

## ✨ Features

### 🔐 Security Features
- **JWT Authentication** - Token-based stateless authentication
- **Role-Based Access Control** - USER, ADMIN, AUDITOR roles
- **Rate Limiting** - Prevents brute force attacks (5 login attempts per 15 min)
- **Account Locking** - Automatic lock after 5 failed login attempts
- **Password Encryption** - BCrypt with strength 12
- **Data Masking** - Sensitive info protected in responses
- **Security Headers** - XSS, clickjacking, MIME-sniffing protection

### 📊 Audit & Compliance
- **Comprehensive Audit Logging** - Every action tracked with IP, user agent, timestamp
- **Asynchronous Logging** - Non-blocking performance
- **Indexed Database** - Fast compliance queries

### 💰 Core Banking Operations
- User registration with auto-generated account numbers
- Account balance management
- Deposit, withdrawal, transfer operations (via Kafka)
- Transaction validation and concurrency control

---

## 🗄️ Database Schema

### Users Table
```sql
users
├── id (BIGSERIAL)
├── username (VARCHAR)
├── password (VARCHAR) - BCrypt encrypted
├── email (VARCHAR)
├── first_name (VARCHAR)
├── last_name (VARCHAR)
├── address (VARCHAR)
├── phone_number (VARCHAR)
├── account_number (VARCHAR) - Auto-generated
├── balance (DOUBLE PRECISION)
├── role (VARCHAR) - USER, ADMIN, AUDITOR
├── account_locked (BOOLEAN)
├── failed_login_attempts (INTEGER)
└── registration_date (TIMESTAMP)
```

### Audit Logs Table
```sql
audit_logs
├── id (BIGSERIAL)
├── user_id (BIGINT)
├── username (VARCHAR)
├── action (VARCHAR) - LOGIN, DEPOSIT, WITHDRAWAL, etc.
├── ip_address (VARCHAR)
├── user_agent (VARCHAR)
├── status (VARCHAR) - SUCCESS, FAILURE, BLOCKED
├── details (TEXT)
├── amount (DOUBLE PRECISION)
├── account_number (VARCHAR)
├── timestamp (TIMESTAMP)
└── error_message (TEXT)
```

---

## 📡 API Endpoints

### Authentication APIs

#### 1. Register User
```http
POST /api/auth/register
Content-Type: application/json

{
  "username": "john_doe",
  "password": "SecurePass123",
  "email": "john@example.com",
  "firstName": "John",
  "lastName": "Doe",
  "address": "123 Main St",
  "phoneNumber": "1234567890"
}
```

**Response:**
```json
{
  "message": "User registered successfully",
  "username": "john_doe",
  "accountNumber": "****0001",
  "status": "201"
}
```

---

#### 2. Login
```http
POST /api/auth/login
Content-Type: application/json

{
  "username": "john_doe",
  "password": "SecurePass123"
}
```

**Response:**
```json
{
  "status": "200",
  "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "message": "Login successful",
  "username": "john_doe",
  "role": "USER"
}
```

---

#### 3. Get Account Details
```http
GET /api/auth/account-details
Authorization: Bearer <JWT_TOKEN>
```

**Response:**
```json
{
  "userDetails": {
    "id": 1,
    "username": "john_doe",
    "email": "j***@example.com",
    "firstName": "John",
    "lastName": "Doe",
    "accountNumber": "****0001",
    "phoneNumber": "***-***-7890",
    "balance": 1000.00
  },
  "role": "USER"
}
```

---

#### 4. Logout
```http
GET /api/auth/logout
Authorization: Bearer <JWT_TOKEN>
```

**Response:**
```json
{
  "message": "Logged out successfully",
  "status": "200"
}
```

---

## 🔄 Kafka Integration

**Topic:** `bank-transactions`

**Consumer:** Listens to transaction events and updates account balances

**Event Types:**
- `DEPOSIT` - Increases account balance
- `WITHDRAWAL` - Decreases account balance
- `TRANSFER` - Transfers between accounts

**Event Format:**
```json
{
  "transactionId": 12345,
  "senderAccountNumber": "012345678900001",
  "receiverAccountNumber": "012345678900002",
  "amount": 500.00,
  "transactionType": "TRANSFER"
}
```

---

## 🚀 Getting Started

### Prerequisites
- Java 17+
- Maven 3.8+
- PostgreSQL 15+
- Apache Kafka 3.5+

### Installation

1. **Clone repository**
```bash
git clone https://github.com/yourusername/bank-account-management.git
cd bank-account-management
```

2. **Set up database**
```bash
psql -U postgres
CREATE DATABASE bankapp_usermanagement_db;
\q
psql -U postgres -d bankapp_usermanagement_db -f database_migration.sql
```

3. **Configure environment**
```bash
cp .env.example .env
# Edit .env with your database and Kafka details
```

4. **Build and run**
```bash
mvn clean install
mvn spring-boot:run
```

Service runs on `http://localhost:8080`

---

## 🔒 Security Configuration

### Rate Limits
- **Login attempts:** 5 per 15 minutes
- **API calls:** 100 per minute
- **Transactions:** 20 per minute

### Business Rules
- **Min transaction:** $0.01
- **Max transaction:** $50,000.00
- **Concurrency:** SERIALIZABLE isolation level

---

## 📊 HTTP Status Codes

| Code | Description |
|------|-------------|
| 200 | Success |
| 201 | Created |
| 400 | Bad Request |
| 401 | Unauthorized |
| 403 | Forbidden (account locked) |
| 404 | Not Found |
| 429 | Too Many Requests |
| 500 | Internal Server Error |

---

## 🏗️ Project Structure
```
src/main/java/com/bankaccountmanagement/
├── Config/
│   ├── SecurityConfig.java
│   ├── JwtAuth.java
│   ├── JwtService.java
│   ├── RateLimitingFilter.java
│   └── SecurityHeadersFilter.java
├── Controller/
│   └── AuthController.java
├── Model/
│   ├── UserModel.java
│   ├── Role.java
│   └── AuditLog.java
├── Repository/
│   ├── UserRepository.java
│   └── AuditLogRepository.java
├── Services/
│   ├── UserService.java
│   ├── AccountService.java
│   ├── AuditService.java
│   ├── RateLimitService.java
│   └── KafkaConsumerService.java
├── Exception/
│   ├── GlobalExceptionHandler.java
│   └── InsufficientFundsException.java
└── Event/
    └── TransactionEvent.java
```

---


## 🙏 Acknowledgments

Built with Spring Boot, PostgreSQL, and Apache Kafka for secure banking operations.
