package com.pgds.repo;

import com.pgds.domain.Distribution;
import com.pgds.domain.GrainType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;

public interface DistributionRepository extends JpaRepository<Distribution, Long> {

    @Query("""
        select coalesce(sum(d.quantityKg), 0) from Distribution d
        where d.rationCard.id = :cardId and d.period = :period and d.grainType = :grain""")
    BigDecimal sumForCard(@Param("cardId") Long cardId, @Param("period") String period, @Param("grain") GrainType grain);

    boolean existsByReceiptNo(String receiptNo);

    List<Distribution> findTop200ByRationCardIdOrderByDistributedAtDesc(Long cardId);

    Page<Distribution> findByFpsIdOrderByDistributedAtDesc(Long fpsId, Pageable pageable);

    @Query("select coalesce(sum(d.quantityKg), 0) from Distribution d where d.period = :period")
    BigDecimal totalForPeriod(@Param("period") String period);

    @Query("""
        select new com.pgds.repo.PeriodGrainSum(d.period, d.grainType, sum(d.quantityKg))
        from Distribution d where d.period >= :fromPeriod group by d.period, d.grainType""")
    List<PeriodGrainSum> trendSince(@Param("fromPeriod") String fromPeriod);

    @Query("""
        select new com.pgds.repo.ShopSum(d.fps.id, sum(d.quantityKg))
        from Distribution d where d.period = :period group by d.fps.id""")
    List<ShopSum> distributedByShop(@Param("period") String period);

    @Query("""
        select d from Distribution d join fetch d.rationCard join fetch d.fps
        where d.distributedAt >= :since order by d.distributedAt""")
    List<Distribution> findSince(@Param("since") java.time.Instant since);

    /** Completed months only: fromPeriod <= period < currentPeriod. */
    @Query("""
        select new com.pgds.repo.ShopPeriodGrainSum(d.fps.id, d.period, d.grainType, sum(d.quantityKg))
        from Distribution d where d.period >= :fromPeriod and d.period < :currentPeriod
        group by d.fps.id, d.period, d.grainType""")
    List<ShopPeriodGrainSum> monthlyByShop(@Param("fromPeriod") String fromPeriod, @Param("currentPeriod") String currentPeriod);
}
