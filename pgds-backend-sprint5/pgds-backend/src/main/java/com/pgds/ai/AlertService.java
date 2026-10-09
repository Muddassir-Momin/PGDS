package com.pgds.ai;

import com.pgds.ai.AiDtos.*;
import com.pgds.common.ApiException;
import com.pgds.common.PageResponse;
import com.pgds.domain.*;
import com.pgds.repo.AiAlertRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class AlertService {

    private final AiAlertRepository alerts;

    /** Saves a new alert unless one with the same referenceKey exists. Returns true if created. */
    public boolean raise(String key, AlertType type, Priority severity, FairPriceShop fps,
                         String title, String details, Double score) {
        if (alerts.existsByReferenceKey(key)) return false;
        alerts.save(AiAlert.builder().referenceKey(key).type(type).severity(severity).fps(fps)
                .title(cut(title, 200)).details(cut(details, 1000)).score(score).build());
        return true;
    }

    @Transactional(readOnly = true)
    public PageResponse<AlertView> list(AlertStatus status, Pageable p) {
        var page = status == null ? alerts.findAll(p) : alerts.findByStatus(status, p);
        return PageResponse.of(page.map(this::view));
    }

    public AlertView update(Long id, String username, AlertUpdate u) {
        AiAlert a = alerts.findById(id).orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Alert not found"));
        a.setStatus(u.status());
        a.setReviewedBy(username);
        if (u.note() != null) a.setReviewNote(u.note());
        return view(a);
    }

    private AlertView view(AiAlert a) {
        return new AlertView(a.getId(), a.getType(), a.getSeverity(), a.getStatus(),
                a.getFps() == null ? null : a.getFps().getId(), a.getFps() == null ? null : a.getFps().getShopCode(),
                a.getTitle(), a.getDetails(), a.getScore(), a.getDetectedAt(), a.getReviewedBy(), a.getReviewNote());
    }

    private static String cut(String s, int max) { return s == null || s.length() <= max ? s : s.substring(0, max); }
}
