package bitwalking.bitwalking.vote_product;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.os.Build;
import android.os.Bundle;
import android.text.Html;
import android.view.View;
import android.view.ViewAnimationUtils;
import android.view.ViewTreeObserver;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;

import bitwalking.bitwalking.AppPreferences;
import bitwalking.bitwalking.BitwalkingApp;
import bitwalking.bitwalking.activityes.BwActivity;
import bitwalking.bitwalking.R;
import bitwalking.bitwalking.server.api.ServerApi;
import bitwalking.bitwalking.server.responses.GetSurveyResponse;
import bitwalking.bitwalking.util.Globals;
import bitwalking.bitwalking.util.Logger;
import bitwalking.bitwalking.util.SpacedTextView;

/**
 * Created by Marcus on 4/11/16.
 */
public class VoteProductActivity extends BwActivity implements OnVoteReady {

    private static final String TAG = VoteProductActivity.class.getSimpleName();

    private String _voteId = null;
    private VoteProductLocal _productsLocal;
    private VoteProductListAdapter _productsAdapter;
    private ListView _productsList;
    private ProgressBar _loadingVoteProgress;
    private ArrayList<VoteProductDrawableInfo> _products = null;
    private boolean _toastShowed;
    private boolean _gotSelectedProduct = false;
    private View _rootLayout;
    boolean _backPressed;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.vote_product_layout);

//        setMainText();

        _productsLocal = new VoteProductLocal(this);
        _loadingVoteProgress = (ProgressBar)findViewById(R.id.vote_loading);
        _loadingVoteProgress.setVisibility(View.VISIBLE);
        _toastShowed = false;

        _productsList = (ListView)findViewById(R.id.vote_product_list);
        _productsList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                String userChoice = _products.get(position).getName();
                String userChoiceId = _products.get(position).getId();
                sendUserVote(userChoiceId);

                if (null != userChoice) {
                    for (VoteProductDrawableInfo p : _products)
                        p.setSelected(p.getName().compareTo(userChoice) == 0);
                }
                _productsAdapter.notifyDataSetChanged();

                ((BitwalkingApp)getApplication()).trackEvent("store.questionnaire", "select." + position, "item." + userChoiceId);
//                refreshVote();
            }
        });

        loadVote();

        _rootLayout = findViewById(R.id.vote_root_layout);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            overridePendingTransition(R.anim.do_not_move, R.anim.do_not_move);

            if (savedInstanceState == null) {
                _rootLayout.setVisibility(View.INVISIBLE);

                ViewTreeObserver viewTreeObserver = _rootLayout.getViewTreeObserver();
                if (viewTreeObserver.isAlive()) {
                    viewTreeObserver.addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
                        @Override
                        public void onGlobalLayout() {
                            enterReveal();
                            _rootLayout.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                        }
                    });
                }
            }
        }
        else {
            overridePendingTransition(R.anim.enter_from_top_right, R.anim.hold);
        }  _backPressed = false;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        destroyProgress();
    }

    private void sendUserVote(final String itemId) {
        ServerApi.sendVote(
                AppPreferences.getUserId(getBaseContext()),
                AppPreferences.getUserSecret(getBaseContext()),
                _voteId,
                itemId,
                new ServerApi.SimpleServerResponseListener() {
                    @Override
                    public void onResponse(int code) {
                        if (200 == code) {
                            _productsLocal.setUserChoice(itemId);
                            if (!_toastShowed) {
                                _toastShowed = true;

                                VoteProductActivity.this.runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        Toast.makeText(VoteProductActivity.this, "Thanks for voting", Toast.LENGTH_SHORT).show();
                                    }
                                });
                            }
                        }
                    }
                });
    }

    private void setMainText() {
        String s1 = "The store is not available in your country. How would you spend your W";
        final String s2 = "$?";//"<sub><small>$</small></sub>?";
        TextView text = (TextView)findViewById(R.id.vote_product_main_text);

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            s1 = SpacedTextView.applySpacing(s1).toString();
        }
        else {
            text.setLetterSpacing(0.1f);
        }

        text.setText(Html.fromHtml(String.format("<br>%s%s<br>", s1, s2)), TextView.BufferType.SPANNABLE);
//        text.setTypeface(FontCache.get(FontCache.FontTypeEnum.DOLCE_VITA_REGULAR, this));
    }

    private void enterReveal() {

        int cx = _rootLayout.getWidth() - 50;//_rootLayout.getWidth() / 2;
        int cy = 50;//_rootLayout.getHeight() / 2;

        float finalRadius =
                (float)Math.sqrt(
                        Math.pow(_rootLayout.getWidth(), 2) + Math.pow(_rootLayout.getHeight(), 2));

        // create the animator for this view (the start radius is zero)
        Animator circularReveal = ViewAnimationUtils.createCircularReveal(_rootLayout, cx, cy, 0, finalRadius);
        circularReveal.setDuration(350);

        // make the view visible and start the animation
        _rootLayout.setVisibility(View.VISIBLE);
        circularReveal.start();
    }

    void exitReveal() {

        // get the center for the clipping circle
        int cx = _rootLayout.getWidth() - 50;//_rootLayout.getWidth() / 2;
        int cy = 50;//_rootLayout.getHeight() / 2;

        // get the initial radius for the clipping circle
        float initialRadius =
                (float)Math.sqrt(
                        Math.pow(_rootLayout.getWidth(), 2) + Math.pow(_rootLayout.getHeight(), 2));

        // create the animation (the final radius is zero)
        Animator anim =
                ViewAnimationUtils.createCircularReveal(_rootLayout, cx, cy, initialRadius, 0);
        anim.setDuration(350);

        // make the view invisible when the animation is done
        anim.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                super.onAnimationEnd(animation);
                _rootLayout.setVisibility(View.INVISIBLE);
                VoteProductActivity.super.onBackPressed();
                overridePendingTransition(0, 0);
            }
        });

        // start the animation
        anim.start();
    }

    public void onBackClick(View v) {
        onBackPressed();
    }

    @Override
    public void onBackPressed() {
        _productsLocal.setIsNewVote(false);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            if (!_backPressed) {
                exitReveal();
                _backPressed = true;
            }
        }
        else {
            super.onBackPressed();
            overridePendingTransition(R.anim.hold, R.anim.exit_to_top_right);
        }

        ((BitwalkingApp)getApplication()).trackEvent("store.questionnaire", "close", (_gotSelectedProduct) ? "true" : "false");
    }

    private void loadVote() {
        new DownloadVoteProductsTask(VoteProductActivity.this, VoteProductActivity.this).execute();
    }

    private void destroyProgress() {
        if (null != _loadingVoteProgress)
            _loadingVoteProgress.setVisibility(View.GONE);
    }

    private void refreshVote() {
        if (null != _products) {
            destroyProgress();

//            // set selected
//            String selectedProduct = _productsLocal.getUserChoice();
//            _gotSelectedProduct = false;
//            if (null != selectedProduct) {
//                for (VoteProductDrawableInfo p : _products) {
//                    if (p.getName().contentEquals(selectedProduct)) {
//                        p.setSelected(true);
//                        _gotSelectedProduct = true;
//                    }
//                    else
//                        p.setSelected(false);
//                }
//            }

            _productsAdapter = new VoteProductListAdapter(VoteProductActivity.this, _products);
            _productsList.setAdapter(_productsAdapter);
        }
    }

    private ArrayList<VoteProductDrawableInfo> loadProductsList(GetSurveyResponse.SurveyInfo survey, String userChoice) {

        _products = new ArrayList<>();
        for (GetSurveyResponse.SurveyInfo.SurveyItem product : survey.items) {
            try {
                VoteProductDrawableInfo newItem = new VoteProductDrawableInfo();
                newItem.setId(product.itemId);
                newItem.setName(product.itemName);
                newItem.setImage(_productsLocal.getStoredImage(product.itemName));
                newItem.setSelected((null != userChoice && product.itemId.contentEquals(userChoice)));

                _products.add(newItem);
            }
            catch (Exception e) {
                Logger.instance().Log(Logger.ERROR, TAG, "failed to add itemName: " + product.itemName);
            }
        }

        return _products;
    }

    @Override
    protected void onResume() {
        super.onResume();
        ((BitwalkingApp)getApplication()).trackScreenView("store.questionnaire");

        refreshVote();
    }

    @Override
    public void onVoteLoadError() {
        VoteProductActivity.this.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Globals.showServerConnectionErrorMsg(VoteProductActivity.this);
                onBackPressed();
            }
        });
    }

    @Override
    public void onVoteReady(final GetSurveyResponse.SurveyInfo survey, final String userChoice, boolean isNewVote) {
        VoteProductActivity.this.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                _voteId = survey.id;
                loadProductsList(survey, userChoice);
                refreshVote();
            }
        });
    }

    @Override
    protected void onBwServiceConnected() {

    }

    @Override
    protected void onBwServiceDisconnected() {

    }


    @Override
    public void onSaveInstanceState(Bundle outState) {
        //first saving my state, so the bundle wont be empty.
        //https://code.google.com/p/android/issues/detail?id=19917
        outState.putString("WORKAROUND_FOR_BUG_19917_KEY", "WORKAROUND_FOR_BUG_19917_VALUE");
        super.onSaveInstanceState(outState);
    }
}
