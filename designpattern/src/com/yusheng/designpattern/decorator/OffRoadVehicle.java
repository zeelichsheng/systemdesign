package com.yusheng.designpattern.decorator;

/**
 * This interface defines an off-road vehicle.
 */
public interface OffRoadVehicle {

  /**
   * Gets the specification of the vehicle.
   */
  String getSpec();

  /**
   * Test drive the vehicle.
   */
  String testDrive();
}
