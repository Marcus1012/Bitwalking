package bitwalking.bitwalking.mvi.welcome;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.TabLayout;
import android.support.v4.app.FragmentActivity;
import android.support.v4.view.ViewPager;
import android.view.View;
import android.widget.TextView;

import bitwalking.bitwalking.R;
import bitwalking.bitwalking.mvi.login.LoginActivity;
import bitwalking.bitwalking.mvi.registration.ActivityFirstName;
import bitwalking.bitwalking.util.ActivityUtils;
import uk.co.chrisjenx.calligraphy.CalligraphyContextWrapper;

/**
 * Created by alexey on 11.08.17.
 */

public class ActivityWelcome extends FragmentActivity{

    private ViewPager viewPager;
    private AdapterText adapter ;
    private  TabLayout tabLayout;
    private TextView signupView,signinView;
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.layout_activity_welcome);

        ActivityUtils.AttachTransperentTitleBar(this);
        initViews();
    }

    private void initViews(){
         tabLayout = (TabLayout) findViewById(R.id.tabDots);

        viewPager = (ViewPager)findViewById(R.id.viewPager);
        adapter = new AdapterText(getSupportFragmentManager());
        tabLayout.setupWithViewPager(viewPager, true);
        viewPager.setAdapter(adapter);

        signupView = (TextView)findViewById(R.id.signupView);
        signinView = (TextView)findViewById(R.id.signinView);
    }


    public void signIn(View v){
        startActivity(new Intent(ActivityWelcome.this, LoginActivity.class));
       // finish();
    }

    public void signUp(View v){
        startActivity(new Intent(ActivityWelcome.this, ActivityFirstName.class));
       // finish();
    }
    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(CalligraphyContextWrapper.wrap(newBase));
    }
}
