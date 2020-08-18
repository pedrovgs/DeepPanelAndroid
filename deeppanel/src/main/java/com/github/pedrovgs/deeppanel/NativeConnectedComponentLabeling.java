package com.github.pedrovgs.deeppanel;

public class NativeConnectedComponentLabeling {


    void initialize() {
        System.loadLibrary("deep-panel");
    }

    native int[][] transformPredictionIntoLabels(float[][][] prediction);

}
