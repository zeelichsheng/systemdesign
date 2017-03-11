package com.yusheng.designpattern.command;

/**
 * This class defines a human that tests a robot.
 */
public class Human {

  private Robot robot;

  public Human(Robot robot) {
    this.robot = robot;
  }

  public void giveRunCommand() {
    this.robot.queueCommand(new RunCommand());
  }

  public void giveWalkCommand() {
    this.robot.queueCommand(new WalkCommand());
  }

  public void testRobot() {
    this.robot.executeCommands();
  }
}
