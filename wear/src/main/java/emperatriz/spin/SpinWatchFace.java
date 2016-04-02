

package emperatriz.spin;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.PowerManager;
import android.support.wearable.watchface.CanvasWatchFaceService;
import android.support.wearable.watchface.WatchFaceStyle;
import android.text.format.Time;
import android.view.SurfaceHolder;
import android.view.WindowInsets;

import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Digital watch face with seconds. In ambient mode, the seconds aren't displayed. On devices with
 * low-bit ambient mode, the text is drawn without anti-aliasing in ambient mode.
 */
public class SpinWatchFace extends CanvasWatchFaceService  implements SensorEventListener {
    private static final Typeface NORMAL_TYPEFACE =
            Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL);

    /**
     * Update rate in milliseconds for interactive mode. We update once a second since seconds are
     * displayed in interactive mode.
     */
    private static final long INTERACTIVE_UPDATE_RATE_MS = TimeUnit.SECONDS.toMillis(1);

    SensorManager mSensorManager;

    int steps=0;
    int todaySteps=0;
    boolean saveLastSteps=true;


    static int NORMAL=0, NORMAL_OVERLAP=1, CIRCLE=2, CIRCLE_OVERLAP=3, EXCENTRIC=4, EXCENTRIC_CIRCLE=5,EXCENTRIC_OPEN=6, EXCENTRIC_OPEN_CIRCLE=7;
    static int WEEKDAY=0, STEPS=1;
    int outmode = NORMAL;
    int inmode = WEEKDAY;

    @Override
    public Engine onCreateEngine() {

        return new Engine();
    }

    PowerManager.WakeLock wakeLock;


    private final ScheduledExecutorService worker =
            Executors.newSingleThreadScheduledExecutor();

    void releaseWakelockAfter(int time) {

        Runnable task = new Runnable() {
            public void run() {
                wakeLock.release();
            }
        };
        worker.schedule(task, time, TimeUnit.SECONDS);

    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_STEP_COUNTER) {
            steps =  (int)event.values[0];
            if (saveLastSteps){
                saveLastSteps=false;
                SimpleDateFormat sdf = new SimpleDateFormat("ddMMyyyy");
                DrawUtils.set("lastSteps",sdf.format(new Date()),SpinWatchFace.this);
                todaySteps=steps-1;
                DrawUtils.set("steps",todaySteps, SpinWatchFace.this);
            }
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {

    }

    private class Engine extends CanvasWatchFaceService.Engine {
        static final int MSG_UPDATE_TIME = 0;

        /**
         * Handler to update the time periodically in interactive mode.
         */
        final Handler mUpdateTimeHandler = new Handler() {
            @Override
            public void handleMessage(Message message) {
                switch (message.what) {
                    case MSG_UPDATE_TIME:
                        invalidateAfter();
                        if (shouldTimerBeRunning()) {
                            long timeMs = System.currentTimeMillis();
                            long delayMs = INTERACTIVE_UPDATE_RATE_MS
                                    - (timeMs % INTERACTIVE_UPDATE_RATE_MS);
                            mUpdateTimeHandler.sendEmptyMessageDelayed(MSG_UPDATE_TIME, delayMs);
                        }
                        break;
                }
            }
        };

        final BroadcastReceiver mTimeZoneReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                DrawUtils.mTime.clear(intent.getStringExtra("time-zone"));
                DrawUtils.mTime.setToNow();
            }
        };

        boolean mRegisteredTimeZoneReceiver = false;

        Paint mBackgroundPaint;
        Paint paintText, paintTime;

        boolean mAmbient;


        float mXOffset;
        float mYOffset;

        /**
         * Whether the display supports fewer bits for each color in ambient mode. When true, we
         * disable anti-aliasing in ambient mode.
         */
        boolean mLowBitAmbient;





        @Override
        public void onCreate(SurfaceHolder holder) {
            super.onCreate(holder);

            setWatchFaceStyle(new WatchFaceStyle.Builder(SpinWatchFace.this)
                    .setCardPeekMode(WatchFaceStyle.PEEK_MODE_SHORT)
                    .setBackgroundVisibility(WatchFaceStyle.BACKGROUND_VISIBILITY_INTERRUPTIVE)
                    .setShowSystemUiTime(false)
                    .setAcceptsTapEvents(true)
                    .build());
            Resources resources = SpinWatchFace.this.getResources();
            mYOffset = resources.getDimension(R.dimen.digital_y_offset);

            mBackgroundPaint = new Paint();
            mBackgroundPaint.setColor(resources.getColor(R.color.digital_background));

            paintText = new Paint();
            paintTime = new Paint();

            Typeface font1 = Typeface.createFromAsset(getApplicationContext().getAssets(), "fonts/SF Movie Poster Bold.ttf");
            Typeface font2 = Typeface.createFromAsset(getApplicationContext().getAssets(), "fonts/SF Movie Poster Condensed.ttf");
            Typeface font3 = Typeface.createFromAsset(getApplicationContext().getAssets(), "fonts/SF Movie Poster.ttf");
            paintText.setTypeface(font2);
            paintTime.setTypeface(font3);
            paintText.setAntiAlias(true);
            paintTime.setAntiAlias(true);
//            paintRegular.setFakeBoldText(true);
            paintText.setLetterSpacing(0.03f);


            DrawUtils.mTime = new Time();

            mSensorManager = ((SensorManager)getSystemService(SENSOR_SERVICE));
            Sensor mStepCountSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER);
            mSensorManager.registerListener(SpinWatchFace.this, mStepCountSensor, SensorManager.SENSOR_DELAY_NORMAL);
        }

        boolean normal=true;



        @Override
        public void onTapCommand(int tapType, int x, int y, long eventTime) {

            if (tapType==TAP_TYPE_TOUCH){
                if (Calendar.getInstance().getTimeInMillis()-DrawUtils.getL("lastTap", getApplicationContext())<300){
                    int centerX = DrawUtils.width/2;
                    int centerY = DrawUtils.width/2;
                    double tapMeasure = Math.sqrt((x-centerX)*(x-centerX)+(y-centerY)*(y-centerY));
                    float radius = DrawUtils.width/2-DrawUtils.p20(isRound?chunk:chunkSq)*4;

                    if (tapMeasure<radius){
                        inmode++;
                        if (inmode>=2) inmode=0;
                    }
                    else{
                        outmode++;
                        if (outmode>=8) outmode=0;
                    }
                }
                DrawUtils.set("lastTap",Calendar.getInstance().getTimeInMillis(),getApplicationContext());

            }


        }

        @Override
        public void onDestroy() {
            mUpdateTimeHandler.removeMessages(MSG_UPDATE_TIME);
            mSensorManager.unregisterListener(SpinWatchFace.this);
            super.onDestroy();
        }

        private Paint createTextPaint(int textColor) {
            Paint paint = new Paint();
            paint.setColor(textColor);
            paint.setTypeface(NORMAL_TYPEFACE);
            paint.setAntiAlias(true);
            return paint;
        }



        @Override
        public void onVisibilityChanged(boolean visible) {
            super.onVisibilityChanged(visible);

            SimpleDateFormat sdf = new SimpleDateFormat("ddMMyyyy");

            String lastSteps = DrawUtils.get("lastSteps",SpinWatchFace.this);
            if (!lastSteps.equals(sdf.format(new Date()))){
                saveLastSteps=true;
            }

            if (visible) {
                registerReceiver();

                // Update time zone in case it changed while we weren't visible.
                DrawUtils.mTime.clear(TimeZone.getDefault().getID());
                DrawUtils.mTime.setToNow();
            } else {
                unregisterReceiver();
            }

            // Whether the timer should be running depends on whether we're visible (as well as
            // whether we're in ambient mode), so we may need to start or stop the timer.
            updateTimer();
        }

        private void registerReceiver() {
            if (mRegisteredTimeZoneReceiver) {
                return;
            }
            mRegisteredTimeZoneReceiver = true;
            IntentFilter filter = new IntentFilter(Intent.ACTION_TIMEZONE_CHANGED);
            SpinWatchFace.this.registerReceiver(mTimeZoneReceiver, filter);
        }

        private void unregisterReceiver() {
            if (!mRegisteredTimeZoneReceiver) {
                return;
            }
            mRegisteredTimeZoneReceiver = false;
            SpinWatchFace.this.unregisterReceiver(mTimeZoneReceiver);
        }
        boolean isRound;
        @Override
        public void onApplyWindowInsets(WindowInsets insets) {
            super.onApplyWindowInsets(insets);

            // Load resources that have alternate values for round watches.
            Resources resources = SpinWatchFace.this.getResources();
            isRound = insets.isRound();
            mXOffset = resources.getDimension(isRound
                    ? R.dimen.digital_x_offset_round : R.dimen.digital_x_offset);
            float textSize = resources.getDimension(isRound
                    ? R.dimen.digital_text_size_round : R.dimen.digital_text_size);

            paintText.setTextSize(textSize);
        }

        @Override
        public void onPropertiesChanged(Bundle properties) {
            super.onPropertiesChanged(properties);
            mLowBitAmbient = properties.getBoolean(PROPERTY_LOW_BIT_AMBIENT, false);
        }

        @Override
        public void onTimeTick() {
            super.onTimeTick();
            invalidateAfter();
        }

        @Override
        public void onAmbientModeChanged(boolean inAmbientMode) {
            super.onAmbientModeChanged(inAmbientMode);

            if (!inAmbientMode){
                PowerManager powerManager = (PowerManager) getSystemService(POWER_SERVICE);

                wakeLock = powerManager.newWakeLock(PowerManager.SCREEN_BRIGHT_WAKE_LOCK,"WatchFaceWakelockTag"); // note WakeLock spelling

                wakeLock.acquire();

                releaseWakelockAfter(15);

//                speed1 = Math.round(DrawUtils.random(3,13));
//                speed2 = Math.round(DrawUtils.random(3,13));
//                speed3 = Math.round(DrawUtils.random(3,13));
//                speed4 = Math.round(DrawUtils.random(3,13));
                size1 = DrawUtils.random(0.55f, 0.80f);
                size2 = DrawUtils.random(0.55f, 0.80f);
                size3 = DrawUtils.random(0.35f, 0.70f);
                size4 = DrawUtils.random(0.35f, 0.70f);
                clockwise1 = DrawUtils.random(0,1)>0.50;
                clockwise3 = DrawUtils.random(0,1)>0.50;
                clockwise4 = DrawUtils.random(0,1)>0.50;
                clockwise2=!clockwise1;

            }
            invalidateAfter();
        }



        int speed1 = 3;
        int speed2 = 5;
        int speed3 = 7;
        int speed4 = 13;
        float size1 = DrawUtils.random(0.55f, 0.80f);
        float size2 = DrawUtils.random(0.55f, 0.80f);
        float size3 = DrawUtils.random(0.35f, 0.70f);
        float size4 = DrawUtils.random(0.35f, 0.70f);
        boolean clockwise1 = DrawUtils.random(0,1)>0.50;
        boolean clockwise2 = !clockwise1;
        boolean clockwise3 = DrawUtils.random(0,1)>0.50;
        boolean clockwise4 = DrawUtils.random(0,1)>0.50;

        float chunk = DrawUtils.p20(0.025f);
        float chunkSq = DrawUtils.p20(0.05f);

        @Override
        public void onDraw(Canvas canvas, Rect bounds) {

            DrawUtils.mTime.setToNow();


            DrawUtils.now = System.currentTimeMillis();
            DrawUtils.height = bounds.height();
            DrawUtils.width = bounds.width();
            DrawUtils.canvas = canvas;
            DrawUtils.isInAmbientMode = isInAmbientMode();
            DrawUtils.ctx = getApplicationContext();
            DrawUtils.p20 = bounds.width()/20;


            SharedPreferences preferences = getSharedPreferences("spin", MODE_PRIVATE);

            int color = preferences.getInt(DrawUtils.ACCENT_COLOR, 0xff00ffdd);

            DrawUtils.drawBackground(0xff000000, new Paint());


            chunk = DrawUtils.p20(0.025f);
            chunkSq = DrawUtils.p20(0.05f);



            if (outmode==NORMAL){
                if (!isRound){
                    DrawUtils.drawSpin2(color, speed1, chunkSq * 4, size1, clockwise1, false);
                    DrawUtils.drawSpin2(color, speed2, chunkSq*3, size2, clockwise2,true);
                    DrawUtils.drawSpin2(color, speed3, chunkSq*2, size3, clockwise3,true);
                    DrawUtils.drawSpin2(color, speed4, chunkSq, 1, clockwise4,true);
                    DrawUtils.drawSeconds2(color,chunkSq);
                }else{
                    DrawUtils.drawSpin(color, speed1, chunk*4, size1, clockwise1,false);
                    DrawUtils.drawSpin(color, speed2, chunk*3, size2, clockwise2,true);
                    DrawUtils.drawSpin(color, speed3, chunk*2, size3, clockwise3,true);
                    DrawUtils.drawSpin(color, speed4, chunk, 1, clockwise4,true);
                    DrawUtils.drawSeconds(color, chunk);
                }
            } else if (outmode==NORMAL_OVERLAP){
                if (!isRound){
                    DrawUtils.drawSpin2(color, speed1, chunkSq * 4, size1, clockwise1, false);
                    DrawUtils.drawSpin2(color, speed2, chunkSq*4, size2, clockwise2,true);
                    DrawUtils.drawSpin2(color, speed3, chunkSq*4, size3, clockwise3,true);
                    DrawUtils.drawSpin2(color, speed4, chunkSq*4, size4, clockwise4,true);
                    DrawUtils.drawSpin2(color, speed4, chunkSq*4, 1, clockwise4,true);
                    DrawUtils.drawSeconds2(color,chunkSq);
                }else{
                    DrawUtils.drawSpin(color, speed1, chunk*4, size1, clockwise1,false);
                    DrawUtils.drawSpin(color, speed2, chunk*4, size2, clockwise2,true);
                    DrawUtils.drawSpin(color, speed3, chunk*4, size3, clockwise3,true);
                    DrawUtils.drawSpin(color, speed4, chunk * 4, size4, clockwise4, true);
                    DrawUtils.drawSpin(color, speed4, chunk*4, 1, clockwise4,true);
                    DrawUtils.drawSeconds(color, chunk);
                }
            }else if (outmode==CIRCLE){
                if (!isRound){
                    DrawUtils.drawSpin2(color, speed4, chunkSq * 4, 1, clockwise1, false);
                    DrawUtils.drawSpin2(color, speed1, chunkSq*3, size2, clockwise2, true);
                    DrawUtils.drawSpin2(color, speed2, chunkSq*2, size3, clockwise3, true);
                    DrawUtils.drawSpin2(color, speed3, chunkSq, size1, clockwise1,true);
                    DrawUtils.drawCircle(color, chunkSq, paintText,isRound);
                }else{
                    DrawUtils.drawSpin(color, speed4, chunk*4, 1, clockwise1,false);
                    DrawUtils.drawSpin(color, speed1, chunk*3, size2, clockwise2,true);
                    DrawUtils.drawSpin(color, speed2, chunk*2, size3, clockwise3,true);
                    DrawUtils.drawSpin(color, speed3, chunk, size1, clockwise1,true);
                    DrawUtils.drawCircle(color, chunk, paintText,isRound);
                }
            } else if (outmode==CIRCLE_OVERLAP){
                if (!isRound){
                    DrawUtils.drawSpin2(color, speed1, chunkSq * 4, size1, clockwise1, false);
                    DrawUtils.drawSpin2(color, speed2, chunkSq*4, size2, clockwise2,true);
                    DrawUtils.drawSpin2(color, speed3, chunkSq*4, size3, clockwise3,true);
                    DrawUtils.drawSpin2(color, speed4, chunkSq * 4, size4, clockwise4, true);
                    DrawUtils.drawSpin2(color, speed4, chunkSq*4, 1, clockwise4,true);
                    DrawUtils.drawNoCircle(color, chunkSq, paintText, isRound);
                }else{
                    DrawUtils.drawSpin(color, speed1, chunk*4, size1, clockwise1,false);
                    DrawUtils.drawSpin(color, speed2, chunk*4, size2, clockwise2,true);
                    DrawUtils.drawSpin(color, speed3, chunk*4, size3, clockwise3,true);
                    DrawUtils.drawSpin(color, speed4, chunk * 4, size4, clockwise4, true);
                    DrawUtils.drawSpin(color, speed4, chunk*4, 1, clockwise4,true);
                    DrawUtils.drawNoCircle(color, chunk, paintText, isRound);
                }
            }else if (outmode==EXCENTRIC){
                if (!isRound){
                    DrawUtils.drawExcentric2(color, 2.6f, chunkSq * 4, clockwise1);
                    DrawUtils.drawExcentric2(color, 4.3f, chunkSq * 4, clockwise2);
                    DrawUtils.drawExcentric2(color, 3.1f, chunkSq * 4, clockwise3);
                    DrawUtils.drawSpin2(color, speed4, chunkSq * 4, 1, clockwise4, true);
                    DrawUtils.drawSeconds2(color, chunkSq);
                }else{
                    DrawUtils.drawExcentric(color, 2.6f, chunk * 4, clockwise1);
                    DrawUtils.drawExcentric(color, 4.3f, chunk * 4, clockwise2);
                    DrawUtils.drawExcentric(color, 3.1f, chunk * 4, clockwise3);
                    DrawUtils.drawSpin(color, speed4, chunk * 4, 1, clockwise4, true);
                    DrawUtils.drawSeconds(color, chunk);
                }
            }else if (outmode==EXCENTRIC_CIRCLE){
                if (!isRound){
                    DrawUtils.drawExcentric2(color, 2.6f, chunkSq * 4, clockwise1);
                    DrawUtils.drawExcentric2(color, 4.3f, chunkSq * 4, clockwise2);
                    DrawUtils.drawExcentric2(color, 3.1f, chunkSq * 4, clockwise3);
                    DrawUtils.drawSpin2(color, speed4, chunkSq * 4, 1, clockwise4, true);
                    DrawUtils.drawCircle(color, chunkSq, paintText, isRound);
                }else{
                    DrawUtils.drawExcentric(color, 2.6f, chunk * 4, clockwise1);
                    DrawUtils.drawExcentric(color, 4.3f, chunk * 4, clockwise2);
                    DrawUtils.drawExcentric(color, 3.1f, chunk * 4, clockwise3);
                    DrawUtils.drawSpin(color, speed4, chunk * 4, 1, clockwise4, true);
                    DrawUtils.drawCircle(color, chunk, paintText, isRound);
                }
            }else if (outmode==EXCENTRIC_OPEN){
                if (!isRound){
                    DrawUtils.drawExcentric2Open(color, speed1 + 1, chunkSq * 4, clockwise1);
                    DrawUtils.drawExcentric2Open(color, speed2, chunkSq * 4, clockwise2);
                    DrawUtils.drawExcentric2Open(color, speed1, chunkSq * 4, clockwise3);
                    DrawUtils.drawExcentric2Open(color, speed2 + 2, chunkSq * 4, clockwise4);
                    DrawUtils.drawSpin2(color, speed4, chunkSq * 4, 1, clockwise4, true);
                    DrawUtils.drawSeconds2(color, chunkSq);
                }else{
                    DrawUtils.drawExcentricOpen(color, speed1+1, chunk * 4, clockwise1);
                    DrawUtils.drawExcentricOpen(color, speed2, chunk * 4, clockwise2);
                    DrawUtils.drawExcentricOpen(color, speed1, chunk * 4, clockwise3);
                    DrawUtils.drawExcentricOpen(color, speed2+2, chunk * 4, clockwise4);
                    DrawUtils.drawSpin(color, speed4, chunk * 4, 1, clockwise4, true);
                    DrawUtils.drawSeconds(color, chunk);
                }
            }else if (outmode==EXCENTRIC_OPEN_CIRCLE){
                if (!isRound){
                    DrawUtils.drawExcentric2Open(color, speed1 + 1, chunkSq * 4, clockwise1);
                    DrawUtils.drawExcentric2Open(color, speed2, chunkSq * 4, clockwise2);
                    DrawUtils.drawExcentric2Open(color, speed1, chunkSq * 4, clockwise3);
                    DrawUtils.drawExcentric2Open(color, speed2 + 2, chunkSq * 4, clockwise4);
                    DrawUtils.drawSpin2(color, speed4, chunkSq * 4, 1, clockwise4, true);
                    DrawUtils.drawCircle(color, chunkSq, paintText, isRound);
                }else{
                    DrawUtils.drawExcentricOpen(color, speed1+1, chunk * 4, clockwise1);
                    DrawUtils.drawExcentricOpen(color, speed2, chunk * 4, clockwise2);
                    DrawUtils.drawExcentricOpen(color, speed1, chunk * 4, clockwise3);
                    DrawUtils.drawExcentricOpen(color, speed2+2, chunk * 4, clockwise4);
                    DrawUtils.drawSpin(color, speed4, chunk * 4, 1, clockwise4, true);
                    DrawUtils.drawCircle(color, chunk, paintText, isRound);
                }
            }




            DecimalFormat df = new DecimalFormat("00");
            float dateHeight = DrawUtils.drawCenteredText(df.format(DrawUtils.mTime.hour)+":"+ df.format(DrawUtils.mTime.minute), paintTime);


            todaySteps = DrawUtils.getI("steps", SpinWatchFace.this);
            DrawUtils.drawDate(0xffbbbbbb, paintText, inmode==WEEKDAY,saveLastSteps?0:steps-todaySteps, dateHeight);


            if (isVisible() && !isInAmbientMode()) {
                invalidateAfter();
            }

        }

        private final ScheduledExecutorService worker =
                Executors.newSingleThreadScheduledExecutor();

        void invalidateAfter() {
            Runnable task = new Runnable() {
                public void run() {
                    invalidate();
                }
            };
            worker.schedule(task, 12, TimeUnit.MILLISECONDS);

        }

        /**
         * Starts the {@link #mUpdateTimeHandler} timer if it should be running and isn't currently
         * or stops it if it shouldn't be running but currently is.
         */
        private void updateTimer() {
            mUpdateTimeHandler.removeMessages(MSG_UPDATE_TIME);
            if (shouldTimerBeRunning()) {
                mUpdateTimeHandler.sendEmptyMessage(MSG_UPDATE_TIME);
            }
        }

        /**
         * Returns whether the {@link #mUpdateTimeHandler} timer should be running. The timer should
         * only run when we're visible and in interactive mode.
         */
        private boolean shouldTimerBeRunning() {
            return isVisible() && !isInAmbientMode();
        }
    }
}
