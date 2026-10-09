package com.pgds.domain;

public enum StockTxnType {
    OPENING(1), RECEIPT(1), TRANSFER_IN(1), TRANSFER_OUT(-1), DISTRIBUTION(-1), DAMAGE(-1);

    private final int sign;
    StockTxnType(int sign) { this.sign = sign; }
    /** +1 adds to stock, -1 reduces stock. Balance = SUM(sign * quantity). */
    public int sign() { return sign; }
}
