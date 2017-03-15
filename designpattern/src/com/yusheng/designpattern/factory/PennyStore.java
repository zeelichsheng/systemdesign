package com.yusheng.designpattern.factory;

import com.yusheng.designpattern.factory.good.Good;

import java.util.List;

/**
 * This class defines a penny store that orders goods from factory.
 */
public abstract class PennyStore {

  private List<Good> goodList;

  /**
   * Shows the inventory of the store.
   */
  public void showInventory() {
    checkInventory();

    for (Good good: goodList) {
      System.console().printf(good.getDescription() + " price: " + good.getPrice());
    }
  }

  /**
   * Sells the inventory of the store.
   */
  public void sellInventory() {
    checkInventory();

    double profit = 0.0;
    for (Good good: goodList) {
      profit += good.getPrice();
    }

    System.console().printf("Total profit: " + profit);
    goodList = null;
  }

  /**
   * Orders good from factory.
   */
  public abstract List<Good> order();

  /**
   * Checks if inventory is empty.
   */
  private void checkInventory() {
    if (goodList == null) {
      System.console().printf("Inventory is empty. Order some!");
      goodList = order();
    }
  }
}
