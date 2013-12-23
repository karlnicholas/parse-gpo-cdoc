package congress;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;

import model.*;

import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;

public class ParseCongress {

	static ArrayList<Congress> notes = new ArrayList<Congress>();

	static Congress currentCongress;
	static Session currentSession;
	static State currentState;
	static Chamber currentChamber;

	static boolean nextLineCongressDate = false;
	static boolean nextLineSessionDate = false;

	static String currentString = null;
	static String lastString = null;


	static ArrayList<String> states = new ArrayList<String>();

	public static void main(String... args) throws Exception {
		XWPFDocument document = new XWPFDocument(new FileInputStream("c:/users/karl/downloads/GPO-CDOC-108hdoc222-3.docx"));

		StringBuilder builder = new StringBuilder();

		addStates();
		
		
		int line = 1;
		BufferedWriter  debugFile = new BufferedWriter( 
				new OutputStreamWriter( 
					new FileOutputStream("c:/users/karl/downloads/debugfile.txt"), Charset.forName("UTF-8") ) );
		Iterator<XWPFParagraph> i = document.getParagraphsIterator();
		while(i.hasNext()) {
			XWPFParagraph paragraph = i.next();

			// Do the paragraph text
			for(XWPFRun run : paragraph.getRuns()) {
				String str = run.toString();
				if ( str.length() == 0 ) continue;
				builder.append(str);
				if ( !str.endsWith(",") && !str.endsWith(", ") && !str.equals(" ")  ) {
					lastString = currentString;
					currentString = builder.toString();
					String debugString = currentString;
					if ( !searchCurrents(currentString) ) {
						if ( !allUpper(str)) {
							debugFile.write(line + ": " + debugString);
							debugFile.newLine();
							line++;
							builder = new StringBuilder();							
						}
					} else {
						debugFile.write(line + ": " + debugString);
						debugFile.newLine();
						line++;
						builder = new StringBuilder();
					}
				}
			}

			// Do endnotes and footnotes
			String footnameText = paragraph.getFootnoteText();
			if(footnameText != null && footnameText.length() > 0) {
//				note.footnote = footnameText;
				String[] splits = footnameText.split("\\[[0-9][0-9][0-9][0-9]:");
				if ( splits.length > 2 ) {
//					System.out.println("** DOUBLE **" + splits.length +":" + lastString);
					Note note = new Note(
						lastString, 							
						footnameText.substring(
							footnameText.indexOf( splits[splits.length-2] )-6, 
							footnameText.indexOf( splits[splits.length-1] )-6
						), 
						line
					);
					addNote( note);
					note = new Note(
						currentString, 
						footnameText.substring(
							footnameText.indexOf( splits[splits.length-1] )-6
						), 
						line
					);
					addNote( note);
				} else {
					Note note = new Note(
						currentString, 
						footnameText, 
						line
					);
					addNote( note);
				}
			}

		}
	
		// write out json
		ObjectMapper mapper = new ObjectMapper();
		ObjectWriter writer = mapper.writer().withDefaultPrettyPrinter();
	    System.out.println(writer.writeValueAsString(notes));
//		mapper.writeValue(new File("c:/users/karl/downloads/congress.json"), notes);
		debugFile.close();
	}
	
	private static void addNote(Note note) {
		Congress lCongress = null;
		if ( notes.size() != 0 ) lCongress = notes.get(notes.size()-1);
		if ( lCongress != currentCongress || lCongress == null) {
			currentCongress.sessions = new ArrayList<Session>();
			lCongress = currentCongress;  
			notes.add(lCongress);
		} 

		Session lSession = null;
		if ( lCongress.sessions.size() != 0 ) lSession = lCongress.sessions.get(lCongress.sessions.size()-1);
		if ( lSession != currentSession || lSession == null ) {
			if ( currentSession == null ) currentSession = new Session(null);
			currentSession.states = new ArrayList<State>();
			lSession = currentSession;
			lCongress.sessions.add(lSession);
		}

		State lState = null;
		if ( lSession.states.size() != 0 ) lState = lSession.states.get(lSession.states.size()-1);
		if ( lState != currentState || lState == null ) {
			if ( currentState == null ) currentState = new State(null);
			currentState.chambers = new ArrayList<Chamber>();
			lState = currentState;
			lSession.states.add(lState);
		}

		Chamber lChamber = null;
		if ( lState.chambers.size() != 0 ) lChamber = lState.chambers.get(lState.chambers.size()-1);
		if ( lChamber != currentChamber || lChamber == null ) {
			if ( currentChamber == null ) currentChamber = new Chamber(null);
			currentChamber.notes = new ArrayList<Note>();
			lChamber = currentChamber;
			lState.chambers.add(lChamber);
		}

		lChamber.notes.add(note);
	}
	
	private static boolean allUpper(String str) {
		for ( int i=0, j=str.length(); i<j; ++i ) {
			char ch = str.charAt(i);
			if ( Character.isLowerCase(ch) ) return false;
		}
		return true;
	}
	
	private static boolean searchCurrents(String str ) {
		int idx;
		str = str.trim();
		if ( str.endsWith("CONGRESS")) {
			if ( currentCongress== null || !str.equals(currentCongress.congress) ) {
				currentCongress = new Congress(str);
				currentSession = null;
				currentState = null;
				currentChamber = null;
				currentString = "";
	
				nextLineCongressDate = true;
			}
			return true;
		} else if ( nextLineCongressDate ) {
			currentCongress.date = str;
			nextLineCongressDate = false;
			return true;
		} else if ( str.contains("SESSION—")) {
			currentSession = new Session(str);
			currentState = null;
			currentChamber = null;
			currentString = "";
	
			nextLineSessionDate = true;
			return true;
		} else if ( nextLineSessionDate ) {
			currentSession.date = str;
			nextLineSessionDate = false;
			return true;
		} else if ( (idx = Collections.binarySearch(states, str)) >= 0 ) {
			currentState = new State(states.get(idx));
			currentChamber = null;
			currentString = "";
			return true;
		} else if ( str.equals("REPRESENTATIVES")) {
			currentChamber = new Chamber(str);
			currentString = "";
			return true;
		} else if ( str.equals("SENATORS")) {
			currentChamber = new Chamber(str);
			currentString = "";
			return true;
		} else if ( str.equals("DELEGATE")) {
			currentChamber = new Chamber(str);
			currentString = "";
			return true;
		} else if ( str.equals("RESIDENT COMMISSIONER")) {
			currentChamber = new Chamber(str);
			currentString = "";
			return true;
		}
		return false;
	}
	private static void addStates() {
		states.add("ALABAMA");
		states.add("ALASKA");
		states.add("ARIZONA");
		states.add("ARKANSAS");
		states.add("CALIFORNIA");
		states.add("COLORADO");
		states.add("CONNECTICUT");
		states.add("DELAWARE");
		states.add("FLORIDA");
		states.add("GEORGIA");
		states.add("HAWAII");
		states.add("IDAHO");
		states.add("ILLINOIS");
		states.add("INDIANA");
		states.add("IOWA");
		states.add("KANSAS");
		states.add("KENTUCKY");
		states.add("LOUISIANA");
		states.add("MAINE");
		states.add("MARYLAND");
		states.add("MASSACHUSETTS");
		states.add("MICHIGAN");
		states.add("MINNESOTA");
		states.add("MISSISSIPPI");
		states.add("MISSOURI");
		states.add("MONTANA");
		states.add("NEBRASKA");
		states.add("NEVADA");
		states.add("NEW HAMPSHIRE"); 
		states.add("NEW JERSEY");
		states.add("NEW MEXICO");
		states.add("NEW YORK");
		states.add("NORTH CAROLINA"); 
		states.add("NORTH DAKOTA");
		states.add("OHIO");
		states.add("OKLAHOMA");
		states.add("OREGON");
		states.add("PENNSYLVANIA");
		states.add("RHODE ISLAND");
		states.add("SOUTH CAROLINA"); 
		states.add("SOUTH DAKOTA"); 
		states.add("TENNESSEE");
		states.add("TEXAS");
		states.add("UTAH");
		states.add("VERMONT");
		states.add("VIRGINIA");
		states.add("WASHINGTON");
		states.add("WEST VIRGINIA");
		states.add("WISCONSIN");
		states.add("WYOMING");
		states.add("AMERICAN SAMOA"); 
		states.add("DISTRICT OF COLUMBIA"); 
		states.add("GUAM");
		states.add("PUERTO RICO");
		states.add("VIRGIN ISLANDS"); 
		Collections.sort(states);
	}

}
