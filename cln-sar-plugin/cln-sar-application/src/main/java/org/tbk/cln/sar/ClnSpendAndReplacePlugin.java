package org.tbk.cln.sar;

import jrpc.clightning.annotation.PluginOption;
import jrpc.clightning.annotation.RPCMethod;
import jrpc.clightning.annotation.Subscription;
import jrpc.clightning.plugins.CLightningPlugin;
import jrpc.clightning.plugins.ICLightningPlugin;
import jrpc.clightning.plugins.log.PluginLog;
import jrpc.service.converters.jsonwrapper.CLightningJsonObject;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class ClnSpendAndReplacePlugin extends CLightningPlugin {

    @NonNull
    private final ApplicationShutdownManager shutdownManager;

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
}
