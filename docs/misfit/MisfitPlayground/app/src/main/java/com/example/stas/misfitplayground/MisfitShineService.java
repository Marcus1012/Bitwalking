package com.example.stas.misfitplayground;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.widget.Toast;

import com.misfit.ble.setting.SDKSetting;
import com.misfit.ble.shine.ActionID;
import com.misfit.ble.shine.ShineAdapter;
import com.misfit.ble.shine.ShineAdapter.ShineScanCallback;
import com.misfit.ble.shine.ShineConfiguration;
import com.misfit.ble.shine.ShineConnectionParameters;
import com.misfit.ble.shine.ShineDevice;
import com.misfit.ble.shine.ShineProfile;
import com.misfit.ble.shine.ShineProperty;
import com.misfit.ble.shine.controller.ConfigurationSession;
import com.misfit.ble.shine.result.Activity;
import com.misfit.ble.shine.result.SyncResult;
import com.misfit.ble.util.MutableBoolean;

import java.util.Hashtable;
import java.util.List;
import java.util.TimeZone;
import java.util.Timer;
import java.util.TimerTask;

public class MisfitShineService extends Service {
	private static final String TAG = MisfitShineService.class.getSimpleName();

	public static final int SHINE_SERVICE_INITIALIZED = 0;
	public static final int SHINE_SERVICE_DISCOVERED = 1;
	public static final int SHINE_SERVICE_CONNECTED = 2;
	public static final int SHINE_SERVICE_CLOSED = 3;
	public static final int SHINE_SERVICE_OPERATION_END = 4;
	public static final int SHINE_SERVICE_OTA_RESET = 5;
	public static final int SHINE_SERVICE_RSSI_READ = 6;
	public static final int SHINE_SERVICE_OTA_PROGRESS_CHANGED = 7;
	public static final int SHINE_SERVICE_STREAMING_USER_INPUT_EVENTS_RECEIVED_EVENT = 8;
	public static final int SHINE_SERVICE_BUTTON_EVENTS = 9;
	public static final int SHINE_SERVICE_MESSAGE = 10;
	public static final int SHINE_SERVICE_SCANNED_FAILED = 11;
	
	// Bundle Key
	public static final String EXTRA_DEVICE = "MisfitShineService.extra.device";
	public static final String EXTRA_RSSI = "MisfitShineService.extra.rssi";
	public static final String EXTRA_MESSAGE = "MisfitShineService.extra.message";
	public static final String EXTRA_SERIAL_STRING = "MisfitShineService.extra.serialstring";
	/**
	 * Connecting TimeOut Timer
	 */
	public static final int CONNECTING_TIMEOUT = 30000;

	/**
	 * Service's Binder
	 */
	private final IBinder binder = new LocalBinder();
	protected Handler mHandler;
	protected Handler mDeviceDiscoveringHandler;
	private ShineProfile mShineProfile;
	private ShineAdapter mShineAdapter;
	private SyncResult mSummaryResult = null;

	private Timer mConnectingTimeOutTimer = new Timer();
	private ConnectingTimedOutTimerTask mCurrentConnectingTimeOutTimerTask = null;

	@Override
	public IBinder onBind(Intent intent) {
		return binder;
	}

	public class LocalBinder extends Binder {
		public MisfitShineService getService() {
			return MisfitShineService.this;
		}
	}

	/**
	 * Set Up
	 */
	@Override
	public void onCreate() {
		super.onCreate();
		try {
			SDKSetting.setUp(this.getApplicationContext(), "user@example.com");
		} catch (IllegalArgumentException ex) {
			ex.printStackTrace();
			Toast.makeText(this, "ShineSDK setup failed: " + ex.toString(), Toast.LENGTH_LONG).show();
		}
		mShineAdapter = ShineAdapter.getDefaultAdapter(this);
	}

	@Override
	public void onDestroy() {
		if (mShineProfile != null) {
			mShineProfile.close();
		}

		super.onDestroy();
	}

	public void setHandler(final Handler handler) {
		mHandler = handler;
	}

	public void setDeviceDiscoveringHandler(final Handler handler) {
		mDeviceDiscoveringHandler = handler;
	}

	/**
	 * Callback
	 */
	private ShineScanCallback mShineScanCallback = new ShineScanCallback() {
		@Override
		public void onScanResult(ShineDevice device, int rssi) {
			onDeviceFound(device, rssi);
		}

		@Override
		public void onScanFailed(final ShineAdapter.ScanFailedErrorCode errorCode) {
			Message msg = Message.obtain(mDeviceDiscoveringHandler, SHINE_SERVICE_SCANNED_FAILED);
			msg.sendToTarget();

			mHandler.postDelayed(new Runnable() {
				@Override
				public void run() {
					onOperationCompleted(SHINE_SERVICE_SCANNED_FAILED, "ScanFailed: " + errorCode);
				}
			}, 500L);
		}
	};

	/**
	 * Public Interface - Scanning
	 */
	public boolean startScanning() {
		if (mShineAdapter == null)
			return false;

		boolean result = true;
		try {
			mShineAdapter.startScanning(mShineScanCallback);
		} catch (IllegalStateException ex) {
			ex.printStackTrace();
			result = false;
			Toast.makeText(this, "Start scanning failed: " + ex.toString(), Toast.LENGTH_LONG).show();
		}

		return result;
	}

	public void stopScanning() {
		if (mShineAdapter == null)
			return;

		mShineAdapter.stopScanning(mShineScanCallback);
	}

	public boolean getConnectedShines() {
		if (mShineAdapter == null)
			return false;

		boolean result = true;
		try {
			mShineAdapter.getConnectedShines(new ShineAdapter.ShineRetrieveCallback() {
				@Override
				public void onConnectedShinesRetrieved(List<ShineDevice> connectedShines) {
					for (ShineDevice device : connectedShines) {
						onDeviceFound(device, 0);
					}
				}
			});
		} catch (IllegalStateException ex) {
			ex.printStackTrace();
			result = false;
			Toast.makeText(this, "Get connected devices failed: " + ex.toString(), Toast.LENGTH_LONG).show();
		}

		return result;
	}

	private void onDeviceFound(ShineDevice device, int rssi) {
		Bundle mBundle = new Bundle();
		mBundle.putParcelable(MisfitShineService.EXTRA_DEVICE, device);
		String serial = device.getSerialNumber();
		mBundle.putString(MisfitShineService.EXTRA_SERIAL_STRING, serial);
		mBundle.putInt(MisfitShineService.EXTRA_RSSI, rssi);

		Message msg = Message.obtain(mDeviceDiscoveringHandler, SHINE_SERVICE_DISCOVERED);
		msg.setData(mBundle);
		msg.sendToTarget();
	}

	/**
	 * Public Interface - Operate
	 */
	public boolean connect(ShineDevice device) {
		try {
			if (mShineProfile != null) {
				mShineProfile.close();
			}

			if (device.isInvalid()) {
				Toast.makeText(this, "ShineDevice instance has become INVALID. Please scan for it again!", Toast.LENGTH_SHORT).show();
				return false;
			}

			mShineProfile = device.connectProfile(this, false, new ShineProfile.ConnectionCallback() {
				@Override
				public void onConnectionStateChanged(ShineProfile shineProfile, ShineProfile.State newState) {
					boolean isConnected = (ShineProfile.State.CONNECTED == newState);
					stopConnectionTimeOutTimer();

					if (isConnected) {
						String firmwareVersion = mShineProfile.getFirmwareVersion();
						String modelNumber = mShineProfile.getModelNumber();
						String deviceFamilyName = getDeviceFamilyName(mShineProfile.getDeviceFamily());

						Bundle mBundle = new Bundle();
						mBundle.putParcelable(MisfitShineService.EXTRA_DEVICE, mShineProfile.getDevice());
						mBundle.putString(MisfitShineService.EXTRA_MESSAGE, deviceFamilyName + " - " + firmwareVersion + " - " + modelNumber);

						Message msg = Message.obtain(mHandler, SHINE_SERVICE_CONNECTED);
						msg.setData(mBundle);
						msg.sendToTarget();
					} else {
						mShineProfile = null;

						Message msg = Message.obtain(mHandler, SHINE_SERVICE_CLOSED);
						msg.sendToTarget();
					}
				}
			});
		} catch (IllegalStateException ex) {
			ex.printStackTrace();
			Toast.makeText(this, "Attempt to connect failed: " + ex.toString(), Toast.LENGTH_LONG).show();
		}

		if (mShineProfile == null)
			return false;

		startConnectionTimeOutTimer();
		return true;
	}

	public int getDeviceFamily() {
		return mShineProfile.getDeviceFamily();
	}

	public boolean isConnected() {
		return mShineProfile != null &&
				(mShineProfile.getState() == ShineProfile.State.OTA || mShineProfile.getState() == ShineProfile.State.CONNECTED);
	}

	public boolean isBusy() {
		return isConnected() && mShineProfile.getCurrentAction() != null;
	}

	public boolean isReady() {
		return isConnected() && mShineProfile.getCurrentAction() == null;
	}

	public void startGettingDeviceConfiguration() {
		mShineProfile.getDeviceConfiguration(new ShineProfile.ConfigurationCallback() {
			@Override
			public void onConfigCompleted(ActionID actionID, ShineProfile.ActionResult resultCode, Hashtable<ShineProperty, Object> data) {
				ConfigurationSession session = (ConfigurationSession) data.get(ShineProperty.SHINE_CONFIGURATION_SESSION);

				if (resultCode == ShineProfile.ActionResult.SUCCEEDED) {
					onOperationCompleted("onGettingDeviceConfigurationSucceeded:" + buildShineConfigurationString(session));
				} else {
					onOperationCompleted("onGettingDeviceConfigurationFailed:" + buildShineConfigurationString(session));
				}
			}
		});
	}

	private class SetConfigurationSession extends ConfigurationSession {
		public SetConfigurationSession(ShineConfiguration shineConfiguration) {
			super();
			mShineConfiguration = shineConfiguration;
		}

		private void prepareSetTimeParams() {
			long timestamp = System.currentTimeMillis();
			mTimestamp = timestamp / 1000;
			mPartialSecond = (short)(timestamp - mTimestamp * 1000);
			mTimeZoneOffset = (short)(TimeZone.getDefault().getOffset(timestamp) / 1000 / 60);
		}
	}

    public void startSettingDeviceConfiguration(String paramsString) {
		ShineConfiguration shineConfiguration = new ShineConfiguration();

		if (paramsString != null) {
			String[] params = paramsString.split(",");
			if (params.length == 3) {
				shineConfiguration.mActivityPoint = Long.parseLong(params[0].trim());
				shineConfiguration.mGoalValue = Long.parseLong(params[1].trim());
				shineConfiguration.mClockState = Byte.parseByte(params[2].trim());
			} else {
				Toast.makeText(this, "Please input the following fields: [point], [goal] and [clockState]", Toast.LENGTH_SHORT).show();
				return;
			}
		}

		SetConfigurationSession configurationSession = new SetConfigurationSession(shineConfiguration);
		configurationSession.prepareSetTimeParams();

		mShineProfile.setDeviceConfiguration(configurationSession, new ShineProfile.ConfigurationCallback() {
			@Override
			public void onConfigCompleted(ActionID actionID, ShineProfile.ActionResult resultCode, Hashtable<ShineProperty, Object> data) {
				if (resultCode == ShineProfile.ActionResult.SUCCEEDED) {
					onOperationCompleted("onSettingDeviceConfigurationFailed");
				} else {
					onOperationCompleted("onSettingDeviceConfigurationSucceeded");
				}
			}
		});
	}

	public void startSync() {
		mSummaryResult = new SyncResult();
		mShineProfile.sync(new ShineProfile.SyncCallback() {
			@Override
			public void onSyncCompleted(ShineProfile.ActionResult resultCode) {
				if (resultCode == ShineProfile.ActionResult.SUCCEEDED) {
					onOperationCompleted("onSyncSucceeded:" + buildSyncResultString(mSummaryResult));
				} else {
					onOperationCompleted("onSyncFailed:" + buildSyncResultString(mSummaryResult));
				}
			}

			@Override
			public void onSyncDataRead(SyncResult syncResult, Bundle extraInfo, MutableBoolean shouldStop) {
				if (syncResult == null)
					return;

				mSummaryResult.mActivities.addAll(0, syncResult.mActivities);
			}
		});
	}

	public void startOTAing(byte[] firmwareData) {
		mShineProfile.ota(firmwareData, new ShineProfile.OTACallback() {
			@Override
			public void onOTACompleted(ShineProfile.ActionResult resultCode) {
				if (ShineProfile.ActionResult.SUCCEEDED == resultCode) {
					onOperationCompleted("OTA COMPLETED - SHINE RESET");
				} else {
					onOperationCompleted("OTA FAILED");
				}
			}

			@Override
			public void onOTAProgressChanged(float progress) {
				String message = "OTA PROGRESS: " + String.format("%.1f", progress * 100) + "%";
				onOperationCompleted(SHINE_SERVICE_OTA_PROGRESS_CHANGED, message);
			}
		});
	}

	public void readRssi() {
		if (mShineProfile != null) {
			mShineProfile.readRssi(new ShineProfile.ConfigurationCallback() {
				@Override
				public void onConfigCompleted(ActionID actionID, ShineProfile.ActionResult resultCode, Hashtable<ShineProperty, Object> data) {
					int rssi = (int) data.get(ShineProperty.RSSI);

					Bundle mBundle = new Bundle();
					mBundle.putInt(MisfitShineService.EXTRA_RSSI, rssi);

					Message msg = Message.obtain(mHandler, SHINE_SERVICE_RSSI_READ);
					msg.setData(mBundle);
					msg.sendToTarget();
				}
			});
		}
	}

	public void playAnimation() {
		mShineProfile.playAnimation(new ShineProfile.ConfigurationCallback() {
			@Override
			public void onConfigCompleted(ActionID actionID, ShineProfile.ActionResult resultCode, Hashtable<ShineProperty, Object> data) {
				if (resultCode == ShineProfile.ActionResult.SUCCEEDED) {
					onOperationCompleted("PLAY ANIMATION SUCCEEDED");
				} else {
					onOperationCompleted("PLAY ANIMATION FAILED");
				}
			}
		});
	}

	public void stopPlayingAnimation() {
		mShineProfile.stopPlayingAnimation(new ShineProfile.ConfigurationCallback() {
			@Override
			public void onConfigCompleted(ActionID actionID, ShineProfile.ActionResult resultCode, Hashtable<ShineProperty, Object> data) {
				if (resultCode == ShineProfile.ActionResult.SUCCEEDED) {
					onOperationCompleted("STOP PLAYING ANIMATION SUCCEEDED");
				} else {
					onOperationCompleted("STOP PLAYING ANIMATION FAILED");
				}
			}
		});
	}

	public void activate() {
		mShineProfile.activate(new ShineProfile.ConfigurationCallback() {
			@Override
			public void onConfigCompleted(ActionID actionID, ShineProfile.ActionResult resultCode, Hashtable<ShineProperty, Object> data) {
				if (resultCode == ShineProfile.ActionResult.SUCCEEDED) {
					onOperationCompleted("ACTIVATE SUCCEEDED");
				} else {
					onOperationCompleted("ACTIVATE FAILED");
				}
			}
		});
	}

	public void close() {
		if (mShineProfile != null) {
			mShineProfile.close();
		}
	}

	public void interrupt() {
		mShineProfile.interrupt();
	}

	/**
	 * Connection Timer
	 */
	private void onConnectingTimedOut(ConnectingTimedOutTimerTask timerTask) {
		if (timerTask == mCurrentConnectingTimeOutTimerTask) {
			mCurrentConnectingTimeOutTimerTask = null;
			close();
		}
	}

	private void startConnectionTimeOutTimer() {
		stopConnectionTimeOutTimer();

		mCurrentConnectingTimeOutTimerTask = new ConnectingTimedOutTimerTask();
		mConnectingTimeOutTimer.schedule(mCurrentConnectingTimeOutTimerTask, CONNECTING_TIMEOUT);
	}

	public void stopConnectionTimeOutTimer() {
		if (mCurrentConnectingTimeOutTimerTask != null) {
			mCurrentConnectingTimeOutTimerTask.mIsCancelled = true;
			mCurrentConnectingTimeOutTimerTask.cancel();
		}
	}

	private class ConnectingTimedOutTimerTask extends TimerTask {
		public boolean mIsCancelled = false;

		public ConnectingTimedOutTimerTask() {
			mIsCancelled = false;
		}

		@Override
		public void run() {
			if (!mIsCancelled) {
				MisfitShineService.this.onConnectingTimedOut(this);
			}
		}
	}

	/**
	 * Util
	 */
	private String buildSyncResultString(SyncResult syncResult) {
		StringBuilder stringBuilder = new StringBuilder();
		if (syncResult != null) {
			int totalPoint = 0;
			int totalSteps = 0;
			if (syncResult.mActivities != null) {
				for (Activity activity : syncResult.mActivities) {
					totalPoint += activity.mPoints;
					totalSteps += activity.mBipedalCount;
				}
			}
			stringBuilder.append("\nActivity - totalPoint:" + totalPoint + " totalSteps:" + totalSteps);
		}
		return stringBuilder.toString();
	}

	private String buildShineConfigurationString(ConfigurationSession session) {
		StringBuilder stringBuilder = new StringBuilder();

		stringBuilder.append("\nTimeStamp: " + session.mTimestamp);
		stringBuilder.append("\nPartialSecond: " + session.mPartialSecond);
		stringBuilder.append("\nTimeZoneOffset: " + session.mTimeZoneOffset);

		stringBuilder.append("\nActivityPoint: " + session.mShineConfiguration.mActivityPoint);
		stringBuilder.append("\nGoalValue: " + session.mShineConfiguration.mGoalValue);
		stringBuilder.append("\nClockState: " + session.mShineConfiguration.mClockState);
		stringBuilder.append("\nBatteryLevel: " + session.mShineConfiguration.mBatteryLevel);

		return stringBuilder.toString();
	}

	private String buildConnectionParametersString(ShineConnectionParameters connectionParameters) {
		if (connectionParameters == null) {
			return "";
		}

		StringBuilder stringBuilder = new StringBuilder();

		stringBuilder.append("\nConnection Interval: " + connectionParameters.getConnectionInterval());
		stringBuilder.append("\nConnection Latency: " + connectionParameters.getConnectionLatency());
		stringBuilder.append("\nSupervision Timeout: " + connectionParameters.getSupervisionTimeout());

		return stringBuilder.toString();
	}

	private String getDeviceFamilyName(int deviceFamily) {
		String deviceFamilyName = "Unknown";

		switch (deviceFamily) {
			case ShineProfile.DEVICE_FAMILY_SHINE:
				deviceFamilyName = "Shine";
				break;
			case ShineProfile.DEVICE_FAMILY_FLASH:
				deviceFamilyName = "Flash";
				break;
			case ShineProfile.DEVICE_FAMILY_BUTTON:
				deviceFamilyName = "Button";
				break;
			case ShineProfile.DEVICE_FAMILY_SHINE_MKII:
				deviceFamilyName = "Shine MKII";
				break;
			default:
				break;
		}
		return deviceFamilyName;
	}

	private void onOperationCompleted(int eventId, String message) {
		Bundle mBundle = new Bundle();
		mBundle.putString(EXTRA_MESSAGE, message);

		Message msg = Message.obtain(mHandler, eventId);
		msg.setData(mBundle);
		msg.sendToTarget();
	}

	private void onOperationCompleted(String message) {
		onOperationCompleted(SHINE_SERVICE_OPERATION_END, message);
	}
}
