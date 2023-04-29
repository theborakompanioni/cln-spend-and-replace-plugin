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
import org.knowm.xchange.instrument.Instrument;
import org.knowm.xchange.service.trade.params.TradeHistoryParamsAll;
import org.knowm.xchange.service.trade.params.orders.DefaultOpenOrdersParamInstrument;
import org.tbk.cln.sar.rpc.command.*;

import java.util.function.Supplier;

@RequiredArgsConstructor
public final class ClnSpendAndReplacePlugin extends CLightningPlugin {

    private static final String DEFAULT_FIAT_CURRENCY = "USD";

    @NonNull
    private final ApplicationShutdownManager shutdownManager;

    @NonNull
    private final Exchange exchange;

    @PluginOption(
            name = "sar-dry-run",
            typeValue = "flag",
            defValue = "false",
            description = "Enable dry run. No trades are executed."
    )
    boolean dryRun;

    @PluginOption(
            name = "sar-default-fiat-currency",
            typeValue = "string",
            defValue = DEFAULT_FIAT_CURRENCY,
            description = "The default fiat currency"
    )
    String defaultFiatCurrency = DEFAULT_FIAT_CURRENCY;

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
        log(PluginLog.DEBUG, "rpc 'sar-listconfigs' invoked: " + request.getWrapper());

        execute(plugin, request, response, () -> {
            JsonObject config = new JsonObject();
            config.addProperty("dry-run", dryRun);

            JsonObject fiatCurrencyData = new JsonObject();
            fiatCurrencyData.addProperty("default", defaultFiatCurrency);
            config.add("fiat-currency", fiatCurrencyData);

            JsonObject exchangeData = new JsonObject();
            exchangeData.addProperty("name", exchange.getExchangeSpecification().getExchangeName());
            exchangeData.addProperty("host", exchange.getExchangeSpecification().getHost());
            config.add("exchange", exchangeData);
            return new ListconfigsCommand(config);
        });
    }

    @RPCMethod(
            name = "sar-version",
            description = "Command to print the plugin version."
    )
    public void rpcVersion(ICLightningPlugin plugin, CLightningJsonObject request, CLightningJsonObject response) {
        log(PluginLog.DEBUG, "rpc 'sar-version' invoked: " + request.getWrapper());

        execute(plugin, request, response, VersionCommand::new);
    }

    @RPCMethod(
            name = "sar-ticker",
            description = "Get the ticker representing the current exchange rate for the provided currency.",
            parameter = "[fiat-currency]"
    )
    public void rpcTicker(ICLightningPlugin plugin, CLightningJsonObject request, CLightningJsonObject response) {
        plugin.log(PluginLog.DEBUG, "rpc 'sar-ticker' invoked: " + request.getWrapper());

        execute(plugin, request, response, () -> {
            initExchangeIfNecessary();
            return new TickerCommand(exchange, Currency.getInstance(defaultFiatCurrency));
        });
    }

    @RPCMethod(
            name = "sar-balance",
            description = "Get the balance of your account."
    )
    public void rpcBalance(ICLightningPlugin plugin, CLightningJsonObject request, CLightningJsonObject response) {
        log(PluginLog.DEBUG, "rpc 'sar-balance' invoked: " + request.getWrapper());

        execute(plugin, request, response, () -> {
            initExchangeIfNecessary();
            return new BalanceCommand(exchange);
        });
    }


    @RPCMethod(
            name = "sar-history",
            description = "Get the trade history of your account."
    )
    public void rpcHistory(ICLightningPlugin plugin, CLightningJsonObject request, CLightningJsonObject response) {
        log(PluginLog.DEBUG, "rpc 'sar-history' invoked: " + request.getWrapper());

        execute(plugin, request, response, () -> {
            initExchangeIfNecessary();

            Instrument instrument = new CurrencyPair(Currency.BTC, Currency.getInstance(defaultFiatCurrency));

            DefaultOpenOrdersParamInstrument openOrdersParams = new DefaultOpenOrdersParamInstrument(instrument);

            TradeHistoryParamsAll tradeHistoryParams = new TradeHistoryParamsAll();
            tradeHistoryParams.setInstrument(instrument);
            return new HistoryCommand(exchange, openOrdersParams, tradeHistoryParams);
        });
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

    private void execute(ICLightningPlugin plugin,
                         CLightningJsonObject request,
                         CLightningJsonObject response,
                         Supplier<RpcCommand> commandSupplier) {
        try {
            commandSupplier.get().execute(plugin, request, response);
        } catch (Exception e) {
            response.add("error", e.getMessage());
        }
    }

    private void initExchangeIfNecessary() {
        try {
            boolean needsInit = exchange.getExchangeMetaData() == null || exchange.getExchangeMetaData().getInstruments() == null;
            if (needsInit) {
                exchange.remoteInit();
            }
        } catch (Exception e) {
            throw new IllegalStateException("Could not initialize exchange", e);
        }
    }
}
