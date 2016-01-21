package eu.fbk.newsreader.naf;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

public class VerbPhraseTenseAnnotator {

	private static List<String> rules = getRules("resources/rules-english.txt");
	/*static int colChunk = 7;
	static int colPOS = 6;
	static int colMorph = 8;
	static int colLemma = 9;
	*/
	static int colChunk;
	static int colPOS;
	static int colMorph;
	static int colLemma;
	
	/**
	 * apply rules on a VP and determine his tense and aspect.
	 * @param tokenvp an array of the tokens contained in the VP
	 * @param nbTok the number of tokens contained in the VP
	 * @return an array with the tense and the aspect
	 */
	public static String[] getTenseVP (String [][] tokenvp, int nbTok){
		String [] tense = new String [2];
		List<String> subListRules = getRulesNb(nbTok);
		for(String rCurr : subListRules){
			int i=0;
			boolean match = true;
			String LHS = rCurr.split(" = ")[0];
			String RHS = rCurr.split(" = ")[1];
			String [] eltR = LHS.split(Pattern.quote(" + "));
			for(String eltCurr : eltR){
				String [] attElt = eltCurr.split("/");
				if(attElt.length == 3 && tokenvp[i][0] != null){
					if((!attElt[0].matches("_v_") 
							&& !attElt[0].contentEquals(tokenvp[i][1])) 
							|| !attElt[1].contentEquals(tokenvp[i][2])
							|| !attElt[2].contentEquals(tokenvp[i][3])){
						match = false;
						break;
					}
				}
				else if(attElt[0].matches(".*\\|.*")){
					if(!tokenvp[i][0].matches(attElt[0])){
						match = false;
						break;
					}
				}
				else{
					if(!attElt[0].contentEquals(tokenvp[i][0])){
						match = false;
						break;
					}
				}
				i++;
			}
			if(match){
				tense[0] = RHS.split(",")[0];
				tense[1] = RHS.split(",")[1];
				break;
			}
		}
		return tense;
	}
	
	/**
	 * Return the rules which can be apply on n tokens
	 * @param nbEltVP = n tokens
	 * @return list of rules
	 */
	public static List<String> getRulesNb (int nbEltVP){
		List<String> subListRules = new ArrayList<String> ();
		for(String r : rules){
			int nbEltRule = (r.split(Pattern.quote("+"))).length;
			if(nbEltRule == nbEltVP){
				subListRules.add(r);
			}
		}
		return subListRules;
	}
	
	/**
	 * Read the file containing the rules
	 * @param rulesFileName
	 * @return a list of rules
	 */
	public static List<String> getRules(String rulesFileName){
		List<String> listRules = new ArrayList<String>();
		try{
			InputStream ips=new FileInputStream(rulesFileName); 
			InputStreamReader ipsr=new InputStreamReader(ips);
			BufferedReader br=new BufferedReader(ipsr);
			String line;
			while ((line=br.readLine())!=null){
				if(! line.matches("^#.*") && ! line.contentEquals("")){
					listRules.add(line);
				}
			}
			br.close(); 
		}		
		catch (Exception e){
			System.out.println(e.toString());
		}
		return listRules;
	}
	
	public static Boolean isNeg (String [][] lines, int ind){
		for (int i=ind-1; i>0 && i>ind-4; i--){
			if (lines[i][0] == null || lines[i][0].toLowerCase().matches("^(,\\.;\"')+$")){
				return false;
			}
			if (lines[i][0] != null && lines[i][0].toLowerCase().matches("^no.*$") && !lines[i][0].toLowerCase().equals("now")
					&& !lines[i][0].toLowerCase().startsWith("note")){
				return true;
			}
		}
		return false;
	}
	
	/**
	 * add the tense, aspect and polarity features
	 * @param lines an array with one token by line and information in columns
	 * @param lastCol the index of the lastCol in which the tense, aspect and poliraty will be add
	 * @return
	 */
	public static String [][] addTenseAspectVP (String [][] lines, int lastCol){
		int j=0;
		int nbTokVP=0;
		int indFirstTokVP = 0;
		boolean neg=false;
		String [][] vp = new String [10][4];
		for(int i=0; i<lines.length; i++){
			if(lines[i] != null){
				if (colChunk > 0 && lines[i].length>=colChunk && lines[i][colChunk] != null){
					if (lines[i][colChunk].contentEquals("B-VP") 
					|| lines[i][colChunk].contentEquals("I-VP") || 
					(j>0 && vp[j] != null && lines[i][colChunk] != null && lines[i][colChunk].equals("B-ADVP") 
					&& lines[i+1][colChunk] != null && lines[i+1][colChunk].equals("B-VP"))){
						if(indFirstTokVP == 0){
							indFirstTokVP = i;
						}
						nbTokVP++;
						if(lines[i][0].matches("(n't)|(not)")){
							neg=true;
						}
						if(lines[i][0].contentEquals("to")){
							String [] colTok = {lines[i][0], lines[i][0], lines[i][0], lines[i][0]};
							vp[j] = colTok;
							j++;
						}
						//else if(lines[i][colPOS].matches("V.*") && colMorph > 0 && lines[i][colMorph] != null
						//		&& lines[i][colMorph].split(Pattern.quote("+")).length>=3){
						else if(colMorph > 0 && lines[i][colMorph] != null
								&& lines[i][colMorph].split(Pattern.quote("+")).length>3){
							String mode = lines[i][colMorph].split(Pattern.quote("+"))[2];
							String tense = lines[i][colMorph].split(Pattern.quote("+"))[3];
							String [] colTok = {lines[i][0],lines[i][colLemma],mode,tense};
							vp[j] = colTok;
							j++;
						}
						/*else if(lines[i][colPOS].matches("V.*")){
							String mode = "";
							String tense = "";
							if(lines[i][colPOS].equals("VVB")){
								mode = "indic";
								tense = "present";
							}
							else if(lines[i][colPOS].equals("VVD")){
								mode = "indic";
								tense = "past";
							}
							else if(lines[i][colPOS].equals("VVG")){
								mode = "gerund";
								tense = "present";
							}
							else if(lines[i][colPOS].equals("VVI")){
								mode = "infin";
								tense = "present";
							}
							else if(lines[i][colPOS].equals("VVN")){
								mode = "part";
								tense = "past";
							}
							else if(lines[i][colPOS].equals("VVZ")){
								mode = "indic";
								tense = "present";
							}
							String [] colTok = {lines[i][0],lines[i][colLemma],mode,tense};
							vp[j] = colTok;
							j++;
						}*/
					}
					else if(j>0 && vp[0] != null){
						//if verb to verb --> split in two VP
						for(int k=0; k<j; k++){
							if(vp[k][0].equals("to") && k>0 && !vp[k-1][1].equals("have") && !vp[k-1][0].equals("going")){
								String addAttVPInf = "INFINITIVE+NONE+pos";
								for(int l=i-1; l>=(i-j+k); l--){
									lines[l][lastCol]=addAttVPInf;
								}
								for(int l=k; l<j; l++){
									vp[l]=new String [4];
								}
								nbTokVP = nbTokVP-(j-k);
								j = k;
								break;
							}
						}
						
						String [] tenseAspectVP = new String [2];
						tenseAspectVP = getTenseVP(vp,j);
						
						if(!neg){
							neg = isNeg(lines,indFirstTokVP);
						}
							
						if(tenseAspectVP != null && tenseAspectVP[0] != null){
							String addAttVP = tenseAspectVP[0].split("=")[1] + "+" + tenseAspectVP[1].split("=")[1];
							if(neg){
								addAttVP += "+neg";
							}
							else{
								addAttVP += "+pos";
							}
							for(int k=indFirstTokVP; k<(indFirstTokVP+nbTokVP); k++){
								if(lines[k][colChunk] != null && lines[k][colChunk].endsWith("-VP") 
										&& ( lines[k][colPOS].startsWith("V") || lines[k][colPOS].startsWith("N") )
										){
									lines[k][lastCol] = addAttVP;
								}
							}
						}
						else{
							if(nbTokVP>0){
								for (int l=0; l<nbTokVP; l++){
									if(vp[l] != null && vp[l][0] != null){
										String [][] vp_temp = new String [10][4];
										vp_temp[0] = vp[l];
										
										tenseAspectVP = getTenseVP(vp_temp,1);
										if(tenseAspectVP != null && tenseAspectVP[0] != null){
											String addAttVP = tenseAspectVP[0].split("=")[1] + "+" + tenseAspectVP[1].split("=")[1];
											if(neg){
												addAttVP += "+neg";
											}
											else{
												addAttVP += "+pos";
											}
											
											if(lines[indFirstTokVP+l][colChunk].endsWith("-VP")){
												lines[indFirstTokVP+l][lastCol] = addAttVP;
											}
										}
									}
								}
							}
							
						}
						j=0;
						nbTokVP=0;
						indFirstTokVP=0;
						neg=false;
						vp = new String [10][4];
					}
					else{
						j=0;
						nbTokVP=0;
						indFirstTokVP=0;
						neg=false;
						vp = new String [10][4];
					}
				}
			}
			//if(lines[i][lastCol] == null){
			//	lines[i][lastCol] = "_NULL_+_NULL_+_NULL_";
			//}
		}
		return lines;
	}

}
