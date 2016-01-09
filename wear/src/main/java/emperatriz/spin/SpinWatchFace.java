

package emperatriz.spin;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Typeface;
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
import java.util.TimeZone;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Digital watch face with seconds. In ambient mode, the seconds aren't displayed. On devices with
 * low-bit ambient mode, the text is drawn without anti-aliasing in ambient mode.
 */
public class SpinWatchFace extends CanvasWatchFaceService {
    private static final Typeface NORMAL_TYPEFACE =
            Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL);

    /**
     * Update rate in milliseconds for interactive mode. We update once a second since seconds are
     * displayed in interactive mode.
     */
    private static final long INTERACTIVE_UPDATE_RATE_MS = TimeUnit.SECONDS.toMillis(1);

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
                        invalidate();
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
        Paint mTextPaint,mTextPaint2;

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
                    .build());
            Resources resources = SpinWatchFace.this.getResources();
            mYOffset = resources.getDimension(R.dimen.digital_y_offset);

            mBackgroundPaint = new Paint();
            mBackgroundPaint.setColor(resources.getColor(R.color.digital_background));

            mTextPaint = new Paint();
            mTextPaint2= new Paint();
            Typeface font1 = Typeface.createFromAsset(getApplicationContext().getAssets(), "fonts/radiospace.ttf");
            Typeface font2 = Typeface.createFromAsset(getApplicationContext().getAssets(), "fonts/electrotome.ttf");
            mTextPaint.setTypeface(font1);
            mTextPaint2.setTypeface(font2);

            DrawUtils.mTime = new Time();
        }

        @Override
        public void onDestroy() {
            mUpdateTimeHandler.removeMessages(MSG_UPDATE_TIME);
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

        @Override
        public void onApplyWindowInsets(WindowInsets insets) {
            super.onApplyWindowInsets(insets);

            // Load resources that have alternate values for round watches.
            Resources resources = SpinWatchFace.this.getResources();
            boolean isRound = insets.isRound();
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
            invalidate();
        }

        @Override
        public void onAmbientModeChanged(boolean inAmbientMode) {
            super.onAmbientModeChanged(inAmbientMode);

            if (!inAmbientMode){
                PowerManager powerManager = (PowerManager) getSystemService(POWER_SERVICE);

                wakeLock = powerManager.newWakeLock(PowerManager.SCREEN_BRIGHT_WAKE_LOCK,"WatchFaceWakelockTag"); // note WakeLock spelling

                wakeLock.acquire();

                releaseWakelockAfter(15);

                int speed11 = Math.round(DrawUtils.random(5,1500));
                int speed22 = Math.round(DrawUtils.random(5,1500));
                int speed33 = Math.round(DrawUtils.random(5,1500));
                size1 = DrawUtils.random(0.4f, 0.65f);
                size2 = DrawUtils.random(0.4f, 0.75f);
                size3 = DrawUtils.random(0.4f, 0.85f);
                clockwise1 = DrawUtils.random(0,1)>0.50;
                clockwise2 = DrawUtils.random(0,1)>0.50;
                clockwise3 = DrawUtils.random(0,1)>0.50;
                if (clockwise1==clockwise2){
                    clockwise3 = !clockwise1;
                }
                int max = Math.max(speed11, Math.max(speed22,speed33));
                int min = Math.min(speed11, Math.min(speed22, speed33));

                speed1 = 12;
                speed2 =12;
                speed3 =12;
                if (speed11==max) {
                    speed1=3;
                } else if (speed22==max) {
                    speed2=3;
                } else {
                    speed3=3;
                }
                if (speed11==min) {
                    speed1=6;
                } else if (speed22==min) {
                    speed2=6;
                } else {
                    speed3=6;
                }
            }
            invalidate();
        }



        int speed1 = 6;
        int speed2 = 12;
        int speed3 = 3;
        float size1 = DrawUtils.random(0.4f, 0.85f);
        float size2 = DrawUtils.random(0.4f, 0.85f);
        float size3 = DrawUtils.random(0.4f, 0.85f);
        boolean clockwise1 = DrawUtils.random(0,1)>0.50;
        boolean clockwise2 = DrawUtils.random(0,1)>0.50;
        boolean clockwise3 = DrawUtils.random(0,1)>0.50;

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

            DrawUtils.drawSpin(color, speed1, 1.65f, size1, clockwise1);
            DrawUtils.drawSpin(color, speed2, 1.10f, size2, clockwise2);
            DrawUtils.drawSpin(color, speed3, 0.55f, size3, clockwise3);

            DrawUtils.drawSeconds(color);
            DecimalFormat df = new DecimalFormat("00");
            DrawUtils.drawCenteredText(df.format(DrawUtils.mTime.hour)+":"+df.format(DrawUtils.mTime.minute),mTextPaint);

            DrawUtils.drawDate(0xffffffff, mTextPaint2);


            if (isVisible() && !isInAmbientMode()) {
                invalidate();
            }

        }

        private final ScheduledExecutorService worker =
                Executors.newSingleThreadScheduledExecutor();

        void invalidateAfter(int time) {

            Runnable task = new Runnable() {
                public void run() {
                    invalidate();
                }
            };
            worker.schedule(task, time, TimeUnit.MILLISECONDS);

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
