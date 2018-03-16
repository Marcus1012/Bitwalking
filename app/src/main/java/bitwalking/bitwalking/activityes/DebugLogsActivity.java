package bitwalking.bitwalking.activityes;

import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import bitwalking.bitwalking.R;

import static android.support.v4.content.FileProvider.getUriForFile;

/**
 * Created by Marcus on 8/21/16.
 */
public class DebugLogsActivity extends BwActivity {

    TextView _total;
    TextView _today;
    TextView _totalIgnored;
    TextView _todayIgnored;
    TextView _output;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.debug_logs_activity);
        _output = (TextView)findViewById(R.id.logs_output);
        _total = (TextView)findViewById(R.id.total_steps_log);
        _today = (TextView)findViewById(R.id.today_steps_log);
        _totalIgnored = (TextView)findViewById(R.id.total_ignored_steps_log);
        _todayIgnored = (TextView)findViewById(R.id.today_ignored_steps_log);
        overridePendingTransition(R.anim.enter_from_right, R.anim.exit_to_left);
        bindToBwService();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unbindBwService();
    }

    public void onClearLogs(View v) {
        _output.setText("");

        try {
            _serviceApi.clearLogs();
        }
        catch (Exception e){
            _output.setText(e.getLocalizedMessage());
        }
    }

    public void onRefreshLogs(View v) {
        refreshLogs();
    }

    public void onRefreshSteps(View v) {
        refreshSteps();
    }

    public void onShareLogs(View v) {
        copyLogsFileToInternal();

        File logsPath = new File(getCacheDir(), "logs");
        File newFile = new File(logsPath, "logs.txt");
        Uri contentUri = getUriForFile(getBaseContext(), "bitwalking.bitwalking.fileprovider", newFile);

        Intent sharingIntent = new Intent(android.content.Intent.ACTION_SEND);
        sharingIntent.setType("text/plain");
        sharingIntent.putExtra(android.content.Intent.EXTRA_SUBJECT, "logs");
        sharingIntent.putExtra(android.content.Intent.EXTRA_TEXT, "Logs file");
        sharingIntent.putExtra(Intent.EXTRA_STREAM, contentUri);
        startActivity(Intent.createChooser(sharingIntent, "Share your logs"));
    }

    private void refreshSteps() {
        try {
            if (null != _serviceApi) {
                String logs = _serviceApi.getSteps();
                _output.setText(logs);
                refreshStepsCount();
            }
        }
        catch (Exception e) {
            _output.setText(e.getLocalizedMessage());
        }
    }

    private void refreshStepsCount() {
        StepsCount steps = new StepsCount();
        countOutputSteps(steps);
        countTodaySteps(steps);

        _total.setText("total: " + steps.total);
        _today.setText("today: " + steps.today);
        _totalIgnored.setText("(" + steps.ignoredTotal + ")");
        _todayIgnored.setText("(" + steps.ignoredToday + ")");
    }

    private void refreshLogs() {
        try {
            if (null != _serviceApi) {
                String logs = _serviceApi.getLogs();
                _output.setText(logs);
                refreshStepsCount();
            }
        }
        catch (Exception e) {
            _output.setText(e.getLocalizedMessage());
        }
    }

    class StepsCount {
        public int total;
        public int today;
        public int ignoredTotal;
        public int ignoredToday;
    }

    private void countOutputSteps(StepsCount steps) {
        steps.total = 0;
        steps.ignoredTotal = 0;

        String allOutput = _output.getText().toString();
        String regexString = "\\[new=(\\d+)\\] \\[total=(\\d+)\\]";
        Pattern pattern = Pattern.compile(regexString);
        String regexIgnoredString = "\\[ignored=(\\d+)\\] \\[total=(\\d+)\\]";
        Pattern patternIgnored = Pattern.compile(regexIgnoredString);

        for (String line : allOutput.split("\\r?\\n")) {
            Matcher matcher = pattern.matcher(line);
            if (matcher.find()) {
                try {
                    steps.total += Integer.parseInt(matcher.group(1));
                } catch (Exception ex) {}
            }
            else {
                matcher = patternIgnored.matcher(line);
                if (matcher.find()) {
                    try {
                        steps.ignoredTotal += Integer.parseInt(matcher.group(1));
                    } catch (Exception ex) {}
                }
            }
        }

        if (0 == steps.total) { // try other way
            regexString = "\"steps\": (\\d+)";
            pattern = Pattern.compile(regexString);

            for (String line : allOutput.split("\\r?\\n")) {
                Matcher matcher = pattern.matcher(line);
                if (matcher.find()) {
                    try {
                        steps.total += Integer.parseInt(matcher.group(1));
                    } catch (Exception ex) {}
                }
            }
        }
    }

    private void countTodaySteps(StepsCount steps) {
        steps.today = 0;
        steps.ignoredToday = 0;

        String allOutput = _output.getText().toString();
        String todayDate = new SimpleDateFormat("yyyy-MM-dd").format(new Date());
        String regexString = todayDate + ".+\\[new=(\\d+)\\] \\[total=(\\d+)\\]";
        Pattern pattern = Pattern.compile(regexString);
        String regexIgnoredString = "\\[ignored=(\\d+)\\] \\[total=(\\d+)\\]";
        Pattern patternIgnored = Pattern.compile(regexIgnoredString);

        for (String line : allOutput.split("\\r?\\n")) {
            Matcher matcher = pattern.matcher(line);
            if (matcher.find()) {
                try {
                    steps.today += Integer.parseInt(matcher.group(1));
                } catch (Exception ex) {}
            }
            else {
                matcher = patternIgnored.matcher(line);
                if (matcher.find()) {
                    try {
                        steps.ignoredToday += Integer.parseInt(matcher.group(1));
                    } catch (Exception ex) {}
                }
            }
        }

        if (0 == steps.today) {
            String dateRegex = "\"start\": \"(\\d+-\\d+-\\d+)";
            Pattern datePattern = Pattern.compile(dateRegex);
            regexString = "\"steps\": (\\d+)";
            pattern = Pattern.compile(regexString);
            boolean lastDateIsToday = false;

            for (String line : allOutput.split("\\r?\\n")) {
                // Start date
                Matcher matcher = datePattern.matcher(line);
                if (matcher.find()) { // Start date found
                    String foundDate = matcher.group(1);
                    if (foundDate.equalsIgnoreCase(todayDate)) {
                        lastDateIsToday = true;
                    }
                    else {
                        lastDateIsToday = false;
                    }
                }
                else if (lastDateIsToday) {
                    matcher = pattern.matcher(line);
                    if (matcher.find()) {
                        try {
                            steps.today += Integer.parseInt(matcher.group(1));
                        } catch (Exception ex) {
                        }
                    }
                }
            }
        }
    }

    @Override
    protected void onBwServiceConnected() {
        refreshLogs();
    }

    @Override
    protected void onBwServiceDisconnected() {

    }

    private void copyLogsFileToInternal() {
        try {
            String logsText = _output.getText().toString();

            File logsPath = new File(getCacheDir(), "logs");
            if (!logsPath.exists())
                logsPath.mkdirs();
            File outFile = new File(logsPath, "logs.txt");

            OutputStream os = new FileOutputStream(outFile.getAbsolutePath());

            os.write(logsText.getBytes("UTF-8"));
            os.flush();
            os.close();

        } catch (IOException e) {
            e.printStackTrace(); // TODO: should close streams properly here
        }
    }

    private class CopyLogsTask extends AsyncTask<String, Integer, String> {

        @Override
        protected String doInBackground(String... params) {

            Uri uri = Uri.parse("content://bitwalking.bitwalking.logs_provider/logs.txt");
            InputStream is = null;
            StringBuilder result = new StringBuilder();
            try {
                is = getApplicationContext().getContentResolver().openInputStream(uri);
                BufferedReader r = new BufferedReader(new InputStreamReader(is));
                String line;
                while ((line = r.readLine()) != null) {
                    result.append(line);
                }
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                try { if (is != null) is.close(); } catch (IOException e) { }
            }

            return result.toString();
        }

        @Override
        protected void onPostExecute(String result) {
            Toast.makeText(DebugLogsActivity.this, result, Toast.LENGTH_LONG).show();
            super.onPostExecute(result);
        }
    }
}
