package emperatriz.spin;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.text.format.Time;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Random;

/**
 * Created by ramon on 1/05/15.
 */
public class DrawUtils {

    public static int height,width, p20;
    public static Canvas canvas;
    public static long now;
    public static Time mTime;
    public static boolean isInAmbientMode;
    public static Context ctx;
    public static String ACCENT_COLOR="accent";

    private static int day=0xffaaaaaa, night= 0xffaaaaaa;


    private static Paint paint = new Paint();






    private static float p20(float factor){
        return p20*factor;
    }





    public static void drawBackground(int color, Paint paint){
        if (!isInAmbientMode){
            paint.setColor(color);
            canvas.drawRect(0, 0, width, height, paint);
        } else {
            paint.setColor(0xff000000);
            canvas.drawRect(0, 0, width, height,paint);
        }
    }




    public static void drawDate(int color, Paint paint2){


        SimpleDateFormat sdf = new SimpleDateFormat("d MMMM");

        SimpleDateFormat sdf2 = new SimpleDateFormat("EEEE");

        Paint paint = paint2;
        paint.setAntiAlias(true);

        paint.setTextSize(p20(1.81f));



        boolean dayTime = (mTime.hour<20) && (mTime.hour>8);

        String dateText = sdf.format(Calendar.getInstance().getTime()).toUpperCase();
        String dateText2 = sdf2.format(Calendar.getInstance().getTime()).toUpperCase();

        float textWidth = paint.measureText(dateText);
        float textWidth2 = paint.measureText(dateText2);

        Rect r = new Rect();
        paint.getTextBounds(mTime.monthDay + "", 0, (mTime.monthDay + "").length(), r);

        if (!isInAmbientMode) {
//            paint.setShadowLayer(shadowRadius,0,0,color2);
            paint.setColor(color);

        }
        else{
//            paint.setShadowLayer(shadowRadius,0,0,0xff000000);
            paint.setColor(dayTime?day:night);
        }
        canvas.drawText(dateText,width/2-textWidth/2,p20(6.43f),paint);
        canvas.drawText(dateText2,width/2-textWidth2/2,height-p20(6.75f)+(p20(1.81f)),paint);


    }

    public static void drawSeconds(int color){
        if (!isInAmbientMode) {




            RectF r1 = new RectF();

            paint.setStrokeWidth(p20(3.3f));
            paint.setAntiAlias(true);
            paint.setStrokeCap(Paint.Cap.BUTT);
            paint.setStyle(Paint.Style.STROKE);
            paint.setFilterBitmap(false);
            float margin = 1.65f;
            r1.set(p20(margin), p20(margin), width - p20(margin), width - p20(margin));

            paint.setShadowLayer(4, 0, 0, 0x00000000);

            float millis = System.currentTimeMillis()%60000;
            float startAngle = (360*millis/60000)-90;

//            paint.setColor(0xff000000);
//            canvas.drawArc(r1, startAngle - 1, 4, false, paint);

            paint.setColor(color);
            paint.setShadowLayer(4, 0, 0, 0xff000000);
            canvas.drawArc(r1, startAngle, 3, false, paint);
            //canvas.drawArc(r1, startAngle, 2, false, paint);




        }

    }

    public static void drawSpin(int color, int speed, float widthStroke, float size, boolean clockwise){
        color = Color.argb(70, Color.red(color), Color.green(color), Color.blue(color));
        if (!isInAmbientMode) {
            RectF r1 = new RectF();

            paint.setStrokeWidth(p20(widthStroke*2));
            paint.setAntiAlias(true);
            paint.setStrokeCap(Paint.Cap.BUTT);
            paint.setStyle(Paint.Style.STROKE);
            paint.setFilterBitmap(false);

            r1.set(p20(widthStroke), p20(widthStroke), width - p20(widthStroke), width - p20(widthStroke));

            paint.setShadowLayer(0, 0, 0, 0xff000000);

            paint.setColor(color);
            float millis = System.currentTimeMillis()%(speed*1000);
            if (!clockwise){
                millis=speed*1000-millis;
            }
            float startAngle = (360*millis/(speed*1000))-125;
            canvas.drawArc(r1, startAngle, 360*size, false, paint);
        }

    }


    public static float random(float max, float min){
        Random r = new Random();
        return (max-min)*r.nextFloat()+min;
    }

    public static void drawCenteredText(String text, Paint paint2){
        float size=p20(6.125f);
//        text = text.replace("1","l");
        Paint paint = paint2;
        paint.setAntiAlias(true);
        paint.setTextSize(size);


        float textWidth = paint.measureText(text);

        if (isInAmbientMode){
            paint.setColor(0xff000000);
            paint.setShadowLayer(2, 0, 0, 0xffffffff);
            canvas.drawText(text, width / 2 - textWidth / 2, (height - size) / 2 + size - p20(0.875f), paint);
            canvas.drawText(text, width / 2 - textWidth / 2, (height - size) / 2 + size - p20(0.875f), paint);
            canvas.drawText(text, width / 2 - textWidth / 2, (height - size) / 2 + size - p20(0.875f), paint);
            canvas.drawText(text, width / 2 - textWidth / 2, (height - size) / 2 + size - p20(0.875f), paint);
            canvas.drawText(text, width / 2 - textWidth / 2, (height - size) / 2 + size - p20(0.875f), paint);
            canvas.drawText(text, width / 2 - textWidth / 2, (height - size) / 2 + size - p20(0.875f), paint);


        }else{
            paint.setColor(0xffffffff);
            paint.setShadowLayer(0, 0, 0, 0xffffffff);
        }


        canvas.drawText(text,width/2-textWidth/2,(height-size)/2+size-14,paint);

    }

}
