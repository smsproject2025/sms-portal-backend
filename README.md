# SMS Portal Backend — Spring Boot

A full-featured telemarketer SMS portal backend similar to Fast2SMS, built with **Spring Boot 3**, **MySQL**, **Redis**, and **RabbitMQ**.

---

## 🏗️ Project Structure

```
src/main/java/com/smsportal/
├── config/                     # Spring configs (Security, JWT, RabbitMQ, Redis)
├── controller/                 # REST API endpoints
├── service/                    # Business logic
├── model/                      # JPA entities
├── repository/                 # Spring Data JPA repositories
├── dto/                        # Request/Response data classes
└── security/                   # JWT filter & token provider
```

---

## ⚙️ Prerequisites

- Java 17+
- Maven 3.8+
- MySQL 8+
- Redis 7+
- RabbitMQ 3.x

---

## 🚀 Quick Start

### Option 1 — Docker Compose (Recommended)

```bash
docker-compose up -d
```

This starts MySQL, Redis, RabbitMQ, and the Spring Boot app.

### Option 2 — Manual Setup

1. **Configure `application.yml`** with your DB credentials, Redis, RabbitMQ, and API keys.

2. **Start infrastructure:**
   ```bash
   # MySQL
   mysql -u root -p -e "CREATE DATABASE IF NOT EXISTS smsportal;"

   # Redis (macOS)
   brew services start redis

   # RabbitMQ (macOS)
   brew services start rabbitmq
   ```

3. **Build & Run:**
   ```bash
   mvn clean install -DskipTests
   mvn spring-boot:run
   ```

---

## 🔑 Configuration

Edit `src/main/resources/application.yml`:

| Key | Description |
|-----|-------------|
| `spring.datasource.*` | MySQL connection |
| `jwt.secret` | 256-bit Base64 key |
| `msg91.auth-key` | MSG91 API key |
| `razorpay.key-id` | Razorpay key ID |
| `razorpay.key-secret` | Razorpay key secret |
| `app.gateway` | `msg91` or `textlocal` |
| `app.sms-rate-*` | Per-SMS pricing |

---

## 📡 API Endpoints

### Auth
| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/auth/register` | Register new user |
| POST | `/api/auth/login` | Login, returns JWT |
| POST | `/api/auth/refresh` | Refresh JWT token |
| POST | `/api/auth/forgot-password` | Forgot password |
| POST | `/api/auth/reset-password` | Reset with token |

### SMS
| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/sms/send` | Send SMS (single/bulk) |
| POST | `/api/sms/send-bulk-csv` | Upload CSV & send bulk |
| GET | `/api/sms/reports` | Delivery reports |
| GET | `/api/sms/reports/status/{status}` | Filter by status |
| POST | `/api/sms/webhook/delivery` | Gateway webhook |

### Wallet
| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/wallet` | Get balance |
| POST | `/api/wallet/recharge/create-order` | Create Razorpay order |
| POST | `/api/wallet/recharge/verify` | Verify & credit wallet |
| GET | `/api/wallet/transactions` | Transaction history |

### Reports
| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/reports/dashboard` | Dashboard stats + charts |

### Sender IDs
| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/sender-ids` | List my sender IDs |
| POST | `/api/sender-ids/request` | Request new sender ID |

### Admin (ROLE_ADMIN only)
| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/admin/stats` | Platform stats |
| GET | `/api/admin/users` | All users |
| PUT | `/api/admin/users/{id}/toggle-active` | Activate/deactivate |
| GET | `/api/admin/sender-ids/pending` | Pending sender IDs |
| PUT | `/api/admin/sender-ids/{id}/approve` | Approve sender ID |
| PUT | `/api/admin/sender-ids/{id}/reject` | Reject sender ID |
| GET | `/api/admin/sms-logs` | All SMS logs |

---

## 📦 Key Request/Response Examples

### Register
```json
POST /api/auth/register
{
  "name": "Rahul Sharma",
  "email": "rahul@example.com",
  "mobile": "9876543210",
  "password": "Pass@1234"
}
```

### Send SMS
```json
POST /api/sms/send
Authorization: Bearer <jwt>
{
  "mobiles": ["9876543210", "9123456789"],
  "message": "Your OTP is 1234",
  "senderId": "SMSPTL",
  "type": "TRANSACTIONAL"
}
```

### Recharge Wallet
```json
POST /api/wallet/recharge/create-order
{
  "amount": 500
}

POST /api/wallet/recharge/verify
{
  "amount": 500,
  "razorpayOrderId": "order_xxx",
  "razorpayPaymentId": "pay_xxx",
  "razorpaySignature": "sig_xxx"
}
```

---

## 🔄 SMS Flow

```
API Request → SmsController → SmsService
    → Check Balance (WalletService)
    → Create SmsLog (QUEUED)
    → Push to RabbitMQ
    → RabbitMQ Consumer picks up
    → SmsGatewayService sends via MSG91
    → Update SmsLog (SENT)
    → Delivery webhook updates (DELIVERED/FAILED)
```

---

## 🏛️ Next Steps for Production

1. Add **email verification** on registration
2. Add **rate limiting** (Spring Boot + Redis)
3. Add **API key authentication** (for programmatic access)
4. Configure **real SMS gateway** credentials (MSG91/Exotel)
5. Set up **Razorpay production** keys
6. Enable **HTTPS** with SSL certificate
7. Set up **monitoring** (Actuator + Prometheus + Grafana)
8. Configure **Angular frontend** to point to this backend

---

## 🛠️ Tech Stack

| Layer | Technology |
|-------|-----------|
| Framework | Spring Boot 3.2 |
| Database | MySQL 8 + Spring Data JPA |
| Cache | Redis |
| Queue | RabbitMQ |
| Auth | JWT (jjwt 0.11) |
| Payments | Razorpay Java SDK |
| SMS Gateway | MSG91 / Twilio |
| Build | Maven |
| Containers | Docker + Docker Compose |
