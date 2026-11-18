package com.xh.hotme.test;

public class TextValueModelBean {
    public int type;
    public String name;
    public int value;
    public boolean isSelect;

    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public boolean isSelect() {
        return isSelect;
    }

    public void setSelect(boolean select) {
        isSelect = select;
    }

    public int getValue() {
        return value;
    }

    public void setValue(int value) {
        this.value = value;
    }

    public TextValueModelBean(int type, String name, int value) {
        this.type = type;
        this.name = name;
        this.value = value;
    }

    public TextValueModelBean(int type, String name, int value, boolean isSelect) {
        this.type = type;
        this.name = name;
        this.value = value;
        this.isSelect = isSelect;
    }
}
