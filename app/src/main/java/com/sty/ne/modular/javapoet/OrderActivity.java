package com.sty.ne.modular.javapoet;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;


import com.sty.ne.modular.annotation.ARouter;

import androidx.appcompat.app.AppCompatActivity;

@ARouter(path = "/app/OrderActivity")
public class OrderActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_order);

        Log.e("sty", "--> OrderActivity");
    }

    public void jumpToPersonal(View view) {
        Class<?> targetClass = PersonalActivity$$ARouter.findTargetClass("/app/PersonalActivity");
        startActivity(new Intent(this, targetClass));
    }
}