package com.byteforge;

import com.byteforge.ui.MainFrame;
import com.byteforge.ui.ScreenshotTour;

import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import java.io.File;

/**
 * ByteForge - Custom PC Configurator.
 * <ul>
 *   <li>no arguments: start the Swing application</li>
 *   <li>{@code --console}: run the text demo of all patterns</li>
 *   <li>{@code --screenshots <dir>}: save GUI screenshots for the report</li>
 * </ul>
 */
public class Main {
    public static void main(String[] args) throws Exception {
        java.util.Locale.setDefault(java.util.Locale.US); // "$1,234.56" regardless of OS region
        if (args.length > 0 && args[0].equals("--console")) {
            ConsoleDemo.run();
            return;
        }
        try {
            UIManager.setLookAndFeel("javax.swing.plaf.nimbus.NimbusLookAndFeel");
        } catch (Exception ignored) {
            // fall back to the default look and feel
        }
        if (args.length > 1 && args[0].equals("--screenshots")) {
            ScreenshotTour.run(new File(args[1]));
            return;
        }
        SwingUtilities.invokeLater(() -> new MainFrame().setVisible(true));
    }
}
