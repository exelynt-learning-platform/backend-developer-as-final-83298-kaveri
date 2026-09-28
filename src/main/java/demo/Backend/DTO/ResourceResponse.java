package demo.Backend.DTO;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
public class ResourceResponse {
    private Long id;
    private String name;
    private String type;
    private String description;
    private BigDecimal price;
    private boolean available;
}
