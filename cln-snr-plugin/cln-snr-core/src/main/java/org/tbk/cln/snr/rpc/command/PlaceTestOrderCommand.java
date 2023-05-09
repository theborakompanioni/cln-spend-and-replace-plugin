package org.tbk.cln.snr.rpc.command;

import com.google.gson.JsonObject;
import jrpc.clightning.plugins.ICLightningPlugin;
import jrpc.service.converters.jsonwrapper.CLightningJsonObject;
import org.knowm.xchange.Exchange;
import org.knowm.xchange.currency.Currency;
import org.knowm.xchange.currency.CurrencyPair;
import org.knowm.xchange.dto.trade.LimitOrder;
import org.tbk.cln.snr.exchange.ExchangeService;
import org.tbk.cln.snr.exchange.OrderId;

import java.io.IOException;

import static java.util.Objects.requireNonNull;

public class PlaceTestOrderCommand implements RpcCommand {
    private final Exchange exchange;

    private final Currency fiatCurrency;

    private final ExchangeService exchangeService;

    public PlaceTestOrderCommand(Exchange exchange, Currency fiatCurrency) {
        this.exchange = requireNonNull(exchange);
        this.fiatCurrency = requireNonNull(fiatCurrency);

        this.exchangeService = new ExchangeService(exchange);
    }

    @Override
    public void execute(ICLightningPlugin plugin, CLightningJsonObject request, CLightningJsonObject response) throws IOException {
        CurrencyPair currencyPair = new CurrencyPair(Currency.BTC, fiatCurrency);
        boolean supportedCurrencyPair = exchange.getExchangeInstruments().contains(currencyPair);
        if (!supportedCurrencyPair) {
            throw new IllegalStateException("Currency pair is not supported: " + currencyPair);
        }

        LimitOrder testLimitOrder = exchangeService.createTestLimitOrder(currencyPair).build();
        OrderId orderId = exchangeService.placeOrder(testLimitOrder);

        JsonObject orderJson = new JsonObject();
        orderJson.addProperty("id", orderId.toString());
        orderJson.addProperty("type", testLimitOrder.getType().name());
        orderJson.addProperty("asset-pair", testLimitOrder.getInstrument().toString());
        orderJson.addProperty("amount", testLimitOrder.getOriginalAmount().toPlainString());
        orderJson.addProperty("price", testLimitOrder.getLimitPrice().toPlainString());

        JsonObject result = new JsonObject();
        result.add("order", orderJson);

        response.add("result", result);
    }
}
