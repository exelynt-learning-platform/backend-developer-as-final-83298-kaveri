package demo.Backend.Controller;

import demo.Backend.DTO.ApiResponse;
import demo.Backend.DTO.PageResponse;
import demo.Backend.DTO.ReservationRequest;
import demo.Backend.DTO.ReservationResponse;
import demo.Backend.DTO.ReservationSortField;
import demo.Backend.DTO.ReservationUpdateRequest;
import demo.Backend.DTO.SortDirection;
import demo.Backend.Entity.ReservationStatus;
import demo.Backend.Service.ReservationService;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;

@RestController
@RequestMapping("/reservations")
@Validated
@RequiredArgsConstructor
@Tag(name = "Reservations")
@SecurityRequirement(name = "bearerAuth")
public class ReservationController {

    private final ReservationService reservationService;

    @PostMapping
    public ResponseEntity<ApiResponse<ReservationResponse>> create(
            Authentication authentication,
            @Valid @RequestBody ReservationRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new ApiResponse<>("Reservation created successfully",
                        reservationService.create(authentication.getName(), request)));
    }

    @GetMapping
    public ApiResponse<PageResponse<ReservationResponse>> list(
            Authentication authentication,
            @Parameter(description = "Reservation status")
            @RequestParam(required = false) ReservationStatus status,
            @Parameter(description = "Lowest price to include")
            @RequestParam(required = false) BigDecimal minPrice,
            @Parameter(description = "Highest price to include")
            @RequestParam(required = false) BigDecimal maxPrice,
            @Parameter(description = "Page number, starting at 1")
            @RequestParam(defaultValue = "1") @Min(value = 1, message = "Page must be 1 or greater") int page,
            @Parameter(description = "Number of rows per page")
            @RequestParam(defaultValue = "10") @Min(value = 1, message = "Size must be at least 1")
            @Max(value = 100, message = "Size cannot be greater than 100") int size,
            @Parameter(description = "Column to sort by")
            @RequestParam(defaultValue = "id") ReservationSortField sortBy,
            @Parameter(description = "Sort direction")
            @RequestParam(defaultValue = "desc") SortDirection direction) {
        return new ApiResponse<>("Reservations fetched successfully", reservationService.list(
                authentication.getName(),
                isAdmin(authentication),
                status,
                minPrice,
                maxPrice,
                pageOf(page, size, sortBy.name(), direction)));
    }

    @GetMapping("/my")
    public ApiResponse<PageResponse<ReservationResponse>> myReservations(
            Authentication authentication,
            @Parameter(description = "Reservation status")
            @RequestParam(required = false) ReservationStatus status,
            @Parameter(description = "Lowest price to include")
            @RequestParam(required = false) BigDecimal minPrice,
            @Parameter(description = "Highest price to include")
            @RequestParam(required = false) BigDecimal maxPrice,
            @Parameter(description = "Page number, starting at 1")
            @RequestParam(defaultValue = "1") @Min(value = 1, message = "Page must be 1 or greater") int page,
            @Parameter(description = "Number of rows per page")
            @RequestParam(defaultValue = "10") @Min(value = 1, message = "Size must be at least 1")
            @Max(value = 100, message = "Size cannot be greater than 100") int size,
            @Parameter(description = "Column to sort by")
            @RequestParam(defaultValue = "id") ReservationSortField sortBy,
            @Parameter(description = "Sort direction")
            @RequestParam(defaultValue = "desc") SortDirection direction) {
        return new ApiResponse<>("Your reservations fetched successfully", reservationService.list(
                authentication.getName(),
                false,
                status,
                minPrice,
                maxPrice,
                pageOf(page, size, sortBy.name(), direction)));
    }

    @GetMapping("/{id}")
    public ApiResponse<ReservationResponse> get(@PathVariable Long id, Authentication authentication) {
        return new ApiResponse<>("Reservation fetched successfully",
                reservationService.get(id, authentication.getName(), isAdmin(authentication)));
    }

    @PutMapping("/{id}")
    public ApiResponse<ReservationResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody ReservationUpdateRequest request) {
        return new ApiResponse<>("Reservation updated successfully", reservationService.update(id, request));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        reservationService.delete(id);
        return new ApiResponse<>("Reservation deleted successfully", null);
    }

    private Pageable pageOf(int page, int size, String sortBy, SortDirection direction) {
        return PageRequest.of(page - 1, size, Sort.by(Sort.Direction.fromString(direction.name()), sortBy));
    }

    private boolean isAdmin(Authentication authentication) {
        return authentication.getAuthorities().stream()
                .anyMatch(authority -> authority.getAuthority().equals("ROLE_ADMIN"));
    }
}
