package org.example;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import contracts.events.EventType;
import main.Application;
import main.application.dto.AvailableCarDto;
import main.application.port.client.StorageCarCatalogClient;
import main.application.port.client.StorageReadClient;
import main.domain.Money;
import main.domain.exception.StorageServiceUnavailableException;
import main.infrastructure.persistence.entity.OutboxEventJpaEntity;
import main.infrastructure.persistence.repository.OutboxEventJpaRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.util.UUID;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = Application.class)
@AutoConfigureMockMvc
@Testcontainers(disabledWithoutDocker = true)
class PostgresIT {

    private static final String CLIENT_USER_ID = "20000000-0000-0000-0000-000000000001";
    private static final String MANAGER_USER_ID = "20000000-0000-0000-0000-000000000002";
    private static final String ADMIN_USER_ID = "20000000-0000-0000-0000-000000000003";

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("testdb")
            .withUsername("test")
            .withPassword("test");

    @Container
    static KafkaContainer kafka = new KafkaContainer(DockerImageName.parse("confluentinc/cp-kafka:7.6.1"));

    @DynamicPropertySource
    static void configure(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.datasource.driver-class-name", postgres::getDriverClassName);
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "validate");
        registry.add("spring.liquibase.change-log", () -> "classpath:db/changelog/db.changelog-master.yaml");
        registry.add("spring.security.oauth2.resourceserver.jwt.issuer-uri", () -> "http://localhost/test-issuer");
        registry.add("spring.security.oauth2.resourceserver.jwt.jwk-set-uri", () -> "http://localhost/test-jwks");
        registry.add("spring.kafka.bootstrap-servers", kafka::getBootstrapServers);
        registry.add("app.kafka.consumer-group", () -> "order-service-it");
    }

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private OutboxEventJpaRepository outboxEventJpaRepository;

    @MockBean
    private JwtDecoder jwtDecoder;

    @MockBean
    private StorageReadClient storageReadClient;

    @MockBean
    private StorageCarCatalogClient storageCarCatalogClient;

    @Test
    void migrations_areApplied() {
        Integer changesets = jdbcTemplate.queryForObject(
                "select count(*) from databasechangelog where id in ('001-create-schema','002-indexes-constraints','003-seed-data','004-add-warehouse-admin-seed')",
                Integer.class);
        Integer seededModels = jdbcTemplate.queryForObject("select count(*) from car_models", Integer.class);
        assertEquals(4, changesets);
        assertNotNull(seededModels);
        assertTrue(seededModels >= 2);
    }

    @Test
    void changingStatusToPaid_createsOutboxRowAtomically() throws Exception {
        UUID carId = UUID.randomUUID();
        when(storageReadClient.getCar(any(UUID.class)))
                .thenReturn(new StorageReadClient.StorageCarSnapshot(carId, true, false));

        String createBody = "{\"carId\":\"" + carId + "\"}";
        String createResponse = mockMvc.perform(post("/api/orders/stock")
                        .contentType("application/json")
                        .content(createBody)
                        .with(jwt().jwt(jwt -> jwt.claim("app_user_id", CLIENT_USER_ID))
                                .authorities(new SimpleGrantedAuthority("ROLE_USER"))))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode created = objectMapper.readTree(createResponse);
        String orderId = created.get("id").asText();

        mockMvc.perform(patch("/api/orders/stock/{id}/status", orderId)
                        .contentType("application/json")
                        .content("{\"status\":\"MANAGER_APPROVED\"}")
                        .with(jwt().jwt(jwt -> jwt.claim("app_user_id", MANAGER_USER_ID))
                                .authorities(new SimpleGrantedAuthority("ROLE_MANAGER"))))
                .andExpect(status().isOk());

        mockMvc.perform(patch("/api/orders/stock/{id}/status", orderId)
                        .contentType("application/json")
                        .content("{\"status\":\"AWAITING_PAYMENT\"}")
                        .with(jwt().jwt(jwt -> jwt.claim("app_user_id", MANAGER_USER_ID))
                                .authorities(new SimpleGrantedAuthority("ROLE_MANAGER"))))
                .andExpect(status().isOk());

        mockMvc.perform(patch("/api/orders/stock/{id}/status", orderId)
                        .contentType("application/json")
                        .content("{\"status\":\"PAID\"}")
                        .with(jwt().jwt(jwt -> jwt.claim("app_user_id", CLIENT_USER_ID))
                                .authorities(new SimpleGrantedAuthority("ROLE_USER"))))
                .andExpect(status().isOk());

        Set<String> eventTypes = outboxEventJpaRepository.findAll().stream()
                .map(OutboxEventJpaEntity::getEventType)
                .collect(Collectors.toSet());
        assertTrue(eventTypes.contains(EventType.STOCK_CAR_RESERVATION_REQUESTED.name()));
        assertTrue(eventTypes.contains(EventType.STOCK_CAR_WRITE_OFF_REQUESTED.name()));
    }

    @Test
    void restApi_requiresAuthentication() throws Exception {
        mockMvc.perform(get("/api/orders/stock"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void adminEndpoint_rejectsNonAdminAndAllowsAdmin() throws Exception {
        mockMvc.perform(get("/api/admin/car-models")
                        .with(jwt().jwt(jwt -> jwt.claim("app_user_id", CLIENT_USER_ID))
                                .authorities(new SimpleGrantedAuthority("ROLE_USER"))))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/admin/car-models")
                        .with(jwt().jwt(jwt -> jwt.claim("app_user_id", ADMIN_USER_ID))
                                .authorities(new SimpleGrantedAuthority("ROLE_ADMIN"))))
                .andExpect(status().isOk());
    }

    @Test
    void carAvailabilityEndpoint_allowsUserManagerAndAdmin() throws Exception {
        UUID carId = UUID.randomUUID();
        when(storageCarCatalogClient.findAvailableCars()).thenReturn(List.of(
                new AvailableCarDto(carId, "VIN-1", UUID.randomUUID(), "Black", new Money(1_000_000), true)));

        mockMvc.perform(get("/api/v1/cars")
                        .with(jwt().jwt(jwt -> jwt.claim("app_user_id", CLIENT_USER_ID))
                                .authorities(new SimpleGrantedAuthority("ROLE_USER"))))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/cars")
                        .with(jwt().jwt(jwt -> jwt.claim("app_user_id", MANAGER_USER_ID))
                                .authorities(new SimpleGrantedAuthority("ROLE_MANAGER"))))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/cars")
                        .with(jwt().jwt(jwt -> jwt.claim("app_user_id", ADMIN_USER_ID))
                                .authorities(new SimpleGrantedAuthority("ROLE_ADMIN"))))
                .andExpect(status().isOk());
    }

    @Test
    void carAvailabilityEndpoint_requiresAuthenticationAndMapsUnavailable() throws Exception {
        mockMvc.perform(get("/api/v1/cars"))
                .andExpect(status().isUnauthorized());

        when(storageCarCatalogClient.findAvailableCars())
                .thenThrow(new StorageServiceUnavailableException("Storage service is unavailable"));

        mockMvc.perform(get("/api/v1/cars")
                        .with(jwt().jwt(jwt -> jwt.claim("app_user_id", CLIENT_USER_ID))
                                .authorities(new SimpleGrantedAuthority("ROLE_USER"))))
                .andExpect(status().isServiceUnavailable());
    }

    @Test
    void testDriveRequest_mapsStorageUnavailableTo503() throws Exception {
        when(storageReadClient.getCar(any(UUID.class)))
                .thenThrow(new StorageServiceUnavailableException("Storage service is unavailable"));

        mockMvc.perform(post("/api/test-drives/requests")
                        .contentType("application/json")
                        .content("{\"carId\":\"50000000-0000-0000-0000-000000000001\",\"scheduledAt\":\"2099-01-01T10:00:00\"}")
                        .with(jwt().jwt(jwt -> jwt.claim("app_user_id", CLIENT_USER_ID))
                                .authorities(new SimpleGrantedAuthority("ROLE_USER"))))
                .andExpect(status().isServiceUnavailable());
    }

    @Test
    void testDriveRequest_keepsBusinessValidationAs400() throws Exception {
        UUID notAvailableCarId = UUID.randomUUID();
        when(storageReadClient.getCar(notAvailableCarId))
                .thenReturn(new StorageReadClient.StorageCarSnapshot(notAvailableCarId, false, true));

        mockMvc.perform(post("/api/test-drives/requests")
                        .contentType("application/json")
                        .content("{\"carId\":\"" + notAvailableCarId + "\",\"scheduledAt\":\"2099-01-01T10:00:00\"}")
                        .with(jwt().jwt(jwt -> jwt.claim("app_user_id", CLIENT_USER_ID))
                                .authorities(new SimpleGrantedAuthority("ROLE_USER"))))
                .andExpect(status().isBadRequest());

        UUID notInTestDriveListCarId = UUID.randomUUID();
        when(storageReadClient.getCar(notInTestDriveListCarId))
                .thenReturn(new StorageReadClient.StorageCarSnapshot(notInTestDriveListCarId, true, false));

        mockMvc.perform(post("/api/test-drives/requests")
                        .contentType("application/json")
                        .content("{\"carId\":\"" + notInTestDriveListCarId + "\",\"scheduledAt\":\"2099-01-01T10:00:00\"}")
                        .with(jwt().jwt(jwt -> jwt.claim("app_user_id", CLIENT_USER_ID))
                                .authorities(new SimpleGrantedAuthority("ROLE_USER"))))
                .andExpect(status().isBadRequest());
    }
}
