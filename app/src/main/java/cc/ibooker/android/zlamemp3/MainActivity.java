package cc.ibooker.android.zlamemp3;


import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.widget.TextView;

import cc.ibooker.android.zlamemp3lib.Mp3Converter;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        TextView tv = findViewById(R.id.tv);
        tv.setText(Mp3Converter.getLameVersion());
    }
}
