# ClockView
Android ClockView

### **1.废话不多说先上图**

![](https://github.com/chuwe1/ClockView/blob/master/screenshots/clock.gif)

--------------------------------
### **2.分析**

* 观看上图咱们需要绘制的有：
	* 时针
	* 分针
	* 秒针
	* 时刻度
	* 秒刻度
	* 仪表盘上的数字
	* 上下午区分标识（AM/PM）

* View与Surface的取舍：
	* View一般用于绘制静态页面或者界面元素跟随用户的操作(点击、拖拽等)而被动的改变位置、大小等
	* SurfaceView一般用于无需用户操作，界面元素就需要不断的刷新的情况（例如打飞机游戏不断移动的背景）
	* 通过以上两条可以确定SurfaceView正好符合我们的需求

-------------------------------------
### **3.SurfaceView注意事项**

* 如何使用SurfaceView
	* 1.继承SurfaceView
	* 2.实现SurfaceHolder.Callback接口
		* surfaceCreated：Surface创建后调用，一般做一些初始化工作
		* surfaceChanged：Surface状态发生变化时调用（例如大小）
		* surfaceDestroyed：Surface销毁时调用，一般在这里结束绘制线程
	* 3.SurfaceHolder：控制Surface的类，得到画布、提交画布、回调等
	* 4.绘制和逻辑

* SurfaceView的写法

```
public class MyView extends SurfaceView implements SurfaceHolder.Callback,Runnable {

    private SurfaceHolder mHolder;

    public MyView(Context context) {
        this(context, null);
    }

    public MyView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public MyView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        mHolder = getHolder();
        mHolder.addCallback(this);
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        new Thread(this).start();
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {

    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {

    }

    @Override
    public void run() {
        while (true) {
            logic();
            draw();
        }
    }

    /**
     * 逻辑操作
     */
    private void logic() {

    }

    /**
     * 绘制操作
     */
    private void draw() {

    }
}
```
这是一般性写法，不过有些人应该已经发现了我们线程里跑的是一个while(true)的无限死循化，因此往往我们会增加一个标识位在surface销毁时置false。修改后部分代码如下：
```
private boolean flag;

@Override
public void surfaceCreated(SurfaceHolder holder) {
    flag = true;
    mThread.start();
}

@Override
public void surfaceDestroyed(SurfaceHolder holder) {
    flag = false;
}

@Override
public void run() {
    while (flag) {
        logic();
        draw();
    }
}
```
当然了这样还是有缺陷的，因为当flag==true时，mThread里面的逻辑操作和绘制操作就在无限运行了。可想而知，如果是那样的话那么我们这里的时钟指针在你眼前飞速的转动，由于gif录制工具不够强大录下的动图根本开不出指针飞速旋转的效果这里就不提供图了，有兴趣的同学可以自己试一下，也可以脑补。。。。。。。。。。。
因此我们就需要对线程加以限制，具体如下：
```
@Override
public void run() {
    long start, end;
    while (flag) {
        start = System.currentTimeMillis();
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
```
这里的1000指的是每隔1S刷新一次界面，因为我们的始终的最小单位是秒

-----------------------------------
### **4.废话啰嗦完了 咱们开始实战**

* **step1：通过[2](#2.)**的分析，定义需要的**属性**

```
// 默认半径
private static final int DEFAULT_RADIUS = 200;

private SurfaceHolder mHolder;
private Canvas mCanvas;
private Thread mThread;
private boolean flag;

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

// 时钟显示的时、分、秒
private int mHour, mMinute, mSecond;
```

* **step2：初始化**操作

```
public ClockView(Context context, AttributeSet attrs, int defStyleAttr) {
    super(context, attrs, defStyleAttr);
    mHour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
    mMinute = Calendar.getInstance().get(Calendar.MINUTE);
    mSecond = Calendar.getInstance().get(Calendar.SECOND);

    mHolder = getHolder();
    mHolder.addCallback(this);
    mThread = new Thread(this);

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
```

* **step3：测量**

```
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

/**
 * 计算指针和刻度长度
 */
private void calculateLengths() {
	// 这里我们定义时刻度长度为半径的1/7
    mHourDegreeLength = (int) (mRadius * 1.0f / 7);
    // 秒刻度长度为时刻度长度的一半
    mSecondDegreeLength = (int) (mHourDegreeLength * 1.0f / 2);

	// 时针长度为半径一半
    // 指针长度比 hour : minute : second = 1 : 1.25 : 1.5
    mHourPointerLength = (int) (mRadius * 1.0 / 2);
    mMinutePointerLength = (int) (mHourPointerLength * 1.25f);
    mSecondPointerLength = (int) (mHourPointerLength * 1.5f);
}
```
测量的前面部分代码基本都是一个模式来写的，需要注意的是当测量模式不是Exectly时的处理，想要了解这一块同学的可以去[鸿神](http://blog.csdn.net/lmj623565791)的博客学习

* **step4：绘制**

```
/**
 * 绘制，这部分代码基本固定
 */
private void draw() {
    try {
        mCanvas = mHolder.lockCanvas(); // 得到画布
        if (mCanvas != null) {
            // 在这里绘制内容
        }
    } catch (Exception e) {
        e.printStackTrace();
    } finally {
        if (mCanvas != null) {
	        // 提交画布，否则什么都看不见
            mHolder.unlockCanvasAndPost(mCanvas);
        }
    }
}

现在开始具体的绘制内容（画什么由画布决定，怎么画由画笔决定，这也就是我们上面给画笔设置一系列属性的原因）：

// 1.将坐标系原点移至去除内边距后的画布中心
// 默认在画布左上角，这样做是为了更方便的绘制
mCanvas.translate(mCanvasWidth * 1.0f / 2 + getPaddingLeft() - getPaddingRight(),mCanvasHeight * 1.0f / 2 + getPaddingTop() - getPaddingBottom());
// 2.绘制圆盘
mPaint.setStrokeWidth(2f); // 画笔设置2个像素的宽度
mCanvas.drawCircle(0, 0, mRadius, mPaint); // 到这一步就能知道第一步的好处了，否则害的去计算园的中心点坐标
// 3.绘制时刻度
for (int i = 0; i < 12; i++) {
    mCanvas.drawLine(0, mRadius, 0, mRadius - mHourDegreeLength, mPaint);
    mCanvas.rotate(30); // 360°平均分成12份，每份30°
}
// 4.绘制秒刻度
mPaint.setStrokeWidth(1.5f);
for (int i = 0; i < 60; i++) {
    //时刻度绘制过的区域不在绘制
    if (i % 5 != 0) {
        mCanvas.drawLine(0, mRadius, 0, mRadius - mSecondDegreeLength, mPaint);
    }
    mCanvas.rotate(6); // 360°平均分成60份，每份6°
}
// 5.绘制数字
mPointerPaint.setColor(Color.BLACK);
for (int i = 0; i < 12; i++) {
    String number = 6 + i < 12 ? String.valueOf(6 + i) : (6 + i) > 12
            ? String.valueOf(i - 6) : "12";
    mCanvas.drawText(number, 0, mRadius * 5.5f / 7, mPointerPaint);
    mCanvas.rotate(30);
}
// 6.绘制上下午
mCanvas.drawText(mHour < 12 ? "AM" : "PM", 0, mRadius * 1.5f / 4, mPointerPaint);
// 7.绘制时针
Path path = new Path();
path.moveTo(0, 0);
int[] hourPointerCoordinates = getPointerCoordinates(mHourPointerLength);
path.lineTo(hourPointerCoordinates[0], hourPointerCoordinates[1]);
path.lineTo(hourPointerCoordinates[2], hourPointerCoordinates[3]);
path.lineTo(hourPointerCoordinates[4], hourPointerCoordinates[5]);
path.close();
mCanvas.save();
mCanvas.rotate(180 + mHour % 12 * 30 + mMinute * 1.0f / 60 * 30);
mCanvas.drawPath(path, mPointerPaint);
mCanvas.restore();
// 8.绘制分针
path.reset();
path.moveTo(0, 0);
int[] minutePointerCoordinates = getPointerCoordinates(mMinutePointerLength);
path.lineTo(minutePointerCoordinates[0], minutePointerCoordinates[1]);
path.lineTo(minutePointerCoordinates[2], minutePointerCoordinates[3]);
path.lineTo(minutePointerCoordinates[4], minutePointerCoordinates[5]);
path.close();
mCanvas.save();
mCanvas.rotate(180 + mMinute * 6);
mCanvas.drawPath(path, mPointerPaint);
mCanvas.restore();
// 9.绘制秒针
mPointerPaint.setColor(Color.RED);
path.reset();
path.moveTo(0, 0);
int[] secondPointerCoordinates = getPointerCoordinates(mSecondPointerLength);
path.lineTo(secondPointerCoordinates[0], secondPointerCoordinates[1]);
path.lineTo(secondPointerCoordinates[2], secondPointerCoordinates[3]);
path.lineTo(secondPointerCoordinates[4], secondPointerCoordinates[5]);
path.close();
mCanvas.save();
mCanvas.rotate(180 + mSecond * 6);
mCanvas.drawPath(path, mPointerPaint);
mCanvas.restore();

这里比较难的可能就是指针的绘制，因为我们的指针是个规则形状，其中getPointerCoordinates便是得到这个不规则形状的3个定点坐标，有兴趣的同学可以去研究一下我的逻辑，也可以定义你自己的逻辑。我的逻辑如下（三角函数学的号的同学应该一眼就能看懂）：

/**
 * 获取指针坐标
 *
 * @param pointerLength 指针长度
 * @return int[]{x1,y1,x2,y2,x3,y3}
 */
private int[] getPointerCoordinates(int pointerLength) {
    int y = (int) (pointerLength * 3.0f / 4);
    int x = (int) (y * Math.tan(Math.PI / 180 * 5));
    return new int[]{-x, y, 0, pointerLength, x, y};
}
```

* **step5：逻辑**
这里逻辑可想而知每个秒，秒数+1到60的时候归0，同时分钟数+1，
分钟数到60的时候归0，小时数+1，小时数到24的时候归0.

```
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
```

* **step6：装逼的时刻到了**

运行来看一下效果
![](http://upload-images.jianshu.io/upload_images/2144156-88cb56be6a081e33?imageMogr2/auto-orient/strip%7CimageView2/2/w/1240)
我靠什么情况为什么画出来是这么个东西

这里就是要值得大家注意的了，使用surface绘制的时候一定要刷屏，所谓的刷屏在每次绘制前画一个什么都没有图层在上次画出来的东西上面盖住再去画本次实现也很简单只需要在绘制的第一行加上一句就行。
我这里是
> //刷屏
mCanvas.drawColor(Color.WHITE);

这里的背景也可以通过自定义属性来自定义的。不知道的同学仍然可以去[鸿神](http://blog.csdn.net/lmj623565791)的博客学习

------------------------------
### **5.总结**

主要涉及知识点：

>* View和SurfaceView的取舍
* SurfaceView的使用
* Canvas使用
* 坐标系的灵活运用

------------------------------

### **6.Tips**
图上的点击按钮改变时间和改变textView上的时候只是定义了一个回调接口，这里不再赘述
