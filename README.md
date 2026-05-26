# SpringAIAgent

# Spring AI Agent

A Spring Boot application that demonstrates Retrieval-Augmented Generation (RAG) using Spring AI. This application ingests PDF documents, converts them into embeddings, stores them in a vector database, and uses an LLM to answer questions based on the document content.

## Features

- **PDF Document Ingestion**: Load and process PDF files
- **Text Chunking**: Split documents into manageable chunks using TokenTextSplitter
- **Vector Embeddings**: Convert text to vector embeddings using Spring AI
- **Vector Database**: Store embeddings in PostgreSQL with pgvector extension
- **Question-Answering**: Answer questions based on document content using QuestionAnswerAdvisor
- **LLM Integration**: Powered by Ollama for local LLM inference
- **REST API**: Simple REST endpoint to query the AI agent

## Prerequisites

- **Java 21+**
- **Maven 3.6+**
- **Docker & Docker Compose** (for PostgreSQL with pgvector)
- **Ollama** (for LLM inference)

## Project Structure

```
SpringAIAgent/
├── src/main/java/com/nikhil/SpringAIAgent/
│   ├── controller/
│   │   └── AgentController.java          # REST API endpoints
│   ├── data/loader/
│   │   └── PdfIngestionDataLoader.java   # PDF ingestion logic
│   └── SpringAiAgentApplication.java     # Main application class
├── src/main/resources/
│   ├── application.properties            # Application configuration
│   └── documents/
│       └── sample.pdf                    # Sample PDF for ingestion
├── pom.xml                               # Maven dependencies
└── docker-compose.yml                    # PostgreSQL with pgvector setup
```

## Dependencies

- **Spring Boot 4.0.6**
- **Spring AI 2.0.0-M6**
  - Ollama Model Starter
  - PGVector Vector Store
  - PDF Document Reader
  - Advisors Vector Store
- **PostgreSQL Driver**
- **Docker Compose**

## Setup Instructions

### 1. Start PostgreSQL with pgvector

```bash
docker-compose up -d
```

This starts a PostgreSQL container with:
- Database: `aidb`
- User: `user`
- Password: `password`
- Port: `5432`

### 2. Configure Ollama

Ensure Ollama is running with a model (e.g., `ollama serve`). The application expects:
- Ollama URL: `http://localhost:11434` (default)
- Model: `ollama` (configurable via application.properties)

### 3. Configure Application

Edit `src/main/resources/application.properties`:

```properties
# PDF file path to ingest
app.pdf-path=classpath:documents/sample.pdf

# Ollama Configuration
spring.ai.ollama.base-url=http://localhost:11434
spring.ai.ollama.chat.options.model=mistral
spring.ai.ollama.embedding.options.model=nomic-embed-text

# PostgreSQL Configuration
spring.datasource.url=jdbc:postgresql://localhost:5432/aidb
spring.datasource.username=user
spring.datasource.password=password
spring.datasource.driver-class-name=org.postgresql.Driver

# JPA Configuration
spring.jpa.hibernate.ddl-auto=update
spring.jpa.database-platform=org.hibernate.dialect.PostgreSQL10Dialect
```

### 4. Build the Application

```bash
mvn clean install
```

### 5. Run the Application

```bash
mvn spring-boot:run
```

Or run the JAR:

```bash
java -jar target/SpringAIAgent-0.0.1-SNAPSHOT.jar
```

The application starts on `http://localhost:8080`

### 6. Access PostgreSQL via pgAdmin UI

pgAdmin is a web-based GUI for managing PostgreSQL databases.

#### Open pgAdmin Dashboard

Open your web browser and navigate to:
```
http://localhost:5050
```

Log in using the default credentials:
- **Email:** admin@example.com
- **Password:** adminpassword

#### Connect to Your AI Database

Once logged in, follow these steps to register your database connection:

1. **Right-click on "Servers"** in the left panel
2. Select **Register → Server...**

3. **In the "General" tab:**
   - Type a name for your database connection (e.g., `AI-Database`)

4. **In the "Connection" tab, enter:**
   - **Host name/address:** `postgres` *(Important: Use the literal word "postgres", which is your Docker network service name)*
   - **Port:** `5432`
   - **Maintenance database:** `aidb`
   - **Username:** `user`
   - **Password:** `password`
   - Check **"Save password?"** to avoid having to retype it

5. **Click "Save"**

Your database connection is now configured. You can browse tables, run queries, and inspect your vector embeddings directly in pgAdmin!

## API Endpoints

### Ask Question

**Endpoint:** `GET /api/ask`

**Parameters:**
- `question` (query parameter): The question to ask based on the PDF content

**Example:**
```bash
curl "http://localhost:8080/api/ask?question=What%20is%20the%20main%20topic%20of%20this%20document?"
```

**Response:**
```json
{
  "answer": "The main topic of this document is..."
}
```

## How It Works

1. **Document Ingestion** (`PdfIngestionDataLoader`)
   - Reads PDF file from configured path
   - Extracts text from PDF

2. **Text Splitting** (`TokenTextSplitter`)
   - Splits text into smaller chunks for efficient processing
   - Default token limit: 800

3. **Vectorization**
   - Converts text chunks to vector embeddings using Ollama
   - Stores embeddings in PostgreSQL vector store

4. **Question Answering** (`AgentController`)
   - Receives user question via REST API
   - `QuestionAnswerAdvisor` retrieves relevant document chunks
   - LLM generates answer based on retrieved context
   - Returns answer to user

## Configuration Properties

| Property | Default | Description |
|----------|---------|-------------|
| `app.pdf-path` | `classpath:documents/sample.pdf` | Path to PDF file to ingest |
| `spring.ai.ollama.base-url` | `http://localhost:11434` | Ollama server URL |
| `spring.ai.ollama.chat.options.model` | `mistral` | LLM model for chat |
| `spring.ai.ollama.embedding.options.model` | `nomic-embed-text` | Model for embeddings |
| `spring.datasource.url` | `jdbc:postgresql://localhost:5432/aidb` | PostgreSQL connection URL |
| `spring.jpa.hibernate.ddl-auto` | `update` | Hibernate DDL strategy |

## Troubleshooting

### PostgreSQL Connection Error
- Ensure Docker container is running: `docker ps`
- Check container logs: `docker logs <container_id>`
- Verify credentials match in `application.properties`

### PDF Ingestion Fails
- Check PDF file path in `application.properties`
- Ensure PDF file exists and is not corrupted
- Check application logs for specific error

### Ollama Connection Error
- Ensure Ollama is running: `ollama serve`
- Verify Ollama URL is correct
- Check that required model is available: `ollama list`

### No Response from API
- Check application is running on port 8080
- Verify PostgreSQL is running and accessible
- Check if PDF has been ingested (logs show "PDF Document successfully ingested")
- Check application logs for errors

## Example Workflow

```bash
# 1. Start PostgreSQL
docker-compose up -d

# 2. Start Ollama (in another terminal)
ollama serve

# 3. Build and run application
mvn clean install
mvn spring-boot:run

# 4. Query the API (in another terminal)
curl "http://localhost:8080/api/ask?question=What%20is%20the%20main%20idea?"
```

## Technologies Used

- **Spring Boot**: Web framework
- **Spring AI**: AI/ML integration
- **Ollama**: Local LLM inference
- **PostgreSQL**: Vector database
- **pgvector**: Vector storage extension
- **Docker**: Containerization

## License

MIT License

## Author

Nikhil Tirmanwar

## Support

For issues, questions, or contributions, please refer to the project repository.
