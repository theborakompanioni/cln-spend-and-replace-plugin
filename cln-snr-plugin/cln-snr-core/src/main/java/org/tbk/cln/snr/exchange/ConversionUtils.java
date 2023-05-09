package org.tbk.cln.snr.exchange;

import fr.acinq.bitcoin.Satoshi;

import java.math.BigDecimal;
import java.math.RoundingMode;

public final class ConversionUtils {

    private ConversionUtils() {
        throw new UnsupportedOperationException();
    }

    private static final int BTC_FRACTION_DIGITS = 8;

    public static BigDecimal satsToBtc(Satoshi val) {
        return BigDecimal.valueOf(val.toLong())
                .movePointLeft(BTC_FRACTION_DIGITS)
                .setScale(BTC_FRACTION_DIGITS, RoundingMode.UNNECESSARY);
    }

    public static Satoshi btcToSats(BigDecimal val) {
        return new Satoshi(val
                .movePointRight(BTC_FRACTION_DIGITS)
                .setScale(0, RoundingMode.UNNECESSARY)
                .longValue());
    }
}
