package org.tbk.cln.snr;

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
import org.tbk.cln.snr.rpc.command.*;
import org.tbk.cln.snr.rpc.subscription.ClnSubscription;
import org.tbk.cln.snr.rpc.subscription.SendpaySuccess;

import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;

@RequiredArgsConstructor
public final class ClnSpendAndReplacePlugin extends CLightningPlugin {

    private static final String DEFAULT_FIAT_CURRENCY = "USD";

    @NonNull
    private final ApplicationShutdownManager shutdownManager;

    @NonNull
    private final Exchange exchange;

    @NonNull
    private final RunOptions runOption;

    @PluginOption(
            name = "snr-dry-run",
            typeValue = "flag",
            defValue = "false",
            description = "Enable dry run. Trades are executed against a demo exchange."
    )
    boolean dryRun;

    @PluginOption(
            name = "snr-default-fiat-currency",
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

        Optional<String> networkOrEmpty = Optional.ofNullable(request.getAsJsonObject("params"))
                .map(it -> it.getAsJsonObject("configuration"))
                .map(it -> it.getAsJsonPrimitive("network").getAsString());

        if (networkOrEmpty.isEmpty()) {
            plugin.log(PluginLog.WARNING, "Safety measure: Disable plugin as no network param could be found.");
            response.add("disable", "No network found");
            return;
        }

        boolean isMainnet = networkOrEmpty
                .map(it -> "bitcoin".equalsIgnoreCase(it) || "mainnet".equalsIgnoreCase(it))
                .orElse(false);

        this.dryRun = this.dryRun || this.runOption.isDryRun() || !isMainnet;

        // test disable (hint: works!)
        // DEBUG   plugin-spend-and-replace: Killing plugin: disabled itself at init: just testing if disabling works
        // response.add("disable", "just testing if disabling works");
    }

    @RPCMethod(
            name = "snr-listconfigs",
            description = "Command to list all configuration options."
    )
    public void rpcListconfigs(ICLightningPlugin plugin, CLightningJsonObject request, CLightningJsonObject response) {
        log(PluginLog.DEBUG, "rpc 'snr-listconfigs' invoked: " + request.getWrapper());

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
            name = "snr-version",
            description = "Command to print the plugin version."
    )
    public void rpcVersion(ICLightningPlugin plugin, CLightningJsonObject request, CLightningJsonObject response) {
        log(PluginLog.DEBUG, "rpc 'snr-version' invoked: " + request.getWrapper());

        execute(plugin, request, response, VersionCommand::new);
    }

    @RPCMethod(
            name = "snr-ticker",
            description = "Get the ticker representing the current exchange rate for the provided currency.",
            parameter = "[fiat-currency]"
    )
    public void rpcTicker(ICLightningPlugin plugin, CLightningJsonObject request, CLightningJsonObject response) {
        plugin.log(PluginLog.DEBUG, "rpc 'snr-ticker' invoked: " + request.getWrapper());

        execute(plugin, request, response, () -> {
            initExchangeIfNecessary();
            return new TickerCommand(exchange, Currency.getInstance(defaultFiatCurrency));
        });
    }

    @RPCMethod(
            name = "snr-balance",
            description = "Get the balance of your account."
    )
    public void rpcBalance(ICLightningPlugin plugin, CLightningJsonObject request, CLightningJsonObject response) {
        log(PluginLog.DEBUG, "rpc 'snr-balance' invoked: " + request.getWrapper());

        execute(plugin, request, response, () -> {
            initExchangeIfNecessary();

            Set<Currency> currencies = Set.of(Currency.BTC, Currency.getInstance(defaultFiatCurrency));
            return new BalanceCommand(exchange, currencies);
        });
    }


    @RPCMethod(
            name = "snr-history",
            description = "Get the trade history of your account."
    )
    public void rpcHistory(ICLightningPlugin plugin, CLightningJsonObject request, CLightningJsonObject response) {
        log(PluginLog.DEBUG, "rpc 'snr-history' invoked: " + request.getWrapper());

        execute(plugin, request, response, () -> {
            initExchangeIfNecessary();

            Instrument instrument = new CurrencyPair(Currency.BTC, Currency.getInstance(defaultFiatCurrency));

            DefaultOpenOrdersParamInstrument openOrdersParams = new DefaultOpenOrdersParamInstrument(instrument);

            TradeHistoryParamsAll tradeHistoryParams = new TradeHistoryParamsAll();
            tradeHistoryParams.setInstrument(instrument);
            return new HistoryCommand(exchange, openOrdersParams, tradeHistoryParams);
        });
    }

    /**
     * React on 'shutdown' notifications
     * <p>
     * <a href="https://lightning.readthedocs.io/PLUGINS.html#shutdown">From the docs</a>:
     * Send in two situations: lightningd is (almost completely) shutdown, or the plugin stop command has been called
     * for this plugin. In both cases the plugin has 30 seconds to exit itself, otherwise it’s killed.
     * In the shutdown case, plugins should not interact with lightnind except via (id-less) logging or notifications.
     * New rpc calls will fail with error code -5 and (plugin’s) responses will be ignored. Because lightningd can
     * crash or be killed, a plugin cannot rely on the shutdown notification always been sent.
     *
     * @param data The event data
     */
    @Subscription(notification = "shutdown")
    public void onNotificationShutdown(CLightningJsonObject data) {
        log(PluginLog.DEBUG, "Notification shutdown received.");
        System.exit(shutdownManager.initiateShutdown(0));
    }

    /**
     * React on 'sendpay_success' notifications
     * <p>
     * <a href="https://lightning.readthedocs.io/PLUGINS.html#sendpay-success">From the docs</a>:
     * A notification for topic sendpay_success is sent every time a sendpay succeeds (with complete status).
     * The json is the same as the return value of the commands sendpay/waitsendpay when these commands succeed.
     *
     * @param data The event data
     */
    @Subscription(notification = "sendpay_success")
    public void onNotificationSendpaySuccess(CLightningJsonObject data) {
        log(PluginLog.DEBUG, "Notification 'sendpay_success' received.");

        this.execute(data, () -> {
            initExchangeIfNecessary();
            return new SendpaySuccess(exchange, Currency.getInstance(defaultFiatCurrency), runOptions());
        });
    }

    private void execute(CLightningJsonObject data,
                         Supplier<ClnSubscription> subscriptionSupplier) {
        try {
            subscriptionSupplier.get().execute(this, data);
        } catch (Exception e) {
            log(PluginLog.ERROR, e.getMessage());
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

    private RunOptions runOptions() {
        return this.runOption.toBuilder()
                .dryRun(this.dryRun)
                .build();
    }
}
