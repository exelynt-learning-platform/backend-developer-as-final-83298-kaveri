package demo.Backend.Controller;

import demo.Backend.DTO.ApiResponse;
import demo.Backend.DTO.PageResponse;
import demo.Backend.DTO.ResourceRequest;
import demo.Backend.DTO.ResourceResponse;
import demo.Backend.DTO.ResourceSortField;
import demo.Backend.DTO.ResourceUpdateRequest;
import demo.Backend.DTO.SortDirection;
import demo.Backend.Service.ResourceService;
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

@RestController
@RequestMapping("/resources")
@Validated
@RequiredArgsConstructor
@Tag(name = "Resources")
@SecurityRequirement(name = "bearerAuth")
public class ResourceController {

    private final ResourceService resourceService;

    @GetMapping
    public ApiResponse<PageResponse<ResourceResponse>> list(
            Authentication authentication,
            @Parameter(description = "Page number, starting at 1")
            @RequestParam(defaultValue = "1") @Min(value = 1, message = "Page must be 1 or greater") int page,
            @Parameter(description = "Number of rows per page")
            @RequestParam(defaultValue = "10") @Min(value = 1, message = "Size must be at least 1")
            @Max(value = 100, message = "Size cannot be greater than 100") int size,
            @Parameter(description = "Column to sort by")
            @RequestParam(defaultValue = "id") ResourceSortField sortBy,
            @Parameter(description = "Sort direction")
            @RequestParam(defaultValue = "desc") SortDirection direction) {
        return new ApiResponse<>("Resources fetched successfully",
                resourceService.list(isAdmin(authentication), pageOf(page, size, sortBy.name(), direction)));
    }

    @GetMapping("/{id}")
    public ApiResponse<ResourceResponse> get(@PathVariable Long id, Authentication authentication) {
        return new ApiResponse<>("Resource fetched successfully",
                resourceService.get(id, isAdmin(authentication)));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<ResourceResponse>> create(@Valid @RequestBody ResourceRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new ApiResponse<>("Resource created successfully", resourceService.create(request)));
    }

    @PutMapping("/{id}")
    public ApiResponse<ResourceResponse> update(@PathVariable Long id, @Valid @RequestBody ResourceUpdateRequest request) {
        return new ApiResponse<>("Resource updated successfully", resourceService.update(id, request));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        resourceService.delete(id);
        return new ApiResponse<>("Resource deleted successfully", null);
    }

    private Pageable pageOf(int page, int size, String sortBy, SortDirection direction) {
        return PageRequest.of(page - 1, size, Sort.by(Sort.Direction.fromString(direction.name()), sortBy));
    }

    private boolean isAdmin(Authentication authentication) {
        return authentication.getAuthorities().stream()
                .anyMatch(authority -> authority.getAuthority().equals("ROLE_ADMIN"));
    }
}
