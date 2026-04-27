package main.presentation;

import main.application.dto.PartDto;
import main.application.service.PartService;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/parts")
public class PartController {
    private final PartService partService;

    public PartController(PartService partService) {
        this.partService = partService;
    }

    @PostMapping
    public PartDto create(@RequestBody PartDto request) {
        return partService.create(request);
    }

    @GetMapping("/{id}")
    public PartDto findById(@PathVariable UUID id) {
        return partService.findById(id);
    }

    @GetMapping
    public List<PartDto> findAll() {
        return partService.findAll();
    }

    @PutMapping("/{id}")
    public PartDto update(@PathVariable UUID id, @RequestBody PartDto request) {
        return partService.update(id, request);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable UUID id) {
        partService.delete(id);
    }
}

