package com.pgds.complaint;

import com.pgds.domain.Priority;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Locale;

@Component
public class RuleBasedComplaintClassifier implements ComplaintClassifier {

    private record Rule(String category, Priority priority, String department, List<String> keywords) {}

    // Order matters: first match wins; the generic CARD_ISSUE rule is last.
    private static final List<Rule> RULES = List.of(
        new Rule("OVERCHARGING", Priority.HIGH, "District Supply Office",
                List.of("overcharg", "extra money", "extra charge", "bribe", "more money", "asking money", "demanded money")),
        new Rule("DENIAL_OF_RATION", Priority.HIGH, "District Supply Office",
                List.of("denied", "refused", "refuse", "not given", "did not give", "didn't give", "no ration")),
        new Rule("SHORT_WEIGHT", Priority.HIGH, "Legal Metrology Department",
                List.of("less quantity", "short weight", "underweight", "less weight", "less grain", "less rice", "less wheat", "weighing")),
        new Rule("POOR_QUALITY", Priority.MEDIUM, "Quality Control Cell",
                List.of("quality", "rotten", "insect", "stones", "dirty", "mould", "mold", "smell", "spoiled")),
        new Rule("SHOP_CLOSED", Priority.MEDIUM, "Shop Licensing Cell",
                List.of("shop closed", "shop is closed", "not open", "never open", "opens late", "irregular timing")),
        new Rule("CARD_ISSUE", Priority.LOW, "Card Section",
                List.of("card", "name missing", "aadhaar", "aadhar", "new member", "correction"))
    );

    @Override
    public Classification classify(String text) {
        String t = text == null ? "" : text.toLowerCase(Locale.ROOT);
        for (Rule r : RULES)
            for (String k : r.keywords())
                if (t.contains(k)) return new Classification(r.category(), r.priority(), r.department());
        return new Classification("OTHER", Priority.LOW, "District Supply Office");
    }

    /** Priority and department for a category (used by the ML classifier too). */
    public static Classification forCategory(String category) {
        for (Rule r : RULES)
            if (r.category().equals(category)) return new Classification(r.category(), r.priority(), r.department());
        return new Classification("OTHER", Priority.LOW, "District Supply Office");
    }
}
