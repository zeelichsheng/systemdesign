package com.yusheng.designpattern.factory.good;

/**
 * This class defines a good that can be sold in penny store.
 */
public abstract class Good {

  // The description of the good.
  private String description;

  // The price of the good.
  private double price;

  protected Good(String description,
                 double price) {
    this.description = description;
    this.price = price;
  }

  public String getDescription() {
    return description;
  }

  public double getPrice() {
    return price;
  }
}
