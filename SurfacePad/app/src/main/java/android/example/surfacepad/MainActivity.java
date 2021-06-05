 package android.example.surfacepad;

import android.Manifest;
import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.app.ActivityManagerCompat;
import androidx.core.content.ContextCompat;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Observer;

 public class MainActivity extends AppCompatActivity {
     private static final String TAG = "MAIN";
     private final int RECORD_PERMISSION_CODE = 9909;
     private final int CALL_PERMISSION_CODE = 9919;
     Intent mServiceIntent;
     private ServiceForBackground mService;

//    public static MutableLiveData<Boolean> isRunning = new MutableLiveData<>(false);
    public static boolean isRunning = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.RECORD_AUDIO}, RECORD_PERMISSION_CODE);
        }

        else if (ContextCompat.checkSelfPermission(this, Manifest.permission.CALL_PHONE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CALL_PHONE}, CALL_PERMISSION_CODE);
        }

        else {
//        if(!isRunning) {
//            isRunning = true;
//            startService(new Intent(this, ServiceForBackground.class));
//        }

            mService = new ServiceForBackground();
            mServiceIntent = new Intent(this, mService.getClass());
            if (!isMyServiceRunning(mService.getClass())) {
                startService(mServiceIntent);
            }

            Intent startMain = new Intent(Intent.ACTION_MAIN);
            startMain.addCategory(Intent.CATEGORY_HOME);
            startMain.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(startMain);
        }
    }

    private boolean isMyServiceRunning(Class<?> serviceClass) {
        ActivityManager manager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if(serviceClass.getName().equals(service.service.getClassName())) {
                Log.d("Service status", "Running");
                return true;
            }
        }
        Log.d("Service status", "Not running");
        return false;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == RECORD_PERMISSION_CODE) {
            if (grantResults.length <= 0 || grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.RECORD_AUDIO}, RECORD_PERMISSION_CODE);
            }
        }
        if (requestCode == CALL_PERMISSION_CODE) {
            if (grantResults.length <= 0 || grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CALL_PHONE}, CALL_PERMISSION_CODE);
            }
        }
    }

     @Override
     protected void onPause() {
         finish();
         super.onPause();
     }

     @Override
     protected void onDestroy() {
         Log.d(TAG, "DESTROYED");
         Intent broadcastIntent = new Intent();
         broadcastIntent.setAction("restartservice");
         broadcastIntent.setClass(this, Restarter.class);
         this.sendBroadcast(broadcastIntent);
         super.onDestroy();
     }
 }