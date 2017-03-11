package com.yusheng.designpattern.decorator;

/**
 * This class defines a Jeep Wrangler.
 */
public class JeepWrangler implements OffRoadVehicle {

  @Override
  public String getSpec() {
    return "Jeep Wrangler";
  }

  @Override
  public String testDrive() {
    return "I am completely stock. Call me a mall crawler and test drive me on highway.";
  }
}
