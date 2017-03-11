package com.yusheng.designpattern.strategy.behavior;

/**
 * This class implements a slow fly behavior.
 */
public class SlowFlyBehavior implements FlyBehavior {

  public void perform() {
    System.console().printf("Flying slowly...");
  }
}
