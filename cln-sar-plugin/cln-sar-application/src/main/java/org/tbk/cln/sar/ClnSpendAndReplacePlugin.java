package org.tbk.cln.sar;

import com.google.gson.JsonObject;
import jrpc.clightning.annotation.PluginOption;
import jrpc.clightning.annotation.RPCMethod;
import jrpc.clightning.annotation.Subscription;
import jrpc.clightning.plugins.CLightningPlugin;
import jrpc.clightning.plugins.ICLightningPlugin;
import jrpc.clightning.plugins.log.PluginLog;
import jrpc.service.converters.jsonwrapper.CLightningJsonObject;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.knowm.xchange.Exchange;
import org.knowm.xchange.currency.Currency;
import org.knowm.xchange.currency.CurrencyPair;
import org.knowm.xchange.dto.marketdata.Ticker;
import org.knowm.xchange.service.marketdata.params.CurrencyPairsParam;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

@RequiredArgsConstructor
public class ClnSpendAndReplacePlugin extends CLightningPlugin {

    @NonNull
    private final ApplicationShutdownManager shutdownManager;

    @NonNull
    private final Exchange exchange;

    @PluginOption(
            name = "sar-dry-run",
            defValue = "false",
            typeValue = "flag",
            description = "Startup the rest server when the node call the method init.")
    private boolean dryRun;

    @Override
    public void start() {
        super.start();
    }

    @Override
    public void onInit(ICLightningPlugin plugin, CLightningJsonObject request, CLightningJsonObject response) {
        super.onInit(plugin, request, response);
        this.log(PluginLog.DEBUG, "spend-and-replace initialized. Request:" + request);

        // test disable (hint: works!)
        // DEBUG   plugin-spend-and-replace: Killing plugin: disabled itself at init: just testing if disabling works
        // response.add("disable", "just testing if disabling works");
    }

    @RPCMethod(
            name = "sar-listconfigs",
            description = "Command to list all configuration options."
    )
    public void rpcListconfigs(ICLightningPlugin plugin, CLightningJsonObject request, CLightningJsonObject response) {
        response.addProperty("dry-run", dryRun);
    }

    @Subscription(notification = "shutdown")
    public void shutdown(CLightningJsonObject data) {
        System.exit(shutdownManager.initiateShutdown(0));
    }

    // https://lightning.readthedocs.io/PLUGINS.html?#sendpay-success
    @Subscription(notification = "sendpay_success")
    public void onNotificationSendpaySuccess(CLightningJsonObject data) {
        log(PluginLog.DEBUG, "Notification sendpay_success received.");

        try {
            long amountWithFees = data.getAsJsonObject("sendpay_success")
                    .getAsJsonPrimitive("amount_sent_msat")
                    .getAsLong();
            log(PluginLog.DEBUG, "Spent amount which needs to be replaced: " + amountWithFees);

            if (dryRun) {
                log(PluginLog.INFO, "Dry run is active; would have replaced: " + amountWithFees);
            } else {
                log(PluginLog.ERROR, "Error while replacing amount: Not implemented yet");
            }
        } catch (Exception e) {
            // empty on purpose
        }
    }

    @RPCMethod(
            name = "sar-ticker",
            description = "Say hello from the spend-and-replace plugin",
            parameter = "[fiat-currency]"

    )
    public void rpcTicker(ICLightningPlugin plugin, CLightningJsonObject request, CLightningJsonObject response) {
        log(PluginLog.DEBUG, "rpc ticker invoked: " + request.getWrapper());

        Currency bitcoinCurrency = Currency.BTC;

        Currency fiatCurrency = Currency.getInstance("USD");
        CurrencyPair currencyPair = new CurrencyPair(bitcoinCurrency, fiatCurrency);

        boolean supportedCurrencyPair = exchange.getExchangeInstruments().contains(currencyPair);
        if (!supportedCurrencyPair) {
            throw new IllegalStateException("Currency pair is not supported: " + currencyPair);
        }

        try {
            CurrencyPairsParam currencyPairsParam = () -> Collections.singletonList(currencyPair);
            List<Ticker> tickers = exchange.getMarketDataService().getTickers(currencyPairsParam).stream()
                    .filter(it -> currencyPair.equals(it.getInstrument()))
                    .toList();

            JsonObject result = new JsonObject();
            tickers.forEach(ticker -> {
                JsonObject data = new JsonObject();
                data.addProperty("ask", ticker.getAsk().toPlainString());
                data.addProperty("bid", ticker.getAsk().toPlainString());
                data.addProperty("high", ticker.getHigh().toPlainString());
                data.addProperty("low", ticker.getLow().toPlainString());
                data.addProperty("open", ticker.getOpen().toPlainString());
                data.addProperty("last", ticker.getLast().toPlainString());

                result.add(ticker.getInstrument().toString(), data);
            });

            response.add("result", result);
        } catch (IOException e) {
            response.add("error", e.getMessage());
        }
    }
}
