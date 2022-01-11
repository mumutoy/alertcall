package com.mjzhou.alertcall;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.IBinder;
import android.os.Message;
import android.provider.CallLog;
import android.util.Log;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import okhttp3.*;


import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.TimeUnit;



public class CallService extends Service {
    String TAG = "CallService";
    private SharedPreferencesUtils sh;
    private NotificationManager notificationManager;
    private String notificationId = "channel_Id";
    private String notificationName = "channel_Name";

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        //创建NotificationChannel
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(notificationId, notificationName, NotificationManager.IMPORTANCE_HIGH);
            notificationManager.createNotificationChannel(channel);
        }

        startForeground(1,getNotification());
        new CallThread().start();
    }
    @Override
    public int onStartCommand(final Intent intent, int flags, int startId) {
        //这里开辟一条线程,用来执行具体的逻辑操作:
        return START_STICKY;
    }

    private Notification getNotification() {
        Notification.Builder builder = new Notification.Builder(this)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle("AutoCall")
                .setContentText("");

        //设置Notification的ChannelID,否则不能正常显示

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            builder.setChannelId(notificationId);
        }
        Notification notification = builder.build();
        return notification;

    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    //获取最新同话记录
    private String getCallDetails() {
        StringBuffer sb = new StringBuffer();
        String[] projection = new String[] {
                CallLog.Calls.CACHED_NAME,
                CallLog.Calls.NUMBER,
                CallLog.Calls.TYPE,
                CallLog.Calls.DATE,
                CallLog.Calls.DURATION
        };
        Cursor managedCursor =  getApplicationContext().getContentResolver().query(CallLog.Calls.CONTENT_URI, projection, null, null, null);
        int number = managedCursor.getColumnIndex(CallLog.Calls.NUMBER);
        int type = managedCursor.getColumnIndex(CallLog.Calls.TYPE);
        int date = managedCursor.getColumnIndex(CallLog.Calls.DATE);
        int duration = managedCursor.getColumnIndex(CallLog.Calls.DURATION);
        //游标至最后一条记录
        managedCursor.moveToLast();
        String phNumber = managedCursor.getString(number);
        String callType = managedCursor.getString(type);
        String callDate = managedCursor.getString(date);
        Date callDayTime = new Date(Long.valueOf(callDate));
        String callDuration = managedCursor.getString(duration);
        String dir = null;
        int dircode = Integer.parseInt(callType);
        switch (dircode) {
            case CallLog.Calls.OUTGOING_TYPE:
                dir = "OUTGOING";
                break;

            case CallLog.Calls.INCOMING_TYPE:
                dir = "INCOMING";
                break;

            case CallLog.Calls.MISSED_TYPE:
                dir = "MISSED";
                break;

        }
        long ts =  callDayTime.getTime();
        sb.append(phNumber + "," + dir + "," + ts + "," + callDuration);
        managedCursor.close();
        return sb.toString();

    }

    //拨打电话
    private  void  call(String phone){
        final Intent it = new Intent();
        it.setAction(Intent.ACTION_CALL);//有权限则直接ACTION_CALL
        it.setData(Uri.parse("tel:" + phone));
        it.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(it);

    }

    //请求接口修改记录状态
    private  void  markMsg(String Url) throws IOException {
        OkHttpClient client = new OkHttpClient();
        Request requestPost = new Request.Builder()
                .url(Url)
                .method("POST", RequestBody.create(null, new byte[0]))
                .header("Content-Length", "0")
                .build();
        OkHttpClient Client = client.newBuilder() .readTimeout(25, TimeUnit.SECONDS).build();
        Client.newCall(requestPost).execute();
    }

    //号码是否接通
    private  boolean checkDial(String number,Long ts){
        String call=getCallDetails();
        Log.d(TAG, "读取的最后通话记录"+call);
        String[] split = call.split(",",4);
        String phone = split[0];
        Log.d(TAG, "检查是否接听"+number);
        long recordTs=Long.parseLong(split[2]);
        int spend=Integer.parseInt(split[3]);
        //获取大于给定时间戳通话记录,并判断是否接听
        if(recordTs<ts){
            Log.d(TAG, "未产生接听记录"+number);
            try {
                Thread.sleep(10000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            Log.d(TAG, "recordTs<ts电话拨打开始时间"+ts);
            return checkDial(number,ts);
        }
        if(phone.equals(number)){
            if(spend>0){
                return  true;
            }else {
                return false;
            }
        }else {
            Log.d(TAG, "phone_else电话拨打开始时间"+ts);
            try {
                Thread.sleep(10000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            return false;
        }
    }

    private  void runLog(String tips){
        Message msg = MainActivity.handler.obtainMessage();
        msg.what=1;
        SimpleDateFormat df = new SimpleDateFormat("HH:mm:ss");
        msg.obj=df.format(System.currentTimeMillis())+":"+tips;
        MainActivity.handler.sendMessage(msg);
    }
    //主线程遍历接口打电话
    class CallThread extends Thread
    {

        @Override
        public void run() {

            try {
                sh =  new  SharedPreferencesUtils();
                String msgId="";
                String realPhone="";
                long startStamp =new Date().getTime();
                Thread.sleep(30000);

                while(true){
                    try {
                        //String strApiTel=sh.getParam( CallService.this,"strApiTel","").toString();
                        String strApi=sh.getParam( CallService.this,"strApi","").toString();
                        if("".equals(strApi)){
                            Log.e(TAG,"请设置url参数");
                        }
                        Log.d(TAG,android.os.Build.MODEL);
                        String url = strApi;
                        OkHttpClient okHttpClient = new OkHttpClient();
                        final Request request = new Request.Builder()
                                .url(url).addHeader("model",android.os.Build.MODEL)
                                .build();
                        final Call call = okHttpClient.newCall(request);
                        JSONObject jsonObject=null;
                        try{
                            Response response = call.execute();
                            jsonObject = JSON.parseObject( response.body().string());
                        }catch(Exception e){
                            Log.e(TAG, "接口异常\n"+e);
                            runLog("接口异常请检查或更新");
                            //call(strApiTel);
                            Thread.sleep(15000);
                            continue;
                        }
                        //JSONObject jsonObject = JSON.parseObject( response.body().string());
                        String code= jsonObject.get("code").toString();
                        Log.d(TAG, "本条告警状态码" + code);
                        if("0".equals(code)){
                            String mid= jsonObject.get("mid").toString();
                            JSONArray jsonArr=jsonObject.getJSONArray("phone");
                            Log.d(TAG, "本条告警" + mid);
                            if(!msgId.equals(mid)) {
                                startStamp =new Date().getTime();
                                msgId = mid;
                                Log.d(TAG, "第一次拨打电话");
                                for (Object phone : jsonArr) {
                                    realPhone = phone.toString();
                                    call(realPhone);
                                    break;
                                }
                            }else {
                                Log.d(TAG, "同一条告警处理中" + mid);
                                for (Object phone : jsonArr) {
                                    if(realPhone.equals(phone)){
                                        //本次号码是否有接通记录
                                        if(checkDial(realPhone,startStamp)){
                                            Log.d(TAG, "本次号码接通"+realPhone);
                                            //接通发送post请求标记本条告警完成拨打并接通
                                            String Url=url+"?resp="+realPhone+"&mid="+mid;
                                            markMsg(Url);
                                            break;
                                        } else {
                                            //本次号码未接通
                                            int nextIndex=jsonArr.indexOf(phone)+1;
                                            if(nextIndex==jsonArr.size()){
                                                //最后一条
                                                String Url=url+"?mid="+mid;
                                                markMsg(Url);
                                                Log.d(TAG, "完成全部拨打并未接听");
                                                realPhone="";
                                                msgId="";
                                            }else{
                                                realPhone = jsonArr.get(nextIndex).toString();
                                                Log.d(TAG, "本次号码未接通，拨打下一条"+realPhone);
                                                call(realPhone);
                                                break;
                                            }
                                        }
                                    }
                                }
                            }
                        }else {
                            Log.d(TAG, "无告警");
                            runLog("无告警");
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    String strApiTime=sh.getParam( CallService.this,"strApiTime","").toString();
                    Thread.sleep(Integer.parseInt(strApiTime)*1000);

                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

        }
    }
}