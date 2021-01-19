package com.example.user.tesseracttess_twoocr;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Environment;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.example.user.tesseracttess_twoocr.Camera2API.Camera2Activity;
import com.googlecode.tesseract.android.TessBaseAPI;

public class OldMainActivity extends AppCompatActivity {

    private static final String SD_PATH= Environment.getExternalStorageDirectory().getPath();

    private Button button;
    private TextView textView;
    private EditText editText;
    private int i = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        VariableEditor.ORC_type = "eng";
        VariableEditor.Picture_type = "1";

                Toast.makeText(OldMainActivity.this, SD_PATH ,  Toast.LENGTH_SHORT).show();
        textView = (TextView) findViewById(R.id.textView);
        button = (Button) findViewById(R.id.button);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                editText = (EditText) findViewById(R.id.editText);
                String str = String.valueOf(i) + ".jpg";
                editText.setText( str);

                TessBaseAPI baseApi = new TessBaseAPI();
                // 指定語言集，sd卡根目錄下放置Tesseract的tessdata資料夾
                baseApi.init(SD_PATH, "eng");
                // 設置psm模式
                baseApi.setPageSegMode(TessBaseAPI.PageSegMode.PSM_SINGLE_LINE);


               // String img = editText.getText().toString();


                BitmapFactory.Options options = new BitmapFactory.Options();
                options.inPreferredConfig = Bitmap.Config.ARGB_8888;
                Bitmap bitmap = BitmapFactory.decodeFile(SD_PATH + "/img/" +   String.valueOf( i ) + ".jpg" , options);

                ImageView imageView = findViewById(R.id.imageView);
                imageView.setImageBitmap( bitmap);
                // 設置圖片
                baseApi.setImage( bitmap );
               // baseApi.setImage(new File(SD_PATH + img));

                // 獲取結果
                final String result = baseApi.getUTF8Text();
                textView.setText(result);
                // 釋放記憶體
                baseApi.clear();
                baseApi.end();

                 i++;
            }
        });


        Button btn = (Button) findViewById(R.id.btn);
        btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

            }
        });

        int MY_PERMISSIONS_REQUEST_READ_CONTACTS = 1; // Results code
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(OldMainActivity.this, new String[]{
                            Manifest.permission.CAMERA,
                            Manifest.permission.READ_EXTERNAL_STORAGE,
                            Manifest.permission.RECORD_AUDIO
                    },
                    MY_PERMISSIONS_REQUEST_READ_CONTACTS);
        } else {
            Intent intentv = new Intent(getApplication(), Camera2Activity.class);
            startActivity(intentv);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        if (requestCode == 1) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Intent intentv = new Intent(getApplication(), Camera2Activity.class);
                startActivity(intentv);
            } else {
                Toast toast = Toast.makeText(this, "Error", Toast.LENGTH_SHORT);
                toast.show();
            }
        }
    }

}
