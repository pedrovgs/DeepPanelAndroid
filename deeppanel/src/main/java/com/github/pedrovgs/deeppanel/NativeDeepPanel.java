package com.github.pedrovgs.deeppanel;

public class NativeDeepPanel {

    void initialize() {
        System.loadLibrary("deep-panel");
    }

    native RawPanelsInfo extractPanelsInfo(
            float[][][] prediction,
            float scale,
            int original_image_width,
            int original_image_height
    );

}


