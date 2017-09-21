package com.eve.copilot.dto;

public class Signature {

    public String sig;
    public String val;

    public Signature(String sig, String val) {
        this.sig = sig;
        this.val = val;
    }

    public String getSig() {
        return sig;
    }

    public void setSig(String sig) {
        this.sig = sig;
    }

    public String getVal() {
        return val;
    }

    public void setVal(String val) {
        this.val = val;
    }
}
