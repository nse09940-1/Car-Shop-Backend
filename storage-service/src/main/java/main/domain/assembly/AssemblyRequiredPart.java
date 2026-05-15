package main.domain.assembly;

import java.util.UUID;

public record AssemblyRequiredPart(
        UUID partId,
        int quantity) {
}
