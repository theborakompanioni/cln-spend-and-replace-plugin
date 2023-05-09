package org.tbk.cln.snr.exchange;

import static java.util.Objects.requireNonNull;

public final class OrderId {

    static OrderId of(String val) {
        return new OrderId(val);
    }

    private final String val;

    private OrderId(String val) {
        this.val = requireNonNull(val);
    }

    public String toString() {
        return val;
    }
}
