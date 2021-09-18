package com.mjzhou.alertcall;
import android.app.Service;
import android.content.Intent;
import android.net.Uri;
import android.os.IBinder;
import android.util.Log;

import java.util.Date;

public class CallForwardService extends Service {
    String TAG = "CallForwardService";
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();

    }
    @Override
    public int onStartCommand(final Intent intent, int flags, int startId) {
        //这里开辟一条线程,用来执行具体的逻辑操作:
        new Thread(new Runnable() {
            final Intent it = new Intent();
            @Override
            public void run() {
                Log.d("CallForwardService",  new Date().toString());
                String content="";
                content = intent.getStringExtra("content");
                String[] split = content.split("\\|",3);
                String phone = split[1];
                it.setAction(Intent.ACTION_CALL);//有权限则直接ACTION_CALL
                it.setData(Uri.parse("tel:"+phone));
                Log.d(TAG,  phone);
                it.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK );
                startActivity(it);
            }
        }).start();
        return super.onStartCommand(intent, flags, startId);
    }
}