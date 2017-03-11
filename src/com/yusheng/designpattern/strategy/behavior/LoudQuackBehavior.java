package com.yusheng.designpattern.strategy.behavior;

/**
 * This class implements a loud quack behavior.
 */
public class LoudQuackBehavior implements QuackBehavior {

  public void perform() {
    System.console().printf("Quacking loudly!!!");
  }
}
