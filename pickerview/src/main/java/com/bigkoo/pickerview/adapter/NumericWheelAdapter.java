package com.bigkoo.pickerview.adapter;


import com.contrarywind.adapter.WheelAdapter;

/**
 * Numeric Wheel adapter.
 */
public class NumericWheelAdapter implements WheelAdapter {

    private final int minValue;
    private final int maxValue;

    private int step = 1;

    /**
     * Constructor
     *
     * @param minValue the wheel min value
     * @param maxValue the wheel max value
     */
    public NumericWheelAdapter(int minValue, int maxValue) {
        this.minValue = minValue;
        this.maxValue = maxValue;
        this.step = 1;
    }

    public NumericWheelAdapter(int minValue, int maxValue, int step) {
        this.minValue = minValue;
        this.maxValue = maxValue;
        this.step = step;
    }

    @Override
    public Object getItem(int index) {
        if (index >= 0 && index < getItemsCount()) {
            int value = minValue + index * step;
            return value;
        }
        return 0;
    }

    @Override
    public int getItemsCount() {
        return (maxValue - minValue) / step + 1;
    }

    @Override
    public int indexOf(Object o) {
        try {
            return (int) (((int) o - minValue) / step);
        } catch (Exception e) {
            return -1;
        }

    }

    @Override
    public int getStep() {
        return this.step;
    }

    private void setStep(int step) {
        this.step = step;
    }
}
