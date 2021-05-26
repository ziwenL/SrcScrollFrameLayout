package com.ziwenl.library.widgets

import android.content.Context
import android.graphics.*
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.widget.FrameLayout
import androidx.annotation.ColorInt
import androidx.annotation.IntDef
import com.ziwenl.library.R

/**
 * PackageName : com.ziwenl.library.widgets
 * Author : Ziwen Lan
 * Date : 2021/5/25
 * Time : 16:41
 * Introduction :仿小红书登陆页面背景图无限滚动 FrameLayout kotlin 版
 * 功能特点：
 * 1.将选择的图片按比例缩放填满当前 View 高度
 * 2.背景图片缩放后宽/高度小于当前 View 宽/高度时自动复制黏贴直到占满当前 View 宽/高度，以此来达到无限滚动效果
 * 3.可通过自定义属性 speed 调整滚动速度，提供 slow、ordinary 和 fast 选项，也可自行填入 int 值，值越大滚动速度越快，建议 1 ≤ speed ≤ 50
 * 4.可通过自定义属性 maskLayerColor 设置遮罩层颜色，建议带透明度
 * 5.提供 startScroll 和 stopScroll 方法控制开始/停止滚动
 * 6.可通过自定义属性 scrollOrientation 设置滚动方向，可设置为上移、下移、左移或右移
 */
class SrcLoopScrollFrameLayout : FrameLayout {

    companion object {
        /**
         * 滚动方向常量
         * 0:往上滚出
         * 1:往下滚出
         * 2:往左滚出
         * 3:往右滚出
         */
        private const val OUT_SLIDE_TOP = 0
        private const val OUT_SLIDE_BOTTOM = 1
        private const val OUT_SLIDE_LEFT = 2
        private const val OUT_SLIDE_RIGHT = 3

        /**
         * 重绘间隔时间
         */
        private const val DEFAULT_DRAW_INTERVALS_TIME = 5L
    }

    /**
     * 滚动方向
     */
    @IntDef(OUT_SLIDE_TOP, OUT_SLIDE_BOTTOM, OUT_SLIDE_LEFT, OUT_SLIDE_RIGHT)
    @Retention(AnnotationRetention.SOURCE)
    annotation class ScrollOrientation

    /**
     * 间隔时间内平移距离
     */
    private var mPanDistance = 0f

    /**
     * 间隔时间内平移增距
     */
    private var mIntervalIncreaseDistance = 0.5f

    /**
     * 填满当前view所需bitmap个数
     */
    private var mBitmapCount = 0

    /**
     * 是否开始滚动
     */
    private var mIsScroll = false

    /**
     * 滚动方向，默认往上滚出
     */
    @ScrollOrientation
    private var mScrollOrientation = OUT_SLIDE_TOP

    /**
     * 遮罩层颜色
     */
    @ColorInt
    private var mMaskLayerColor = 0

    private var mDrawable: Drawable? = null
    private var mSrcBitmap: Bitmap? = null
    private lateinit var mPaint: Paint
    private lateinit var mMatrix: Matrix

    constructor(context: Context) : super(context) {
        initView(context, null, 0)
    }

    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs) {
        initView(context, attrs, 0)
    }

    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr
    ) {
        initView(context, attrs, defStyleAttr)
    }

    private fun initView(context: Context, attrs: AttributeSet?, defStyleAttr: Int) {
        val array = context.theme.obtainStyledAttributes(
            attrs,
            R.styleable.SrcLoopScrollFrameLayout,
            defStyleAttr,
            0
        )
        val speed = array.getInteger(R.styleable.SrcLoopScrollFrameLayout_speed, 3)
        mScrollOrientation =
            array.getInteger(R.styleable.SrcLoopScrollFrameLayout_scrollOrientation, OUT_SLIDE_TOP)
        mIntervalIncreaseDistance *= speed
        mDrawable = array.getDrawable(R.styleable.SrcLoopScrollFrameLayout_src)
        mIsScroll = array.getBoolean(R.styleable.SrcLoopScrollFrameLayout_isScroll, true)
        mMaskLayerColor =
            array.getColor(R.styleable.SrcLoopScrollFrameLayout_maskLayerColor, Color.TRANSPARENT)
        array.recycle()

        setWillNotDraw(false)
        mPaint = Paint()
        mMatrix = Matrix()
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        if (mDrawable == null || mDrawable !is BitmapDrawable) {
            return
        }
        if (visibility == GONE) {
            return
        }
        if (w == 0 || h == 0) {
            return
        }
        if (mSrcBitmap == null) {
            val bitmap = (mDrawable as BitmapDrawable).bitmap
            //调整色彩模式进行质量压缩
            val compressBitmap = bitmap.copy(Bitmap.Config.RGB_565, true)
            //缩放 Bitmap
            mSrcBitmap = scaleBitmap(compressBitmap)
            //计算至少需要几个 bitmap 才能填满当前 view
            when (mScrollOrientation) {
                OUT_SLIDE_TOP, OUT_SLIDE_BOTTOM -> {
                    mBitmapCount = measuredHeight / mSrcBitmap!!.height + 1
                }
                OUT_SLIDE_LEFT, OUT_SLIDE_RIGHT -> {
                    mBitmapCount = measuredWidth / mSrcBitmap!!.width + 1
                }
            }
            if (!compressBitmap.isRecycled) {
                compressBitmap.isRecycled
                System.gc()
            }
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (mSrcBitmap == null) {
            return
        }
        val length = if (scrollOrientationIsVertical()) mSrcBitmap!!.height else mSrcBitmap!!.width
        if (length + mPanDistance != 0f) {
            //第一张图片未完全滚出屏幕
            mMatrix.reset()
            when (mScrollOrientation) {
                OUT_SLIDE_TOP -> {
                    mMatrix.postTranslate(0f, mPanDistance)
                }
                OUT_SLIDE_BOTTOM -> {
                    mMatrix.postTranslate(0f, measuredHeight - length - mPanDistance)
                }
                OUT_SLIDE_LEFT -> {
                    mMatrix.postTranslate(mPanDistance, 0f)
                }
                OUT_SLIDE_RIGHT -> {
                    mMatrix.postTranslate(measuredWidth - length - mPanDistance, 0f)
                }
            }
            canvas.drawBitmap(mSrcBitmap!!, mMatrix, mPaint)
        }
        if (length + mPanDistance < (if (scrollOrientationIsVertical()) measuredHeight else measuredWidth)) {
            //用于补充留白的图片出现在屏幕
            for (i in 0 until mBitmapCount) {
                mMatrix.reset()
                when (mScrollOrientation) {
                    OUT_SLIDE_TOP -> {
                        mMatrix.postTranslate(0f, (i + 1) * length + mPanDistance)
                    }
                    OUT_SLIDE_BOTTOM -> {
                        mMatrix.postTranslate(0f, measuredHeight - (i + 2) * length - mPanDistance)
                    }
                    OUT_SLIDE_LEFT -> {
                        mMatrix.postTranslate((i + 1) * length + mPanDistance, 0f)
                    }
                    OUT_SLIDE_RIGHT -> {
                        mMatrix.postTranslate(measuredWidth - (i + 2) * length - mPanDistance, 0f)
                    }
                }
                canvas.drawBitmap(mSrcBitmap!!, mMatrix, mPaint)
            }
        }
        //绘制遮罩层
        if (mMaskLayerColor != Color.TRANSPARENT) {
            canvas.drawColor(mMaskLayerColor)
        }
        //延时重绘实现滚动效果
        if (mIsScroll) {
            handler.postDelayed(mRedrawRunnable, DEFAULT_DRAW_INTERVALS_TIME)
        }
    }

    /**
     * 重绘
     */
    private val mRedrawRunnable = Runnable {
        val length = if (scrollOrientationIsVertical()) mSrcBitmap!!.height else mSrcBitmap!!.width
        if (length + mPanDistance <= 0) {
            //第一张已完全滚出屏幕，重置平移距离
            mPanDistance = 0f
        }
        mPanDistance -= mIntervalIncreaseDistance
        invalidate()
    }

    /**
     * 设置背景图 bitmap
     * 通过该方法设置的背景图，当 屏幕翻转/暗黑模式切换 等涉及到 activity 重构的情况出现时，需要在 activity 重构后重新设置背景图
     * @srcBitmap 背景图
     * @isMassReduce 是否进行质量压缩(通过调整色彩模式实现，默认进行压缩)
     */
    fun setSrcBitmap(srcBitmap: Bitmap, isMassReduce: Boolean = true) {
        val oldScrollStatus = mIsScroll
        if (oldScrollStatus) {
            stopScroll()
        }
        val compressBitmap: Bitmap =
            if (isMassReduce && srcBitmap.config != Bitmap.Config.RGB_565) {
                //调整色彩模式进行质量压缩
                srcBitmap.copy(Bitmap.Config.RGB_565, true)
            } else {
                srcBitmap
            }
        //按当前View宽度比例缩放 Bitmap
        mSrcBitmap = scaleBitmap(compressBitmap)
        //计算至少需要几个 bitmap 才能填满当前 view
        mBitmapCount =
            if (scrollOrientationIsVertical()) measuredHeight / mSrcBitmap!!.height + 1 else measuredWidth / mSrcBitmap!!.width + 1
        if (!srcBitmap.isRecycled) {
            srcBitmap.isRecycled
            System.gc()
        }
        if (!compressBitmap.isRecycled) {
            compressBitmap.isRecycled
            System.gc()
        }
        if (oldScrollStatus) {
            startScroll()
        }
    }

    /**
     * 开始滚动
     */
    fun startScroll() {
        if (mSrcBitmap != null && mIsScroll) {
            return
        }
        mIsScroll = true
        handler.postDelayed(mRedrawRunnable, DEFAULT_DRAW_INTERVALS_TIME)
    }

    /**
     * 停止滚动
     */
    fun stopScroll() {
        if (!mIsScroll) {
            return
        }
        mIsScroll = false
        handler.removeCallbacks(mRedrawRunnable)
    }

    /**
     * 切换滚动方向
     */
    fun changeScrollOrientation() {
        mPanDistance = 0f
        if (mScrollOrientation == OUT_SLIDE_RIGHT) {
            mScrollOrientation = OUT_SLIDE_TOP
        } else {
            mScrollOrientation++
        }
        if (mSrcBitmap != null) {
            if (mDrawable != null && mDrawable is BitmapDrawable) {
                val bitmap = (mDrawable as BitmapDrawable).bitmap
                if (!bitmap.isRecycled) {
                    setSrcBitmap(bitmap)
                    return
                }
            }
            setSrcBitmap(mSrcBitmap!!)
        }
    }

    /**
     * 判断是否为竖直滚动
     */
    private fun scrollOrientationIsVertical(): Boolean {
        return mScrollOrientation == OUT_SLIDE_TOP || mScrollOrientation == OUT_SLIDE_BOTTOM
    }


    /**
     * 缩放 Bitmap
     */
    private fun scaleBitmap(originBitmap: Bitmap): Bitmap? {
        val width = originBitmap.width
        val height = originBitmap.height
        val newHeight: Int
        val newWidth: Int
        if (scrollOrientationIsVertical()) {
            newWidth = measuredWidth
            newHeight = newWidth * height / width
        } else {
            newHeight = measuredHeight
            newWidth = newHeight * width / height
        }
        return Bitmap.createScaledBitmap(originBitmap, newWidth, newHeight, true)
    }
}