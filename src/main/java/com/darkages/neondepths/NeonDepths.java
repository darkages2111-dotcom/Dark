package com.darkages.neondepths;

/** Entry point for Neon Depths. */
public final class NeonDepths {
    private NeonDepths() {}

    public static void main(String[] args) {
        javax.swing.SwingUtilities.invokeLater(DungeonApp::new);
    }
}
