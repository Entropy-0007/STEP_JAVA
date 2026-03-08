import java.util.*;
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
        System.out.printf("It %s a pallindrome", construct(str, 0, str.length()-1)?"is":"is not");
    }
    public static boolean construct(String in, int l, int r){
        if(l>=r){
            return true;
        }
        if(in.charAt(l) != in.charAt(r)) return false;
        return construct(in, ++l, --r);
    }
}
    