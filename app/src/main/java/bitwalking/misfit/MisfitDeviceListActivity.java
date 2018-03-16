package bitwalking.misfit;

import android.app.Activity;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothDevice;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.misfit.ble.shine.ShineDevice;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import bitwalking.bitwalking.AppPreferences;
import bitwalking.bitwalking.R;
import bitwalking.bitwalking.util.Logger;

/**
 * Created by Marcus on 5/3/16.
 */
public class MisfitDeviceListActivity extends Activity {
    public static final String TAG = MisfitDeviceListActivity.class.getSimpleName();

    private static int _state;
    private static final int BTLE_STATE_IDLE = 0;
    private static final int BTLE_STATE_SCANNING = 1;
    private static final int BTLE_STATE_CLOSED = 2;
    private static final int BTLE_STATE_CONNECTING = 3;
    private static final int BTLE_STATE_CONNECTED = 4;

    private String _selectedDeviceAddress = null;

    private List<ShineDevice> _deviceList;
    private Map<String, String> devSerialValues;

    private DeviceAdapter _deviceAdapter;
    private MisfitService _service;

    private ProgressBar _scanning;
    private ProgressDialog _progress;
    private TextView _txtMessage;

    private void bindShineService() {
        Intent bindIntent = new Intent(this, MisfitService.class);
        //todo:newserver
//        bindIntent.putExtra("userName", new AppPreferences(this).getLoggedInUser());
        startService(bindIntent);
        bindService(bindIntent, _serviceConnection, Context.BIND_AUTO_CREATE);
    }

    private ServiceConnection _serviceConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName className, IBinder rawBinder) {
            _service = ((MisfitService.LocalBinder) rawBinder).getService();
            _service.setDeviceDiscoveringHandler(_discoverHandler);
            if (_service.startScanning() == false) {
                Toast.makeText(MisfitDeviceListActivity.this, "Failed start scanning.", Toast.LENGTH_LONG).show();
                finish();
                return;
            }
            else {
                _scanning.setVisibility(View.VISIBLE);
            }

            if (_service.getConnectedShines() == false) {
                Toast.makeText(MisfitDeviceListActivity.this, "Failed retrieve connected device.", Toast.LENGTH_LONG).show();
                finish();
                return;
            }
        }

        public void onServiceDisconnected(ComponentName classname) {
            _service = null;
        }
    };

    private void setState(int state) {
        _state = state;
    }

    private void setMessage(final String message) {
        MisfitDeviceListActivity.this.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                _txtMessage.setText(message);
            }
        });
    }

    public Handler _discoverHandler = new Handler() {
        @Override
        public void handleMessage(Message message) {
            switch (message.what) {
                case MisfitService.SHINE_SERVICE_DISCOVERED:
                    Bundle data = message.getData();
                    final ShineDevice device = data.getParcelable(MisfitService.EXTRA_DEVICE);
                    final String serialString = data.getString(MisfitService.EXTRA_SERIAL_STRING);
                    final int rssi = data.getInt(MisfitService.EXTRA_RSSI);

                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            addDevice(device, serialString, rssi);
                        }
                    });
                    break;
                case MisfitService.SHINE_SERVICE_SCANNED_FAILED:
                    _scanning.setVisibility(View.INVISIBLE);
                    setState(BTLE_STATE_IDLE);
                    setMessage(message.getData().getString(MisfitService.EXTRA_MESSAGE));
                    break;
                default:
                    super.handleMessage(message);
            }
        }
    };

    public Handler _configureHandler = new Handler() {
        @Override
        public void handleMessage(Message message) {
            switch (message.what) {
                case MisfitService.SHINE_SERVICE_DISCOVERED:
                    Bundle data = message.getData();
                    final ShineDevice device = data.getParcelable(MisfitService.EXTRA_DEVICE);
                    final String serialString = data.getString(MisfitService.EXTRA_SERIAL_STRING);
                    final int rssi = data.getInt(MisfitService.EXTRA_RSSI);

                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            addDevice(device, serialString, rssi);
                        }
                    });
                    break;
                case MisfitService.SHINE_SERVICE_INITIALIZED:
                    break;
                case MisfitService.SHINE_SERVICE_CONNECTED:
                    setState(BTLE_STATE_CONNECTED);
                    setMessage(message.getData().getString(MisfitService.EXTRA_MESSAGE));
                    break;
                case MisfitService.SHINE_SERVICE_CLOSED:
                    setState(BTLE_STATE_IDLE);
                    setMessage("CLOSED");
                    break;
                case MisfitService.SHINE_SERVICE_OPERATION_END:
                    setState(BTLE_STATE_CONNECTED);
                    setMessage(message.getData().getString(MisfitService.EXTRA_MESSAGE));
                    break;
                case MisfitService.SHINE_SERVICE_OTA_RESET:
                case MisfitService.SHINE_SERVICE_OTA_PROGRESS_CHANGED:
                case MisfitService.SHINE_SERVICE_STREAMING_USER_INPUT_EVENTS_RECEIVED_EVENT:
                case MisfitService.SHINE_SERVICE_BUTTON_EVENTS:
                    setMessage(message.getData().getString(MisfitService.EXTRA_MESSAGE));
                    break;
                case MisfitService.SHINE_SERVICE_RSSI_READ:
//                    mRssi = message.getData().getInt(MisfitService.EXTRA_RSSI);
//                    updateUi();
                    break;
                case MisfitService.SHINE_SERVICE_SCANNED_FAILED:
                    _scanning.setVisibility(View.INVISIBLE);
                    setState(BTLE_STATE_IDLE);
                    setMessage(message.getData().getString(MisfitService.EXTRA_MESSAGE));
                    break;
                default:
                    super.handleMessage(message);
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        setContentView(R.layout.misfit_device_list);

        populateList();

        _scanning = (ProgressBar) findViewById(R.id.device_scanning_progress);
        _scanning.setVisibility(View.VISIBLE);
        _txtMessage = (TextView) findViewById(R.id.misfit_service_msg);

        bindShineService();
    }

    private void populateList() {
        _deviceList = new ArrayList<>();
        devSerialValues = new HashMap<>();
        _deviceAdapter = new DeviceAdapter(this, _deviceList);

        ListView newDevicesListView = (ListView) findViewById(R.id.devices_list);
        newDevicesListView.setAdapter(_deviceAdapter);
        newDevicesListView.setOnItemClickListener(_deviceClickListener);
    }

    private void addDevice(ShineDevice device, String serialString, int rssi) {
        boolean deviceFound = false;

        for (ShineDevice listDev : _deviceList) {
            if (listDev.getAddress().equals(device.getAddress())) {
                deviceFound = true;
                break;
            }
        }

        String deviceAddrr = device.getAddress();
        if (serialString != null) {
            devSerialValues.put(deviceAddrr, serialString);
            Logger.instance().Log(Logger.DEBUG, TAG, "device serial is " + serialString);
        }
        else {
            Logger.instance().Log(Logger.DEBUG, TAG, "device serial is null");
        }

        if (!deviceFound) {
            _deviceList.add(0, device);
            _deviceAdapter.notifyDataSetChanged();
        }
    }

    public void onScanStop(View v) {
        if (null != _service) {
            if (_service.startScanning()) {
                _scanning.setVisibility(View.VISIBLE);
            }
        }
    }

    void destroyProgress() {
        if (null != _progress && _progress.isShowing())
            _progress.dismiss();
    }

    public void onSaveDevice(View v) {
        if (null != _selectedDeviceAddress) {
            ShineDevice device = null;
            for (ShineDevice d : _deviceList) {
                if (_selectedDeviceAddress.contentEquals(d.getAddress())) {
                    device = d;
                    break;
                }
            }

            destroyProgress();

            _progress = new ProgressDialog(MisfitDeviceListActivity.this);
            _progress.setMessage("Configuring device ...");
            _progress.setCancelable(false);
            _progress.show();

            Bundle bundle = new Bundle();
            bundle.putParcelable(MisfitService.EXTRA_DEVICE, device);

            Intent result = new Intent();
            result.putExtras(bundle);

            setResult(Activity.RESULT_OK, result);
            _service.stopScanning();

            destroyProgress();
            finish();
        }
    }

    public void onCancel(View v) {
        onBackPressed();
    }

    @Override
    public void onBackPressed() {
        setResult(Activity.RESULT_CANCELED, null);
        _service.stopScanning();
        super.onBackPressed();
    }

    @Override
    protected void onDestroy() {
        unbindService(_serviceConnection);
        super.onDestroy();
        destroyProgress();
    }

    private AdapterView.OnItemClickListener _deviceClickListener = new AdapterView.OnItemClickListener() {
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            if (position < _deviceList.size()) {
                _selectedDeviceAddress = _deviceList.get(position).getAddress();
                _deviceAdapter.notifyDataSetChanged();
            }
        }
    };

    class DeviceAdapter extends BaseAdapter {
        Context context;
        List<ShineDevice> devices;
        LayoutInflater inflater;
        String _emptyDevice;

        public DeviceAdapter(Context context, List<ShineDevice> devices) {
            this.context = context;
            this.inflater = LayoutInflater.from(context);
            this.devices = devices;

            _emptyDevice = "bob";
        }

        @Override
        public int getCount() {
            return devices.size() + 1;
        }

        @Override
        public Object getItem(int position) {
            return (position < devices.size()) ? devices.get(position) : _emptyDevice;
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View view;

            if (convertView != null) {
                view = convertView;
            } else {
                view = inflater.inflate(R.layout.misfit_device_list_item, null);
            }

            if (position < devices.size()) {

                ShineDevice device = devices.get(position);
                String deviceAddr = device.getAddress();
                final TextView txtDeviceName = ((TextView) view.findViewById(R.id.device_name));
                final TextView txtDeviceAddress = ((TextView) view.findViewById(R.id.device_address));

                txtDeviceName.setText(String.format("%s [%s]", device.getName(), device.getSerialNumber()));
                txtDeviceAddress.setText(deviceAddr);

                if (null == _selectedDeviceAddress) {
                    ((ImageView) view.findViewById(R.id.device_selected_image)).setImageResource(
                            (device.getBondState() == BluetoothDevice.BOND_BONDED) ?
                                    R.drawable.vote_product_selected_v : R.drawable.vote_product_empty_v);
                } else {
                    ((ImageView) view.findViewById(R.id.device_selected_image)).setImageResource(
                            (_selectedDeviceAddress.contentEquals(_deviceList.get(position).getAddress())) ?
                                    R.drawable.vote_product_selected_v : R.drawable.vote_product_empty_v);
                }

                view.findViewById(R.id.device_info_layout).setVisibility(View.VISIBLE);
                view.findViewById(R.id.misfit_scanning_item_progress).setVisibility(View.GONE);
//                view.setClickable(true);
            }
            else {
                view.findViewById(R.id.device_info_layout).setVisibility(View.INVISIBLE);
                view.findViewById(R.id.misfit_scanning_item_progress).setVisibility(View.VISIBLE);
//                view.setClickable(false);
            }

            return view;
        }
    }
}
