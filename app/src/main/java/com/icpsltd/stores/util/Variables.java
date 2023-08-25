package com.icpsltd.stores.util;

import android.content.SharedPreferences;

public class Variables {
    public static final int CARD_ABSENT = 1;
    public static boolean mIsDocPresentOnEPassport = false;

    public static SharedPreferences sharedPreferences;
    public static final String pref_name = "pref_name";
    public static final String is_card_reader_open = "is_card_reader_open";
}
