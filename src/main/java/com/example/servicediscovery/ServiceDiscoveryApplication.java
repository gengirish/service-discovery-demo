package com.example.servicediscovery;

import com.netflix.discovery.EurekaClient;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.servlet.context.ServletWebServerApplicationContext;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

/**
 * Service Discovery Application demonstrating Eureka client integration
 * 
 * This application showcases:
 * - Service registration with Eureka server
 * - RESTful endpoints for service discovery
 * - Health monitoring and service metadata
 * - Proper configuration management
 * - Graceful shutdown handling
 * 
 * @author Service Discovery Demo
 * @version 1.0.0
 */
@SpringBootApplication
public class ServiceDiscoveryApplication {

    private static final Logger logger = LoggerFactory.getLogger(ServiceDiscoveryApplication.class);

    public static void main(String[] args) {
        logger.info("Starting Service Discovery Application...");
        SpringApplication.run(ServiceDiscoveryApplication.class, args);
        logger.info("Service Discovery Application started successfully!");
    }

    /**
     * REST Controller providing discoverable endpoints
     */
    @RestController
    @RequestMapping("/api")
    public static class ServiceController {

        private static final Logger logger = LoggerFactory.getLogger(ServiceController.class);

        @Autowired
        private ServiceDiscoveryService discoveryService;

        @Autowired
        private EurekaClient eurekaClient;

        @Autowired
        private ApplicationContext applicationContext;

        @Value("${spring.application.name}")
        private String applicationName;

        @Value("${server.port:0}")
        private String serverPort;

        /**
         * Health check endpoint for service monitoring
         * 
         * @return Health status and service information
         */
        @GetMapping("/health")
        public ResponseEntity<Map<String, Object>> health() {
            logger.debug("Health check endpoint called");
            
            Map<String, Object> healthInfo = new HashMap<>();
            healthInfo.put("status", "UP");
            healthInfo.put("timestamp", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
            healthInfo.put("service", applicationName);
            healthInfo.put("port", serverPort);
            healthInfo.put("eureka-status", discoveryService.getEurekaStatus());
            
            return ResponseEntity.ok(healthInfo);
        }

        /**
         * Service information endpoint
         * 
         * @return Service metadata and configuration
         */
        @GetMapping("/info")
        public ResponseEntity<Map<String, Object>> info() {
            logger.debug("Service info endpoint called");
            
            Map<String, Object> serviceInfo = new HashMap<>();
            serviceInfo.put("name", applicationName);
            serviceInfo.put("version", "1.0.0");
            serviceInfo.put("description", "Spring Boot microservice with Eureka service discovery");
            
            // Get the actual running port
            int actualPort = 0;
            if (applicationContext instanceof ServletWebServerApplicationContext) {
                actualPort = ((ServletWebServerApplicationContext) applicationContext).getWebServer().getPort();
            } else {
                // Fallback for test environment - try to get port from server.port property
                try {
                    actualPort = Integer.parseInt(serverPort);
                    if (actualPort == 0) {
                        // If port is 0 (random), we can't determine the actual port in test context
                        actualPort = 8080; // Default fallback
                    }
                } catch (NumberFormatException e) {
                    actualPort = 8080; // Default fallback
                }
            }
            serviceInfo.put("port", String.valueOf(actualPort));
            
            serviceInfo.put("timestamp", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
            serviceInfo.put("features", new String[]{
                "Service Discovery", 
                "Health Monitoring", 
                "RESTful APIs", 
                "Eureka Integration"
            });
            
            return ResponseEntity.ok(serviceInfo);
        }

        /**
         * Discovery status endpoint showing Eureka registration details
         * 
         * @return Discovery server connection and registration status
         */
        @GetMapping("/discovery/status")
        public ResponseEntity<Map<String, Object>> discoveryStatus() {
            logger.debug("Discovery status endpoint called");
            
            Map<String, Object> discoveryInfo = new HashMap<>();
            
            try {
                // Check if there's a connection error first
                boolean hasConnectionError = discoveryService.hasEurekaConnectionError();
                
                if (hasConnectionError) {
                    discoveryInfo.put("status", "ERROR");
                    discoveryInfo.put("registered", false);
                    discoveryInfo.put("message", "Connection error while checking Eureka registration status");
                    discoveryInfo.put("applicationName", applicationName);
                    discoveryInfo.put("eurekaServiceUrl", discoveryService.getEurekaServiceUrl());
                    discoveryInfo.put("instanceId", discoveryService.getInstanceId());
                } else {
                    // Get Eureka client status
                    boolean isRegistered = discoveryService.isRegisteredWithEureka();
                    String eurekaServiceUrl = discoveryService.getEurekaServiceUrl();
                    
                    discoveryInfo.put("registered", isRegistered);
                    discoveryInfo.put("eurekaServiceUrl", eurekaServiceUrl);
                    discoveryInfo.put("applicationName", applicationName);
                    discoveryInfo.put("instanceId", discoveryService.getInstanceId());
                    discoveryInfo.put("homePageUrl", discoveryService.getHomePageUrl());
                    discoveryInfo.put("healthCheckUrl", discoveryService.getHealthCheckUrl());
                    discoveryInfo.put("statusPageUrl", discoveryService.getStatusPageUrl());
                    discoveryInfo.put("lastHeartbeat", discoveryService.getLastHeartbeatTime());
                    discoveryInfo.put("registrationTime", discoveryService.getRegistrationTime());
                    
                    if (isRegistered) {
                        discoveryInfo.put("status", "REGISTERED");
                        discoveryInfo.put("message", "Service successfully registered with Eureka");
                    } else {
                        discoveryInfo.put("status", "NOT_REGISTERED");
                        discoveryInfo.put("message", "Service not yet registered with Eureka or registration failed");
                    }
                }
                
            } catch (Exception e) {
                logger.error("Error retrieving discovery status", e);
                discoveryInfo.put("status", "ERROR");
                discoveryInfo.put("message", "Error retrieving discovery status: " + e.getMessage());
                discoveryInfo.put("registered", false);
            }
            
            discoveryInfo.put("timestamp", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
            
            return ResponseEntity.ok(discoveryInfo);
        }

        /**
         * Service metadata endpoint
         * 
         * @return Additional service metadata for discovery
         */
        @GetMapping("/metadata")
        public ResponseEntity<Map<String, Object>> metadata() {
            logger.debug("Service metadata endpoint called");
            
            Map<String, Object> metadata = new HashMap<>();
            metadata.put("service.name", applicationName);
            metadata.put("service.version", "1.0.0");
            metadata.put("service.type", "microservice");
            metadata.put("service.framework", "Spring Boot 3.2.0");
            metadata.put("service.discovery", "Netflix Eureka");
            metadata.put("service.endpoints", new String[]{
                "/api/health", 
                "/api/info", 
                "/api/discovery/status", 
                "/api/metadata"
            });
            metadata.put("timestamp", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
            
            return ResponseEntity.ok(metadata);
        }
    }

    /**
     * Service class handling Eureka client operations
     */
    @Service
    public static class ServiceDiscoveryService {

        private static final Logger logger = LoggerFactory.getLogger(ServiceDiscoveryService.class);

        @Autowired
        protected EurekaClient eurekaClient;

        @Value("${spring.application.name}")
        protected String applicationName;

        @Value("${eureka.client.service-url.defaultZone}")
        protected String eurekaServiceUrl;

        private LocalDateTime registrationTime;
        private LocalDateTime lastHeartbeatTime;

        /**
         * Initialize service registration tracking
         */
        public ServiceDiscoveryService() {
            this.registrationTime = LocalDateTime.now();
            this.lastHeartbeatTime = LocalDateTime.now();
        }

        /**
         * Check if service is registered with Eureka
         * 
         * @return true if registered, false otherwise
         */
        public boolean isRegisteredWithEureka() {
            try {
                var application = eurekaClient.getApplication(applicationName);
                boolean registered = application != null && !application.getInstances().isEmpty();
                
                if (registered) {
                    lastHeartbeatTime = LocalDateTime.now();
                    logger.debug("Service is registered with Eureka");
                } else {
                    logger.warn("Service is not registered with Eureka");
                }
                
                return registered;
            } catch (Exception e) {
                logger.error("Error checking Eureka registration status", e);
                return false; // Return false instead of throwing exception
            }
        }

        /**
         * Check if there's a connection error with Eureka
         * 
         * @return true if there's a connection error, false otherwise
         */
        public boolean hasEurekaConnectionError() {
            try {
                eurekaClient.getApplication(applicationName);
                return false;
            } catch (Exception e) {
                logger.error("Connection error with Eureka", e);
                return true;
            }
        }

        /**
         * Get Eureka service URL
         * 
         * @return Eureka server URL
         */
        public String getEurekaServiceUrl() {
            return eurekaServiceUrl;
        }

        /**
         * Get Eureka client status
         * 
         * @return Status string
         */
        public String getEurekaStatus() {
            try {
                return isRegisteredWithEureka() ? "CONNECTED" : "DISCONNECTED";
            } catch (Exception e) {
                logger.error("Error getting Eureka status", e);
                return "ERROR";
            }
        }

        /**
         * Get service instance ID
         * 
         * @return Instance ID
         */
        public String getInstanceId() {
            try {
                var instanceInfo = eurekaClient.getApplicationInfoManager().getInfo();
                return instanceInfo != null ? instanceInfo.getInstanceId() : "unknown";
            } catch (Exception e) {
                logger.error("Error getting instance ID", e);
                return "error";
            }
        }

        /**
         * Get home page URL
         * 
         * @return Home page URL
         */
        public String getHomePageUrl() {
            try {
                var instanceInfo = eurekaClient.getApplicationInfoManager().getInfo();
                return instanceInfo != null ? instanceInfo.getHomePageUrl() : "unknown";
            } catch (Exception e) {
                logger.error("Error getting home page URL", e);
                return "error";
            }
        }

        /**
         * Get health check URL
         * 
         * @return Health check URL
         */
        public String getHealthCheckUrl() {
            try {
                var instanceInfo = eurekaClient.getApplicationInfoManager().getInfo();
                return instanceInfo != null ? instanceInfo.getHealthCheckUrl() : "unknown";
            } catch (Exception e) {
                logger.error("Error getting health check URL", e);
                return "error";
            }
        }

        /**
         * Get status page URL
         * 
         * @return Status page URL
         */
        public String getStatusPageUrl() {
            try {
                var instanceInfo = eurekaClient.getApplicationInfoManager().getInfo();
                return instanceInfo != null ? instanceInfo.getStatusPageUrl() : "unknown";
            } catch (Exception e) {
                logger.error("Error getting status page URL", e);
                return "error";
            }
        }

        /**
         * Get last heartbeat time
         * 
         * @return Last heartbeat timestamp
         */
        public String getLastHeartbeatTime() {
            return lastHeartbeatTime.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        }

        /**
         * Get registration time
         * 
         * @return Registration timestamp
         */
        public String getRegistrationTime() {
            return registrationTime.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        }

        /**
         * Handle graceful shutdown
         */
        @PreDestroy
        public void onShutdown() {
            logger.info("Service shutting down, deregistering from Eureka...");
            try {
                eurekaClient.shutdown();
                logger.info("Successfully deregistered from Eureka");
            } catch (Exception e) {
                logger.error("Error during Eureka deregistration", e);
            }
        }
    }

    /**
     * Configuration class for service discovery
     */
    @Configuration
    public static class ServiceDiscoveryConfig {

        private static final Logger logger = LoggerFactory.getLogger(ServiceDiscoveryConfig.class);

        /**
         * Custom configuration bean for additional setup if needed
         * 
         * @return Configuration properties
         */
        @Bean
        public Map<String, String> serviceDiscoveryProperties() {
            logger.info("Initializing service discovery configuration");
            
            Map<String, String> properties = new HashMap<>();
            properties.put("discovery.type", "eureka");
            properties.put("discovery.version", "2023.0.0");
            properties.put("service.framework", "Spring Boot 3.2.0");
            
            return properties;
        }
    }
}
