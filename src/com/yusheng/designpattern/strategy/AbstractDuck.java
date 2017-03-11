package com.yusheng.designpattern.strategy;

import com.yusheng.designpattern.strategy.behavior.FlyBehavior;
import com.yusheng.designpattern.strategy.behavior.QuackBehavior;

/**
 * This class defines abstract duck behaviors.
 */
public abstract class AbstractDuck implements Duck {

  // The fly behavior that the duck object can perform.
  private FlyBehavior flyBehavior;

  // The quack behavior that the duck object can perform.
  private QuackBehavior quackBehavior;

  public abstract void display();

  @Override
  public void fly() {
    if (this.flyBehavior != null) {
      this.flyBehavior.perform();
    }
  }

  @Override
  public void quack() {
    if (this.quackBehavior != null) {
      this.quackBehavior.perform();
    }
  }

  public void setFlyBehavior(FlyBehavior flyBehavior) {
    this.flyBehavior = flyBehavior;
  }

  public void setQuackBehavior(QuackBehavior quackBehavior) {
    this.quackBehavior = quackBehavior;
  }
}
