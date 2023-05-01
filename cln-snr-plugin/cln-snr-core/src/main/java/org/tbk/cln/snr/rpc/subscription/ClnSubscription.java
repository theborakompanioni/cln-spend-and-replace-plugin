package org.tbk.cln.snr.rpc.subscription;

import jrpc.clightning.plugins.ICLightningPlugin;
import jrpc.service.converters.jsonwrapper.CLightningJsonObject;

public interface ClnSubscription {
    void execute(ICLightningPlugin plugin, CLightningJsonObject data) throws Exception;
}
