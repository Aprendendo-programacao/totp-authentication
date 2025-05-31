# TOTP Authentication Demo

This project is a simple Spring Boot application demonstrating 2FA authentication using TOTP (Time-based One-Time Password), compatible with Google Authenticator, Authy, and similar apps.

## Features
- User registration and login
- Enable/disable TOTP 2FA per user
- TOTP QR code generation for easy setup in authenticator apps
- PostgreSQL database integration

## Prerequisites
- Java 17+
- Maven
- Docker & Docker Compose

## Getting Started

### 1. Clone the repository
```
git clone git@github.com:Aprendendo-programacao/totp-authentication.git
cd totp-authentication
```

### 2. Start PostgreSQL with Docker Compose
```
docker-compose up -d
```
This will start a PostgreSQL 17.4 instance with the following credentials:
- Database: `totp_auth_db`
- User: `totp_user`
- Password: `totp_pass_123`

### 3. Build and run the application
```
./mvnw spring-boot:run
```

The application will start on [http://localhost:8080](http://localhost:8080).

## Usage
1. Register a new user at `/register`.
2. Login at `/login`.
3. After login, you will be prompted to enable TOTP 2FA:
   - Scan the QR code with your authenticator app (Google Authenticator, Authy, etc).
   - Enter the 6-digit code from your app to activate 2FA.
4. On subsequent logins, you will be required to enter the TOTP code.

## Notes
- Passwords are stored in plain text for demo purposes. **Do not use in production!**
- TOTP validation is implemented according to RFC 6238.
- The UI is intentionally minimal for demonstration and testing.

## Stopping the Database
To stop the PostgreSQL container:
```
docker-compose down
```

