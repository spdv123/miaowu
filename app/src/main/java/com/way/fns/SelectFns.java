package com.way.fns;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;

import com.way.activity.PetActivity;
import com.way.consts.msgExtraType;

/**
 * Created by deva on 16/7/28.
 */
public class SelectFns {

    public void use(int id, Handler handler, Context context, Activity activity) {
        switch (id) {
            case msgExtraType.FNS_IMAGE:
                Intent intent = new Intent(Intent.ACTION_PICK,
                        android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                activity.startActivityForResult(intent, msgExtraType.UPLOAD_IMAGE);
                break;
            case msgExtraType.FNS_PET:
                Intent intent1 = new Intent();
                intent1.setClass(context, PetActivity.class);
                activity.startActivity(intent1);
                break;
            default:
                break;
        }
    };
}
