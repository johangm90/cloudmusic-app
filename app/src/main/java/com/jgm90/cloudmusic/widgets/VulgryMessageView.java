package com.jgm90.cloudmusic.widgets;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.core.content.ContextCompat;

import com.jgm90.cloudmusic.R;

public class VulgryMessageView extends RelativeLayout {

    ImageView imageView;
    TextView textView;

    public VulgryMessageView(Context context, AttributeSet attrs) {
        super(context, attrs);
        TypedArray typedArray = context.obtainStyledAttributes(attrs,
                R.styleable.VulgryMessageView, 0, 0);
        Drawable img = typedArray.getDrawable(R.styleable.VulgryMessageView_imageDrawable);
        String text = typedArray.getString(R.styleable.VulgryMessageView_messageText);
        typedArray.recycle();
        LayoutInflater inflater = LayoutInflater.from(context);
        inflater.inflate(R.layout.vulgry_message_view, this);
        imageView = this.findViewById(R.id.message_img);
        if (img != null) {
            imageView.setImageDrawable(img);
        }
        textView = this.findViewById(R.id.message_text);
        if (text != null) {
            textView.setText(text);
        }
    }

    public void setDrawable(Drawable drawable) {
        imageView.setImageDrawable(drawable);
    }

    public void setDrawable(int drawable) {
        Drawable img = ContextCompat.getDrawable(getContext(), drawable);
        imageView.setImageDrawable(img);
    }

    public void setText(String message) {
        textView.setText(message);
    }

    public void setText(int message) {
        textView.setText(message);
    }
}
