package eu.fbk.newsreader.naf;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class AddALINK {

	private static int colSentId = 0;
	private static int colTimexId = 0;
	private static int colEventId = 0;
	private static int colPairs = 0;
	private static int colMainVb = 0;
	private static int colEventClass = 0;
	
	private static void init_col_id (HashMap<String,Integer> intCol){
		if(intCol.containsKey("sentid")){
			colSentId = intCol.get("sentid");
		}
		if(intCol.containsKey("timex_id")){
			colTimexId = intCol.get("timex_id");
		}
		if(intCol.containsKey("event_id")){
			colEventId = intCol.get("event_id");
		}
		if(intCol.containsKey("alink")){
			colPairs = intCol.get("alink");
		}
		if(intCol.containsKey("main_verb")){
			colMainVb = intCol.get("main_verb");
		}
		if(intCol.containsKey("pred_class")){
			colEventClass = intCol.get("pred_class");
		}
	}
	
	public static String[][] add_candidate_pairs(String [][] lines, int nbCol, HashMap<String,Integer> intCol){
		
		init_col_id(intCol);
		Integer [][] sentences = get_sentences(lines);
		
		
		for (int j=0; j<sentences.length; j++){
			if(sentences[j] != null && sentences[j][0] != null && sentences[j][1] != null){
				List<String> list_event = new ArrayList<String> ();
				for (int i=sentences[j][0]; i<=sentences[j][1]; i++){
					if(lines[i][0] != null && lines[i][colEventId] != null && lines[i][colEventId].startsWith("e")){
						if(!list_event.contains(lines[i][colEventId])){
							list_event.add(lines[i][colEventId]);
						}
					}
				}
			
				int k=0;
				for (int i=sentences[j][0]; i<=sentences[j][1]; i++){
					if(k<=list_event.size() && lines[i][0] != null && lines[i][colEventId] != null 
							&& lines[i][colEventId].startsWith("e")){
						//event-event in a sentence
						if(lines[i][colEventClass].equals("ASPECTUAL") || lines[i][colEventClass].equals("I_ACTION")){
							for (int l=0; l<list_event.size(); l++){
								if (l != k){
									if(lines[i][colPairs] == null){
										lines[i][colPairs] = "";
									}
									else{
										lines[i][colPairs] += "||";
									}
									lines[i][colPairs] += lines[i][colEventId]+":"+list_event.get(l)+":NONE";
								}
							}
						}
						/*for(int l=k+1; l<list_event.size(); l++){
							if(lines[i][colPairs] == null){
								lines[i][colPairs] = "";
							}
							else{
								lines[i][colPairs] += "||";
							}
							lines[i][colPairs] += lines[i][colEventId]+":"+list_event.get(l)+":NONE";
						}*/
						
						k++;
					}
				}
			}
			
		}
		
		for (int i=0; i<lines.length; i++){
			if(lines[i] != null && (lines[i][colPairs] == null || lines[i][colPairs].equals(""))){
				lines[i][colPairs] = "O";
			}
		}
		
		return lines;
	}
	
	private static Integer[][] get_sentences(String[][]lines){
		int i=lines.length-1;
		String numLastSent = "-1";
		while(numLastSent.equals("-1") && i>0){
			if(lines[i][0] != null && lines[i][colSentId] != null){
				numLastSent = lines[i][colSentId];
			}
			i--;
		}
		Integer [][] sentences = new Integer [Integer.parseInt(numLastSent)+1][2];
		
		String numSent = "-1";
		int firstId = -1;
		int lastId = -1;
		for(i=5; i<lines.length; i++){
			if (lines[i][0] != null && lines[i][colSentId] != null && !numSent.equals(lines[i][colSentId])){
				if (!numSent.equals("-1")){
					lastId = i-1;
					sentences[Integer.parseInt(numSent)][0] = firstId;
					sentences[Integer.parseInt(numSent)][1] = lastId;
				}
				numSent = lines[i][colSentId];
				firstId = i;
			}
		}
		lastId = lines.length-1;
		sentences[Integer.parseInt(numSent)][0] = firstId;
		sentences[Integer.parseInt(numSent)][1] = lastId;
		
		return sentences;
	}
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		
	}

}
