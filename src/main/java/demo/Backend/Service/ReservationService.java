package demo.Backend.Service;

import demo.Backend.DTO.PageResponse;
import demo.Backend.DTO.ReservationRequest;
import demo.Backend.DTO.ReservationResponse;
import demo.Backend.DTO.ReservationUpdateRequest;
import demo.Backend.Entity.Reservation;
import demo.Backend.Entity.ReservationStatus;
import demo.Backend.Entity.ResourceItem;
import demo.Backend.Entity.User;
import demo.Backend.Exception.ApiException;
import demo.Backend.Repository.ReservationRepo;
import demo.Backend.Repository.UserRepo;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class ReservationService {

    private static final Set<String> SORT_FIELDS = Set.of("id", "price", "status", "startTime", "endTime");

    private final ReservationRepo reservationRepo;
    private final ResourceService resourceService;
    private final UserRepo userRepo;

    @Transactional
    public ReservationResponse create(String username, ReservationRequest request) {
        validateTimes(request.getStartTime(), request.getEndTime());
        ResourceItem resource = resourceService.find(request.getResourceId());
        if (!resource.isAvailable()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Resource is not available");
        }
        requireMatchingPrice(request.getPrice(), resource);
        ensureNoOverlap(resource.getId(), request.getStartTime(), request.getEndTime(), null);

        Reservation reservation = new Reservation();
        reservation.setResource(resource);
        reservation.setUser(currentUser(username));
        reservation.setStartTime(request.getStartTime());
        reservation.setEndTime(request.getEndTime());
        reservation.setPrice(request.getPrice());
        reservation.setStatus(ReservationStatus.PENDING);
        return toResponse(reservationRepo.save(reservation));
    }

    @Transactional(readOnly = true)
    public PageResponse<ReservationResponse> list(
            String username,
            boolean admin,
            ReservationStatus status,
            BigDecimal minPrice,
            BigDecimal maxPrice,
            Pageable pageable) {
        if (minPrice != null && maxPrice != null && minPrice.compareTo(maxPrice) > 0) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Minimum price cannot be greater than maximum price");
        }
        String owner = admin ? null : username;
        Page<Reservation> page = reservationRepo.findAll(filter(owner, status, minPrice, maxPrice), sanitize(pageable));
        return new PageResponse<>(
                page.map(this::toResponse).getContent(),
                page.getNumber() + 1,
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages());
    }

    @Transactional(readOnly = true)
    public ReservationResponse get(Long id, String username, boolean admin) {
        return toResponse(findOwned(id, username, admin));
    }

    @Transactional
    public ReservationResponse update(Long id, ReservationUpdateRequest request) {
        Reservation reservation = findOwned(id, null, true);
        Long resourceId = request.getResourceId() == null ? reservation.getResource().getId() : request.getResourceId();
        LocalDateTime start = request.getStartTime() == null ? reservation.getStartTime() : request.getStartTime();
        LocalDateTime end = request.getEndTime() == null ? reservation.getEndTime() : request.getEndTime();
        validateTimes(start, end);
        ResourceItem resource = resourceService.find(resourceId);
        BigDecimal price = request.getPrice() == null ? reservation.getPrice() : request.getPrice();
        if (request.getPrice() != null || request.getResourceId() != null) {
            requireMatchingPrice(price, resource);
        }
        ensureNoOverlap(resource.getId(), start, end, reservation.getId());

        reservation.setResource(resource);
        reservation.setStartTime(start);
        reservation.setEndTime(end);
        if (request.getPrice() != null) {
            reservation.setPrice(request.getPrice());
        }
        if (request.getStatus() != null) {
            reservation.setStatus(request.getStatus());
        }
        return toResponse(reservationRepo.save(reservation));
    }

    @Transactional
    public void delete(Long id) {
        Reservation reservation = findOwned(id, null, true);
        reservationRepo.delete(reservation);
    }

    private Reservation findOwned(Long id, String username, boolean admin) {
        Reservation reservation = reservationRepo.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Reservation not found"));
        if (!admin && !reservation.getUser().getUsername().equals(username)) {
            throw new ApiException(HttpStatus.FORBIDDEN, "You can only access your own reservations");
        }
        return reservation;
    }

    private User currentUser(String username) {
        return userRepo.findByUsername(username)
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "User not found"));
    }

    private void validateTimes(LocalDateTime start, LocalDateTime end) {
        if (start == null || end == null || !end.isAfter(start)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "End time must be after start time");
        }
        if (start.isBefore(LocalDateTime.now())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Start time must not be in the past");
        }
    }

    private void requireMatchingPrice(BigDecimal price, ResourceItem resource) {
        if (price.compareTo(resource.getPrice()) != 0) {
            throw new ApiException(HttpStatus.BAD_REQUEST,
                    "Price must match the resource price of " + resource.getPrice().toPlainString());
        }
    }

    private void ensureNoOverlap(Long resourceId, LocalDateTime start, LocalDateTime end, Long excludeId) {
        if (reservationRepo.existsOverlap(resourceId, start, end, excludeId)) {
            throw new ApiException(HttpStatus.CONFLICT, "Resource is already reserved for that time");
        }
    }

    private Pageable sanitize(Pageable pageable) {
        int size = Math.min(Math.max(pageable.getPageSize(), 1), 100);
        Sort sort = pageable.getSort();
        if (sort.isUnsorted()) {
            sort = Sort.by(Sort.Direction.DESC, "id");
        } else {
            sort = Sort.by(sort.stream().map(order -> {
                if (!SORT_FIELDS.contains(order.getProperty())) {
                    throw new ApiException(HttpStatus.BAD_REQUEST,
                            "Cannot sort by " + order.getProperty() + ". Use id, price, status, startTime, or endTime");
                }
                return order;
            }).toList());
        }
        return PageRequest.of(pageable.getPageNumber(), size, sort);
    }

    private Specification<Reservation> filter(
            String username,
            ReservationStatus status,
            BigDecimal minPrice,
            BigDecimal maxPrice) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (username != null) {
                predicates.add(cb.equal(root.get("user").get("username"), username));
            }
            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }
            if (minPrice != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("price"), minPrice));
            }
            if (maxPrice != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("price"), maxPrice));
            }
            if (predicates.isEmpty()) {
                return cb.conjunction();
            }
            return cb.and(predicates.toArray(Predicate[]::new));
        };
    }

    private ReservationResponse toResponse(Reservation reservation) {
        return new ReservationResponse(
                reservation.getId(),
                reservation.getResource().getId(),
                reservation.getResource().getName(),
                reservation.getUser().getId(),
                reservation.getUser().getUsername(),
                reservation.getStartTime(),
                reservation.getEndTime(),
                reservation.getStatus(),
                reservation.getPrice());
    }
}
