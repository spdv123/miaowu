package com.way.util;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.NoSuchAlgorithmException;
import java.util.logging.LogRecord;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Matrix;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.Editable;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.CharacterStyle;
import android.text.style.ClickableSpan;
import android.text.style.ForegroundColorSpan;
import android.text.style.ImageSpan;
import android.util.Log;
import android.util.TypedValue;
import android.view.View;
import android.widget.Toast;

import com.way.activity.ChatActivity;
import com.way.app.XXApp;
import com.way.consts.setting;
import com.way.exception.XXAdressMalformedException;
import com.way.thread.ImageThread;

public class XMPPHelper {
	private static final Pattern EMOTION_URL = Pattern.compile("\\[(\\S+?)\\]");

	public static void verifyJabberID(String jid)
			throws XXAdressMalformedException {
		if (jid != null) {
			Pattern p = Pattern
					.compile("(?i)[a-z0-9\\-_\\.]++@[a-z0-9\\-_]++(\\.[a-z0-9\\-_]++)++");
			Matcher m = p.matcher(jid);

			if (!m.matches()) {
				throw new XXAdressMalformedException(
						"Configured Jabber-ID is incorrect!");
			}
		} else {
			throw new XXAdressMalformedException("Jabber-ID wasn't set!");
		}
	}

	public static void verifyJabberID(Editable jid)
			throws XXAdressMalformedException {
		verifyJabberID(jid.toString());
	}

	public static int tryToParseInt(String value, int defVal) {
		int ret;
		try {
			ret = Integer.parseInt(value);
		} catch (NumberFormatException ne) {
			ret = defVal;
		}
		return ret;
	}

	public static int getEditTextColor(Context ctx) {
		TypedValue tv = new TypedValue();
		boolean found = ctx.getTheme().resolveAttribute(
				android.R.attr.editTextColor, tv, true);
		if (found) {
			// SDK 11+
			return ctx.getResources().getColor(tv.resourceId);
		} else {
			// SDK < 11
			return ctx.getResources().getColor(
					android.R.color.primary_text_light);
		}
	}

	public static String splitJidAndServer(String account) {
		if (!account.contains("@"))
			return account;
		String[] res = account.split("@");
		String userName = res[0];
		return userName;
	}


    /*
    static Handler image_handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            Bundle data = msg.getData();
            String val = data.getString("value");
            Log.i("mylog", "请求结果为-->" + val);
            // TODO
            // UI界面的更新等相关操作
        }
    };
    /**
     * 图片下载相关的子线程
     ///
    static Runnable imageTask = new Runnable() {
        private String URL;

        public  void setURL(String url) {
            URL = url;
        }

        @Override
        public void run() {
            // TODO
            // 在这里进行 http request.网络请求相关操作
            Message msg = new Message();
            Bundle data = new Bundle();
            data.putString("value", "请求结果");
            msg.setData(data);
            image_handler.sendMessage(msg);
        }
    };
    */

	public static SpannableString AddClickSpan(SpannableString s) {
        ImageSpan[] image_spans = s.getSpans(0, s.length(), ImageSpan.class);
        for (ImageSpan span : image_spans) {
            final String image_src = span.getSource();
            final int start = s.getSpanStart(span);
            final int end = s.getSpanEnd(span);
            ClickableSpan click_span = new ClickableSpan() {
                @Override
                public void onClick(View widget) {
                    Log.d("SpanClick", image_src);
                }
            };
            ClickableSpan[] click_spans = s.getSpans(start, end, ClickableSpan.class);
            if(click_spans.length != 0) {
                // remove all click spans
                for(ClickableSpan c_span : click_spans) {
                    s.removeSpan(c_span);
                }
            }
            s.setSpan(click_span, start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        }
        return s;
    }

    public static String getImageSource(String message) {
        if(message.contains("#IMAGE#")) {
            try {
                String ImageURL = message.replace("#IMAGE#", "");
                final String ImageFile;
                ImageFile = MD5.getMD5(ImageURL);
                File f = new File(setting.savePath, ImageFile);
                if (!f.exists()) {
                    return null;
                }
                return setting.savePath + ImageFile;
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return null;
    }

    // 处理字符串图片
    //  11111 用Handler处理下载图片,(想办法避免重复下载?),保存到hash
	// Todo 动态表情gif
    public static CharSequence convertStringToImage(
            Context context, String message, boolean small) {
        SpannableString value = SpannableString.valueOf("#");
        //必须先设置一个值
        String ImageURL = message.replace("#IMAGE#", "");
        try {
            final String ImageFile = MD5.getMD5(ImageURL);
            Log.d("FileName", ImageFile);
            File f = new File(setting.savePath, ImageFile);
            File f_fail = new File(setting.savePath, ImageFile + ".FAIL");
            if(f_fail.exists()) {
                value = SpannableString.valueOf("图片加载失败");
                value.setSpan(new ForegroundColorSpan(Color.RED), 0, value.length(),
                        Spannable.SPAN_INCLUSIVE_EXCLUSIVE);
                return value;
            }
            if (!f.exists()) {
                value = SpannableString.valueOf("图片加载中...");
                value.setSpan(new ForegroundColorSpan(Color.BLUE), 0, value.length(),
                        Spannable.SPAN_INCLUSIVE_EXCLUSIVE);
                // 新建Runnable
                ImageThread mThread = new ImageThread();
                mThread.setURL(ImageURL, ImageFile);
                //mThread.run();
                new Thread(mThread).start();
                return value;
            } else {
                Bitmap bitmap = BitmapFactory.decodeFile(setting.savePath+ImageFile);
                // 过小可以适当缩放
                if(bitmap.getWidth() < setting.screenWidth / 3) {
                    Matrix matrix = new Matrix();
                    float scale = (float) (setting.screenWidth / 3.0 / bitmap.getWidth());
                    matrix.postScale(scale, scale);
                    bitmap = Bitmap.createBitmap(bitmap,0,0,bitmap.getWidth(),bitmap.getHeight(),matrix,true);
                }
                //Log.d("Bitmapped", bitmap.toString());
                ImageSpan localImageSpan = new ImageSpan(context,
                        bitmap, ImageSpan.ALIGN_BASELINE);
                value.setSpan(localImageSpan, 0, 1,
                        Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                //value = AddClickSpan(value);
                //Log.d("Bitvalue", value.toString());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return value;
    }
	/**
	 * 处理字符串中的表情
	 * 
	 * @param context
	 * @param message
	 *            传入的需要处理的String
	 * @param small
	 *            是否需要小图片
	 * @return
	 */
	public static CharSequence convertNormalStringToSpannableString(
			Context context, String message, boolean small) {
        // 处理图片URL
        if(message.contains("#IMAGE#")) {
            return convertStringToImage(context, message, small);
        }

		String hackTxt;
		if (message.startsWith("[") && message.endsWith("]")) {
			hackTxt = message + " ";
		} else {
			hackTxt = message;
		}
		SpannableString value = SpannableString.valueOf(hackTxt);

		Matcher localMatcher = EMOTION_URL.matcher(value);
		while (localMatcher.find()) {
			String str2 = localMatcher.group(0);
			//Log.d("regx", str2);
			int k = localMatcher.start();
			int m = localMatcher.end();
			if (m - k < 8) {
				if (XXApp.getInstance().getFaceMap().containsKey(str2)) {
					int face = XXApp.getInstance().getFaceMap().get(str2);
					Bitmap bitmap = BitmapFactory.decodeResource(
							context.getResources(), face);
					if (bitmap != null) {
						if (small) {
							int rawHeigh = bitmap.getHeight();
							int rawWidth = bitmap.getHeight();
							int newHeight = 60;
							int newWidth = 60;
							// 计算缩放因子
							float heightScale = ((float) newHeight) / rawHeigh;
							float widthScale = ((float) newWidth) / rawWidth;
							// 新建立矩阵
							Matrix matrix = new Matrix();
							matrix.postScale(heightScale, widthScale);
							// 设置图片的旋转角度
							// matrix.postRotate(-30);
							// 设置图片的倾斜
							// matrix.postSkew(0.1f, 0.1f);
							// 将图片大小压缩
							// 压缩后图片的宽和高以及kB大小均会变化
							bitmap = Bitmap.createBitmap(bitmap, 0, 0,
									rawWidth, rawHeigh, matrix, true);
						}
						ImageSpan localImageSpan = new ImageSpan(context,
								bitmap, ImageSpan.ALIGN_BASELINE);
						value.setSpan(localImageSpan, k, m,
								Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
					}
				}
			}
		}
		return value;
	}

}
