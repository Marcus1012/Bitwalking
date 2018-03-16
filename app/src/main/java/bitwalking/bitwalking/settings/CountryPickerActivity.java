package bitwalking.bitwalking.settings;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ListView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Locale;
import java.util.Map;

import bitwalking.bitwalking.R;
import bitwalking.bitwalking.registration_and_login.IsoToPhone;
import bitwalking.bitwalking.util.BWEditText;
import bitwalking.bitwalking.util.Globals;
import bitwalking.bitwalking.util.Logger;
import bitwalking.bitwalking.util.RobotoTextView;

/**
 * Created by Marcus on 1/13/16.
 */
public class CountryPickerActivity extends Activity {

    private static final String TAG = CountryPickerActivity.class.getSimpleName();

    ArrayList<CountryInfo> _countries, _filteredCountries;
    String _phoneCountry;
    ListView _countryList;
    CountryInfo selectedCountry;
    CountryAdapter _adapter;
    boolean _showCountryCode = true;
    boolean _isDark = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        _showCountryCode = getIntent().getBooleanExtra("showCountryCode", true);
        _isDark = getIntent().getBooleanExtra("dark", false);

        if (_isDark)
            setContentView(R.layout.country_picker_dark_layout);
        else
            setContentView(R.layout.country_picker_layout);

        _countryList = (ListView) findViewById(R.id.country_picker_listview);
        selectedCountry = null;
        // Load countries
        _phoneCountry = getBaseContext().getResources().getConfiguration().locale.getDisplayCountry();
        loadCountriesInfo();
        _filteredCountries = new ArrayList<>(_countries);
        _adapter = new CountryAdapter(CountryPickerActivity.this, _filteredCountries);
        _countryList.setAdapter(_adapter);
        _countryList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                selectedCountry = _filteredCountries.get(position);
                onBackPressed();
            }
        });

        ((BWEditText)findViewById(R.id.country_picker_search)).addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                search(s.toString());
            }
        });

        overridePendingTransition(R.anim.enter_from_right, R.anim.exit_to_left);
    }

    @Override
    public void startActivityForResult(Intent intent, int requestCode) {
        try {
            super.startActivityForResult(intent, requestCode);
        } catch (Exception e) {
            // fixes Google Maps bug: http://stackoverflow.com/a/20905954/2075875
        }
    }

    private void loadCountriesInfo() {
        // Load countries
        _countries = new ArrayList<>();

        Logger.instance().Log(Logger.DEBUG, TAG, "country code count = " + IsoToPhone.getAllCodes().size());

        for (Map.Entry<String, String> iso : IsoToPhone.getAllCodes().entrySet()) {
            String code = iso.getKey();
            String country = IsoToPhone.getCountryName(code);

            if (null != iso.getValue() && null != country) {
                CountryInfo countryInfo = new CountryInfo(country, code, iso.getValue());

                if (country.trim().length() > 0 && iso.getValue().trim().length() > 0 && !_countries.contains(countryInfo)) {
                    _countries.add(countryInfo);
                }
            }
            else {
                Logger.instance().Log(Logger.DEBUG, TAG, String.format("ignore [%s] [%s]", country, code));
            }
        }

        Collections.sort(_countries);

        for (int i = 0; i < _countries.size(); ++i) {
            if (_countries.get(i).getName().contentEquals(_phoneCountry)) {
                break;
            }
        }
    }

    private class CountryAdapter extends BaseAdapter {

        LayoutInflater _inflater;
        ArrayList<CountryInfo> _countries;

        CountryAdapter(Context context, ArrayList<CountryInfo> countries) {
            _inflater = LayoutInflater.from(context);
            _countries = countries;

            if (null == _countries) {
                Logger.instance().Log(Logger.ERROR, "CountryCodeAdapter", "Countries and Code are not valid");
                _countries = new ArrayList<>();
            }
        }

        @Override
        public int getCount() {
            return _countries.size();
        }

        @Override
        public Object getItem(int position) {
            return _countries.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            return getCustomView(position, convertView, parent);
        }

        @Override
        public View getDropDownView(int position, View convertView, ViewGroup parent) {
            return getCustomView(position, convertView, parent);
        }

        public View getCustomView(int position, View convertView, ViewGroup parent) {
            if (_isDark)
                convertView = _inflater.inflate(R.layout.country_phone_spinner_dark_layout, null);
            else
                convertView = _inflater.inflate(R.layout.country_phone_spinner_layout, null);

            ((RobotoTextView) convertView.findViewById(R.id.country_name)).setText(_countries.get(position).getName());
            View codeView = convertView.findViewById(R.id.country_phone_code);
            if (_showCountryCode) {
                ((RobotoTextView) codeView).setText(_countries.get(position).getCode());
                codeView.setVisibility(View.VISIBLE);
            }
            else
                codeView.setVisibility(View.INVISIBLE);
            convertView.setMinimumHeight(50);

            return convertView;
        }
    }

    private void search(String text) {
        _filteredCountries.clear();

        for (CountryInfo country : _countries) {
            if (country.getName().toLowerCase(Locale.ENGLISH).startsWith(text.toLowerCase())) {
                _filteredCountries.add(country);
            }
        }

        for (CountryInfo country : _countries) {
            if (country.getName().toLowerCase(Locale.ENGLISH).contains(text.toLowerCase())) {
                if (!_filteredCountries.contains(country))
                    _filteredCountries.add(country);
            }
        }

        _adapter.notifyDataSetChanged();
    }

    public void onCancelCountry(View v) {
        onBackPressed();
    }

    @Override
    public void onBackPressed() {
        if (null != selectedCountry) {
            Intent resultIntent = new Intent();
            resultIntent.putExtra("Country", selectedCountry.getName());
            resultIntent.putExtra("CountryIso", selectedCountry.getIso());
            resultIntent.putExtra("Code", selectedCountry.getCode());
            setResult(Activity.RESULT_OK, resultIntent);
        }
        else {
            setResult(Activity.RESULT_CANCELED, null);
        }

        Globals.hideSoftKeyboard(CountryPickerActivity.this);

        super.onBackPressed();

        overridePendingTransition(R.anim.enter_from_left, R.anim.exit_to_right);
    }
}
