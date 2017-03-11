package com.yusheng.designpattern.observer;

/**
 * This class defines an police station that responds to an event.
 */
public class PolicyStation implements Observer {

  @Override
  public void notify(Event event) {
    System.console().printf("Policy station received event: " + event.getDescription());
    System.console().printf("Dispatching police cars...");
  }
}
