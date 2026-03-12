import java.util.*;
public class U1{
    public static final int VERSION = 1;
    static class PallindromeChecker{
        private String original;
        private char[] input;
        public PallindromeChecker(String in){
            this.original = in;
            this.input = preProcess(in).toCharArray();
        }
        public String preProcess(String x){
            String y = x.replaceAll("\\s", "");
            y = x.toLowerCase();
            return y;
        }
        public boolean check(){
            int r= input.length-1;
            for(int i=0; i<r/2; i++,r--){
            if(input[i]!=input[r]){return false;}
            }
            return true;
        }
        public void result(){
            System.out.printf("Is \"%s (preprocessed: %s)\" a pallindrome? %s", original, new String(input), check());
        }
    }
    public static void main(String[] args){
        try {
            System.out.println("Welcome to Palindrome Vhecker Management System");
            System.out.printf("Version: %d%n", VERSION);
        } catch (java.lang.RuntimeException e) {
            throw new RuntimeException(e);
        }
        System.out.println("System initialized successfully");
        String s = "Madam";

        PallindromeChecker service = new PallindromeChecker(s);
        service.result();
    }
    
}
    