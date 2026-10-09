package com.pgds.repo;

import com.pgds.domain.FairPriceShop;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;

public interface FairPriceShopRepository extends JpaRepository<FairPriceShop, Long> {
    Optional<FairPriceShop> findByShopCode(String shopCode);
    boolean existsByShopCode(String shopCode);
    List<FairPriceShop> findByDistrictIgnoreCase(String district);
    List<FairPriceShop> findByWarehouseId(Long warehouseId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from FairPriceShop s where s.id = :id")
    Optional<FairPriceShop> findByIdForUpdate(@Param("id") Long id);
}
