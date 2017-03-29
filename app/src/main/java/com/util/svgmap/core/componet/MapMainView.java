package com.util.svgmap.core.componet;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Picture;
import android.graphics.PointF;
import android.graphics.Rect;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceHolder.Callback;
import android.view.SurfaceView;
import android.view.View;
import android.view.WindowManager;

import com.util.svgmap.SVGMapViewListener;
import com.util.svgmap.core.helper.CommonMathHelper;
import com.util.svgmap.core.helper.map.SVGBuilder;
import com.util.svgmap.overlay.SVGMapBaseOverlay;

import java.util.ArrayList;
import java.util.List;


public class MapMainView extends SurfaceView implements Callback
{

    private static final String TAG = "MapMainView";

    private SVGMapViewListener mapViewListener = null;
    private SurfaceHolder surfaceHolder;
    private List<SVGMapBaseOverlay> layers;
    private MapOverlay mapOverlay;
    private SparkOverlay sparkOverlay;

    private boolean isRotationGestureEnabled = true;
    private boolean isZoomGestureEnabled = true;
    private boolean isScrollGestureEnabled = true;
    private boolean isRotateWithTouchEventCenter = false;
    private boolean isZoomWithTouchEventCenter = false;
    private boolean isMapLoadFinsh = false;


    private static final int TOUCH_STATE_REST = 0;
    private static final int TOUCH_STATE_SCROLLING = 1;
    private static final int TOUCH_STATE_SCALING = 2;
    private static final int TOUCH_STATE_ROTATE = 4;
    private static final int TOUCH_STATE_POINTED = 8;
    private int mTouchState = MapMainView.TOUCH_STATE_REST;

    private float disX; // 判断旋转和缩放的手势专用
    private float disY; // 判断的方式是夹角 钝角啊blablabla
    private float disZ; // 三角形的三条边 用的是余弦定理
    private float lastX;
    private float lastY;

    private float minZoomValue = 2.0f; // 默认的最小缩放
    private float maxZoomValue = 5.0f;
    private boolean isFirstPointedMove = true;

    private Matrix matrix = new Matrix(); // 当前地图应用的矩阵变化
    private Matrix savedMatrix = new Matrix(); // 保存手势Down下时的矩阵

    private PointF start = new PointF(); // 手势触摸的起始点
    private PointF mid = new PointF(); // 双指手势的中心点

    private float firstDegrees; //
    private float firstDistance; // 判断旋转和缩放的手势专用

    private float rotateDegrees = 0f;
    private float currentRotateDegrees = 0f;
    private float zoom = 1f;
    private float currentZoom = 1f;

    private Rect dirty = null;
    
    private float initScale = 0f;
    private float mvX = 0f;
    private float mvY = 0f;
    private float mpX = 0f;
    private float mpY = 0f;
    
    private float mcX = 0f;
    private float mcY = 0f;

    public MapMainView(Context context, AttributeSet attrs)
    {
        this(context, attrs, 0);
    }

    public MapMainView(Context context, AttributeSet attrs, int defStyle)
    {
        super(context, attrs, defStyle);
        initMapView();
    }

    private void initMapView()
    {
        layers = new ArrayList<SVGMapBaseOverlay>()
        {
            private static final long serialVersionUID = 1L;

            @Override
            public boolean add(SVGMapBaseOverlay overlay)
            {
                synchronized (this)
                {
                    if (this.size() != 0)
                    {
                        if (overlay.showLevel >= this.get(this.size() - 1).showLevel)
                        {
                            super.add(overlay);
                        }
                        else
                        {
                            for (int i = 0; i < this.size(); i++)
                            {
                                if (overlay.showLevel <= this.get(i).showLevel)
                                {
                                    super.add(i, overlay);
                                    break;
                                }
                            }
                        }
                    }
                    else
                    {
                        super.add(overlay);
                    }

                }
                return true;
            }

            @Override
            public void clear()
            {
                super.clear();
                MapMainView.this.mapOverlay = null;
            }
        };
        getHolder().addCallback(this);
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height)
    {
        this.surfaceHolder = holder;
        if (dirty == null || dirty.bottom == 0 || dirty.right == 0)
        {
            dirty = new Rect(0, 0, this.getWidth(), this.getHeight());
        }
        if (surfaceHolder != null)
        {
            this.refresh();
        }
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder)
    {
        this.surfaceHolder = holder;
        Canvas canvas = holder.lockCanvas();
        canvas.drawColor(-1);
        holder.unlockCanvasAndPost(canvas);
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder)
    {

    }

    public void refresh()
    {
        try
        {
            if (surfaceHolder != null)
            {
                synchronized (this.surfaceHolder)
                {
                    Canvas canvas = surfaceHolder.lockCanvas(dirty);
                    if (canvas != null)
                    {
                        canvas.drawColor(-1);
                        for (int i = 0; i < layers.size(); i++)
                        {
                            if (layers.get(i).isVisible)
                            {
                                layers.get(i).draw(canvas, matrix, currentZoom, currentRotateDegrees);
                            }
                        }
                        surfaceHolder.unlockCanvasAndPost(canvas);
                    }
                }
            }
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }

    @Override
    public boolean performClick() {
        return super.performClick();
    }

    @Override
    public boolean onTouchEvent(MotionEvent event)
    {
        performClick();
        if (!isMapLoadFinsh || mapOverlay == null)
        {
            return false;
        }

        switch (event.getAction() & MotionEvent.ACTION_MASK)
        {

            case MotionEvent.ACTION_DOWN:
                savedMatrix.set(matrix);
                start.set(event.getX(), event.getY());
                this.mTouchState = TOUCH_STATE_SCROLLING;
                break;
            case MotionEvent.ACTION_POINTER_DOWN:
                if (event.getPointerCount() == 2)
                {
                    this.mTouchState = TOUCH_STATE_POINTED;
                }
                break;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_POINTER_UP:
                Log.i("xxxxxxxxxxxx-yao", "event.getX(): " + event.getX() + " event.getY(): " + event.getY());
                Log.i("xxxxxxxxxxxx-yao", "event.getRawX(): " + event.getRawX() + " event.getRawY(): " + event.getRawY());
                Log.i("xxxxxxxxxxxx-yao", "mpX: " + mpX + " mpY: " + mpY + " mvX: " + mvX + " mvY: " + mvY);
                Log.i("xxxxxxxxxxxx-yao", "mcX: " + mcX + " mcY: " + mcY);
                float px = eventX2MapX(event.getX());
                float py = eventY2MapY(event.getY());
                Log.i("xxxxxxxxxxxx-yao", "pX: " + px + " pY: " + py + " initScale: " + initScale);
                isFirstPointedMove = true;
                this.refresh();
                if (this.mTouchState == TOUCH_STATE_SCALING)
                {
                    this.zoom = this.currentZoom;
                }
                else if (this.mTouchState == TOUCH_STATE_ROTATE)
                {
                    this.rotateDegrees = this.currentRotateDegrees;
                }
                else if (withFloorPlan(event.getX(), event.getY()) && event.getAction() == MotionEvent.ACTION_UP)
                {
                    try
                    {
                        for (int i = 0; i < layers.size(); i++)
                        {
                            layers.get(i).onTap(event);
                        }
                    }
                    catch (Exception e)
                    {
                        e.printStackTrace();
                    }
                }

                if (!isRotationGestureEnabled)
                {
                    // 調整地圖的位置居中顯示
                    mapCenter(true, true);
                }

                this.mTouchState = TOUCH_STATE_REST;
                break;

            case MotionEvent.ACTION_MOVE:
                if (this.mTouchState == TOUCH_STATE_POINTED)
                {
                    if (isFirstPointedMove)
                    {
                        midPoint(mid, event);
                        lastX = event.getX(0);
                        lastY = event.getY(0);
                        disX = CommonMathHelper.getDistanceBetweenTwoPoints(event.getX(0), event.getY(0), mid.x, mid.y);
                        isFirstPointedMove = false;
                    }
                    else
                    {
                        savedMatrix.set(matrix);
                        disY = CommonMathHelper.getDistanceBetweenTwoPoints(lastX, lastY, event.getX(0), event.getY(0));
                        disZ = CommonMathHelper.getDistanceBetweenTwoPoints(mid.x, mid.y, event.getX(0), event.getY(0));
                        if (justRotateGesture())
                        {
                            firstDegrees = rotation(event);
                            this.mTouchState = TOUCH_STATE_ROTATE;
                            Log.i("xxxxxxxxxxxx-yao", "this.mTouchState: " + this.mTouchState);
                        }
                        else
                        {
                            firstDistance = spaceBetweenTwoEvents(event);
                            this.mTouchState = TOUCH_STATE_SCALING;
                        }
                    }
                }
                if (this.mTouchState == TOUCH_STATE_SCALING)
                {
                    if (this.isZoomGestureEnabled)
                    {
                        matrix.set(savedMatrix);
                        if (isZoomWithTouchEventCenter)
                        {
                            midPoint(mid, event);
                        }
                        else
                        {
                            mid.x = this.getWidth() / 2;
                            mid.y = this.getHeight() / 2;
                        }
                        // 求地图左上角离mid的屏幕坐标距离
                        float xxx = eventX2MapX(mid.x) * initScale;
                        float yyy = eventY2MapY(mid.y) * initScale;
                        float sencondDistance = spaceBetweenTwoEvents(event);
                        float scale = sencondDistance / firstDistance;
                        Log.i("xxxxxxxxxxxx-yao", "scale: " + scale);
                        Log.i("xxxxxxxxxxxx-yao", "initScale: " + initScale);
                        xxx *= scale;
                        yyy *= scale;
                        mpX = mid.x - xxx;
                        mpY = mid.y - yyy;
                        initScale = initScale * scale;
                        Log.i("xxxxxxxxxxxx-yao", "initScale: " + initScale);
                        float ratio = this.zoom * scale;
                        if (ratio < minZoomValue)
                        {
                            ratio = minZoomValue;
                            scale = ratio / this.zoom;
                        }
                        else if (ratio > maxZoomValue)
                        {
                            ratio = maxZoomValue;
                            scale = ratio / this.zoom;
                        }
                        this.currentZoom = ratio;
                        this.matrix.postScale(scale, scale, mid.x, mid.y);
                        this.refresh();
                    }
                }
                if (this.mTouchState == TOUCH_STATE_ROTATE)
                {
                    if (this.isRotationGestureEnabled)
                    {
                        matrix.set(savedMatrix);
                        if (isRotateWithTouchEventCenter)
                        {
                            midPoint(mid, event);
                        }
                        else
                        {
                            mid.x = this.getWidth() / 2;
                            mid.y = this.getHeight() / 2;
                        }
                        float deltaDegrees = rotation(event) - firstDegrees;
                        float tempD = (this.rotateDegrees + deltaDegrees) % 360;
                        this.currentRotateDegrees = tempD > 0 ? tempD : 360 + tempD;
                        this.matrix.postRotate(deltaDegrees, mid.x, mid.y);
                        this.refresh();
                    }
                }
                if (this.mTouchState == TOUCH_STATE_SCROLLING)
                {
                    if (this.isScrollGestureEnabled)
                    {
                        Log.i("xxxxxxxxxxxx-yao", "event.getX(): " + event.getX() + " event.getY(): " + event.getY() + " start.x: " + start.x + " start.y: " + start.y);
                        matrix.set(savedMatrix);
                        mvX = event.getX() - start.x;
                        mvY = event.getY() - start.y;
                        matrix.postTranslate(mvX, mvY);
                        Log.i("xxxxxxxxxxxx-yao", "event.getX() - start.x: " + mvX + " event.getY() - start.y: " + mvY);
                        
                        mcX = mvX;
                        mcY = mvY;
                        Log.i("xxxxxxxxxxxx-yao", "--------mcX: " + mcX + " mcY: " + mcY);
                        
                        this.refresh();
                    }
                }
                break;
        }
        return true;
    }

    public void onDestroy()
    {
        try
        {
            for (int i = 0; i < layers.size(); i++)
            {
                layers.get(i).onDestroy();
            }
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }

    public void onPause()
    {
        try
        {
            for (int i = 0; i < layers.size(); i++)
            {
                layers.get(i).onPause();
            }
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }

    public void onResume()
    {
        try
        {
            for (int i = 0; i < layers.size(); i++)
            {
                layers.get(i).onResume();
            }
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }

    public float getCurrentRotateDegrees()
    {
        return this.currentRotateDegrees;
    }

    public void setCurrentRotationDegrees(float degrees, float pivotX, float pivotY)
    {
        if (isRotationGestureEnabled)
        {
            this.matrix.postRotate(-currentRotateDegrees + degrees, pivotX, pivotY);
            this.rotateDegrees = this.currentRotateDegrees = degrees;
            setCurrentRotateDegreesWithRule();
            refresh();
            mapCenter(true, true);
        }
    }

    public float getCurrentZoomValue()
    {
        return this.currentZoom;
    }

    public void setCurrentZoomValue(float zoom, float pivotX, float pivotY)
    {
        this.matrix.postScale(zoom / currentZoom, zoom / currentZoom, pivotX, pivotY);
        this.zoom = this.currentZoom = zoom;
        this.refresh();
    }

    public float getMaxZoomValue()
    {
        return maxZoomValue;
    }

    public void setMaxZoomValue(float maxZoomValue)
    {
        this.maxZoomValue = maxZoomValue;
    }

    public List<SVGMapBaseOverlay> getOverLays()
    {
        return this.layers;
    }

    public void translateBy(float x, float y)
    {
        this.matrix.postTranslate(x, y);
    }

    public float getMinZoomValue()
    {
        return minZoomValue;
    }

    public void setMinZoomValue(float minZoomValue)
    {
        this.minZoomValue = minZoomValue;
    }


    public float[] getMapCoordinateWithScreenCoordinate(float x, float y)
    {
        Matrix inverMatrix = new Matrix();
        float returnValue[] = {x, y};
        this.matrix.invert(inverMatrix);
        inverMatrix.mapPoints(returnValue);
        return returnValue;
    }

    public void registeMapViewListener(SVGMapViewListener mapViewListener)
    {
        this.mapViewListener = mapViewListener;
    }


    public void loadMap(final String svgString)
    {
        isMapLoadFinsh = false;
        new Thread()
        {
            @Override
            public void run()
            {
                super.run();
                Picture picture = new SVGBuilder().readFromString(svgString).build().getPicture();
                if (picture != null)
                {
                    if (MapMainView.this.mapOverlay == null)
                    {
                        MapMainView.this.mapOverlay = new MapOverlay(MapMainView.this);
                        MapMainView.this.getOverLays().add(mapOverlay);
                    }
                    MapMainView.this.mapOverlay.setData(picture);
                    Log.i(TAG, "mapLoadFinished");
                    if (mapViewListener != null)
                    {
                        while (0 == MapMainView.this.getWidth());
                        if (picture.getWidth() > picture.getHeight()) {
                            Log.i("xxxxxxxxxxxx-yao", "MapMainView.this.getWidth(): " + MapMainView.this.getWidth() + " picture.getWidth(): " + picture.getWidth());
                            initScale = (float) MapMainView.this.getWidth() / picture.getWidth();
                            Log.i("xxxxxxxxxxxx-yao", "0initScale:" + initScale);
                            mpX = 0f;
                            mpY = (MapMainView.this.getHeight() - picture.getHeight() * initScale) / 2;
                        } else {
                            Log.i("xxxxxxxxxxxx-yao", "MapMainView.this.getHeight(): " + MapMainView.this.getHeight() + " picture.getHeight(): " + picture.getHeight());
                            initScale = (float) MapMainView.this.getHeight() / picture.getHeight();
                            Log.i("xxxxxxxxxxxx-yao", "1initScale:" + initScale);
                            mpX = (MapMainView.this.getWidth() - picture.getWidth() * initScale) / 2;
                            mpY = 0f;
                        }
                        /*Log.i("xxxxxxxxxxxx-yao", "w:" + picture.getWidth() + " h: " + picture.getHeight());
                        DisplayMetrics outMetrics = new DisplayMetrics();
                        ((WindowManager) getContext().getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay().getMetrics(outMetrics);
                        Log.i("xxxxxxxxxxxx-yao", "w:" + outMetrics.widthPixels + " h: " + outMetrics.heightPixels);
                        Log.i("2xxxxxxxxxxxx-yao", "w:" + MapMainView.this.getMeasuredWidth() + " h: " + MapMainView.this.getMeasuredHeight());*/
                        mapViewListener.onMapLoadComplete();
                    }
                    isMapLoadFinsh = true;
                }
                else
                {
                    if (mapViewListener != null)
                    {
                        mapViewListener.onMapLoadError();
                    }
                }
            }
        }.start();
    }

    public void setRotationGestureEnabled(boolean enabled)
    {
        this.isRotationGestureEnabled = enabled;
    }

    public void setZoomGestureEnabled(boolean enabled)
    {
        this.isZoomGestureEnabled = enabled;
    }

    public void setScrollGestureEnabled(boolean enabled)
    {
        this.isScrollGestureEnabled = enabled;
    }

    public void setRotateWithTouchEventCenter(boolean isRotateWithTouchEventCenter)
    {
        this.isRotateWithTouchEventCenter = isRotateWithTouchEventCenter;
    }

    public void setZoomWithTouchEventCenter(boolean isZoomWithTouchEventCenter)
    {
        this.isZoomWithTouchEventCenter = isZoomWithTouchEventCenter;
    }

    public boolean isMapLoadFinsh()
    {
        return this.isMapLoadFinsh;
    }


    public void sparkAtPoint(PointF point, float radius, int color, int repeatTimes)
    {
        sparkOverlay = new SparkOverlay(this, radius, point, color, repeatTimes);
        this.layers.add(sparkOverlay);
    }

    public void getCurrentMap()
    {
        try
        {
            Bitmap bitmap = Bitmap.createBitmap(getWidth(), getHeight(), Bitmap.Config.ARGB_8888);
            Canvas bitCanvas = new Canvas(bitmap);
            for (SVGMapBaseOverlay layer : layers)
            {
                layer.draw(bitCanvas, matrix, currentZoom, currentRotateDegrees);
            }
            if (mapViewListener != null)
            {
                mapViewListener.onGetCurrentMap(bitmap);
            }
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }

    /**
     * **********************************************************************
     */

    // 地图居中显示
    private void mapCenter(boolean horizontal, boolean vertical)
    {
        Matrix m = new Matrix();
        m.set(matrix);
        RectF mapRect = new RectF(0, 0, this.mapOverlay.getFloorMap().getWidth(), this.mapOverlay.getFloorMap().getHeight());
        m.mapRect(mapRect);
        float width = mapRect.width();
        float height = mapRect.height();
        float deltaX = 0;
        float deltaY = 0;
        if (vertical)
        {
            if (height < this.getHeight())
            {
                deltaY = (getHeight() - height) / 2 - mapRect.top;
            }
            else if (mapRect.top > 0)
            {
                deltaY = -mapRect.top;
            }
            else if (mapRect.bottom < getHeight())
            {
                deltaY = getHeight() - mapRect.bottom;
            }
        }

        if (horizontal)
        {
            if (width < getWidth())
            {
                deltaX = (getWidth() - width) / 2 - mapRect.left;

            }
            else if (mapRect.left > 0)
            {
                deltaX = -mapRect.left;
            }
            else if (mapRect.right < getWidth())
            {
                deltaX = getWidth() - mapRect.right;
            }
            matrix.postTranslate(deltaX, deltaY);
        }
        refresh();
    }

    private float rotation(MotionEvent event)
    {
        float delta_x = (event.getX(0) - event.getX(1));
        float delta_y = (event.getY(0) - event.getY(1));
        double radians = Math.atan2(delta_y, delta_x);
        return (float) Math.toDegrees(radians);
    }

    private float spaceBetweenTwoEvents(MotionEvent event)
    {
        float x = event.getX(0) - event.getX(1);
        float y = event.getY(0) - event.getY(1);
        return (float) Math.sqrt(x * x + y * y);
    }

    private void midPoint(PointF point, MotionEvent event)
    {
        float x = event.getX(0) + event.getX(1);
        float y = event.getY(0) + event.getY(1);
        point.set(x / 2, y / 2);
    }

    private boolean justRotateGesture()
    {
        if (!this.isRotationGestureEnabled)
        {
            return false;
        }
        float cos = (disX * disX + disY * disY - disZ * disZ) / (2 * disX * disY);
        if (Float.isNaN(cos))
        {
            return false;
        }
        if (Math.acos(cos) * (180 / Math.PI) < 120 && Math.acos(cos) * (180 / Math.PI) > 45)
            if (Math.acos(cos) * (180 / Math.PI) < 120 && Math.acos(cos) * (180 / Math.PI) > 45)
            {
                return true;
            }
        return false;
    }

    public float[] getHorizontalDistanceWithRotateDegree(float degrees, float x, float y)
    {
        float[] goal = new float[2];
        double f = Math.PI * (degrees / 180.0F);
        goal[0] = (float) (x * Math.cos(f) - y * Math.sin(f));
        goal[1] = (float) (x * Math.sin(f) + y * Math.cos(f));
        return goal;
    }


    private void setCurrentRotateDegreesWithRule()
    {
        if (getCurrentRotateDegrees() > 360)
        {
            this.currentRotateDegrees = getCurrentRotateDegrees() % 360;
        }
        else if (getCurrentRotateDegrees() < 0)
        {
            this.currentRotateDegrees = 360 + (getCurrentRotateDegrees() % 360);
        }
    }
    
    /**
     * point is/not in floor plan
     *
     * @param x
     * @param y
     * @return
     */
    public boolean withFloorPlan(float x, float y) {
        float[] goal = getMapCoordinateWithScreenCoordinate(x, y);
        return goal[0] > 0 && goal[0] < mapOverlay.getFloorMap().getWidth() && goal[1] > 0
                && goal[1] < mapOverlay.getFloorMap().getHeight();
    }
    
    private float eventX2MapX(float x) {
        mpX += mvX;
        mvX = 0;
        return (x - mpX) / initScale;
    }
    
    private float eventY2MapY(float y) {
        mpY += mvY;
        mvY = 0;
        return (y - mpY) / initScale;
    }
    
    private float spaceBetweenTwoEvents4Map(MotionEvent event)
    {
        float x = eventX2MapX(event.getX(0)) - eventX2MapX(event.getX(1));
        float y = eventX2MapX(event.getY(0)) - eventX2MapX(event.getY(1));
        return (float) Math.sqrt(x * x + y * y);
    }

}
