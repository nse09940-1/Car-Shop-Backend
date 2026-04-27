package main.application.service;

import main.application.dto.PartDto;
import main.application.mapper.AppMapper;
import main.application.port.repository.PartRepository;
import main.domain.exception.EntityNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Service
public class PartService {
    private final PartRepository partRepository;

    public PartService(PartRepository partRepository) {
        this.partRepository = Objects.requireNonNull(partRepository, "partRepository is required");
    }

    public PartDto create(PartDto request) {
        var part = AppMapper.toDomain(request);
        partRepository.save(part);
        return AppMapper.toDto(part);
    }

    public PartDto findById(UUID id) {
        var part = partRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Part not found"));
        return AppMapper.toDto(part);
    }

    public List<PartDto> findAll() {
        return partRepository.findAll().stream().map(AppMapper::toDto).toList();
    }

    public PartDto update(UUID id, PartDto request) {
        partRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Part not found"));
        var part = AppMapper.toDomain(id, request);
        partRepository.save(part);
        return AppMapper.toDto(part);
    }

    public void delete(UUID id) {
        partRepository.deleteById(id);
    }
}

