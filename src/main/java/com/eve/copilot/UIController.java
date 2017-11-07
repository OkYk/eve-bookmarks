package com.eve.copilot;

import java.awt.*;

public class UIController {

    private static Image H1NT = Toolkit.getDefaultToolkit().getImage(Thread.currentThread().getContextClassLoader().getResource("h1nt.png"));
    private static TrayIcon trayIcon = new TrayIcon(H1NT, "H1NT");

    private BookmarkManager bmManager;

    public UIController(BookmarkManager bmManager) {
        if (SystemTray.isSupported()) {
            this.bmManager = bmManager;
            SystemTray tray = SystemTray.getSystemTray();

            trayIcon.setImageAutoSize(true);
            trayIcon.addActionListener(e -> {
                System.out.println("In here");
            });

            final PopupMenu popup = new PopupMenu();
            CheckboxMenuItem enableFlag = new CheckboxMenuItem("Enable", true);
            Menu featuresMenu = new Menu("Features");
            CheckboxMenuItem probeResultsFeature = new CheckboxMenuItem("Probe scanner", true);
            CheckboxMenuItem signatureFeature = new CheckboxMenuItem("Class by signature", true);
            CheckboxMenuItem distanceToHubs = new CheckboxMenuItem("Distance to hubs", true);
            featuresMenu.add(probeResultsFeature);
            featuresMenu.add(signatureFeature);
            featuresMenu.add(distanceToHubs);
            MenuItem exitItem = new MenuItem("Exit");

            popup.add(enableFlag);
            popup.add(featuresMenu);
            popup.add(exitItem);

            trayIcon.setPopupMenu(popup);

            try {
                tray.add(trayIcon);
            } catch (AWTException e) {
                System.err.println("TrayIcon could not be added.");
            }

            probeResultsFeature.addActionListener(e -> bmManager.setProbeResultEnabled(probeResultsFeature.getState()));
            signatureFeature.addItemListener(e -> bmManager.setSigEnabled(signatureFeature.getState()));
            distanceToHubs.addItemListener(e -> bmManager.setHubDistanceEnabled(distanceToHubs.getState()));
            enableFlag.addItemListener(e -> bmManager.setEnabled(enableFlag.getState()));
            exitItem.addActionListener(e -> { tray.remove(trayIcon); System.exit(0);});
        }
    }

    public void message(String message){
        System.out.println("Message: "+message);
        trayIcon.displayMessage("", message, TrayIcon.MessageType.NONE);
    }

}
