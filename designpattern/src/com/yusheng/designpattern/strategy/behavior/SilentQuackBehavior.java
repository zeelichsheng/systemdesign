package com.yusheng.designpattern.strategy.behavior;

/**
 * This class implements a silent quack behavior.
 */
public class SilentQuackBehavior implements QuackBehavior {

  public void perform() {
    System.console().printf("Quacking silently...");
  }
}
