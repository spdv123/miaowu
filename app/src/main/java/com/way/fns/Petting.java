package com.way.fns;

/**
 * Created by deva on 16/7/30.
 */
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.ArrayMap;

import com.way.consts.msgExtraType;
import com.way.consts.petstr;
import com.way.util.JSONUtil;

import org.json.JSONObject;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

public class Petting {
    private Handler petHandler;
    private String key, newname, enemyName;
    Runnable refresher, feeder, namechanger, enemyrefresher;

    // 返回方法构成的get参数
    public String f(Map<String, String> map) {
        String res = "k=" + key;
        for(Map.Entry<String, String> entry:map.entrySet()) {
            String tmp = "&" + entry.getKey() + "=" + entry.getValue();
            res += tmp;
        }
        return res;
    }

    public Petting(Handler handler, String k) {
        petHandler = handler;
        key = k;
        initRunnables();
        Refresh();
    }

    public void Refresh() {
        new Thread(refresher).start();
    }

    public void Feed() {
        new Thread(feeder).start();
    }

    public void ChangeName(String name) {
        newname = name;
        new Thread(namechanger).start();
    }

    public void EnemyRefresh(String enemy) {
        enemyName = enemy;
        new Thread(enemyrefresher).start();
    }

    private void initRunnables() {
        refresher  = new Runnable() {
            @Override
            public void run() {
                try {
                    Map<String, String> func = new LinkedHashMap<String, String>();
                    func.put("f", petstr.GetInfo);
                    JSONObject jsonObject = JSONUtil.getJSONFrom(petstr.PetURL + f(func));
                    Message refreshed = new Message();
                    refreshed.setTarget(petHandler);
                    refreshed.what = msgExtraType.PET_REFRESH;

                    Bundle bundle1 = JSONUtil.fromJson(jsonObject);

                    refreshed.setData(bundle1);
                    refreshed.sendToTarget();
                }catch (Exception e) {
                    e.printStackTrace();
                }
            }
        };

        feeder = new Runnable() {
            @Override
            public void run() {
                try{
                    Map<String, String> func = new LinkedHashMap<String, String>();
                    func.put("f", petstr.Feed);
                    JSONObject jsonObject = JSONUtil.getJSONFrom(petstr.PetURL + f(func));
                    Message feeded = new Message();
                    feeded.setTarget(petHandler);
                    feeded.what = msgExtraType.PET_FEED;
                    Bundle bundle1 = JSONUtil.fromJson(jsonObject);

                    feeded.setData(bundle1);
                    feeded.sendToTarget();
                }catch (Exception e) {
                    e.printStackTrace();
                }
            }
        };

        namechanger = new Runnable() {
            @Override
            public void run() {
                try{
                    Map<String, String> func = new LinkedHashMap<String, String>();
                    func.put("f", petstr.ChangeName);
                    func.put("name", newname);
                    JSONObject jsonObject = JSONUtil.getJSONFrom(petstr.PetURL + f(func));
                    Message changed = new Message();
                    changed.setTarget(petHandler);
                    changed.what = msgExtraType.PET_CHANGE_NAME;
                    Bundle bundle1 = JSONUtil.fromJson(jsonObject);

                    changed.setData(bundle1);
                    changed.sendToTarget();
                }catch (Exception e) {
                    e.printStackTrace();
                }
            }
        };

        enemyrefresher = new Runnable() {
            @Override
            public void run() {
                try {
                    Map<String, String> func = new LinkedHashMap<String, String>();
                    func.put("f", petstr.EnemyInfo);
                    func.put(petstr.Owner, enemyName);
                    JSONObject jsonObject = JSONUtil.getJSONFrom(petstr.PetURL + f(func));
                    Message refreshed = new Message();
                    refreshed.setTarget(petHandler);
                    refreshed.what = msgExtraType.PET_ENEMY_REFRESH;
                    Bundle bundle1 = JSONUtil.fromJson(jsonObject);

                    refreshed.setData(bundle1);
                    refreshed.sendToTarget();
                }catch (Exception e) {
                    e.printStackTrace();
                }
            }
        };
    }
}
