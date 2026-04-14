package Main.HotelBookingManagementSystem;

import java.util.Arrays;
import java.util.Iterator;

public class hotelBookingApp {
	static int VERSION;
	static String GREETINGS;
	static int availability[] = {3, 6, 2}; // in order SingleRoom, DoubleRoom, SuitRoom

	static{
		try{
			VERSION =2;
			GREETINGS = "Welcome to Hotel Booking Management Application";
			System.out.println();
			System.out.println("Initialized Successfuly");
			System.out.println("Version: "+VERSION);
			System.out.println(GREETINGS);
			System.out.println();
		}
		catch(Exception e){
			System.out.println("Initialization failed"+ e.getMessage());
			System.exit(1);
		}
	}

	public static void main(String[] args) {
		Room rooms[] = {new SingleRoom(), 
						new DoubleRoom(),
						new SuitRoom()};
		Iterator<Integer> availableRooms = Arrays.stream(availability).boxed().iterator();

		for(Room r : rooms){
			if(availableRooms.hasNext()) r.displayRoomDetails(availableRooms.next());

			System.out.println();
		}
	}
}

abstract class Room{
	protected int noOfBeds, size;
	protected double pricePerNight;

	public Room(int nOb, int s, double ppN){
		this.noOfBeds = nOb;
		this.size = s;
		this.pricePerNight = ppN;
	}

	public abstract String getRoomName();
	public void displayRoomDetails(int avail){
		System.out.println(getRoomName());
		System.out.println("Beds: "+ noOfBeds);
		System.out.println("Size: "+ size);
		System.out.println("Price per Night: "+ pricePerNight);
		System.out.println("Available: "+ avail);
	}

}

class SingleRoom extends Room{
	public SingleRoom(){super(1, 250, 1500.0);}
	@Override 
	public String getRoomName(){return "SingleRoom";}
}

class DoubleRoom extends Room{
	public DoubleRoom(){super(2, 400, 2500.0);}
	@Override 
	public String getRoomName(){return "DoubleRoom";}
}

class SuitRoom extends Room{
	public SuitRoom(){super(3, 750, 5000.0);}
	@Override 
	public String getRoomName(){return "SuitRoom";}
}
