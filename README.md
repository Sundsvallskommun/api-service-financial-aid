# Financial Aid

_The service is a proxy for Försäkringskassan's SSBTEK composite service (Sammansatt Bastjänst för Ekonomiskt Bistånd). It aggregates data from seven Swedish government agencies (CSN, FK, AF, SKV, SO, TNS, MIV) into a single JSON response used as decision support in social welfare casework._

## Getting Started

### Prerequisites

- **Java 25 or higher**
- **Maven**
- **Git**

### Installation

1. **Clone the repository:**

```bash
git clone https://github.com/Sundsvallskommun/api-service-financial-aid.git
cd api-service-financial-aid
```

2. **Configure the application:**

   Before running the application, you need to set up configuration settings.
   See [Configuration](#configuration)

   **Note:** Ensure all required configurations are set; otherwise, the application may fail to start.

3. **Ensure dependent services are running:**

   *SSBTEK (Försäkringskassan)*

   - Purpose: Composite service that aggregates data from CSN, FK, AF, SKV, SO, TNS and MIV.
   - Setup Instructions: Ensure you have access to the SSBTEK endpoint and a valid mTLS keystore issued for the consuming organisation.
4. **Build and run the application:**

- Using Maven:

```bash
mvn spring-boot:run
```

- Using Gradle:

```bash
gradle bootRun
```

## API Documentation

Access the API documentation via:

- **Swagger UI:** [http://localhost:8080/api-docs](http://localhost:8080/api-docs)

## Usage

### API Endpoints

See the [API Documentation](#api-documentation) for detailed information on available endpoints.

### Example Request

```bash
curl -X 'GET' 'http://localhost:8080/financial-aid?personalNumber=199001011234&fromDate=2025-01-01&toDate=2025-06-30'
```

## Configuration

Configuration is crucial for the application to run successfully. Ensure all necessary settings are configured in
`application.yml`.

### Key Configuration Parameters

- **Server Port:**

```yaml
server:
  port: 8080
```

- **Integration Settings:**

```yaml
integration:
  ssbtek:
    url: <ssbtek-endpoint-url>
    connect-timeout: <connect-timeout-seconds>
    read-timeout: <read-timeout-seconds>
    key-store-as-base64: <base64-encoded-keystore>
    key-store-password: <keystore-password>
```

The `key-store-as-base64` and `key-store-password` fields are optional. When omitted (or left blank) the service skips mTLS configuration and uses the default HTTP client.

### Additional Notes

- **Application Profiles:**

  Use Spring profiles (`dev`, `prod`, etc.) to manage different configurations for different environments.

- **Logging Configuration:**

  Adjust logging levels if necessary.

## Contributing

Contributions are welcome! Please
see [CONTRIBUTING.md](https://github.com/Sundsvallskommun/.github/blob/main/.github/CONTRIBUTING.md) for guidelines.

## License

This project is licensed under the [MIT License](LICENSE).

## Status

[![Quality Gate Status](https://sonarcloud.io/api/project_badges/measure?project=Sundsvallskommun_api-service-financial-aid&metric=alert_status)](https://sonarcloud.io/summary/overall?id=Sundsvallskommun_api-service-financial-aid)
[![Reliability Rating](https://sonarcloud.io/api/project_badges/measure?project=Sundsvallskommun_api-service-financial-aid&metric=reliability_rating)](https://sonarcloud.io/summary/overall?id=Sundsvallskommun_api-service-financial-aid)
[![Security Rating](https://sonarcloud.io/api/project_badges/measure?project=Sundsvallskommun_api-service-financial-aid&metric=security_rating)](https://sonarcloud.io/summary/overall?id=Sundsvallskommun_api-service-financial-aid)
[![Maintainability Rating](https://sonarcloud.io/api/project_badges/measure?project=Sundsvallskommun_api-service-financial-aid&metric=sqale_rating)](https://sonarcloud.io/summary/overall?id=Sundsvallskommun_api-service-financial-aid)
[![Vulnerabilities](https://sonarcloud.io/api/project_badges/measure?project=Sundsvallskommun_api-service-financial-aid&metric=vulnerabilities)](https://sonarcloud.io/summary/overall?id=Sundsvallskommun_api-service-financial-aid)
[![Bugs](https://sonarcloud.io/api/project_badges/measure?project=Sundsvallskommun_api-service-financial-aid&metric=bugs)](https://sonarcloud.io/summary/overall?id=Sundsvallskommun_api-service-financial-aid)

## 

&copy; 2026 Sundsvalls kommun
