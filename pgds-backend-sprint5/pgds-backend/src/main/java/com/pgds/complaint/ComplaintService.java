package com.pgds.complaint;

import com.pgds.common.ApiException;
import com.pgds.common.PageResponse;
import com.pgds.complaint.ComplaintClassifier.Classification;
import com.pgds.complaint.ComplaintDtos.*;
import com.pgds.domain.Complaint;
import com.pgds.domain.ComplaintStatus;
import com.pgds.repo.ComplaintRepository;
import com.pgds.repo.FairPriceShopRepository;
import com.pgds.repo.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class ComplaintService {

    private final ComplaintRepository complaints;
    private final UserRepository users;
    private final FairPriceShopRepository shops;
    private final ComplaintClassifier classifier;

    public ComplaintView submit(String username, ComplaintRequest r) {
        var user = users.findByUsername(username)
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "Unknown user"));
        var fps = r.fpsId() == null ? null : shops.findById(r.fpsId())
                .orElseThrow(() -> new ApiException(HttpStatus.BAD_REQUEST, "Fair Price Shop not found"));
        Classification c = classifier.classify(r.description());
        return view(complaints.save(Complaint.builder().submittedBy(user).fps(fps).description(r.description().trim())
                .category(c.category()).priority(c.priority()).department(c.department()).build()));
    }

    @Transactional(readOnly = true)
    public PageResponse<ComplaintView> mine(String username, Pageable p) {
        return PageResponse.of(complaints.findBySubmittedByUsername(username, p).map(this::view));
    }

    @Transactional(readOnly = true)
    public PageResponse<ComplaintView> list(ComplaintStatus status, Pageable p) {
        var page = status == null ? complaints.findAll(p) : complaints.findByStatus(status, p);
        return PageResponse.of(page.map(this::view));
    }

    /** Officer triage: officers can override the automatic category, priority and department. */
    public ComplaintView update(Long id, ComplaintUpdate u) {
        Complaint c = complaints.findById(id).orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Complaint not found"));
        c.setStatus(u.status());
        if (u.category() != null && !u.category().isBlank()) c.setCategory(u.category());
        if (u.priority() != null) c.setPriority(u.priority());
        if (u.department() != null && !u.department().isBlank()) c.setDepartment(u.department());
        if (u.resolutionNote() != null) c.setResolutionNote(u.resolutionNote());
        return view(c);
    }

    private ComplaintView view(Complaint c) {
        return new ComplaintView(c.getId(), c.getDescription(), c.getFps() == null ? null : c.getFps().getId(),
                c.getCategory(), c.getPriority(), c.getDepartment(), c.getStatus(), c.getResolutionNote(), c.getCreatedAt());
    }
}
