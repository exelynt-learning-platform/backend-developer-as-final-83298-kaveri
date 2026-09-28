package demo.Backend.DTO;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ApiResponse<T> {

    private String message;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private T data;
}
