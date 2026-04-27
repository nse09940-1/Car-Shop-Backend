package main;

import main.application.service.BaseConfigurationQueryService;
import main.application.service.CarCatalogService;
import main.application.service.ConfiguratorService;
import main.application.service.OrderService;
import main.application.service.SystemAdminService;
import main.application.service.TestDriveService;
import main.infrastructure.persistence.adapter.JpaCarModelRepositoryAdapter;
import main.infrastructure.persistence.adapter.JpaCustomOrderRepositoryAdapter;
import main.infrastructure.persistence.adapter.JpaInteriorOptionRepositoryAdapter;
import main.infrastructure.persistence.adapter.JpaSteeringOptionRepositoryAdapter;
import main.infrastructure.persistence.adapter.JpaStockOrderRepositoryAdapter;
import main.infrastructure.persistence.adapter.JpaTestDriveRepositoryAdapter;
import main.infrastructure.persistence.adapter.JpaTransmissionOptionRepositoryAdapter;
import main.infrastructure.persistence.adapter.JpaUserRepositoryAdapter;
import main.infrastructure.persistence.adapter.JpaWheelOptionRepositoryAdapter;
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
import main.infrastructure.security.OrderAccessEvaluator;
import main.presentation.AdminController;
import main.presentation.CatalogController;
import main.presentation.ConfiguratorController;
import main.presentation.OrderController;
import main.presentation.TestDriveController;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
@ComponentScan(
        excludeFilters = {
                @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = {
                        BaseConfigurationQueryService.class,
                        CarCatalogService.class,
                        ConfiguratorService.class,
                        OrderService.class,
                        SystemAdminService.class,
                        TestDriveService.class,
                        AdminController.class,
                        CatalogController.class,
                        ConfiguratorController.class,
                        OrderController.class,
                        TestDriveController.class,
                        JpaCarModelRepositoryAdapter.class,
                        JpaCustomOrderRepositoryAdapter.class,
                        JpaInteriorOptionRepositoryAdapter.class,
                        JpaSteeringOptionRepositoryAdapter.class,
                        JpaStockOrderRepositoryAdapter.class,
                        JpaTestDriveRepositoryAdapter.class,
                        JpaTransmissionOptionRepositoryAdapter.class,
                        JpaUserRepositoryAdapter.class,
                        JpaWheelOptionRepositoryAdapter.class,
                        OrderAccessEvaluator.class
                })
        })
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
