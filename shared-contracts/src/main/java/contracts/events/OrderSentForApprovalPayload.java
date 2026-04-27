package contracts.events;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;
import java.util.UUID;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record OrderSentForApprovalPayload(
        UUID carId,
        UUID carModelId,
        List<RequiredPartItem> requiredParts) {
}
