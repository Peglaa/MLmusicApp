package com.example.mlmusicplayer;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.widget.TextView;

import com.chaquo.python.PyObject;
import com.chaquo.python.Python;
import com.chaquo.python.android.AndroidPlatform;

public class MainActivity extends AppCompatActivity {

    TextView textView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        textView = findViewById(R.id.textView);

        if (! Python.isStarted()) {
            Python.start(new AndroidPlatform(this));
        }

        Python py = Python.getInstance();
        PyObject pyObject = py.getModule("classifier");
        PyObject obj = pyObject.callAttr("test_function", 5);

        textView.setText(obj.toString());
    }
}