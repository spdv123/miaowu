package com.way.activity;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.media.Image;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.provider.ContactsContract;
import android.text.util.Linkify;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.way.consts.msgExtraType;
import com.way.consts.petstr;
import com.way.consts.setting;
import com.way.fns.Petting;
import com.way.swipeback.SwipeBackActivity;
import com.way.util.L;
import com.way.util.MD5;
import com.way.util.MaterialDialog;
import com.way.util.PreferenceConstants;
import com.way.util.PreferenceUtils;
import com.way.util.T;
import com.way.view.PetToast;
import com.way.xx.R;

/**
 * Created by deva on 16/7/30.
 */
public class PetActivity extends SwipeBackActivity {
    Context petContext;
    ImageView pet_head;
    TextView pet_power, pet_intel, pet_spirit, pet_speed, pet_name;
    ImageButton pet_feed, pet_enemy;
    Petting petting;
    String key;
    PetToast petToast;
    LinearLayout pet_attr_layout;
    MaterialDialog mMaterialDialog;
    int headRes, enemyHeadRes;
    String enemy = null;
    boolean isEnemyShow = false;

    View enemyView;
    ImageView enemy_head;
    TextView enemy_power, enemy_intel, enemy_spirit, enemy_speed, enemy_name, enemy_owner;

    Handler petHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {
                case msgExtraType.PET_REFRESH:
                    petRefresh(msg.getData());
                    break;
                case msgExtraType.PET_FEED:
                    petFeed(msg.getData());
                    break;
                case msgExtraType.PET_CHANGE_NAME:
                    petChangeName(msg.getData());
                    break;
                case msgExtraType.PET_ENEMY_REFRESH:
                    petEnemyRefresh(msg.getData());
                    break;
                default:
                    break;
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.petting);
        petContext = this;
        Intent intent = getIntent();
        enemy = intent.getStringExtra("enemy");
        //Log.d("get enemy", enemy);
        initView();
    }

    private void initView() {
        pet_attr_layout = (LinearLayout) findViewById(R.id.pet_attr_layout);
        pet_head = (ImageView) findViewById(R.id.pet_head);
        pet_name = (TextView) findViewById(R.id.pet_name);
        pet_power = (TextView) findViewById(R.id.pet_power);
        pet_speed = (TextView) findViewById(R.id.pet_speed);
        pet_spirit = (TextView) findViewById(R.id.pet_spirit);
        pet_intel = (TextView) findViewById(R.id.pet_intel);
        pet_feed = (ImageButton) findViewById(R.id.pet_feed);
        petToast = new PetToast(petContext, R.drawable.qzone_edit_face_drawable);
        pet_feed.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                DisableButton(pet_feed);
                petting.Feed();
            }
        });
        pet_enemy = (ImageButton) findViewById(R.id.pet_enemy);
        pet_enemy.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showEnemy();
            }
        });
        pet_name.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                changeName(view);
            }
        });
        // test resource
        pet_head.setImageResource(R.drawable.qzone_edit_face_drawable);
        pet_name.setText("昵称加载中...");
        String usr = PreferenceUtils.getPrefString(PetActivity.this,
                PreferenceConstants.ACCOUNT, "");
        usr = usr.split("@")[0];
        String password = PreferenceUtils.getPrefString(
                PetActivity.this, PreferenceConstants.PASSWORD, "");
        key = usr + "@" + MD5.FNVHash1(password);
        petting = new Petting(petHandler, key);
        initEnemyView();
        mMaterialDialog  = new MaterialDialog(this);
        //L.d("calced key", key);
        DisableButton(pet_feed);
        DisableButton(pet_enemy);
    }

    private void initEnemyView() {
        LayoutInflater layoutInflater = getLayoutInflater();
        enemyView = layoutInflater.inflate(R.layout.pet_enemy, null);
        enemy_head = (ImageView) enemyView.findViewById(R.id.pet_enemy_head);
        enemy_name = (TextView) enemyView.findViewById(R.id.pet_enemy_name);
        enemy_power = (TextView) enemyView.findViewById(R.id.pet_enemy_power);
        enemy_speed = (TextView) enemyView.findViewById(R.id.pet_enemy_speed);
        enemy_spirit = (TextView) enemyView.findViewById(R.id.pet_enemy_spirit);
        enemy_intel = (TextView) enemyView.findViewById(R.id.pet_enemy_intel);
        enemy_owner = (TextView) enemyView.findViewById(R.id.pet_enemy_owner);
    }

    private void DisableButton(ImageButton btn) {
        btn.setEnabled(false);
        Drawable drw = btn.getBackground();
        drw.setColorFilter(Color.GRAY, PorterDuff.Mode.SRC_IN);
        btn.setBackground(drw);
    }

    private void EnableButton(ImageButton btn) {
        btn.setEnabled(true);
        Drawable drw = btn.getBackground();
        drw.clearColorFilter();
        btn.setBackground(drw);
    }

    // 宠物刷新事件
    private void petRefresh(Bundle bundle) {
        String result = bundle.getString(petstr.Result);
        if(result == null || result.equals(petstr.Fail)) {
            petToast.showShort(bundle.getString(petstr.Reason));
            return;
        }
        String new_name = bundle.getString(petstr.Name);
        int power = bundle.getInt(petstr.Power);
        int spirit = bundle.getInt(petstr.Spirit);
        int speed = bundle.getInt(petstr.Speed);
        int intel = bundle.getInt(petstr.Intel);

        // 没关系,这里的error只在Head是0才会出现,详见SelectFns
        int pet_head_icon = bundle.getInt(petstr.Head);
        pet_name.setText(petstr._Nick + new_name);
        pet_intel.setText(petstr._Intel + intel);
        pet_power.setText(petstr._Power + power);
        pet_spirit.setText(petstr._Spirit + spirit);
        pet_speed.setText(petstr._Speed + speed);
        setHeadRes(pet_head_icon);

        pet_head.setColorFilter(Color.rgb(0, 0xb3, 0xe0), PorterDuff.Mode.DST_ATOP);
        //Log.d("color set", "c" + Color.BLUE);
        EnableButton(pet_feed);
        EnableButton(pet_enemy);
    }

    // 敌人宠物刷新事件
    private void petEnemyRefresh(Bundle bundle) {
        String result = bundle.getString(petstr.Result);
        if(result == null || result.equals(petstr.Fail)) {
            petToast.showShort(bundle.getString(petstr.Reason));
            return;
        }
        String new_name = bundle.getString(petstr.Name);
        String owner = bundle.getString(petstr.Owner);
        int power = bundle.getInt(petstr.Power);
        int spirit = bundle.getInt(petstr.Spirit);
        int speed = bundle.getInt(petstr.Speed);
        int intel = bundle.getInt(petstr.Intel);

        // 没关系,这里的error只在Head是0才会出现,详见SelectFns
        int pet_head_icon = bundle.getInt(petstr.Head);
        enemy_name.setText(petstr._Nick + new_name);
        enemy_owner.setText(petstr._Owner+owner);
        enemy_intel.setText(petstr._Intel + intel);
        enemy_power.setText(petstr._Power + power);
        enemy_spirit.setText(petstr._Spirit + spirit);
        enemy_speed.setText(petstr._Speed + speed);
        setEnemyHeadRes(pet_head_icon);
    }

    // 宠物喂养事件
    private void petFeed(Bundle bundle) {
        EnableButton(pet_feed);
        String result = bundle.getString(petstr.Result);
        if(result == null || result.equals(petstr.Fail)) {
            petToast.showShort(bundle.getString(petstr.Reason));
            return;
        }
        String attr = bundle.getString("attr");
        int value = bundle.getInt("value");
        petToast.showShort(petstr.FeedSuccess);
        petToast.addAttr(attr, value);
        petHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                petting.Refresh();
            }
        }, 2100);
    }

    // 宠物换名字事件
    private void petChangeName(Bundle bundle) {
        String result = bundle.getString(petstr.Result);
        if(result == null || result.equals(petstr.Fail)) {
            petToast.showShort(bundle.getString(petstr.Reason));
            return;
        }
        petToast.showShort("昵称更改成功~");
        petting.Refresh();
    }

    private void setHeadRes(int pet_head_icon) {
        try {
            Resources res = getResources();
            int head_res = res.getIdentifier("pet_head_" + pet_head_icon + "_nor", "drawable", getPackageName());
            Log.d("head_res", "p" + head_res);
            headRes = head_res;
            pet_head.setImageResource(headRes);
            petToast.setHead(headRes);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void setEnemyHeadRes(int pet_head_icon) {
        try {
            Resources res = getResources();
            int head_res = res.getIdentifier("pet_head_" + pet_head_icon + "_nor", "drawable", getPackageName());
            Log.d("head_res", "p" + head_res);
            enemyHeadRes = head_res;
            enemy_head.setImageResource(enemyHeadRes);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void changeName(View view) {
        mMaterialDialog = new MaterialDialog(this);
        // 必须先inflator,用来找view
        LayoutInflater layoutInflater = getLayoutInflater();
        View nView = layoutInflater.inflate(R.layout.pet_change_name, null);
        LinearLayout container = (LinearLayout)nView.findViewById(R.id.name_changer);
        final EditText newname = (EditText)nView.findViewById(R.id.new_name);
        String curname = pet_name.getText().toString();
        curname = curname.substring(3);
        newname.setText(curname);
        Button OK = (Button)nView.findViewById(R.id.name_OK);
        Button cancel = (Button)nView.findViewById(R.id.name_cancel);
        OK.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String name = newname.getText().toString();
                mMaterialDialog.dismiss();
                if(name == null || name.equals("")) {
                    petToast.showShort("昵称不能为空~");
                    return;
                }
                petting.ChangeName(name);
            }
        });
        cancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mMaterialDialog.dismiss();
            }
        });
        mMaterialDialog.setView(nView);
        mMaterialDialog.setCanceledOnTouchOutside(true);
        mMaterialDialog.show();
    }

    private void showEnemy() {
        mMaterialDialog = new MaterialDialog(this);
        // Todo Why must re-init EnemyView?
        initEnemyView();
        //mMaterialDialog.setContentView(enemyView);
        mMaterialDialog.setView(enemyView);
        isEnemyShow = true;
        mMaterialDialog.setCanceledOnTouchOutside(true);
        //*
        mMaterialDialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialogInterface) {
                isEnemyShow = false;
            }
        });
        //*/
        petting.EnemyRefresh(enemy);
        mMaterialDialog.show();
    }
}
