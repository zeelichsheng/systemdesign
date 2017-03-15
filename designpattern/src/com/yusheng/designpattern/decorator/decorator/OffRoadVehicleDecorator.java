package com.yusheng.designpattern.decorator.decorator;

import com.yusheng.designpattern.decorator.OffRoadVehicle;

/**
 * This class defines the decorator that decorates an off-road vehicle.
 */
public abstract class OffRoadVehicleDecorator implements OffRoadVehicle {

  // The off-road vehicle that this stubby bumper is mounted on.
  private OffRoadVehicle offRoadVehicle;

  protected OffRoadVehicleDecorator(OffRoadVehicle offRoadVehicle) {
    this.offRoadVehicle = offRoadVehicle;
  }

  @Override
  public String getSpec() {
    return offRoadVehicle.getSpec();
  }

  @Override
  public abstract String testDrive();
}
