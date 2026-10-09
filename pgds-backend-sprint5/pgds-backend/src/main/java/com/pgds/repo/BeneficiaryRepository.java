package com.pgds.repo;

import com.pgds.domain.Beneficiary;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface BeneficiaryRepository extends JpaRepository<Beneficiary, Long> {
    boolean existsByAadhaarLast4AndNameIgnoreCase(String aadhaarLast4, String name);
    long countByRationCardId(Long rationCardId);
    List<Beneficiary> findByRationCardId(Long rationCardId);
    Optional<Beneficiary> findByUserUsername(String username);
    boolean existsByUserId(Long userId);

    @Query("""
        select new com.pgds.repo.ShopCount(r.fps.id, count(b))
        from Beneficiary b join b.rationCard r where r.fps is not null group by r.fps.id""")
    List<ShopCount> countByShop();
}
