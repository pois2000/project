/**
 * Created by pois_home on 2017-06-06.
 */

public class CanvasActivity extends ActionBarActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
//        setContentView(R.layout.activity_main);
        MyView m = new MyView(MainActivity.this);
        setContentView(m);
    } // end of onCreate
} // end of class

class MyView extends View {
    public MyView(Context context) {
        super(context); // 부모의 인자값이 있는 생성자를 호출한다
    }

    @Override
    protected void onDraw(Canvas canvas) { // 화면을 그려주는 작업
        Paint paint = new Paint(); // 화면에 그려줄 도구를 셋팅하는 객체
        paint.setColor(Color.RED); // 색상을 지정

        setBackgroundColor(Color.GREEN); // 배경색을 지정
        canvas.drawRect(100,100,200,200,paint); // 사각형의 좌상,우하 좌표
        canvas.drawCircle(300, 300, 40, paint); // 원의중심 x,y, 반지름,paint

        paint.setColor(Color.YELLOW);
        paint.setStrokeWidth(10f);    // 선의 굵기
        canvas.drawLine(400, 100, 500, 150, paint); // 직선

        // path 자취 만들기
        Path path = new Path();
        path.moveTo(20, 100); // 자취 이동
        path.lineTo(100, 200); // 자취 직선
        path.cubicTo(150, 30, 200, 300, 300, 200); // 자취 베이지곡선

        paint.setColor(Color.MAGENTA);

        canvas.drawPath(path, paint);
    }
}
