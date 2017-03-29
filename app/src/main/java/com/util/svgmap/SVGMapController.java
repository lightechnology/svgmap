package com.util.svgmap;


import android.graphics.PointF;
import android.view.View.OnLongClickListener;

import com.util.svgmap.core.componet.MapMainView;

/**
 * 鍦板浘鎺у埗绫�
 *
 * @author forward
 * @since 1/7/2014
 */
public class SVGMapController
{
    private MapMainView mapMainView;

    public SVGMapController(SVGMapView mapView)
    {
        this.mapMainView = (MapMainView) mapView.getChildAt(0);
    }

    /**
     * 璁剧疆鏄惁寮�鍚湴鍥炬墜鍔跨Щ鍔紝榛樿寮�鍚�
     *
     * @param enabled true/false
     */
    public void setScrollGestureEnabled(boolean enabled)
    {
        this.mapMainView.setScrollGestureEnabled(enabled);
    }


    /**
     * 璁剧疆鏄惁寮�鍚湴鍥惧湴鍥炬墜鍔跨缉鏀撅紝榛樿寮�鍚�
     *
     * @param enabled
     */
    public void setZoomGestureEnabled(boolean enabled)
    {
        this.mapMainView.setZoomGestureEnabled(enabled);
    }


    /**
     * 璁剧疆鏄惁寮�鍚湴鍥炬墜鍔挎棆杞紝榛樿寮�鍚�
     *
     * @param enabled true/false
     */
    public void setRotationGestureEnabled(boolean enabled)
    {
        if (!enabled)
        {
            setCurrentRotationDegrees(0);
        }
        this.mapMainView.setRotationGestureEnabled(enabled);

    }


    /**
     * 璁剧疆鍦板浘鎵嬪娍缂╂斁涓績鏄惁涓烘墜鍔跨殑涓績鐐�
     *
     * @param enabled
     */
    public void setZoomWithTouchEventCenterEnabled(boolean enabled)
    {
        this.mapMainView.setZoomWithTouchEventCenter(enabled);
    }

    /**
     * 璁剧疆鍦板浘鎵嬪娍鏃嬭浆涓績鏄惁涓烘墜鍔跨殑涓績鐐�
     *
     * @param enabled true/false
     */
    public void setRotateWithTouchEventCenterEnabled(boolean enabled)
    {
        this.mapMainView.setRotateWithTouchEventCenter(enabled);
    }


    /**
     * 闈炲姩鐢诲舰寮忕殑鍦板浘绉诲姩
     *
     * @param x x鏂瑰悜鍋忕Щ閲� +鍙�
     * @param y y鏂瑰悜鍋忕Щ閲� +涓�
     */
    public void translateBy(float x, float y)
    {
        this.mapMainView.translateBy(x, y);
    }


    /**
     * 浠ラ潪鍔ㄧ敾褰㈠紡璁剧疆褰撳墠鍦板浘鐨勭缉鏀炬瘮渚�
     *
     * @param zoom   缂╂斁鐜�
     * @param pivotX 缂╂斁涓績鐐箈
     * @param pivotY 缂╂斁涓績鐐箉
     */
    public void setCurrentZoomValue(float zoom, float pivotX, float pivotY)
    {
        this.mapMainView.setCurrentZoomValue(zoom, pivotX, pivotY);
    }

    public void setCurrentZoomValue(float zoom)
    {
        setCurrentZoomValue(zoom, mapMainView.getWidth() / 2, mapMainView.getHeight() / 2);
    }

    /**
     * 浠ラ潪鍔ㄧ敾鐨勫舰寮忚缃綋鍓嶅湴鍥剧殑鏃嬭浆瑙掑害 銆�0锛�360銆�
     *
     * @param degrees
     */
    public void setCurrentRotationDegrees(float degrees)
    {
        setCurrentRotationDegrees(degrees, mapMainView.getWidth() / 2, mapMainView.getHeight() / 2);
    }

    public void setCurrentRotationDegrees(float degrees, float pivotX, float pivotY)
    {
        this.mapMainView.setCurrentRotationDegrees(degrees, pivotX, pivotY);
    }


    /**
     * 璁剧疆鍦板浘鐨勬渶澶х缉缂╂斁姣斾緥锛岄粯璁や负鎵�鏈夊湴鍥句笂鐨刄nit鏄剧ず鏅傜殑澶у皬
     *
     * @param maxZoomValue
     */
    public void setMaxZoomValue(float maxZoomValue)
    {
        this.mapMainView.setMaxZoomValue(maxZoomValue);
    }

    /**
     * 璁剧疆鍦板浘闂儊鐐�
     *
     * @param point       鐩爣鐐�
     * @param radius      鍗婂緞
     * @param color       棰滆壊
     * @param repeatTimes 閲嶅娆℃暟
     */
    public void sparkAtPoint(PointF point, float radius, int color, int repeatTimes)
    {
        this.mapMainView.sparkAtPoint(point, radius, color, repeatTimes);
    }
    
    public void setOnLongClickListener(OnLongClickListener onLongClickListener) {
        this.mapMainView.setOnLongClickListener(onLongClickListener);
    }
    
    public boolean isLongClickable() {
        return this.mapMainView.isLongClickable();
    }

}
