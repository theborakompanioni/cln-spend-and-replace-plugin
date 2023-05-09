package org.tbk.cln.snr.exchange;

import fr.acinq.bitcoin.Satoshi;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.knowm.xchange.Exchange;
import org.knowm.xchange.currency.CurrencyPair;
import org.knowm.xchange.dto.Order;
import org.knowm.xchange.dto.marketdata.Ticker;
import org.knowm.xchange.dto.meta.InstrumentMetaData;
import org.knowm.xchange.dto.trade.LimitOrder;
import org.knowm.xchange.dto.trade.MarketOrder;
import org.knowm.xchange.service.marketdata.params.CurrencyPairsParam;
import org.knowm.xchange.service.trade.TradeService;
import org.knowm.xchange.utils.OrderValuesHelper;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Optional;

@RequiredArgsConstructor
public final class ExchangeService {

    // Run in demo with massively undervalued order!
    // e.g. from the kraken docs:
    // "[...] we recommend placing very small market orders (orders for the minimum order size),
    // or limit orders that are priced far away from the current market price"
    private static final BigDecimal TEST_LIMIT_ORDER_PRICE_FACTOR = new BigDecimal("0.1");

    // most exchange settings have 0.00001 included (e.g. kraken.json in v5.1.0)
    private static final Satoshi FALLBACK_MIN_AMOUNT = new Satoshi(1_000L);

    private static void initExchangeIfNecessary(Exchange exchange) {
        try {
            boolean needsInit = exchange.getExchangeMetaData() == null || exchange.getExchangeMetaData().getInstruments() == null;
            if (needsInit) {
                exchange.remoteInit();
            }
        } catch (Exception e) {
            throw new IllegalStateException("Could not initialize exchange", e);
        }
    }

    @NonNull
    private final Exchange exchange;

    public MarketOrder.Builder createMarketOrder(CurrencyPair currencyPair, Satoshi amount) {
        initExchangeIfNecessary(exchange);

        InstrumentMetaData instrumentMetaData = exchange.getExchangeMetaData().getInstruments().get(currencyPair);
        OrderValuesHelper orderValuesHelper = new OrderValuesHelper(instrumentMetaData);

        BigDecimal bitcoinAmount = orderValuesHelper.adjustAmount(ConversionUtils.satsToBtc(amount));

        MarketOrder marketOrder = new MarketOrder.Builder(Order.OrderType.BID, currencyPair)
                .originalAmount(bitcoinAmount)
                .build();

        return MarketOrder.Builder.from(marketOrder);
    }

    public OrderId placeOrder(Order order) throws IOException {
        initExchangeIfNecessary(exchange);

        TradeService tradeService = exchange.getTradeService();
        if (order instanceof MarketOrder marketOrder) {
            return OrderId.of(tradeService.placeMarketOrder(marketOrder));
        } else if (order instanceof LimitOrder limitOrder) {
            return OrderId.of(tradeService.placeLimitOrder(limitOrder));
        } else {
            String errorMessage = String.format("Order type is not supported: %s",
                    order.getClass().getSimpleName());
            throw new IllegalStateException(errorMessage);
        }
    }

    public LimitOrder.Builder createTestLimitOrder(CurrencyPair currencyPair) {
        initExchangeIfNecessary(exchange);

        Satoshi amount = minimumOrderAmount(currencyPair);

        return createTestLimitOrder(currencyPair, amount);
    }

    public LimitOrder.Builder createTestLimitOrder(CurrencyPair currencyPair, Satoshi amount) {
        initExchangeIfNecessary(exchange);

        Ticker ticker = this.fetchTicker(currencyPair);
        BigDecimal buyingPrice = ticker.getBid().multiply(TEST_LIMIT_ORDER_PRICE_FACTOR);

        return createLimitOrder(currencyPair, amount, buyingPrice);
    }

    private LimitOrder.Builder createLimitOrder(CurrencyPair currencyPair, Satoshi amount, BigDecimal buyingPrice) {
        initExchangeIfNecessary(exchange);

        InstrumentMetaData instrumentMetaData = exchange.getExchangeMetaData().getInstruments().get(currencyPair);
        OrderValuesHelper orderValuesHelper = new OrderValuesHelper(instrumentMetaData);

        BigDecimal adjustedBuyingPrice = orderValuesHelper.adjustPrice(buyingPrice, RoundingMode.CEILING);

        return LimitOrder.Builder.from(createMarketOrder(currencyPair, amount).build())
                .limitPrice(adjustedBuyingPrice);
    }

    private Satoshi minimumOrderAmount(CurrencyPair currencyPair) {
        initExchangeIfNecessary(exchange);
        return Optional.ofNullable(exchange.getExchangeMetaData().getInstruments().get(currencyPair))
                .map(InstrumentMetaData::getMinimumAmount)
                .map(ConversionUtils::btcToSats)
                .orElse(FALLBACK_MIN_AMOUNT);
    }

    private Ticker fetchTicker(CurrencyPair currencyPair) {
        initExchangeIfNecessary(exchange);

        try {
            CurrencyPairsParam currencyPairsParam = () -> List.of(currencyPair);
            return exchange.getMarketDataService().getTickers(currencyPairsParam).stream()
                    .filter(it -> currencyPair.equals(it.getInstrument()))
                    .findFirst()
                    .orElseThrow(() -> new IllegalStateException("Could not find ticker data for currency pair."));
        } catch (Exception e) {
            String errorMessage = String.format("Could not fetch ticker from '%s' for currency pair '%s'.",
                    exchange.getExchangeSpecification().getExchangeName(), currencyPair);
            throw new IllegalStateException(errorMessage, e);
        }
    }
}
