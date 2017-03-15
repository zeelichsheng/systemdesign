package com.yusheng.designpattern.observer;

/**
 * This interface defines an dispatcher that dispatches an event.
 */
public interface Dispatcher {

  /**
   * Registers an observer.
   */
  void register(Observer observer);

  /**
   * Dispatches an incoming event to the registered observers.
   */
  void dispatch(Event event);
}
