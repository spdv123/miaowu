package com.way.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.way.app.XXApp;
import com.way.consts.setting;
import com.way.xx.R;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

/**
 * Created by deva on 16/7/27.
 */
public class FnsAdapter extends BaseAdapter {
    private LayoutInflater inflater;
    private int currentPage = 0;
    private Map<String, Integer> mFnsMap;
    private List<Integer> fnsList = new ArrayList<Integer>();
    private List<String> fnsName = new ArrayList<String>();

    public FnsAdapter(Context context, int currentPage) {
        // TODO Auto-generated constructor stub
        this.inflater = LayoutInflater.from(context);
        this.currentPage = currentPage;
        mFnsMap = XXApp.getInstance().getFnsMap();
        initData();
    }

    private void initData() {
        for(Map.Entry<String, Integer> entry:mFnsMap.entrySet()){
            fnsList.add(entry.getValue());
            fnsName.add(entry.getKey());
        }
    }

    @Override
    public int getCount() {
        return setting.FNS_EACH_PAGE + 1;
    }

    @Override
    public Object getItem(int position) {
        return fnsList.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @SuppressWarnings("deprecation")
    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        // TODO Auto-generated method stub
        ViewHolder viewHolder;
        if (convertView == null) {
            viewHolder = new ViewHolder();
            convertView = inflater.inflate(R.layout.fns, null, false);
            viewHolder.fnsIV = (ImageView) convertView
                    .findViewById(R.id.fns_iv);
            viewHolder.fnstxt = (TextView) convertView
                    .findViewById(R.id.fns_txt);
            viewHolder.fnslay = (LinearLayout) convertView
                    .findViewById(R.id.fns_layout);
            convertView.setTag(viewHolder);
        } else {
            viewHolder = (ViewHolder) convertView.getTag();
        }

            int count = setting.FNS_EACH_PAGE * currentPage + position;
            if (count < setting.TOTAL_FNS) {
                viewHolder.fnsIV.setImageResource(fnsList.get(count));
                viewHolder.fnstxt.setText(fnsName.get(count).replace("#", ""));
            } else {
                viewHolder.fnsIV.setImageDrawable(null);
                viewHolder.fnsIV.setVisibility(View.INVISIBLE);
                viewHolder.fnstxt.setText(null);
                viewHolder.fnstxt.setVisibility(View.INVISIBLE);
                viewHolder.fnslay.setVisibility(View.INVISIBLE);
            }

        return convertView;
    }

    public static class ViewHolder {
        ImageView fnsIV;
        TextView fnstxt;
        LinearLayout fnslay;
    }
}
