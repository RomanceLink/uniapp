package com.example.uniplugin_electronic;

public interface ScalePresenterCallback {
    void getData(int net, int tare, boolean isStable);

    void isScaleCanUse(boolean isCan);

}
