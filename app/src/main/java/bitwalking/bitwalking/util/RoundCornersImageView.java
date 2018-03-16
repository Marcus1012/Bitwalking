package bitwalking.bitwalking.util;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Path;
import android.graphics.RectF;
import android.os.Build;
import android.util.AttributeSet;
import android.view.View;
import android.widget.ImageView;

/**
 * Created by Marcus on 12/4/15.
 */
public class RoundCornersImageView extends ImageView {

    public static float radius = 500.0f;
    private static boolean doClip = true;

    public RoundCornersImageView(Context context) {
        super(context);
    }

    public RoundCornersImageView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public RoundCornersImageView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        Path clipPath = new Path();
        RectF rect = new RectF(0, 0, this.getWidth(), this.getHeight());
        clipPath.addRoundRect(rect, radius, radius, Path.Direction.CW);

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN_MR2
                && Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            setLayerType(LAYER_TYPE_SOFTWARE, null);
        }

        if (doClip) {
            try {
                canvas.clipPath(clipPath);
            } catch (UnsupportedOperationException e) {
                doClip = false;
            }
        }

        super.onDraw(canvas);
    }
}