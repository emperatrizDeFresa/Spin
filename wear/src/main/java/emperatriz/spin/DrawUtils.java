package emperatriz.spin;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
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

    private static int day=0xff888888, night= 0xff888888;


    private static Paint paint = new Paint();




    public static float p20(float factor){
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

    public static float drawCenteredText(String t,  Paint paint){
        float size=p20(10f);

        float gap=180;
        if (!isInAmbientMode) {
            long millis = System.currentTimeMillis() % 60000;
            if (millis > 60000 - gap) {
                size = p20(11f) * ((60000 - millis) / gap);
            }
            if (millis < gap) {
                size = p20(11f) * ((millis) / gap);
            }
        }
        paint.setAntiAlias(true);
        paint.setTextSize(size);



        int cHeight = canvas.getClipBounds().height();
        int cWidth = canvas.getClipBounds().width();
        Rect r = new Rect();
        paint.setTextAlign(Paint.Align.LEFT);
        paint.getTextBounds(t, 0, t.length(), r);
        Rect r2 = new Rect();
        float xPos = cWidth / 2f - (r.width()+r2.width()) / 2f - r.left - r2.left;
        float x2Pos = xPos + r.width()+ r2.left*2;
        float yPos = cHeight / 2f + r.height() / 2f - r.bottom;

        if (isInAmbientMode){
            paint.setColor(0xff000000);
            paint.setShadowLayer(2, 0, 0, 0xffffffff);
            canvas.drawText(t, xPos, yPos, paint);
            canvas.drawText(t, xPos, yPos, paint);
            canvas.drawText(t, xPos, yPos, paint);
            canvas.drawText(t, xPos, yPos, paint);
            canvas.drawText(t, xPos, yPos, paint);
            canvas.drawText(t, xPos, yPos, paint);



        }else{
            paint.setColor(0xffffffff);
            paint.setShadowLayer(0, 0, 0, 0xffffffff);
            canvas.drawText(t, xPos, yPos, paint);
        }


        return r.height();

    }



    public static void drawDate(int color, Paint paint2, boolean normal, int steps, float centerGap){

        float padding = p20(1.5f);

        SimpleDateFormat sdf = new SimpleDateFormat("d MMMM");

        SimpleDateFormat sdf2 = new SimpleDateFormat("EEEE");

        Paint paint = paint2;
        paint.setAntiAlias(true);

        paint.setTextSize(Math.round(p20(2f)));
        paint.setFakeBoldText(true);
        paint.setSubpixelText(true);



        boolean dayTime = (mTime.hour < 20) && (mTime.hour>8);

        String dateText = sdf.format(Calendar.getInstance().getTime()).toUpperCase().replace(".","");
        String dateText2 = normal?sdf2.format(Calendar.getInstance().getTime()).toUpperCase():steps+ctx.getString(R.string.steps);

        float textWidth = paint.measureText(dateText);
        float textWidth2 = paint.measureText(dateText2);

        int cHeight = canvas.getClipBounds().height();
        int cWidth = canvas.getClipBounds().width();
        Rect r = new Rect();
        paint.setTextAlign(Paint.Align.LEFT);
        paint.getTextBounds(dateText, 0, dateText.length(), r);
        float xUp = cWidth / 2f - r.width() / 2f - r.left;
        float yUp = cHeight / 2f + r.height() / 2f - r.bottom - centerGap/2 - padding;
        paint.getTextBounds(dateText2, 0, dateText2.length(), r);
        float xDown = cWidth / 2f - r.width() / 2f - r.left;
        float yDown = cHeight / 2f + r.height() / 2f - r.bottom + centerGap/2 + padding;


//        Rect r = new Rect();
//        paint.getTextBounds(mTime.monthDay + "", 0, (mTime.monthDay + "").length(), r);

        if (!isInAmbientMode) {
            paint.setShadowLayer(0, 0, 0, 0xff000000);
            paint.setColor(color);

        }
        else{
            paint.setShadowLayer(0, 0, 0, 0xff000000);
            paint.setColor(dayTime ? day : night);
        }

        canvas.drawText(dateText,xUp,yUp,paint);
        canvas.drawText(dateText2,xDown, yDown,paint);


    }

    public static void drawSeconds(int color, float chunk){
        if (!isInAmbientMode) {

            RectF r1 = new RectF();

            paint.setStrokeWidth(p20(chunk * 8));
            paint.setAntiAlias(true);
            paint.setStrokeCap(Paint.Cap.BUTT);
            paint.setStyle(Paint.Style.STROKE);
            paint.setFilterBitmap(false);
            float margin = chunk*4;
            r1.set(p20(margin), p20(margin), width - p20(margin), width - p20(margin));

            float millis = System.currentTimeMillis()%60000;
            float startAngle = (360*millis/60000)-90;

            paint.setColor(Color.argb(160, Color.red(0), Color.green(0), Color.blue(0)));
            canvas.drawArc(r1, startAngle-4, 5, false, paint);
            paint.setColor(color);
            canvas.drawArc(r1, startAngle - 3, 3, false, paint);

        }

    }

    public static void drawSeconds2(int color, float chunk){
        if (!isInAmbientMode) {

           RectF r1 = new RectF();

            paint.setStrokeWidth(p20(chunk * 8));
            paint.setAntiAlias(true);
            paint.setStrokeCap(Paint.Cap.BUTT);
            paint.setStyle(Paint.Style.STROKE);
            paint.setFilterBitmap(false);
            float margin = 3.65f;
            r1.set(0, 0, width, width);


            float millis = System.currentTimeMillis()%60000;
            float startAngle = (360*millis/60000)-90;

            paint.setColor(Color.argb(160, Color.red(0), Color.green(0), Color.blue(0)));
            canvas.drawArc(r1, startAngle-4, 5, false, paint);
            paint.setColor(color);
            canvas.drawArc(r1, startAngle - 3, 3, false, paint);




        }

    }


    public static void drawCircle(int color, float chunk, Paint textPaint, boolean isRound){
        if (!isInAmbientMode) {

            chunk = p20(chunk);

            paint.setStrokeCap(Paint.Cap.BUTT);
            paint.setAntiAlias(true);
            paint.setStyle(Paint.Style.FILL);
            paint.setFilterBitmap(false);
            //paint.setShadowLayer(4, 0, 0, 0xff000000);
            paint.setColor(0xff000000);

            float radius = isRound?chunk*4:chunk*2;


            float distanceFromCenter =  width/2 - radius;


            float millis = System.currentTimeMillis()%60000;
            double angle = (2*Math.PI*millis/60000)-Math.PI/2 - 0.03;

            double diffX = Math.cos(angle)*distanceFromCenter;
            double diffY = Math.sin(angle)*distanceFromCenter;

            double x = distanceFromCenter+diffX+radius;
            double y = distanceFromCenter+diffY+radius;

            canvas.drawCircle((float) x, (float) y, radius, paint);


            String secs = mTime.second<10?("0"+mTime.second):(mTime.second+"");

            Rect r = new Rect();
            textPaint.setTextAlign(Paint.Align.LEFT);
            textPaint.setTextSize(Math.round(radius*1.7));
            textPaint.getTextBounds(secs, 0, secs.length(), r);

            textPaint.setShadowLayer(0, 0, 0, 0x00000000);
            textPaint.setColor(0xffffffff);
            double textX = x-r.width() / 2 - r.left;
            double textY = y+r.height()/2+r.bottom;
            canvas.drawText(secs,(float)textX,(float)textY,textPaint);

        }

    }

    public static void drawNoCircle(int color, float chunk, Paint textPaint, boolean isRound){
        if (!isInAmbientMode) {

            chunk = p20(chunk);

            paint.setStrokeCap(Paint.Cap.BUTT);
            paint.setAntiAlias(true);
            paint.setStyle(Paint.Style.FILL);
            paint.setFilterBitmap(false);
            //paint.setShadowLayer(4, 0, 0, 0xff000000);
            paint.setColor(color);

            float radius = isRound?chunk*4:chunk*2;

            float distanceFromCenter =  width/2 - radius;


            float millis = System.currentTimeMillis()%60000;
            double angle = (2*Math.PI*millis/60000)-Math.PI/2 - 0.03;

            double diffX = Math.cos(angle)*distanceFromCenter;
            double diffY = Math.sin(angle)*distanceFromCenter;

            double x = distanceFromCenter+diffX+radius;
            double y = distanceFromCenter+diffY+radius;

            float startAngle = (360*millis/60000)-90;
            RectF r1 = new RectF();
            r1.set(0, 0, width, width);
            paint.setStrokeWidth(radius*4);
            paint.setAntiAlias(true);
            paint.setStrokeCap(Paint.Cap.BUTT);
            paint.setStyle(Paint.Style.STROKE);
            paint.setFilterBitmap(false);
            canvas.drawArc(r1, startAngle - 12, 21, false, paint);
            //canvas.drawCircle((float) x, (float) y, radius, paint);

            paint.setColor(0xff000000);
            String secs = mTime.second<10?("0"+mTime.second):(mTime.second+"");

            Rect r = new Rect();
            textPaint.setTextAlign(Paint.Align.LEFT);
            textPaint.setTextSize(Math.round(radius*1.7));
            textPaint.getTextBounds(secs, 0, secs.length(), r);

            textPaint.setShadowLayer(0, 0, 0, 0x00000000);
            textPaint.setColor(0xff000000);
            double textX = x-r.width() / 2 - r.left;
            double textY = y+r.height()/2+r.bottom;
            canvas.drawText(secs,(float)textX,(float)textY,textPaint);

        }

    }



    public static void drawSpin(int color, int speed, float widthStroke, float size, boolean clockwise, boolean alpha){

        if (!isInAmbientMode) {
            if (!alpha){
                float[] hsv = new float[3];
                Color.colorToHSV(color, hsv);
                hsv[2] *= 0.274f; // value component
                color= Color.HSVToColor(hsv);
            }else{
                color = Color.argb(70, Color.red(color), Color.green(color), Color.blue(color));
            }

            RectF r1 = new RectF();

            paint.setStrokeWidth(p20(widthStroke * 2));
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

    public static void drawSpin2(int color, int speed, float widthStroke, float size, boolean clockwise, boolean alpha){

        if (!isInAmbientMode) {
            if (!alpha){
                float[] hsv = new float[3];
                Color.colorToHSV(color, hsv);
                hsv[2] *= 0.274f; // value component
                color= Color.HSVToColor(hsv);
            }else{
                color = Color.argb(70, Color.red(color), Color.green(color), Color.blue(color));
            }

            RectF r1 = new RectF();

            paint.setStrokeWidth(p20(widthStroke * 2));
            paint.setAntiAlias(true);
            paint.setStrokeCap(Paint.Cap.BUTT);
            paint.setStyle(Paint.Style.STROKE);
            paint.setFilterBitmap(false);

            r1.set(0, 0, width, width);

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

    public static String get(String key, Context context){
        SharedPreferences preferences = context.getSharedPreferences("spin", context.MODE_PRIVATE);
        return preferences.getString(key, "");
    }

    public static void set(String key, String value, Context context){
        SharedPreferences preferences = context.getSharedPreferences("spin", context.MODE_PRIVATE);
        SharedPreferences.Editor edit = preferences.edit();
        edit.putString(key, value);
        edit.commit();
    }

    public static void set(String key, int value, Context context){
        SharedPreferences preferences = context.getSharedPreferences("spin", context.MODE_PRIVATE);
        SharedPreferences.Editor edit = preferences.edit();
        edit.putInt(key, value);
        edit.commit();
    }

    public static int getI(String key, Context context){
        SharedPreferences preferences = context.getSharedPreferences("spin", context.MODE_PRIVATE);
        return preferences.getInt(key, 0);
    }

    public static void set(String key, long value, Context context){
        SharedPreferences preferences = context.getSharedPreferences("spin", context.MODE_PRIVATE);
        SharedPreferences.Editor edit = preferences.edit();
        edit.putLong(key, value);
        edit.commit();
    }

    public static long getL(String key, Context context){
        SharedPreferences preferences = context.getSharedPreferences("spin", context.MODE_PRIVATE);
        return preferences.getLong(key, 0);
    }



}
