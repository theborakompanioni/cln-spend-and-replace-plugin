package org.tbk.cln.snr.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.tbk.cln.snr.RunOptions;

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

    @Bean
    public RunOptions runOptions(Environment env, ApplicationArguments applicationArguments) {
        boolean isExplicitTestEnvironment = env.acceptsProfiles(Profiles.of("test | debug | development | staging"));
        boolean isExplicitDryRunGivenByUserViaArguments = applicationArguments.containsOption("dry");
        boolean isExplicitDryRunGivenByUserViaProperties = properties.getDry();

        boolean dryRunEnabled = isExplicitTestEnvironment
                || isExplicitDryRunGivenByUserViaArguments
                || isExplicitDryRunGivenByUserViaProperties;

        return RunOptions.builder()
                .dryRun(dryRunEnabled)
                .demo(properties.getDemo())
                .build();
    }
}
