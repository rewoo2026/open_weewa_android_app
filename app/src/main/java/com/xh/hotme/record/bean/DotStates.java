package com.xh.hotme.record.bean;

import java.util.ArrayList;
import java.util.List;

public class DotStates {
    int id;
    String name;

    boolean isCustom;

    public List<DotStates> children = new ArrayList<>();

    public DotStates(int id, String name){
        this.id = id;
        this.name = name;
    }

    public DotStates(int id, String name, boolean isCustom){
        this.id = id;
        this.name = name;
        this.isCustom = isCustom;
    }

    public void addChildren(int id, String name){
        DotStates dotStates = new DotStates(id, name);
        this.children.add(dotStates);
    }

    public int getId(){
        return id;
    }
    public String getName(){
        return this.name;
    }
}
