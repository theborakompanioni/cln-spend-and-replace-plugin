package org.tbk.cln.snr.exchange;

import fr.acinq.bitcoin.Satoshi;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.tbk.cln.snr.exchange.ConversionUtils.btcToSats;
import static org.tbk.cln.snr.exchange.ConversionUtils.satsToBtc;

class ConversionUtilsTest {

    @Test
    void testSatsToBtc() {
        assertThat(satsToBtc(new Satoshi(-1)), is(new BigDecimal("-0.00000001")));
        assertThat(satsToBtc(new Satoshi(0)), is(new BigDecimal("0.00000000")));
        assertThat(satsToBtc(new Satoshi(1)), is(new BigDecimal("0.00000001")));
        assertThat(satsToBtc(new Satoshi(1_000)), is(new BigDecimal("0.00001000")));
        assertThat(satsToBtc(new Satoshi(999_999_999L)), is(new BigDecimal("9.99999999")));
        assertThat(satsToBtc(new Satoshi(4_213_370_000L)), is(new BigDecimal("42.13370000")));
        assertThat(satsToBtc(new Satoshi(2_100_000_000_000_000L)), is(new BigDecimal("21000000.00000000")));
    }

    @Test
    void testBtcToSats() {
        assertThat(btcToSats(new BigDecimal("-0.00000001")), is(new Satoshi(-1)));
        assertThat(btcToSats(new BigDecimal("0")), is(new Satoshi(0)));
        assertThat(btcToSats(new BigDecimal("0.00000000")), is(new Satoshi(0)));
        assertThat(btcToSats(new BigDecimal("0.00000001")), is(new Satoshi(1)));
        assertThat(btcToSats(new BigDecimal("0.00001000")), is(new Satoshi(1_000)));
        assertThat(btcToSats(new BigDecimal("9.99999999")), is(new Satoshi(999_999_999L)));
        assertThat(btcToSats(new BigDecimal("1")), is(new Satoshi(100_000_000L)));
        assertThat(btcToSats(new BigDecimal("42.1337")), is(new Satoshi(4_213_370_000L)));
        assertThat(btcToSats(new BigDecimal("21000000")), is(new Satoshi(2_100_000_000_000_000L)));
        assertThat(btcToSats(new BigDecimal("21000000.00000000")), is(new Satoshi(2_100_000_000_000_000L)));
    }

    @Test
    void testBtcToSatsInvalid() {
        Assertions.assertThrowsExactly(
                ArithmeticException.class,
                () -> btcToSats(new BigDecimal("0.000000000001")),
                "Rounding necessary"
        );
    }
}