package com.xh.hotme.utils;

import java.util.List;

public class ListUtils {

    public static <T> boolean isValid(List<T> list) {
        return list != null && !list.isEmpty();
    }

    public static <T> void mergeList(List<T> oldList, List<T> newList) {
        if(!isValid(newList))
            return;
        for(int i = 0; i < newList.size(); i++) {
            if(oldList != null && !oldList.contains(newList.get(i))) {
                oldList.add(newList.get(i));
            }
        }
    }

    /**
     * 是否是有效的 index
     *
     * @param list
     * @param index
     * @return
     */
    public static boolean isValidIndex(List list, int index) {
        if(isValid(list)) {
            return index >= 0 && index < list.size();
        }
        return false;
    }

    public static <T> int getListSize(List<T> list) {
        return list == null ? 0 : list.size();
    }
}
