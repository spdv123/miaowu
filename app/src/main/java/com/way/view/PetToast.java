package com.way.view;

import android.content.Context;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.way.consts.petstr;
import com.way.xx.R;

/**
 * Created by deva on 16/7/31.
 */
public class PetToast {
    Toast toast, attrToast;
    Context mContext;
    int res;
    View view, attrView;
    TextView toast_text, attr_text;
    ImageView toast_head;


    public PetToast(Context context, int resID) {
        mContext = context;
        res = resID;
        toast = new Toast(mContext);
        attrToast = new Toast(mContext);
        // 加载自定义布局
        LayoutInflater inflater = (LayoutInflater)mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);;
        view=inflater.inflate(R.layout.pet_toast, null);
        attrView = inflater.inflate(R.layout.pet_add_attr, null);
        attr_text = (TextView) attrView.findViewById(R.id.pet_attr);
        toast_text = (TextView) view.findViewById(R.id.toast_text);
        toast_head = (ImageView) view.findViewById(R.id.toast_head);
        try {
            toast_head.setImageResource(resID);
        } catch (Exception e) {
            e.printStackTrace();
        }
        toast.setGravity(Gravity.CENTER_HORIZONTAL| Gravity.BOTTOM, 0, 80);
        toast.setDuration(Toast.LENGTH_SHORT);
        toast.setView(view);
        attrToast.setGravity(Gravity.CENTER_HORIZONTAL| Gravity.BOTTOM, 0, 80);
        attrToast.setDuration(Toast.LENGTH_SHORT);
        attrToast.setView(attrView);
    }

    public void setHead(int resID) {
        try {
            res = resID;
            toast_head.setImageResource(res);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void showShort(String text) {
        toast_text.setText(text);
        toast.setDuration(Toast.LENGTH_SHORT);
        toast.show();
    }

    public void showLong(String text) {
        toast_text.setText(text);
        toast.setDuration(Toast.LENGTH_LONG);
        toast.show();
    }

    public void addAttr(String type, int num) {
        String text = String.valueOf(num);
        if(num > 0) {
            text = "+" + text;
        }
        attr_text.setText(text);

        int addY = 120, powerY = 365;
        if(type.equals(petstr.Power)) {
            attr_text.setTextColor(petstr.P_Red);
            attrToast.setGravity(Gravity.CENTER_HORIZONTAL| Gravity.TOP, -180, powerY + 0 * addY);
            attrToast.show();
        } else if(type.equals(petstr.Intel)) {
            attr_text.setTextColor(petstr.I_Blue);
            attrToast.setGravity(Gravity.CENTER_HORIZONTAL| Gravity.TOP, -180, powerY + 1 * addY);
            attrToast.show();
        } else if(type.equals(petstr.Spirit)) {
            attr_text.setTextColor(petstr.S_Green);
            attrToast.setGravity(Gravity.CENTER_HORIZONTAL| Gravity.TOP, -180, powerY + 3 * addY);
            attrToast.show();
        } else if(type.equals(petstr.Speed)) {
            attr_text.setTextColor(petstr.S_Yellow);
            attrToast.setGravity(Gravity.CENTER_HORIZONTAL| Gravity.TOP, -190, powerY + 2 * addY);
            attrToast.show();
        }
    }
}
