# DockerMate

DockerMate is a comprehensive Docker management system that provides functionalities for creating, managing, starting, stopping, and deleting Docker containers and images. It integrates with Spring Boot for backend operations and leverages Docker commands to interact with containerized environments.

## Features

- **Container Management**: Create, start, stop, and delete Docker containers.
- **Image Management**: Check the status of Docker images and delete them.
- **User Authentication**: Secure user operations with authentication and authorization.
- **Command Execution**: Execute commands within Docker containers.
- **Status Reporting**: Retrieve the status of containers and images.

## Prerequisites

- Java 11 or higher
- Maven
- Docker
- Spring Boot

## Installation

1. **Clone the Repository**

   ```sh
   git clone https://github.com/akshanshdcode/dockermate.git

2. **Navigate to the Project Directory**

   ```sh
   cd dockermate

3. **Build the Project**

   ```sh
   mvn clean install

4. **Run the Application**

   ```sh
   mvn spring-boot:run
