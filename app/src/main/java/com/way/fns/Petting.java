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
    private String key;
    Runnable refresher;

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
                    Bundle bundle = new Bundle();

                    Bundle bundle1 = JSONUtil.fromJson(jsonObject);

                    refreshed.setData(bundle1);
                    refreshed.sendToTarget();
                }catch (Exception e) {
                    e.printStackTrace();
                }
            }
        };
        new Thread(refresher).start();
    }
}
