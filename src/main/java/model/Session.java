package model;

import java.util.ArrayList;

public class Session {
	public String session;
	public String date;
	public ArrayList<State> states;
	public Session(String session) {
		this.session = session;
		this.date = date;
//		states = new ArrayList<State>();
	}
}
