// BWServiceListener.aidl
package bitwalking.bitwalking.remote_service;

// Declare any non-default types here with import statements

interface BWServiceListener {
    /**
     * Demonstrates some basic types that you can use as parameters
     * and return values in AIDL.
     */
//    void basicTypes(int anInt, long aLong, boolean aBoolean, float aFloat,
//            double aDouble, String aString);

       void onTodayUpdate(String today);
       void onVerifiedSteps();
       void onDeviceDetached();
       void onGoogleApiClientError();
}
