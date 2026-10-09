package com.pgds.repo;

import com.pgds.domain.AiAlert;
import com.pgds.domain.AlertStatus;
import com.pgds.domain.Priority;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;

public interface AiAlertRepository extends JpaRepository<AiAlert, Long> {
    boolean existsByReferenceKey(String referenceKey);
    Page<AiAlert> findByStatus(AlertStatus status, Pageable pageable);
    long countByStatusIn(Collection<AlertStatus> statuses);
    long countBySeverityAndStatusIn(Priority severity, Collection<AlertStatus> statuses);
}
