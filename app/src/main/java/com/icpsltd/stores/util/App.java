package com.icpsltd.stores.util;

import android.app.Application;
import android.content.Context;

import com.credenceid.biometrics.BiometricsManager;
import com.credenceid.biometrics.DeviceFamily;
import com.credenceid.biometrics.DeviceType;

public class App extends Application {

    private static App INSTANCE = null;
    public static BiometricsManager BioManager = null;
    public static Context Context = null;
    public static DeviceFamily deviceFamily = DeviceFamily.InvalidDevice;
    public static DeviceType deviceType = DeviceType.InvalidDevice;
    public static final int FACE_MATCH_SCORE_THRESHOLD = 80;
    public static final int FINGER_MATCH_SCORE_THRESHOLD = 80;

    private App() { }

    public static App getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new App();
        }
        return(INSTANCE);
    }

    public static App getPergasusApplication() {
        return (App) Context;
    }

    public void onCreate() {
        super.onCreate();
        Context = getApplicationContext();



    }

    public void onDestroy() {
        BioManager.cancelCapture();
        BioManager.closeFingerprintReader();
        BioManager.closeIrisScanner();
        BioManager.cardCloseCommand();
        BioManager.closeMRZ();
    }

}
