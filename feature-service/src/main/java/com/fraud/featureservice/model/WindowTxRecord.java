package com.fraud.featureservice.model;

import com.fraud.common.model.Location;

import java.math.BigDecimal;

public class WindowTxRecord extends com.fraud.common.model.WindowTxRecord {

    public WindowTxRecord() {
        super();
    }

    public WindowTxRecord(String transactionId, long timestampEpochMs, BigDecimal amount, Location location) {
        super(transactionId, timestampEpochMs, amount, location);
    }

    public WindowTxRecord(String transactionId, long timestampEpochMs, BigDecimal amount) {
        super(transactionId, timestampEpochMs, amount, null);
    }
}
