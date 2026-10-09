package com.pgds.complaint;

import com.pgds.domain.Priority;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RuleBasedComplaintClassifierTest {

    private final RuleBasedComplaintClassifier c = new RuleBasedComplaintClassifier();

    @Test void overcharging() {
        var r = c.classify("The dealer demanded extra money for wheat");
        assertEquals("OVERCHARGING", r.category());
        assertEquals(Priority.HIGH, r.priority());
    }
    @Test void denial() { assertEquals("DENIAL_OF_RATION", c.classify("I was refused ration yesterday").category()); }
    @Test void shortWeight() { assertEquals("SHORT_WEIGHT", c.classify("Received less quantity than my entitlement").category()); }
    @Test void quality() { assertEquals("POOR_QUALITY", c.classify("Rice had insects and a bad smell").category()); }
    @Test void shopClosed() { assertEquals("SHOP_CLOSED", c.classify("The shop is closed on most working days").category()); }
    @Test void cardIssue() { assertEquals("CARD_ISSUE", c.classify("My son's name is missing from the card").category()); }
    @Test void fallsBackToOther() { assertEquals("OTHER", c.classify("Please improve the facilities nearby").category()); }
}
