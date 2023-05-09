package org.tbk.cln.snr.rpc.subscription;

import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import fr.acinq.bitcoin.Satoshi;
import fr.acinq.lightning.MilliSatoshi;
import jrpc.clightning.plugins.ICLightningPlugin;
import jrpc.clightning.plugins.log.PluginLog;
import jrpc.service.converters.jsonwrapper.CLightningJsonObject;
import org.knowm.xchange.Exchange;
import org.knowm.xchange.currency.Currency;
import org.knowm.xchange.currency.CurrencyPair;
import org.knowm.xchange.dto.Order;
import org.knowm.xchange.dto.meta.InstrumentMetaData;
import org.knowm.xchange.utils.OrderValuesHelper;
import org.tbk.cln.snr.RunOptions;
import org.tbk.cln.snr.exchange.ExchangeService;
import org.tbk.cln.snr.exchange.OrderId;

import java.util.HexFormat;
import java.util.Optional;

import static java.util.Objects.requireNonNull;

public class SendpaySuccess implements ClnSubscription {

    private final Exchange exchange;

    private final Currency fiatCurrency;

    private final RunOptions runOptions;

    private final ExchangeService exchangeService;

    public SendpaySuccess(Exchange exchange, Currency fiatCurrency, RunOptions runOptions) {
        this.exchange = requireNonNull(exchange);
        this.fiatCurrency = requireNonNull(fiatCurrency);
        this.runOptions = requireNonNull(runOptions);

        this.exchangeService = new ExchangeService(exchange);
    }

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

        MilliSatoshi amountSent = payload
                .map(it -> it.getAsJsonPrimitive("msatoshi_sent"))
                .map(it -> new MilliSatoshi(it.getAsLong()))
                .orElseThrow(() -> new IllegalStateException("Could not extract 'msatoshi_sent' from payload."));

        plugin.log(PluginLog.DEBUG, "Spent amount which needs to be replaced: " + amountSent);

        Satoshi amountToReplace = amountSent.truncateToSatoshi().plus(new Satoshi(1L));

        int shortPaymentHash = payload
                .map(it -> it.getAsJsonPrimitive("payment_hash"))
                .map(JsonPrimitive::getAsString)
                .map(it -> HexFormat.fromHexDigits(it, 0, 8))
                .orElseThrow(() -> new IllegalStateException("Could not extract 'payment_hash' from payload."));

        InstrumentMetaData instrumentMetaData = exchange.getExchangeMetaData().getInstruments().get(currencyPair);
        OrderValuesHelper orderValuesHelper = new OrderValuesHelper(instrumentMetaData);

        Order order = createOrder(currencyPair, amountToReplace)
                // e.g. kraken needs a 32-byte integer as user reference
                .userReference(String.valueOf(shortPaymentHash))
                .build();

        boolean isUnderMinimum = orderValuesHelper.amountUnderMinimum(order.getOriginalAmount());
        if (isUnderMinimum) {
            String warnMessage = String.format("Will **NOT** place order for outgoing payment. Amount is too small: %s < %s",
                    order.getOriginalAmount().toPlainString(), instrumentMetaData.getMinimumAmount().toPlainString());
            plugin.log(PluginLog.WARNING, warnMessage);
        } else {
            plugin.log(PluginLog.INFO, "Will place order: " + order);

            try {
                OrderId orderId = exchangeService.placeOrder(order);

                String successMessage = String.format("Placed an order on %s with id '%s' and ref '%s'",
                        exchange.getExchangeSpecification().getExchangeName(), orderId, order.getUserReference());
                plugin.log(PluginLog.INFO, successMessage);
            } catch (Exception e) {
                String errorMessage = String.format("Could not place order on %s for amount %s: %s",
                        exchange.getExchangeSpecification().getExchangeName(), order.getOriginalAmount().toPlainString(), e.getMessage());
                plugin.log(PluginLog.ERROR, errorMessage);
            }
        }
    }

    private Order.Builder createOrder(CurrencyPair currencyPair, Satoshi amount) {
        if (!runOptions.isDryRun()) {
            return exchangeService.createMarketOrder(currencyPair, amount);
        } else {
            return exchangeService.createTestLimitOrder(currencyPair, amount);
        }
    }
}
