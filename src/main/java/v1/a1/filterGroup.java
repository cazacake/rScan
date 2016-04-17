package v1.a1;
import java.util.Map;

public class filterGroup {
	//list of subs to be searched for
	//ArrayList <String>filterList;
	String filterDescription="DefaultFilterDescription";
	String subs[];
	int minInst=1;
	String name;
	
	//public String[] returnFilterArray(){
		//String [] list=(String[]) filterList.toArray();
		//return list;
	//}
	public filterGroup(Map m){
		subs=(String[]) m.get("Subs");
		minInst=(Integer) m.get("minInst");
		name=(String) m.get("Name");
	}
	public filterGroup(String name,int minInst, String []subs){
		this.subs=subs;
		this.minInst=minInst;
		this.name=name;
	}
	public String[]getSubs(){
		return this.subs;
	}
}
