package com.yusheng.designpattern.observer;

/**
 * This class defines an fire station that responds to an event.
 */
public class FireStation implements Observer {

  @Override
  public void notify(Event event) {
    System.console().printf("Fire station received event: " + event.getDescription());
    System.console().printf("Dispatching fire trucks...");
  }
}
