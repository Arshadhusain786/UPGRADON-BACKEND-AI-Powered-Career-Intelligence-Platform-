# 🚀 UpgradeOn  
### Continuous Career Growth Powered by AI & Intelligent Referrals

> UpgradeOn is a production-grade backend system designed to help users evolve their careers through AI-driven insights and a structured referral ecosystem.

---

# 📖 Overview

**UpgradeOn** is an AI-powered career intelligence platform that enables users to:

- Generate personalized career roadmaps  
- Identify skill gaps based on target roles  
- Analyze and score resumes  
- Connect with professionals for job referrals  

The platform bridges the gap between **career planning and real-world opportunities** using a scalable backend architecture and intelligent automation.

---

# ✨ Features

## 🤖 AI-Powered Career Intelligence
- Career Roadmap Generation
- Skill Gap Analysis
- Resume Scoring System
- Structured AI responses with validation

## 💰 Referral System (Core Engine)
- Send referral requests to professionals
- Credit-based request model
- Status tracking lifecycle

## 🔐 Authentication & Security
- JWT-based authentication (access + refresh tokens)
- Role-based authorization
- Stateless security architecture
- Secure logout & token rotation

## 🧠 Scalable System Design
- Clean architecture (controller → service → repository)
- DTO & Mapper abstraction
- Centralized exception handling
- Standard API response format

---

# 🏗️ Architecture

UpgradeOn follows a **clean, layered architecture**:


Controller → Service → Repository


### Core Layers:

- **Controller Layer** → Handles incoming API requests  
- **Service Layer** → Business logic & validations  
- **Repository Layer** → Database operations (JPA/Hibernate)  

### Supporting Components:

- DTO Layer (Data Transfer Objects)  
- Mapper Layer (Entity ↔ DTO conversion)  
- GlobalExceptionHandler  
- Standardized API responses  

---

# ⚙️ Tech Stack

| Category        | Technology                          |
|----------------|-------------------------------------|
| Backend        | Java 17, Spring Boot 3.2+          |
| Security       | Spring Security (JWT)              |
| Database       | PostgreSQL, JPA/Hibernate          |
| Build Tool     | Maven                              |
| AI Integration | OpenRouter API (GPT-based)         |

### Planned Integrations:
- Redis (Caching & Rate Limiting)
- Kafka (Asynchronous Processing)
- Cloudinary (File Storage)
- Razorpay (Payments)

---

# 🔐 Authentication Flow

1. User registers or logs in  
2. Server generates:
   - Access Token (short-lived)
   - Refresh Token (long-lived)  
3. Access token is used for API requests  
4. Refresh token is used to renew session  
5. Logout invalidates tokens  

### User Roles:

- `STUDENT`  
- `MENTOR`  
- `COMPANY`  
- `ADMIN`  

---

# 🤖 AI System Overview

### Endpoints

```http
POST /api/ai/roadmap
POST /api/ai/skill-gap
POST /api/ai/resume-score
Capabilities:
Structured JSON outputs
Prompt-engineered responses
Safe parsing & validation
External API fallback handling
💰 Referral System
Workflow
User sends a referral request
1 credit is deducted
Recipient reviews and responds
Status Lifecycle:
PENDING → ACCEPTED → REJECTED → EXPIRED
Validation Rules:
Cannot request yourself
Duplicate requests restricted
Daily limits enforced
User must have sufficient credits
⚖️ Legal Disclaimer

⚠️ Important Notice

UpgradeOn does NOT guarantee jobs or interviews
Referrals are voluntary actions by users
Credits are used for platform access only
This is NOT a pay-for-job system
UpgradeOn acts as a networking facilitator, not a recruitment agency
📂 Project Structure
src/
 ├── controller/
 ├── service/
 ├── repository/
 ├── dto/
 ├── mapper/
 ├── entity/
 ├── security/
 ├── config/
 └── exception/
⚡ Getting Started
Prerequisites
Java 17+
PostgreSQL
Maven
Installation
git clone https://github.com/your-username/upgradeon-backend.git
cd upgradeon-backend
Build & Run
mvn clean install
mvn spring-boot:run
🔑 Environment Variables

Configure in application.yml:

spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/upgradeon
    username: your_db_user
    password: your_db_password

jwt:
  secret: your_jwt_secret

openrouter:
  api-key: your_openrouter_api_key
🧪 Sample API Endpoints
Auth
POST /api/auth/register
POST /api/auth/login
POST /api/auth/refresh
AI
POST /api/ai/roadmap
POST /api/ai/skill-gap
POST /api/ai/resume-score
Referral (Upcoming)
POST /api/referrals/request
GET /api/referrals
PATCH /api/referrals/{id}/status
🚀 Future Roadmap
📄 Resume File Upload (PDF/DOC parsing)
💳 Razorpay Payment Integration
🎯 Mentor Booking System
⚡ Redis-based caching & rate limiting
📡 Kafka-based async workflows
🤝 Contributing

Contributions are welcome.

Steps:
Fork the repository
Create a feature branch
Commit your changes
Submit a pull request
📜 License

This project is licensed under the MIT License.

👨‍💻 Author

Arshad Husain
Backend Developer | System Designer

⭐ Final Note

UpgradeOn is built with a focus on:

Scalability
Real-world applicability
Ethical and legal compliance

This is not just a project — it is a production-ready backend foundation for a modern career-tech startup 🚀
