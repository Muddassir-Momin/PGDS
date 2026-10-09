package com.pgds.stock;

import com.pgds.common.ApiException;
import com.pgds.domain.*;
import com.pgds.repo.*;
import com.pgds.security.AccessGuard;
import com.pgds.stock.StockDtos.TransferRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StockServiceTest {

    @Mock WarehouseRepository warehouses;
    @Mock FairPriceShopRepository shops;
    @Mock StockTransactionRepository txns;
    @Mock AccessGuard access;

    StockService service;
    Warehouse wh;
    FairPriceShop fps;

    @BeforeEach
    void setUp() {
        service = new StockService(warehouses, shops, txns, access);
        wh = Warehouse.builder().id(1L).name("WH").district("Pune").build();
        fps = FairPriceShop.builder().id(10L).shopCode("FPS101").name("Shop").district("Pune").warehouse(wh).build();
        when(warehouses.findByIdForUpdate(1L)).thenReturn(Optional.of(wh));
        when(shops.findById(10L)).thenReturn(Optional.of(fps));
    }

    @Test
    void transferFailsWhenStockIsInsufficient() {
        when(txns.warehouseBalance(1L, GrainType.RICE)).thenReturn(new BigDecimal("50"));
        var req = new TransferRequest(10L, GrainType.RICE, new BigDecimal("100"), null);
        var ex = assertThrows(ApiException.class, () -> service.transfer("wm", 1L, req));
        assertTrue(ex.getMessage().startsWith("Insufficient stock"));
        verify(txns, never()).saveAll(any());
    }

    @Test
    void transferWritesOutAndInEntries() {
        when(txns.warehouseBalance(1L, GrainType.RICE)).thenReturn(new BigDecimal("500"));
        service.transfer("wm", 1L, new TransferRequest(10L, GrainType.RICE, new BigDecimal("100"), "Oct quota"));

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<StockTransaction>> cap = ArgumentCaptor.forClass(List.class);
        verify(txns).saveAll(cap.capture());
        var saved = cap.getValue();
        assertEquals(2, saved.size());
        assertEquals(StockTxnType.TRANSFER_OUT, saved.get(0).getTxnType());
        assertEquals(StockTxnType.TRANSFER_IN, saved.get(1).getTxnType());
        assertEquals(saved.get(0).getQuantityKg(), saved.get(1).getQuantityKg());
    }

    @Test
    void transferToShopServedByAnotherWarehouseIsRejected() {
        fps.setWarehouse(Warehouse.builder().id(2L).name("Other").district("Pune").build());
        var req = new TransferRequest(10L, GrainType.RICE, new BigDecimal("10"), null);
        assertThrows(ApiException.class, () -> service.transfer("wm", 1L, req));
        verify(txns, never()).saveAll(any());
    }
}
