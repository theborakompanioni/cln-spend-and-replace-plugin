package org.tbk.cln.snr;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.knowm.xchange.Exchange;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static java.util.Objects.requireNonNull;

@Slf4j
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(ClnSpendAndReplaceProperties.class)
public class ClnSpendAndReplaceConfig {

    private final ClnSpendAndReplaceProperties properties;

    public ClnSpendAndReplaceConfig(ClnSpendAndReplaceProperties properties) {
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
                .build();
    }

    @Bean
    public ApplicationShutdownManager applicationShutdownManager(ApplicationContext applicationContext) {
        return new ApplicationShutdownManager(applicationContext);
    }

    @Bean
    public ClnSpendAndReplacePlugin clnSpendAndReplacePlugin(ApplicationShutdownManager applicationShutdownManager,
                                                             Exchange exchange,
                                                             RunOptions dryRunOption) {
        return new ClnSpendAndReplacePlugin(applicationShutdownManager, exchange, dryRunOption);
    }

    @Bean
    public PluginInitializer pluginInitializer(ClnSpendAndReplacePlugin plugin) {
        return new PluginInitializer(plugin);
    }

    @RequiredArgsConstructor
    public static final class PluginInitializer implements InitializingBean, DisposableBean {

        private final ExecutorService executor = Executors.newSingleThreadExecutor();
        private final ClnSpendAndReplacePlugin plugin;


        @Override
        public void afterPropertiesSet() {
            executor.submit(plugin::start);
        }

        @Override
        public void destroy() {
            executor.shutdown();
        }
    }
}
