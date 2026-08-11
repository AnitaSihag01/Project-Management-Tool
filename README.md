#  Project Management Tool

A full-stack project management application inspired by Trello and Asana. The application allows users to securely log in, create and manage projects, assign tasks, track progress, and collaborate through task comments and real-time updates.

##  Features

*  User login and authentication
*  JWT-based security
*  Login frontend
*  Project and team member management
*  Create and manage projects
* Create, update, and delete tasks
* Assign tasks to team members
* Track task status and progress
*  Add comments to tasks
*  Real-time updates using WebSockets
*  Spring Security protected APIs
*  PostgreSQL database
*  RESTful APIs

##  Tech Stack

### Backend

* Java
* Spring Boot
* Spring Security
* JWT
* Spring Data JPA
* Hibernate
* WebSocket
* STOMP
* Maven

### Frontend

* Thymeleaf
* HTML
* CSS
* JavaScript

### Database

* PostgreSQL

## 📂 Project Structure

```text
src
└── main
    ├── java
    │   └── com.example.Project_Management
    │       ├── controller
    │       ├── service
    │       ├── repository
    │       ├── entity
    │       ├── dto
    │       ├── security
    │       ├── websocket
    │       └── exception
    │
    └── resources
        ├── templates
        ├── static - index.html , app.js
        └── application.yml
```

## 🔐 Authentication

The application uses **Spring Security and JWT** to secure user authentication and protected resources.

```text
Login
  ↓
Authentication
  ↓
JWT Token
  ↓
Authenticated Request
  ↓
Spring Security
  ↓
Protected API
```

Passwords are securely hashed before being stored in the database.

##  Real-Time Updates

The project uses **WebSocket** to support real-time communication between connected users.

```text
User A
   │
   │ Action
   ▼
Spring Boot
   │
   │ WebSocket
   ▼
Connected Users
   │
   ▼
Real-Time Update
```

This allows changes and notifications to be delivered without requiring users to continuously refresh the page.

##  Task Management

Tasks can be created and managed within projects.

Each task can contain:

* Title
* Description
* Status
* Priority
* Due date
* Assigned user
* Comments

Example workflow:

```text
TODO → IN_PROGRESS → DONE
```

## 💬 Task Comments

Users can communicate within individual tasks by adding comments.

```text
Task
 │
 ├── Comment 1
 ├── Comment 2
 └── Comment 3
```

This keeps project-related communication connected to the relevant task.

## 🌐 API Overview

### Authentication

```http
POST /api/auth/register
POST /api/auth/login
```

### Projects

```http
POST   /api/projects
GET    /api/projects
GET    /api/projects/{id}
PUT    /api/projects/{id}
DELETE /api/projects/{id}
```

### Tasks

```http
POST   /api/projects/{projectId}/tasks
GET    /api/projects/{projectId}/tasks
GET    /api/tasks/{id}
PUT    /api/tasks/{id}
DELETE /api/tasks/{id}
```

### Comments

```http
POST /api/tasks/{taskId}/comments
GET  /api/tasks/{taskId}/comments
DELETE /api/comments/{id}
```

##  Main Entities

The application is built around the following entities:

* **User** – Stores user authentication and account information.
* **Project** – Represents a project created and managed by users.
* **Project Member** – Associates users with projects.
* **Task** – Represents work assigned to a project member.
* **Comment** – Allows users to communicate within tasks.

```text
User
 │
 ├──────────────┐
 │              │
 ▼              ▼
Project     Project Member
 │
 ▼
Task
 │
 ▼
Comment
```

##  Getting Started

### 1. Clone the repository

```bash
git clone https://github.com/AnitaSihag01/Project-Management-Tool.git
```

### 2. Configure PostgreSQL

Create a PostgreSQL database:

```sql
CREATE DATABASE project_management;
```

Configure your database credentials in `application.properties`:

```properties
spring.datasource.url=jdbc:postgresql://localhost:5432/project_management
spring.datasource.username=your_username
spring.datasource.password=your_password

spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=true
```

### 3. Configure JWT

Configure the JWT secret and related security properties according to your local environment.

### 4. Run the application

Using Maven:

```bash
mvn spring-boot:run
```

Or run the main Spring Boot application class from IntelliJ IDEA.

##  Testing

The APIs can be tested using:

* Postman
* Browser
* Frontend UI
* JUnit / Spring Boot tests

Recommended flow:

```text
Register
   ↓
Login
   ↓
Receive JWT
   ↓
Create Project
   ↓
Add Members
   ↓
Create Task
   ↓
Assign Task
   ↓
Add Comment
   ↓
Receive Real-Time Updates
```

##  Future Enhancements

*  Advanced notification system
*  Email notifications
*  File attachments
*  Task search and filtering
*  Calendar and deadline view
*  Project analytics dashboard
*  Drag-and-drop Kanban board
*  Dark mode
*  Improved team collaboration features

##  Learning Outcomes

This project demonstrates practical experience with:

* Spring Boot application development
* REST API development
* JWT authentication
* Spring Security
* Secure login implementation
* JPA and Hibernate
* PostgreSQL database integration
* Entity relationships
* CRUD operations
* DTO-based architecture
* Exception handling
* Thymeleaf frontend development
* WebSocket real-time communication
* Full-stack application development

##  Author

**Anita Sihag**

A full-stack Java/Spring Boot project built to practice backend development, authentication, database design, frontend integration, and real-time collaborative features.
