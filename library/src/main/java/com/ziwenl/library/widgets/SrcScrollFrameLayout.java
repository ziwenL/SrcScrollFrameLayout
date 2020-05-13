package com.ziwenl.library.widgets;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.util.Log;
import android.widget.FrameLayout;

import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.ziwenl.library.R;

/**
 * PackageName : com.ziwenl.library.widgets
 * Author : Ziwen Lan
 * Date : 2020/5/13
 * Time : 11:23
 * Introduction :仿小红书登陆页面背景图无限滚动 FrameLayout
 * 功能特点：
 * 1.将选择的图片按比例缩放填满当前 View 宽度
 * 2.背景图片缩放后高度小于当前 View 高度时自动复制黏贴直到占满当前 View 高度，以此来达到无限滚动效果
 * 3.可通过自定义属性 speed 调整滚动速度，提供 slow、ordinary 和 fast 选项，也可自行填入 int 值，值越大滚动速度越快，建议 1 ≤ speed ≤ 50
 * 4.可通过自定义属性 maskLayerColor 设置遮罩层颜色，建议带透明度
 * 5.提供 startScroll 和 stopScroll 方法控制开始/停止滚动
 */
public class SrcScrollFrameLayout extends FrameLayout {

    /**
     * 重绘间隔时间
     */
    private final static long DEFAULT_DRAW_INTERVALS_TIME = 5L;
    /**
     * 间隔时间内平移距离
     */
    private float mPanDistance = 0;
    /**
     * 间隔时间内平移增距
     */
    private float mIntervalIncreaseDistance = 0.5f;
    /**
     * 填满当前view所需bitmap个数
     */
    private int mBitmapCount = 0;
    /**
     * 是否开始滚动
     */
    private boolean mIsScroll;
    /**
     * 遮罩层颜色
     */
    @ColorInt
    private int mMaskLayerColor;

    private Drawable mDrawable;
    private Bitmap mSrcBitmap;
    private Paint mPaint;
    private Matrix mMatrix;

    public SrcScrollFrameLayout(@NonNull Context context) {
        this(context, null, 0);
    }

    public SrcScrollFrameLayout(@NonNull Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public SrcScrollFrameLayout(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        TypedArray array = context.getTheme().obtainStyledAttributes(attrs, R.styleable.SrcScrollFrameLayout, defStyleAttr, 0);
        int speed = array.getInteger(R.styleable.SrcScrollFrameLayout_speed, 3);
        mIntervalIncreaseDistance = speed * mIntervalIncreaseDistance;
        mDrawable = array.getDrawable(R.styleable.SrcScrollFrameLayout_src);
        mIsScroll = array.getBoolean(R.styleable.SrcScrollFrameLayout_isScroll, true);
        mMaskLayerColor = array.getColor(R.styleable.SrcScrollFrameLayout_maskLayerColor, Color.TRANSPARENT);
        array.recycle();

        setWillNotDraw(false);
        mPaint = new Paint();
        mMatrix = new Matrix();

    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        if (mDrawable == null || !(mDrawable instanceof BitmapDrawable)) {
            return;
        }
        if (getVisibility() == GONE) {
            return;
        }
        if (w == 0 || h == 0) {
            return;
        }
        if (mSrcBitmap == null) {
            Bitmap bitmap = ((BitmapDrawable) mDrawable).getBitmap();
            //按当前View宽度比例缩放 Bitmap
            mSrcBitmap = scaleBitmap(bitmap, getMeasuredWidth());
            //计算至少需要几个 bitmap 才能填满当前 view
            mBitmapCount = getMeasuredHeight() / mSrcBitmap.getHeight() + 1;
            if (!bitmap.isRecycled()) {
                bitmap.isRecycled();
                System.gc();
            }
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (mSrcBitmap == null) {
            return;
        }
        if (mSrcBitmap.getHeight() + mPanDistance != 0) {
            //第一张图片未完全滚出屏幕
            mMatrix.reset();
            mMatrix.postTranslate(0, mPanDistance);
            canvas.drawBitmap(mSrcBitmap, mMatrix, mPaint);
        }
        if (mSrcBitmap.getHeight() + mPanDistance < getMeasuredHeight()) {
            //用于补充留白的图片出现在屏幕
            for (int i = 0; i < mBitmapCount; i++) {
                mMatrix.reset();
                mMatrix.postTranslate(0, (i + 1) * mSrcBitmap.getHeight() + mPanDistance);
                canvas.drawBitmap(mSrcBitmap, mMatrix, mPaint);
            }
        }
        //绘制遮罩层
        if (mMaskLayerColor != Color.TRANSPARENT) {
            canvas.drawColor(mMaskLayerColor);
        }
        //延时重绘实现滚动效果
        if (mIsScroll) {
            getHandler().postDelayed(mRedrawRunnable, DEFAULT_DRAW_INTERVALS_TIME);
        }
    }

    /**
     * 重绘
     */
    private Runnable mRedrawRunnable = new Runnable() {
        @Override
        public void run() {
            if (mSrcBitmap.getHeight() + mPanDistance <= 0) {
                //第一张已完全滚出屏幕，重置平移距离
                mPanDistance = 0;
            }
            mPanDistance = mPanDistance - mIntervalIncreaseDistance;
            invalidate();
        }
    };

    /**
     * 开始滚动
     */
    public void startScroll() {
        if (mIsScroll) {
            return;
        }
        mIsScroll = true;
        getHandler().postDelayed(mRedrawRunnable, DEFAULT_DRAW_INTERVALS_TIME);
    }

    /**
     * 停止滚动
     */
    public void stopScroll() {
        if (!mIsScroll) {
            return;
        }
        mIsScroll = false;
        getHandler().removeCallbacks(mRedrawRunnable);
    }

    /**
     * 设置背景图 bitmap
     * 通过该方法设置的背景图，当 屏幕翻转/暗黑模式切换 等涉及到 activity 重构的情况出现时，需要在 activity 重构后重新设置背景图
     */
    public void setSrcBitmap(Bitmap srcBitmap) {
        boolean oldScrollStatus = mIsScroll;
        if (oldScrollStatus) {
            stopScroll();
        }
        //按当前View宽度比例缩放 Bitmap
        mSrcBitmap = scaleBitmap(srcBitmap, getMeasuredWidth());
        //计算至少需要几个 bitmap 才能填满当前 view
        mBitmapCount = getMeasuredHeight() / mSrcBitmap.getHeight() + 1;
        if (!srcBitmap.isRecycled()) {
            srcBitmap.isRecycled();
            System.gc();
        }
        if (oldScrollStatus) {
            startScroll();
        }
    }

    /**
     * 缩放Bitmap
     */
    private Bitmap scaleBitmap(Bitmap originBitmap, int newWidth) {
        int width = originBitmap.getWidth();
        int height = originBitmap.getHeight();
        float scaleWidth = (float) newWidth / width;
        float newHeight = scaleWidth * height;
        float scaleHeight = newHeight / height;
        Matrix matrix = new Matrix();
        matrix.postScale(scaleWidth, scaleHeight);
        return Bitmap.createBitmap(originBitmap, 0, 0, width, height, matrix, true);
    }
}
