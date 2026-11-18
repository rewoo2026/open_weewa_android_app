package com.xh.hotme.bean;

import androidx.annotation.Keep;

import java.io.Serializable;
import java.util.List;

@Keep
public class PostProc implements Serializable {
    //wait表示还没处理, ongoing是正在处理, done 完成的, error处理异常
    public List<String> wait;
    public List<String> ongoing;
    public List<String> done;
    public List<String> error;
}
