package com.miis.horusendoview.manager;

import android.util.Log;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import timber.log.Timber;

public interface SystemPropertiesUnit {
    String TAG = "SystemPropertiesUnit";
    boolean DEBUG=true;

    String PROPERTY_KEY_SERIALNUMBER="persist.horusendoview.serialnumber";


    static String getSystemProperty(String key){
        String prop = "null";
        try {
            Class clazz = null;
            clazz = Class.forName("android.os.SystemProperties");
            Method method = clazz.getDeclaredMethod("get", String.class);
            prop = (String)method.invoke(null, key);
        } catch (ClassNotFoundException e) {
            Timber.w("Exception: "+ e.getMessage());
        }catch (NoSuchMethodException e) {
            Timber.w("Exception: "+ e.getMessage());
        }catch (IllegalAccessException e) {
            Timber.w("Exception: "+ e.getMessage());
        } catch (InvocationTargetException e) {
            Timber.w("Exception: "+ e.getMessage());
        }
        if(DEBUG) Log.d(TAG, "[getSystemProperty] "+key+" : "+prop);
        return prop;
    }

    static String setSystemProperty(String strProperty, String value){
        String prop = "null";
        try {
            Class clazz = null;
            clazz = Class.forName("android.os.SystemProperties");
            Method method = clazz.getDeclaredMethod("set", new Class[] {String.class, String.class});
            prop = (String)method.invoke(clazz, new Object[] {strProperty, value});
        } catch (ClassNotFoundException e) {
            Timber.w("Exception: "+ e.getMessage());
        }catch (NoSuchMethodException e) {
            Timber.w("Exception: "+ e.getMessage());
        }catch (IllegalAccessException e) {
            Timber.w("Exception: "+ e.getMessage());
        } catch (InvocationTargetException e) {
            Timber.w("Exception: "+ e.getMessage());
        }
        if(DEBUG) Log.d(TAG, "[setSystemProperty] "+strProperty+" : "+prop);
        return prop;
    }
}
