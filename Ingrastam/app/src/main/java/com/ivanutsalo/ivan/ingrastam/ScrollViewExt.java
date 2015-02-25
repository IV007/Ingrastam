package com.ivanutsalo.ivan.ingrastam;

/**
 * Created by Ivan on 2/24/2015.
 */

import android.content.Context;
import android.util.AttributeSet;
import android.widget.ScrollView;

public class ScrollViewExt extends ScrollView {
    private ScrollViewListener scrollViewListener = null;

    public ScrollViewExt(Context context){
        super(context);
    }

    public ScrollViewExt(Context context, AttributeSet attributeSet, int defStyle){
        super(context, attributeSet, defStyle);
    }

    public ScrollViewExt(Context context, AttributeSet attributeSet){
        super(context, attributeSet);
    }

    public void setScrollViewListener(ScrollViewListener scrollViewListener){
        this.scrollViewListener = scrollViewListener;
    }

    @Override
    protected void onScrollChanged(int l, int t, int oldl, int oldt) {
        super.onScrollChanged(l, t, oldl, oldt);
        if(scrollViewListener != null){
            scrollViewListener.onScrollChanged(this, l, t, oldl, oldt);
        }
    }
}

