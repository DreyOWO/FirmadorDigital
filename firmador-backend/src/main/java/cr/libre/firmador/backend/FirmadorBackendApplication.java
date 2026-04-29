package cr.libre.firmador.backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * Main Spring Boot application for Firmador Backend
 * 
 * This application provides REST API endpoints for:
 * - Document management and signature workflows
 * - User authentication and authorization
 * - Integration with Supabase for storage and database
 * - Digital signature operations using firmador-core library
 */
@SpringBootApplication
@EnableAsync
public class FirmadorBackendApplication {
    
    public static void main(String[] args) {
        SpringApplication.run(FirmadorBackendApplication.class, args);
    }
}

// Made with Bob
