package com.cj.mobile.myarapplication;

import android.Manifest;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import permissions.dispatcher.NeedsPermission;
import permissions.dispatcher.OnNeverAskAgain;
import permissions.dispatcher.OnPermissionDenied;
import permissions.dispatcher.RuntimePermissions;

@RuntimePermissions
public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        MainActivityPermissionsDispatcher.runAppWithPermissionCheck(this);
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        MainActivityPermissionsDispatcher.onRequestPermissionsResult(this, requestCode, grantResults);
    }

    /**
     * 申请权限成功后的逻辑
     */
    @NeedsPermission({
            Manifest.permission.CAMERA,
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
    })
    public void runApp() {
        Toast.makeText(this, "权限申请成功", Toast.LENGTH_SHORT).show();
        Log.i("MainActivity", "runApp: 权限申请成功");

        // 添加Fragment到容器
//        getSupportFragmentManager().beginTransaction()
//                .replace(R.id.fragment_container, new CameraFragment())
//                .commit();

        findViewById(R.id.btn_switch_camera).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(MainActivity.this, FaceActivity.class);
                startActivity(intent);
            }
        });
    }
}