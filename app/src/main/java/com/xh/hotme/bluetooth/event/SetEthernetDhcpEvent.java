
package com.xh.hotme.bluetooth.event;

import androidx.annotation.Keep;

@Keep
public class SetEthernetDhcpEvent {
    public final int status;
    public SetEthernetDhcpEvent(int st) {
        this.status = st;
    }
}