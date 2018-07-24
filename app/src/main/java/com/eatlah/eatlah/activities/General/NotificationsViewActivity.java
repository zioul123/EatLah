package com.eatlah.eatlah.activities.General;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import com.eatlah.eatlah.Config;
import com.eatlah.eatlah.R;
import com.eatlah.eatlah.helpers.MyFirebaseMessagingService;
import com.eatlah.eatlah.helpers.NotificationUtils;

public class NotificationsViewActivity extends AppCompatActivity {

    private static final String TAG = NotificationsViewActivity.class.getSimpleName();
    private BroadcastReceiver mRegistrationBroadcastReceiver;
    private TextView txtTitle, txtBody;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_notifications_view);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        toolbar.setTitle("Order Progress Notifications Page");
        setSupportActionBar(toolbar);

        Log.d(TAG, "on create called");

        txtTitle = (TextView) findViewById(R.id.txtTitle_textView);
        txtBody = (TextView) findViewById(R.id.txtBody_textView);
        setTextFields();

        mRegistrationBroadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {

                // checking for type intent filter
                if (intent.getAction().equals(Config.PUSH_NOTIFICATION)) {
                    // new push notification is received

                    String msg_title = intent.getStringExtra(MyFirebaseMessagingService.MSG_TITLE);
                    String msg_body = intent.getStringExtra(MyFirebaseMessagingService.MSG_BODY);

                    Toast.makeText(getApplicationContext(), "Push notification (title): " + msg_title + " (body): " + msg_body, Toast.LENGTH_LONG).show();
                    txtTitle.setText(msg_title);
                    txtBody.setText(msg_body);
                }
            }
        };
    }

    private void setTextFields() {
        Intent intent = getIntent();
        txtTitle.setText(intent.getStringExtra(MyFirebaseMessagingService.MSG_TITLE));
        txtBody.setText(intent.getStringExtra(MyFirebaseMessagingService.MSG_BODY));
    }

    @Override
    protected void onResume() {
        super.onResume();

        // register new push message receiver
        // by doing this, the activity will be notified each time a new message arrives
        LocalBroadcastManager.getInstance(this).registerReceiver(mRegistrationBroadcastReceiver,
                new IntentFilter(Config.PUSH_NOTIFICATION));

        // clear the notification area when the app is opened
        NotificationUtils.clearNotifications(getApplicationContext());
    }

    @Override
    protected void onPause() {
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mRegistrationBroadcastReceiver);
        super.onPause();
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
    }
}