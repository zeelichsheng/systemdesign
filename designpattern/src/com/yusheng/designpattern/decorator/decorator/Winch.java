package com.yusheng.designpattern.decorator.decorator;

import com.yusheng.designpattern.decorator.OffRoadVehicle;

/**
 * This class defines a winch that decorates an off-road vehicle.
 */
public class Winch extends OffRoadVehicleDecorator {

  public Winch(OffRoadVehicle offRoadVehicle) {
    super(offRoadVehicle);
  }

  @Override
  public String getSpec() {
    return super.getSpec() + ", winch installed";
  }

  @Override
  public String testDrive() {
    return "I have a winch installed. You can test drive me over some hard obstacles without worrying about " +
      "getting stuck.";
  }
}
