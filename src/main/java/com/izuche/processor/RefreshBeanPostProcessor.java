package com.izuche.processor;

import com.izuche.interfaces.DiamondRefresh;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.beans.factory.config.DependencyDescriptor;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.core.annotation.AnnotationAttributes;
import org.springframework.util.ReflectionUtils;

import java.io.StringReader;
import java.lang.annotation.Annotation;
import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

public class RefreshBeanPostProcessor implements BeanPostProcessor, DiamondRefresh {

    private static final String[] dateFormats  = {"yyyy-MM-dd HH:mm:ss","yyyy-MM-dd","yyyyMMdd"};

    private Logger logger = LoggerFactory.getLogger(RefreshBeanPostProcessor.class);

    private ConcurrentHashMap<String, List<DependencyDescriptorClass>> cache = new ConcurrentHashMap<>();

    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
        Class<?> targetClass = bean.getClass();
        // 获取@Value注解
        ReflectionUtils.doWithFields(targetClass,(field)->{
            AnnotationAttributes annotationAttributes = findAutowiredAnnotation(field, Value.class);
            if(annotationAttributes != null){
                boolean required = determineRequiredStatus(annotationAttributes, "required");
                DependencyDescriptorClass desc = new DependencyDescriptorClass(bean,field,required);
                // 获取@Value注解内容
                // 注册的beanProcessor无法调用setbeanfactory，因为在内部注册使用的类是BeanPostProcessor
//                Object value = beanFactory.getAutowireCandidateResolver().getSuggestedValue(desc);
                Object value = determineValue(annotationAttributes, "value");
                if(value instanceof String){// 仅支持string
                    // 解析@Value的key
                    String key = parseKey((String) value);
                    if(key == null){
                        return;
                    }
                    List<DependencyDescriptorClass> descriptors = cache.get(key);
                    if(descriptors != null){
                        descriptors.add(desc);
                    }else{
                        synchronized (this) {// todo 减小锁粒度
                            descriptors = new CopyOnWriteArrayList<>();
                            descriptors.add(desc);
                            cache.put(key,descriptors);
                        }
                    }
                }
            }
        });
        // todo 添加static刷新
        return bean;
    }

    private boolean determineRequiredStatus(AnnotationAttributes ann,String requiredParameterName) {
        return (!ann.containsKey(requiredParameterName) || ann.getBoolean(requiredParameterName));
    }

    private Object determineValue(AnnotationAttributes ann,String requiredParameterName){
        return ann.get(requiredParameterName);
    }

    private AnnotationAttributes findAutowiredAnnotation(AccessibleObject ao, Class<? extends Annotation> type) {
        if (ao.getAnnotations().length > 0) {  // autowiring annotations have to be local
            AnnotationAttributes attributes = AnnotatedElementUtils.getMergedAnnotationAttributes(ao, type);
            if (attributes != null) {
                return attributes;
            }
        }
        return null;
    }

    @Override
    public void onRefresh(String content) {
        try {
            Properties properties = new Properties();
            properties.load(new StringReader(content));
            for (Map.Entry<Object, Object> entry : properties.entrySet()) {
                String key = entry.getKey().toString();
                String propValue = entry.getValue().toString();
                List<DependencyDescriptorClass> descriptors = cache.get(key);
                if(descriptors != null&& descriptors.size()>0){
                    for (DependencyDescriptorClass descriptor : descriptors) {
                        logger.info("刷新diamond配置,key:{},value:{}",key,propValue);
                        Field field = descriptor.getField();
                        Object bean = descriptor.getBean();
                        field.setAccessible(true);
                        Class<?> fieldType = field.getType();
                        // todo 优化成convert
                        if(fieldType.isPrimitive()) {//--------------------------------8种基本数据类型
                            if(fieldType.equals(boolean.class)) {
                                field.setBoolean(bean, Boolean.parseBoolean(propValue));
                            }else if(fieldType.equals(byte.class)) {
                                field.setByte(bean, Byte.parseByte(propValue));
                            }else if(fieldType.equals(char.class)) {
                                field.setChar(bean, propValue.charAt(0));
                            }else if(fieldType.equals(double.class)) {
                                field.setDouble(bean, Double.parseDouble(propValue));
                            }else if(fieldType.equals(float.class)) {
                                field.setFloat(bean, Float.parseFloat(propValue));
                            }else if(fieldType.equals(int.class)) {
                                field.setInt(bean, Integer.parseInt(propValue));
                            }else if(fieldType.equals(long.class)) {
                                field.setLong(bean, Long.parseLong(propValue));
                            }else if(fieldType.equals(short.class)) {
                                field.setShort(bean, Short.parseShort(propValue));
                            }
                        }else if(fieldType.equals(Boolean.class)) {
                            field.set(bean, Boolean.valueOf(propValue));
                        }else if(fieldType.equals(Byte.class)) {
                            field.set(bean, Byte.valueOf(propValue));
                        }else if(fieldType.equals(Character.class)) {
                            field.set(bean, Character.valueOf(propValue.charAt(0)));
                        }else if(fieldType.equals(Double.class)) {
                            field.set(bean, Double.valueOf(propValue));
                        }else if(fieldType.equals(Float.class)) {
                            field.set(bean, Float.valueOf(propValue));
                        }else if(fieldType.equals(Integer.class)) {
                            field.set(bean, Integer.valueOf(propValue));
                        }else if(fieldType.equals(Long.class)) {
                            field.set(bean, Long.valueOf(propValue));
                        }else if(fieldType.equals(Short.class)) {
                            field.set(bean, Short.valueOf(propValue));
                        }else if(fieldType.equals(String.class)) {
                            field.set(bean, propValue);
                        }else if(fieldType.equals(Date.class)) {
                            //尝试多种格式
                            for(String format : dateFormats) {
                                try {
                                    field.set(bean, new SimpleDateFormat(format).parse(propValue));
                                    break;
                                }catch(Exception e) {
                                    continue;
                                }
                            }
                        }else if(fieldType.equals(BigDecimal.class)) {
                            field.set(bean, new BigDecimal(propValue));
                        }
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private String parseKey(String key){
        if(key == null) return null;
        int start = 2;
        int end = key.indexOf("}");
        if(checkKey(key,start,end)){
            return key.substring(start,end);
        }
        return null;
    }

    private boolean checkKey(String key,int start,int end){
        return start!=-1&&end!=-1&&key.length()>start&&key.length()>=end;
    }

    private class DependencyDescriptorClass extends DependencyDescriptor{

        Object bean;
        boolean require;
        Field field;

        public DependencyDescriptorClass(Object bean,Field field,boolean require){
            super(field,require);
            this.bean = bean;
            this.field = field;
            this.require = require;
        }

        public Object getBean() {
            return bean;
        }
    }

}
