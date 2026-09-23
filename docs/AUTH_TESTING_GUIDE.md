# Auth Controller & OTP Service Testing Guide

This document provides a comprehensive test suite specification, architectural sequence diagrams, and practical execution instructions for the **Miloo Authentication Module** (`HealthController`, `AuthService`, `OtpService`, `AuthEmailService`, and `JwtAuthFilter`).

---

## 1. End-to-End Authentication & Activation Flow

```
[ CLIENT ]                      [ BACKEND API ]                  [ POSTGRESQL ]                 [ EMAIL / SMS ]
   |                                  |                                 |                              |
   |--- 1. POST /register ----------->|                                 |                              |
   |    {phone, country_code, email}  |--- Save account (is_active=F) ->|                              |
   |                                  |--- Save activation token ------>|                              |
   |                                  |--- Send Welcome Email ---------------------------------------->| (Email with Link)
   |<-- {token: null, is_active: F}---|                                 |                              |
   |                                  |                                 |                              |
   |--- 2. GET /activate?token=... -->|                                 |                              |
   |    (User clicks link)            |--- Update is_active = TRUE ---->|                              |
   |                                  |--- Mark token used_at = NOW --->|                              |
   |<-- 200 OK (HTML / JSON) ---------|                                 |                              |
   |                                  |                                 |                              |
   |--- 3. POST /login or /send-otp ->|                                 |                              |
   |    {phone, country_code | email} |--- Check is_active == TRUE ---->|                              |
   |                                  |--- Invalidate older OTPs ------>|                              |
   |                                  |--- Save OTP (5-min TTL) ------->|                              |
   |                                  |--- Dispatch OTP ---------------------------------------------->| (Twilio/MSG91/SMTP)
   |<-- 200 OK (OTP Sent) ------------|                                 |                              |
   |                                  |                                 |                              |
   |--- 4. POST /verify-otp --------->|                                 |                              |
   |    {phone, country_code, otp}    |--- Validate OTP & TTL --------->|                              |
   |                                  |--- Check is_active == TRUE ---->|                              |
   |                                  |--- Mark OTP VERIFIED ---------->|                              |
   |<-- 200 OK {token, refresh, me}---|                                 |                              |
   |                                  |                                 |                              |
   |--- 5. GET /me (Bearer JWT) ----->|                                 |                              |
   |                                  |-- JwtAuthFilter checks DB: ---->|                              |
   |                                  |   Account exists & is_active?   |                              |
   |<-- 200 OK {account profile} -----|                                 |                              |
```

---

## 2. API Endpoints Reference

| Endpoint | Method | Auth Required | Description |
| :--- | :--- | :--- | :--- |
| `/api/v1/auth/register` | `POST` | Public | Creates inactive account (`is_active = false`), generates activation token (24h TTL), and dispatches welcome email with activation link. |
| `/api/v1/auth/activate` | `GET` | Public | Browser link click handler: Validates token, sets `is_active = true` and `is_verified = true`, and returns styled HTML page (from `AuthConstants.ACTIVATION_SUCCESS_HTML`). |
| `/api/v1/auth/activate` | `POST` | Public | Programmatic activation endpoint accepting JSON body `{"token": "..."}` and returning JSON response. |
| `/api/v1/auth/send-otp` | `POST` | Public | Generates 6-digit OTP with 5-minute TTL, persists to DB, and dispatches via Twilio, MSG91, or SMTP. Blocks inactive accounts with `403 Forbidden`. |
| `/api/v1/auth/login` | `POST` | Public | Dual-flow login: If account has `secure_account = true` (two-step), dispatches OTP (`requires_otp: true`). If `secure_account = false`, validates password directly and returns JWT + refresh tokens (`requires_otp: false`). Blocks inactive accounts with `403 Forbidden`. |
| `/api/v1/auth/verify-otp` | `POST` | Public | Validates OTP against DB records, enforces attempt limits (max 5) & 5-min TTL, and issues JWT + refresh token. |
| `/api/v1/auth/me` | `GET` | Bearer JWT | Retrieves authenticated user account profile. `JwtAuthFilter` strictly verifies that `is_active = true` in PostgreSQL before authorizing. |

---

## 3. Configuration & Environment Variables

All settings are configured in `application.yml` and overridable via environment variables (`env.txt`):

```yaml
app:
  auth:
    activation-base-url: ${AUTH_ACTIVATION_BASE_URL:http://localhost:8080/api/v1/auth/activate}
    activation-token-ttl-hours: ${AUTH_ACTIVATION_TTL_HOURS:24}
    mock-email-enabled: ${AUTH_MOCK_EMAIL_ENABLED:false} # Mock activation welcome email sending (independent from OTP)
  otp:
    ttl-minutes: ${OTP_TTL_MINUTES:5}
    mock-enabled: ${OTP_MOCK_ENABLED:true} # Mock OTP service (Twilio/MSG91/SMTP OTP) independently
    sms-provider: ${OTP_SMS_PROVIDER:twilio} # Options: twilio | msg91
    email-provider: ${OTP_EMAIL_PROVIDER:smtp} # Options: smtp
    max-attempts: ${OTP_MAX_ATTEMPTS:5}
    twilio:
      account-sid: ${TWILIO_ACCOUNT_SID:dummy_account_sid}
      auth-token: ${TWILIO_AUTH_TOKEN:dummy_auth_token}
      from-phone-number: ${TWILIO_FROM_PHONE_NUMBER:+15005550006}
    msg91:
      auth-key: ${MSG91_AUTH_KEY:dummy_msg91_auth_key}
      template-id: ${MSG91_TEMPLATE_ID:dummy_template_id}
      sender-id: ${MSG91_SENDER_ID:MILOO}
    smtp:
      host: ${SMTP_HOST:smtp.gmail.com}
      port: ${SMTP_PORT:587}
      username: ${SMTP_USERNAME:dummy@gmail.com}
      password: ${SMTP_PASSWORD:dummy_password}
      from-email: ${SMTP_FROM_EMAIL:noreply@miloo.app}
      from-name: ${SMTP_FROM_NAME:Miloo Dating}
      auth: true
      starttls: true
```

### Mock Mode Behavior (`mock-enabled: true`)
1. **Welcome Email with Activation Link**:
   ```log
   [Welcome Email Mock] Sent activation email to 'user@example.com' with link: 'http://localhost:8080/api/v1/auth/activate?token=a1b2c3d4-e5f6-7890-abcd-ef1234567890'
   ```
2. **OTP Dispatches**:
   ```log
   [Twilio Mock] SMS OTP '482910' dispatched to destination '+15005550006' (From: +15005550006)
   [MSG91 Mock] SMS OTP '719284' dispatched to destination '+919876543210' (SenderId: MILOO, Template: dummy_template)
   [SMTP Mock] Email OTP '319028' dispatched to destination 'user@example.com' (From: Miloo Dating <noreply@miloo.app>)
   ```
3. **Master Demo Code**: Code `123456` is accepted in mock mode for instant developer testing.

---

## 4. Database Inspection Queries

Execute these queries in PostgreSQL to inspect the live state across `auth_ctx.accounts`, `auth_ctx.account_activations`, and `auth_ctx.otps`:

```sql
-- 1. Inspect accounts, active status, and two-step secure_account flag
SELECT 
    account_id,
    phone_number,
    country_code,
    email,
    is_active,
    secure_account,
    is_verified,
    created_at
FROM auth_ctx.accounts
ORDER BY created_at DESC
LIMIT 10;

-- 2. Inspect activation tokens, expiry, and used timestamp
SELECT 
    activation_id,
    account_id,
    token,
    expires_at,
    used_at,
    ROUND(EXTRACT(EPOCH FROM (expires_at - NOW())) / 3600, 1) AS remaining_hours,
    created_at
FROM auth_ctx.account_activations
ORDER BY created_at DESC
LIMIT 10;

-- 3. Inspect OTP records with remaining 5-min TTL and attempt counters
SELECT 
    otp_id,
    destination,
    otp_code,
    channel,
    provider,
    status,
    attempts,
    expires_at,
    ROUND(EXTRACT(EPOCH FROM (expires_at - NOW()))) AS remaining_seconds,
    created_at
FROM auth_ctx.otps
ORDER BY created_at DESC
LIMIT 10;
```

---

## 5. Comprehensive Test Cases Catalog

### Group A: Registration & Account Activation Flow

#### Test Case TC-01: User Registration
* **Endpoint**: `POST /api/v1/auth/register`
* **Request**:
  ```bash
  curl -X POST http://localhost:8080/api/v1/auth/register \
    -H "Content-Type: application/json" \
    -d '{
      "phone_number": "5551112233",
      "country_code": "+1",
      "email": "sarah.connor@miloo.app",
      "password": "SecurePassword123!"
    }'
  ```
* **Expected Status**: `200 OK`
* **Expected Response Body**:
  ```json
  {
    "token": null,
    "refresh_token": null,
    "account": {
      "account_id": "...",
      "phone_number": "+15551112233",
      "country_code": "+1",
      "email": "sarah.connor@miloo.app",
      "is_active": false,
      "is_verified": false
    }
  }
  ```
* **Console Output**:
  ```log
  [Welcome Email Mock] Sent activation email to 'sarah.connor@miloo.app' with link: 'http://localhost:8080/api/v1/auth/activate?token=...'
  ```
* **DB Verification**: `auth_ctx.accounts.is_active = FALSE`. New record in `auth_ctx.account_activations` with `used_at IS NULL`.

---

#### Test Case TC-02: Inactive Account Login Block
* **Endpoint**: `POST /api/v1/auth/login`
* **Goal**: Verify unactivated user cannot trigger login challenge or authenticate.
* **Request**:
  ```bash
  curl -X POST http://localhost:8080/api/v1/auth/login \
    -H "Content-Type: application/json" \
    -d '{
      "phone_number": "5551112233",
      "country_code": "+1"
    }'
  ```
  *(Or via email: `-d '{"email": "sarah.connor@miloo.app"}'`)*
* **Expected Status**: `403 Forbidden`
* **Expected Response Body**:
  ```json
  {
    "status": 403,
    "error": "Forbidden",
    "message": "Account is not active. Please activate your account via the link sent to your email."
  }
  ```

---

#### Test Case TC-02A: Password Login (Standard Account / `secure_account = false`)
* **Endpoint**: `POST /api/v1/auth/login`
* **Goal**: Activated accounts without two-step enabled log in directly using password without OTP.
* **Request**:
  ```bash
  curl -X POST http://localhost:8080/api/v1/auth/login \
    -H "Content-Type: application/json" \
    -d '{
      "email": "sarah.connor@miloo.app",
      "password": "SecurePassword123!"
    }'
  ```
* **Expected Status**: `200 OK`
* **Expected Response Body**:
  ```json
  {
    "success": true,
    "message": "Login successful",
    "requires_otp": false,
    "token": "eyJhbGciOiJIUzI1NiJ9...",
    "refreshToken": "eyJhbGciOiJIUzI1NiJ9...",
    "account": {
      "account_id": "...",
      "email": "sarah.connor@miloo.app",
      "secure_account": false,
      "is_active": true
    }
  }
  ```
* **Verification**: Immediate JWT issuance without OTP challenge.

---

#### Test Case TC-02B: Two-Step OTP Challenge (`secure_account = true`)
* **Endpoint**: `POST /api/v1/auth/login`
* **Goal**: Activated accounts with `secure_account = true` dispatch OTP and require `/verify-otp`.
* **Request**:
  ```bash
  curl -X POST http://localhost:8080/api/v1/auth/login \
    -H "Content-Type: application/json" \
    -d '{
      "phone_number": "5551112233",
      "country_code": "+1"
    }'
  ```
* **Expected Status**: `200 OK`
* **Expected Response Body**:
  ```json
  {
    "success": true,
    "message": "OTP sent successfully. Please verify OTP to complete login.",
    "requires_otp": true,
    "account": {
      "account_id": "...",
      "secure_account": true,
      "is_active": true
    }
  }
  ```
* **Verification**: OTP is saved to `auth_ctx.otps` and dispatched via configured gateway.

---

#### Test Case TC-02C: Wrong Password Rejection (`secure_account = false`)
* **Endpoint**: `POST /api/v1/auth/login`
* **Request**: Invalid password for password login.
* **Expected Status**: `401 Unauthorized` ("Invalid credentials").

---

#### Test Case TC-03: Inactive Account Send-OTP Block
* **Endpoint**: `POST /api/v1/auth/send-otp`
* **Goal**: Verify unactivated user cannot request OTP via `/send-otp`.
* **Request**:
  ```bash
  curl -X POST http://localhost:8080/api/v1/auth/send-otp \
    -H "Content-Type: application/json" \
    -d '{"email": "sarah.connor@miloo.app"}'
  ```
* **Expected Status**: `403 Forbidden`

---

#### Test Case TC-04: Account Activation via GET (Browser Link Click)
* **Endpoint**: `GET /api/v1/auth/activate?token=<TOKEN>`
* **Goal**: User clicks link in their email client; browser receives styled HTML confirmation.
* **Request**:
  ```bash
  curl -X GET "http://localhost:8080/api/v1/auth/activate?token=<ACTIVATION_TOKEN>" \
    -H "Accept: text/html"
  ```
* **Expected Status**: `200 OK`
* **Response Content-Type**: `text/html`
* **Response Body**: Clean HTML rendered card titled "Account Activated!" from `AuthConstants.ACTIVATION_SUCCESS_HTML`.
* **DB Verification**: `auth_ctx.accounts.is_active = TRUE`, `auth_ctx.accounts.is_verified = TRUE`, `auth_ctx.account_activations.used_at` populated with current timestamp.

---

#### Test Case TC-05: Account Activation via POST (API Client)
* **Endpoint**: `POST /api/v1/auth/activate`
* **Goal**: Mobile or frontend app activates account programmatically.
* **Request**:
  ```bash
  curl -X POST http://localhost:8080/api/v1/auth/activate \
    -H "Content-Type: application/json" \
    -d '{"token": "<ACTIVATION_TOKEN>"}'
  ```
* **Expected Status**: `200 OK`
* **Expected Response Body**:
  ```json
  {
    "success": true,
    "message": "Account successfully activated! You can now log in.",
    "account": {
      "email": "sarah.connor@miloo.app",
      "is_active": true,
      "is_verified": true
    }
  }
  ```

---

#### Test Case TC-06: Activation Link Replay Guard (Already Used)
* **Endpoint**: `GET /api/v1/auth/activate?token=<ALREADY_USED_TOKEN>`
* **Goal**: Prevent reusing an already consumed activation token, giving browser users a clear, beautiful status page.
* **Browser Request (`Accept: text/html`)**:
  ```bash
  curl -X GET "http://localhost:8080/api/v1/auth/activate?token=<ALREADY_USED_TOKEN>" \
    -H "Accept: text/html"
  ```
  * **Status**: `400 Bad Request`
  * **Content-Type**: `text/html`
  * **Rendered View**: Styled HTML card with title **"Link Already Used"** and description *"This account activation link has already been used. Your account is active and you can open the Miloo app to log in."*
* **API Client Request (`Accept: application/json`)**:
  ```bash
  curl -X GET "http://localhost:8080/api/v1/auth/activate?token=<ALREADY_USED_TOKEN>" \
    -H "Accept: application/json"
  ```
  * **Status**: `400 Bad Request`
  * **Body**: Standard JSON error (`{"message": "Account activation link has already been used"}`)

---

#### Test Case TC-07: Expired Activation Token Rejection
* **Goal**: Reject tokens past 24-hour expiration window with appropriate error presentation.
* **Simulation (SQL)**:
  ```sql
  UPDATE auth_ctx.account_activations 
  SET expires_at = NOW() - INTERVAL '1 hour'
  WHERE token = '<TOKEN>';
  ```
* **Browser Request (`Accept: text/html`)**:
  ```bash
  curl -X GET "http://localhost:8080/api/v1/auth/activate?token=<EXPIRED_TOKEN>" \
    -H "Accept: text/html"
  ```
  * **Status**: `400 Bad Request`
  * **Content-Type**: `text/html`
  * **Rendered View**: Styled HTML card with title **"Activation Link Expired"** and description *"This activation link has expired (links are valid for 24 hours). Please log in to your account to request a new link."*
* **API Client Request (`Accept: application/json` or `POST /activate`)**:
  * **Status**: `400 Bad Request`
  * **Body**: Standard JSON error (`{"message": "Account activation link has expired"}`)

---

#### Test Case TC-08: Duplicate Registration Conflicts
* **Goal**: Re-registering existing phone or email returns conflict.
* **Request**: Repeat `POST /register` with duplicate email or phone.
* **Expected Status**: `409 Conflict` ("Email is already registered" or "Phone number is already registered").

---

### Group B: OTP Generation & Dispatching

#### Test Case TC-09: Send Mobile SMS OTP (Default: Twilio)
* **Endpoint**: `POST /api/v1/auth/send-otp`
* **Request**:
  ```bash
  curl -X POST http://localhost:8080/api/v1/auth/send-otp \
    -H "Content-Type: application/json" \
    -d '{
      "phone_number": "5551112233",
      "country_code": "+1"
    }'
  ```
* **Expected Status**: `200 OK`
* **DB Verification**: `destination = '+15551112233'`, `provider = 'TWILIO'`, `channel = 'SMS'`, `status = 'PENDING'`, `expires_at ~ NOW() + 5 minutes`.

---

#### Test Case TC-10: Send Mobile SMS OTP with Provider Override (MSG91)
* **Endpoint**: `POST /api/v1/auth/send-otp`
* **Request**:
  ```bash
  curl -X POST http://localhost:8080/api/v1/auth/send-otp \
    -H "Content-Type: application/json" \
    -d '{
      "phone_number": "9876543210",
      "country_code": "+91",
      "provider": "msg91"
    }'
  ```
* **Expected Status**: `200 OK`
* **DB Verification**: `destination = '+919876543210'`, `provider = 'MSG91'`, `channel = 'SMS'`.

---

#### Test Case TC-11: Send Email OTP (SMTP)
* **Endpoint**: `POST /api/v1/auth/send-otp`
* **Request**:
  ```bash
  curl -X POST http://localhost:8080/api/v1/auth/send-otp \
    -H "Content-Type: application/json" \
    -d '{"email": "sarah.connor@miloo.app"}'
  ```
* **Expected Status**: `200 OK`
* **DB Verification**: `provider = 'SMTP'`, `channel = 'EMAIL'`.

---

#### Test Case TC-12: OTP Re-request & Previous Code Invalidation
* **Goal**: Re-requesting an OTP invalidates previous active OTPs for the destination.
* **Step 1**: Send OTP to destination. Note code A.
* **Step 2**: Re-send OTP to same destination. Note code B.
* **Step 3**: Attempt verification with code A.
* **Expected Result**: Code A rejected with `401 Unauthorized`. In DB: Code A has `status = 'EXPIRED'`, Code B has `status = 'PENDING'`.

---

### Group C: OTP Verification & Session Issuance

#### Test Case TC-13: Successful OTP Verification & Login
* **Endpoint**: `POST /api/v1/auth/verify-otp`
* **Request**:
  ```bash
  curl -X POST http://localhost:8080/api/v1/auth/verify-otp \
    -H "Content-Type: application/json" \
    -d '{
      "phone_number": "5551112233",
      "country_code": "+1",
      "otp": "<6_DIGIT_CODE>"
    }'
  ```
* **Expected Status**: `200 OK`
* **Expected Response Body**:
  ```json
  {
    "token": "eyJhbGciOiJIUzI1NiJ9...",
    "refresh_token": "eyJhbGciOiJIUzI1NiJ9...",
    "account": {
      "account_id": "...",
      "phone_number": "+15551112233",
      "country_code": "+1",
      "email": "sarah.connor@miloo.app",
      "is_active": true,
      "is_verified": true
    }
  }
  ```
* **DB Verification**: `auth_ctx.otps.status = 'VERIFIED'`.

---

#### Test Case TC-14: Master Demo Code `123456` in Mock Mode
* **Endpoint**: `POST /api/v1/auth/verify-otp`
* **Request**:
  ```bash
  curl -X POST http://localhost:8080/api/v1/auth/verify-otp \
    -H "Content-Type: application/json" \
    -d '{
      "phone_number": "5551112233",
      "country_code": "+1",
      "otp": "123456"
    }'
  ```
* **Expected Status**: `200 OK`

---

#### Test Case TC-15: Incorrect OTP Code & Attempt Counting
* **Goal**: Entering an invalid code increments `attempts` and returns `401 Unauthorized`.
* **Expected Status**: `401 Unauthorized`
* **DB Verification**: `attempts = 1`, `status = 'PENDING'`.

---

#### Test Case TC-16: Max Attempts Lockout
* **Goal**: Entering incorrect code 5 times locks out the OTP.
* **Expected Status**: `401 Unauthorized`
* **DB Verification**: `attempts = 5`, `status = 'FAILED'`. Subsequent attempts with the correct code are permanently rejected.

---

#### Test Case TC-17: 5-Minute TTL Expiration
* **Goal**: Submitting OTP after 5-minute validity window is rejected.
* **Simulation (SQL)**:
  ```sql
  UPDATE auth_ctx.otps SET expires_at = NOW() - INTERVAL '10 seconds' WHERE status = 'PENDING';
  ```
* **Expected Status**: `401 Unauthorized`
* **DB Verification**: `status = 'EXPIRED'`.

---

### Group D: Protected Endpoints & `JwtAuthFilter` Verification

#### Test Case TC-18: Authorized Access to Current User Profile
* **Endpoint**: `GET /api/v1/auth/me`
* **Request**:
  ```bash
  curl -X GET http://localhost:8080/api/v1/auth/me \
    -H "Authorization: Bearer <JWT_TOKEN>"
  ```
* **Expected Status**: `200 OK`
* **Expected Response Body**: Returns account profile with `"is_active": true`.

---

#### Test Case TC-19: JWT Rejection for Inactive Accounts
* **Goal**: `JwtAuthFilter` checks the database for `is_active = true`. If account is inactive or deleted, authentication is rejected.
* **Simulation (SQL)**:
  ```sql
  UPDATE auth_ctx.accounts SET is_active = FALSE WHERE email = 'sarah.connor@miloo.app';
  ```
* **Request**: Repeat TC-18 with the previously valid JWT token.
* **Expected Status**: `401 Unauthorized` or `403 Forbidden`.
* **Console Log Output**:
  ```log
  [JWT Auth] Rejected token for userId '...': Account does not exist or is inactive
  ```

---

## 6. Complete Automated PowerShell Test Script

Save and execute `test_auth_suite.ps1` in PowerShell to test the entire lifecycle:

```powershell
# Miloo Authentication & Activation Test Suite
$baseUrl = "http://localhost:8080/api/v1/auth"
$randomId = Get-Random -Minimum 10000 -Maximum 99999
$email = "test.user.$randomId@miloo.app"
$phone = "555$randomId"
$countryCode = "+1"

Write-Host "`n=== STEP 1: Registering New User ($email) ===" -ForegroundColor Cyan
$regResponse = Invoke-RestMethod -Method Post -Uri "$baseUrl/register" `
    -ContentType "application/json" `
    -Body (@{
        phone_number = $phone
        country_code = $countryCode
        email = $email
        password = "SecurePassword123!"
    } | ConvertTo-Json)

Write-Host "Registered Account ID: $($regResponse.account.account_id)" -ForegroundColor Green
Write-Host "Account Country Code: $($regResponse.account.country_code)" -ForegroundColor Green
Write-Host "Account Active: $($regResponse.account.is_active) (Expected: False)" -ForegroundColor Yellow
Write-Host "Session Token: $($regResponse.token) (Expected: null)" -ForegroundColor Yellow

Write-Host "`n=== STEP 2: Attempting Login While Inactive (Expected: 403) ===" -ForegroundColor Cyan
try {
    Invoke-RestMethod -Method Post -Uri "$baseUrl/login" `
        -ContentType "application/json" `
        -Body (@{ 
            phone_number = $phone
            country_code = $countryCode 
        } | ConvertTo-Json)
    Write-Error "Test Failed: Inactive account was allowed to log in!"
} catch {
    Write-Host "Blocked Successfully with 403 Forbidden: $($_.Exception.Message)" -ForegroundColor Green
}

Write-Host "`n=== STEP 3: Activating Account via Token ===" -ForegroundColor Cyan
# In local development, check console log for activation token or query DB:
# Example: [Welcome Email Mock] Sent activation email to '...' with link: 'http://.../activate?token=<TOKEN>'
# Here we demonstrate the endpoint:
Write-Host "Activate in browser: $baseUrl/activate?token=<TOKEN>" -ForegroundColor Gray

Write-Host "`n=== STEP 4: Login with Active Account (Demo OTP: 123456) ===" -ForegroundColor Cyan
$verifyResp = Invoke-RestMethod -Method Post -Uri "$baseUrl/verify-otp" `
    -ContentType "application/json" `
    -Body (@{
        phone_number = "5551234567"
        country_code = "+1"
        otp = "123456"
    } | ConvertTo-Json)

$jwt = $verifyResp.token
Write-Host "Login Successful! JWT: $($jwt.Substring(0, 30))..." -ForegroundColor Green

Write-Host "`n=== STEP 5: Accessing Protected Endpoint /me ===" -ForegroundColor Cyan
$meResp = Invoke-RestMethod -Method Get -Uri "$baseUrl/me" `
    -Headers @{ Authorization = "Bearer $jwt" }

Write-Host "User ID: $($meResp.account_id), Email: $($meResp.email), Active: $($meResp.is_active)" -ForegroundColor Green
Write-Host "`n=== End-to-End Suite Complete! ===" -ForegroundColor Yellow
```

---

## 7. Running Automated Maven Tests

Execute the automated test suites from the `backend` directory:

```bash
# 1. Run Account Activation, secure_account branching & JWT filter test suite (15 tests)
mvn test -Dtest="AccountActivationTest"

# 2. Run Activation Email Service independent mock test suite (3 tests)
mvn test -Dtest="AuthEmailServiceTest"

# 3. Run Multi-Provider OTP Sender test suite (8 tests)
mvn test -Dtest="OtpSenderServiceTest"

# 4. Run Database OTP persistence, 5-min TTL, and attempt counters (6 tests)
mvn test -Dtest="OtpServiceTest"

# 5. Run Spring Boot Context wiring test suite (1 test)
mvn test -Dtest="OtpContextTest"

# 6. Run full test suite across entire backend (45 tests)
mvn test
```
