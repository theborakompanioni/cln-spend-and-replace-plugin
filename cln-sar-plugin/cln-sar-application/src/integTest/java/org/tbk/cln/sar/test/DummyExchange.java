package org.tbk.cln.sar.test;

import org.knowm.xchange.BaseExchange;
import org.knowm.xchange.Exchange;
import org.knowm.xchange.ExchangeSpecification;
import org.knowm.xchange.currency.Currency;
import org.knowm.xchange.currency.CurrencyPair;
import org.knowm.xchange.dto.Order;
import org.knowm.xchange.dto.account.AccountInfo;
import org.knowm.xchange.dto.account.Balance;
import org.knowm.xchange.dto.account.Wallet;
import org.knowm.xchange.dto.marketdata.Ticker;
import org.knowm.xchange.dto.marketdata.Trades;
import org.knowm.xchange.dto.trade.LimitOrder;
import org.knowm.xchange.dto.trade.OpenOrders;
import org.knowm.xchange.dto.trade.UserTrade;
import org.knowm.xchange.dto.trade.UserTrades;
import org.knowm.xchange.instrument.Instrument;
import org.knowm.xchange.service.account.AccountService;
import org.knowm.xchange.service.marketdata.MarketDataService;
import org.knowm.xchange.service.marketdata.params.CurrencyPairsParam;
import org.knowm.xchange.service.marketdata.params.InstrumentsParams;
import org.knowm.xchange.service.marketdata.params.Params;
import org.knowm.xchange.service.trade.TradeService;
import org.knowm.xchange.service.trade.params.TradeHistoryParams;
import org.knowm.xchange.service.trade.params.orders.OpenOrdersParams;
import org.testcontainers.shaded.com.google.common.collect.ImmutableList;
import si.mazi.rescu.LongValueFactory;
import si.mazi.rescu.SynchronizedValueFactory;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

public class DummyExchange extends BaseExchange implements Exchange {
    private final SynchronizedValueFactory<Long> nonceFactory = new LongValueFactory();

    @Override
    protected void initServices() {
        this.marketDataService = new DummyMarketDataService();
        this.tradeService = new DummyTradeService();
        this.accountService = new DummyAccountService();
    }

    @Override
    public SynchronizedValueFactory<Long> getNonceFactory() {
        return nonceFactory;
    }

    @Override
    public ExchangeSpecification getDefaultExchangeSpecification() {
        ExchangeSpecification exchangeSpecification = new ExchangeSpecification(this.getClass());
        exchangeSpecification.setSslUri("https://www.example.com");
        exchangeSpecification.setHost("www.example.com");
        exchangeSpecification.setPort(80);
        exchangeSpecification.setExchangeName("Dummy");
        exchangeSpecification.setExchangeDescription("Dummy is an exchange that should only be used in tests");
        exchangeSpecification.setShouldLoadRemoteMetaData(false);
        return exchangeSpecification;
    }

    public static class DummyMarketDataService implements MarketDataService {
        @Override
        public Ticker getTicker(CurrencyPair currencyPair, Object... args) {
            return this.getTicker((Instrument) currencyPair);
        }

        @Override
        public Ticker getTicker(Instrument currencyPair, Object... args) {
            return new Ticker.Builder()
                    .instrument(currencyPair)
                    .ask(new BigDecimal("0.12"))
                    .askSize(new BigDecimal("0.13"))
                    .bid(new BigDecimal("0.14"))
                    .bidSize(new BigDecimal("0.14"))
                    .high(new BigDecimal("0.15"))
                    .last(new BigDecimal("0.16"))
                    .low(new BigDecimal("0.17"))
                    .open(new BigDecimal("0.18"))
                    .quoteVolume(new BigDecimal("0.19"))
                    .timestamp(new Date())
                    .volume(new BigDecimal("100000"))
                    .vwap(new BigDecimal("0.2"))
                    .build();
        }

        @Override
        public List<Ticker> getTickers(Params params) {
            Collection<? extends Instrument> instruments =
                    params instanceof InstrumentsParams ? ((InstrumentsParams) params).getInstruments() :
                            params instanceof CurrencyPairsParam ? ((CurrencyPairsParam) params).getCurrencyPairs() :
                                    Collections.emptyList();
            return instruments.stream()
                    .map(this::getTicker)
                    .collect(Collectors.toList());
        }
    }

    public static class DummyTradeService implements TradeService {
        /**
         * "abcdef-00000-000001" : {
         * "id" : "abcdef-00000-000001",
         * "type" : "BID",
         * "status" : "NEW",
         * "is-open" : true,
         * "is-final" : false,
         * "original-amount" : "0.42",
         * "remaining-amount" : "0.42",
         * "limit-price" : "21.0",
         * "asset-pair" : "BTC/USD",
         * "ref" : "0",
         * "date" : "2023-05-22T10:05:22.042Z",
         * "timestamp" : 1622000000
         * }
         */
        @Override
        public OpenOrders getOpenOrders(OpenOrdersParams params) {
            return new OpenOrders(ImmutableList.<LimitOrder>builder()
                    .add(new LimitOrder.Builder(Order.OrderType.BID, CurrencyPair.BTC_USD)
                            .id("abcdef-00000-000001")
                            .orderStatus(Order.OrderStatus.NEW)
                            .originalAmount(BigDecimal.valueOf(0.42d))
                            .remainingAmount(BigDecimal.valueOf(0.42d))
                            .limitPrice(BigDecimal.valueOf(21.0))
                            .userReference("0")
                            .timestamp(Date.from(Instant.ofEpochSecond(1622000000)))
                            .build())
                    .build());
        }

        /**
         * "abcdef-00000-000000" : {
         * "id" : "abcdef-00000-000000",
         * "type" : "BID",
         * "order-id" : "abcdef",
         * "price" : "21000.00000",
         * "asset-pair" : "BTC/USD",
         * "original-amount" : "0.21",
         * "ref" : "",
         * "fee-amount" : "0.090103",
         * "fee-currency" : "USD",
         * "date" : "2023-01-03T09:01:03.042Z",
         * "timestamp" : 1621000000
         * }
         */
        @Override
        public UserTrades getTradeHistory(TradeHistoryParams params) {
            return new UserTrades(
                    ImmutableList.<UserTrade>builder()
                            .add(new UserTrade.Builder()
                                    .id("abcdef-00000-000000")
                                    .type(Order.OrderType.BID)
                                    .orderId("abcdef")
                                    .price(BigDecimal.valueOf(21000.00000d))
                                    .originalAmount(BigDecimal.valueOf(0.21d))
                                    .instrument(CurrencyPair.BTC_USD)
                                    .feeAmount(BigDecimal.valueOf(0.090103d))
                                    .feeCurrency(Currency.USD)
                                    .orderUserReference("")
                                    .timestamp(Date.from(Instant.ofEpochSecond(1621000000)))
                                    .build())
                            .build(),
                    Trades.TradeSortType.SortByTimestamp
            );
        }
    }

    public static class DummyAccountService implements AccountService {
        @Override
        public AccountInfo getAccountInfo() {
            // return a "kraken" like account info object
            return new AccountInfo(ImmutableList.<Wallet>builder()
                    .add(Wallet.Builder.from(ImmutableList.<Balance>builder()
                                    .add(Balance.Builder.from(Balance.zero(Currency.BTC))
                                            .total(BigDecimal.ONE.movePointLeft(10))
                                            .build())
                                    .add(Balance.Builder.from(Balance.zero(Currency.USD))
                                            .total(BigDecimal.ONE.movePointLeft(4))
                                            .build())
                                    .build())
                            .id(null)
                            .name(null)
                            .build())
                    .add(Wallet.Builder.from(ImmutableList.<Balance>builder()
                                    .add(Balance.Builder.from(Balance.zero(Currency.BTC))
                                            .total(BigDecimal.ONE.movePointLeft(10))
                                            .build())
                                    .add(Balance.Builder.from(Balance.zero(Currency.USD))
                                            .total(BigDecimal.ONE.movePointLeft(4))
                                            .build())
                                    .build())
                            .id("margin")
                            .name("margin")
                            .build())
                    .build());
        }
    }
}