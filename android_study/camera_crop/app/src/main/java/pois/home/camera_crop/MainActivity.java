package pois.home.camera_crop;

import android.graphics.BitmapFactory;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

public class MainActivity extends AppCompatActivity {

    private FaceOverlayView mFaceOverlayView;

    @Override    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mFaceOverlayView = (FaceOverlayView) findViewById(R.id.face_overlay);
        mFaceOverlayView.setBitmap(BitmapFactory.decodeResource(getResources(), R.drawable.img_2));
    }
}