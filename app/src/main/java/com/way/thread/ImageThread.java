package com.way.thread;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;

import com.way.consts.setting;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * Created by deva on 16/7/28.
 */
public class ImageThread implements Runnable {
    private String URL;
    private String md5_name;

    public  void setURL(String url, String md5) {
        URL = url;
        md5_name = md5;
    }

    /** 保存方法 */
    public void saveBitmap(Bitmap bm, String picName) {

        File f = new File(setting.savePath, picName);
        if (f.exists()) {
            return;
            //f.delete();
        }
        try {
            FileOutputStream out = new FileOutputStream(f);
            bm.compress(Bitmap.CompressFormat.PNG, 90, out);
            out.flush();
            out.close();
            Log.i("SAVE_BITMAP", "已经保存");
        } catch (FileNotFoundException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

    }

    /**
     * 获取网络图片资源
     * @param url
     * @return
     */
    public Bitmap getHttpBitmap(String url){
        URL myFileURL;
        Bitmap bitmap=null;
        try{
            myFileURL = new URL(url);
            //获得连接
            HttpURLConnection conn=(HttpURLConnection)myFileURL.openConnection();
            //设置超时时间为6000毫秒，conn.setConnectionTiem(0);表示没有时间限制
            conn.setConnectTimeout(6000);
            //连接设置获得数据流
            conn.setDoInput(true);
            //不使用缓存
            conn.setUseCaches(false);
            //这句可有可无，没有影响
            //conn.connect();
            //得到数据流
            InputStream is = conn.getInputStream();
            //解析得到图片
            bitmap = BitmapFactory.decodeStream(is);
            //关闭数据流
            is.close();
        }catch(Exception e){
            e.printStackTrace();
        }

        return bitmap;

    }

    @Override
    public void run() {
        File f_downloading = new File(
                setting.savePath, md5_name + ".DOWN");
        File f_failed = new File(
                setting.savePath, md5_name + ".FAIL");
        File f_complete = new File(
                setting.savePath, md5_name);
        if(f_complete.exists() || f_failed.exists() ||
                f_downloading.exists()) {
            return;
            //正在下载,已经失败和已经完成直接返回
        }
        try {
            f_downloading.createNewFile();
            Bitmap image = getHttpBitmap(URL);
            saveBitmap(image, md5_name);
            f_downloading.delete();
        } catch (Exception e) {
            e.printStackTrace();
            try {
                f_failed.createNewFile();
                f_complete.delete();
                f_downloading.delete();
            } catch (Exception e1) {
                e1.printStackTrace();
            }
        }
    }
}
