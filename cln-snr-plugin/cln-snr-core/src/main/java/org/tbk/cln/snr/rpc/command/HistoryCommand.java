package org.tbk.cln.snr.rpc.command;

import com.google.gson.JsonObject;
import jrpc.clightning.plugins.ICLightningPlugin;
import jrpc.service.converters.jsonwrapper.CLightningJsonObject;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.knowm.xchange.Exchange;
import org.knowm.xchange.dto.trade.LimitOrder;
import org.knowm.xchange.dto.trade.OpenOrders;
import org.knowm.xchange.dto.trade.UserTrade;
import org.knowm.xchange.dto.trade.UserTrades;
import org.knowm.xchange.service.trade.TradeService;
import org.knowm.xchange.service.trade.params.TradeHistoryParams;
import org.knowm.xchange.service.trade.params.orders.OpenOrdersParams;

import java.io.IOException;

@RequiredArgsConstructor
public class HistoryCommand implements RpcCommand {

    @NonNull
    private final Exchange exchange;

    @NonNull
    private final OpenOrdersParams openOrdersParams;

    @NonNull
    private final TradeHistoryParams tradeHistoryParams;

    @Override
    public void execute(ICLightningPlugin plugin, CLightningJsonObject request, CLightningJsonObject response) throws IOException {
        TradeService tradeService = exchange.getTradeService();

        OpenOrders openOrders = tradeService.getOpenOrders(openOrdersParams);
        JsonObject openOrdersJson = new JsonObject();
        openOrders.getOpenOrders().forEach(val -> openOrdersJson.add(val.getId(), toJson(val)));

        UserTrades userTrades = tradeService.getTradeHistory(tradeHistoryParams);
        JsonObject closedOrdersJson = new JsonObject();
        userTrades.getUserTrades().forEach(val -> closedOrdersJson.add(val.getId(), toJson(val)));

        JsonObject result = new JsonObject();
        result.add("open", openOrdersJson);
        result.add("closed", closedOrdersJson);

        response.add("result", result);
    }

    private static JsonObject toJson(LimitOrder val) {
        JsonObject json = new JsonObject();
        json.addProperty("id", val.getId());
        json.addProperty("type", val.getType().name());
        json.addProperty("status", val.getStatus().name());
        json.addProperty("is-open", val.getStatus().isOpen());
        json.addProperty("is-final", val.getStatus().isFinal());
        json.addProperty("original-amount", val.getOriginalAmount().toPlainString());
        json.addProperty("remaining-amount", val.getRemainingAmount().toPlainString());
        json.addProperty("limit-price", val.getLimitPrice().toPlainString());
        json.addProperty("asset-pair", val.getInstrument().toString());
        json.addProperty("ref", val.getUserReference());
        json.addProperty("date", val.getTimestamp().toInstant().toString());
        json.addProperty("timestamp", val.getTimestamp().toInstant().getEpochSecond());
        return json;
    }

    private static JsonObject toJson(UserTrade val) {
        JsonObject json = new JsonObject();
        json.addProperty("id", val.getId());
        json.addProperty("type", val.getType().name());
        json.addProperty("order-id", val.getOrderId());
        json.addProperty("price", val.getPrice().toPlainString());
        json.addProperty("original-amount", val.getOriginalAmount().toPlainString());
        json.addProperty("asset-pair", val.getInstrument().toString());
        json.addProperty("ref", val.getOrderUserReference());
        json.addProperty("fee-amount", val.getFeeAmount().toPlainString());
        json.addProperty("fee-currency", val.getFeeCurrency().getCurrencyCode());
        json.addProperty("date", val.getTimestamp().toInstant().toString());
        json.addProperty("timestamp", val.getTimestamp().toInstant().getEpochSecond());
        return json;
    }
}
