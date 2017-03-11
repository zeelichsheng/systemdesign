package com.yusheng.designpattern.factory;

import com.yusheng.designpattern.factory.good.Candy;
import com.yusheng.designpattern.factory.good.Good;
import com.yusheng.designpattern.factory.good.TeddyBear;

import java.util.ArrayList;
import java.util.List;

/**
 * This class defines a penny store located on the east.
 */
public class EastPennyStore extends PennyStore {

  @Override
  public List<Good> order() {
    List<Good> goodList = new ArrayList<>();
    goodList.add(new TeddyBear(1.2));
    goodList.add(new Candy(1.1));

    return goodList;
  }
}
