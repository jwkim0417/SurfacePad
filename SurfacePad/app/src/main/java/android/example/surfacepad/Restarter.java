package android.example.surfacepad;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.widget.Toast;

public class Restarter extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d("Broadcast Listened", "Service tried to stop");
        Toast.makeText(context, "Service Started", Toast.LENGTH_SHORT).show();

        context.startForegroundService(new Intent(context, ServiceForBackground.class));
    }
}
