package com.example.user.tesseracttess_twoocr.Camera2API;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.RequiresApi;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.WindowManager;
import android.widget.Toast;

import com.example.user.tesseracttess_twoocr.R;
import com.example.user.tesseracttess_twoocr.VariableEditor;

public class Camera2Activity extends AppCompatActivity {

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        int MY_PERMISSIONS_REQUEST_READ_CONTACTS = 1; // Results code
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{
                            Manifest.permission.CAMERA,
                            Manifest.permission.READ_EXTERNAL_STORAGE,
                            Manifest.permission.RECORD_AUDIO
                    },
                    MY_PERMISSIONS_REQUEST_READ_CONTACTS);
        }

        VariableEditor.ORC_type = "eng";
        VariableEditor.Picture_type = "1";

        setContentView(R.layout.activity_camera2api);
        //Hide the status bar
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);

        //Hide the ActionBar
        ActionBar actionBar = getSupportActionBar();
        actionBar.hide();

        if (null == savedInstanceState) {
            getSupportFragmentManager().beginTransaction().replace(R.id.container, Camera2BasicFragment.newInstance()).commit();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        if (requestCode == 1) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {

            } else {
                Toast toast = Toast.makeText(this, "Error", Toast.LENGTH_SHORT);
                toast.show();
            }
        }
    }

//    @Override
//    protected void onStart() {
//        super.onStart();
//        Log.d("debug","onStart()");
//    }
//
//    @Override
//    protected void onRestart() {
//        super.onRestart();
//        Log.d("debug","onRestart()");
//
//    }
//
//    @Override
//    protected void onResume() {
//        super.onResume();
//        Log.d("debug","onResume()");
//    }
//
//    @Override
//    protected void onPause() {
//        super.onPause();
//        Log.d("debug","onPause()");
//
//    }
//
//    @Override
//    protected void onStop() {
//        super.onStop();
//        Log.d("debug","onStop()");
//
//    }
//
//    @Override
//    protected void onDestroy() {
//        super.onDestroy();
//        Log.d("debug","onDestroy()");
//    }

    @Override
    public void onBackPressed() {  // on Back_Key

        finish();
    }
}