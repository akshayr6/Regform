package com.example.khush.reg;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.Layout;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.FirebaseException;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.PhoneAuthCredential;
import com.google.firebase.auth.PhoneAuthProvider;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;

import java.util.Collection;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

public class MainActivity extends AppCompatActivity {
    View login_first;
    View login_second;
    ImageView first_back;
    int activity=0;
    FirebaseFirestore db;
    CollectionReference ref;
    EditText usn_et;
    EditText otp_et;
    TextView forgot_pwd;
    EditText pwd_et;
    FloatingActionButton otp_fab;
    FloatingActionButton email_fab;
    String usn;
    String pwd;
    private PhoneAuthProvider.OnVerificationStateChangedCallbacks mCallBack;
    private FirebaseAuth auth;
    private String mVerificationId ;
    private PhoneAuthProvider.ForceResendingToken mResendToken ;
    FloatingActionButton fab;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        activate();
        if(auth.getCurrentUser()!=null)
        {
            //if logged in already
            startActivity(new Intent(this,Main2Activity.class));
            finish();

        }
         fab = (FloatingActionButton) findViewById(R.id.fab);

        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                usn_et.setHintTextColor(Color.rgb(255,255,255));
                usn=usn_et.getText().toString().toLowerCase();
                if(usn.length()<10) {
                    error(usn_et,fab);
                }else{
                    usn_et.setEnabled(false);
                    fab.setEnabled(false);

                        ref.document(usn).get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                            @Override
                            public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                                if (task.getResult().exists()) {
                                    DocumentSnapshot studentdata = task.getResult();
                                    Snackbar.make(getCurrentFocus(), "Resend after 20 seconds.", Snackbar.LENGTH_SHORT)
                                            .setAction("Action", null).show();
                                    Timer buttonTimer = new Timer();
                                    buttonTimer.schedule(new TimerTask() {

                                        @Override
                                        public void run() {
                                            runOnUiThread(new Runnable() {

                                                @Override
                                                public void run() {
                                                    fab.setEnabled(true);
                                                }
                                            });
                                        }
                                    }, 20000);
                                    Login_first();
                                    usn_et.setEnabled(true);

                                    PhoneAuthProvider.getInstance().verifyPhoneNumber(studentdata.get("Phone Number").toString(), 60, TimeUnit.SECONDS, MainActivity.this, mCallBack);
                                } else {
                                    error(usn_et, fab);
                                    Log.d("task","",task.getException());
                                }
                            }

                        });



                }



            }
        });


        otp_fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                otp_et.setEnabled(false);
                otp_fab.setEnabled(false);
                if(isValidOtp(otp_et.getText().toString().trim())) {
                    PhoneAuthCredential credential=PhoneAuthProvider.getCredential(mVerificationId, otp_et.getText().toString().trim());
                    signInWithPhoneAuthCredential(credential);

                }
                else
                {
                    error(otp_et,otp_fab);
                }
            }

    });
        email_fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                email_fab.setEnabled(false);
                pwd_et.setEnabled(false);
                ref.document(usn).get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                        if(task.getResult().exists())
                        {
                            DocumentSnapshot studentdata=task.getResult();

                            auth.signInWithEmailAndPassword(studentdata.get("Email").toString(), pwd_et.getText().toString().trim().length()<1?"z":pwd_et.getText().toString() )
                                    .addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                                        @Override
                                        public void onComplete(@NonNull Task<AuthResult> task) {
                                            if(task.isSuccessful())
                                            {
                                                Toast.makeText(getApplicationContext(),"Signing In",Toast.LENGTH_SHORT).show();
                                                startActivity(new Intent(MainActivity.this,Main2Activity.class));

                                                finish();
                                            }
                                            else
                                            {
                                                error(pwd_et,email_fab);

                                            }
                                        }
                                    });




                        }
                        else{
                            Login_first_back();
                            error(usn_et,fab);
                        }
                    }
                });

            }

        });
        forgot_pwd.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                forgot_pwd.setEnabled(false);
                pwdreset();

            }
        });
    }


    @Override
    public void onBackPressed() {
        if(activity==1)
        Login_first_back();
        else
            finish();
    }

    void activate()
    {
login_first=findViewById(R.id.enter_usn);
login_second=findViewById(R.id.verification);
first_back=findViewById(R.id.first_back);
db=FirebaseFirestore.getInstance();
ref=db.collection("users");
usn_et=findViewById(R.id.usn);
otp_et=findViewById(R.id.otp);
otp_fab=findViewById(R.id.fab_otp);
email_fab=findViewById(R.id.fab_pwd);
pwd_et=findViewById(R.id.pwd);
forgot_pwd=findViewById(R.id.forgot_pwd);
        auth=FirebaseAuth.getInstance();
        mCallBack=new PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
            @Override
            public void onVerificationCompleted(PhoneAuthCredential phoneAuthCredential) {
                signInWithPhoneAuthCredential(phoneAuthCredential);


            }

            @Override
            public void onVerificationFailed(FirebaseException e) {
                Toast.makeText(getApplicationContext(),"Invalid OTP or your account maybe blocked",Toast.LENGTH_SHORT).show();
                Log.d("error","error:",e);

            }
            @Override
            public void onCodeSent(String verificationId,
                                   PhoneAuthProvider.ForceResendingToken token) {
                // The SMS verification code has been sent to the provided phone number, we
                // now need to ask the user to enter the code and then construct a credential
                // by combining the code with a verification ID.


                // Save verification ID and resending token so we can use them later
                mVerificationId = verificationId;
                mResendToken = token;
                Snackbar.make(getCurrentFocus(), "An OTP has been sent to your number", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();


            }
        };

    }
    private void signInWithPhoneAuthCredential(final PhoneAuthCredential credential) {
        auth.signInWithCredential(credential)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {

                            // Sign in success, update UI with the signed-in user's information
                            otp_et.setText(credential.getSmsCode());
                            Toast.makeText(getApplicationContext(),"Signing In",Toast.LENGTH_SHORT).show();
                            startActivity(new Intent(MainActivity.this,Main2Activity.class));

                            finish();

                            FirebaseUser user = task.getResult().getUser();
                            // ...
                        } else {
                            // Sign in failed, display a message and update the UI
                            error(otp_et,otp_fab);
                            if (task.getException() instanceof FirebaseAuthInvalidCredentialsException) {
                                Toast.makeText(getApplicationContext(),"Invalid OTP",Toast.LENGTH_SHORT).show();
                            }
                        }
                    }
                });
    }

    void Login_first()
    {
        login_first.animate().translationY(login_first.getHeight());
        activity=1;

        login_second.setVisibility(View.VISIBLE);

        first_back.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Login_first_back();
            }
        });
    }
    void Login_first_back()
    {
        login_second.setVisibility(View.INVISIBLE);
        activity=0;
        login_first.setVisibility(View.VISIBLE);
        login_first.animate().translationY(0);
    }
    void error(EditText errorTB,FloatingActionButton fab)
    {
        Vibrator v = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
        // Vibrate for 500 milliseconds
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            v.vibrate(VibrationEffect.createOneShot(500,VibrationEffect.DEFAULT_AMPLITUDE));
        }else{
            //deprecated in API 26
            v.vibrate(500);
        }
        errorTB.setEnabled(true);

        errorTB.setText("");
        errorTB.setHintTextColor(Color.rgb(255,0,0));
        fab.setEnabled(true);
    }
        private boolean isValidOtp(CharSequence otp) {
            boolean check=false;
            if(Pattern.matches("[0-9]+", otp))
            {
                if(otp.length() < 6 || otp.length() > 6)
                {
                    check = false;

                }
                else
                {
                    check = true;

                }
            }
            else
            {
                check=false;
            }
            return check;
        }
        void pwdreset()
        {

            ref.document(usn).get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                @Override
                public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                    if(task.getResult().exists())
                    {
                        DocumentSnapshot studentdata=task.getResult();

                        auth.sendPasswordResetEmail(studentdata.get("Email").toString())
                                .addOnCompleteListener(new OnCompleteListener<Void>() {
                                    @Override
                                    public void onComplete(@NonNull Task<Void> task) {
                                        if (task.isSuccessful()) {
                                            Snackbar.make(getCurrentFocus(), "Steps for password reset has been mailed to you", Snackbar.LENGTH_LONG)
                                                    .setAction("Action", null).show();
                                            forgot_pwd.setEnabled(true);
                                        }
                                        else
                                        {
                                            Login_first_back();
                                            error(usn_et,fab);
                                            forgot_pwd.setEnabled(true);
                                        }
                                    }
                                });




                    }
                    else{
                        Login_first_back();
                        error(usn_et,fab);
                        forgot_pwd.setEnabled(true);
                    }
                }
            });
        }


}
