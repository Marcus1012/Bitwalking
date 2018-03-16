// BWServiceApi.aidl
package bitwalking.bitwalking.remote_service;

import bitwalking.bitwalking.remote_service.BWServiceListener;

// Declare any non-default types here with import statements

interface BWServiceApi {
    /**
     * Demonstrates some basic types that you can use as parameters
     * and return values in AIDL.
     */

    boolean updateStepsAndDetails();

    void refreshToday();

    void dispatchLastStepsUpdate();

    void addListener(BWServiceListener listener);

    void removeListener(BWServiceListener listener);

    void updateServiceInfo(String serviceInfoJson);

    String getServiceInfo();

    void startSteps();

    void stopSteps();

    String getMisfitDeviceSerial();

    void setMisfitDevice(String deviceAddr, String deviceSerial);

    boolean setStepsSource(int source);

    int getStepsSource();

    // Debug
    void clearLogs();
    String getLogs();
    String getSteps();
}
