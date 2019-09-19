package com.izuche.config;

import com.izuche.processor.RefreshBeanFactoryProcessor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnClass(RefreshBeanFactoryProcessor.class)
public class BeanProcessorsConfig {

    @Bean
    public RefreshBeanFactoryProcessor refreshBeanFactoryProcessor(){
        return new RefreshBeanFactoryProcessor();
    }


}
