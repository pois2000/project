package intenttest.pois.home.web_camara;

import android.app.Activity;
import android.content.Intent;
import android.content.res.Configuration;
import android.net.Uri;
import android.provider.MediaStore;
import android.provider.Settings;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import java.net.URI;

public class MainActivity extends Activity
{
    Button btn = null;
    ImageView iv = null;
    Uri imageUri = null;
    TextView text = null;
    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        setup();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        // TODO Auto-generated method stub
        super.onConfigurationChanged(newConfig);
    }


    private void setup()
    {
        btn = (Button)findViewById(R.id.btn);
        iv = (ImageView)findViewById(R.id.iv);
        text = (TextView) findViewById(R.id.text);
//        Uri uri = Uri.parse("content://media/external/images/media/6472");
        iv.setImageURI(imageUri);
        text.setText(""+imageUri);


        btn.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                System.out.println("촬영해줘");
                startActivityForResult(intent,1);
                System.out.println("촬영해줘인텐트후");
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        System.out.println("촬영완료");
        imageUri = data.getData();
        iv.setImageURI(imageUri);
//            Toast.makeText(this, "안되안되"+imageUri,Toast.LENGTH_LONG).show();
        text.setText(""+imageUri);
        System.out.println("촬영 사진 저장 경로"+imageUri);
    }
}
