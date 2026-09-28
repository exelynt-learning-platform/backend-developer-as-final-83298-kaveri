package demo.Backend.DTO;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class ResourceUpdateRequest {
    private String name;
    private String type;
    private String description;

    @DecimalMin(value = "0.0", inclusive = true, message = "Price must be 0 or greater")
    @Digits(integer = 8, fraction = 2, message = "Price can have at most 8 digits and 2 decimal places")
    private BigDecimal price;

    private Boolean available;
}
