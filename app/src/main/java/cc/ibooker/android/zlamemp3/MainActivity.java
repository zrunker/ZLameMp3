package cc.ibooker.android.zlamemp3;


import android.annotation.SuppressLint;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.widget.TextView;

import cc.ibooker.android.zlamemp3lib.AudioToMp3Util;
import cc.ibooker.android.zlamemp3lib.Mp3Converter;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        TextView tv = findViewById(R.id.tv);
        tv.setText(Mp3Converter.getLameVersion());

//        AudioToMp3Util.getInstance().startConvert(
//                "xxx.pcm/xxx.mav", "xx.mp3", new AudioToMp3Util.ConverterListener() {
//                    @SuppressLint("SetTextI18n")
//                    @Override
//                    public void onProgress(float progress) {
//                        // 转码进度："转码中" + progress + "%"
//                    }
//
//                    @Override
//                    public void onComplete(String mp3Path) {
//                        // 转码完成
//                    }
//
//                    @Override
//                    public void onError(String msg) {
//                        // 转码出错
//                    }
//                });
    }

//    @Override
//    protected void onDestroy() {
//        super.onDestroy();
//        AudioToMp3Util.getInstance().destroy();
//    }
}
