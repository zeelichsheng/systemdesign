package lru.model;

/**
 * This class implements a simple linked list node.
 */
public class LinkedListNode<V> {

  private V value;
  private LinkedListNode<V> prev;
  private LinkedListNode<V> next;

  public LinkedListNode(V value) {
    this.value = value;
  }

  public V getValue() {
    return value;
  }

  public void setValue(V value) {
    this.value = value;
  }

  public LinkedListNode<V> getPrev() {
    return prev;
  }

  public void setPrev(LinkedListNode<V> prev) {
    this.prev = prev;
  }

  public LinkedListNode<V> getNext() {
    return next;
  }

  public void setNext(LinkedListNode<V> next) {
    this.next = next;
  }
}
