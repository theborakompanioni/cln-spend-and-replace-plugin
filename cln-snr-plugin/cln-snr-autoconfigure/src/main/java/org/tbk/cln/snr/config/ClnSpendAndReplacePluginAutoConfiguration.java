package org.tbk.cln.snr.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import static java.util.Objects.requireNonNull;

@Slf4j
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(ClnSpendAndReplacePluginAutoConfigurationProperties.class)
@ConditionalOnProperty(value = "org.tbk.cln.plugin.spend-and-replace.enabled", havingValue = "true", matchIfMissing = true)
public class ClnSpendAndReplacePluginAutoConfiguration {

    private final ClnSpendAndReplacePluginAutoConfigurationProperties properties;

    public ClnSpendAndReplacePluginAutoConfiguration(ClnSpendAndReplacePluginAutoConfigurationProperties properties) {
        this.properties = requireNonNull(properties);
    }

}
