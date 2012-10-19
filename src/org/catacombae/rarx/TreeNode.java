package org.catacombae.rarx;

public abstract class TreeNode<A> {
    private A value;
    public TreeNode(A value) {
	this.value = value;
    }
    public TreeNode() {
	this.value = null;
    }
    public A getValue() {
	return value;
	}
    public void setValue(A a) {
	value = a;
    }
}
