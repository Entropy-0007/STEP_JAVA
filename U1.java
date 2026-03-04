import java.util.*;
class U1{
public static int version = 1;
    public static void main(String[] args){
        try {
            System.out.println("Welcome to Palindrome Vhecker Management System");
            System.out.printf("Version: %d\n", version);
        } catch (java.lang.RuntimeException e) {
            throw new RuntimeException(e);
        }
        System.out.println("System initialized successfully");
    }
    String str = "Madam";
    int i=0,j=str.length();
    String strD = new String();
    for(char c : str.toLowerCase().toCharArray()){
        strD=c+strD;
    }
    for(;i<j;i++){
        if(str.charAt(i)!=strD.charAt(i)) System.out.println("Not a pallindrome"); return;
    }
    System.out.println("Is a pallindrome");
}