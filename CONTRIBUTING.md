# Contributing to NexusChat

Thank you for considering contributing to NexusChat! 🎉

## 📋 Table of Contents

- [Code of Conduct](#code-of-conduct)
- [Getting Started](#getting-started)
- [Development Workflow](#development-workflow)
- [Coding Standards](#coding-standards)
- [Testing Guidelines](#testing-guidelines)
- [Pull Request Process](#pull-request-process)
- [Reporting Bugs](#reporting-bugs)

## Code of Conduct

This project adheres to a code of conduct. By participating, you are expected to uphold this code. Please report unacceptable behavior.

## Getting Started

1. Fork the repository
2. Clone your fork: `git clone https://github.com/YOUR_USERNAME/nexuschat.git`
3. Add upstream remote: `git remote add upstream https://github.com/EagleSoft461/nexuschat.git`
4. Create a new branch: `git checkout -b feature/your-feature-name`

### Prerequisites

- Java 17 or higher
- Maven 3.6+
- Docker & Docker Compose
- PostgreSQL 16 (via Docker)
- Redis 7 (via Docker)

## Development Workflow

### 1. Set Up Local Environment

```bash
# Start dependencies
docker-compose up -d postgres redis rabbitmq

# Run the application
mvn spring-boot:run
```

### 2. Make Your Changes

- Write clean, readable code
- Follow existing code patterns
- Add/update tests as needed
- Update documentation if required

### 3. Run Tests

```bash
# Run all tests
mvn clean test

# Run tests with coverage
mvn clean verify jacoco:report

# View coverage report
open target/site/jacoco/index.html
```

### 4. Check Code Quality

```bash
# Run Checkstyle
mvn checkstyle:check

# Run SpotBugs
mvn spotbugs:check

# Run OWASP Dependency Check
mvn dependency-check:check
```

## Coding Standards

### Java Code Style

- Follow Google Java Style Guide
- Use meaningful variable and method names
- Keep methods small and focused (ideally < 50 lines)
- Add JavaDoc for public methods and classes
- Use `@Override` annotation where applicable

### Project Structure

```
src/
├── main/
│   ├── java/com/nexuschat/
│   │   ├── config/          # Configuration classes
│   │   ├── controller/      # REST & WebSocket controllers
│   │   ├── dto/             # Data Transfer Objects
│   │   ├── model/           # JPA entities
│   │   ├── repository/      # Spring Data repositories
│   │   ├── service/         # Business logic
│   │   ├── security/        # Security components
│   │   └── exception/       # Custom exceptions
│   └── resources/
│       ├── db/migration/    # Flyway migrations
│       └── application.properties
└── test/
    └── java/com/nexuschat/  # Mirror main structure
```

### Naming Conventions

- **Classes**: PascalCase (e.g., `MessageService`, `UserRepository`)
- **Methods**: camelCase (e.g., `sendMessage`, `getUserById`)
- **Constants**: UPPER_SNAKE_CASE (e.g., `MAX_MESSAGE_LENGTH`)
- **Packages**: lowercase (e.g., `com.nexuschat.service`)

## Testing Guidelines

### Test Coverage Requirements

- **Minimum coverage**: 50% (enforced by JaCoCo)
- **Target coverage**: 70%+ for new features
- **Critical paths**: 90%+ coverage (auth, message sending)

### Test Structure

```java
@Test
void shouldDoSomething_whenCondition() {
    // Arrange
    User user = createTestUser();
    
    // Act
    Result result = service.performAction(user);
    
    // Assert
    assertThat(result).isNotNull();
    assertThat(result.isSuccess()).isTrue();
}
```

### Test Types

1. **Unit Tests** — Test individual methods in isolation with mocks
2. **Integration Tests** — Test multiple components working together
3. **Controller Tests** — Test REST API endpoints with MockMvc
4. **Repository Tests** — Test database operations with `@DataJpaTest`

## Pull Request Process

### Before Submitting

- [ ] All tests pass locally
- [ ] Code coverage meets minimum threshold
- [ ] Code quality checks pass (Checkstyle, SpotBugs)
- [ ] Documentation updated (if needed)
- [ ] Commit messages are clear and descriptive
- [ ] Branch is up to date with `main`

### Commit Message Format

```
type(scope): subject

body (optional)

footer (optional)
```

**Types:**
- `feat`: New feature
- `fix`: Bug fix
- `docs`: Documentation changes
- `style`: Code style changes (formatting)
- `refactor`: Code refactoring
- `test`: Adding or updating tests
- `chore`: Build process or auxiliary tool changes

**Example:**
```
feat(message): add message editing functionality

- Add PATCH endpoint for message editing
- Broadcast edit events via Redis
- Add edited indicator in UI

Closes #42
```

### PR Checklist

1. Fill out the PR template completely
2. Link related issues
3. Request review from maintainers
4. Address review feedback promptly
5. Ensure CI pipeline passes

## Reporting Bugs

### Before Submitting a Bug Report

- Check if the bug has already been reported
- Ensure you're using the latest version
- Try to reproduce the bug consistently

### Bug Report Template

```markdown
**Describe the bug**
A clear and concise description of what the bug is.

**To Reproduce**
Steps to reproduce the behavior:
1. Go to '...'
2. Click on '....'
3. See error

**Expected behavior**
What you expected to happen.

**Actual behavior**
What actually happened.

**Environment:**
- OS: [e.g., Ubuntu 22.04]
- Java Version: [e.g., 17]
- Spring Boot Version: [e.g., 3.2.5]

**Additional context**
Add any other context about the problem here.
```

## Feature Requests

We welcome feature requests! Please:

1. Check if the feature has already been requested
2. Provide a clear use case
3. Explain why it would benefit the project
4. Be open to discussion and alternative solutions

## Questions?

If you have questions, feel free to:
- Open a GitHub Discussion
- Comment on related issues
- Reach out to maintainers

---

Thank you for contributing to NexusChat! 🚀
