package com.pgds.complaint;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class WekaComplaintClassifierTest {

    private static WekaComplaintClassifier classifier;

    @BeforeAll
    static void train() {
        classifier = new WekaComplaintClassifier(new RuleBasedComplaintClassifier());
        classifier.train();
    }

    @Test
    void modelTrainsOnBundledData() {
        var info = classifier.info();
        assertTrue(info.trained());
        assertEquals(7, info.categories().size());
        assertTrue(info.trainingSamples() >= 60);
    }

    @Test
    void classifiesFamiliarComplaints() {
        assertEquals("POOR_QUALITY", classifier.classify("The rice had insects and a bad smell").category());
        assertEquals("SHOP_CLOSED", classifier.classify("The shop was locked when I came").category());
    }

    @Test
    void categoryDeterminesPriorityAndDepartment() {
        var r = classifier.classify("Dealer is charging extra money for wheat");
        assertEquals("OVERCHARGING", r.category());
        assertEquals(com.pgds.domain.Priority.HIGH, r.priority());
        assertEquals("District Supply Office", r.department());
    }

    @Test
    void blankTextFallsBackWithoutError() {
        assertEquals("OTHER", classifier.classify("").category());
    }
}
