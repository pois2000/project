package com.example.pois_home.sprite_test;
//
//import android.app.Activity;
//import android.content.Context;
//import android.graphics.Canvas;
//import android.graphics.Color;
//import android.graphics.Paint;
//import android.os.Bundle;
//import android.view.View;
//
//import java.util.Random;
//
//
//public class CanvasActivity extends Activity {
//    @Override
//    protected void onCreate(Bundle savedInstanceState) {
//        super.onCreate(savedInstanceState);
//        MyView m = new MyView(CanvasActivity.this);
//        setContentView(m);
//    } // end of onCreate
//} // end of class
//
//
//class MyView extends View {
//    public MyView(Context context) {
//        super(context); // 부모의 인자값이 있는 생성자를 호출한다
//    }
//
//    @Override
//    protected void onDraw(Canvas canvas) { // 화면을 그려주는 작업
//
//        int[][] slot = new int[][] {{25,25},{50,25},{75,25},
//                {25,50},{50,50},{75,50},
//                {25,75},{50,75},{75,75}};
//
////        setBackgroundColor(Color.GREEN); // 배경색을 지정
//        Paint paint = new Paint(); // 화면에 그려줄 도구를 셋팅하는 객체
//        paint.setColor(Color.BLACK); // 색상을 지정
//        int dx = canvas.getWidth();
//        int dy = canvas.getHeight();
//        int w = 20*dx/200;
//        int h = 1*dy/200;
//
//        System.out.println("dx:"+dx);
//        System.out.println("dy:"+dy);
//        System.out.println("density:"+canvas.getDensity());
//
//        for(int i=0;i<9;i++){
//            int x = slot[i][0]*dx/100;
//            int y = slot[i][1]*dy/100;
//            canvas.drawRect(x-w,y-h,x+w,y+h,paint); // 사각형의 좌상,우하 좌표
//        }
//
//        int r = new Random().nextInt(9);
//        int x1 = slot[r][0]*dx/100;
//        int y1 = (slot[r][1]-5)*dy/100;
//        paint.setColor(Color.YELLOW);
//        canvas.drawCircle(x1, y1, 40, paint); // 원의중심 x,y, 반지름,paint
//
//    }
//}

