package com.moovaa.plugins.scanner;

/**
 * Created by rams on 05.03.2015.
 */



import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.BitmapDrawable;
import android.os.Handler;
import android.util.AttributeSet;
import android.widget.ImageView;



public class AnimatedView extends ImageView {
    private Context mContext;
    int x = -1;
    int y = -1;
    private int xVelocity = 10;
    private int yVelocity = 0;
    private Handler h;
    private final int FRAME_RATE = 10;

    public AnimatedView(Context context, AttributeSet attrs)  {
        super(context, attrs);
        mContext = context;
        h = new Handler();
    }
    private Runnable r = new Runnable() {
        @Override
        public void run() {
            invalidate();
        }
    };
    protected void onDraw(Canvas c) {
        BitmapDrawable ball = (BitmapDrawable) mContext.getResources().getDrawable(R.drawable.scanline);
        int height = this.getHeight();
        Bitmap copy = Bitmap.createScaledBitmap(ball.getBitmap(), (int) 30, (int) height*2, false);

        if (x<0 && y <0) {
            x = this.getWidth()/2;
            y = this.getHeight()/2 - copy.getHeight()/2;
        } else {
            x += xVelocity;
            //     y += yVelocity;
            if ((x > this.getWidth() - copy.getWidth()) || (x < 0)) {
                xVelocity = xVelocity*-1;
            }
            if ((y > this.getHeight() - copy.getHeight()) || (y < 0)) {
                yVelocity = yVelocity*-1;
            }
        }
        c.drawBitmap(copy, x, y, null);
        h.postDelayed(r, FRAME_RATE);
    }
}