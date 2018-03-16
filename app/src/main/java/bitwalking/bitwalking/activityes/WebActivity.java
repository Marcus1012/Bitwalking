package bitwalking.bitwalking.activityes;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.MailTo;
import android.os.Bundle;
import android.view.View;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import bitwalking.bitwalking.R;

/**
 * Created by Marcus on 9/8/16.
 */
public class WebActivity extends Activity {

    WebView webView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.faq_layout);
        webView = (WebView) findViewById(R.id.web_view);
        webView.getSettings().setJavaScriptEnabled(true);
        webView.getSettings().setUserAgentString("Mozilla/5.0 (Linux; U; Android 2.2; en-us; Nexus One Build/FRF91) AppleWebKit/533.1 (KHTML, like Gecko) Version/4.0 Mobile Safari/533.1");
        webView.setWebViewClient(new WebViewClient() {
            public void onPageFinished(WebView view, String url) {
                findViewById(R.id.web_loading).setVisibility(View.GONE);
            }

            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                if (url.startsWith("mailto:")) {
                    MailTo mt = MailTo.parse(url);
                    Intent i = newEmailIntent(WebActivity.this, mt.getTo(), mt.getSubject(), mt.getBody(), mt.getCc());
                    WebActivity.this.startActivity(i);
                    view.reload();

                    return true;
                } else {
                    view.loadUrl(url);
                }
                return true;
            }
        });

        findViewById(R.id.web_loading).setVisibility(View.VISIBLE);
        String url = getIntent().getStringExtra("url");
        openBrowser(url);

        overridePendingTransition(R.anim.enter_from_right, R.anim.exit_to_left);
    }

    private void openBrowser(String url) {
        webView.getSettings().setJavaScriptEnabled(true);
        if(!url.trim().startsWith("http://") && !url.trim().startsWith("https://")){
            url="http://" + url.trim();
        }

        webView.loadUrl(url.trim());
    }

    @Override
    public void onBackPressed() {
        if(webView.canGoBack()) {
            webView.goBack();
        } else {
            super.onBackPressed();
            overridePendingTransition(R.anim.enter_from_left, R.anim.exit_to_right);
        }
    }

    private Intent newEmailIntent(Context context, String address, String subject, String body, String cc) {
        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.putExtra(Intent.EXTRA_EMAIL, new String[] { address });
        intent.putExtra(Intent.EXTRA_TEXT, body);
        intent.putExtra(Intent.EXTRA_SUBJECT, subject);
        intent.putExtra(Intent.EXTRA_CC, cc);
        intent.setType("message/rfc822");
        return intent;
    }
}
