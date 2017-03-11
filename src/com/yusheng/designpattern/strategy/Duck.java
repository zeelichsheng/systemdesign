package com.yusheng.designpattern.strategy;

/**
 * This interface defines behavior of a duck object.
 */
public interface Duck {

  /**
   * Shows the description of the duck.
   */
  void display();

  /**
   * Makes the duck quacking.
   */
  void quack();

  /**
   * Makes the duck flying.
   */
  void fly();
}
