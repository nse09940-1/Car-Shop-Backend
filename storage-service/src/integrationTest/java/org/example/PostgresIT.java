package org.example;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import main.Application;
import main.infrastructure.persistence.entity.CarJpaEntity;
import main.infrastructure.persistence.repository.CarJpaRepository;
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = Application.class)
@AutoConfigureMockMvc
@Testcontainers(disabledWithoutDocker = true)
class PostgresIT {

    private static final UUID SEED_MODEL_ID = UUID.fromString("10000000-0000-0000-0000-000000000001");
    private static final String WAREHOUSE_ADMIN_USER_ID = "20000000-0000-0000-0000-000000000004";

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
        registry.add("app.kafka.consumer-group", () -> "storage-service-it");
    }

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private CarJpaRepository carJpaRepository;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private JwtDecoder jwtDecoder;

    @Test
    void migrations_areApplied() {
        Integer changesets = jdbcTemplate.queryForObject(
                "select count(*) from databasechangelog where id in ('001-create-schema','002-indexes-constraints','003-seed-data')",
                Integer.class);
        Integer seededCars = jdbcTemplate.queryForObject("select count(*) from cars", Integer.class);
        assertEquals(3, changesets);
        assertNotNull(seededCars);
        assertTrue(seededCars >= 2);
    }

    @Test
    void jpaRepository_worksWithSoftDelete() {
        CarJpaEntity car = new CarJpaEntity();
        car.setVin("VIN-IT-" + UUID.randomUUID());
        car.setCarModelId(SEED_MODEL_ID);
        car.setColor("White");
        car.setPrice(3_100_000);
        car.setAvailable(true);
        car.setAvailableForTestDrive(false);

        CarJpaEntity saved = carJpaRepository.save(car);
        assertTrue(carJpaRepository.findByIdAndRemovedFalse(saved.getId()).isPresent());

        saved.setRemoved(true);
        carJpaRepository.save(saved);
        assertFalse(carJpaRepository.findByIdAndRemovedFalse(saved.getId()).isPresent());
    }

    @Test
    void restApi_requiresAuthentication() throws Exception {
        mockMvc.perform(get("/api/parts"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void warehouseEndpoint_rejectsUserAndAllowsWarehouseAdmin() throws Exception {
        mockMvc.perform(get("/api/parts")
                        .with(jwt().jwt(jwt -> jwt.claim("app_user_id", UUID.randomUUID().toString()))
                                .authorities(new SimpleGrantedAuthority("ROLE_USER"))))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/parts")
                        .with(jwt().jwt(jwt -> jwt.claim("app_user_id", WAREHOUSE_ADMIN_USER_ID))
                                .authorities(new SimpleGrantedAuthority("ROLE_WAREHOUSE_ADMIN"))))
                .andExpect(status().isOk());
    }

    @Test
    void internalEndpoint_isAvailableWithoutAuthentication() throws Exception {
        String body = mockMvc.perform(get("/internal/cars/{id}", "50000000-0000-0000-0000-000000000001"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode json = objectMapper.readTree(body);
        assertEquals("50000000-0000-0000-0000-000000000001", json.get("id").asText());
    }
}
