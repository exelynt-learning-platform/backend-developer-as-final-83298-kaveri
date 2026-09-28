package demo.Backend.Repository;

import demo.Backend.Entity.Reservation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;

public interface ReservationRepo extends JpaRepository<Reservation, Long>, JpaSpecificationExecutor<Reservation> {

    long countByResourceId(Long resourceId);

    @Query("""
            SELECT CASE WHEN COUNT(r) > 0 THEN true ELSE false END
            FROM Reservation r
            WHERE r.resource.id = :resourceId
              AND r.status <> demo.Backend.Entity.ReservationStatus.CANCELLED
              AND (:excludeId IS NULL OR r.id <> :excludeId)
              AND r.startTime < :endTime
              AND r.endTime > :startTime
            """)
    boolean existsOverlap(
            @Param("resourceId") Long resourceId,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime,
            @Param("excludeId") Long excludeId);
}
