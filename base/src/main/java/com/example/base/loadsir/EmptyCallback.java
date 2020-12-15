package com.example.base.loadsir;

import com.example.base.R;
import com.kingja.loadsir.callback.Callback;

public class EmptyCallback extends Callback {

    @Override
    protected int onCreateView() {
        return R.layout.layout_empty;
    }

}
