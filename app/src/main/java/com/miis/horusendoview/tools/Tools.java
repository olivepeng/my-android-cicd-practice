package com.miis.horusendoview.tools;

import android.content.Context;
import android.content.res.Resources;
import android.os.storage.StorageVolume;
import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.os.LocaleListCompat;

import com.miis.horusendoview.R;

import org.xmlpull.v1.XmlPullParser;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import timber.log.Timber;

public class Tools {

    @Nullable
    public static Object getPrivateObject(@NonNull Object obj, @NonNull String name) {
        try {
            Field field = obj.getClass().getDeclaredField(name);
            field.setAccessible(true);
            return field.get(obj);
        } catch (Exception e) {
            Timber.e(e);
            return null;
        }
    }

    @NonNull
    public static LocaleListCompat getLocaleListFromXml(@NonNull Context context) {
        List<CharSequence> tagsList = new ArrayList<>();
        try {
            Resources resources = context.getResources();
            XmlPullParser xpp = resources.getXml(R.xml.locales_config);
            while (xpp.getEventType() != XmlPullParser.END_DOCUMENT) {
                if (xpp.getEventType() == XmlPullParser.START_TAG) {
                    if (xpp.getName().equals("locale")) {
                        tagsList.add(xpp.getAttributeValue(0));
                    }
                }
                xpp.next();
            }
        } catch (Exception e) {
            Timber.e(e);
        }

        return LocaleListCompat.forLanguageTags(TextUtils.join(",", tagsList));
    }

    public static boolean isSdCard(@NonNull StorageVolume storageVolume, @NonNull Context context) {
        String storageDescription = storageVolume.getDescription(context);
        if (storageDescription != null) {
            storageDescription = storageDescription.toLowerCase(Locale.getDefault());
            return storageDescription.contains("sd") && storageDescription.contains("card");
        }
        return false;
    }

    @NonNull
    public static String getDisplayName(@NonNull StorageVolume storageVolume, @NonNull Context context) {
        String volumeName = storageVolume.getMediaStoreVolumeName();
        if (isSdCard(storageVolume, context)) {
            return context.getString(R.string.sd_card) + "(" + (volumeName != null ? volumeName : "") + ")";
        } else {
            return context.getString(R.string.usb) + "(" + (volumeName != null ? volumeName : "") + ")";
        }
    }
}
