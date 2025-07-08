# Smart Appointment Booking System

A full-stack appointment booking platform with user, provider, and admin roles. Features include slot management, appointment booking, queueing, notifications, and analytics. Built with Java Spring Boot (backend), Next.js (frontend), PostgreSQL, Redis, Kafka, and Docker Compose for orchestration.

## Features
- User registration, OTP verification, and authentication
- Provider registration with specialization
- Slot creation, update, and deletion by providers
- Appointment booking and cancellation by users
- Real-time queue management for slots
- Admin analytics endpoints
- Kafka-based notifications
- RESTful API (see Postman collection)

## Tech Stack
- **Backend:** Java Spring Boot
- **Frontend:** Next.js (React, TypeScript, Tailwind CSS)
- **Database:** PostgreSQL
- **Cache/Queue:** Redis
- **Messaging:** Kafka, Zookeeper
- **Containerization:** Docker, Docker Compose

## Getting Started

### Prerequisites
- Docker & Docker Compose
- Node.js (for local frontend dev)
- Java 17+ (for local backend dev)

### Running with Docker Compose
1. Clone the repository:
   ```sh
   git clone https://github.com/ashank14/Appointment-Booking-System.git
   cd Appointment-Booking-System
   ```
2. Start all services:
   ```sh
   docker-compose up --build
   ```
3. Backend: http://localhost:8080  
   Frontend: http://localhost:3000

### Environment Variables
- Backend DB connection is configured in `docker-compose.yml` for containers.
- For local dev, set:
  - `SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/smartappointments`
  - `SPRING_DATASOURCE_USERNAME=smartuser`
  - `SPRING_DATASOURCE_PASSWORD=smartpass`

### API Documentation
- Import `smart-appointment-backend.postman_collection.json` into Postman for ready-to-use API requests.
- **Main Endpoints:**

  **User**
  - `POST   /user/register` — Register a new user or provider
  - `POST   /user/verify` — Verify OTP for registration
  - `POST   /user/signin` — User or provider login
  - `GET    /user/get-users` — List all users (admin)
  - `GET    /user/{id}` — Get user by ID

  **Slots (Provider)**
  - `POST   /slots/addSlot` — Create a slot
  - `GET    /slots/getMySlots` — Get provider's slots
  - `PUT    /slots/updateSlot/{slotId}` — Update a slot
  - `DELETE /slots/{id}` — Delete a slot
  - `GET    /slots/{id}` — Get slot by ID
  - `GET    /slots/getAllSlots` — Get all slots

  **Appointments (User)**
  - `POST   /appointments/createAppointment/{slotId}` — Book an appointment
  - `GET    /appointments/getUserAppointments` — Get user's appointments
  - `DELETE /appointments/cancel/{id}` — Cancel appointment
  - `GET    /appointments/getById/{id}` — Get appointment by ID

  **Queue**
  - `POST   /queue/join/{slotId}` — Join queue for a slot
  - `POST   /queue/leave/{slotId}` — Leave queue
  - `GET    /queue/{slotId}` — Get queue size for a slot

  **Admin**
  - `GET    /admin/appointments/count/{providerId}`
  - `GET    /admin/appointments/getAppointmentList/{providerId}`
  - `GET    /admin/cancellations/count/{providerId}`
  - `GET    /admin/cancellations/rate/{providerId}`
  - `GET    /admin/appointments/peak-hours`

### Running Tests
- Backend: `./mvnw test` (integration and unit tests)
- Frontend: `pnpm test` (if tests are present)

### Project Structure
- `backend/` - Spring Boot backend
- `frontend/` - Next.js frontend
- `docker-compose.yml` - Multi-service orchestration
- `smart-appointment-backend.postman_collection.json` - Postman API collection

