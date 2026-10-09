package com.pgds.master;

import com.pgds.common.ApiException;
import com.pgds.domain.*;
import com.pgds.repo.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** Warehouse and Fair Price Shop master data. SUPER_ADMIN only (/api/admin/**). */
@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class MasterController {

    public record WarehouseRequest(@NotBlank String name, @NotBlank String district, String address) {}
    public record WarehouseView(Long id, String name, String district, String address) {}
    public record FpsRequest(@NotBlank String shopCode, @NotBlank String name, @NotBlank String district,
                             String address, @NotNull Long warehouseId) {}
    public record FpsView(Long id, String shopCode, String name, String district, String address, Long warehouseId) {}

    private final WarehouseRepository warehouses;
    private final FairPriceShopRepository shops;

    @PostMapping("/warehouses") @ResponseStatus(HttpStatus.CREATED)
    public WarehouseView createWarehouse(@Valid @RequestBody WarehouseRequest r) {
        return view(warehouses.save(Warehouse.builder().name(r.name()).district(r.district()).address(r.address()).build()));
    }

    @GetMapping("/warehouses")
    public List<WarehouseView> listWarehouses() { return warehouses.findAll().stream().map(MasterController::view).toList(); }

    @PostMapping("/fps") @ResponseStatus(HttpStatus.CREATED) @Transactional
    public FpsView createFps(@Valid @RequestBody FpsRequest r) {
        if (shops.existsByShopCode(r.shopCode()))
            throw new ApiException(HttpStatus.CONFLICT, "Shop code already exists");
        Warehouse wh = warehouses.findById(r.warehouseId())
                .orElseThrow(() -> new ApiException(HttpStatus.BAD_REQUEST, "Warehouse not found"));
        return view(shops.save(FairPriceShop.builder().shopCode(r.shopCode()).name(r.name())
                .district(r.district()).address(r.address()).warehouse(wh).build()));
    }

    @GetMapping("/fps") @Transactional(readOnly = true)
    public List<FpsView> listFps() { return shops.findAll().stream().map(MasterController::view).toList(); }

    public static WarehouseView view(Warehouse w) { return new WarehouseView(w.getId(), w.getName(), w.getDistrict(), w.getAddress()); }
    public static FpsView view(FairPriceShop s) {
        return new FpsView(s.getId(), s.getShopCode(), s.getName(), s.getDistrict(), s.getAddress(),
                s.getWarehouse() == null ? null : s.getWarehouse().getId());
    }
}
