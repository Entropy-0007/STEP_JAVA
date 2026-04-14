import java.util.HashMap;
import java.util.Random;
import java.util.random.*;

class P1{
    public static final String useCase = "Social Media Username Availability Checker";
    public static java.util.Scanner scanner = new java.util.Scanner(System.in);

    static class UserName{
        private static java.util.Set<String> userName = java.util.concurrent.ConcurrentHashMap.newKeySet();
        private static java.util.concurrent.ConcurrentHashMap<String, java.util.concurrent.atomic.AtomicInteger> requestFrequency = new java.util.concurrent.ConcurrentHashMap<>();
        private String request;
        private String finalized;

        private String preProcess(String request){
            return request.replaceAll("[\\s$%*():;'\"\\\\]", "").toLowerCase().trim();
        }

        public void getRequest(){
            System.out.println("Enter User-Name: ");
            request = preProcess(scanner.nextLine());
            System.out.printf("Processed User-Name: %s%n", request);
        }
        public boolean verifyAvailability(String req){
            requestFrequency.computeIfAbsent(req, k -> new java.util.concurrent.atomic.AtomicInteger(0)).incrementAndGet();
            return !userName.contains(req);
        }
        public void registerUserName(){
            if(verifyAvailability(request)){finalized=request; userName.add(finalized);System.out.println("Finalized User-Name:"+finalized);}
            else{
                System.out.println("Username taken. Suggestions:");
                String candidate = request;
                while(true){
                    String[] suggestions = generateAlternative(candidate);
                    java.util.stream.IntStream.range(0,suggestions.length).mapToObj(i->(i+1)+". "+suggestions[i]).forEach(System.out::println);
                    System.out.println("Enter new User-Name or selection: ");
                    String input = scanner.nextLine();
                    if(input.matches("\\d+")){
                        int choice = Integer.parseInt(input)-1;
                        if(choice >= 0 && choice < suggestions.length){candidate = suggestions[choice];}
                    }
                    else{candidate = preProcess(input);}

                    if(verifyAvailability(candidate)){
                        finalized=candidate; userName.add(finalized); System.out.println("Finalized User-Name:"+finalized); break;
                    }
                    System.out.println("Still taken. Try again.");
                }
            }
        }
        public int getRequestFrequency(String req){
            return requestFrequency.getOrDefault(req,new java.util.concurrent.atomic.AtomicInteger(0)).intValue();
        }
        public String[] generateAlternative(String base){
            String[] prefix = {"real", "the", "iam", "official", "its"};
            String[] suffix  = {"dev", "tech", "music", "hub", "world"};
            String[] delimiter = {".", "-", "_"};

            java.util.Set<String> suggestions = new java.util.LinkedHashSet<>();
            int attempts = 0;

            while(suggestions.size() < 5 && attempts <50){
                attempts++;

                int strategy = java.util.concurrent.ThreadLocalRandom.current().nextInt(6);
                int suffixNo = java.util.concurrent.ThreadLocalRandom.current().nextInt(1, 9999);
                String delimit = delimiter[java.util.concurrent.ThreadLocalRandom.current().nextInt(delimiter.length)];

                String candidate = null;

                switch(strategy){
                    case 0:
                        candidate = base + suffixNo;
                        break;
                    case 1:
                        candidate = base + delimit + suffixNo;
                        break;
                    case 2:
                        int year = java.util.concurrent.ThreadLocalRandom.current().nextInt(1980,java.time.Year.now().getValue());
                        candidate = base + year;
                        break;
                    case 3:
                        String p = prefix[java.util.concurrent.ThreadLocalRandom.current().nextInt(prefix.length)];
                        candidate = p + base;
                        break;
                    case 4:
                        String s = suffix[java.util.concurrent.ThreadLocalRandom.current().nextInt(suffix.length)];
                        candidate = base + s;
                        break;
                    default:
                        char l = base.charAt(base.length()-1);
                        candidate = base+l;
                        break;
                }
                if(candidate != null && !userName.contains(candidate)){suggestions.add(candidate);}
            }
            return suggestions.toArray(new String[0]);
        }
    }
    
    public static void main(String[] args) {
        int loop = 0;
        while(true){
            loop++;
            UserName service = new UserName();
            service.getRequest();
            service.registerUserName();
            if(loop>50) break;
        }
    }
}