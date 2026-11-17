package com.campusride;

import javax.swing.SwingUtilities;

import com.campusride.ui.LoginFrame;
import com.campusride.ui.UiUtil;

// CampusRide - College Bus Pass & Route Allocation System
// Eshanshuman Singh, Neeti Soni, Avin Shrivastava (AS-1, SRM)
public class Main {

    public static void main(String[] args) {
        // Swing windows should be created on the Event Dispatch Thread, invokeLater does that
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                UiUtil.applyTheme();
                new LoginFrame().setVisible(true);
            }
        });
    }
}
