package com.campusride;

import javax.swing.SwingUtilities;

import com.campusride.ui.LoginFrame;
import com.campusride.ui.UiUtil;

/**
 * CampusRide - College Bus Pass & Route Allocation System.
 * Team: Eshanshuman Singh, Neeti Soni, Avin Shrivastava (AS-1, SRM).
 *
 * Starts the app. Swing is not thread-safe, so every window must be created on
 * the "Event Dispatch Thread" (EDT); SwingUtilities.invokeLater does that.
 */
public class Main {

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            UiUtil.applyTheme();
            new LoginFrame().setVisible(true);
        });
    }
}
