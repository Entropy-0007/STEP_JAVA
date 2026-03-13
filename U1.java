import java.util.*;
public class U1{
    public static final int VERSION = 1;
    static interface PallindromeChecker{
        public static String preProcess(String x){
            String y = x.replaceAll("\\s", "");
            y = y.toLowerCase();
            return y;
        }
        public default boolean check(char[] input){
            for(int l=0, r= input.length-1; l<r; l++,r--){
            if(input[l]!=input[r]){return false;}
            }
            return true;
        }
        public default void result(boolean res, String original, char[] input){
            System.out.printf("Is \"%s (preprocessed: %s)\" a pallindrome? %s", original, new String(input), res);
        }
        public void printResult();
    }

    static class StackCheck implements PallindromeChecker{
        private String original;
        private char[] input;
        public StackCheck(String in){
            this.original = in;
            this.input = PallindromeChecker.preProcess(original).toCharArray();
        }
        @Override
        public boolean check(char[] input) {
            Stack<Character> st = new Stack<>();
            for(char c : input){st.add(c);}
            for(int i=0;i<input.length;i++){
                if(st.pop() != input[i]){return false;}
            }
            return true;
        }
        public void printResult(){
            PallindromeChecker.super.result(check(input), original, input);
        }
    }

    static class DqueueCheck implements PallindromeChecker{
        private String original;
        private char[] input;
        public DqueueCheck(String in){
            this.original = in;
            this.input = PallindromeChecker.preProcess(original).toCharArray();
        }
        @Override
        public boolean check(char[] input){
            Deque<Character> dq = new LinkedList<>();
            for(char c : input){dq.add(c);}
            while(dq.size()>1){
                if(!dq.removeFirst().equals(dq.removeLast())){return false;}
            }
            return true;
        }
        public void printResult(){
            PallindromeChecker.super.result(check(input), original, input);
        }
    }

    @FunctionalInterface
    interface Strategy{
        boolean check();
    }

    static void runTime(String strategy, Strategy algo){
        long s,e;
        s = System.nanoTime();
        boolean res = algo.check();
        e = System.nanoTime();
        System.out.printf("%s : Time = %d%n", strategy, e-s);
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
        PallindromeChecker service;
        /*char choise; choise = new Scanner(System.in).next().toLowerCase().charAt(0);
        switch(choise){
            case 's':
                service = new StackCheck(s);
                break;
            case 'q':
                service = new DqueueCheck(s);
                break;
            default:
                service = new PallindromeChecker() {
                    String original = s;
                    char[] input = PallindromeChecker.preProcess(original).toCharArray();
                    @Override
                    public boolean check(char[] input) {
                        return PallindromeChecker.super.check(input);
                    }
                    public void printResult(){
                        PallindromeChecker.super.result(check(input), original, input);
                    }   
                };
                break;
        }
        service.printResult();*/
        runTime("DEFAULT", ()-> new PallindromeChecker(){public void printResult(){}}.check(PallindromeChecker.preProcess(s).toCharArray()));
        runTime("Stack", () -> new StackCheck(s).check(PallindromeChecker.preProcess(s).toCharArray()));
        runTime("Dqueu", () -> new DqueueCheck(s).check(PallindromeChecker.preProcess(s).toCharArray()));     
    }  
}