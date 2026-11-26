package org.me.gcu.mboue_jonathan_s2028595.ui;

/** Implemented by MainActivity so fragments can ask for a screen change. */
public interface Navigator {
    /** Open the full-screen detail of the ViewModel's selected situation. */
    void showDetail();

    /** Switch to the map tab. */
    void showMap();
}
