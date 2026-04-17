# Study Assistant

Study Assistant is a JavaFX desktop application for managing flashcards and study materials. It is built with Java, Maven, and MySQL, and currently serves as a working prototype for a study tool with a desktop interface.

## Features

- MySQL login screen for connecting to the database
- JavaFX desktop UI
- Dashboard, My Cards, and All Cards screens
- Flashcard, deck, study session, and review models
- DAO and controller structure for database-driven features
- Included SQL setup file and sample JSON data

## Tech Stack

- Java 21
- JavaFX
- Maven
- MySQL
- Gson

## Requirements

- JDK 21
- Maven
- MySQL Server

## Setup

1. Create the MySQL database for the project.
2. Import [`TestDB.sql`](./TestDB.sql) to create the tables and sample records.
3. Make sure your MySQL server is running.
4. Open the project folder and run:

```powershell
mvn javafx:run
```

## Project Structure

```text
study-assistant/
|-- pom.xml
|-- README.md
|-- TestDB.sql
|-- sample-data/
`-- src/main/java/com/studyapp/
    |-- controller/
    |-- dao/
    |-- db/
    |-- model/
    |-- service/
    `-- view/
```

## Current Status

This project is still in prototype stage. The main JavaFX navigation flow is available, but some screens still use hardcoded sample data and a few features are not fully connected yet.
