# Spring Boot Event-Driven Auth Platform & React Frontend

A production-grade, event-driven microservices architecture built with Spring Boot, Apache Kafka, Kubernetes, and React. This project demonstrates modern distributed system patterns including event-driven communication, distributed tracing, circuit breakers, and decoupled frontend/backend infrastructure.

---

## 🏛 Architecture Overview

### 1. Backend Microservices (Spring Boot)
* **API Gateway (`port 8080`):** The central entry point. Handles routing, centralized CORS configured for React, and Resilience4j circuit breakers with fallback handling.
* **Auth Service (`port 8081`):** Core domain logic. Handles user registration, generates 6-digit OTP codes, stores them in Postgres, and acts as a **Kafka Producer** to broadcast registration events.
* **Notification Service (`port 8082`):** Decoupled consumer. Listens for `user-registration-events` via a **Kafka Consumer**, and dispatches the OTP asynchronously using Gmail SMTP.

### 2. Infrastructure & Messaging
* **Apache Kafka & Zookeeper:** Message broker facilitating the asynchronous, decoupled flow between Auth and Notification services.
* **PostgreSQL:** Persistent storage for user accounts and OTP tracking.
* **Zipkin:** Distributed tracing server to trace requests end-to-end across services.
* **Prometheus & Grafana:** Containerized metrics collection and visualizations.

### 3. Frontend Application (React)
* **React + Vite / React Router:** Modern frontend running locally via Hot-Module Replacement (HMR).
* **Axios & Context API:** Centralized Axios instance with interceptors configured to handle API interactions with the backend Gateway seamlessly.

---

## 🔄 Registration & Auth Workflow

1. **User Submits Form (React):** User enters credentials in the frontend and hits `/auth/register`.
2. **Gateway Routing:** API Gateway routes request to Auth Service.
3. **Database & Event (Auth):** Auth Service persists the user as *Unverified*, generates an OTP, and drops a `UserRegisteredEvent` onto the Kafka topic. Auth service returns `201 Created` immediately.
4. **Asynchronous Email (Notification):** Notification service picks up the event and dispatches an HTML email containing the OTP via Gmail SMTP.
5. **Verification (React):** User types OTP into the React Verify screen -> hits `/auth/verify` -> Account enabled!
6. **Login:** User hits `/login` -> receives a JWT `Authorization: Bearer <token>` header, granting access to protected routes.

---

## 🚀 Getting Started Locally

### Prerequisites
* Docker & Docker Compose
* Minikube & `kubectl` (Optional, for Kubernetes deployment)
* Java 17+ and Maven
* Node.js v18+ & NPM (for React frontend)

### Option 1: Docker Compose (Simplest)
This will spin up all databases, message brokers, and backend microservices locally.

1. Create a `docker-compose.env` file (or just rely on the `.yml` defaults if configured for testing).
2. Start the infrastructure:
   ```bash
   docker-compose up -d --build
   ```
3. Check the logs to ensure Kafka and services are stable:
   ```bash
   docker-compose logs -f
   ```

### Option 2: Kubernetes (Production Simulation)
If you want to run this purely in K8s locally using Minikube:

1. Start Minikube: `minikube start`
2. Open the Docker daemon to Minikube: `eval $(minikube docker-env)` (Linux/Mac) or use Hyper-V on Windows.
3. Apply the manifests:
   ```bash
   kubectl apply -f k8s.yml
   ```
4. Access the API gateway via NodePort: You can find the URL by running `minikube service api-gateway --url`.

### CI/CD: Jenkins Pipeline
A complete `Jenkinsfile` is provided to automate build and Kubernetes deployments.
1. Deploy Jenkins container locally mounting your local Docker socket.
2. In Jenkins, create a new **Pipeline** pointing to the Git repository.
3. Add a Global Jenkins Credential (ID: `github-credentials`) using your GitHub Personal Access Token.
4. The pipeline will automatically checkout, build multi-stage Docker images, apply `k8s.yml`, and rollout restarts.

---

## 💻 Running the React Frontend
*(Assumes frontend is located in your `reactpractice` directory)*

The frontend runs using Vite's ultra-fast dev server and will automatically refresh when you make code changes.

1. Open a new terminal and navigate to your React project directory.
   ```bash
   cd reactjspractice/reactpractice
   ```
2. Install NodeJS dependencies (only needed the first time):
   ```bash
   npm install
   ```
3. Start the dev daemon:
   ```bash
   npm run dev
   ```
4. Open your browser to `http://localhost:5173` (or the port Vite provides) to interact with the Auth App!

### Environment Variables
Ensure your React application connects to the API Gateway. Typically, you will have an Axios baseURL set in `src/api/apiClient.js` pointing to `http://localhost:8080/auth`. 

If deploying to Minikube, you must update the React Axios base URL to point to your specific `http://<MINIKUBE_IP>:30080` address.

---

## 🧪 Quick Test Endpoints

If you wish to test via cURL or Postman (A Postman collection `springpractice-postman_collection.json` is included in this repository!):

* **Register:** `POST http://localhost:8080/auth/register` (body contains `email`, `username`, `password`)
* **Verify:** `POST http://localhost:8080/auth/verify` (body contains `email`, `otpCode`)
* **Login:** `POST http://localhost:8080/auth/login` (body contains `email`, `password`)

---

*Project built leveraging Spring Cloud, Resilience4j, Apache Kafka, Vite, and Kubernetes.*
