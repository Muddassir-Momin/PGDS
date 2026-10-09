package com.pgds.repo;

import com.pgds.domain.Warehouse;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.Optional;

public interface WarehouseRepository extends JpaRepository<Warehouse, Long> {
    /** Row lock so concurrent outflows cannot both pass the balance check. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select w from Warehouse w where w.id = :id")
    Optional<Warehouse> findByIdForUpdate(@Param("id") Long id);
}
