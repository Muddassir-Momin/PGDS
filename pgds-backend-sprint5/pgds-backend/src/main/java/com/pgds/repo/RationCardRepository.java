package com.pgds.repo;

import com.pgds.domain.RationCard;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface RationCardRepository extends JpaRepository<RationCard, Long> {
    Optional<RationCard> findByCardNumber(String cardNumber);
    boolean existsByCardNumber(String cardNumber);
    Page<RationCard> findByDistrictIgnoreCase(String district, Pageable pageable);

    /** Eligible cards and members per shop and category: the monthly demand each shop must serve. */
    @Query("""
        select new com.pgds.repo.ShopDemandRow(r.fps.id, r.category, count(r), sum(r.familyMembers))
        from RationCard r where r.eligible = true and r.fps is not null
        group by r.fps.id, r.category""")
    List<ShopDemandRow> shopDemand();
}
