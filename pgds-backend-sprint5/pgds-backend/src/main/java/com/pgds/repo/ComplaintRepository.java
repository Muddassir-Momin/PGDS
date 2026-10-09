package com.pgds.repo;

import com.pgds.domain.Complaint;
import com.pgds.domain.ComplaintStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ComplaintRepository extends JpaRepository<Complaint, Long> {
    Page<Complaint> findByStatus(ComplaintStatus status, Pageable pageable);
    Page<Complaint> findBySubmittedByUsername(String username, Pageable pageable);
    long countByStatus(ComplaintStatus status);
}
