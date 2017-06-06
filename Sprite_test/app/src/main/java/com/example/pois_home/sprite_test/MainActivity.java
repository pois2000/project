package com.example.pois_home.sprite_test;

import android.app.Activity;
import android.content.Context;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.drawable.AnimationDrawable;
import android.graphics.drawable.BitmapDrawable;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;

import java.io.IOException;
import java.io.InputStream;
import java.util.Random;


public class MainActivity extends Activity {
    // frame width
    final int FRAME_W = 70;
    // frame height
    final int FRAME_H = 103;
    // number of frames
//    private static final int NB_FRAMES = 25;
    // nb of frames in x
    final int COUNT_X = 5;
    // nb of frames in y
    final int COUNT_Y = 5;
    // frame duration
    // we can slow animation by changing frame duration
    final int FRAME_DURATION = 200; // in ms !
    // scale factor for each frame
    final int SCALE_FACTOR = 5;
    public ImageView img;
    // stores each frame
    public Bitmap[][] bmps;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        MyView img = new MyView(MainActivity.this);
        setContentView(img);

        // load bitmap from assets
        Bitmap birdBmp = getBitmapFromAssets(this, "family_sprite2.png");

        if (birdBmp != null) {
            bmps = new Bitmap[COUNT_X][COUNT_Y];


            for (int i = 0; i < COUNT_Y; i++) {
                for (int j = 0; j < COUNT_X; j++) {
                    bmps[j][i] = Bitmap.createBitmap(birdBmp, FRAME_W
                            * j, FRAME_H * i, FRAME_W, FRAME_H);

                    // apply scale factor
                    bmps[j][i] = Bitmap.createScaledBitmap(
                            bmps[j][i], FRAME_W * SCALE_FACTOR, FRAME_H
                                    * SCALE_FACTOR, true);
                }
            }

            // create animation programmatically
            final AnimationDrawable animation = new AnimationDrawable();
            animation.setOneShot(false); // repeat animation
            int r = new Random().nextInt(COUNT_Y);
            for (int i = 0; i < COUNT_X; i++) {
                animation.addFrame(new BitmapDrawable(getResources(), bmps[i][r]),
                        FRAME_DURATION);
            }

            // load animation on image
            if (Build.VERSION.SDK_INT < 16) {
                img.setBackgroundDrawable(animation);
            } else {
                img.setBackground(animation);
            }

            // start animation on image
            img.post(new Runnable() {

                @Override
                public void run() {
                    animation.start();
                }

            });

        }
    }

    private Bitmap getBitmapFromAssets(MainActivity mainActivity,
                                       String filepath) {
        AssetManager assetManager = mainActivity.getAssets();
        InputStream istr = null;
        Bitmap bitmap = null;

        try {
            istr = assetManager.open(filepath);
            bitmap = BitmapFactory.decodeStream(istr);
        } catch (IOException ioe) {
            // manage exception
        } finally {
            if (istr != null) {
                try {
                    istr.close();
                } catch (IOException e) {
                }
            }
        }

        return bitmap;
    }
}

class MyView extends View {
    public MyView(Context context) {
        super(context); // 부모의 인자값이 있는 생성자를 호출한다
    }

    @Override
    protected void onDraw(Canvas canvas) { // 화면을 그려주는 작업

        int[][] slot = new int[][] {{25,25},{50,25},{75,25},
                {25,50},{50,50},{75,50},
                {25,75},{50,75},{75,75}};

//        setBackgroundColor(Color.GREEN); // 배경색을 지정
        Paint paint = new Paint(); // 화면에 그려줄 도구를 셋팅하는 객체
        paint.setColor(Color.BLACK); // 색상을 지정
        int dx = canvas.getWidth();
        int dy = canvas.getHeight();
        int w = 20*dx/200;
        int h = 1*dy/200;

        System.out.println("dx:"+dx);
        System.out.println("dy:"+dy);
        System.out.println("density:"+canvas.getDensity());

        for(int i=0;i<9;i++){
            int x = slot[i][0]*dx/100;
            int y = slot[i][1]*dy/100;
            canvas.drawRect(x-w,y-h,x+w,y+h,paint); // 사각형의 좌상,우하 좌표
        }

        int r = new Random().nextInt(9);
        int x1 = slot[r][0]*dx/100;
        int y1 = (slot[r][1]-5)*dy/100;
        paint.setColor(Color.YELLOW);
        canvas.drawCircle(x1, y1, 40, paint); // 원의중심 x,y, 반지름,paint

    }
}