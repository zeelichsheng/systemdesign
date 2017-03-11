package com.yusheng.designpattern.command;

/**
 * This class defines a command that instructs the robot to walk.
 */
public class WalkCommand implements Command {

  @Override
  public void execute() {
    System.console().printf("Walk");
  }
}
