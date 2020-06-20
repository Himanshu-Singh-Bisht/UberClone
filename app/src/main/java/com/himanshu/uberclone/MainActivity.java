package com.himanshu.uberclone;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.Toast;

import com.parse.LogInCallback;
import com.parse.ParseAnonymousUtils;
import com.parse.ParseException;
import com.parse.ParseInstallation;
import com.parse.ParseUser;
import com.parse.SaveCallback;
import com.parse.SignUpCallback;
import com.shashank.sony.fancytoastlib.FancyToast;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    enum State{
        SIGNUP , LOGIN
    }

    private State state;
    private Button btnSignUpLogin , btnOneTimeLogin;
    private RadioButton rdbDriver , rdbPassenger;
    private EditText edtUsername , edtPassword , edtDriverOrPassenger;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        ParseInstallation.getCurrentInstallation().saveInBackground();

        state = State.SIGNUP;

        edtUsername = findViewById(R.id.edtUsername);
        edtPassword = findViewById(R.id.edtPassword);

        edtDriverOrPassenger = findViewById(R.id.edtDriverOrPassenger);

        btnSignUpLogin = findViewById(R.id.btnSignUpLogin);
        rdbDriver = findViewById(R.id.rdbDriver);
        rdbPassenger = findViewById(R.id.rdbPassenger);
        btnOneTimeLogin = findViewById(R.id.btnOneTimeLogin);

        btnSignUpLogin.setOnClickListener(this);
        btnOneTimeLogin.setOnClickListener(this);


        if(ParseUser.getCurrentUser() != null)
        {
            //ParseUser.logOut();
            transitionToPassengerActivity();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_signup , menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        switch(item.getItemId())
        {
            case R.id.loginItem :
                if(state == State.SIGNUP)
                {
                    state = State.LOGIN;
                    item.setTitle("Sign Up");
                    btnSignUpLogin.setText("Login");
                }
                else if(state == State.LOGIN)
                {
                    state = State.SIGNUP;
                    item.setTitle("Login");
                    btnSignUpLogin.setText("Sign Up");
                }
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onClick(View view)
    {
        switch (view.getId())
        {
            case R.id.btnSignUpLogin :
                if(state == State.SIGNUP)
                {
                    if(rdbDriver.isChecked() == false && rdbPassenger.isChecked() == false)
                    {
                        FancyToast.makeText(MainActivity.this , "Choose whether you are Driver or Passenger.." ,
                                FancyToast.LENGTH_SHORT , FancyToast.ERROR , true).show();
                        return;
                    }

                    ParseUser appUser = new ParseUser();
                    appUser.setUsername(edtUsername.getText().toString());
                    appUser.setPassword(edtPassword.getText().toString());

                    if(rdbDriver.isChecked())       // as from multiple radio buttons, we can only choose one.
                    {
                        appUser.put("as" , "Driver");
                    }
                    else if(rdbPassenger.isChecked())
                    {
                        appUser.put("as" , "Passenger");
                    }

                    appUser.signUpInBackground(new SignUpCallback() {
                        @Override
                        public void done(ParseException e) {
                            if(e == null)
                            {
                                Toast.makeText(MainActivity.this , "Signed Up!"  , Toast.LENGTH_SHORT).show();
                                transitionToPassengerActivity();        // to transition to passenger activity
                            }
                        }
                    });
                }
                else if(state == State.LOGIN)
                {
                    ParseUser.logInInBackground(edtUsername.getText().toString() , edtPassword.getText().toString(), new LogInCallback() {
                        @Override
                        public void done(ParseUser user, ParseException e) {
                            if(user != null && e == null)
                            {
                                Toast.makeText(MainActivity.this , "User Logged In!" , Toast.LENGTH_SHORT).show();
                                transitionToPassengerActivity();            // to transition to passenger activity
                            }
                            else if(user == null)
                            {
                                Toast.makeText(MainActivity.this , "User Not Signed Up!" , Toast.LENGTH_SHORT).show();
                            }
                            else
                            {
                                Toast.makeText(MainActivity.this , e.getMessage() , Toast.LENGTH_SHORT).show();
                            }
                        }
                    });
                }

                break;

            case R.id.btnOneTimeLogin :

                if(edtDriverOrPassenger.getText().toString().equals("Driver") || edtDriverOrPassenger.getText().toString().equals("Passenger"))
                {
                    if(ParseUser.getCurrentUser() == null)
                    {
                        // NOW , WE CAN GENERATE THE ANONYMOUS USER.
                        ParseAnonymousUtils.logIn(new LogInCallback() {
                            @Override
                            public void done(ParseUser user, ParseException e) {
                                if(user != null && e == null)
                                {
                                    Toast.makeText(MainActivity.this , "We have an anonymous User" , Toast.LENGTH_SHORT).show();

                                    user.put("as" , edtDriverOrPassenger.getText().toString());
                                    user.saveInBackground(new SaveCallback() {
                                        @Override
                                        public void done(ParseException e) {
                                            if(e == null)
                                            {
                                                transitionToPassengerActivity();            // to transition to passenger activity
                                            }
                                        }
                                    });
                                }
                            }
                        });
                    }
                }
                else
                {
                    Toast.makeText(MainActivity.this , "" , Toast.LENGTH_SHORT).show();
                }
                break;
        }
    }

    // Method to transition to passenger activity.
    private void transitionToPassengerActivity()
    {
        if(ParseUser.getCurrentUser() != null)
        {
            if(ParseUser.getCurrentUser().get("as").equals("Passenger"))
            {
                Intent intent = new Intent(MainActivity.this , PassengerActivity.class);
                startActivity(intent);
            }
        }
    }
}