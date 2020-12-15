package com.hero.basiclib.loadsir;

import com.hero.basiclib.R;
import com.kingja.loadsir.callback.Callback;

public class EmptyCallback extends Callback {

    @Override
    protected int onCreateView() {
        return R.layout.layout_empty;
    }

}
