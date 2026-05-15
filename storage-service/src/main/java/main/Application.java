package main;

import main.infrastructure.persistence.entity.AssemblyOrderJpaEntity;
import main.infrastructure.persistence.entity.AssemblyOrderRequiredPartJpaEntity;
import main.infrastructure.persistence.entity.BaseJpaEntity;
import main.infrastructure.persistence.entity.CarJpaEntity;
import main.infrastructure.persistence.entity.ProcessedEventJpaEntity;
import main.infrastructure.persistence.entity.OutboxEventJpaEntity;
import main.infrastructure.persistence.entity.PartJpaEntity;
import main.infrastructure.persistence.repository.AssemblyOrderJpaRepository;
import main.infrastructure.persistence.repository.AssemblyOrderRequiredPartJpaRepository;
import main.infrastructure.persistence.repository.CarJpaRepository;
import main.infrastructure.persistence.repository.ProcessedEventJpaRepository;
import main.infrastructure.persistence.repository.OutboxEventJpaRepository;
import main.infrastructure.persistence.repository.PartJpaRepository;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
@EntityScan(basePackageClasses = {
        BaseJpaEntity.class,
        CarJpaEntity.class,
        PartJpaEntity.class,
        AssemblyOrderJpaEntity.class,
        AssemblyOrderRequiredPartJpaEntity.class,
        OutboxEventJpaEntity.class,
        ProcessedEventJpaEntity.class
})
@EnableJpaRepositories(basePackageClasses = {
        CarJpaRepository.class,
        PartJpaRepository.class,
        AssemblyOrderJpaRepository.class,
        AssemblyOrderRequiredPartJpaRepository.class,
        OutboxEventJpaRepository.class,
        ProcessedEventJpaRepository.class
})
public class Application {

    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }
}
