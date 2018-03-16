package bitwalking.bitwalking.mvi.registration;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import com.jakewharton.rxbinding2.widget.RxTextView;

import java.util.concurrent.TimeUnit;

import bitwalking.bitwalking.R;
import bitwalking.bitwalking.util.ActivityUtils;
import butterknife.BindView;
import butterknife.ButterKnife;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;
import uk.co.chrisjenx.calligraphy.CalligraphyContextWrapper;

/**
 * Created by alexey on 29.08.17.
 */

public class ActivityPassword extends AppCompatActivity{



    @BindView(R.id.passView)
    EditText passView;
    @BindView(R.id.doneView)
    Button doneView;
    @BindView(R.id.toolbar)
    Toolbar toolbar;

    private static  final int PASSLENGHT=8;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.layout_registration_passwd);
        ButterKnife.bind(this);
        ActivityUtils.ColorizeStatusBar(this,android.R.color.white);
        overridePendingTransition(R.anim.enter_from_right, R.anim.exit_to_left);
        setSupportActionBar(toolbar);
        handleInit();
    }

    private void handleInit(){
        RxTextView.textChanges(passView)
                .debounce(100, TimeUnit.MILLISECONDS)
                .map(it->it.length()>=PASSLENGHT)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(it->doneView.setEnabled(it));
        toolbar.setNavigationOnClickListener(v->onBackPressed());
    }


    public void onDoneClick(View v){
        Intent intent = new Intent(this,ActivityDone.class);
        intent.putExtras(getIntent());
        startActivity(intent);
    }





    @Override
    public void onBackPressed() {
        super.onBackPressed();
        overridePendingTransition(R.anim.enter_from_left, R.anim.exit_to_right);
    }

    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(CalligraphyContextWrapper.wrap(newBase));
    }
}
