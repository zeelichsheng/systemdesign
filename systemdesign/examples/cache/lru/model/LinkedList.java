package lru.model;

/**
 * This class implements a simple linked list.
 */
public class LinkedList<V> {

  private int capacity;
  private int size;
  private LinkedListNode<V> head;
  private LinkedListNode<V> tail;

  public LinkedList(int capacity) {
    this.capacity = capacity;
    this.size = 0;
    this.head = null;
    this.tail = null;
  }

  /**
   * Adds a node to the head of the list.
   */
  public void add(LinkedListNode<V> node) {
    if (node == null) {
      return;
    }

    // If we have reached the capacity of the list,
    // remove the least visited node.
    if (size == capacity) {
      remove(tail);
    }

    node.setNext(head);
    head = node;
    ++size;
  }

  /**
   * Removes the node from the list.
   */
  public void remove(LinkedListNode<V> node) {
    if (node == null) {
      return;
    }

    LinkedListNode<V> prevNode = node.getPrev();
    LinkedListNode<V> nextNode = node.getNext();

    if (prevNode != null) {
      prevNode.setNext(nextNode);
    }

    if (nextNode != null) {
      nextNode.setPrev(prevNode);
    }

    if (node == head) {
      head = nextNode;
    }

    if (tail == node) {
      if (tail.getPrev() != null) {
        tail.getPrev().setNext(null);
      }
      tail = tail.getPrev();
    }

    --size;
  }
}
