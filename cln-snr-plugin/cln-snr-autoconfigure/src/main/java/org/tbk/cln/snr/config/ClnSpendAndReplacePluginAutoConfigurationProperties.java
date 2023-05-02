package org.tbk.cln.snr.config;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.ConstructorBinding;

import java.util.Objects;

@ConfigurationProperties(
        prefix = "org.tbk.cln.plugin.spend-and-replace",
        ignoreUnknownFields = false
)
@Getter
@AllArgsConstructor(onConstructor = @__(@ConstructorBinding))
public class ClnSpendAndReplacePluginAutoConfigurationProperties {

    private boolean enabled;

    private Boolean dry;

    public boolean getDry() {
        return Objects.requireNonNullElse(dry, false);
    }

}
