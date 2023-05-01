package org.tbk.cln.snr.rpc.command;

import com.google.gson.JsonObject;
import jrpc.clightning.plugins.ICLightningPlugin;
import jrpc.service.converters.jsonwrapper.CLightningJsonObject;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.knowm.xchange.Exchange;
import org.knowm.xchange.currency.Currency;
import org.knowm.xchange.dto.account.Balance;
import org.knowm.xchange.dto.account.Wallet;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.Map;
import java.util.stream.Collectors;

import static com.google.common.base.MoreObjects.firstNonNull;

@RequiredArgsConstructor
public class BalanceCommand implements RpcCommand {

    private static BigDecimal nullToZero(BigDecimal valOrNull) {
        return firstNonNull(valOrNull, BigDecimal.ZERO);
    }

    @NonNull
    private final Exchange exchange;

    @Override
    public void execute(ICLightningPlugin plugin, CLightningJsonObject request, CLightningJsonObject response) throws IOException {
        Map<String, Wallet> wallets = exchange.getAccountService()
                .getAccountInfo()
                .getWallets();

        if (wallets.isEmpty()) {
            throw new IllegalStateException("There is no wallet available.");
        }

        Map<String, JsonObject> walletJsonMap = wallets.entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, walletEntry -> {
                    Wallet wallet = walletEntry.getValue();

                    Map<Currency, Balance> positiveBalances = wallet.getBalances().entrySet().stream()
                            .filter(it -> it.getValue().getTotal().compareTo(BigDecimal.ZERO) > 0)
                            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

                    Map<Currency, JsonObject> currencyBalanceJsonMap = positiveBalances.entrySet().stream()
                            .collect(Collectors.toMap(Map.Entry::getKey, entry -> toJson(entry.getValue())));

                    JsonObject balancesData = new JsonObject();
                    currencyBalanceJsonMap.forEach((key, val) -> balancesData.add(key.getCurrencyCode(), val));

                    JsonObject walletJson = new JsonObject();
                    walletJson.addProperty("id", wallet.getId());
                    walletJson.addProperty("name", wallet.getName());
                    walletJson.add("balances", balancesData);
                    return walletJson;
                }));

        JsonObject walletsData = new JsonObject();
        walletJsonMap.forEach((key, val) -> {
            String safeKey = firstNonNull(key, "_");
            walletsData.add(safeKey, val);
        });

        response.add("result", walletsData);
    }

    private static JsonObject toJson(Balance balance) {
        JsonObject data = new JsonObject();
        data.addProperty("available", nullToZero(balance.getAvailable()).toPlainString());
        data.addProperty("available-for-withdrawal", nullToZero(balance.getAvailableForWithdrawal()).toPlainString());
        data.addProperty("borrowed", nullToZero(balance.getBorrowed()).toPlainString());
        data.addProperty("depositing", nullToZero(balance.getDepositing()).toPlainString());
        data.addProperty("frozen", nullToZero(balance.getFrozen()).toPlainString());
        data.addProperty("loaned", nullToZero(balance.getLoaned()).toPlainString());
        data.addProperty("total", nullToZero(balance.getTotal()).toPlainString());
        data.addProperty("withdrawing", nullToZero(balance.getWithdrawing()).toPlainString());
        return data;
    }
}
