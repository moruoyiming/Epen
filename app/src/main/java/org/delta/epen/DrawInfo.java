package org.delta.epen;

import com.tstudy.blepenlib.data.CoordinateInfo;

public class DrawInfo extends CoordinateInfo {
    private String callbackname;
    public DrawInfo(int state, String pageAddress, int coordX, int coordY, int coordForce, int strokeNum, long timeLong, boolean isOFFLine, int offLineDataAllSize, int offLineDateCurrentSize) {
        super(state, pageAddress, coordX, coordY, coordForce, strokeNum, timeLong, isOFFLine, offLineDataAllSize, offLineDateCurrentSize);
    }

    public String getCallbackname() {
        return callbackname;
    }

    public void setCallbackname(String callbackname) {
        this.callbackname = callbackname;
    }

}
