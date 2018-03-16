package bitwalking.bitwalking.settings;

/**
 * Created by Marcus on 1/13/16.
 */
public class CountryInfo implements Comparable<CountryInfo> {
    String _name;
    String _code;
    String _iso;

    public CountryInfo(String name, String iso, String code) {
        _name = name;
        _code = code;
        _iso = iso;
    }

    public String getName() { return _name; }
    public String getIso() { return _iso; }
    public String getCode() { return _code; }

    @Override
    public int compareTo(CountryInfo another) {
        int result = _name.compareTo(another._name);

        return (0 == result) ? _code.compareTo(another._code) : result;
    }

    @Override
    public boolean equals(Object o) {
        return _name.contentEquals(((CountryInfo)o).getName());
    }
}
