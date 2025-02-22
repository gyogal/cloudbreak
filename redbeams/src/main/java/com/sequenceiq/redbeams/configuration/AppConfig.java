package com.sequenceiq.redbeams.configuration;

import java.io.IOException;
import java.security.Security;

import javax.annotation.PostConstruct;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ResourceLoaderAware;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import com.sequenceiq.cloudbreak.concurrent.TracingAndMdcCopyingTaskDecorator;

import io.opentracing.Tracer;

@Configuration
@EnableRetry
public class AppConfig implements ResourceLoaderAware {

    private static final Logger LOGGER = LoggerFactory.getLogger(AppConfig.class);

    // from com.sequenceiq.cloudbreak.auth.filter.AuthFilterConfiguration
    // redbeams should probably control this itself
    // private static final int BEAN_ORDER_CRN_FILTER = 0;

    private static final int BEAN_ORDER_REQUEST_ID_GENERATING_FILTER = 100;

    private static final int BEAN_ORDER_REQUEST_ID_FILTER = 110;

    @Inject
    private Tracer tracer;

    @Value("${redbeams.etc.config.dir}")
    private String etcConfigDir;

    @Value("${redbeams.threadpool.core.size:}")
    private int corePoolSize;

    @Value("${redbeams.threadpool.capacity.size:}")
    private int queueCapacity;

    @Value("${redbeams.intermediate.threadpool.core.size:}")
    private int intermediateCorePoolSize;

    @Value("${redbeams.intermediate.threadpool.capacity.size:}")
    private int intermediateQueueCapacity;

    @Value("${redbeams.client.id}")
    private String clientId;

    @Value("${rest.debug}")
    private boolean restDebug;

    @Value("${cert.validation}")
    private boolean certificateValidation;

    @Value("${cert.ignorePreValidation}")
    private boolean ignorePreValidation;

    private ResourceLoader resourceLoader;

    @PostConstruct
    public void init() throws IOException {
        Security.addProvider(new org.bouncycastle.jce.provider.BouncyCastleProvider());
    }

    @Bean
    public AsyncTaskExecutor intermediateBuilderExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(intermediateCorePoolSize);
        executor.setQueueCapacity(intermediateQueueCapacity);
        executor.setThreadNamePrefix("intermediateBuilderExecutor-");
        executor.setTaskDecorator(new TracingAndMdcCopyingTaskDecorator(tracer));
        executor.initialize();
        return executor;
    }

    @Bean
    public AsyncTaskExecutor resourceBuilderExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(corePoolSize);
        executor.setQueueCapacity(queueCapacity);
        executor.setThreadNamePrefix("resourceBuilderExecutor-");
        executor.setTaskDecorator(new TracingAndMdcCopyingTaskDecorator(tracer));
        executor.initialize();
        return executor;
    }

    @Override
    public void setResourceLoader(ResourceLoader resourceLoader) {
        this.resourceLoader = resourceLoader;
    }
}
