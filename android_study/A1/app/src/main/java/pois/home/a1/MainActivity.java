package pois.home.a1;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.telephony.TelephonyManager;
import android.view.View;
import android.widget.ImageButton;
import android.widget.Toast;

import static pois.home.a1.R.layout;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(layout.activity_main);

        ImageButton getbtn = (ImageButton) findViewById(R.id.getlove);
        ImageButton sendbtn = (ImageButton) findViewById(R.id.sendlove);
        getbtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                TelephonyManager telephonyManager = (TelephonyManager) getApplicationContext().getSystemService(getApplicationContext().TELEPHONY_SERVICE);
                String phoneNum = telephonyManager.getLine1Number();
                Toast.makeText(getApplicationContext(), phoneNum+"님에게 온 메시지",1000).show();
                Intent myIntent = new Intent(getApplicationContext(), GetLoveActivity.class);
                startActivity(myIntent);
            }
        });
        sendbtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent myIntent = new Intent(getApplicationContext(), SendLoveActivity.class);
                startActivity(myIntent);
            }
        });

    }


}
