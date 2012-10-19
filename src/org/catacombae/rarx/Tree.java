package org.catacombae.rarx;
import java.util.LinkedList;

public class Tree<A> extends TreeNode<A> {
    public Tree(A value) { super(value); }
    public Tree() { super(); }
    
    private LinkedList<Pair<String, TreeNode<A>>> children = new LinkedList<Pair<String, TreeNode<A>>>();
    
    private static class Pair<A, B> {
	public A a;
	public B b;
	public Pair(A a, B b) {
	    this.a = a;
	    this.b = b;
	}
    }
    
    public void put(String entryName, TreeNode<A> tn) {
	children.addLast(new Pair<String, TreeNode<A>>(entryName, tn));
    }
    public TreeNode<A> get(String entryName) {
	for(Pair<String, TreeNode<A>> p : children) {
	    if(p.a.equals(entryName))
		return p.b;
	}
	return null;
    }
    
    public int childCount() {
	return children.size();
    }
    
    public String[] listChildrenNames() {
	String[] targetArray = new String[children.size()];
	int i = 0;
	for(Pair<String, TreeNode<A>> p : children)
	    targetArray[i++] = p.a;
	
	return targetArray;
	
    }
    
    public A[] listChildren(A[] targetArray) {
	if(targetArray.length != children.size())
	    throw new IllegalArgumentException("target array not matching number of children");
	else {
	    int i = 0;
	    for(Pair<String, TreeNode<A>> p : children) {
		targetArray[i++] = p.b.getValue();
	    }
	    return targetArray;
	}
    }
}
