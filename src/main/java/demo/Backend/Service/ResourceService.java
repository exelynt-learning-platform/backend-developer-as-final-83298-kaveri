package demo.Backend.Service;

import demo.Backend.DTO.PageResponse;
import demo.Backend.DTO.ResourceRequest;
import demo.Backend.DTO.ResourceResponse;
import demo.Backend.DTO.ResourceUpdateRequest;
import demo.Backend.Entity.ResourceItem;
import demo.Backend.Exception.ApiException;
import demo.Backend.Repository.ReservationRepo;
import demo.Backend.Repository.ResourceRepo;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ResourceService {

    private final ResourceRepo resourceRepo;
    private final ReservationRepo reservationRepo;

    @Transactional
    public ResourceResponse create(ResourceRequest request) {
        ResourceItem resource = new ResourceItem();
        apply(resource, request);
        return toResponse(resourceRepo.save(resource));
    }

    @Transactional(readOnly = true)
    public PageResponse<ResourceResponse> list(boolean adminOnlyAvailable, Pageable pageable) {
        Page<ResourceItem> page = adminOnlyAvailable
                ? resourceRepo.findAll(pageable)
                : resourceRepo.findAll((root, query, cb) -> cb.isTrue(root.get("available")), pageable);
        return toPage(page);
    }

    @Transactional(readOnly = true)
    public ResourceResponse get(Long id, boolean admin) {
        ResourceItem resource = find(id);
        if (!admin && !resource.isAvailable()) {
            throw new ApiException(HttpStatus.NOT_FOUND, "Resource not found");
        }
        return toResponse(resource);
    }

    @Transactional
    public ResourceResponse update(Long id, ResourceUpdateRequest request) {
        ResourceItem resource = find(id);
        applyChanges(resource, request);
        return toResponse(resourceRepo.save(resource));
    }

    @Transactional
    public void delete(Long id) {
        ResourceItem resource = find(id);
        if (reservationRepo.countByResourceId(id) > 0) {
            throw new ApiException(HttpStatus.CONFLICT, "Resource has reservations and cannot be deleted");
        }
        resourceRepo.delete(resource);
    }

    public ResourceItem find(Long id) {
        return resourceRepo.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Resource not found"));
    }

    private void apply(ResourceItem resource, ResourceRequest request) {
        resource.setName(request.getName().trim());
        resource.setType(request.getType().trim());
        resource.setDescription(request.getDescription());
        resource.setPrice(request.getPrice());
        resource.setAvailable(request.getAvailable() == null || request.getAvailable());
    }

    private void applyChanges(ResourceItem resource, ResourceUpdateRequest request) {
        if (request.getName() != null) {
            if (request.getName().isBlank()) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "Name cannot be empty");
            }
            resource.setName(request.getName().trim());
        }
        if (request.getType() != null) {
            if (request.getType().isBlank()) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "Type cannot be empty");
            }
            resource.setType(request.getType().trim());
        }
        if (request.getDescription() != null) {
            resource.setDescription(request.getDescription());
        }
        if (request.getPrice() != null) {
            resource.setPrice(request.getPrice());
        }
        if (request.getAvailable() != null) {
            resource.setAvailable(request.getAvailable());
        }
    }

    private PageResponse<ResourceResponse> toPage(Page<ResourceItem> page) {
        return new PageResponse<>(
                page.map(this::toResponse).getContent(),
                page.getNumber() + 1,
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages());
    }

    private ResourceResponse toResponse(ResourceItem resource) {
        return new ResourceResponse(
                resource.getId(),
                resource.getName(),
                resource.getType(),
                resource.getDescription(),
                resource.getPrice(),
                resource.isAvailable());
    }
}
