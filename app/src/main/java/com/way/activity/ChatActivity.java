package com.way.activity;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.RunnableFuture;

import android.app.Activity;
import android.content.AsyncQueryHandler;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.database.ContentObserver;
import android.database.Cursor;
import android.database.DataSetObserver;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.provider.ContactsContract;
import android.provider.MediaStore;
import android.support.v4.view.ViewPager;
import android.support.v4.view.ViewPager.OnPageChangeListener;
import android.text.Editable;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.text.style.ImageSpan;
import android.util.Log;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnKeyListener;
import android.view.View.OnTouchListener;
import android.view.ViewGroup.LayoutParams;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.GridView;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListAdapter;
import android.widget.TextView;
import android.widget.Toast;

import com.way.adapter.ChatAdapter;
import com.way.adapter.FaceAdapter;
import com.way.adapter.FacePageAdeapter;
import com.way.adapter.FnsAdapter;
import com.way.adapter.FnsPageAdapter;
import com.way.app.XXApp;
import com.way.consts.msgExtraType;
import com.way.consts.setting;
import com.way.db.ChatProvider;
import com.way.db.ChatProvider.ChatConstants;
import com.way.db.RosterProvider;
import com.way.fns.SelectFns;
import com.way.fns.UploadImage;
import com.way.service.IConnectionStatusCallback;
import com.way.service.XXService;
import com.way.swipeback.SwipeBackActivity;
import com.way.util.L;
import com.way.util.PreferenceConstants;
import com.way.util.PreferenceUtils;
import com.way.util.StatusMode;
import com.way.util.T;
import com.way.util.XMPPHelper;
import com.way.view.CirclePageIndicator;
import com.way.xlistview.MsgListView;
import com.way.xlistview.MsgListView.IXListViewListener;
import com.way.xx.R;

public class ChatActivity extends SwipeBackActivity implements OnTouchListener,
		OnClickListener, IXListViewListener, IConnectionStatusCallback {
	public static final String INTENT_EXTRA_USERNAME = ChatActivity.class
			.getName() + ".username";// 昵称对应的key
	private MsgListView mMsgListView;// 对话ListView
	private ViewPager mFaceViewPager;// 表情选择ViewPager
    private ViewPager mFnsViewPager;// 功能选择ViewPager
	private int mCurrentPage = 0;// 当前表情页
	private int mFnsCurrentPage = 0;// 当前功能页
	private int mIsFaceShow = 0;// 是否显示表情
	private Button mSendMsgBtn;// 发送消息button
	private ImageButton mFaceSwitchBtn;// 切换键盘和表情的button
    private ImageButton mFnsSwitchBtn;// 切换键盘和功能的button
	private TextView mTitleNameView;// 标题栏
	private ImageView mTitleStatusView;
	private EditText mChatEditText;// 消息输入框
	private LinearLayout mFaceRoot;// 表情父容器
    private LinearLayout mFnsRoot;// 功能父容器
	private WindowManager.LayoutParams mWindowNanagerParams;
	private InputMethodManager mInputMethodManager;
	private List<String> mFaceMapKeys;// 表情对应的字符串数组
	private String mWithJabberID = null;// 当前聊天用户的ID
    public Context chatContext;
    public Activity chatActivity;
    public int messagesToShow;

	private static final String[] PROJECTION_FROM = new String[] {
			ChatProvider.ChatConstants._ID, ChatProvider.ChatConstants.DATE,
			ChatProvider.ChatConstants.DIRECTION,
			ChatProvider.ChatConstants.JID, ChatProvider.ChatConstants.MESSAGE,
			ChatProvider.ChatConstants.DELIVERY_STATUS };// 查询字段

	private ContentObserver mContactObserver = new ContactObserver();// 联系人数据监听，主要是监听对方在线状态
	private XXService mXxService;// Main服务
	ServiceConnection mServiceConnection = new ServiceConnection() {

		@Override
		public void onServiceConnected(ComponentName name, IBinder service) {
			mXxService = ((XXService.XXBinder) service).getService();
			mXxService.registerConnectionStatusCallback(ChatActivity.this);
			// 如果没有连接上，则重新连接xmpp服务器
			if (!mXxService.isAuthenticated()) {
				String usr = PreferenceUtils.getPrefString(ChatActivity.this,
						PreferenceConstants.ACCOUNT, "");
				String password = PreferenceUtils.getPrefString(
						ChatActivity.this, PreferenceConstants.PASSWORD, "");
				mXxService.Login(usr, password);
			}
		}

		@Override
		public void onServiceDisconnected(ComponentName name) {
			mXxService.unRegisterConnectionStatusCallback();
			mXxService = null;
		}

	};

	/**
	 * 解绑服务
	 */
	private void unbindXMPPService() {
		try {
			unbindService(mServiceConnection);
		} catch (IllegalArgumentException e) {
			L.e("Service wasn't bound!");
		}
	}

	/**
	 * 绑定服务
	 */
	private void bindXMPPService() {
		Intent mServiceIntent = new Intent(this, XXService.class);
		Uri chatURI = Uri.parse(mWithJabberID);
		mServiceIntent.setData(chatURI);
		bindService(mServiceIntent, mServiceConnection, BIND_AUTO_CREATE);
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.chat);
        chatContext = this;
        chatActivity = this;
        messagesToShow = 10;
		initData();// 初始化数据
		initView();// 初始化view
		initFacePage();// 初始化表情
        initFnsPage();// 初始化功能
		setChatWindowAdapter(true);// 初始化对话数据
		getContentResolver().registerContentObserver(
				RosterProvider.CONTENT_URI, true, mContactObserver);// 开始监听联系人数据库
	}

	@Override
	protected void onResume() {
		super.onResume();
		updateContactStatus();// 更新联系人状态
	}

	@Override
	protected void onPause() {
		super.onPause();
	}

	// 查询联系人数据库字段
	private static final String[] STATUS_QUERY = new String[] {
			RosterProvider.RosterConstants.STATUS_MODE,
			RosterProvider.RosterConstants.STATUS_MESSAGE, };

	private void updateContactStatus() {
		Cursor cursor = getContentResolver().query(RosterProvider.CONTENT_URI,
				STATUS_QUERY, RosterProvider.RosterConstants.JID + " = ?",
				new String[] { mWithJabberID }, null);
		int MODE_IDX = cursor
				.getColumnIndex(RosterProvider.RosterConstants.STATUS_MODE);
		int MSG_IDX = cursor
				.getColumnIndex(RosterProvider.RosterConstants.STATUS_MESSAGE);

		if (cursor.getCount() == 1) {
			cursor.moveToFirst();
			int status_mode = cursor.getInt(MODE_IDX);
			String status_message = cursor.getString(MSG_IDX);
			L.d("contact status changed: " + status_mode + " " + status_message);
			mTitleNameView.setText(XMPPHelper.splitJidAndServer(getIntent()
					.getStringExtra(INTENT_EXTRA_USERNAME)));
			int statusId = StatusMode.values()[status_mode].getDrawableId();
			if (statusId != -1) {// 如果对应离线状态
				// Drawable icon = getResources().getDrawable(statusId);
				// mTitleNameView.setCompoundDrawablesWithIntrinsicBounds(icon,
				// null,
				// null, null);
				mTitleStatusView.setImageResource(statusId);
				mTitleStatusView.setVisibility(View.VISIBLE);
			} else {
				mTitleStatusView.setVisibility(View.GONE);
			}
		}
		cursor.close();
	}

	/**
	 * 联系人数据库变化监听
	 * 
	 */
	private class ContactObserver extends ContentObserver {
		public ContactObserver() {
			super(new Handler());
		}

		public void onChange(boolean selfChange) {
			L.d("ContactObserver.onChange: " + selfChange);
			updateContactStatus();// 联系人状态变化时，刷新界面
		}
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		if (hasWindowFocus())
			unbindXMPPService();// 解绑服务
		getContentResolver().unregisterContentObserver(mContactObserver);
	}

	@Override
	public void onWindowFocusChanged(boolean hasFocus) {
		super.onWindowFocusChanged(hasFocus);
		// 窗口获取到焦点时绑定服务，失去焦点将解绑
		if (hasFocus)
			bindXMPPService();
		else
			unbindXMPPService();
	}

	private void initData() {
		mWithJabberID = getIntent().getDataString().toLowerCase();// 获取聊天对象的id
		// 将表情map的key保存在数组中
		Set<String> keySet = XXApp.getInstance().getFaceMap().keySet();
		mFaceMapKeys = new ArrayList<String>();
		mFaceMapKeys.addAll(keySet);
	}

    Handler showMoreHandler = new Handler() {
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case msgExtraType.SHOW_MORE:
                    messagesToShow += 10;
                    setChatWindowAdapter(false);
                    break;
                default:
                    break;
            }
            super.handleMessage(msg);
        }
    };

	/**
	 * 设置聊天的Adapter
	 */
	private void setChatWindowAdapter(final boolean down) {
        String getrows = "((select count(*) from " + ChatProvider.TABLE_NAME + ") - 10)";
		String selection = ChatConstants.JID + "='" + mWithJabberID + "'";
        String orderby = ChatConstants.DATE + " desc"  + " limit " + messagesToShow;

		// 异步查询数据库
		new AsyncQueryHandler(getContentResolver()) {

			@Override
			protected void onQueryComplete(int token, Object cookie,
					Cursor cursor) {
				// ListAdapter adapter = new ChatWindowAdapter(cursor,
				// PROJECTION_FROM, PROJECTION_TO, mWithJabberID);
                L.d("Query Complete", "times");
				final ListAdapter adapter = new ChatAdapter(ChatActivity.this,
						cursor, PROJECTION_FROM);
                // 给adapter注册一个观察者,观察者具有onchanged事件
                // http://blog.csdn.net/chunqiuwei/article/details/39934169
                adapter.registerDataSetObserver(new DataSetObserver() {
                    @Override
                    public void onChanged() {
                        super.onChanged();
						mMsgListView.setSelection(adapter.getCount() - 1);
                    }
                });
				mMsgListView.setAdapter(adapter);
                if(down) {
                    mMsgListView.setSelection(adapter.getCount() - 1);
                } else {
                    int pos = adapter.getCount()  - messagesToShow + 10;
                    pos = Math.max(pos, 0);
                    mMsgListView.setSelection(pos);
                }
			}

		}.startQuery(0, null, ChatProvider.CONTENT_URI, PROJECTION_FROM,
				selection, null, orderby);
		// 同步查询数据库，建议停止使用,如果数据庞大时，导致界面失去响应
		// Cursor cursor = managedQuery(ChatProvider.CONTENT_URI,
		// PROJECTION_FROM,
		// selection, null, null);
		// ListAdapter adapter = new ChatWindowAdapter(cursor, PROJECTION_FROM,
		// PROJECTION_TO, mWithJabberID);
		// mMsgListView.setAdapter(adapter);
		// mMsgListView.setSelection(adapter.getCount() - 1);
	}

	private void initView() {
		mInputMethodManager = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
		mWindowNanagerParams = getWindow().getAttributes();

		mMsgListView = (MsgListView) findViewById(R.id.msg_listView);
		// 触摸ListView隐藏表情和输入法
		mMsgListView.setOnTouchListener(this);
		mMsgListView.setPullLoadEnable(false);
		mMsgListView.setXListViewListener(this);
        mMsgListView.setShowMoreHandler(showMoreHandler);
		mSendMsgBtn = (Button) findViewById(R.id.send);
		mFaceSwitchBtn = (ImageButton) findViewById(R.id.face_switch_btn);
        mFnsSwitchBtn = (ImageButton) findViewById(R.id.fns_switch_btn);
		mChatEditText = (EditText) findViewById(R.id.input);
		mFaceRoot = (LinearLayout) findViewById(R.id.face_ll);
        mFnsRoot = (LinearLayout) findViewById(R.id.fns_ll);
		mFaceViewPager = (ViewPager) findViewById(R.id.face_pager);
        mFnsViewPager = (ViewPager) findViewById(R.id.fns_pager);
		mChatEditText.setOnTouchListener(this);
		mTitleNameView = (TextView) findViewById(R.id.ivTitleName);
		mTitleStatusView = (ImageView) findViewById(R.id.ivTitleStatus);
		mChatEditText.setOnKeyListener(new OnKeyListener() {

			@Override
			public boolean onKey(View v, int keyCode, KeyEvent event) {
				// TODO Auto-generated method stub
				if (keyCode == KeyEvent.KEYCODE_BACK) {
					if (mWindowNanagerParams.softInputMode == WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE
							|| mIsFaceShow!=0) {
						closeRoots();
						mIsFaceShow = 0;
						// imm.showSoftInput(msgEt, 0);
						return true;
					}
				}
				return false;
			}
		});
		mChatEditText.addTextChangedListener(new TextWatcher() {

			@Override
			public void onTextChanged(CharSequence s, int start, int before,
					int count) {
				// TODO Auto-generated method stub
			}

			@Override
			public void beforeTextChanged(CharSequence s, int start, int count,
					int after) {
				// TODO Auto-generated method stub

			}

			@Override
			public void afterTextChanged(Editable s) {
				// TODO Auto-generated method stub
				if (s.length() > 0) {
					mSendMsgBtn.setEnabled(true);
				} else {
					mSendMsgBtn.setEnabled(false);
				}
			}
		});
		mFaceSwitchBtn.setOnClickListener(this);
		mSendMsgBtn.setOnClickListener(this);
        mFnsSwitchBtn.setOnClickListener(this);
	}

	@Override
	public void onRefresh() {
		mMsgListView.stopRefresh();
	}

	@Override
	public void onLoadMore() {
		// do nothing
	}

    // 关闭所有roots
    private void closeRoots() {
        mFaceRoot.setVisibility(View.GONE);
        mFnsRoot.setVisibility(View.GONE);
    }

    // 关闭输入法
    private void closeInput() {
        mInputMethodManager.hideSoftInputFromWindow(
                mChatEditText.getWindowToken(), 0);
        try {
            Thread.sleep(80);// 解决此时会黑一下屏幕的问题
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
	@Override
	public void onClick(View v) {
		switch (v.getId()) {
		case R.id.face_switch_btn:
			if (mIsFaceShow != 1) {
				closeInput();
                closeRoots();
				mFaceRoot.setVisibility(View.VISIBLE);
				mFaceSwitchBtn.setImageResource(R.drawable.aio_keyboard);
				mIsFaceShow = 1;
			} else {
                closeRoots();
				//mInputMethodManager.showSoftInput(mChatEditText, 0);
				mFaceSwitchBtn
						.setImageResource(R.drawable.qzone_edit_face_drawable);
				mIsFaceShow = 0;
			}
			break;
		case R.id.send:// 发送消息
			sendMessageIfNotNull();
			break;
        case R.id.fns_switch_btn:
            if (mIsFaceShow != 2) {
                closeInput();
                closeRoots();
                mFnsRoot.setVisibility(View.VISIBLE);
                mIsFaceShow = 2;
            } else {
                closeRoots();
                mIsFaceShow = 0;
            }
            break;
		default:
			break;
		}
	}

    public void sendMessageByHandler(String msg) {
        if(msg.length() >= 1) {
            //mMsgListView.setSelection(mMsgListView.getBottom());
            mMsgListView.setSelection(mMsgListView.getAdapter().getCount() - 1);
            if (mXxService != null) {
                mXxService.sendMessage(mWithJabberID, msg);
                if (!mXxService.isAuthenticated())
                    T.showShort(this, "消息已经保存随后发送");
            }
        }
    }

    Handler msg_sender = new Handler() {
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case msgExtraType.SEND:
                    Bundle data = msg.getData();
                    String message = data.getString("msg");
                    sendMessageByHandler(message);
                    break;
				case msgExtraType.TOAST:
                    Bundle data2 = msg.getData();
                    String message2 = data2.getString("msg");
                    T.showShort(chatContext, message2);
                    break;
                default:
                    break;
            }
            super.handleMessage(msg);
        }
    };

	private void sendMessageIfNotNull() {
		if (mChatEditText.getText().length() >= 1) {
            // 加上这句话让窗口发送消息随之滚动
            //mMsgListView.setSelection(mMsgListView.getBottom());
            mMsgListView.setSelection(mMsgListView.getAdapter().getCount() - 1);
			if (mXxService != null) {
				mXxService.sendMessage(mWithJabberID, mChatEditText.getText()
						.toString());
				if (!mXxService.isAuthenticated())
					T.showShort(this, "消息已经保存随后发送");
			}
			mChatEditText.setText(null);
			mSendMsgBtn.setEnabled(false);
		}
	}

	@Override
	public boolean onTouch(View v, MotionEvent event) {
		switch (v.getId()) {
		case R.id.msg_listView:
			// 此处输入法隐藏,重新设置显示大小
			mInputMethodManager.hideSoftInputFromWindow(
					mChatEditText.getWindowToken(), 0);
			mFaceSwitchBtn
					.setImageResource(R.drawable.qzone_edit_face_drawable);
			closeRoots();
			mIsFaceShow = 0;
			break;
		case R.id.input:
			mInputMethodManager.showSoftInput(mChatEditText, 0);
			mFaceSwitchBtn
					.setImageResource(R.drawable.qzone_edit_face_drawable);
			closeRoots();
			mIsFaceShow = 0;
			mMsgListView.setSelection(mMsgListView.getAdapter().getCount() - 1);
			break;

		default:
			break;
		}
		return false;
	}

	private void initFacePage() {
		// TODO Auto-generated method stub
		List<View> lv = new ArrayList<View>();
		for (int i = 0; i < setting.FACE_PAGES; ++i)
			lv.add(getFaceGridView(i));
		FacePageAdeapter adapter = new FacePageAdeapter(lv);
		mFaceViewPager.setAdapter(adapter);
		mFaceViewPager.setCurrentItem(mCurrentPage);
		CirclePageIndicator indicator = (CirclePageIndicator) findViewById(R.id.indicator);
		indicator.setViewPager(mFaceViewPager);
		adapter.notifyDataSetChanged();
		mFaceRoot.setVisibility(View.GONE);
		indicator.setOnPageChangeListener(new OnPageChangeListener() {

			@Override
			public void onPageSelected(int arg0) {
				mCurrentPage = arg0;
			}

			@Override
			public void onPageScrolled(int arg0, float arg1, int arg2) {
				// do nothing
			}

			@Override
			public void onPageScrollStateChanged(int arg0) {
				// do nothing
			}
		});

	}

    private void initFnsPage() {
        // TODO Auto-generated method stub
        List<View> lv = new ArrayList<View>();
        for (int i = 0; i < setting.FNS_PAGES; ++i)
            lv.add(getFnsGridView(i));
        FnsPageAdapter adapter = new FnsPageAdapter(lv);
        mFnsViewPager.setAdapter(adapter);
        mFnsViewPager.setCurrentItem(mFnsCurrentPage);
        CirclePageIndicator indicator = (CirclePageIndicator) findViewById(R.id.fns_indicator);
        indicator.setViewPager(mFnsViewPager);
        adapter.notifyDataSetChanged();
        mFnsRoot.setVisibility(View.GONE);
        indicator.setOnPageChangeListener(new OnPageChangeListener() {

            @Override
            public void onPageSelected(int arg0) {
                mFnsCurrentPage = arg0;
            }

            @Override
            public void onPageScrolled(int arg0, float arg1, int arg2) {
                // do nothing
            }

            @Override
            public void onPageScrollStateChanged(int arg0) {
                // do nothing
            }
        });

    }


    private GridView getFaceGridView(int i) {
        // TODO Auto-generated method stub
        GridView gv = new GridView(this);
        gv.setNumColumns(7);
        gv.setSelector(new ColorDrawable(Color.TRANSPARENT));// 屏蔽GridView默认点击效果
        gv.setBackgroundColor(Color.TRANSPARENT);
        gv.setCacheColorHint(Color.TRANSPARENT);
        gv.setHorizontalSpacing(1);
        gv.setVerticalSpacing(1);
        gv.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT,
                LayoutParams.MATCH_PARENT));
        gv.setGravity(Gravity.CENTER);
        gv.setAdapter(new FaceAdapter(this, i));
        gv.setOnTouchListener(forbidenScroll());
        gv.setOnItemClickListener(new OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> arg0, View arg1, int arg2,
                                    long arg3) {
                // TODO Auto-generated method stub
                if (arg2 == setting.FACES_EACH_PAGE) {// 删除键的位置
                    int selection = mChatEditText.getSelectionStart();
                    String text = mChatEditText.getText().toString();
                    if (selection > 0) {
                        String text2 = text.substring(selection - 1);
                        if ("]".equals(text2)) {
                            int start = text.lastIndexOf("[");
                            int end = selection;
                            mChatEditText.getText().delete(start, end);
                            return;
                        }
                        mChatEditText.getText()
                                .delete(selection - 1, selection);
                    }
                } else {
                    int count = mCurrentPage * setting.FACES_EACH_PAGE + arg2;
                    Log.d("emoji count", String.valueOf(count));
                    // 注释的部分，在EditText中显示字符串
                    // String ori = msgEt.getText().toString();
                    // int index = msgEt.getSelectionStart();
                    // StringBuilder stringBuilder = new StringBuilder(ori);
                    // stringBuilder.insert(index, keys.get(count));
                    // msgEt.setText(stringBuilder.toString());
                    // msgEt.setSelection(index + keys.get(count).length());

                    // 下面这部分，在EditText中显示表情
                    Bitmap bitmap = BitmapFactory.decodeResource(
                            getResources(), (Integer) XXApp.getInstance()
                                    .getFaceMap().values().toArray()[count]);
                    if (bitmap != null) {
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
                        Bitmap newBitmap = Bitmap.createBitmap(bitmap, 0, 0,
                                rawWidth, rawHeigh, matrix, true);
                        ImageSpan imageSpan = new ImageSpan(ChatActivity.this,
                                newBitmap);
                        String emojiStr = mFaceMapKeys.get(count);
                        SpannableString spannableString = new SpannableString(
                                emojiStr);
                        spannableString.setSpan(imageSpan,
                                emojiStr.indexOf('['),
                                emojiStr.indexOf(']') + 1,
                                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                        mChatEditText.append(spannableString);
                    } else {
                        String ori = mChatEditText.getText().toString();
                        int index = mChatEditText.getSelectionStart();
                        StringBuilder stringBuilder = new StringBuilder(ori);
                        stringBuilder.insert(index, mFaceMapKeys.get(count));
                        mChatEditText.setText(stringBuilder.toString());
                        mChatEditText.setSelection(index
                                + mFaceMapKeys.get(count).length());
                    }
                }
            }
        });
        return gv;
    }

    private GridView getFnsGridView(int i) {
        // TODO Auto-generated method stub
        GridView gv = new GridView(this);
        //*
        gv.setNumColumns(3);
        gv.setSelector(new ColorDrawable(Color.TRANSPARENT));// 屏蔽GridView默认点击效果
        gv.setBackgroundColor(Color.TRANSPARENT);
        gv.setCacheColorHint(Color.TRANSPARENT);
        gv.setHorizontalSpacing(1);
        gv.setVerticalSpacing(1);
        gv.setLayoutParams(new LayoutParams(LayoutParams.WRAP_CONTENT,
                LayoutParams.WRAP_CONTENT));
        gv.setGravity(Gravity.CENTER);
        //*/
        gv.setAdapter(new FnsAdapter(this, i));
        gv.setOnTouchListener(forbidenScroll());
        gv.setOnItemClickListener(new OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> arg0, View arg1, int arg2,
                                    long arg3) {
                // TODO Auto-generated method stub

                int count = mFnsCurrentPage * setting.FNS_EACH_PAGE + arg2;
                //Toast.makeText(chatContext, "count is" + count, Toast.LENGTH_LONG).show();
                Log.d("count is", String.valueOf(count));
                SelectFns sfns = new SelectFns();
				sfns.setEnemy(mWithJabberID.split("@")[0]);
                sfns.use(count,msg_sender, chatContext, chatActivity);
            }
        });
        return gv;
    }

    //获取图片路径 响应startActivityForResult
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        //打开图片
        if(resultCode==RESULT_OK && requestCode==msgExtraType.UPLOAD_IMAGE) {
            Uri uri = data.getData();
            if (!TextUtils.isEmpty(uri.getAuthority())) {
                //查询选择图片
                Cursor cursor = getContentResolver().query(
                        uri,
                        new String[] { MediaStore.Images.Media.DATA },
                        null,
                        null,
                        null);
                //返回 没找到选择图片
                if (null == cursor) {
                    return;
                }
                //光标移动至开头 获取图片路径
                cursor.moveToFirst();
                String ImagePath = cursor.getString(cursor
                        .getColumnIndex(MediaStore.Images.Media.DATA));
                Log.d("Select Image", ImagePath);
				//==========测试pinch=======
                /*
                //生成Intent对象（包含了ctivity间传的Data，param）;相当于一个请求
                Intent intent=new Intent();
                //键值对
                intent.putExtra("bitmap", ImagePath);
                //从此ctivity传到另一Activity
                intent.setClass(ChatActivity.this, ImageShow.class);
                //启动另一个Activity
                ChatActivity.this.startActivity(intent);
                */
                //==========测试结束=========
                final File imageFile = new File(ImagePath);
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            String uploaded = UploadImage.uploadFile(imageFile, setting.UploadURL);
                            Log.d("Upload Image", uploaded);
                            if (uploaded.equals("0") || uploaded.equals("invalid")) {
								Message testmsg = new Message();
								testmsg.what = msgExtraType.TOAST;
								testmsg.setTarget(msg_sender);
								Bundle bddata = new Bundle();
								bddata.putString("msg", "上传失败");
								testmsg.setData(bddata);
								testmsg.sendToTarget();
                            } else {
                                Message testmsg = new Message();
                                testmsg.what = msgExtraType.SEND;
                                testmsg.setTarget(msg_sender);
                                Bundle bddata = new Bundle();
                                bddata.putString("msg", "#IMAGE#" + uploaded);
                                testmsg.setData(bddata);
                                testmsg.sendToTarget();
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }).start();
            }
        }  //end if 打开图片
    }

	// 防止乱pageview乱滚动
	private OnTouchListener forbidenScroll() {
		return new OnTouchListener() {
			public boolean onTouch(View v, MotionEvent event) {
				if (event.getAction() == MotionEvent.ACTION_MOVE) {
					return true;
				}
				return false;
			}
		};
	}

	@Override
	public void connectionStatusChanged(int connectedState, String reason) {
		// TODO Auto-generated method stub

	}

}
