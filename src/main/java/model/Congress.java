package model;

import java.util.ArrayList;

public class Congress {
	public String congress;
	public String date;
	public ArrayList<Session> sessions;
	public Congress(String congress) {
		this.congress = congress;
		this.date = date;
//		sessions = new ArrayList<Session>();
	}

}
