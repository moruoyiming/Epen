package com.rwls.students;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PointF;
import android.graphics.Rect;
import android.os.Handler;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.LinearLayout;

import com.tstudy.blepenlib.data.CoordinateInfo;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

import static com.tstudy.blepenlib.constant.Constant.PEN_COODINAT_MESSAGE;
import static com.tstudy.blepenlib.constant.Constant.PEN_DOWN_MESSAGE;
import static com.tstudy.blepenlib.constant.Constant.PEN_UP_MESSAGE;


public class CanvasFrame extends LinearLayout {
    private Context context;
    public SignatureView bDrawl;
    public static String TAG = "CanvasFrame_tag";
    Matrix matrix = new Matrix();
    public TouchListener mTouchListener;

    public CanvasFrame(Context context) {
        super(context);
        this.context = context;
        addWordView();

        WindowManager wm = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        DisplayMetrics metrics = new DisplayMetrics();
        wm.getDefaultDisplay().getMetrics(metrics);

        setLayoutParams(new LayoutParams(metrics.widthPixels, (int) (metrics.widthPixels * 1.414)));
        setWillNotDraw(false);
        this.setLayerType(View.LAYER_TYPE_NONE, null);
    }

    public CanvasFrame(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.context = context;
        setWillNotDraw(false);
    }

    public CanvasFrame(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        this.context = context;
        setWillNotDraw(false);
    }

    private void addWordView() {
        bDrawl = new SignatureView(context);
        this.addView(bDrawl);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        canvas.concat(matrix);
    }

    @Override
    public Matrix getMatrix() {
        return matrix;
    }

    public void setMatrix(Matrix matrix) {
        this.matrix = matrix;
        final Handler handler = new Handler();
        handler.post(new Runnable() {
            @Override
            public void run() {
                postInvalidate();
            }
        });
    }

    public class SignatureView extends View {
        private float mSignatureWidth = 2f;
        private Bitmap mSignature = null;

        private static final boolean GESTURE_RENDERING_ANTIALIAS = true;
        private static final boolean DITHER_FLAG = true;

        private Paint mPaint = new Paint();
        private Path mPath = new Path();

        public ArrayList<AFStroke> strokes = new ArrayList<>();
        AFStroke curStroke = new AFStroke();
        boolean isFirst;
        CoordinateInfo lastDot;

        public void addDot(CoordinateInfo dot) {
            if (dot.state == PEN_COODINAT_MESSAGE) {
                if (isFirst) {
                    isFirst = false;
                    curStroke.add(dot);
                    lastDot = dot;
                } else {
                    double sqrt = Math.sqrt(Math.pow(dot.coordX - lastDot.coordX, 2) + Math.pow(dot.coordY - lastDot.coordY, 2));
                    if (sqrt<=2.5){
                        return;
                    }
                    CoordinateInfo dot1 = new CoordinateInfo(PEN_COODINAT_MESSAGE, "0.0.0.0", (lastDot.coordX * 2 + dot.coordX) / 3, (lastDot.coordY * 2 + dot.coordY) / 3, 100, 1, 1, false, 0, 0);
                    CoordinateInfo dot2 = new CoordinateInfo(PEN_COODINAT_MESSAGE, "0.0.0.0", (lastDot.coordX + dot.coordX * 2) / 3, (lastDot.coordY + dot.coordY * 2) / 3, 100, 1, 1, false, 0, 0);
                    curStroke.add(dot1);
                    curStroke.add(dot2);
                    curStroke.add(dot);
                    lastDot = dot;
                }
                curStroke.buildBezierPath(getWidth(), getHeight());


            } else if (dot.state == PEN_DOWN_MESSAGE) {
                isFirst = true;
                strokes.add(curStroke);
                curStroke = new AFStroke();
            }
            invalidate();
        }

        public void addDots(List<CoordinateInfo> dots) {
            curStroke = new AFStroke();
            for (int i = 0; i < dots.size(); i++) {
                CoordinateInfo dot = dots.get(i);
                if (dot.state == PEN_COODINAT_MESSAGE) {
                    curStroke.add(dot);
                    curStroke.buildBezierPath(getWidth(), getHeight());
                } else if (dot.state == PEN_DOWN_MESSAGE) {
                    strokes.add(curStroke);
                    curStroke = new AFStroke();
                }
            }
            invalidate();
        }
        public void clear(){
            strokes = new ArrayList<>();
            curStroke = new AFStroke();
            invalidate();

        }

        public SignatureView(Context context, AttributeSet attrs, int defStyle) {
            super(context, attrs, defStyle);
            init(context);
        }

        public void setSignatureBitmap(Bitmap signature) {
            mSignature = signature;
            invalidate();
        }

        public SignatureView(Context context) {
            super(context);
            init(context);
        }

        public SignatureView(Context context, AttributeSet attrs) {
            super(context, attrs);
            init(context);
        }

        private void init(Context context) {
            setWillNotDraw(false);
            mPaint.setAntiAlias(GESTURE_RENDERING_ANTIALIAS);
            mPaint.setColor(Color.BLACK);
            mPaint.setStyle(Paint.Style.STROKE);
            mPaint.setStrokeJoin(Paint.Join.ROUND);
            mPaint.setStrokeCap(Paint.Cap.ROUND);
            mPaint.setStrokeWidth(mSignatureWidth);
            mPaint.setDither(DITHER_FLAG);

            mPath.reset();
        }

        @Override
        protected void onDraw(Canvas canvas) {

            if (mSignature != null) {
                canvas.drawBitmap(mSignature, null, new Rect(0, 0, getWidth(), getHeight()), null);
            }
            for (int i = 0; i < strokes.size(); i++) {
                if (strokes.get(i).fullPath != null) {
                    canvas.drawPath(strokes.get(i).fullPath, mPaint);
                }
            }
            if (curStroke.fullPath != null) {
                canvas.drawPath(curStroke.fullPath, mPaint);
            }

        }

        public class AFStroke {
            private ArrayList<CoordinateInfo> dots = null;

            public class CGPoint {
                public float x;
                public float y;

                public CGPoint(float x, float y) {
                    this.x = x;
                    this.y = y;
                }
            }

            public int lastDrawIdx;
            public Path fullPath;
            ArrayList<CGPoint> pts;
            int ctr;

            public AFStroke() {
                this.dots = new ArrayList<>();
                pts = new ArrayList<>();
                for (int i = 0; i < 4; i++) {
                    pts.add(new CGPoint(0, 0));
                }
            }

            public boolean add(CoordinateInfo dot) {
                this.dots.add(dot);
                return true;
            }

            public CoordinateInfo get(int index) {
                return dots.get(index);
            }

            private ArrayList<CoordinateInfo> getDots() {
                return dots;
            }

            private CGPoint BLEPoint2Point(int pX, int pY, int refW, int refH) {
                float pw = 5600;
                float ph = 7920;
                return new CGPoint(pX >= pw ? pw : pX * (refW / pw), pY >= ph ? ph : pY * (refH / ph));
            }

            private void buildBezierPath(int frameW, int frameH, int fromIdx, int toIdx) {
                try {
                    if (frameW != 0 && frameH != 0 && getDots() != null && getDots().size() > 0) {

                        if (this.fullPath == null) {
                            this.fullPath = new Path();//[UIBezierPath bezierPath];
                            ctr = 0;
                        }
                        for (int i = fromIdx; i < toIdx; i++) {
                            CoordinateInfo penPoint = getDots().get(i);
                            CGPoint p = BLEPoint2Point(penPoint.coordX, penPoint.coordY, frameW, frameH);
                            if (i == 0) {
                                pts.set(0, p);
                                Log.d(TAG, "buildBezierPath: 0");
                                continue;
                            }
                            ctr++;
                            pts.set(ctr, p);
                            if (ctr == 3) {
                                Log.d(TAG, "buildBezierPath: " + ctr);
                                pts.set(2, new CGPoint((float) ((pts.get(1).x + pts.get(3).x) / 2.0), (float) ((pts.get(1).y + pts.get(3).y) / 2.0)));
                                this.fullPath.moveTo(pts.get(0).x, pts.get(0).y);
                                this.fullPath.quadTo(pts.get(1).x, pts.get(1).y, pts.get(2).x, pts.get(2).y);

                                pts.set(0, pts.get(2));
                                pts.set(1, pts.get(3));
                                ctr = 1;
                            }

                            if ((i == (toIdx - 1) && getDots().get(i).state == PEN_UP_MESSAGE) && (ctr > 0) && (ctr < 3)) {
                                Log.d(TAG, "buildBezierPath: " + ctr);
                                CGPoint ctr1;
                                CGPoint ctr2;

                                if (ctr == 1) {
                                    this.fullPath.lineTo(pts.get(ctr).x, pts.get(ctr).y);
                                } else {
                                    ctr1 = ctr2 = pts.get(ctr - 1);
                                    this.fullPath.cubicTo(ctr1.x, ctr1.y, ctr2.x, ctr2.y, pts.get(ctr).x, pts.get(ctr).y);
                                }
                            }
                        }
                        this.lastDrawIdx = toIdx;
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            public void buildBezierPath(int frameW, int frameH) {
                buildBezierPath(frameW, frameH, this.lastDrawIdx, dots.size());
            }
        }
    }

    public void setLayout(int width, int height) {
        mTouchListener = new TouchListener(width, height);
    }

    public class TouchListener implements OnTouchListener {

        double minScaleX;
        double minScaleY;

        private Matrix matrix = new Matrix();
        /**
         * 用于记录图片要进行拖拉时候的坐标位置
         */
        private Matrix currentMatrix = new Matrix();

        /**
         * 记录是拖拉照片模式还是放大缩小照片模式
         */
        private int mode = 0;// 初始状态
        /**
         * 拖拉照片模式
         */
        private static final int MODE_DRAG = 1;
        /**
         * 放大缩小照片模式
         */
        private static final int MODE_ZOOM = 2;
        /**
         * 用于记录开始时候的坐标位置
         */
        private PointF startPoint = new PointF();
        /**
         * 两个手指的开始距离
         */
        private float startDis;
        /**
         * 两个手指的中间点
         */
        private PointF midPoint;


        private float mMaxScaleRank = 300f;
        private float mMaxScale = 1f;
        /**
         * 最小缩放级别
         */
        private float mMinScale = 0.21774194f;
        private boolean isGetmMinScale = false;
        private boolean isMinScale = false;
        private boolean isMinScale2 = false;
        private boolean isMinScale3 = false;
        private int imgWidth;
        private int imgHeight;
        private int intrinsicWidth;
        private int intrinsicHeight;

        public TouchListener(int imgWidth, int imgHeight) {
            minScaleX = 1;
            minScaleY = 1;
            this.imgWidth = imgWidth;
            this.imgHeight = imgHeight;
            this.intrinsicWidth = getMeasuredWidth();
            this.intrinsicHeight = getMeasuredHeight();
        }

        @Override
        public boolean onTouch(View v, MotionEvent event) {
            /** 通过与运算保留最后八位 MotionEvent.ACTION_MASK = 255 */
            switch (event.getAction() & MotionEvent.ACTION_MASK) {// 单点监听和多点触碰监听
                // 手指压下屏幕
                case MotionEvent.ACTION_DOWN:
                    mode = MODE_DRAG;
                    // 记录ImageView当前的移动位置
                    currentMatrix.set(getMatrix());
                    startPoint.set(event.getX(), event.getY());
                    matrix.set(currentMatrix);
                    if (!isGetmMinScale) {
                        float[] values = new float[9];
                        matrix.getValues(values);
                        mMinScale = values[Matrix.MSCALE_X];
                        mMaxScale = mMinScale * mMaxScaleRank;
                        isGetmMinScale = true;
                    }
//                    makeImageViewFit();
                    break;
                // 手指在屏幕上移动，改事件会被不断触发
                case MotionEvent.ACTION_MOVE:
                    // 拖拉图片
                    if (mode == MODE_DRAG) {
                        // System.out.println("ACTION_MOVE_____MODE_DRAG");
                        float dx = event.getX() - startPoint.x; // 得到x轴的移动距离
                        float dy = event.getY() - startPoint.y; // 得到y轴的移动距离
                        // 在没有移动之前的位置上进行移动z
                        matrix.set(currentMatrix);
                        float[] values = new float[9];
                        matrix.getValues(values);
                        dx = checkDxBound(values, dx);
                        dy = checkDyBound(values, dy);
                        matrix.postTranslate(dx, dy);
                    }
                    // 放大缩小图片
                    else if (mode == MODE_ZOOM) {
                        float endDis = distance(event);// 结束距离
                        if (endDis > 10f) { // 两个手指并拢在一起的时候像素大于10
                            float scale = endDis / startDis;// 得到缩放倍数
                            matrix.set(currentMatrix);
                            if (scale >= 1) {
                                isMinScale3 = false;
                            } else {
                                isMinScale3 = true;
                            }

                            float[] values = new float[9];
                            matrix.getValues(values);

                            scale = checkFitScale(scale, values);

                            matrix.postScale(scale, scale, midPoint.x, midPoint.y);
                            matrix.getValues(values);

                            double valuesX = Math.floor(values[Matrix.MSCALE_X] * 1000000) / 1000000.0;
                            double valuesY = Math.floor(values[Matrix.MSCALE_Y] * 1000000) / 1000000.0;

                            if (valuesX == minScaleX && valuesY == minScaleY) {
                                isMinScale2 = true;
                            }

                            if (isMinScale && isMinScale2 && isMinScale3) {
                                matrix.getValues(values);
                                makeImgCenter(values);
                            }
                        }
                    }
                    break;
                // 手指离开屏幕
                case MotionEvent.ACTION_UP:
                    if (isMinScale && isMinScale2 && isMinScale3) {
                        isMinScale2 = false;
                    }
                    isMinScale = false;
                    isMinScale3 = false;
                    if (Math.abs(event.getX() - startPoint.x) < 10 && Math.abs(event.getY() - startPoint.y) < 10) {
                        try {
                            Field field = View.class.getDeclaredField("mListenerInfo");
                            field.setAccessible(true);
                            Object object = field.get(v);
                            field = object.getClass().getDeclaredField("mOnClickListener");
                            field.setAccessible(true);
                            object = field.get(object);
                            if (object != null && object instanceof OnClickListener) {
                                ((OnClickListener) object).onClick(v);
                            }
                        } catch (Exception e) {

                        }
                    }

                case MotionEvent.ACTION_POINTER_UP:
                    mode = 0;
                    float[] values = new float[9];
                    matrix.getValues(values);
                    makeImgCenter(values);
                    break;
                // 当屏幕上已经有触点(手指)，再有一个触点压下屏幕
                case MotionEvent.ACTION_POINTER_DOWN:
                    mode = MODE_ZOOM;
                    /** 计算两个手指间的距离 */
                    startDis = distance(event);
                    /** 计算两个手指间的中间点 */
                    if (startDis > 10f) { // 两个手指并拢在一起的时候像素大于10
                        midPoint = mid(event);
                        // 记录当前ImageView的缩放倍数
                        currentMatrix.set(getMatrix());
                    }
                    break;
                default:
            }
            setMatrix(matrix);
            return true;
        }

        /**
         * 计算两个手指间的距离
         */
        private float distance(MotionEvent event) {
            float dx = event.getX(1) - event.getX(0);
            float dy = event.getY(1) - event.getY(0);
            /** 使用勾股定理返回两点之间的距离 */
            return (float) Math.sqrt(dx * dx + dy * dy);
        }

        /**
         * 计算两个手指间的中间点
         */
        private PointF mid(MotionEvent event) {
            float midX = (event.getX(1) + event.getX(0)) / 2;
            float midY = (event.getY(1) + event.getY(0)) / 2;
            return new PointF(midX, midY);
        }

        /**
         * 和当前矩阵对比，检验dy，使图像移动后不会超出ImageView边界
         *
         * @param values
         * @param dy
         * @return
         */
        private float checkDyBound(float[] values, float dy) {

            float height = imgHeight;
            if (intrinsicHeight * values[Matrix.MSCALE_Y] < height) {
                return 0;
            }
            if (values[Matrix.MTRANS_Y] + dy > 0) {
                dy = -values[Matrix.MTRANS_Y];
            } else if (values[Matrix.MTRANS_Y] + dy < -(intrinsicHeight
                    * values[Matrix.MSCALE_Y] - height)) {
                dy = -(intrinsicHeight * values[Matrix.MSCALE_Y] - height)
                        - values[Matrix.MTRANS_Y];
            }
            return dy;
        }

        /**
         * 和当前矩阵对比，检验dx，使图像移动后不会超出ImageView边界
         *
         * @param values
         * @param dx
         * @return
         */
        private float checkDxBound(float[] values, float dx) {

            float width = imgWidth;
            if (intrinsicWidth * values[Matrix.MSCALE_X] < width) {
                return 0;
            }
            if (values[Matrix.MTRANS_X] + dx > 0) {
                dx = -values[Matrix.MTRANS_X];
            } else if (values[Matrix.MTRANS_X] + dx < -(intrinsicWidth
                    * values[Matrix.MSCALE_X] - width)) {
                dx = -(intrinsicWidth * values[Matrix.MSCALE_X] - width)
                        - values[Matrix.MTRANS_X];
            }
            return dx;
        }

        /**
         * MSCALE用于处理缩放变换
         * MSKEW用于处理错切变换
         * MTRANS用于处理平移变换
         */

        /**
         * 检验scale，使图像缩放后不会超出最大倍数
         *
         * @param scale
         * @param values
         * @return
         */
        private float checkFitScale(float scale, float[] values) {
            if (scale * values[Matrix.MSCALE_X] > mMaxScale) {
                scale = mMaxScale / values[Matrix.MSCALE_X];
            }
            if (scale * values[Matrix.MSCALE_X] < mMinScale) {
                scale = mMinScale / values[Matrix.MSCALE_X];
                isMinScale = true;
            }
            return scale;
        }

        /**
         * 促使图片居中
         *
         * @param values (包含着图片变化信息)
         */
        private void makeImgCenter(float[] values) {

            // 缩放后图片的宽高
            float zoomY = intrinsicHeight * values[Matrix.MSCALE_Y];
            float zoomX = intrinsicWidth * values[Matrix.MSCALE_X];
            // 图片左上角Y坐标
            float leftY = values[Matrix.MTRANS_Y];
            // 图片左上角X坐标
            float leftX = values[Matrix.MTRANS_X];
            // 图片右下角Y坐标
            float rightY = leftY + zoomY;
            // 图片右下角X坐标
            float rightX = leftX + zoomX;

            // 使图片垂直居中
            if (zoomY < imgHeight) {
                float marY = (imgHeight - zoomY) / 2.0f;
                matrix.postTranslate(0, marY - leftY);
            }

            // 使图片水平居中
            if (zoomX < imgWidth) {
                float marX = (imgWidth - zoomX) / 2.0f;
                matrix.postTranslate(marX - leftX, 0);

            }

            // 使图片缩放后上下不留白（即当缩放后图片的大小大于imageView的大小，但是上面或下面留出一点空白的话，将图片移动占满空白处）
            if (zoomY >= imgHeight) {
                if (leftY > 0) {// 判断图片上面留白
                    matrix.postTranslate(0, -leftY);
                }
                if (rightY < imgHeight) {// 判断图片下面留白
                    matrix.postTranslate(0, imgHeight - rightY);
                }
            }

            // 使图片缩放后左右不留白
            if (zoomX >= imgWidth) {
                if (leftX > 0) {// 判断图片左边留白
                    matrix.postTranslate(-leftX, 0);
                }
                if (rightX < imgWidth) {// 判断图片右边不留白
                    matrix.postTranslate(imgWidth - rightX, 0);
                }
            }
        }
    }

}
