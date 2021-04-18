package org.delta.epen.listenner;

public interface NetWorkStateListener {

    void netWorkChange(boolean wifiNetworkState,boolean dataNetworkState);

    void netStrength(int strength);

}
