package org.example;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import main.Application;
import main.infrastructure.persistence.entity.CarJpaEntity;
import main.infrastructure.persistence.repository.CarJpaRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(classes = Application.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
class PostgresIT {

    private static final UUID SEED_MODEL_ID = UUID.fromString("10000000-0000-0000-0000-000000000001");

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("testdb")
            .withUsername("test")
            .withPassword("test");

    @DynamicPropertySource
    static void configureDatasource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.datasource.driver-class-name", postgres::getDriverClassName);
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "validate");
        registry.add("spring.liquibase.change-log", () -> "classpath:db/changelog/db.changelog-master.yaml");
    }

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private CarJpaRepository carJpaRepository;

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private ObjectMapper objectMapper;

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
    void restApi_returnsFilteredCars() throws Exception {
        ResponseEntity<String> response = restTemplate.getForEntity("/api/cars?brand=BMW", String.class);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());

        JsonNode json = objectMapper.readTree(response.getBody());
        assertTrue(json.isArray());
        assertTrue(json.size() > 0);
        for (JsonNode item : json) {
            assertEquals("BMW", item.get("brand").asText());
        }
    }
}
