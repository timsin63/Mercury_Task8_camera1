package com.example.user.task_8_camera1;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Drawable;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import java.io.File;

public class PreviewActivity extends AppCompatActivity {

    public static final String TAG = "PREVIEW_ACTIVITY";
    public static final String IMAGE_TAG = "IMAGE_TAG";
    public static final int REQUEST_CODE_WRITE = 133;

    Bitmap bitmap;
    File file;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.a_preview);

        Log.d(TAG, "created");

        ImageView preview = (ImageView) findViewById(R.id.image_preview);


        bitmap = BitmapFactory.decodeFile(MainActivity.TEMPORARY_FILE_NAME);

        preview.setImageBitmap(bitmap);

        file = new File(MainActivity.TEMPORARY_FILE_NAME);

        Button buttonCancel = (Button) findViewById(R.id.button_cancel);

        buttonCancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                PreviewActivity.super.onBackPressed();
            }
        });

    }

    public void saveToGallery(View view){

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED){
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, REQUEST_CODE_WRITE);
        } else {
            MediaStore.Images.Media.insertImage(getContentResolver(), bitmap, "title", "description");
            Toast.makeText(getApplicationContext(), R.string.save_completed, Toast.LENGTH_SHORT).show();
            super.onBackPressed();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == REQUEST_CODE_WRITE){
            boolean writeAccepted = grantResults[0] == PackageManager.PERMISSION_GRANTED;
            saveToGallery(findViewById(R.id.button_save));
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        file.delete();
    }
}
