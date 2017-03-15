package com.yusheng.designpattern.decorator.decorator;

import com.yusheng.designpattern.decorator.OffRoadVehicle;

/**
 * This class defines a stubby bumper that decorates an off-road vehicle.
 */
public class StubbyBumper extends OffRoadVehicleDecorator {

  public StubbyBumper(OffRoadVehicle offRoadVehicle) {
    super(offRoadVehicle);
  }

  @Override
  public String getSpec() {
    return super.getSpec() + ", stubby bumper mounted";
  }

  @Override
  public String testDrive() {
    return "I have a stubby bumper mounted. You can test drive me over some large rock.";
  }
}
