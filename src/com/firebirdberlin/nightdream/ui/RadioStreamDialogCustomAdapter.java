package com.firebirdberlin.nightdream.ui;

import com.android.volley.RequestQueue;
import com.android.volley.toolbox.ImageLoader;
import com.android.volley.toolbox.NetworkImageView;
import com.android.volley.toolbox.Volley;
import com.firebirdberlin.nightdream.R;
import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import androidx.collection.LruCache;

import java.util.List;
import java.util.Locale;

public class RadioStreamDialogCustomAdapter extends ArrayAdapter<RadioStreamDialogItem> {

    RequestQueue mRequestQueue;
    ImageLoader mImageLoader;
    Context context;

    public RadioStreamDialogCustomAdapter(Context context, int resourceId,
                                 List<RadioStreamDialogItem> items) {
        super(context, resourceId, items);
        this.context = context;
        mRequestQueue = Volley.newRequestQueue(context);
        mImageLoader = new ImageLoader(mRequestQueue, new ImageLoader.ImageCache() {
            private final LruCache<String, Bitmap> mCache = new LruCache<String, Bitmap>(10);
            public void putBitmap(String url, Bitmap bitmap) {
                mCache.put(url, bitmap);
            }
            public Bitmap getBitmap(String url) {
                return mCache.get(url);
            }
        });
    }

    private class ViewHolder {
        NetworkImageView imageView;
        TextView txtName;
        TextView txtDesc;
    }

    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder holder;
        RadioStreamDialogItem item = getItem(position);

        LayoutInflater mInflater = (LayoutInflater) context
                .getSystemService(Activity.LAYOUT_INFLATER_SERVICE);
        if (convertView == null) {
            convertView = mInflater.inflate(R.layout.radio_stream_list_adapter, null);
            holder = new ViewHolder();
            holder.txtName = convertView.findViewById(R.id.name);
            holder.txtDesc = convertView.findViewById(R.id.description);
            holder.imageView = convertView.findViewById(R.id.icon);
            convertView.setTag(holder);
        } else
            holder = (ViewHolder) convertView.getTag();

        holder.txtName.setText(getText(item));
        holder.txtDesc.setText(getDesc(item));

       if (item.getImageUrl() != null && !item.getImageUrl().isEmpty()) {
           holder.imageView.setImageUrl(item.getImageUrl(), mImageLoader);
        }

        holder.imageView.setContentDescription(item.getName());
        holder.imageView.setDefaultImageResId(R.drawable.ic_audiotrack_dark);
        holder.imageView.setErrorImageResId(R.drawable.ic_audiotrack_dark);

        return convertView;
    }

    public String getText(RadioStreamDialogItem item) {
        String countryCode = (item.getIsCountrySelected()) ? "" : String.format("%s ", item.getCountryCode());
        if (countryCode.equals(" ")) {
            countryCode = "";
        }
        return String.format(Locale.getDefault(), "%s %s",
                countryCode, item.getName());
    }

    public String getDesc(RadioStreamDialogItem item) {
        String streamOffline =
                (item.getIsOnline())
                        ? ""
                        : String.format(" - %s", this.context.getResources().getString(R.string.radio_stream_offline));
        String bitRate =
                (item.getBitrate() == 0) ? "???" : String.format("%s ", item.getBitrate());
        return String.format(Locale.getDefault(), "(%s kbit/s) %s",
                bitRate, streamOffline);
    }



}
