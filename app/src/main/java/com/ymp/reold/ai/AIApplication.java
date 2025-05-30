package com.ymp.reold.ai;

import android.app.Application;
import android.util.Log;

import org.conscrypt.Conscrypt;

import java.security.Security;

public class AIApplication extends Application {
    //Запуск библиотеки Conscrypt для обеспечения безопасности приложения. Это необходимо для работы с сетью и безопасностью данных.
    private static final String TAG = "AIApplication";

    @Override
    public void onCreate() {
        super.onCreate();
        try {
            Security.insertProviderAt(Conscrypt.newProvider(), 1);
            Log.i(TAG, "Conscrypt security provider installed successfully.");
        } catch (NoClassDefFoundError e) {
            Log.e(TAG, "Conscrypt classes not found. Is minSdkVersion compatible? Error: " + e.getMessage());
        } catch (Exception e) {
            Log.e(TAG, "Failed to install Conscrypt security provider: " + e.getMessage());
            e.printStackTrace();
        }
    }
}