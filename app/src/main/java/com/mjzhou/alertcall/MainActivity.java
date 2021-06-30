package com.mjzhou.alertcall;
import android.Manifest;
import android.os.Handler;
import android.os.Message;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.util.Log;
import android.view.View;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;


public class MainActivity extends AppCompatActivity {

    private  EditText etApi;
    private  EditText etApiTime;
    private  EditText etLog;
    private  Button btnSet;
    private  String strApi;
    private  String strApiTime;
    private SharedPreferencesUtils sh;
    public  static Handler handler;
    String TAG = "MainActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //设置默认值
        setContentView(R.layout.activity_main);

        etApi =  findViewById(R.id.etApi);
        etApiTime =  findViewById(R.id.etApiTime);
        sh =  new  SharedPreferencesUtils();

        strApi=sh.getParam( MainActivity.this,"strApi","").toString();
        strApiTime= sh.getParam( MainActivity.this,"strApiTime","").toString();
        if("".equals(strApi)){
            Log.e(TAG,"请设置url参数");
            sh.setParam( MainActivity.this,"strApi","http://14.215.177.39/alert/phone");
            sh.setParam( MainActivity.this,"strApiTime",10);
            etApi.setText("http://14.215.177.39/alert/phone",EditText.BufferType.EDITABLE);
            etApiTime.setText("10",EditText.BufferType.EDITABLE);
        }else {
            strApi=sh.getParam( MainActivity.this,"strApi","").toString();
            Log.e(TAG,strApi);
            strApiTime= sh.getParam( MainActivity.this,"strApiTime","").toString();
            etApi.setText(strApi,EditText.BufferType.EDITABLE);
            etApiTime.setText(strApiTime,EditText.BufferType.EDITABLE);
        }
        //申请接受短信权限

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECEIVE_SMS)!= PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,new String []{Manifest.permission.RECEIVE_SMS},1);//无权限则询问开启权限
        }
        //申请电话权限
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CALL_PHONE)!= PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,new String []{Manifest.permission.CALL_PHONE},1);//无权限则询问开启权限
        }



        etLog =  findViewById(R.id.etLog);
        handler = new Handler(new Handler.Callback(){
            @Override
            public boolean handleMessage(Message msg) {
                String string = (String)msg.obj;
                etLog.setText(string);
                return  false;
            }
        });
        //

        bindViews();
        if(ContextCompat.checkSelfPermission(this, Manifest.permission.RECEIVE_SMS)== PackageManager.PERMISSION_GRANTED){
            if(ContextCompat.checkSelfPermission(this, Manifest.permission.CALL_PHONE)== PackageManager.PERMISSION_GRANTED){
                //启动主进程
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    this.startForegroundService(new Intent(this, CallService.class));
                } else {
                    this.startService(new Intent(this, CallService.class));
                }


            }
        }
    }

    private void bindViews() {
        etApi =  findViewById(R.id.etApi);
        etApiTime =  findViewById(R.id.etApiTime);
        btnSet = findViewById(R.id.btnset);
        btnSet.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                strApi = etApi.getText().toString();
                strApiTime= etApiTime.getText().toString();
                sh.setParam( MainActivity.this,"strApi",strApi);
                sh.setParam( MainActivity.this,"strApiTime",strApiTime);
                etLog.setText("更新中...");
                Log.d(TAG,"已设置"+strApi);
                Toast ts = Toast.makeText(getBaseContext(),"设置已生效", Toast.LENGTH_LONG);
                ts.show();
            }
        });

}
}




