package com.yusheng.designpattern.command;

import java.util.LinkedList;
import java.util.Queue;

/**
 * This class defines a robot that executes commands.
 */
public class Robot {

  private final Queue<Command> commandQueue;

  public Robot() {
    this.commandQueue = new LinkedList<>();
  }

  /**
   * Receives a command and executes it.
   */
  public void queueCommand(Command command) {
    synchronized (this.commandQueue) {
      this.commandQueue.offer(command);
    }
  }

  public void executeCommands() {
    synchronized (this.commandQueue) {
      for (Command command : this.commandQueue) {
        command.execute();
      }
    }
  }
}
