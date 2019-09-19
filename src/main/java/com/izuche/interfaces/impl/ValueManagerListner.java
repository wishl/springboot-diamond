package com.izuche.interfaces.impl;

import com.izuche.config.RefreshHolder;
import com.taobao.diamond.manager.ManagerListener;

import java.util.concurrent.Executor;

// 使用的是spring的classLoader
public class ValueManagerListner implements ManagerListener {
    @Override
    public Executor getExecutor() {
        return null;
    }

    @Override
    public void receiveConfigInfo(String configInfo) {
        RefreshHolder.refreshe(configInfo);
    }
}
