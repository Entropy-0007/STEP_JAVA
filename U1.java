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
    int i=0,j=str.length()-1;
    // Iterator<Character> ls = new LinkedList<>(str.chars().mapToObj(c->(char)c).toList()).iterator();
    List<Character> ls = new LinkedList<>(str.chars().mapToObj(c->(char)c).toList());
    //Iterator<Character> f = ls.iterator(); Iterator<Character> s = ls.iterator();
    int f=0,s=0;
    while(f<ls.size() && f+1<ls.size()){
        f+=2;s=1;
    }
    Collections.reverse(ls.subList(s, ls.size()));
    int mid =s;f=0;
    while(f<mid && s<ls.size()){
        if(ls.get(s)!=ls.get(f)){System.out.println("Not a pallindrome"); return;}
    }
    System.out.println("Is a pallindrome");
}
    