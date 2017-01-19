package com.example.user.task_8_camera1;

import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.PorterDuff;
import android.graphics.Rect;
import android.graphics.RectF;
import android.hardware.Camera;
import android.hardware.SensorManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.MotionEvent;
import android.view.OrientationEventListener;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.RotateAnimation;
import android.widget.ImageButton;
import android.widget.ProgressBar;

import org.apache.commons.io.IOUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity implements SurfaceHolder.Callback, Camera.PreviewCallback {

    public static final String TAG = "ACTIVITY_MAIN";
    public static final String PREF_CURRENT_CAMERA_ID = "CURRENT_CAMERA_ID";
    public static final String PREF_FLASH_ACTIVATED = "FLASH_ACTIVATED";
    private static final int REQUEST_CODE_CAMERA = 350;

    static final int REQUEST_CODE_STORAGE = 1123;

    String[] PERMISSIONS = {"android.permission.READ_EXTERNAL_STORAGE", "android.permission.WRITE_EXTERNAL_STORAGE"};

    public static final String TEMPORARY_FILE_NAME = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES) + "/file.jpg";

    private Camera camera;
    SurfaceView surfaceView, transparentView;
    SurfaceHolder surfaceHolder, transparentViewHolder;
    int cameraId;
    SharedPreferences.Editor editor;
    ImageButton flashButton;
    boolean isFlashActivated;
    Camera.Parameters parameters;
    ProgressBar progressBar;

    int mainRotation = 0;

    OrientationEventListener orientationEventListener;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.a_main);

        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);

        cameraId = getPreferences(MODE_PRIVATE).getInt(PREF_CURRENT_CAMERA_ID, Camera.CameraInfo.CAMERA_FACING_BACK);
        isFlashActivated = getPreferences(MODE_PRIVATE).getBoolean(PREF_FLASH_ACTIVATED, false);

        progressBar = (ProgressBar) findViewById(R.id.progressBar);
        surfaceView = (SurfaceView) findViewById(R.id.surface_view);
        transparentView = (SurfaceView) findViewById(R.id.transparent_view);

        surfaceHolder = surfaceView.getHolder();
        surfaceHolder.addCallback(this);
        surfaceHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);

        transparentViewHolder = transparentView.getHolder();
        transparentViewHolder.setFormat(PixelFormat.TRANSLUCENT);
        transparentView.setZOrderMediaOverlay(true);

        editor = getPreferences(MODE_PRIVATE).edit();


        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                requestPermissions(PERMISSIONS, REQUEST_CODE_STORAGE);
            }
        }


        //Capturing
        final ImageButton buttonCapture = (ImageButton) findViewById(R.id.capture_btn);
        buttonCapture.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                progressBar.setVisibility(View.VISIBLE);

                camera.takePicture(null, null, null, new Camera.PictureCallback() {
                    @Override
                    public void onPictureTaken(byte[] bytes, Camera camera) {

                        Log.d(TAG, "onPictureTaken");

                        Intent intent = new Intent(MainActivity.this, PreviewActivity.class);

                        File file = new File(TEMPORARY_FILE_NAME);


                        file.delete();

                        if (!file.exists()) {
                            FileOutputStream out;
                            FileInputStream fis = null;
                            try {
                                out = new FileOutputStream(file);
                                if (fis != null) {
                                    IOUtils.copy(fis, out);
                                    fis.close();
                                }
                                if (out != null) {

                                    Bitmap bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
                                    Matrix matrix = new Matrix();

                                    int photoRotation = 0;

                                    if (mainRotation == 0 || mainRotation == 360) {
                                        photoRotation = 90;
                                    } else if (mainRotation == 180) {
                                        photoRotation = 270;
                                    } else if (mainRotation == 90) {
                                        photoRotation = 180;
                                    }

                                    if (cameraId == Camera.CameraInfo.CAMERA_FACING_FRONT) {
                                        photoRotation = 360 - photoRotation;
                                    }

                                    matrix.postRotate(photoRotation);

                                    bitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
                                    bitmap.compress(Bitmap.CompressFormat.JPEG, 100, out);
                                    out.flush();
                                    out.close();
                                }
                            } catch (Exception e) {
                                Log.e(TAG, e.getMessage());
                            }
                        }

                        startActivity(intent);

                    }
                });
            }
        });


        //Swiping
        final ImageButton buttonSwipe = (ImageButton) findViewById(R.id.swipe_cam);
        if (Camera.getNumberOfCameras() == 1) {
            buttonSwipe.setEnabled(false);
            buttonSwipe.setVisibility(View.INVISIBLE);
        } else {
            buttonSwipe.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    cameraId = cameraId == Camera.CameraInfo.CAMERA_FACING_FRONT ? Camera.CameraInfo.CAMERA_FACING_BACK : Camera.CameraInfo.CAMERA_FACING_FRONT;

                    editor.putInt(PREF_CURRENT_CAMERA_ID, cameraId);
                    editor.commit();
                    Intent intent = new Intent(MainActivity.this, MainActivity.class);
                    startActivity(intent);
                    finish();
                }
            });
        }


        //Flash
        flashButton = (ImageButton) findViewById(R.id.flash_button);

        if (!getApplicationContext().getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA_FLASH)
                || cameraId == Camera.CameraInfo.CAMERA_FACING_FRONT) {
            flashButton.setEnabled(false);
            flashButton.setVisibility(View.INVISIBLE);
            isFlashActivated = false;
        } else {
            if (isFlashActivated) {
                flashButton.setBackgroundResource(R.drawable.no_flash);
            } else {
                flashButton.setBackgroundResource(R.drawable.flash);
            }
            flashButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if (isFlashActivated) {
                        isFlashActivated = false;
                        flashButton.setBackgroundResource(R.drawable.flash);
                        parameters.setFlashMode(Camera.Parameters.FLASH_MODE_OFF);
                        camera.setParameters(parameters);
                    } else {
                        isFlashActivated = true;
                        flashButton.setBackgroundResource(R.drawable.no_flash);
                        parameters.setFlashMode(Camera.Parameters.FLASH_MODE_ON);
                        camera.setParameters(parameters);
                    }
                    editor.putBoolean(PREF_FLASH_ACTIVATED, isFlashActivated);
                    editor.commit();

                }
            });
        }


        //Focusing
        surfaceView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent event) {

                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    float x = event.getX();
                    float y = event.getY();


                    Log.d("TOUCH", String.valueOf(x));
                    Log.d("TOUCH", String.valueOf(y));

                    final Rect touchRect = calculateTapArea(x, y, 1);

                    final Rect targetFocusRect = new Rect(
                            touchRect.left * 2000 / surfaceView.getWidth() - 1000,
                            touchRect.top * 2000 / surfaceView.getHeight() - 1000,
                            touchRect.right * 2000 / surfaceView.getWidth() - 1000,
                            touchRect.bottom * 2000 / surfaceView.getHeight() - 1000);


                    Log.d("Touch rect", String.valueOf(touchRect.left));
                    Log.d("Touch rect", String.valueOf(touchRect.top));
                    Log.d("Touch rect", String.valueOf(touchRect.right));
                    Log.d("Touch rect", String.valueOf(touchRect.bottom));


                    Log.d("Target focus rect", String.valueOf(targetFocusRect.left));
                    Log.d("Target focus rect", String.valueOf(targetFocusRect.top));
                    Log.d("Target focus rect", String.valueOf(targetFocusRect.right));
                    Log.d("Target focus rect", String.valueOf(targetFocusRect.bottom));

                    Paint paint = new Paint();
                    paint.setStyle(Paint.Style.STROKE);
                    paint.setColor(getResources().getColor(android.R.color.white));
                    final Canvas canvas = transparentViewHolder.lockCanvas();

                    canvas.drawColor(getResources().getColor(android.R.color.transparent), PorterDuff.Mode.CLEAR);

                    canvas.drawRect(touchRect, paint);
                    transparentViewHolder.unlockCanvasAndPost(canvas);

                    new Handler().postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            transparentViewHolder.lockCanvas();
                            canvas.drawColor(getResources().getColor(android.R.color.transparent), PorterDuff.Mode.CLEAR);
                            transparentViewHolder.unlockCanvasAndPost(canvas);
                        }
                    }, 1000);


                    doTouchFocus(targetFocusRect);
                }


                return false;
            }
        });


        //Button rotation
        orientationEventListener = new OrientationEventListener(this, SensorManager.SENSOR_DELAY_NORMAL) {
            @Override
            public void onOrientationChanged(int orientation) {

                int previousRotation = mainRotation;

                if (orientation >= 75 && orientation <= 105) {
                    mainRotation = 90;
                } else if (orientation >= 165 && orientation <= 195) {
                    mainRotation = 180;
                } else if (orientation >= 255 && orientation <= 285) {
                    mainRotation = 270;
                } else if (orientation >= 345 && mainRotation != 0) {
                    mainRotation = 360;
                } else if (orientation <= 15 && mainRotation != 360) {
                    mainRotation = 0;
                }


                if (previousRotation != mainRotation) {

                    Log.d("previous", String.valueOf(360 - previousRotation));
                    Log.d("main", String.valueOf(360 - mainRotation));

                    RotateAnimation rotateAnimation;

                    if (mainRotation == 90 && previousRotation == 360) {
                        previousRotation = 0;
                    } else if (mainRotation == 270 && previousRotation == 0) {
                        previousRotation = 360;
                    }

                    if (mainRotation > previousRotation) {
                        rotateAnimation = new RotateAnimation(90, 0, Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f);
                    } else {
                        rotateAnimation = new RotateAnimation(270, 360, Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f);
                    }
                    rotateAnimation.setDuration(400L);

                    if (buttonSwipe.isEnabled()) {
                        buttonSwipe.startAnimation(rotateAnimation);
                        buttonSwipe.setRotation(360 - mainRotation);
                    }
                    buttonCapture.startAnimation(rotateAnimation);
                    buttonCapture.setRotation(360 - mainRotation);
                    if (flashButton.isEnabled()) {
                        flashButton.startAnimation(rotateAnimation);
                        flashButton.setRotation(360 - mainRotation);
                    }
                }
            }

        };
    }


    @Override
    protected void onResume() {
        super.onResume();

        progressBar.setVisibility(View.INVISIBLE);
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, REQUEST_CODE_CAMERA);
        } else {
            camera = Camera.open(cameraId);
            parameters = camera.getParameters();
            if (isFlashActivated) {
                parameters.setFlashMode(Camera.Parameters.FLASH_MODE_ON);
            }
            if (cameraId == Camera.CameraInfo.CAMERA_FACING_BACK) {
                parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE);
            }
            camera.setParameters(parameters);
        }
        orientationEventListener.enable();

    }

    @Override
    protected void onPause() {
        super.onPause();

        if (camera != null) {
            camera.setPreviewCallback(null);
            camera.stopPreview();
            camera.release();
            camera = null;
        }

        progressBar.setVisibility(View.INVISIBLE);
        orientationEventListener.disable();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "destroyed");
    }

    @Override
    public void surfaceCreated(SurfaceHolder surfaceHolder) {

    }

    @Override
    public void surfaceChanged(SurfaceHolder surfaceHolder, int i, int i1, int i2) {

        if (camera != null) {
            try {
                camera.setPreviewDisplay(surfaceHolder);
                camera.setPreviewCallback(this);
            } catch (IOException e) {
                Log.e(TAG, e.getLocalizedMessage());
            }


            Camera.CameraInfo cameraInfo = new Camera.CameraInfo();
            Camera.getCameraInfo(Camera.CameraInfo.CAMERA_FACING_BACK, cameraInfo);

            camera.setDisplayOrientation(getCorrectCameraOrientation(cameraInfo, camera));
            camera.getParameters().setRotation(getCorrectCameraOrientation(cameraInfo, camera));


            Camera.Parameters parameters = camera.getParameters();
            camera.setParameters(parameters);

            camera.startPreview();
        }
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder surfaceHolder) {

    }

    @Override
    public void onPreviewFrame(byte[] bytes, Camera camera) {

    }


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CODE_CAMERA) {
            boolean cameraAccepted = grantResults[0] == PackageManager.PERMISSION_GRANTED;

        }
        if (requestCode == REQUEST_CODE_STORAGE) {
            boolean readAccepted = grantResults[0] == PackageManager.PERMISSION_GRANTED;
            boolean writeAccepted = grantResults[1] == PackageManager.PERMISSION_GRANTED;
        }
        finish();
        startActivity(getIntent());
    }


    public int getCorrectCameraOrientation(Camera.CameraInfo info, Camera camera) {

        int rotation = getWindowManager().getDefaultDisplay().getRotation();
        int degrees = 0;

        switch (rotation) {
            case Surface.ROTATION_0:
                degrees = 0;
                Log.d("surfaceRotation", String.valueOf(degrees));
                break;

            case Surface.ROTATION_90:
                degrees = 90;
                Log.d("surfaceRotation", String.valueOf(degrees));
                break;

            case Surface.ROTATION_180:
                degrees = 180;
                Log.d("surfaceRotation", String.valueOf(degrees));
                break;

            case Surface.ROTATION_270:
                degrees = 270;
                Log.d("surfaceRotation", String.valueOf(degrees));
                break;

        }

        int result;
        if (info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
            result = (info.orientation + degrees) % 360;
            result = (360 - result) % 360;
        } else {
            result = (info.orientation - degrees + 360) % 360;
        }

        return result;
    }


    public void doTouchFocus(final Rect tfocusRect) {
        try {

            camera.cancelAutoFocus();


            List<Camera.Area> focusList = new ArrayList<Camera.Area>();
            Camera.Area focusArea = new Camera.Area(tfocusRect, 1000);
            focusList.add(focusArea);

            Camera.Parameters param = camera.getParameters();
            param.setFocusAreas(focusList);
            param.setMeteringAreas(focusList);
            param.setFocusMode(Camera.Parameters.FOCUS_MODE_MACRO);
            camera.setParameters(param);

            camera.autoFocus(myAutoFocusCallback);
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    private Camera.AutoFocusCallback myAutoFocusCallback = new Camera.AutoFocusCallback() {

        @Override
        public void onAutoFocus(boolean arg0, Camera arg1) {
//            if (arg0) {
//                camera.cancelAutoFocus();
//            }

            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    if (camera != null) {
                        camera.cancelAutoFocus();
                        parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE);
                        camera.setParameters(parameters);
                    }
                }
            }, 4000);
        }
    };


    private Rect calculateTapArea(float x, float y, float coefficient) {
        int areaSize = Float.valueOf(100 * coefficient).intValue();

        int left = clamp((int) x - areaSize / 2, 0, surfaceView.getWidth() - areaSize);
        int top = clamp((int) y - areaSize / 2, 0, surfaceView.getHeight() - areaSize);

        RectF rectF = new RectF(left, top, left + areaSize, top + areaSize);

        Matrix matrix = new Matrix();
        matrix.mapRect(rectF);

        return new Rect(Math.round(rectF.left), Math.round(rectF.top), Math.round(rectF.right), Math.round(rectF.bottom));
    }

    private int clamp(int x, int min, int max) {
        if (x > max) {
            return max;
        }
        if (x < min) {
            return min;
        }
        return x;
    }
}
