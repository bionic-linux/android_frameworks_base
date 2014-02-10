/*
 * Copyright (C) 2008 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.systemui.statusbar.policy;

import com.android.systemui.R;

class TelephonyIcons {
    public static final int Type_1X = 0;
    public static final int Type_3G = 1;
    public static final int Type_4G = 2;
    public static final int Type_E = 3;
    public static final int Type_G = 4;
    public static final int Type_H = 5;
    public static final int Type_H_PLUS = 6;


    //***** Signal strength icons
    public static int[] getTelephonySignalStrengthIconList(int simColorId) {
        return TELEPHONY_SIGNAL_STRENGTH[simColorId];
    }

    //GSM/UMTS
    static final int[][] TELEPHONY_SIGNAL_STRENGTH = {
        { R.drawable.stat_sys_sim_signal_0,
            R.drawable.stat_sys_sim_signal_1_blue,
            R.drawable.stat_sys_sim_signal_2_blue,
            R.drawable.stat_sys_sim_signal_3_blue,
            R.drawable.stat_sys_sim_signal_4_blue },
          { R.drawable.stat_sys_sim_signal_0,
            R.drawable.stat_sys_sim_signal_1_orange,
            R.drawable.stat_sys_sim_signal_2_orange,
            R.drawable.stat_sys_sim_signal_3_orange,
            R.drawable.stat_sys_sim_signal_4_orange },
          { R.drawable.stat_sys_sim_signal_0,
            R.drawable.stat_sys_sim_signal_1_green,
            R.drawable.stat_sys_sim_signal_2_green,
            R.drawable.stat_sys_sim_signal_3_green,
            R.drawable.stat_sys_sim_signal_4_green },
          { R.drawable.stat_sys_sim_signal_0,
            R.drawable.stat_sys_sim_signal_1_purple,
            R.drawable.stat_sys_sim_signal_2_purple,
            R.drawable.stat_sys_sim_signal_3_purple,
            R.drawable.stat_sys_sim_signal_4_purple }
    };

    static final int[][] QS_TELEPHONY_SIGNAL_STRENGTH = {
        { R.drawable.ic_qs_signal_0,
          R.drawable.ic_qs_signal_1,
          R.drawable.ic_qs_signal_2,
          R.drawable.ic_qs_signal_3,
          R.drawable.ic_qs_signal_4 },
        { R.drawable.ic_qs_signal_full_0,
          R.drawable.ic_qs_signal_full_1,
          R.drawable.ic_qs_signal_full_2,
          R.drawable.ic_qs_signal_full_3,
          R.drawable.ic_qs_signal_full_4 }
    };

    static final int[][] TELEPHONY_SIGNAL_STRENGTH_ROAMING = {
        { R.drawable.stat_sys_sim_signal_0,
            R.drawable.stat_sys_sim_signal_1_blue,
            R.drawable.stat_sys_sim_signal_2_blue,
            R.drawable.stat_sys_sim_signal_3_blue,
            R.drawable.stat_sys_sim_signal_4_blue },
          { R.drawable.stat_sys_sim_signal_0,
            R.drawable.stat_sys_sim_signal_1_orange,
            R.drawable.stat_sys_sim_signal_2_orange,
            R.drawable.stat_sys_sim_signal_3_orange,
            R.drawable.stat_sys_sim_signal_4_orange },
          { R.drawable.stat_sys_sim_signal_0,
            R.drawable.stat_sys_sim_signal_1_green,
            R.drawable.stat_sys_sim_signal_2_green,
            R.drawable.stat_sys_sim_signal_3_green,
            R.drawable.stat_sys_sim_signal_4_green },
          { R.drawable.stat_sys_sim_signal_0,
            R.drawable.stat_sys_sim_signal_1_purple,
            R.drawable.stat_sys_sim_signal_2_purple,
            R.drawable.stat_sys_sim_signal_3_purple,
            R.drawable.stat_sys_sim_signal_4_purple }
    };

    static final int[] QS_DATA_R = {
        R.drawable.ic_qs_signal_r,
        R.drawable.ic_qs_signal_full_r
    };

    static final int[][] DATA_SIGNAL_STRENGTH = TELEPHONY_SIGNAL_STRENGTH;

    //***** Data connection icons

    //GSM/UMTS
    static final int[] DATA_G = {
        R.drawable.stat_sys_sim_data_connected_g_blue,
        R.drawable.stat_sys_sim_data_connected_g_orange,
        R.drawable.stat_sys_sim_data_connected_g_green,
        R.drawable.stat_sys_sim_data_connected_g_purple,
        R.drawable.stat_sys_sim_data_connected_g_blue
    };

    static final int[] QS_DATA_G = {
        R.drawable.ic_qs_signal_g,
        R.drawable.ic_qs_signal_full_g
    };

    static final int[] DATA_3G = {
        R.drawable.stat_sys_sim_data_connected_3g_blue,
        R.drawable.stat_sys_sim_data_connected_3g_orange,
        R.drawable.stat_sys_sim_data_connected_3g_green,
        R.drawable.stat_sys_sim_data_connected_3g_purple,
        R.drawable.stat_sys_sim_data_connected_3g_blue
    };

    static final int[] QS_DATA_3G = {
        R.drawable.ic_qs_signal_3g,
        R.drawable.ic_qs_signal_full_3g
    };

    static final int[] DATA_E = {
        R.drawable.stat_sys_sim_data_connected_e_blue,
        R.drawable.stat_sys_sim_data_connected_e_orange,
        R.drawable.stat_sys_sim_data_connected_e_green,
        R.drawable.stat_sys_sim_data_connected_e_purple,
        R.drawable.stat_sys_sim_data_connected_e_blue
    };
    static final int[] QS_DATA_E = {
        R.drawable.ic_qs_signal_e,
        R.drawable.ic_qs_signal_full_e
    };

    //3.5G    
    static final int[] DATA_H = {
        R.drawable.stat_sys_sim_data_connected_h_blue,
        R.drawable.stat_sys_sim_data_connected_h_orange,
        R.drawable.stat_sys_sim_data_connected_h_green,
        R.drawable.stat_sys_sim_data_connected_h_purple,
        R.drawable.stat_sys_sim_data_connected_h_blue
    };

    //3.5G
    static final int[] DATA_H_PLUS = {
        R.drawable.stat_sys_sim_data_connected_h_plus_blue,
        R.drawable.stat_sys_sim_data_connected_h_plus_orange,
        R.drawable.stat_sys_sim_data_connected_h_plus_green,
        R.drawable.stat_sys_sim_data_connected_h_plus_purple,
        R.drawable.stat_sys_sim_data_connected_h_plus_blue
    };

    static final int[] QS_DATA_H = {
                R.drawable.ic_qs_signal_h,
                R.drawable.ic_qs_signal_full_h
    };

    //CDMA
    // Use 3G icons for EVDO data and 1x icons for 1XRTT data
    static final int[] DATA_1X = {
        R.drawable.stat_sys_sim_data_connected_1x_blue,
        R.drawable.stat_sys_sim_data_connected_1x_orange,
        R.drawable.stat_sys_sim_data_connected_1x_green,
        R.drawable.stat_sys_sim_data_connected_1x_purple,
        R.drawable.stat_sys_sim_data_connected_1x_blue
    };

    static final int[] QS_DATA_1X = {
        R.drawable.ic_qs_signal_1x,
        R.drawable.ic_qs_signal_full_1x
    };

    // LTE and eHRPD
    static final int[] DATA_4G = {
        R.drawable.stat_sys_sim_data_connected_4g_blue,
        R.drawable.stat_sys_sim_data_connected_4g_orange,
        R.drawable.stat_sys_sim_data_connected_4g_green,
        R.drawable.stat_sys_sim_data_connected_4g_purple,
        R.drawable.stat_sys_sim_data_connected_4g_blue
    };

    static final int[] QS_DATA_4G = {
        R.drawable.ic_qs_signal_4g,
        R.drawable.ic_qs_signal_full_4g
    };

    // LTE branded "LTE"
    static final int[][] DATA_LTE = {
            { R.drawable.stat_sys_data_fully_connected_lte,
                    R.drawable.stat_sys_data_fully_connected_lte,
                    R.drawable.stat_sys_data_fully_connected_lte,
                    R.drawable.stat_sys_data_fully_connected_lte },
            { R.drawable.stat_sys_data_fully_connected_lte,
                    R.drawable.stat_sys_data_fully_connected_lte,
                    R.drawable.stat_sys_data_fully_connected_lte,
                    R.drawable.stat_sys_data_fully_connected_lte }
    };

    static final int[] QS_DATA_LTE = {
        R.drawable.ic_qs_signal_lte,
        R.drawable.ic_qs_signal_full_lte
    };

    static final int[][] DATA = {
        DATA_1X,
        DATA_3G,
        DATA_4G,
        DATA_E,
        DATA_G,
        DATA_H,
        DATA_H_PLUS,
    };

    static final int[] DATA_1X_ROAM = {
        R.drawable.stat_sys_sim_data_connected_1x_blue_roam,
        R.drawable.stat_sys_sim_data_connected_1x_orange_roam,
        R.drawable.stat_sys_sim_data_connected_1x_green_roam,
        R.drawable.stat_sys_sim_data_connected_1x_purple_roam,
        R.drawable.stat_sys_sim_data_connected_1x_blue_roam
    };
    
    static final int[] DATA_3G_ROAM = {
        R.drawable.stat_sys_sim_data_connected_3g_blue_roam,
        R.drawable.stat_sys_sim_data_connected_3g_orange_roam,
        R.drawable.stat_sys_sim_data_connected_3g_green_roam,
        R.drawable.stat_sys_sim_data_connected_3g_purple_roam,
        R.drawable.stat_sys_sim_data_connected_3g_blue_roam
    };
    
    static final int[] DATA_4G_ROAM = {
        R.drawable.stat_sys_sim_data_connected_4g_blue_roam,
        R.drawable.stat_sys_sim_data_connected_4g_orange_roam,
        R.drawable.stat_sys_sim_data_connected_4g_green_roam,
        R.drawable.stat_sys_sim_data_connected_4g_purple_roam,
        R.drawable.stat_sys_sim_data_connected_4g_blue_roam
    };
    
    static final int[] DATA_E_ROAM = {
        R.drawable.stat_sys_sim_data_connected_e_blue_roam,
        R.drawable.stat_sys_sim_data_connected_e_orange_roam,
        R.drawable.stat_sys_sim_data_connected_e_green_roam,
        R.drawable.stat_sys_sim_data_connected_e_purple_roam,
        R.drawable.stat_sys_sim_data_connected_e_blue_roam
    };
    
    static final int[] DATA_G_ROAM = {
        R.drawable.stat_sys_sim_data_connected_g_blue_roam,
        R.drawable.stat_sys_sim_data_connected_g_orange_roam,
        R.drawable.stat_sys_sim_data_connected_g_green_roam,
        R.drawable.stat_sys_sim_data_connected_g_purple_roam,
        R.drawable.stat_sys_sim_data_connected_g_blue_roam
    };
    
    static final int[] DATA_H_ROAM = {
        R.drawable.stat_sys_sim_data_connected_h_blue_roam,
        R.drawable.stat_sys_sim_data_connected_h_orange_roam,
        R.drawable.stat_sys_sim_data_connected_h_green_roam,
        R.drawable.stat_sys_sim_data_connected_h_purple_roam,
        R.drawable.stat_sys_sim_data_connected_h_blue_roam
    };

    static final int[] DATA_H_PLUS_ROAM = {
        R.drawable.stat_sys_sim_data_connected_h_plus_blue_roam,
        R.drawable.stat_sys_sim_data_connected_h_plus_orange_roam,
        R.drawable.stat_sys_sim_data_connected_h_plus_green_roam,
        R.drawable.stat_sys_sim_data_connected_h_plus_purple_roam,
        R.drawable.stat_sys_sim_data_connected_h_plus_blue_roam
    };

    static final int[][] DATA_ROAM = {
        DATA_1X_ROAM,
        DATA_3G_ROAM,
        DATA_4G_ROAM,
        DATA_E_ROAM,
        DATA_G_ROAM,
        DATA_H_ROAM,
        DATA_H_PLUS_ROAM,
    };

    static final int[] ROAMING = {
        R.drawable.stat_sys_sim_data_connected_roam_blue,
        R.drawable.stat_sys_sim_data_connected_roam_orange,
        R.drawable.stat_sys_sim_data_connected_roam_green,
        R.drawable.stat_sys_sim_data_connected_roam_purple,
        R.drawable.stat_sys_sim_data_connected_roam_blue
    };

    public static int getDataTypeId(int typeId, int simColorId, boolean roaming) {
        if (roaming) {
            int [] iconList = DATA_ROAM[typeId];
            return iconList[simColorId];
        } else {
            int [] iconList = DATA[typeId];
            return iconList[simColorId];
        }
    }
}

