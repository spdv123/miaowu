package com.way.activity;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.view.View;

import com.way.view.PinchImageView;
import com.way.xx.R;

/**
 * Created by deva on 16/7/29.
 */
public class ImageShow extends Activity {
    PinchImageView recv_image;
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.imageview);
        recv_image = (PinchImageView) findViewById(R.id.recv_image_view);
        recv_image.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
        try {
            Intent intent = getIntent();
            String bmp_path = intent.getStringExtra("bitmap");
            Bitmap bmp = BitmapFactory.decodeFile(bmp_path);
            recv_image.setImageBitmap(bmp);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
