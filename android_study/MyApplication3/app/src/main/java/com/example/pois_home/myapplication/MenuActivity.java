package com.example.pois_home.myapplication;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.text.method.ScrollingMovementMethod;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import net.objecthunter.exp4j.Expression;
import net.objecthunter.exp4j.ExpressionBuilder;


public class MenuActivity extends AppCompatActivity {
    public String exp="";
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_menu);

    }

    public void click(View v){
        String str = ((Button)v).getText().toString();
        exp = exp + str;
        TextView display = (TextView) findViewById(R.id.Display);
        display.setText(exp);
        display.setMovementMethod(new ScrollingMovementMethod());
    }

    public void allclear(View v){
        TextView display = (TextView) findViewById(R.id.Display);
        exp = " ";
        display.setText(exp);
    }

    public void clear(View v){
        try {
//            Toast.makeText(this, exp.length(), Toast.LENGTH_SHORT).show();
            TextView display = (TextView) findViewById(R.id.Display);
            exp = exp.substring(0, exp.length()-1);
            display.setText(exp);
        }
        catch(Exception e){
            TextView display = (TextView) findViewById(R.id.Display);
            display.setText(" ");

        }

    }

    public void equal(View v){
        TextView display = (TextView) findViewById(R.id.Display);
        try {
            Expression calc = new ExpressionBuilder(exp).build();
            double result = calc.evaluate();
//            java.text.DecimalFormat df = new java.text.DecimalFormat("#,###.000");
//            String temp = String.valueOf(df.format(result));
//            exp = temp.replace(",","");
            display.setText(String.valueOf(result));
            exp=String.valueOf(result);
        }
        catch (Exception e) {
            Toast.makeText(this, "계산이 불가능한 식입니다.", Toast.LENGTH_SHORT).show();
//            exp = "";
//            display.setText(exp);
        }
    }

}

