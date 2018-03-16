package bitwalking.bitwalking.steps;

import android.content.Context;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;

import bitwalking.bitwalking.BitwalkingApp;
import bitwalking.bitwalking.util.AESEncryption;
import bitwalking.bitwalking.ServicePreferences;
import bitwalking.bitwalking.steps.steps_info.StepsBulk;
import bitwalking.bitwalking.util.Globals;
import bitwalking.bitwalking.util.Logger;

/**
 * Created by Marcus on 11/5/15.
 */
public enum LogSteps {
    INSTANCE;

    private static final String TAG                     = LogSteps.class.getSimpleName();
    private static final String _LOG_FILENAME           = "steps_log";
    private static final String _LOGS_FOLDER            = "steps_log";
    private static final String _LOG_PENDING_FILENAME   = "steps_log_pending";
    private static final int    _MAX_LOG_FILE_SIZE      = 10 * 1024; // 10K

    private byte[] getKey(Context context) {
        return Arrays.copyOf(new ServicePreferences(context).getStepsLogKey(), 32);
    }

    public synchronized void addLog(Context context, ArrayList<StepsBulk> steps) {
        ServicePreferences servicePrefs = new ServicePreferences(context);

        try {
            File mainLogs = getLogsFile(context);
            if (!mainLogs.exists()) {
                mainLogs.createNewFile();
                Logger.instance().Log(Logger.VERB, TAG, "create new log file");
            }

            if (Globals.LOG_TO_FILE) {
                int totalSteps = 0;
                for (StepsBulk bulk : steps)
                    totalSteps += bulk.getTotalSteps();
                servicePrefs.addLog(String.format("log steps: %d bulks = %d steps", steps.size(), totalSteps));
            }

            if (0 == steps.size())
                // No need to write zero steps
                return;

            byte[] key = getKey(context);

            FileWriter fw = new FileWriter(mainLogs, true);

            for (StepsBulk s : steps) {
                if (null != s.getLocation() || s.getTotalSteps() > 0) {

                    String stepsLogString = s.toJsonString();
                    Logger.instance().Log(Logger.DEBUG, TAG, "stepsLogString = " + stepsLogString);
                    String logEncrypted = AESEncryption.wrap(stepsLogString, key);

                    fw.append(logEncrypted + "\n");
                }
            }

            fw.flush();
            fw.close();

        }
        catch (Exception e) {
            e.printStackTrace();
            BitwalkingApp.getInstance().trackException("failed to add steps log", e);

            if (Globals.LOG_TO_FILE) {
                servicePrefs.addLog(String.format("log steps: !!! failed adding steps !!! : %s", e.getMessage()));
            }
        }
    }

    public synchronized ArrayList<StepsBulk> getNextStepsLog(Context context) {
        File pendingLogs = getPendingLogsFile(context);
        ServicePreferences servicePrefs = new ServicePreferences(context);

        if (!pendingLogs.exists()) { // There is no pending logs
            File logsFile = getLogsFile(context);
            long logsFileSize = logsFile.length();

            if (logsFileSize <= _MAX_LOG_FILE_SIZE) { // Pending logs can be sent as is
                logsFile.renameTo(pendingLogs);
                logsFile = getLogsFile(context);
                Logger.instance().Log(Logger.VERB, TAG, "rename log file into pending");
                Logger.instance().Log(Logger.VERB, TAG, "logsFile = " + logsFile.getName());
                Logger.instance().Log(Logger.VERB, TAG, "pendingLogs = " + pendingLogs.getName());
            }
            else {
                Logger.instance().Log(Logger.VERB, TAG, "logsFile size is above max, split: " + logsFileSize);
                // copy up to _MAX_LOG_FILE_SIZE and leave the rest for next time
                try {
                    moveFromFileToFile(context, pendingLogs, logsFile, _MAX_LOG_FILE_SIZE);
                }
                catch (Exception e) {
                    BitwalkingApp.getInstance().trackException(e);
                }
            }
        }
        else {
            servicePrefs.addLog("no need renaming, use pending");
        }

        String entireFile = readFileContent(pendingLogs);
        ArrayList<StepsBulk> bulks = new ArrayList<>();
        if (null == entireFile)
            return bulks;

        byte[] key = getKey(context);

        for (String line : entireFile.split("\\\\n")) {
            try {
                if (!line.isEmpty()) {
                    String logDecrypted = AESEncryption.unwrap(line, key);
                    StepsBulk bulk = StepsBulk.fromJsonString(logDecrypted);
                    if (null != bulk)
                        bulks.add(bulk);
                }
            }
            catch (Exception e) {
                addDebugLog(context, "Error - failed to decrypt log steps line");
                BitwalkingApp.getInstance().trackException("failed to decrypt log steps line", e);
            }
        }

        return bulks;
    }

    private synchronized void moveFromFileToFile(Context context, File dst, File src, int maxBytes) throws IOException {
        int bytesMoved = 0;
        if (!dst.exists())
            dst.createNewFile();

        if (src.exists()) {
            //Read text from file
            StringBuilder text = new StringBuilder();

            File tmpFile = new File(getLogFolder(context).getPath() + "/tmp_file");
            if (tmpFile.exists())
                tmpFile.delete();

            tmpFile.createNewFile();

            try {
                BufferedReader br = new BufferedReader(new FileReader(src));
                String line;

                // Copy up to maxBytes
                FileWriter fw = new FileWriter(dst);
                do {
                    line = br.readLine();
                    if (null != line) {
                        fw.write(line);
                        fw.append('\n');
                        bytesMoved += line.length();
                    }
                } while (line != null && bytesMoved < maxBytes);

                fw.close();

                // Copy the rest
                fw = new FileWriter(tmpFile);
                while ((line = br.readLine()) != null) {
                    fw.append(line);
                    fw.append("\n");
                }

                fw.close();
                br.close();

                // Delete old src
                src.delete();
                // Rename tmp to src
                tmpFile.renameTo(src);

                addDebugLog(context, String.format("_logFilePending size = %d, _logFile size = %d",
                        getPendingLogsFile(context).length(), getLogsFile(context).length()));
            }
            catch (IOException e) {
                //You'll need to add proper error handling here
                e.printStackTrace();
            }
        }
    }

    private String readFileContent(File f) {
        String info = null;
        if (f.exists()) {
            //Read text from file
            StringBuilder text = new StringBuilder();

            try {
                BufferedReader br = new BufferedReader(new FileReader(f));
                String line;

                while ((line = br.readLine()) != null) {
                    text.append(line);
                    text.append("\\n");
                }

                br.close();
                info = text.toString();
            }
            catch (IOException e) {
                //You'll need to add proper error handling here
                e.printStackTrace();
            }
        }

        return info;
    }

    public synchronized void clearPendingSteps(Context context) {
        File pendingLogs = getPendingLogsFile(context);
        if (pendingLogs.exists())
            pendingLogs.delete();
        Logger.instance().Log(Logger.VERB, TAG, "clear pending");
    }

    public synchronized void clearAllLogs(Context context) {
        // Main
        File mainLogs = getLogsFile(context);
        if (mainLogs.exists())
            mainLogs.delete();
        // Pending
        File pendingLogs = getPendingLogsFile(context);
        if (pendingLogs.exists())
            pendingLogs.delete();
    }

    private File getLogFolder(Context context) {
        File myDir = new File(context.getFilesDir(), _LOGS_FOLDER);
        if (!myDir.exists())
            myDir.mkdirs();

        return myDir;
    }

    private File getLogsFile(Context context) {
        return new File(getLogFolder(context).getPath() + "/" + _LOG_FILENAME);
    }

    private File getPendingLogsFile(Context context) {
        return new File(getLogFolder(context).getPath() + "/" + _LOG_PENDING_FILENAME);
    }

    private void addDebugLog(Context context, String log) {
        if (Globals.LOG_TO_FILE) {
            new ServicePreferences(context).addLog(log);
        }

        Logger.instance().Log(Logger.DEBUG, TAG, log);
    }
}
