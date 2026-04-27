package contracts.events;

import java.util.UUID;

public record RequiredPartItem(
        UUID partId,
        int quantity) {
}
