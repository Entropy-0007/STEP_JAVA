package Main.HotelBookingManagementSystem;

public class hotelBookingApp {
	static int VERSION;
	static String GREETINGS;

	static{
		try{
			VERSION =1;
			GREETINGS = "Welcome to Hotel Booking Management Application";
			System.out.println("Initialized Successfuly");
			System.out.println("Version: "+VERSION);
			System.out.println(GREETINGS);
		}
		catch(Exception e){
			System.out.println("Initialization failed"+ e.getMessage());
			System.exit(1);
		}
	}
	public static void main(String[] args) {
		
	}
}
