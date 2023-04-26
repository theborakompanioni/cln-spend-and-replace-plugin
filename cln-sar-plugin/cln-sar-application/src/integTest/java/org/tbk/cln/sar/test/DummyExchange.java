package org.tbk.cln.sar.test;

import org.knowm.xchange.BaseExchange;
import org.knowm.xchange.Exchange;
import org.knowm.xchange.ExchangeSpecification;
import org.knowm.xchange.currency.CurrencyPair;
import org.knowm.xchange.dto.marketdata.Ticker;
import org.knowm.xchange.instrument.Instrument;
import org.knowm.xchange.service.account.AccountService;
import org.knowm.xchange.service.marketdata.MarketDataService;
import org.knowm.xchange.service.marketdata.params.CurrencyPairsParam;
import org.knowm.xchange.service.marketdata.params.InstrumentsParams;
import org.knowm.xchange.service.marketdata.params.Params;
import org.knowm.xchange.service.trade.TradeService;
import si.mazi.rescu.LongValueFactory;
import si.mazi.rescu.SynchronizedValueFactory;

import java.math.BigDecimal;
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

    }

    public static class DummyAccountService implements AccountService {

    }
}