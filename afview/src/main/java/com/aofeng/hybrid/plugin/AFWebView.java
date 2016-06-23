package com.aofeng.hybrid.plugin;

import android.content.Context;
import android.util.AttributeSet;
import android.webkit.WebView;
import android.widget.ProgressBar;

public class AFWebView extends WebView{

	private ProgressBar mProgressBar;
	
	@SuppressWarnings("deprecation")
	public AFWebView(Context context, AttributeSet attrs) {
		super(context, attrs);
		mProgressBar = new ProgressBar(context, null, android.R.attr.progressBarStyleHorizontal);
		mProgressBar.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, 10, 0, 0));
        addView(mProgressBar);
	}

    @SuppressWarnings("deprecation")
	@Override
    protected void onScrollChanged(int left, int top, int oldl, int oldt) {
        LayoutParams lp = (LayoutParams) mProgressBar.getLayoutParams();
        lp.x = left;
        lp.y = top;
        mProgressBar.setLayoutParams(lp);
        super.onScrollChanged(left, top, oldl, oldt);
    }
    
	public ProgressBar getmProgressBar() {
		return mProgressBar;
	}

    
}
