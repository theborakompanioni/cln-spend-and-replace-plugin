package org.tbk.cln.snr;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.knowm.xchange.Exchange;
import org.knowm.xchange.ExchangeFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.tbk.cln.snr.demo.exchange.DummyExchange;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Slf4j
@Configuration(proxyBeanMethods = false)
public class ClnSpendAndReplaceApplicationConfig {

    @Primary
    @Bean
    public Exchange primaryExchange(RunOptions runOptions, Exchange exchange) {
        if (runOptions.isDemo()) {
            return ExchangeFactory.INSTANCE.createExchange(DummyExchange.class);
        }
        return exchange;
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
