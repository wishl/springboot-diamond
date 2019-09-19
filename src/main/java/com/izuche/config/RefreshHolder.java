package com.izuche.config;

import com.alibaba.fastjson.JSONObject;
import com.izuche.interfaces.DiamondRefresh;

import java.util.ArrayList;
import java.util.List;

// todo 使用devtools导致加载类的classLoader不同，加载两次
public class RefreshHolder {

    private static List<DiamondRefresh> refreshes = new ArrayList<>();

    static {
        System.out.println("RefreshHolder 被加载，使用的classloder"+RefreshHolder.class.getClassLoader());
    }

    public static boolean addListener(DiamondRefresh refresh){
        boolean result = refreshes.add(refresh);
        return result;
    }

    public static void refreshe(String content){
        System.out.println(JSONObject.toJSONString(refreshes));
        refreshes.stream().forEach(refresh -> {
            refresh.onRefresh(content);
        });
    }

    public static List<DiamondRefresh> listRefreshes(){
        return refreshes;
    }

    static class RefreshTest implements DiamondRefresh{

        private String name = "refreshTest";

        @Override
        public void onRefresh(String content) {
            System.out.println(content);
        }

        public String getName() {
            return name;
        }
    }

}
