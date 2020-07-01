package com.shz.gift;


import com.shz.gift.algotest.Trigger;

public class Main {

    public static void main(String[] args)  {
        Trigger trigger = new Trigger();
        try {
            trigger.start();
        } catch (InterruptedException ignored) {

        }
    }
}
