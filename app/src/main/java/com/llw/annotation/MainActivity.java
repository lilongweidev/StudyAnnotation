package com.llw.annotation;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;

import com.llw.apt_annotation.BindView;

public class MainActivity extends AppCompatActivity {

    @BindView(R.id.tv_text)
    TextView tvText;
    @BindView(R.id.btn_text)
    Button btnText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        CustomKnife.bind(this);
        tvText.setText("Annotation Processor");
        btnText.setText("This is Button");
    }

}