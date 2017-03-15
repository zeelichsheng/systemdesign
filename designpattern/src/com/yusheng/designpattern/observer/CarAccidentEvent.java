package com.yusheng.designpattern.observer;

/**
 * This class defines an car accident event.
 */
public class CarAccidentEvent implements Event {

  // The location of the accident.
  private String location;

  // The number of person involved in the accident.
  private int numPersonInvolved;

  public CarAccidentEvent(String location,
                          int numPersonInvolved) {
    this.location = location;
    this.numPersonInvolved = numPersonInvolved;
  }

  @Override
  public String getDescription() {
    return "Car accident at " + this.location + ": " + this.numPersonInvolved + " involved.";
  }
}
