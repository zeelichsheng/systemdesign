package com.yusheng.designpattern.observer;

import java.util.ArrayList;
import java.util.List;

/**
 * This class defines an emergency center that dispatches emergency events to
 * registered observers.
 */
public class EmergencyCenter implements Dispatcher {

  // This is a list of observers that get notified when an emergency event happens.
  private List<Observer> observerList;

  public EmergencyCenter() {
    this.observerList = new ArrayList<Observer>();
  }

  @Override
  public void register(Observer observer) {
    this.observerList.add(observer);
  }

  @Override
  public void dispatch(Event event) {
    this.observerList.forEach(
        observer -> observer.notify(event)
    );
  }
}
