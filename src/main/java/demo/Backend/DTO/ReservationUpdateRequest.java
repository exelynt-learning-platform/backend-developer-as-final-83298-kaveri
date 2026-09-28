package demo.Backend.DTO;

import demo.Backend.Entity.ReservationStatus;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class ReservationUpdateRequest {
    private Long resourceId;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private ReservationStatus status;

    @DecimalMin(value = "0.0", inclusive = true, message = "Price must be 0 or greater")
    @Digits(integer = 8, fraction = 2, message = "Price can have at most 8 digits and 2 decimal places")
    private BigDecimal price;
}
