package com.chuwe1.customizeview;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.os.Handler;
import android.os.Message;
import android.util.AttributeSet;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;

import java.util.Calendar;

/**
 * 自定义时钟View
 *
 * @author Chuwe1
 * @see <a href="https://github.com/chuwe1/ClockView">ClockView@github</a>
 */
public class ClockView extends SurfaceView implements SurfaceHolder.Callback, Runnable {

    // 默认半径
    private static final int DEFAULT_RADIUS = 200;

    private SurfaceHolder mHolder;
    private Canvas mCanvas;
    private boolean flag;

    private OnTimeChangeListener onTimeChangeListener;

    public void setOnTimeChangeListener(OnTimeChangeListener onTimeChangeListener) {
        this.onTimeChangeListener = onTimeChangeListener;
    }

    // 圆和刻度的画笔
    private Paint mPaint;
    // 指针画笔
    private Paint mPointerPaint;

    // 画布的宽高
    private int mCanvasWidth, mCanvasHeight;
    // 时钟半径
    private int mRadius = DEFAULT_RADIUS;
    // 秒针长度
    private int mSecondPointerLength;
    // 分针长度
    private int mMinutePointerLength;
    // 时针长度
    private int mHourPointerLength;
    // 时刻度长度
    private int mHourDegreeLength;
    // 秒刻度
    private int mSecondDegreeLength;

    // 指针坐标{x1,y1,x2,y2,x3,y3}
    private int[] pointerCoordinates = new int[6];
    // 数字刻度的区域
    private Rect numberRect = new Rect();
    // 用于绘制指针
    private Path pointerPath = new Path();

    // 时钟显示的时、分、秒
    private int mHour, mMinute, mSecond;

    public ClockView(Context context) {
        this(context, null);
    }

    public ClockView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public ClockView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        mHour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
        mMinute = Calendar.getInstance().get(Calendar.MINUTE);
        mSecond = Calendar.getInstance().get(Calendar.SECOND);

        mHolder = getHolder();
        mHolder.addCallback(this);

        mPaint = new Paint();
        mPointerPaint = new Paint();

        mPaint.setColor(Color.BLACK);
        mPaint.setAntiAlias(true);
        mPaint.setStyle(Paint.Style.STROKE);

        mPointerPaint.setColor(Color.BLACK);
        mPointerPaint.setAntiAlias(true);
        mPointerPaint.setStyle(Paint.Style.FILL_AND_STROKE);
        mPointerPaint.setTextSize(22);
        mPointerPaint.setTextAlign(Paint.Align.CENTER);

        setFocusable(true);
        setFocusableInTouchMode(true);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);

        int widthSize = MeasureSpec.getSize(widthMeasureSpec);
        int widthMode = MeasureSpec.getMode(widthMeasureSpec);
        int heightSize = MeasureSpec.getSize(heightMeasureSpec);
        int heightMode = MeasureSpec.getMode(heightMeasureSpec);

        int desiredWidth, desiredHeight;
        if (widthMode == MeasureSpec.EXACTLY) {
            desiredWidth = widthSize;
        } else {
            desiredWidth = mRadius * 2 + getPaddingLeft() + getPaddingRight();
            if (widthMode == MeasureSpec.AT_MOST) {
                desiredWidth = Math.min(widthSize, desiredWidth);
            }
        }

        if (heightMode == MeasureSpec.EXACTLY) {
            desiredHeight = heightSize;
        } else {
            desiredHeight = mRadius * 2 + getPaddingTop() + getPaddingBottom();
            if (heightMode == MeasureSpec.AT_MOST) {
                desiredHeight = Math.min(heightSize, desiredHeight);
            }
        }

        // +4是为了设置默认的2px的内边距，因为绘制时钟的圆的画笔设置的宽度是2px
        setMeasuredDimension(mCanvasWidth = desiredWidth + 4, mCanvasHeight = desiredHeight + 4);

        mRadius = (int) (Math.min(desiredWidth - getPaddingLeft() - getPaddingRight(),
                desiredHeight - getPaddingTop() - getPaddingBottom()) * 1.0f / 2);
        calculateLengths();
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        flag = true;
        new Thread(this).start();
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        flag = false;
    }

    @Override
    public void run() {
        long start, end;
        while (flag) {
            start = System.currentTimeMillis();
            handler.sendEmptyMessage(0);
            draw();
            logic();
            end = System.currentTimeMillis();

            try {
                if (end - start < 1000) {
                    Thread.sleep(1000 - (end - start));
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    private Handler handler = new Handler(new Handler.Callback() {
        @Override
        public boolean handleMessage(Message msg) {
            if (onTimeChangeListener != null) {
                onTimeChangeListener.onTimeChange(ClockView.this, mHour, mMinute, mSecond);
            }
            return false;
        }
    });

    /**
     * 逻辑
     */
    private void logic() {
        mSecond++;
        if (mSecond == 60) {
            mSecond = 0;
            mMinute++;
            if (mMinute == 60) {
                mMinute = 0;
                mHour++;
                if (mHour == 24) {
                    mHour = 0;
                }
            }
        }
    }

    /**
     * 绘制
     */
    private void draw() {
        try {
            mCanvas = mHolder.lockCanvas();
            if (mCanvas != null) {
                //刷屏
                mCanvas.drawColor(Color.WHITE);
                //将坐标系原点移至去除内边距后的画布中心
                mCanvas.translate(mCanvasWidth * 1.0f / 2 + getPaddingLeft() - getPaddingRight(),
                        mCanvasHeight * 1.0f / 2 + getPaddingTop() - getPaddingBottom());
                //绘制圆盘
                mPaint.setStrokeWidth(2f);
                mCanvas.drawCircle(0, 0, mRadius, mPaint);
                //绘制时刻度
                for (int i = 0; i < 12; i++) {
                    mCanvas.drawLine(0, mRadius, 0, mRadius - mHourDegreeLength, mPaint);
                    mCanvas.rotate(30);
                }
                //绘制秒刻度
                mPaint.setStrokeWidth(1.5f);
                for (int i = 0; i < 60; i++) {
                    //时刻度绘制过的区域不再绘制
                    if (i % 5 != 0) {
                        mCanvas.drawLine(0, mRadius, 0, mRadius - mSecondDegreeLength, mPaint);
                    }
                    mCanvas.rotate(6);
                }
                //绘制数字
                mPointerPaint.setColor(Color.BLACK);
                for (int i = 0; i < 12; i++) {
                    String number = 6 + i < 12 ? String.valueOf(6 + i)
                            : 6 + i > 12 ? String.valueOf(i - 6)
                            : "12";
                    mPointerPaint.getTextBounds(number, 0, number.length(), numberRect);
                    float radius = mRadius * 5.25f / 7;
                    double numberRadians = Math.PI / 180 * (90 + i * 30);
                    mCanvas.drawText(number, 0, number.length(),
                            (float) (radius * Math.cos(numberRadians)), // 对齐方式是Center
                            (float) (radius * Math.sin(numberRadians) + numberRect.height() / 2),
                            mPointerPaint);
                }
                //绘制上下午
                mCanvas.drawText(mHour < 12 ? "AM" : "PM", 0, mRadius * 1.5f / 4, mPointerPaint);
                //绘制时针
                pointerPath.moveTo(0, 0);
                refreshPointerCoordinates(mHourPointerLength);
                pointerPath.lineTo(pointerCoordinates[0], pointerCoordinates[1]);
                pointerPath.lineTo(pointerCoordinates[2], pointerCoordinates[3]);
                pointerPath.lineTo(pointerCoordinates[4], pointerCoordinates[5]);
                pointerPath.close();
                mCanvas.save();
                mCanvas.rotate(180 + mHour % 12 * 30 + mMinute * 1.0f / 60 * 30);
                mCanvas.drawPath(pointerPath, mPointerPaint);
                mCanvas.restore();
                //绘制分针
                pointerPath.reset();
                pointerPath.moveTo(0, 0);
                refreshPointerCoordinates(mMinutePointerLength);
                pointerPath.lineTo(pointerCoordinates[0], pointerCoordinates[1]);
                pointerPath.lineTo(pointerCoordinates[2], pointerCoordinates[3]);
                pointerPath.lineTo(pointerCoordinates[4], pointerCoordinates[5]);
                pointerPath.close();
                mCanvas.save();
                mCanvas.rotate(180 + mMinute * 6);
                mCanvas.drawPath(pointerPath, mPointerPaint);
                mCanvas.restore();
                //绘制秒针
                mPointerPaint.setColor(Color.RED);
                pointerPath.reset();
                pointerPath.moveTo(0, 0);
                refreshPointerCoordinates(mSecondPointerLength);
                pointerPath.lineTo(pointerCoordinates[0], pointerCoordinates[1]);
                pointerPath.lineTo(pointerCoordinates[2], pointerCoordinates[3]);
                pointerPath.lineTo(pointerCoordinates[4], pointerCoordinates[5]);
                pointerPath.close();
                mCanvas.save();
                mCanvas.rotate(180 + mSecond * 6);
                mCanvas.drawPath(pointerPath, mPointerPaint);
                mCanvas.restore();
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (mCanvas != null) {
                mHolder.unlockCanvasAndPost(mCanvas);
            }
        }
    }

    /**
     * 获取指针坐标
     *
     * @param pointerLength 指针长度
     */
    private void refreshPointerCoordinates(int pointerLength) {
        int y = (int) (pointerLength * 3.0f / 4);
        int x = (int) (y * Math.tan(Math.PI / 180 * 5));

        pointerCoordinates[0] = -x;
        pointerCoordinates[1] = y;
        pointerCoordinates[2] = 0;
        pointerCoordinates[3] = pointerLength;
        pointerCoordinates[4] = x;
        pointerCoordinates[5] = y;
    }

    /**
     * 计算指针和刻度长度
     */
    private void calculateLengths() {
        mHourDegreeLength = (int) (mRadius * 1.0f / 7);
        mSecondDegreeLength = (int) (mHourDegreeLength * 1.0f / 2);

        // hour : minute : second = 1 : 1.25 : 1.5
        mHourPointerLength = (int) (mRadius * 1.0 / 2);
        mMinutePointerLength = (int) (mHourPointerLength * 1.25f);
        mSecondPointerLength = (int) (mHourPointerLength * 1.5f);
    }

    //-----------------Setter and Getter start-----------------//
    public int getHour() {
        return mHour;
    }

    public void setHour(int hour) {
        mHour = Math.abs(hour) % 24;
        if (onTimeChangeListener != null) {
            onTimeChangeListener.onTimeChange(this, mHour, mMinute, mSecond);
        }
    }

    public int getMinute() {
        return mMinute;
    }

    public void setMinute(int minute) {
        mMinute = Math.abs(minute) % 60;
        if (onTimeChangeListener != null) {
            onTimeChangeListener.onTimeChange(this, mHour, mMinute, mSecond);
        }
    }

    public int getSecond() {
        return mSecond;
    }

    public void setSecond(int second) {
        mSecond = Math.abs(second) % 60;
        if (onTimeChangeListener != null) {
            onTimeChangeListener.onTimeChange(this, mHour, mMinute, mSecond);
        }
    }

    public void setTime(Integer... time) {
        if (time.length > 3) {
            throw new IllegalArgumentException("the length of argument should bo less than 3");
        }
        if (time.length > 2)
            setSecond(time[2]);
        if (time.length > 1)
            setMinute(time[1]);
        if (time.length > 0)
            setHour(time[0]);
    }
    //-----------------Setter and Getter end-------------------//

    /**
     * 当时间改变的时候提供回调的接口
     */
    public interface OnTimeChangeListener {
        /**
         * 时间发生改变时调用
         *
         * @param view   时间正在改变的view
         * @param hour   改变后的小时时刻
         * @param minute 改变后的分钟时刻
         * @param second 改变后的秒时刻
         */
        void onTimeChange(View view, int hour, int minute, int second);
    }
}
