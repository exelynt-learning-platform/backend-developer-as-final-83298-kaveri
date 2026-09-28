package demo.Backend.Repository;

import demo.Backend.Entity.ResourceItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface ResourceRepo extends JpaRepository<ResourceItem, Long>, JpaSpecificationExecutor<ResourceItem> {
}
