package com.izuche.processor;


import com.izuche.interfaces.impl.ValueManagerListner;
import com.taobao.diamond.manager.DiamondManager;
import com.taobao.diamond.manager.ManagerListener;
import com.taobao.diamond.manager.impl.DefaultDiamondManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.PropertiesPropertySource;
import org.springframework.core.env.PropertySource;

import java.util.Properties;

public class DiamondEnvironmentPostProcessor implements EnvironmentPostProcessor {

    private static final Logger log = LoggerFactory.getLogger(DiamondEnvironmentPostProcessor.class);

    // 先调用
    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        // 读取
        try {
            String activeProfiles = environment.getActiveProfiles()[0];
            log.info("当前环境:"+activeProfiles);
            System.setProperty("current.profile",activeProfiles);
            PropertySource<?> propertySource = environment.getPropertySources().get("applicationConfig: [classpath:/application.properties]");
            Object isDiamond = propertySource.getProperty("spring.izuche.diamond");
            Object groupAndDataID = propertySource.getProperty("spring.izuche.diamond.groupa-data-id");

            if(isDiamond==null||groupAndDataID==null){
                log.info("============>未配置diamond,不使用");
                return;
            }
            String[] groupsAndDataID = groupAndDataID.toString().split(",");
            boolean b = Boolean.parseBoolean(isDiamond.toString());
            if(b){
                for (int i = 0; i < groupsAndDataID.length; i++) {
                    String groupAndID = groupsAndDataID[i];
                    // 暂时不能动态修改
                    ManagerListener listenser = new ValueManagerListner();
                    String group = groupsAndDataID[i].split("@")[0];
                    String dataId = groupsAndDataID[i].split("@")[1];
                    DiamondManager manager = new DefaultDiamondManager(group,dataId,listenser);
                    Properties properties = manager.getPropertiesConfigureInfomation(5000);
                    // 添加配置文件
                    PropertiesPropertySource propertiesPropertySource = new PropertiesPropertySource(groupAndID,properties);
                    environment.getPropertySources().addLast(propertiesPropertySource);
                }
            }else{
                  log.info("diamond未启用");
            }
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("获取不到配置");
        }

    }

}
