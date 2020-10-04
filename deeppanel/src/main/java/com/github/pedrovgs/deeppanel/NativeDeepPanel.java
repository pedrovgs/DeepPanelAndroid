package com.github.pedrovgs.deeppanel;

public class NativeDeepPanel {


    void initialize() {
        System.loadLibrary("deep-panel");
    }

    native int[][] extractPanelsInfo(float[][][] prediction, float scale);

}
