package com.yusheng.designpattern.command;

/**
 * This class defines a command that instructs the robot to run.
 */
public class RunCommand implements Command {

  @Override
  public void execute() {
    System.console().printf("Run");
  }
}
