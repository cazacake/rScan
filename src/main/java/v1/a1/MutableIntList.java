package v1.a1;

import java.util.TreeMap;

public class MutableIntList {
	TreeMap<String,MutableInt>mutableList;
	public MutableIntList() {
		mutableList=new TreeMap();
	}
	public void increment(String varName){
		mutableList.get(varName).increment();
	}
}
