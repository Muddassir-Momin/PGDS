package com.pgds.complaint;

import com.pgds.domain.Priority;

/** Sprint 3 ships a rule-based implementation; Sprint 4 can add an ML implementation behind this interface. */
public interface ComplaintClassifier {
    record Classification(String category, Priority priority, String department) {}
    Classification classify(String text);
}
