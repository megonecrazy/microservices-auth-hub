# Event-Driven Microservices Auth Platform

A production-grade, event-driven backend microservices architecture built with Spring Boot, Apache Kafka, Kubernetes, Docker, and PostgreSQL. This project demonstrates modern distributed system patterns, asynchronous communication, decentralized data, and resilient infrastructure.

---

## 🏛 Architecture Overview

### 1. Application Microservices (Spring Boot)
* **API Gateway (`port 8080`):** The central entry point. Handles intelligent routing, centralized CORS, global exception mapping, and Resilience4j circuit breakers mapped to discrete fallback controllers (handles `5xx` downstream faults asynchronously without breaking the HTTP chain).
* **Auth Service (`port 8081`):** Core domain logic. Exposes registration and authentication validation, handles JWT issuance/verification, manages the database layer, and acts as a **Kafka Producer** mapping business transactions (`UserRegisteredEvent`) asynchronously to the broker.
* **Notification Service (`port 8082`):** Decoupled infrastructure worker. Acts as a strict **Kafka Consumer** listening for `user-registration-events`, consuming the DTO via custom Type Mappings, and dispatching HTML OTP verification emails via Gmail SMTP.

### 2. Infrastructure & Messaging
* **Apache Kafka & Zookeeper:** High-throughput streaming bus facilitating the asynchronous, decoupled flow between bounded contexts. Persistent Volume Claims guarantee topic/message resilience across Pod restarts.
* **PostgreSQL:** ACID-compliant persistent relational map. Implements flyway DB migrations and JPA auditing.
* **Zipkin:** Distributed telemetry/tracing to map network bounds end-to-end across service invocations.
* **Prometheus & Grafana:** Containerized metrics collection pulling actuator endpoints at strict intervals.

---

## 🔄 Registration & Auth Workflow

1. **Client POST:** HTTP Client hits `/auth/register` through API Gateway.
2. **Gateway Routing:** API Gateway authenticates the unauthenticated permissive route and forwards to Auth Service.
3. **Database & Publishing (Auth):** Auth Service persists the user as *Inactive/Unverified*, generates a 6-digit OTP, and drops a `UserRegisteredEvent` onto the Kafka `user-registration-events` topic context. Returns `201 Created` immediately.
4. **Asynchronous Dispatch (Notification):** Notification service picks up the uncommitted offset and dispatches an HTML email containing the OTP. 
5. **OTP Verification:** Client hits `/auth/verify` with the OTP. Auth Service updates postgres boolean state `verified = true`, allowing authentication.
6. **Authentication:** Client hits `/auth/login` → Returns 15-minute standard JWT token to be injected into Authorization Bearer headers for downstream protected routes.

---

## 🚀 Getting Started Locally

### Prerequisites
* Docker & Docker Compose
* Minikube & `kubectl` (Optional, for strictly Kubernetes provisioning)
* Java 17+ and Maven

### Option 1: Docker Compose (Simplest)
This spins up all databases, message brokers, tracking layers, and backend microservices inside a localized docker network map.

1. Ensure environment variables (`JWT_SECRET`, `SPRING_DATASOURCE`, etc.) are mapped locally.
2. Start the infrastructure:
   ```bash
   docker-compose up -d --build
   ```
3. Read the stream map to ensure Kafka and services are stable:
   ```bash
   docker-compose logs -f
   ```

### Option 2: Kubernetes (Production Simulation)
Runs the entire bounded context natively inside K8s (Minikube).

1. Start Minikube: `minikube start`
2. Apply the orchestration schema:
   ```bash
   kubectl apply -f k8s.yml
   ```
3. Wait for all rollouts (`Zookeeper` → `Kafka` → `Services`)
4. Access the API gateway via NodePort mapping: `minikube service api-gateway --url`.

### CI/CD: Jenkins Pipeline
A complete pipeline artifact `Jenkinsfile` is provided to automate build and Kubernetes deployments directly tied to a Git polling trigger.
1. Build a custom Jenkins agent via provided `Dockerfile.jenkins` and pre-populated `plugins.txt`.
2. Map your local `$HOME/.kube` and `/var/run/docker.sock` explicitly.
3. Map global Jenkins credential ID `github-credentials` as a standard username/PAT scope.
4. The pipeline will automatically checkout, construct the multi-stage Docker images, deploy `k8s.yml`, and manage rollouts based on the probe intervals.

---

## 🧪 Postman Automation

A Postman collection `springpractice-postman_collection.json` is located in the root repository. 

1. Import the file directly into Postman.
2. Adjust the `base_url` variable to match the Docker output (`http://localhost:8080`) or your Minikube generated ingress pointer (`http://<MINIKUBE_IP>:30080`).
3. Fire `Register User` `->` fetch the code locally in your Gmail inbox `->` Fire `Verify OTP` `->` Fire `Login`.
