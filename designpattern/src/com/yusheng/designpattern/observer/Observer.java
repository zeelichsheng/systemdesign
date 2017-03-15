package com.yusheng.designpattern.observer;

/**
 * This interface defines an observer that gets notified with event.
 */
public interface Observer {

  /**
   * Notifies the observer with an event.
   */
  void notify(Event event);
}
