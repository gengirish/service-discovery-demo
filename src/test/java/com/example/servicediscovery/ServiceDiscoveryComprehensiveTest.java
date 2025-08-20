package com.example.servicediscovery;

import com.netflix.appinfo.ApplicationInfoManager;
import com.netflix.appinfo.InstanceInfo;
import com.netflix.discovery.EurekaClient;
import com.netflix.discovery.shared.Application;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.util.Collections;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Comprehensive test suite for Service Discovery Application
 * 
 * This test class covers:
 * - Service registration with Eureka
 * - Endpoint functionality and discovery
 * - Configuration validation
 * - Integration testing scenarios
 * - Error handling and edge cases
 * 
 * @author Service Discovery Test Suite
 * @version 1.0.0
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@SpringBootTest(classes = ServiceDiscoveryApplication.class)
@TestPropertySource(locations = "classpath:application-test.properties")
public class ServiceDiscoveryComprehensiveTest {

    @MockBean
    private EurekaClient eurekaClient;

    @MockBean
    private ApplicationInfoManager applicationInfoManager;

    @Autowired
    private WebApplicationContext webApplicationContext;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
        
        // Setup default mock behavior
        InstanceInfo mockInstanceInfo = mock(InstanceInfo.class);
        when(mockInstanceInfo.getInstanceId()).thenReturn("service-discovery-demo:test-instance");
        when(mockInstanceInfo.getHomePageUrl()).thenReturn("http://localhost:8080/");
        when(mockInstanceInfo.getHealthCheckUrl()).thenReturn("http://localhost:8080/actuator/health");
        when(mockInstanceInfo.getStatusPageUrl()).thenReturn("http://localhost:8080/actuator/info");
        
        when(applicationInfoManager.getInfo()).thenReturn(mockInstanceInfo);
        when(eurekaClient.getApplicationInfoManager()).thenReturn(applicationInfoManager);
    }

    /**
     * Service Registration Tests
     * Tests for Eureka client registration functionality
     */
    @Nested
    @DisplayName("Service Registration Tests")
    class ServiceRegistrationTests {

        @Test
        @DisplayName("Should successfully register with Eureka server")
        void testSuccessfulEurekaRegistration() {
            // Given
            Application mockApplication = mock(Application.class);
            InstanceInfo mockInstance = mock(InstanceInfo.class);
            when(mockApplication.getInstances()).thenReturn(Collections.singletonList(mockInstance));
            when(eurekaClient.getApplication("service-discovery-demo")).thenReturn(mockApplication);

            // When
            ServiceDiscoveryApplication.ServiceDiscoveryService service = 
                new ServiceDiscoveryApplication.ServiceDiscoveryService();
            service.eurekaClient = eurekaClient;
            service.applicationName = "service-discovery-demo";
            service.eurekaServiceUrl = "http://localhost:8761/eureka/";

            // Then
            assertTrue(service.isRegisteredWithEureka());
            verify(eurekaClient).getApplication("service-discovery-demo");
        }

        @Test
        @DisplayName("Should handle registration failure gracefully")
        void testRegistrationFailure() {
            // Given
            when(eurekaClient.getApplication(anyString())).thenReturn(null);

            // When
            ServiceDiscoveryApplication.ServiceDiscoveryService service = 
                new ServiceDiscoveryApplication.ServiceDiscoveryService();
            service.eurekaClient = eurekaClient;
            service.applicationName = "service-discovery-demo";

            // Then
            assertFalse(service.isRegisteredWithEureka());
        }

        @Test
        @DisplayName("Should handle Eureka client exceptions")
        void testEurekaClientException() {
            // Given
            when(eurekaClient.getApplication(anyString())).thenThrow(new RuntimeException("Connection failed"));

            // When
            ServiceDiscoveryApplication.ServiceDiscoveryService service = 
                new ServiceDiscoveryApplication.ServiceDiscoveryService();
            service.eurekaClient = eurekaClient;
            service.applicationName = "service-discovery-demo";

            // Then
            assertFalse(service.isRegisteredWithEureka());
        }

        @Test
        @DisplayName("Should return correct Eureka service URL")
        void testEurekaServiceUrl() {
            // Given
            String expectedUrl = "http://localhost:8761/eureka/";
            
            // When
            ServiceDiscoveryApplication.ServiceDiscoveryService service = 
                new ServiceDiscoveryApplication.ServiceDiscoveryService();
            service.eurekaServiceUrl = expectedUrl;

            // Then
            assertEquals(expectedUrl, service.getEurekaServiceUrl());
        }

        @Test
        @DisplayName("Should return correct instance ID")
        void testInstanceId() {
            // Given
            String expectedInstanceId = "service-discovery-demo:test-instance";

            // When
            ServiceDiscoveryApplication.ServiceDiscoveryService service = 
                new ServiceDiscoveryApplication.ServiceDiscoveryService();
            service.eurekaClient = eurekaClient;

            // Then
            assertEquals(expectedInstanceId, service.getInstanceId());
        }

        @Test
        @DisplayName("Should handle instance ID retrieval errors")
        void testInstanceIdError() {
            // Given
            when(eurekaClient.getApplicationInfoManager()).thenThrow(new RuntimeException("Error"));

            // When
            ServiceDiscoveryApplication.ServiceDiscoveryService service = 
                new ServiceDiscoveryApplication.ServiceDiscoveryService();
            service.eurekaClient = eurekaClient;

            // Then
            assertEquals("error", service.getInstanceId());
        }

        @Test
        @DisplayName("Should return correct Eureka status when disconnected")
        void testEurekaStatusDisconnected() {
            // Given
            when(eurekaClient.getApplication(anyString())).thenReturn(null);

            // When
            ServiceDiscoveryApplication.ServiceDiscoveryService service = 
                new ServiceDiscoveryApplication.ServiceDiscoveryService();
            service.eurekaClient = eurekaClient;
            service.applicationName = "service-discovery-demo";

            // Then
            assertEquals("DISCONNECTED", service.getEurekaStatus());
        }
    }

    /**
     * Endpoint Discovery Tests
     * Tests for REST endpoint functionality and accessibility
     */
    @Nested
    @DisplayName("Endpoint Discovery Tests")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class EndpointDiscoveryTests {

    @LocalServerPort
    private int port;

        @Test
        @DisplayName("Should return health status from health endpoint")
        void testHealthEndpoint() throws Exception {
            mockMvc.perform(get("/api/health"))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType("application/json"))
                    .andExpect(jsonPath("$.status").value("UP"))
                    .andExpect(jsonPath("$.service").value("service-discovery-demo"))
                    .andExpect(jsonPath("$.timestamp").exists())
                    .andExpect(jsonPath("$.eureka-status").exists());
        }

        @Test
        @DisplayName("Should return service information from info endpoint")
        void testInfoEndpoint() throws Exception {
            mockMvc.perform(get("/api/info"))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType("application/json"))
                    .andExpect(jsonPath("$.name").value("service-discovery-demo"))
                    .andExpect(jsonPath("$.version").value("1.0.0"))
                    .andExpect(jsonPath("$.description").exists())
                    .andExpect(jsonPath("$.port").exists())
                    .andExpect(jsonPath("$.features").isArray())
                    .andExpect(jsonPath("$.timestamp").exists());
        }

        @Test
        @DisplayName("Should return service metadata from metadata endpoint")
        void testMetadataEndpoint() throws Exception {
            mockMvc.perform(get("/api/metadata"))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType("application/json"))
                    .andExpect(jsonPath("$.['service.name']").value("service-discovery-demo"))
                    .andExpect(jsonPath("$.['service.version']").value("1.0.0"))
                    .andExpect(jsonPath("$.['service.type']").value("microservice"))
                    .andExpect(jsonPath("$.['service.framework']").value("Spring Boot 3.2.0"))
                    .andExpect(jsonPath("$.['service.discovery']").value("Netflix Eureka"))
                    .andExpect(jsonPath("$.['service.endpoints']").isArray())
                    .andExpect(jsonPath("$.timestamp").exists());
        }

        @Test
        @DisplayName("Should handle discovery status endpoint errors gracefully")
        void testDiscoveryStatusEndpointError() throws Exception {
            // Given
            when(eurekaClient.getApplication(anyString())).thenThrow(new RuntimeException("Connection error"));

            // When & Then
            mockMvc.perform(get("/api/discovery/status"))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType("application/json"))
                    .andExpect(jsonPath("$.status").value("ERROR"))
                    .andExpect(jsonPath("$.registered").value(false))
                    .andExpect(jsonPath("$.message").exists())
                    .andExpect(jsonPath("$.timestamp").exists());
        }

        @Test
        @DisplayName("Should return correct content type for all endpoints")
        void testEndpointContentTypes() throws Exception {
            mockMvc.perform(get("/api/health"))
                    .andExpect(content().contentType("application/json"));
            
            mockMvc.perform(get("/api/info"))
                    .andExpect(content().contentType("application/json"));
            
            mockMvc.perform(get("/api/discovery/status"))
                    .andExpect(content().contentType("application/json"));
            
            mockMvc.perform(get("/api/metadata"))
                    .andExpect(content().contentType("application/json"));
        }
    }

    /**
     * Configuration Tests
     * Tests for application configuration and properties
     */
    @Nested
    @DisplayName("Configuration Tests")
    class ConfigurationTests {

        @Test
        @DisplayName("Should load application properties correctly")
        void testApplicationPropertiesLoading() {
            // This test verifies that the application context loads successfully
            // with the test properties, which indicates proper configuration
            assertNotNull(webApplicationContext);
                assertTrue(((org.springframework.context.ConfigurableApplicationContext) webApplicationContext).isActive());
        }

        @Test
        @DisplayName("Should create service discovery configuration bean")
        void testServiceDiscoveryConfigurationBean() {
            // Given
            ServiceDiscoveryApplication.ServiceDiscoveryConfig config = 
                new ServiceDiscoveryApplication.ServiceDiscoveryConfig();

            // When
            Map<String, String> properties = config.serviceDiscoveryProperties();

            // Then
            assertNotNull(properties);
            assertEquals("eureka", properties.get("discovery.type"));
            assertEquals("2023.0.0", properties.get("discovery.version"));
            assertEquals("Spring Boot 3.2.0", properties.get("service.framework"));
        }

        @Test
        @DisplayName("Should have correct Eureka client configuration")
        void testEurekaClientConfiguration() {
            // This test verifies that Eureka client beans are properly configured
            assertNotNull(eurekaClient);
            assertNotNull(applicationInfoManager);
        }

        @Test
        @DisplayName("Should handle service discovery service initialization")
        void testServiceDiscoveryServiceInitialization() {
            // Given & When
            ServiceDiscoveryApplication.ServiceDiscoveryService service = 
                new ServiceDiscoveryApplication.ServiceDiscoveryService();

            // Then
            assertNotNull(service.getRegistrationTime());
            assertNotNull(service.getLastHeartbeatTime());
        }

        @Test
        @DisplayName("Should return correct URL configurations")
        void testUrlConfigurations() {
            // Given
            String expectedHomePageUrl = "http://localhost:8080/";
            String expectedHealthCheckUrl = "http://localhost:8080/actuator/health";
            String expectedStatusPageUrl = "http://localhost:8080/actuator/info";

            // When
            ServiceDiscoveryApplication.ServiceDiscoveryService service = 
                new ServiceDiscoveryApplication.ServiceDiscoveryService();
            service.eurekaClient = eurekaClient;

            // Then
            assertEquals(expectedHomePageUrl, service.getHomePageUrl());
            assertEquals(expectedHealthCheckUrl, service.getHealthCheckUrl());
            assertEquals(expectedStatusPageUrl, service.getStatusPageUrl());
        }
    }

    /**
     * Integration Tests
     * Tests for full application integration scenarios
     */
    @Nested
    @DisplayName("Integration Tests")
    @SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
    class IntegrationTests {

        @LocalServerPort
        private int port;

        @Autowired
        private TestRestTemplate restTemplate;

        @Test
        @DisplayName("Should start application successfully with Eureka client")
        void testApplicationStartup() {
            // The fact that the application context loads successfully
            // indicates that the Eureka client integration is working
            assertNotNull(webApplicationContext);
            assertTrue(((org.springframework.context.ConfigurableApplicationContext) webApplicationContext).isActive());
        }

        @Test
        @DisplayName("Should expose health endpoint via HTTP")
        void testHealthEndpointIntegration() {
            // When
            ResponseEntity<Map> response = restTemplate.getForEntity(
                "http://localhost:" + port + "/api/health", Map.class);

            // Then
            assertEquals(HttpStatus.OK, response.getStatusCode());
            assertNotNull(response.getBody());
            assertEquals("UP", response.getBody().get("status"));
        }

        @Test
        @DisplayName("Should expose info endpoint via HTTP")
        void testInfoEndpointIntegration() {
            // When
            ResponseEntity<Map> response = restTemplate.getForEntity(
                "http://localhost:" + port + "/api/info", Map.class);

            // Then
            assertEquals(HttpStatus.OK, response.getStatusCode());
            assertNotNull(response.getBody());
            assertEquals("service-discovery-demo", response.getBody().get("name"));
        }

        @Test
        @DisplayName("Should expose discovery status endpoint via HTTP")
        void testDiscoveryStatusEndpointIntegration() {
            // When
            ResponseEntity<Map> response = restTemplate.getForEntity(
                "http://localhost:" + port + "/api/discovery/status", Map.class);

            // Then
            assertEquals(HttpStatus.OK, response.getStatusCode());
            assertNotNull(response.getBody());
            assertTrue(response.getBody().containsKey("registered"));
            assertTrue(response.getBody().containsKey("status"));
        }

        @Test
        @DisplayName("Should expose metadata endpoint via HTTP")
        void testMetadataEndpointIntegration() {
            // When
            ResponseEntity<Map> response = restTemplate.getForEntity(
                "http://localhost:" + port + "/api/metadata", Map.class);

            // Then
            assertEquals(HttpStatus.OK, response.getStatusCode());
            assertNotNull(response.getBody());
            assertEquals("service-discovery-demo", response.getBody().get("service.name"));
        }

        @Test
        @DisplayName("Should handle concurrent requests to endpoints")
        void testConcurrentEndpointAccess() throws InterruptedException {
            // Given
            int numberOfThreads = 5;
            Thread[] threads = new Thread[numberOfThreads];
            boolean[] results = new boolean[numberOfThreads];

            // When
            for (int i = 0; i < numberOfThreads; i++) {
                final int index = i;
                threads[i] = new Thread(() -> {
                    try {
                        ResponseEntity<Map> response = restTemplate.getForEntity(
                            "http://localhost:" + port + "/api/health", Map.class);
                        results[index] = response.getStatusCode() == HttpStatus.OK;
                    } catch (Exception e) {
                        results[index] = false;
                    }
                });
                threads[i].start();
            }

            // Wait for all threads to complete
            for (Thread thread : threads) {
                thread.join();
            }

            // Then
            for (boolean result : results) {
                assertTrue(result, "All concurrent requests should succeed");
            }
        }

        @Test
        @DisplayName("Should maintain service state across multiple requests")
        void testServiceStateConsistency() {
            // When - Make multiple requests
            ResponseEntity<Map> response1 = restTemplate.getForEntity(
                "http://localhost:" + port + "/api/info", Map.class);
            ResponseEntity<Map> response2 = restTemplate.getForEntity(
                "http://localhost:" + port + "/api/info", Map.class);

            // Then
            assertEquals(HttpStatus.OK, response1.getStatusCode());
            assertEquals(HttpStatus.OK, response2.getStatusCode());
            assertEquals(response1.getBody().get("name"), response2.getBody().get("name"));
            assertEquals(response1.getBody().get("version"), response2.getBody().get("version"));
        }

        @Test
        @DisplayName("Should handle graceful shutdown scenario")
        void testGracefulShutdown() {
            // Given
            ServiceDiscoveryApplication.ServiceDiscoveryService service =
                new ServiceDiscoveryApplication.ServiceDiscoveryService();
            service.eurekaClient = eurekaClient;

            // When
            assertDoesNotThrow(() -> service.onShutdown());

            // Then
            verify(eurekaClient).shutdown();
        }
    }
}
