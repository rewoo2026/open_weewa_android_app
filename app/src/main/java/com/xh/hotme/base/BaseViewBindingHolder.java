package com.xh.hotme.base;

import androidx.viewbinding.ViewBinding;


public class BaseViewBindingHolder <VB extends ViewBinding> extends BaseViewHolder {
    public VB viewBind;
    public BaseViewBindingHolder(VB viewBind){
        super(viewBind.getRoot());
        this.viewBind = viewBind;
    }

    public VB getViewBind(){
        return viewBind;
    }
}
