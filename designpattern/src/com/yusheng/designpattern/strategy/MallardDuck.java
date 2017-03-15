package com.yusheng.designpattern.strategy;

/**
 * This class defines the Mallard duck.
 */
public class MallardDuck extends AbstractDuck {

  @Override
  public void display() {
    System.console().printf("A mallard duck");
  }
}
