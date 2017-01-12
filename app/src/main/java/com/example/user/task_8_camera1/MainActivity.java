package com.example.user.task_8_camera1;

import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Rect;
import android.hardware.Camera;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageButton;

import org.apache.commons.io.IOUtils;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.security.Policy;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity implements SurfaceHolder.Callback, Camera.PreviewCallback {

    public static final String TAG = "ACTIVITY_MAIN";
    public static final String PREF_CURRENT_CAMERA_ID = "CURRENT_CAMERA_ID";
    public static final String PREF_FLASH_ACTIVATED = "FLASH_ACTIVATED";
    private static final int REQUEST_CODE_CAMERA = 350;

    static final int REQUEST_CODE_STORAGE = 1123;

    String[] PERMISSIONS = {"android.permission.READ_EXTERNAL_STORAGE","android.permission.WRITE_EXTERNAL_STORAGE"};

    public static final String TEMPORARY_FILE_NAME = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES) + "/file.jpg";

    private Camera camera;
    SurfaceView surfaceView;
    SurfaceHolder surfaceHolder;
    int cameraId;
    SharedPreferences.Editor editor;
    ImageButton flashButton;
    boolean isFlashActivated;
    Camera.Parameters parameters;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.a_main);

        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);

        cameraId = getPreferences(MODE_PRIVATE).getInt(PREF_CURRENT_CAMERA_ID, Camera.CameraInfo.CAMERA_FACING_BACK);
        isFlashActivated = getPreferences(MODE_PRIVATE).getBoolean(PREF_FLASH_ACTIVATED, false);

        surfaceView = (SurfaceView) findViewById(R.id.surface_view);

        surfaceHolder = surfaceView.getHolder();
        surfaceHolder.addCallback(this);
        surfaceHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);

        editor = getPreferences(MODE_PRIVATE).edit();



        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                requestPermissions(PERMISSIONS, REQUEST_CODE_STORAGE);
            }
        }


        //Capturing
        ImageButton buttonCapture = (ImageButton) findViewById(R.id.capture_btn);
        buttonCapture.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
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
        ImageButton buttonSwipe = (ImageButton) findViewById(R.id.swipe_cam);
        if (Camera.getNumberOfCameras() == 1){
            buttonSwipe.setEnabled(false);
            buttonSwipe.setVisibility(View.INVISIBLE);
        } else {
            buttonSwipe.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    cameraId = cameraId == Camera.CameraInfo.CAMERA_FACING_FRONT? Camera.CameraInfo.CAMERA_FACING_BACK : Camera.CameraInfo.CAMERA_FACING_FRONT;

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

                    Rect touchRect = new Rect(
                            (int) (x - 100),
                            (int) (y - 100),
                            (int) (x + 100),
                            (int) (y + 100));


                    final Rect targetFocusRect = new Rect(
                            touchRect.left * 2000 / surfaceView.getWidth() - 1000,
                            touchRect.top * 2000 / surfaceView.getHeight() - 1000,
                            touchRect.right * 2000 / surfaceView.getWidth() - 1000,
                            touchRect.bottom * 2000 / surfaceView.getHeight() - 1000);

                    doTouchFocus(targetFocusRect);
                }

                return false;
            }
        });
    }


    @Override
    protected void onResume() {
        super.onResume();

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED){
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, REQUEST_CODE_CAMERA);
        } else {
            camera = Camera.open(cameraId);
            parameters = camera.getParameters();
            if (isFlashActivated){
                parameters.setFlashMode(Camera.Parameters.FLASH_MODE_ON);
            }
            parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE);
            camera.setParameters(parameters);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();

        if (camera != null)
        {
            camera.setPreviewCallback(null);
            camera.stopPreview();
            camera.release();
            camera = null;
        }
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
            camera.getParameters().setRotation(getCorrectCameraOrientation(cameraInfo, camera) + 90);

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
        if (requestCode == REQUEST_CODE_CAMERA){
            boolean cameraAccepted = grantResults[0] == PackageManager.PERMISSION_GRANTED;

        }
        if (requestCode == REQUEST_CODE_STORAGE){
            boolean readAccepted = grantResults[0]== PackageManager.PERMISSION_GRANTED;
            boolean writeAccepted = grantResults[1]== PackageManager.PERMISSION_GRANTED;
        }
        finish();
        startActivity(getIntent());
    }


    public int getCorrectCameraOrientation(Camera.CameraInfo info, Camera camera) {

        int rotation = getWindowManager().getDefaultDisplay().getRotation();
        int degrees = 0;

        switch(rotation){
            case Surface.ROTATION_0:
                degrees = 0;
                break;

            case Surface.ROTATION_90:
                degrees = 90;
                break;

            case Surface.ROTATION_180:
                degrees = 180;
                break;

            case Surface.ROTATION_270:
                degrees = 270;
                break;

        }

        int result;
        if(info.facing==Camera.CameraInfo.CAMERA_FACING_FRONT){
            result = (info.orientation + degrees) % 360;
            result = (360 - result) % 360;
        }else{
            result = (info.orientation - degrees + 360) % 360;
        }

        return result;
    }


    public void doTouchFocus(final Rect tfocusRect) {
        try {
            List<Camera.Area> focusList = new ArrayList<Camera.Area>();
            Camera.Area focusArea = new Camera.Area(tfocusRect, 1000);
            focusList.add(focusArea);

            Camera.Parameters param = camera.getParameters();
            param.setFocusAreas(focusList);
            param.setMeteringAreas(focusList);
            param.setFocusMode(Camera.Parameters.FOCUS_MODE_AUTO);
            camera.setParameters(param);

            camera.autoFocus(myAutoFocusCallback);
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    private Camera.AutoFocusCallback myAutoFocusCallback = new Camera.AutoFocusCallback() {

        @Override
        public void onAutoFocus(boolean arg0, Camera arg1) {
            if (arg0) {
                camera.cancelAutoFocus();
            }

            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE);
                    camera.setParameters(parameters);
                }
            }, 2500);
        }
    };

}
