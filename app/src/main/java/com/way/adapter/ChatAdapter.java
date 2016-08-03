package com.way.adapter;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;

import com.way.activity.ChatActivity;
import com.way.activity.ImageShow;
import com.way.consts.setting;
import com.way.db.ChatProvider;
import com.way.db.ChatProvider.ChatConstants;
import com.way.thread.ImageThread;
import com.way.util.L;
import com.way.util.PreferenceConstants;
import com.way.util.PreferenceUtils;
import com.way.util.T;
import com.way.util.TimeUtil;
import com.way.util.XMPPHelper;
import com.way.xx.R;

public class ChatAdapter extends SimpleCursorAdapter {

	private static final int DELAY_NEWMSG = 2000;
	private Context mContext;
	private LayoutInflater mInflater;

	public ChatAdapter(Context context, Cursor cursor, String[] from) {
		// super(context, android.R.layout.simple_list_item_1, cursor, from,
		// to);
		super(context, 0, cursor, from, null);
		mContext = context;
		mInflater = LayoutInflater.from(context);
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		Cursor cursor = this.getCursor();
		L.d("cursor pos", "" + position);
        // 反序显示
		cursor.moveToPosition(cursor.getCount()-1-position);

		long dateMilliseconds = cursor.getLong(cursor
				.getColumnIndex(ChatProvider.ChatConstants.DATE));

		int _id = cursor.getInt(cursor
				.getColumnIndex(ChatProvider.ChatConstants._ID));
		String date = TimeUtil.getChatTime(dateMilliseconds);
		String message = cursor.getString(cursor
				.getColumnIndex(ChatProvider.ChatConstants.MESSAGE));
		int come = cursor.getInt(cursor
				.getColumnIndex(ChatProvider.ChatConstants.DIRECTION));// 消息来自
		boolean from_me = (come == ChatConstants.OUTGOING);
		String jid = cursor.getString(cursor
				.getColumnIndex(ChatProvider.ChatConstants.JID));
		int delivery_status = cursor.getInt(cursor
				.getColumnIndex(ChatProvider.ChatConstants.DELIVERY_STATUS));
		ViewHolder viewHolder;
		if (convertView == null
				|| convertView.getTag(R.drawable.ic_launcher + come) == null) {
			if (come == ChatConstants.OUTGOING) {
				convertView = mInflater.inflate(R.layout.chat_item_right,
						parent, false);
			} else {
				convertView = mInflater.inflate(R.layout.chat_item_left, null);
			}
			viewHolder = buildHolder(convertView);
			convertView.setTag(R.drawable.ic_launcher + come, viewHolder);
			convertView
					.setTag(R.string.app_name, R.drawable.ic_launcher + come);
		} else {
			viewHolder = (ViewHolder) convertView.getTag(R.drawable.ic_launcher
					+ come);
		}

		if (!from_me && delivery_status == ChatConstants.DS_NEW) {
			markAsReadDelayed(_id, DELAY_NEWMSG);
		}

		bindViewData(viewHolder, date, from_me, jid, message, delivery_status);
		return convertView;
	}

	private void markAsReadDelayed(final int id, int delay) {
		new Handler().postDelayed(new Runnable() {
			@Override
			public void run() {
				markAsRead(id);
			}
		}, delay);
	}

	/**
	 * 标记为已读消息
	 * 
	 * @param id
	 */
	private void markAsRead(int id) {
		Uri rowuri = Uri.parse("content://" + ChatProvider.AUTHORITY + "/"
				+ ChatProvider.TABLE_NAME + "/" + id);
		L.d("markAsRead: " + rowuri);
		ContentValues values = new ContentValues();
		values.put(ChatConstants.DELIVERY_STATUS, ChatConstants.DS_SENT_OR_READ);
		mContext.getContentResolver().update(rowuri, values, null, null);
	}

	private void bindViewData(ViewHolder holder, String date, boolean from_me,
			String from, String message, int delivery_status) {
		holder.avatar.setBackgroundResource(R.drawable.login_default_avatar);
		if (from_me
				&& !PreferenceUtils.getPrefBoolean(mContext,
						PreferenceConstants.SHOW_MY_HEAD, true)) {
			holder.avatar.setVisibility(View.GONE);
		}
		// 如果显示自己头像会超长,这句话设置不超长
        holder.content.setMaxWidth(Math.max(setting.screenWidth - 250, 10));
        // 传进程过去
        //ImageThread image_save = new ImageThread();
		holder.content.setText(XMPPHelper.convertNormalStringToSpannableString(
				mContext, message, false));
        holder.imageSource = XMPPHelper.getImageSource(message);
		holder.time.setText(date);

	}

	private ViewHolder buildHolder(View convertView) {
		final ViewHolder holder = new ViewHolder();
		holder.content = (TextView) convertView.findViewById(R.id.textView2);
		holder.time = (TextView) convertView.findViewById(R.id.datetime);
		holder.avatar = (ImageView) convertView.findViewById(R.id.icon);
		holder.content.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //Log.d("Image Source", holder.imageSource);
                if(holder.imageSource != null) {
                    try {
                        //生成Intent对象（包含了ctivity间传的Data，param）;相当于一个请求
                        Intent intent = new Intent();
                        //键值对
                        intent.putExtra("bitmap", holder.imageSource);
                        //从此ctivity传到另一Activity
                        intent.setClass(mContext, ImageShow.class);
                        //启动另一个Activity
                        mContext.startActivity(intent);
                    } catch (Exception e) {
                        e.printStackTrace();
                        T.showShort(mContext, "图片错误");
                    }
                }
            }
        });
        holder.content.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                String text = holder.content.getText().toString();
                Log.d("Long click holder", text);
                ClipboardManager myClipboard;
                myClipboard = (ClipboardManager)mContext.getSystemService(mContext.CLIPBOARD_SERVICE);
                if(text.equals("#")) {
                    if(holder.imageSource != null) {
                        text = "#IMAGE#" + holder.imageSource;
                    }
                }
                myClipboard.setText(text);
                T.showShort(mContext, "文本已复制");
                return true;
            }
        });
		return holder;
	}

	private static class ViewHolder {
		TextView content;
		TextView time;
		ImageView avatar;
        String imageSource;
	}

}
