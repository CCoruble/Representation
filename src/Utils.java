import org.apache.jena.base.Sys;

/**
 * Created by Clovis on 23/05/2018.
 */
public class Utils
{
	public static void print(String args){
		System.out.println(args);
	}
	public static void printSpacer(String arg){
		String s = "";
		for(int i = 0; i < 20; i++)
			s += arg;
		System.out.println(s);
	}
}
