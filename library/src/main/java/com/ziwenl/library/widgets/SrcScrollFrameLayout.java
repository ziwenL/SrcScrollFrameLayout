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
import android.widget.FrameLayout;

import androidx.annotation.ColorInt;
import androidx.annotation.IntDef;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.ziwenl.library.R;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * PackageName : com.ziwenl.library.widgets
 * Author : Ziwen Lan
 * Date : 2020/5/13
 * Time : 11:23
 * Introduction :仿小红书登陆页面背景图无限滚动 FrameLayout
 * 功能特点：
 * 1.将选择的图片按比例缩放填满当前 View 高度
 * 2.背景图片缩放后宽/高度小于当前 View 宽/高度时自动复制黏贴直到占满当前 View 宽/高度，以此来达到无限滚动效果
 * 3.可通过自定义属性 speed 调整滚动速度，提供 slow、ordinary 和 fast 选项，也可自行填入 int 值，值越大滚动速度越快，建议 1 ≤ speed ≤ 50
 * 4.可通过自定义属性 maskLayerColor 设置遮罩层颜色，建议带透明度
 * 5.提供 startScroll 和 stopScroll 方法控制开始/停止滚动
 * 6.可通过自定义属性 scrollOrientation 设置滚动方向，可设置为上移、下移、左移或右移
 *
 * @Deprecated 建议使用最新的 kotlin 版 {@link SrcLoopScrollFrameLayout}，后续 Java 版本可能将放弃维护
 */
@Deprecated
public class SrcScrollFrameLayout extends FrameLayout {
    /**
     * 滚动方向
     * 0:往上滚出
     * 1:往下滚出
     * 2:往左滚出
     * 3:往右滚出
     */
    public final static int OUT_SLIDE_TOP = 0;
    public final static int OUT_SLIDE_BOTTOM = 1;
    public final static int OUT_SLIDE_LEFT = 2;
    public final static int OUT_SLIDE_RIGHT = 3;

    @IntDef({OUT_SLIDE_TOP, OUT_SLIDE_BOTTOM, OUT_SLIDE_LEFT, OUT_SLIDE_RIGHT})
    @Retention(RetentionPolicy.SOURCE)
    public @interface ScrollOrientation {
    }

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
     * 滚动方向，默认往上滚出
     */
    @ScrollOrientation
    private int mScrollOrientation;
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
        mScrollOrientation = array.getInteger(R.styleable.SrcScrollFrameLayout_scrollOrientation, OUT_SLIDE_TOP);
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
            //调整色彩模式进行质量压缩
            Bitmap compressBitmap = bitmap.copy(Bitmap.Config.RGB_565, true);
            //缩放 Bitmap
            mSrcBitmap = scaleBitmap(compressBitmap);
            //计算至少需要几个 bitmap 才能填满当前 view
            mBitmapCount = scrollOrientationIsVertical() ?
                    getMeasuredHeight() / mSrcBitmap.getHeight() + 1
                    :
                    getMeasuredWidth() / mSrcBitmap.getWidth() + 1;
            if (!compressBitmap.isRecycled()) {
                compressBitmap.isRecycled();
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
        int length = scrollOrientationIsVertical() ? mSrcBitmap.getHeight() : mSrcBitmap.getWidth();
        int measuredHeight = getMeasuredHeight();
        int measuredWidth = getMeasuredWidth();
        if (length + mPanDistance != 0) {
            //第一张图片未完全滚出屏幕
            mMatrix.reset();
            switch (mScrollOrientation) {
                case OUT_SLIDE_TOP:
                    mMatrix.postTranslate(0, mPanDistance);
                    break;
                case OUT_SLIDE_BOTTOM:
                    mMatrix.postTranslate(0f, measuredHeight - length - mPanDistance);
                    break;
                case OUT_SLIDE_LEFT:
                    mMatrix.postTranslate(mPanDistance, 0);
                    break;
                case OUT_SLIDE_RIGHT:
                    mMatrix.postTranslate(measuredWidth - length - mPanDistance, 0f);
                    break;

            }
            canvas.drawBitmap(mSrcBitmap, mMatrix, mPaint);
        }
        if (length + mPanDistance < (scrollOrientationIsVertical() ? measuredHeight : measuredWidth)) {
            //用于补充留白的图片出现在屏幕
            for (int i = 0; i < mBitmapCount; i++) {
                mMatrix.reset();
                switch (mScrollOrientation) {
                    case OUT_SLIDE_TOP:
                        mMatrix.postTranslate(0f, (i + 1) * length + mPanDistance);
                        break;
                    case OUT_SLIDE_BOTTOM:
                        mMatrix.postTranslate(0f, measuredHeight - (i + 2) * length - mPanDistance);
                        break;
                    case OUT_SLIDE_LEFT:
                        mMatrix.postTranslate((i + 1) * length + mPanDistance, 0f);
                        break;
                    case OUT_SLIDE_RIGHT:
                        mMatrix.postTranslate(measuredWidth - (i + 2) * length - mPanDistance, 0f);
                        break;
                }
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
            int length = scrollOrientationIsVertical() ? mSrcBitmap.getHeight() : mSrcBitmap.getWidth();
            if (length + mPanDistance <= 0) {
                //第一张已完全滚出屏幕，重置平移距离
                mPanDistance = 0;
            }
            mPanDistance -= mIntervalIncreaseDistance;
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
        Bitmap compressBitmap;
        if (srcBitmap.getConfig() != Bitmap.Config.RGB_565) {
            compressBitmap = srcBitmap.copy(Bitmap.Config.RGB_565, true);
        } else {
            compressBitmap = srcBitmap;
        }
        //按当前View宽度比例缩放 Bitmap
        mSrcBitmap = scaleBitmap(compressBitmap);
        //计算至少需要几个 bitmap 才能填满当前 view
        mBitmapCount = scrollOrientationIsVertical() ?
                getMeasuredHeight() / mSrcBitmap.getHeight() + 1
                :
                getMeasuredWidth() / mSrcBitmap.getWidth() + 1;
        if (!srcBitmap.isRecycled()) {
            srcBitmap.isRecycled();
            System.gc();
        }
        if (!compressBitmap.isRecycled()) {
            compressBitmap.isRecycled();
            System.gc();
        }
        if (oldScrollStatus) {
            startScroll();
        }
    }

    /**
     * 判断是否为竖直滚动
     */
    private boolean scrollOrientationIsVertical() {
        return mScrollOrientation == OUT_SLIDE_TOP || mScrollOrientation == OUT_SLIDE_BOTTOM;
    }

    /**
     * 设置滚动方向
     * @param scrollOrientation
     */
    public void setScrollOrientation(@ScrollOrientation int scrollOrientation) {
        mPanDistance = 0;
        mScrollOrientation = scrollOrientation;
        if (mSrcBitmap != null) {
            if (mDrawable != null && (mDrawable instanceof BitmapDrawable)) {
                Bitmap bitmap = ((BitmapDrawable) mDrawable).getBitmap();
                if (!bitmap.isRecycled()) {
                    setSrcBitmap(bitmap);
                    return;
                }
            }
            setSrcBitmap(mSrcBitmap);
        }
    }

    /**
     * 切换滚动方向
     */
    public void changeScrollOrientation() {
        mPanDistance = 0;
        if (mScrollOrientation == OUT_SLIDE_RIGHT) {
            mScrollOrientation = OUT_SLIDE_TOP;
        } else {
            mScrollOrientation++;
        }
        if (mSrcBitmap != null) {
            if (mDrawable != null && (mDrawable instanceof BitmapDrawable)) {
                Bitmap bitmap = ((BitmapDrawable) mDrawable).getBitmap();
                if (!bitmap.isRecycled()) {
                    setSrcBitmap(bitmap);
                    return;
                }
            }
            setSrcBitmap(mSrcBitmap);
        }
    }

    /**
     * 缩放Bitmap
     */
    private Bitmap scaleBitmap(Bitmap originBitmap) {
        int width = originBitmap.getWidth();
        int height = originBitmap.getHeight();
        int newHeight;
        int newWidth;
        if (scrollOrientationIsVertical()) {
            newWidth = getMeasuredWidth();
            newHeight = newWidth * height / width;
        } else {
            newHeight = getMeasuredHeight();
            newWidth = newHeight * width / height;
        }

        return Bitmap.createScaledBitmap(originBitmap, newWidth, newHeight, true);
    }
}
