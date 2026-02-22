## API Rate Limit Testing – Java

This project validates API rate limiting behavior for login endpoints by simulating rapid consecutive requests and verifying throttling responses.

### Project Structure
- `tests` → Test classes for rate limit validation
- `utils` → HTTP client and Excel utility classes
- `resources/data` → Test data files (credentials, inputs)

### Tools & Technologies
- Java
- Maven
- REST APIs
- Apache HttpClient
- Excel (Data-driven testing)

### Test Scenarios Covered
- Multiple login requests within short intervals
- Validation of HTTP 429 (Too Many Requests)
- System recovery after cooldown period

### Key Highlights
- Data-driven testing using Excel
- Reusable HTTP client utilities
- Clear separation of test logic and helpers

### How to Run
1. Clone the repository  
2. Update API endpoint in test class  
3. Run `mvn test`
