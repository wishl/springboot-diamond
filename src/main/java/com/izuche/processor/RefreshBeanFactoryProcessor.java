package com.izuche.processor;

import com.izuche.config.RefreshHolder;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;

public class RefreshBeanFactoryProcessor implements BeanFactoryPostProcessor {

    // 添加beanProcessor
    @Override
    public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {
        RefreshBeanPostProcessor processor = new RefreshBeanPostProcessor();
        beanFactory.addBeanPostProcessor(processor);
        RefreshHolder.addListener(processor);
    }

}
