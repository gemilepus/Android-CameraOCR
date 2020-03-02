package com.example.user.tesseracttess_twoocr.Camera2API;

import android.os.Build;
import android.os.Bundle;
import android.support.annotation.RequiresApi;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.WindowManager;

import com.example.user.tesseracttess_twoocr.R;
public class Camera2Activity extends AppCompatActivity {

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
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
    protected void onStart() {
        super.onStart();
        Log.d("debug","onStart()");
    }


    @Override
    protected void onRestart() {
        super.onRestart();
        Log.d("debug","onRestart()");

    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d("debug","onResume()");
    }



    @Override
    protected void onPause() {
        super.onPause();
        Log.d("debug","onPause()");

    }

    @Override
    protected void onStop() {
        super.onStop();
        Log.d("debug","onStop()");

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d("debug","onDestroy()");
    }



    @Override
    public void onBackPressed() {  // on Back_Key

        finish();
    }

}