package com.pgds.seed;

import com.pgds.domain.*;
import com.pgds.repo.*;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/** Seeds one account per role + sample master data. Disable with SEED_DEMO=false. */
@Component
@RequiredArgsConstructor
public class DemoDataSeeder implements CommandLineRunner {

    private static final String DEMO_PASSWORD = "Pgds@12345";

    private final UserRepository users;
    private final WarehouseRepository warehouses;
    private final FairPriceShopRepository shops;
    private final RationCardRepository cards;
    private final BeneficiaryRepository beneficiaries;
    private final StockTransactionRepository stock;
    private final PasswordEncoder encoder;
    @Value("${pgds.seed-demo-data}") private boolean enabled;

    @Override
    public void run(String... args) {
        if (!enabled || users.count() > 0) return;

        var wh = warehouses.save(Warehouse.builder().name("Pune Central Godown").district("Pune").address("Hadapsar").build());
        var fps = shops.save(FairPriceShop.builder().shopCode("FPS101").name("Ganesh Ration Shop")
                .district("Pune").address("Shivajinagar").warehouse(wh).build());

        user("admin", "Super Admin", Role.SUPER_ADMIN, null, null);
        user("officer", "Manasi (District Officer)", Role.GOVT_OFFICER, null, null);
        user("warehouse", "Warehouse Manager", Role.WAREHOUSE_MANAGER, wh, null);
        user("dealer", "FPS Dealer", Role.FPS_DEALER, null, fps);
        user("auditor", "Auditor", Role.AUDITOR, null, null);
        var benUser = user("beneficiary", "Ramesh Patil", Role.BENEFICIARY, null, null);

        var card = cards.save(RationCard.builder().cardNumber("MH-PUN-000001").headOfFamily("Ramesh Patil")
                .address("Shivajinagar, Pune").district("Pune").category(CardCategory.PHH)
                .familyMembers(4).eligible(true).fps(fps).build());
        beneficiaries.save(Beneficiary.builder().name("Ramesh Patil").age(42).aadhaarLast4("1234")
                .rationCard(card).user(benUser).build());

        stock.save(StockTransaction.builder().locationType(LocationType.WAREHOUSE).warehouse(wh)
                .grainType(GrainType.RICE).txnType(StockTxnType.OPENING)
                .quantityKg(new BigDecimal("10000")).remarks("Opening stock").createdBy("seed").build());
        stock.save(StockTransaction.builder().locationType(LocationType.WAREHOUSE).warehouse(wh)
                .grainType(GrainType.WHEAT).txnType(StockTxnType.OPENING)
                .quantityKg(new BigDecimal("6000")).remarks("Opening stock").createdBy("seed").build());

        // Demo shop stock (warehouse -> FPS transfers) so Sprint 3 distribution has stock to issue
        transfer(wh, fps, GrainType.RICE, "1000");
        transfer(wh, fps, GrainType.WHEAT, "600");
    }

    private void transfer(Warehouse wh, FairPriceShop fps, GrainType g, String qty) {
        BigDecimal q = new BigDecimal(qty);
        stock.save(StockTransaction.builder().locationType(LocationType.WAREHOUSE).warehouse(wh).grainType(g)
                .txnType(StockTxnType.TRANSFER_OUT).quantityKg(q).remarks("To " + fps.getShopCode()).createdBy("seed").build());
        stock.save(StockTransaction.builder().locationType(LocationType.FPS).fps(fps).grainType(g)
                .txnType(StockTxnType.TRANSFER_IN).quantityKg(q).remarks("From warehouse " + wh.getId()).createdBy("seed").build());
    }

    private AppUser user(String username, String name, Role role, Warehouse wh, FairPriceShop fps) {
        return users.save(AppUser.builder().username(username).passwordHash(encoder.encode(DEMO_PASSWORD))
                .fullName(name).email(username + "@pgds.local").role(role).warehouse(wh).fps(fps).build());
    }
}
