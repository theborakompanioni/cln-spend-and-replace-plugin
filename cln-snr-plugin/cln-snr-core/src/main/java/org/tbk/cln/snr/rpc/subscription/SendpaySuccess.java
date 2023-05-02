package org.tbk.cln.snr.rpc.subscription;

import com.google.gson.JsonObject;
import fr.acinq.bitcoin.Satoshi;
import fr.acinq.lightning.MilliSatoshi;
import jrpc.clightning.plugins.ICLightningPlugin;
import jrpc.clightning.plugins.log.PluginLog;
import jrpc.service.converters.jsonwrapper.CLightningJsonObject;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.knowm.xchange.Exchange;
import org.knowm.xchange.currency.Currency;
import org.knowm.xchange.currency.CurrencyPair;
import org.knowm.xchange.dto.Order;
import org.knowm.xchange.dto.marketdata.Ticker;
import org.knowm.xchange.dto.trade.LimitOrder;
import org.knowm.xchange.dto.trade.MarketOrder;
import org.knowm.xchange.service.marketdata.params.CurrencyPairsParam;
import org.knowm.xchange.service.trade.TradeService;
import org.knowm.xchange.utils.OrderValuesHelper;
import org.tbk.cln.snr.RunOptions;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Optional;

@RequiredArgsConstructor
public class SendpaySuccess implements ClnSubscription {
    private static final int BTC_FRACTION_DIGITS = 8;

    @NonNull
    private final Exchange exchange;

    @NonNull
    private final Currency fiatCurrency;

    @NonNull
    private final RunOptions runOptions;

    @Override
    public void execute(ICLightningPlugin plugin, CLightningJsonObject data) throws Exception {
        plugin.log(PluginLog.DEBUG, data);

        CurrencyPair currencyPair = new CurrencyPair(Currency.BTC, fiatCurrency);
        boolean supportedCurrencyPair = exchange.getExchangeInstruments().contains(currencyPair);
        if (!supportedCurrencyPair) {
            throw new IllegalStateException("Currency pair is not supported: " + currencyPair);
        }

        Optional<JsonObject> payload = Optional.ofNullable(data.getAsJsonObject("params"))
                .map(it -> it.getAsJsonObject("sendpay_success"));

        String shortPaymentHash = payload
                .map(it -> it.getAsJsonPrimitive("payment_hash"))
                .map(it -> it.getAsString().substring(0, 8))
                .orElseThrow(() -> new IllegalStateException("Could not extract 'payment_hash' from payload."));

        String orderUserRef = String.format("cln-snr-%s", shortPaymentHash);

        MilliSatoshi amountSent = payload
                .map(it -> it.getAsJsonPrimitive("msatoshi_sent"))
                .map(it -> new MilliSatoshi(it.getAsLong()))
                .orElseThrow(() -> new IllegalStateException("Could not extract 'msatoshi_sent' from payload."));

        plugin.log(PluginLog.DEBUG, "Spent amount which needs to be replaced: " + amountSent);

        Satoshi amountToReplace = amountSent.truncateToSatoshi().plus(new Satoshi(1L));

        Order order = createOrder(currencyPair, amountToReplace)
                .userReference(orderUserRef)
                .build();

        plugin.log(PluginLog.INFO, "Will place order: " + order);

        String orderId = placeOrder(order);

        String successMessage = String.format("Placed an order on %s with id '%s' and ref '%s'",
                exchange.getExchangeSpecification().getExchangeName(), orderId, order.getUserReference());
        plugin.log(PluginLog.INFO, successMessage);
    }

    private static BigDecimal satsToBtc(Satoshi val) {
        return BigDecimal.valueOf(val.toLong())
                .movePointLeft(BTC_FRACTION_DIGITS)
                .setScale(BTC_FRACTION_DIGITS, RoundingMode.UNNECESSARY);
    }

    private Order.Builder createOrder(CurrencyPair currencyPair, Satoshi amount) throws Exception {
        OrderValuesHelper orderValuesHelper = new OrderValuesHelper(exchange.getExchangeMetaData().getInstruments().get(currencyPair));

        BigDecimal bitcoinAmount = orderValuesHelper.adjustAmount(satsToBtc(amount));

        MarketOrder marketOrder = new MarketOrder.Builder(Order.OrderType.BID, currencyPair)
                .originalAmount(bitcoinAmount)
                .build();

        if (!runOptions.isDryRun()) {
            return MarketOrder.Builder.from(marketOrder);
        } else {
            // Run in demo with massively undervalued order!
            // e.g. from the kraken docs:
            // "[...] we recommend placing very small market orders (orders for the minimum order size),
            // or limit orders that are priced far away from the current market price"
            BigDecimal priceMultiplier = new BigDecimal("0.01");

            Ticker ticker = fetchTicker(currencyPair);

            BigDecimal buyingPrice = orderValuesHelper
                    .adjustPrice(ticker.getBid().multiply(priceMultiplier), RoundingMode.CEILING);

            return LimitOrder.Builder.from(marketOrder)
                    .limitPrice(buyingPrice);
        }
    }

    private String placeOrder(Order order) throws IOException {
        TradeService tradeService = exchange.getTradeService();
        if (order instanceof MarketOrder marketOrder) {
            return tradeService.placeMarketOrder(marketOrder);
        } else if (order instanceof LimitOrder limitOrder) {
            return tradeService.placeLimitOrder(limitOrder);
        } else {
            String errorMessage = String.format("Order type is not supported: %s",
                    order.getClass().getSimpleName());
            throw new IllegalStateException(errorMessage);
        }
    }

    private Ticker fetchTicker(CurrencyPair currencyPair) {
        CurrencyPairsParam currencyPairsParam = () -> List.of(currencyPair);
        try {
            return exchange.getMarketDataService().getTickers(currencyPairsParam).stream()
                    .filter(it -> currencyPair.equals(it.getInstrument()))
                    .findFirst()
                    .orElseThrow(() -> new IllegalStateException("Could not find ticker data for currency pair."));
        } catch (Exception e) {
            String errorMessage = String.format("Could not fetch ticker from %s for currency pair '%s'.",
                    exchange.getExchangeSpecification().getExchangeName(), currencyPair);
            throw new IllegalStateException(errorMessage, e);
        }
    }
}
