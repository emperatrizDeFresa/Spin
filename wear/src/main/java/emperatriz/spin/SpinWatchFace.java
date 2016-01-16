

package emperatriz.spin;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
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

import java.lang.ref.WeakReference;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
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
                todaySteps=steps;
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
        Paint mTextPaint,mTextPaint2,mTextPaint3;

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

            mTextPaint = new Paint();
            mTextPaint2= new Paint();
            mTextPaint3= new Paint();
            Typeface font1 = Typeface.createFromAsset(getApplicationContext().getAssets(), "fonts/SF Movie Poster Bold.ttf");
            Typeface font2 = Typeface.createFromAsset(getApplicationContext().getAssets(), "fonts/SF Movie Poster.ttf");
//            Typeface font3 = Typeface.createFromAsset(getApplicationContext().getAssets(), "fonts/SF Movie Poster Bold.ttf");
            mTextPaint.setTypeface(font1);
            mTextPaint2.setTypeface(font1);
            mTextPaint3.setTypeface(font2);

            DrawUtils.mTime = new Time();

            mSensorManager = ((SensorManager)getSystemService(SENSOR_SERVICE));
            Sensor mStepCountSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER);
            mSensorManager.registerListener(SpinWatchFace.this, mStepCountSensor, SensorManager.SENSOR_DELAY_NORMAL);
        }

        boolean normal=true;

        @Override
        public void onTapCommand(int tapType, int x, int y, long eventTime) {

            if (tapType==TAP_TYPE_TAP){
                normal=!normal;
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

            mTextPaint.setTextSize(textSize);
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


            float chunk = DrawUtils.p20(0.025f);
            float chunkSq = DrawUtils.p20(0.05f);

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


            DecimalFormat df = new DecimalFormat("00");
            float dateHeight = DrawUtils.drawCenteredText(df.format(DrawUtils.mTime.hour)+":"+ df.format(DrawUtils.mTime.minute),"", mTextPaint3, mTextPaint);


            todaySteps = DrawUtils.getI("steps", SpinWatchFace.this);
            DrawUtils.drawDate(0xffbbbbbb, mTextPaint, normal,steps-todaySteps, dateHeight);


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
