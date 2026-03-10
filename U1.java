import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
class U1{
public static int version = 1;
    public static void main(String[] args){
        try {
            System.out.println("Welcome to Palindrome Vhecker Management System");
            System.out.printf("Version: %d%n", version);
        } catch (java.lang.RuntimeException e) {
            throw new RuntimeException(e);
        }
        System.out.println("System initialized successfully");
        String str = "Madam";

        str = str.replaceAll("\s", "");
        str = str.toLowerCase();

        int l=0, r=str.length()-1;
        while(l<=r){
            if(str.charAt(l)!=str.charAt(r)){System.out.println("Is not a pallindrome"); return;}
        }
        System.out.println("Is a pallindrome");


    }
}
    