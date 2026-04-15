package Main.HotelBookingManagementSystem;

import Main.HotelBookingManagementSystem.hotelBookingApp.RoomType;

import java.util.*;

public class hotelBookingApp {
	static int VERSION;
	static String GREETINGS;
	static int availability[] = {3, 6, 2}; // in order SingleRoom, DoubleRoom, SuitRoom
	public enum RoomType{
		SINGLE{@Override public Room createRoom(){return new SingleRoom();}},
		DOUBLE{@Override public Room createRoom(){return new DoubleRoom();}},
		SUIT{@Override public Room createRoom(){return new SuitRoom();}};

		public abstract Room createRoom();
	} 
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
		RoomInventory inventory = new RoomInventory();

		for (RoomType type : RoomType.values()) {
			type.createRoom().displayRoomDetails(inventory.getRoomAvailability().get(type));
			System.out.println();
		}


	}
}

abstract class Room{
	protected int noOfBeds;
	protected int size;
	protected double pricePerNight;

	Room(int nOb, int s, double ppN){
		this.noOfBeds = nOb;
		this.size = s;
		this.pricePerNight = ppN;
	}

	public abstract RoomType getRoomName();
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
	public RoomType getRoomName(){return RoomType.SINGLE;}
}

class DoubleRoom extends Room{
	public DoubleRoom(){super(2, 400, 2500.0);}
	@Override 
	public RoomType getRoomName(){return RoomType.DOUBLE;}
}

class SuitRoom extends Room{
	public SuitRoom(){super(3, 750, 5000.0);}
	@Override 
	public RoomType getRoomName(){return RoomType.SUIT;}
}

class RoomInventory{
	private Map<RoomType, Integer> roomAvailability;

	public RoomInventory(){
		initializeInventory();
	}
	private void initializeInventory(){
		roomAvailability = new HashMap<>();
		updateAvailability(RoomType.SINGLE, 10);
		updateAvailability(RoomType.DOUBLE, 5);
		updateAvailability(RoomType.SUIT, 6);
	}
	public Map<RoomType, Integer> getRoomAvailability(){return roomAvailability;}
	public void updateAvailability(RoomType type, int count){roomAvailability.put(type, count);}

}