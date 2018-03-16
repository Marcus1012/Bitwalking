package com.example.stas.misfitplayground;

import android.Manifest;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.example.stas.misfitplayground.utils.FileDialog;
import com.misfit.ble.setting.SDKSetting;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Timer;
import java.util.TimerTask;

public class ShineActivity extends BaseActivity {

	public static final String TAG = "ShineActivity";

	private static final int REQUEST_SELECT_DEVICE = 1;
	private static final int REQUEST_ENABLE_BT = 2;
	private static final int REQUEST_FIRMWARE_SELECTION = 3;

	private Button mScanButton, mConnectButton, mCloseButton;
	private Button mAnimateButton, mStopAnimateButton, mConfigButton, mSyncButton, mOTAButton;
	private Button mActivateButton;
	private EditText mConfigurationEditText;

	private TextView mDeviceLabel, mShineApiInfoTextView;
	private Button mInterruptButton;
	private TextView mSDKVersion;

	/**
	 * Activity Events
	 */
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.shine_layout);

		// Request location service (from Android 6.0)
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
			if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
					!= PackageManager.PERMISSION_GRANTED) {
				ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, 0);
			}

			if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
					!= PackageManager.PERMISSION_GRANTED) {
				ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, 0);
			}
		}

		mScanButton = (Button) findViewById(R.id.btn_start_or_stop_scan);
		mScanButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				if (mState == BTLE_STATE_IDLE) {
					if (!BluetoothAdapter.getDefaultAdapter().isEnabled()) {
						Log.i(TAG, "onClick - BT not enabled yet");
						Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
						startActivityForResult(enableIntent, REQUEST_ENABLE_BT);
					} else {
						Intent newIntent = new Intent(ShineActivity.this, DeviceListActivity.class);
						startActivityForResult(newIntent, REQUEST_SELECT_DEVICE);

						mMessage = "SCANNING";
						setState(BTLE_STATE_SCANNING);
					}
				}
			}
		});

		mAnimateButton = (Button) findViewById(R.id.btn_animate);
		mAnimateButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				mMessage = "PLAY ANIMATION";
				mService.playAnimation();
				updateUi();
			}
		});

		mStopAnimateButton = (Button) findViewById(R.id.btn_stop_animate);
		mStopAnimateButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				setMessage("STOP PLAYING ANIMATION");
				mService.stopPlayingAnimation();
			}
		});

		mConnectButton = (Button) findViewById(R.id.btn_connect);
		mConnectButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				if (mState == BTLE_STATE_IDLE || mState == BTLE_STATE_CLOSED) {
					if (mService.connect(mDevice)) {
						mMessage = "CONNECTING";
						setState(BTLE_STATE_CONNECTING);
					} else {
						mMessage = "CONNECTING FAILED.\nDEVICE WAS INVALIDATED, PLEASE SCAN AGAIN.";
						updateUi();
					}
				}
			}
		});

		mConfigButton = (Button) findViewById(R.id.btn_configuration);
		mConfigButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				String paramsString = mConfigurationEditText.getText().toString();
				if (TextUtils.isEmpty(paramsString)) {
					mMessage = "GETTING CONFIGURATION";
					mService.startGettingDeviceConfiguration();
				} else {
					mMessage = "SETTING CONFIGURATION";
					mService.startSettingDeviceConfiguration(paramsString);
				}
				updateUi();
			}
		});

		mSyncButton = (Button) findViewById(R.id.btn_sync);
		mSyncButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				mMessage = "SYNCING";
				mService.startSync();
				updateUi();
			}
		});

		mOTAButton = (Button) findViewById(R.id.btn_ota);
		mOTAButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				showFileChooser();
			}
		});

		mCloseButton = (Button) findViewById(R.id.btn_close);
		mCloseButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				if (mState >= BTLE_STATE_CLOSED) {
					mService.close();
				}
			}
		});

		mConfigurationEditText = (EditText) findViewById(R.id.edit_set_configuration);

		mShineApiInfoTextView = (TextView) findViewById(R.id.textView);
		mDeviceLabel = (TextView) findViewById(R.id.deviceName);

		Timer rssiTimer = new Timer();
		rssiTimer.schedule(new TimerTask() {
			@Override
			public void run() {
				if (mState >= BTLE_STATE_CONNECTED) {
					mService.readRssi();
				}
			}
		}, 1000, 1000);

		mActivateButton = (Button) findViewById(R.id.btn_activate);
		mActivateButton.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				mMessage = "ACTIVATING";
				mService.activate();
				updateUi();
			}
		});

		mInterruptButton = (Button) findViewById(R.id.btn_interrupt);
		mInterruptButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				stopCurrentOperation();
			}
		});
		
		mSDKVersion = (TextView)findViewById(R.id.sdk_version);
		mSDKVersion.setText(SDKSetting.getSDKVersion());
	}

	/**
	 * Activity Result
	 */
	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		switch (requestCode) {
			case REQUEST_SELECT_DEVICE:
				if (resultCode == Activity.RESULT_OK && data != null) {
					mDevice = data.getParcelableExtra(MisfitShineService.EXTRA_DEVICE);
					Log.d(TAG, "... onActivityResultdevice.address==" + mDevice + "mserviceValue" + mService);
				}

				mService.stopScanning();

				mMessage = "SCANNING STOPPED";
				setState(BTLE_STATE_IDLE);
				break;
			case REQUEST_ENABLE_BT:
				// When the request to enable Bluetooth returns
				if (resultCode == Activity.RESULT_OK) {
					Toast.makeText(this, "Bluetooth has turned on ", Toast.LENGTH_SHORT).show();
				} else {
					// User did not enable Bluetooth or an error occurred
					Log.d(TAG, "BT not enabled");
					Toast.makeText(this, "Problem in BT Turning ON ", Toast.LENGTH_SHORT).show();
					finish();
				}
				break;
			case REQUEST_FIRMWARE_SELECTION:
				if (resultCode == Activity.RESULT_OK && data != null) {
					try {
						onFirmwareSelected(getPath(this, data.getData()));
					} catch (URISyntaxException e) {
						Log.e(TAG, "URISyntaxException");
					}
				}
				break;
			default:
				Log.e(TAG, "wrong request Code");
				break;
		}
	}

	@Override
	protected void setUiState() {
		super.setUiState();

		mScanButton.setEnabled(mState <= BTLE_STATE_SCANNING);
		mScanButton.setText(mState != BTLE_STATE_SCANNING ? R.string.scan : R.string.stop_scan);

		mConnectButton.setEnabled(mDevice != null && mState < BTLE_STATE_CLOSED);
		mConnectButton.setText(R.string.connect);

		mAnimateButton.setEnabled(isReady);
		mStopAnimateButton.setEnabled(isReady);
		mConfigButton.setEnabled(isReady);

		mSyncButton.setEnabled(isReady);
		mOTAButton.setEnabled(isReady);

		mActivateButton.setEnabled(isReady);
		mCloseButton.setEnabled(mState >= BTLE_STATE_CLOSED);
		mInterruptButton.setEnabled(isBusy);

		if (mDevice != null) {
			mDeviceLabel.setText(mDevice.getName() + " - " + mDevice.getSerialNumber() + " - rssi:  " + mRssi);
		} else {
			mDeviceLabel.setText("");
		}

		mShineApiInfoTextView.setText(mMessage);
	}

	private byte[] readRawResourceFile(String path) {
		byte[] bytes = null;

		try {
			FileInputStream fis = new FileInputStream(new File(path));
			ByteArrayOutputStream bos = new ByteArrayOutputStream();

			byte data[] = new byte[1024];
			int count;

			while ((count = fis.read(data)) != -1) {
				bos.write(data, 0, count);
			}

			bos.flush();
			bos.close();
			fis.close();

			bytes = bos.toByteArray();
		} catch (FileNotFoundException e) {
			Log.e(TAG, "FileNotFoundException");
		} catch (IOException ioe) {
			Log.e(TAG, "IOException");
		} catch (NullPointerException npe) {
			Log.e(TAG, "NullPointerException");
		}

		return bytes;
	}

	private void showFileChooser() {
		try {
			Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
			intent.setType("application/bin");
			startActivityForResult(intent, REQUEST_FIRMWARE_SELECTION);
		} catch (android.content.ActivityNotFoundException ex) {	// If there is no a File Explorer
			showHandmadeFileChooser();
		}
	}

	/**
	 * A handmade file chooser by myself
	 */
	private void showHandmadeFileChooser() {
		File mPath = new File(Environment.getExternalStorageDirectory().getPath());
		FileDialog fileDialog = new FileDialog(this, mPath);
		fileDialog.setFileEndsWith(".bin");
		fileDialog.addFileListener(new FileDialog.FileSelectedListener() {
			public void fileSelected(File file) {
				onFirmwareSelected(file.getPath());
			}
		});

		fileDialog.showDialog();
	}

	private static String getPath(Context context, Uri uri) throws URISyntaxException {
		String result;
		Cursor cursor = context.getContentResolver().query(uri, null, null, null, null);
		if (cursor == null) {
			result = uri.getPath();
		} else {
			cursor.moveToFirst();
			int idx = cursor.getColumnIndex(MediaStore.Files.FileColumns.DATA);
			result = cursor.getString(idx);
			cursor.close();
		}
		return result;
	}

	private void onFirmwareSelected(String path) {
		byte[] firmwareData = readRawResourceFile(path);

		if (firmwareData == null || firmwareData.length <= 0) {
			Toast.makeText(ShineActivity.this, "Invalid data", Toast.LENGTH_SHORT).show();
			return;
		}

		mMessage = "Updating to " + path.substring(path.lastIndexOf("/") + 1);
		mService.startOTAing(firmwareData);
		updateUi();
	}
}
