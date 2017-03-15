package com.yusheng.designpattern.factory;

import com.yusheng.designpattern.factory.good.Good;
import com.yusheng.designpattern.factory.good.TeddyBear;
import com.yusheng.designpattern.factory.good.ToyCar;

import java.util.ArrayList;
import java.util.List;

/**
 * This class defines a penny store located on the west.
 */
public class WestPennyStore extends PennyStore {

  @Override
  public List<Good> order() {
    List<Good> goodList = new ArrayList<>();
    goodList.add(new ToyCar(1.5));
    goodList.add(new TeddyBear(1.4));

    return goodList;
  }
}
