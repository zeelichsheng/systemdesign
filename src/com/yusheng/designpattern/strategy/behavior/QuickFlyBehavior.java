package com.yusheng.designpattern.strategy.behavior;

/**
 * This class implements a quick fly behavior.
 */
public class QuickFlyBehavior implements FlyBehavior {

  public void perform() {
    System.console().printf("Flying quickly!!!");
  }
}
