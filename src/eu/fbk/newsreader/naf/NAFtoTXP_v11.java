/**
 * Anne-Lyse Minard
 * NAFtoTXP
 * Convert NAF layers in columns.
 * use version 1.1.8 of kaflib (from EHU)
 */

package eu.fbk.newsreader.naf;


import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.ListIterator;
import java.util.Random;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.util.ValidationEventCollector;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamWriter;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.TransformerFactoryConfigurationError;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.sax.SAXSource;
import javax.xml.transform.stream.StreamResult;

import org.jdom2.JDOMException;
import org.w3c.dom.DOMException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.UserDataHandler;
import org.xml.sax.AttributeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXNotRecognizedException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.AttributeListImpl;
import org.xml.sax.helpers.AttributesImpl;
import org.xml.sax.helpers.DefaultHandler;
import org.xml.sax.helpers.XMLFilterImpl;
import org.xml.sax.helpers.XMLReaderFactory;
import org.xml.sax.Attributes;


import ixa.kaflib.*;
import ixa.kaflib.KAFDocument.AnnotationType;
import ixa.kaflib.KAFDocument.Layer;
import ixa.kaflib.Predicate.Role;

public class NAFtoTXP_v11 {	
	
	static final String JAXP_SCHEMA_LANGUAGE = "http://java.sun.com/xml/jaxp/properties/schemaLanguage";
	static final String JAXP_SCHEMA_LOCATION = "http://java.sun.com/xml/jaxp/properties/schemaSource";
	static final String W3C_XML_SCHEMA = "http://www.w3.org/2001/XMLSchema";
	
	static String NULLVALUE = "__NULL__";
	

	static HashMap<String,Integer> intCol = new HashMap<String,Integer> ();

	static List<String> listNominalization;
	
	static int endHeaderLine;
	
	static BufferedWriter buffout = null;
	static private String encodingOUT = "UTF8";
	
	
	/**
	 * @param args
	 * @throws IOException 
	 * @throws SAXException 
	 * @throws ParserConfigurationException 
	 * @throws JAXBException 
	 * @throws InterruptedException 
	 * @throws JDOMException 
	 * @throws ParseException 
	 */

		
	public static void main(String[] args) throws JAXBException, ParserConfigurationException, SAXException, IOException, InterruptedException, JDOMException, ParseException {
		//BufferedReader br = new BufferedReader(new FileReader(args[2]));
		BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
		String fileNameOut = args[0];
		String fileNameRef = null;
		String listColString = null;
		String typeCorpus = null;
		if (args.length > 3){
			fileNameRef = args[1];//in order to prepare training data
			listColString = args[2];//list of columns needed
			typeCorpus = args[3];//train or eval (temprel and causalrel), or timex (TimePro)
		}
		else{
			listColString = args[1];
			typeCorpus = args[2];
		}
		String [] listCol = listColString.split("\\+");
		
		String [][] lines = NAF2TXP(br, true, listCol, (new File(fileNameOut).getName()), typeCorpus);
		
		if(inArray(listCol, "event_pred") && fileNameRef != null){
			 lines = addEventPred(lines, fileNameRef);
		 }
			
		
		 if(inArray(listCol,"coevent")){
			 lines = correctCoevent(lines);
		}
		
		
		if(inArray(listCol,"morpho") || inArray(listCol,"event") || inArray(listCol,"event_pred") || inArray(listCol,"event_ref")  || inArray(listCol,"event_ref_train")){
			if (inArray(listCol,"event") || inArray(listCol,"event_pred") || inArray(listCol,"event_ref") || inArray(listCol,"event_ref_train")){
				lines = addMorpho(lines,true,endHeaderLine);
			}
			else{
				lines = addMorpho(lines,false,endHeaderLine);
			}
		}
		
		 
		
		
		//Add the Timex reference annotation (for building or evaluation model for timex annotation)
		if ((inArray(listCol,"timex_ref") || inArray(listCol,"event_ref") || inArray(listCol,"event_ref_train") || inArray(listCol,"tlink_ref") || inArray(listCol,"slink_ref") 
				|| inArray(listCol,"alink_ref")) 
				&& fileNameRef != null){
			lines = RefTimeML.addTimeMLRefToTXP(lines, fileNameRef, intCol,
					inArray(listCol,"timex_ref"),(inArray(listCol,"event_ref") ||  inArray(listCol,"event_ref_train")),
					inArray(listCol,"tlink_ref"),inArray(listCol,"slink_ref"),inArray(listCol,"alink_ref"),
					typeCorpus);
		}
	
		
		if(inArray(listCol,"nominalization")){
			String current_path = NAFtoTXP_v11.class.getProtectionDomain().getCodeSource().getLocation().getPath();
			current_path = current_path.substring(0, current_path.lastIndexOf("/"));
			current_path = current_path.replace("/lib", "");
			listNominalization = readNominalizationfile(current_path+"/resources/list_noun_nominalization.txt");
			lines = addNominalization(lines);
		}
		
		if(inArray(listCol,"pairs") && !inArray(listCol,"tlink_ref")){
			//AddTLINKTimex.add_tlinks_between_timexes(lines, lines[0].length, intCol);
			lines = AddTLINK.add_candidate_pairs(lines, lines[0].length, intCol, false);
		}
		
		if(inArray(listCol,"alink")){
			lines = AddALINK.add_candidate_pairs(lines, lines[0].length, intCol);
		}
			
		TextProFileFormat.writeTextProFile(lines, fileNameOut, lines[0].length);
	}
	
	/**
	 * 
	 * @param f
	 * @param writePOS
	 * @param listCol
	 * @return
	 * @throws JAXBException
	 * @throws ParserConfigurationException
	 * @throws SAXException
	 * @throws IOException
	 * @throws InterruptedException 
	 * @throws JDOMException 
	 */
	public static String [][] NAF2TXP(Reader f, boolean writePOS, String [] listCol, String nameFile, String typeCorpus)
				throws JAXBException, ParserConfigurationException, SAXException,
				IOException, InterruptedException, JDOMException {

			String [][] lines = null;
			KAFDocument nafFile = null;
			try{
				nafFile = KAFDocument.createFromStream(f);

				
			} catch (Exception e){
				System.err.println("Error when reading the NAF input file");
			}
			//KAFDocument nafFile = KAFDocument.createFromFile(f);
			List<WF> tokenListTemp = nafFile.getWFs();
		
			int nbCol = 5;
			nbCol += listCol.length;
			
			if(inArray(listCol, "srl")){
				nbCol += 5;
			}
			if(inArray(listCol,"event") || inArray(listCol,"event_pred") || inArray(listCol,"event_ref")|| inArray(listCol,"event_ref_train")){
				nbCol += 2;
			}
			if((inArray(listCol,"event") || inArray(listCol,"event_pred") || inArray(listCol,"event_ref")) && inArray(listCol,"srl")){
				nbCol -= 1;
			}
			if((inArray(listCol,"event") || inArray(listCol,"event_pred") || inArray(listCol,"event_ref")|| inArray(listCol,"event_ref_train")) && !inArray(listCol,"morpho")){
				nbCol ++;
			}
			if(inArray(listCol,"timex") || inArray(listCol,"timex_ref")){
				nbCol += 2;
			}
			if(inArray(listCol,"event_ref_train")){
				nbCol += 2;
			}
			if(inArray(listCol,"event")){
				nbCol += 1;
			}
			if ((inArray(listCol,"event") || inArray(listCol,"event_pred") || inArray(listCol,"event_ref") 
					|| inArray(listCol,"event_ref_train"))
					&& !inArray(listCol,"pairs") && !inArray(listCol,"tlink_ref") && !inArray(listCol,"tlink")){
				nbCol += 2;
			}
			/*else if(inArray(listCol, "dep")){
				nbCol += 1;
			}*/
			
			int nbLine = 4;
			nbLine += tokenListTemp.size();
			nbLine += tokenListTemp.get(tokenListTemp.size()-1).getSent();
			
			if(inArray(listCol, "dct")){
				nbLine += 2;
				nbCol -= 1;
			}
			
			if(typeCorpus.equals("timex")){
				nbCol -= 2;
			}
			
			lines = new String [nbLine][nbCol];
			
			ListIterator<WF> tokenList = tokenListTemp.listIterator();
			
			List<String> termIdCandMainVerb = MainVerb.detect_main_verbs(nafFile);
			
			//HashMap<String,String> listConnectives = AddConnectivesClasses.getListConnectives(nafFile);
			HashMap<String,String> listConnectives = new HashMap<String,String> ();
			
			int lineNumber = -1;
			int indLine = 0;
			int indCol = 5;
			int indTimex = 0;
			int indDCT = 0;
			
			//Two first lines (TextPro format)
			lines[indLine][0] = "# FILE: " + nameFile;
			indLine ++;
			String docCT = getDocCreationTime(nafFile);
			//if(docCT != ""){
			lines[indLine][0] = "# DATE: " + getDocCreationTime(nafFile);
			indLine ++;
			//}
			lines[indLine][0] = "# FIELDS: token";
			
			if(typeCorpus.equals("timex")){
				lines[indLine][1] = "POS";
				lines[indLine][2] = "lemma";
				indCol = 3;
			}
			else{
				lines[indLine][1] = "tokenid";
				lines[indLine][2] = "sentid";
				lines[indLine][3] = "POS";
				lines[indLine][4] = "lemma";
			}
			
			if (inArray(listCol,"dep")){
				//lines[indLine][indCol++] = "dep_from";
				//lines[indLine][indCol++] = "dep_to";
				lines[indLine][indCol++] = "dep";
			}
			if (inArray(listCol,"timex") || inArray(listCol,"timex_ref")){
				indTimex = indCol;
				lines[indLine][indCol++] = "timex_id";
				lines[indLine][indCol++] = "timex_type";
				lines[indLine][indCol++] = "timex_value";
			}
			/*if(inArray(listCol,"dct")){
				indDCT = indCol;
				lines[indLine][indCol++] = "dct";
			}*/
			if (inArray(listCol,"entity")){
				lines[indLine][indCol++] = "entity";
			}
			if(inArray(listCol,"srl") || inArray(listCol,"event") || inArray(listCol,"event_pred") || inArray(listCol,"event_ref") || inArray(listCol, "event_ref_train")){
				if((inArray(listCol,"event_ref_train")||inArray(listCol,"event")) 
						&& !inArray(listCol,"pairs") && !inArray(listCol,"tlink_ref") && !inArray(listCol,"tlink")){
					lines[indLine][indCol++] = "pred";
				}
				lines[indLine][indCol++] = "pred_class";
				if(inArray(listCol,"event") || inArray(listCol,"event_pred") || inArray(listCol,"event_ref") || inArray(listCol, "event_ref_train")){
					lines[indLine][indCol++] = "event_id";
					//lines[indLine][indCol++] = "tense+aspect+polarity";
				}
				
				if (inArray(listCol,"srl")){
					lines[indLine][indCol++] = "role1";
					lines[indLine][indCol++] = "role2";
					lines[indLine][indCol++] = "role3";
					lines[indLine][indCol++] = "is_arg_pred";
					lines[indLine][indCol++] = "has_semrole";
				}
			}
			
			if (inArray(listCol,"chunk")){
				lines[indLine][indCol++] = "chunk";
			}
			if (inArray(listCol,"main_verb")){
				lines[indLine][indCol++] = "main_verb";
			}
			/*if (inArray(listCol,"timex_ref")){
				lines[indLine][indCol++] = "timex_ref";
			}*/
			if (inArray(listCol,"connectives")){
				lines[indLine][indCol++] = "connectives";
			}
			if (inArray(listCol,"morpho") || inArray(listCol,"event") || inArray(listCol,"event_pred") || inArray(listCol,"event_ref") || inArray(listCol,"event_ref_train")){
				lines[indLine][indCol++] = "morpho";
			}
			if ((inArray(listCol,"event") || inArray(listCol,"event_pred") || inArray(listCol,"event_ref") 
					|| inArray(listCol,"event_ref_train")) 
					&& (inArray(listCol,"pairs") || inArray(listCol,"tlink_ref") || inArray(listCol,"tlink"))){
				lines[indLine][indCol++] = "tense+aspect+pol";
				//lines[indLine][indCol++] = "tense";
				//lines[indLine][indCol++] = "aspect";
				//lines[indLine][indCol++] = "pol";
			}
			else if(inArray(listCol,"event") || inArray(listCol,"event_pred") || inArray(listCol,"event_ref") || inArray(listCol,"event_ref_train")){
				lines[indLine][indCol++] = "tense+aspect+pol";
				//lines[indLine][indCol++] = "tense";
				lines[indLine][indCol++] = "aspect";
				lines[indLine][indCol++] = "pol";
			}
			if(inArray(listCol,"coevent")){
				lines[indLine][indCol++] = "coevent";
			}
			if(inArray(listCol,"nominalization")){
				lines[indLine][indCol++] = "nominalization";
			}
			if (inArray(listCol,"pairs") || inArray(listCol,"tlink_ref")){
				lines[indLine][indCol++] = "pairs";
			}
			if (inArray(listCol,"slink_ref")){
				lines[indLine][indCol++] = "slink";
			}
			if (inArray(listCol,"alink_ref") || inArray(listCol, "alink")){
				lines[indLine][indCol++] = "alink";
			}
			if (inArray(listCol,"tlink")){
				lines[indLine][indCol++] = "tlink";
			}
			if (inArray(listCol,"event_ref_train")){
				lines[indLine][indCol++] = "event_ref_train";
				lines[indLine][indCol++] = "event_class";
			}
			
			for(int k=0; k<lines[indLine].length; k++){
				if(k==0){
					intCol.put("token",k);
				}
				intCol.put(lines[indLine][k],k);
			}
			
			indLine += 2;
			
			/*if(inArray(listCol,"dct") && getDCT(nafFile) != null){
				indCol = 5;
				Timex3 dct_timex = getDCT(nafFile);
				lines[indLine][indDCT] = "dct";	

				if(inArray(listCol,"timex")){
					lines[indLine][indTimex] = dct_timex.getId();
					lines[indLine][indTimex+1] = dct_timex.getType();
					lines[indLine][indTimex+2] = dct_timex.getValue();
				}
				indLine += 2;
			}*/
			
			if(inArray(listCol,"dct")){
				//indCol = 5;
				//lines[indLine][indDCT] = "dct";

				if(inArray(listCol,"timex_ref")){
					lines[indLine][0] = "DCT";
				}
				else{
					lines[indLine][indTimex+2] = getDocCreationTime(nafFile);
					lines[indLine][0] = "DCT_"+getDocCreationTime(nafFile);
				
					if(inArray(listCol,"timex")){
						lines[indLine][indTimex] = "tmx0";
						lines[indLine][indTimex+1] = "B-DATE";
					}
				}
				for (int k=0; k<lines[indLine].length; k++){
					if(lines[indLine][k] == null){
						lines[indLine][k] = "O";
					}
				}
				indLine += 2;
			}
			
			HashMap<String,String> termIdCoeventId = getTermIdCoeventId(nafFile);
			HashMap<String,String> termIdPredId = getTermIdPredId(nafFile);
			HashMap<String,Term> termId = getTerm(nafFile);
			HashMap<String,Coref> termIdCoref = getCoreferences(nafFile);
			HashMap<String,Timex3> termIdTimex = getTimex3(nafFile);
			HashMap<String,List<Dep>> termIdDep = getDep(nafFile);
			HashMap<String,Entity> termIdEntity = getEntity(nafFile);
			HashMap<String,List<TLink>> fromIdTlink = getTlink(nafFile);
			
			HashMap<String,List<String>> list_chunks_sentence = new HashMap<String, List<String>> ();
			//if (inArray(listCol,"clink")) getTlink(nafFile);
			//else nafFile.removeLayer(KAFDocument.Layer.tlinks);
			
			HashMap<String,Predicate> termIdPred = getPredicates(nafFile);
			
			
			HashMap<String,String> coeventIdTermId = new HashMap<String,String> ();
			for(String tid : termIdCoeventId.keySet()){
				if(!coeventIdTermId.containsKey(termIdCoeventId.get(tid))){
					coeventIdTermId.put(termIdCoeventId.get(tid), tid);
				}
			}
			
			HashMap<String,String> predIdTermId = new HashMap<String,String> ();
			for(String tid : termIdPredId.keySet()){
				if(!predIdTermId.containsKey(termIdPredId.get(tid))){
					predIdTermId.put(termIdPredId.get(tid), tid);
				}
			}
			
			
			endHeaderLine = indLine;
			
			String [] prevChunkID = new String [3];
			prevChunkID[1] = "c-1";
			prevChunkID[0] = "c";
			
			int cptToken = 1;
			
			boolean sentStartZero = false;
			
			while (tokenList.hasNext()) {
				indCol = 0;
				
				WF token = tokenList.next();

				int sentenceNumber = token.getSent(); // sentence

				if (!sentStartZero && sentenceNumber == 0){
					sentStartZero = true;
				}
				
				if (lineNumber == -1) {
					lineNumber = sentenceNumber;
					
					//List<Annotation> list_annot_constituency = nafFile.getBySent(Layer.CONSTITUENCY, sentenceNumber);
					
					List<Annotation> list_annot_constituency = nafFile.getBySent(AnnotationType.TREE, sentenceNumber);
					
					if(list_annot_constituency.size() > 0){
						list_chunks_sentence = constituency_tree((Tree) list_annot_constituency.get(0));
					}
					
					
					//list_chunks_sentence = constituency_tree (nafFile.getConstituentsBySent(sentenceNumber).get(0));
				} else if (lineNumber < sentenceNumber) {
					// add new empty line as this is new sentence and make
					// lineNumber=token sentence
					indLine ++;
					lineNumber = sentenceNumber;
					prevChunkID[1] = "c-1";
					prevChunkID[0] = "c";
					cptToken = 1;
					
					//get_list_chunks
					//List<Annotation> list_annot_constituency = nafFile.getBySent(Layer.CONSTITUENCY, sentenceNumber);
					List<Annotation> list_annot_constituency = nafFile.getBySent(AnnotationType.TREE, sentenceNumber);
					
					if (list_annot_constituency.size()>0){
						list_chunks_sentence = constituency_tree((Tree) list_annot_constituency.get(0));
					}
					//list_chunks_sentence = constituency_tree (nafFile.getConstituentsBySent(sentenceNumber).get(0));
					
				}
				//Term termTmp = getTerm(token.getId(), nafFile);
				
				//List<Dep> depTmp = getDep (token.getId(), nafFile);
				
				
				if (termId.containsKey(token.getId())) {
					Term termTmp = termId.get(token.getId());
					
					List<Dep> depTmp = null;
					if(termIdDep.containsKey(token.getId())){
						depTmp = termIdDep.get(token.getId());
					}
					
					String pos = "";
					if (termTmp.getMorphofeat() != null && !termTmp.getMorphofeat().equals("")) {
						pos = termTmp.getMorphofeat();
					} else {
						pos = NULLVALUE;
					}
					
					lines[indLine][indCol++] = token.getForm();
					if(typeCorpus.equals("timex")){
						lines[indLine][indCol++] = (writePOS ? Penn2BNC(token.getForm(), pos): "");
						lines[indLine][indCol++] = termTmp.getLemma().toLowerCase();
					}
					else{
						lines[indLine][indCol++] = termTmp.getId();
						//lines[indLine][indCol++] = token.getId().replaceFirst("w", "");
						if(sentStartZero){
							lines[indLine][indCol++] = Integer.toString(token.getSent()+1);
						}
						else{
							lines[indLine][indCol++] = Integer.toString(token.getSent());
						}
						lines[indLine][indCol++] = (writePOS ? Penn2BNC(token.getForm(), pos): "");
						lines[indLine][indCol++] = termTmp.getLemma().toLowerCase();
					}
					
					int colLemma = intCol.get("lemma");
					if (lines[indLine][colLemma].equals("'re")){
						lines[indLine][colLemma] = "be";
						lines[indLine][0] = "are";
					}
					else if (lines[indLine][colLemma].equals("'ve")){
						lines[indLine][colLemma] = "have";
						lines[indLine][0] = "have";
					}
					
					//Dependency
					if (inArray(listCol,"dep")){
						if(depTmp != null && depTmp.size() > 0){
							String contentDep = "";
							for (int k=0; k<depTmp.size(); k++){
								//if(depTmp != null){
								//lines[indLine][indCol++] = depTmp.getFrom().getLemma().replace(" ", "_");
								//lines[indLine][indCol++] = depTmp.getTo().getLemma().replace(" ", "_");
								//lines[indLine][indCol++] = depTmp.getRfunc();
								if(k > 0){
									contentDep += "||";
								}
								contentDep += depTmp.get(k).getTo().getId();
								contentDep += ":";
								contentDep += depTmp.get(k).getRfunc();
							}
							lines[indLine][indCol++] = contentDep;
						}
						else{
							lines[indLine][indCol++] = "O";
						}
					}
					
					//Timex3
					if (inArray(listCol, "timex")){
						if(termIdTimex.containsKey(token.getId())){
							Timex3 t = termIdTimex.get(token.getId());
							List<WF> list_wfs = t.getSpan().getTargets();
							String type_pref = "I";
							if (list_wfs.get(0).getId() == token.getId()){
								type_pref = "B";
							}
							
							lines[indLine][intCol.get("timex_id")] = t.getId();
							lines[indLine][intCol.get("timex_type")] = type_pref+"-"+t.getType();
							//lines[indLine][intCol.get("timex_type")] = "TIMEX";
							if(t.getValue() == null){
								lines[indLine][intCol.get("timex_value")] = "O";
							}
							else{
								lines[indLine][intCol.get("timex_value")] = t.getValue();
							}
						}
						else{
							lines[indLine][intCol.get("timex_id")] = "O";
							lines[indLine][intCol.get("timex_type")] = "O";
							lines[indLine][intCol.get("timex_value")] = "O";
						}
					}
					
					
					//Entity
					if (inArray(listCol, "entity")){
						//Entity ent = getEntity (termTmp.getId(), nafFile);
						
						if(termIdEntity.containsKey(termTmp.getId())){
							Entity ent = termIdEntity.get(termTmp.getId());
							String typeEnt = "O";
							if (ent.hasType()){
								typeEnt = ent.getType().toLowerCase();
								if(typeEnt.equals("org")){
									typeEnt = "organization";
								}
								else if(typeEnt.equals("per")){
									typeEnt = "person";
								}
								else if(typeEnt.equals("loc")){
									typeEnt = "location";
								}
								if(! typeEnt.equals("organization") && ! typeEnt.equals("person") 
										&& ! typeEnt.equals("location") && ! typeEnt.equals("misc")){
									typeEnt = "O";
								}
							}
							lines[indLine][intCol.get("entity")] = typeEnt;
						}
						else{
							lines[indLine][intCol.get("entity")] = "O";
						}
					}
					
					//SRL
					if (inArray(listCol, "srl") || inArray(listCol, "event") || inArray(listCol,"event_ref_train")){
						Predicate pred = getPredicate (termTmp.getId(), nafFile, sentenceNumber);
						//Coref coref = getCoreferences (termTmp.getId(), nafFile);
						
						if(pred != null){

							String eventType = getEventType (pred);
							/*if (termIdCoref.containsKey(termTmp.getId()) && (pred.getExternalRefs().size()>1 
									|| (pred.getExternalRefs().size() == 1 
									&& !pred.getExternalRefs().get(0).getResource().equals("NomBank")))
									&& !eventType.equals("grammatical")){*/
							

							if (termIdCoref.containsKey(termTmp.getId())){
							
								Coref coref = termIdCoref.get(termTmp.getId());
								if(eventType.equals("")){
									lines[indLine][intCol.get("pred_class")] = "pred";
								}
								else{
									lines[indLine][intCol.get("pred_class")] = eventType;
								}
								
								if(intCol.containsKey("pred")){
									lines[indLine][intCol.get("pred")] = "pred";
								}
								
								if(inArray(listCol, "event") || inArray(listCol, "event_ref_train")){
									if (! pred.getSpanStr().equals("%") && ! pred.getSpanStr().equals("''")  
											&& ! pred.getSpanStr().equals("\"")  && ! pred.getSpanStr().equals("\\+") 
											&& ! pred.getSpanStr().equals("``")  && ! pred.getSpanStr().equals("'s") 
											&& ! pred.getSpanStr().equals("_")  && pred.getSpanStr().length() > 1 
											&& ! pred.getSpanStr().matches("^[a-zA-Z]'$")
											&& (lines[indLine][intCol.get("timex_id")] == null 
													|| !lines[indLine][intCol.get("timex_id")].startsWith("tmx"))
										){
										//lines[indLine][intCol.get("event_id")] = pred.getId().replace("pr", "e");
										lines[indLine][intCol.get("event_id")] = termTmp.getId().replace("t", "e");
										//lines[indLine][indCol++] = "NONE+NONE+POS";
									}
									else{
										lines[indLine][intCol.get("event_id")] = "O";
									}
								}
								
								if(inArray(listCol, "coevent")){
									String list_terms = "";
									if(coref.getSpans().size() > 1){
										for(int j=0; j<coref.getSpans().size(); j++){
											if(!coref.getSpans().get(j).getFirstTarget().getId().equals(termTmp.getId())){
												list_terms += coref.getSpans().get(j).getFirstTarget().getId().replace("t","e")+":";
											}
										}
										list_terms = list_terms.substring(0, list_terms.length()-1);
									}
									if (list_terms.equals("")){
										list_terms = "O";
									}
									if(inArray(listCol, "event") && !inArray(listCol,"pairs") && !inArray(listCol,"tlink_ref")){
										lines[indLine][intCol.get("coevent")] = "coevent";
									}
									else{
										lines[indLine][intCol.get("coevent")] = list_terms;
									}
								}
								
							}
							
							if(inArray(listCol,"srl")){
								for(int i=0; i<3; i++){	
									if(i < pred.getRoles().size()){
										lines[indLine][intCol.get("role"+Integer.toString(i+1))] = pred.getRoles().get(i).getSemRole();
									}
									else{
										lines[indLine][intCol.get("role"+Integer.toString(i+1))] = "O";
									}
								}
							}
						}
						else{
							lines[indLine][intCol.get("pred_class")] = "O";
							if(inArray(listCol, "srl")){
								for(int i=0; i<3; i++){	
									lines[indLine][intCol.get("role"+Integer.toString(i+1))] = "O";
								}
							}
							if(inArray(listCol, "event") || inArray(listCol, "event_pred") || inArray(listCol,"event_ref")){
								lines[indLine][intCol.get("event_id")] = "O";
							}
							
							if(inArray(listCol, "coevent")){
								lines[indLine][intCol.get("coevent")] = "O";
							}
						}
						pred = getPredicateFromToken (termTmp.getId(), nafFile, sentenceNumber);
						
						if(inArray(listCol, "srl")){
							if(pred != null){
								String semRole = getSemRoleFromToken (pred, termTmp.getId());
								lines[indLine][intCol.get("is_arg_pred")] = pred.getTerms().get(0).getLemma();
								lines[indLine][intCol.get("has_semrole")] = semRole;
								
							}
							else{
								lines[indLine][intCol.get("is_arg_pred")] = "O";
								lines[indLine][intCol.get("has_semrole")] = "O";
							}
						}
						
					}
					
					// CHUNK
					if (inArray(listCol,"chunk")){
						//String [] chunkInfo = getSmallestChunk(termTmp.getId(), sentenceNumber, nafFile);
						String [] chunkInfo = getSmallestChunk(termTmp.getId(), list_chunks_sentence);
						String chunkLabel = chunkInfo[0];

						if(!prevChunkID[1].equals(chunkInfo[1]) && !chunkInfo[2].equals("1") 
								&& ((chunkLabel.equals("NP") && prevChunkID[0].equals("NP") && pos.startsWith("N"))
								|| (chunkLabel.equals("VP") && prevChunkID[0].equals("VP") && pos.startsWith("V")))){
							chunkLabel = "I-"+chunkLabel;
							//System.out.println("1:: "+termTmp.getId()+" : "+chunkLabel);
						}
						else if(!prevChunkID[1].equals(chunkInfo[1]) && !chunkInfo[2].equals("1") 
								&& ((chunkLabel.equals("NP")&& pos.startsWith("N"))
								|| ((chunkLabel.equals("VP")&& pos.startsWith("V"))))){
							chunkLabel = "B-"+chunkLabel;
							//System.out.println("2:: "+termTmp.getId()+" : "+chunkLabel);
						}
						else if ((!prevChunkID[1].equals(chunkInfo[1]) && !chunkInfo[2].equals("1")) || (chunkLabel.equals("O"))){
							chunkLabel = "O";	
							chunkInfo[0] = "O";
							//System.out.println("3:: "+termTmp.getId()+" : "+chunkLabel);
						}
						else if (!prevChunkID[1].equals(chunkInfo[1]) && chunkInfo[2].equals("1") 
								&& (!prevChunkID[0].equals(chunkLabel) || chunkLabel.equals("NP"))){
							chunkLabel = "B-"+chunkLabel;
							//System.out.println("4:: "+termTmp.getId()+" : "+chunkLabel);
						}
						else{
							chunkLabel = "I-"+chunkLabel;
							//System.out.println("5:: "+termTmp.getId()+" : "+chunkLabel);
						}
						
						lines[indLine][intCol.get("chunk")] = chunkLabel;
						prevChunkID = chunkInfo;
					}
					
					//mainVerb
					if (inArray(listCol, "main_verb")){
						if (termIdCandMainVerb.contains(termTmp.getId()) && lines[indLine][intCol.get("chunk")].endsWith("VP")){
							lines[indLine][intCol.get("main_verb")] = "mainVb";
						}
						else {
							if(intCol.containsKey("chunk") && lines[indLine][intCol.get("chunk")] != null && lines[indLine][intCol.get("chunk")].equals("I-VP")
									&& lines[indLine-1] != null && lines[indLine-1][intCol.get("main_verb")] != null
									&& lines[indLine-1][intCol.get("main_verb")].equals("mainVb")){
								lines[indLine][intCol.get("main_verb")] = "mainVb";
							}
							else{
								lines[indLine][intCol.get("main_verb")] = "O";
							}
						}
					}
					
					if (inArray(listCol, "connectives")){
						if(listConnectives.containsKey(Integer.toString(sentenceNumber)+":"+cptToken+":"+token.getForm())){
							lines[indLine][intCol.get("connectives")] = listConnectives.get(Integer.toString(sentenceNumber)+":"+cptToken+":"+token.getForm());
						}
						else{
							lines[indLine][intCol.get("connectives")] = "O";
						}
					}
					
					
					//tlink (from NAF)
					if (inArray(listCol, "tlink")){
						String fromId = "";
						if(termIdTimex.containsKey(token.getId())){
							fromId = termIdTimex.get(token.getId()).getId();
						}
						/*else if(termIdCoref.containsKey(termTmp.getId())){
							fromId = termIdCoref.get(termTmp.getId()).getId();
						}*/
						else if(termIdPred.containsKey(termTmp.getId())){
							fromId = termIdPred.get(termTmp.getId()).getId();
						}
						
						if(fromIdTlink.containsKey(fromId)){
							List<TLink> tlinkl = fromIdTlink.get(fromId);
						

							String valueCol = "";
							if(lines[indLine][intCol.get("tlink")] != null){
								valueCol = lines[indLine][intCol.get("tlink")];
							}
							
							for(TLink t : tlinkl){
								String toId = t.getTo().getId();
								String fId = t.getFrom().getId();
								//if(t.getTo().matches("^t[0-9]+")) toId = t.getTo().replace("t", "e");
								//if(t.getFrom().matches("^t[0-9]+")) fId = t.getFrom().replace("t", "e");
								//if(t.getTo().getId().contains("coevent")) toId = coeventIdTermId.get(t.getTo().getId()).replace("t","e");
								//if(t.getFrom().getId().contains("coevent")) fId = coeventIdTermId.get(t.getFrom().getId()).replace("t","e");
								if(t.getTo().getId().contains("pr")) toId = predIdTermId.get(t.getTo().getId()).replace("t","e");
								if(t.getFrom().getId().contains("pr")) fId = predIdTermId.get(t.getFrom().getId()).replace("t","e");
								valueCol += fId+":"+toId+":"+t.getRelType()+"||";
							}
							valueCol = valueCol.substring(0,valueCol.length()-2);
							lines[indLine][intCol.get("tlink")] = valueCol;
						}
						else{
							lines[indLine][intCol.get("tlink")] = "O";
						}
						
					}
					
					indLine ++;
					cptToken++;
				}//end while tokenList.hasNext()

			}
			
		
		return lines;
	}
	
	
	private static HashMap<String, String> getTermIdCoeventId(KAFDocument nafFile){
		HashMap<String,String> termIdCoeventId = new HashMap<String,String> ();
		
		ListIterator<Coref> corefl = nafFile.getCorefs().listIterator();
		while (corefl.hasNext()) {
			Coref co = corefl.next();
			if (co.getId().startsWith("coevent")){
				ListIterator<Term> tarl = co.getTerms().listIterator();
				while (tarl.hasNext()) {
					Term tar = tarl.next();
					termIdCoeventId.put(tar.getId(), co.getId());
				}
			}
		}
		return termIdCoeventId;
	}
	
	
	private static String[][] addEventPred (String [][] lines, String fileRef){
		String [][] lines_pred_event = TextProFileFormat.readFileTextPro(fileRef, 29, false);
		int j=1;
		for (int i=0; i<lines.length; i++){
			if(j<lines_pred_event.length && lines[i][1] != null && lines_pred_event[j][1] != null 
					&& lines[i][1].startsWith("t") && lines[i][1].equals(lines_pred_event[j][1])){
				if(lines_pred_event[j][lines_pred_event[j].length-1] != null && !lines_pred_event[j][lines_pred_event[j].length-1].equals("O")){
					lines[i][intCol.get("pred_class")] = lines_pred_event[j][lines_pred_event[j].length-1];
					lines[i][intCol.get("event_id")] = lines[i][intCol.get("tokenid")].replace("t", "e");
					
				}
				else{
					lines[i][intCol.get("pred_class")] = "O";
					lines[i][intCol.get("event_id")] = "O";
				}
				
				j++;
			}
			else if(lines[i][1] == null && lines_pred_event[j][1] == null){
				j++;
			}
			
		}
		
		return lines; 
	}
	

	private static HashMap<String, String> getTermIdPredId(KAFDocument nafFile){
		HashMap<String,String> termIdPredId = new HashMap<String,String> ();
		
		ListIterator<Predicate> predl = nafFile.getPredicates().listIterator();
		while (predl.hasNext()) {
			Predicate pred = predl.next();
			ListIterator<Term> tarl = pred.getTerms().listIterator();
			while (tarl.hasNext()) {
				Term tar = tarl.next();
				termIdPredId.put(tar.getId(), pred.getId());
			}
		}
		return termIdPredId;
	}
	
	private static String [][] addMorpho (String [][] lines, boolean tenseAspect, int endHeaderLine) throws IOException{

        int colTok = intCol.get("token");
        int colMorpho = intCol.get("morpho");
        int colLemma = intCol.get("lemma");
        int colPOS = intCol.get("POS");
        int colChunk = intCol.get("chunk");
		
        String current_path = NAFtoTXP_v11.class.getProtectionDomain().getCodeSource().getLocation().getPath();
		current_path = current_path.substring(0, current_path.lastIndexOf("/"));
		current_path = current_path.replace("/lib", "");
		 
		Random random = new Random();
		int START = 0;
		int END = 10000;
		int randomNum = random.nextInt((END - START) + 1) + END;
		
		String filein = "/tmp/"+randomNum+"-morpho-in.txt";
		//System.out.println(filein);
		//write tokens into a file
		try{
			FileWriter fw = new FileWriter(filein);
			BufferedWriter bw = new BufferedWriter(fw);
			
			for (int k=0; k<lines.length; k++){
				if(lines[k] != null && lines[k][colTok] != null && !lines[k][colTok].startsWith("#")){
					bw.write(lines[k][colTok].toLowerCase()+"\n");
				}
				else{
					bw.write("\n");
				}
			}
			
			bw.close();
		} catch (IOException e) {
            e.printStackTrace();
        }
		
		
		//process MorphoPro:
		String[] CONFIG = {"TEXTPRO="+current_path+"/tools/"};
        String[] cmd = {"/bin/tcsh", "-c", 
        		current_path+"/tools/MorphoPro/bin/MorphoPro.sh -l ENG " +filein};
        
        Process p = Runtime.getRuntime().exec(cmd,CONFIG);
		
        InputStream stdout = p.getInputStream();
        String line;
        String [][] linesTokMorph = new String [lines.length][10];
        int id = 0;
        try {
            // change the first space of each line into tabular
           // OutputStreamWriter out = new OutputStreamWriter(new FileOutputStream(fileout), "UTF8");
            BufferedReader brCleanUp = new BufferedReader (new InputStreamReader (stdout));
            while ((line = brCleanUp.readLine()) != null) {
                line = line.trim();
                if (line.contains(" ")) {
                	linesTokMorph[id] = line.split(" ");
                    //out.write(line.substring(0, line.indexOf(" ")) + "\t" + line.substring(line.indexOf(" ")+1).trim());
                } else if (line.length() > 0) {
                	linesTokMorph[id][0] = line;
                	linesTokMorph[id][1] = "_NULL_";
                    //out.write(line + "\t" + "_NULL_");
                }
                id ++;
                //out.write("\n");
            }
            brCleanUp.close();
            p.waitFor();

            //out.close();

        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        
        
        for(int i=endHeaderLine; i<lines.length; i++){
        	if (lines[i] != null && lines[i][colTok] != null && lines[i][colTok].toLowerCase().equals(linesTokMorph[i][colTok])){
        		String full_morpho = "";
        		String morph_verb_default = "";
        		for(int k=1; k<linesTokMorph[i].length; k++){
            		Boolean add = false;
        			if(linesTokMorph[i][k] != null){
        				String [] m = linesTokMorph[i][k].split("\\+");
        				if(m[0].equals(lines[i][colLemma].toLowerCase())){
        					String pos = m[1];
        					if(pos.equals("pn") && lines[i][colPOS].startsWith("NP"))
        						add = true;
        					else if(pos.equals("n") && lines[i][colPOS].startsWith("N") && !lines[i][colChunk].endsWith("-VP"))
        						add = true;
        					else if(pos.equals("v") && (lines[i][colPOS].startsWith("V") || lines[i][colChunk].endsWith("-VP"))){
        						if(m[2].equals("part") && m[3].equals("past") && ( 
        								lines[i][colPOS].equals("VVN") || lines[i][colPOS].equals("VVZ"))){
        							add = true;
        						}
        						else if(m[2].equals("indic") && m[3].equals("present") && lines[i][colPOS].equals("VVB")){
    								add = true;
    							}
    							else if(m[2].equals("indic") && m[3].equals("past") && lines[i][colPOS].equals("VVD")){
    								add = true;
    							}
    							else if(m[2].equals("gerund") && m[3].equals("present") && lines[i][colPOS].equals("VVG")){
    								add = true;
    							}
    							else if(m[2].equals("infin") && m[3].equals("present") && lines[i][colPOS].equals("VVI")){
    								add = true;
    							}
    							else if(lines[i][colChunk].endsWith("-VP") && lines[i][colPOS].startsWith("N")){
    								add = true;
    							}
        						if(morph_verb_default.equals("")){
        							morph_verb_default = linesTokMorph[i][k];
        						}
        					}
        					else if(pos.equals("adj") && lines[i][colPOS].startsWith("AJ"))
        						add = true;
        					else if(pos.equals("art") && lines[i][colPOS].startsWith("AT"))
        						add = true;
        					else if(pos.equals("pron") && lines[i][colPOS].startsWith("DTQ"))
        						add = true;
        					else if(pos.equals("adv") && lines[i][colPOS].startsWith("AV0"))
        						add = true;
        					else if(pos.equals("conj") && lines[i][colPOS].startsWith("CJC"))
        						add = true;
        					else if(pos.equals("prep") && ( lines[i][colPOS].startsWith("PR") || lines[i][colPOS].startsWith("TO")))
        						add = true;
        					else if(pos.equals("inter") && lines[i][colPOS].startsWith("inter"))
        						add = true;
        				}
        				if (add){
        					if(full_morpho.equals("")){
        						//full_morpho += " ";
        					//}
        						full_morpho = linesTokMorph[i][k];
        					}
        				}
        			}	
        		}
        		if (full_morpho.equals("") && lines[i][colPOS].startsWith("V")){
    				full_morpho = morph_verb_default;
    			}
        		
        		else if (full_morpho.equals("") && linesTokMorph[i][1] != null){
        			full_morpho = linesTokMorph[i][1];
        		}
        		
        		if (linesTokMorph[i][1].equals("_NULL_")){
    				full_morpho = "O";
    			}
        		/*else if (full_morpho.equals("")){
        			full_morpho = "O";
        		}*/
        		lines[i][colMorpho] = full_morpho;
        	}
        }
        
        if(tenseAspect){
	        VerbPhraseTenseAnnotator vb = new VerbPhraseTenseAnnotator();
			vb.colChunk = colChunk;
			vb.colLemma = colLemma;
			vb.colMorph = colMorpho;
			vb.colPOS = colPOS;
			lines = vb.addTenseAspectVP(lines,intCol.get("tense+aspect+pol"));
			lines = addPredTenseAspect(lines);
        }
        
        try{
			File filetmp = new File(filein);
			if(filetmp.delete()){
				//System.out.println("deleted");
			}
			else{
				//System.out.println("not deleted");
			}
		}
		catch(Exception e){
			e.printStackTrace();
		}
        
		return lines;
	}

	private static String[][] addPredTenseAspect(String[][] lines){
		int colPOS = intCol.get("POS");
		int colEv = 0;
		if (intCol.containsKey("event_id")){
			colEv = intCol.get("event_id");
		}
		else if(intCol.containsKey("event_ref_train")){
			colEv = intCol.get("event_ref_train");
		}
		int colTenseAspect = intCol.get("tense+aspect+pol");
		for(int i=4; i<lines.length; i++){
			if(lines[i] != null && lines[i][colEv] != null && !lines[i][colEv].equals("O") && lines[i][colTenseAspect] == null){
				if(intCol.containsKey("aspect")){
					lines[i][colTenseAspect] = "NONE";
					lines[i][colTenseAspect+1] = "NONE";
					lines[i][colTenseAspect+2] = "pos";
				}
				else{
					lines[i][colTenseAspect] = "NONE+NONE+pos";
				}
			}
			else if(lines[i] != null && lines[i][colTenseAspect] == null){
				if(intCol.containsKey("aspect")){
					lines[i][colTenseAspect] = "O";
					lines[i][colTenseAspect+1] = "O";
					lines[i][colTenseAspect+2] = "O";
				}
				else{
					lines[i][colTenseAspect] = "O";
				}
			}
			else{
				if(intCol.containsKey("aspect")){
					String [] tenseaspectpol = lines[i][colTenseAspect].split("\\+");
					if(tenseaspectpol.length == 3){
						lines[i][colTenseAspect] = tenseaspectpol[0];
						lines[i][colTenseAspect+1] = tenseaspectpol[1];
						lines[i][colTenseAspect+2] = tenseaspectpol[2];
					}
				}
			}
		}
		return lines;
	}
	
	
	/**
	 * add a features if the noun is in the WordNet list of event nouns
	 * @param lines
	 * @return lines
	 */
	private static String [][] addNominalization (String [][] lines){
		
		for(int i=0; i<lines.length; i++){
			if(lines[i].length>0 && lines[i][0] != null && lines[i][intCol.get("POS")] != null){
				if(listNominalization.contains(lines[i][intCol.get("lemma")]) && lines[i][intCol.get("POS")].startsWith("N")){
					lines[i][intCol.get("nominalization")] = "nominalization";
				}
				else{
					lines[i][intCol.get("nominalization")] = "O";
				}
			}
		}
		return lines;
	}
	
	/**
	 * read the file containing the list of nouns associated to the event class in WordNet
	 * @param name of the file
	 * @return list of the nouns
	 */
	public static List<String> readNominalizationfile (String NMZFileName){
		List<String> listNoun = new ArrayList<String>();
		
		try{
			InputStream ips=new FileInputStream(NMZFileName); 
			InputStreamReader ipsr=new InputStreamReader(ips);
			BufferedReader br=new BufferedReader(ipsr);
			String line;
			
			while ((line=br.readLine())!=null){
				if(!line.equals("")){
					listNoun.add(line);
				}
			}
			br.close(); 
		}		
		catch (Exception e){
			System.out.println(e.toString());
		}
		
		return listNoun;
	}
	
	private static String[][] correctCoevent (String[][] lines){
		HashMap<String,String> list_event_id = new HashMap<String,String>();
		for (int i=0; i<lines.length; i++){
			if (lines[i][intCol.get("lemma")] != null){
				if (lines[i][intCol.get("event_id")] != null && lines[i][intCol.get("event_id")].startsWith("e")){
					list_event_id.put(lines[i][intCol.get("tokenid")].replace("t","e"),lines[i][intCol.get("event_id")]);
				}
			}
		}
		for (int i=1; i<lines.length; i++){
			if (lines[i][intCol.get("lemma")] != null){
				/*if (lines[i][intCol.get("coevent")] != null){
					System.out.println(lines[i][0]+" : "+lines[i][intCol.get("coevent")]);
					System.out.println(lines[i][intCol.get("event_id")]);
				}*/
				if(lines[i][intCol.get("coevent")] != null && lines[i][intCol.get("coevent")].startsWith("e")){
					if(!lines[i][intCol.get("event_id")].startsWith("e")){
						lines[i][intCol.get("coevent")] = "O";
					}
					else{
						String[] list_coev = lines[i][intCol.get("coevent")].split(":");
						String list_coevent_string = "";
						for(int j=0; j<list_coev.length; j++){
							if(list_event_id.containsKey(list_coev[j])){
								list_coevent_string += list_event_id.get(list_coev[j])+":";
							}
						}
						if(list_coevent_string.equals("")){
							list_coevent_string = "O";
						}
						else{
							list_coevent_string = list_coevent_string.substring(0, list_coevent_string.length()-1);
						}
						lines[i][intCol.get("coevent")] = list_coevent_string;
					}
				}
			}
		}
		
		return lines;
	}
	
	/**
	 * Test if an string is in an array
	 * @param tab
	 * @param elt
	 * @return
	 */
	private static Boolean inArray (String [] tab, String elt){
		for (int i=0; i<tab.length; i++){
			if (tab[i].equals(elt)){
				return true;
			}
		}
		return false;
	}
	
		private static String Penn2BNC(String token, String POS) {
			if (token.equals("that")) {
				return "CJT";
			} else if (token.equals("of")) {
				return "PRF";
			} else if (token.equals("not") || token.equals("n't")) {
				return "XX0";
			}

			if (POS.equals("$") || POS.equals(",") || POS.contains("-")
					|| POS.equals(".") || POS.equals(":")) {
				return "PUN";
			} else if (POS.equals("``") || POS.equals("\"")) {
				return "PUQ";
			} else if (POS.equals("(")) {
				return "PUL";
			} else if (POS.equals(")")) {
				return "PUR";
			} else if (POS.equals("CC")) {
				return "CJC";
			} else if (POS.equals("JJ")) {
				return "AJ0";
			} else if (POS.equals("JJR")) {
				return "AJC";
			} else if (POS.equals("DT")) {
				return "AT0";
			} else if (POS.equals("RB")) {
				return "AV0";
			} else if (POS.equals("RP")) {
				return "AVP";
			} else if (POS.equals("WRB")) {
				return "AVQ";
			} else if (POS.equals("CD")) {
				return "CRD";
			} else if (POS.equals("PRP$")) {
				return "DPS";
			} else if (POS.equals("WDT") || POS.equals("WP$")) {
				return "DTQ";
			} else if (POS.equals("EX")) {
				return "EX0";
			} else if (POS.equals("UH")) {
				return "ITJ";
			} else if (POS.equals("NN")) {
				return "NN1";
			} else if (POS.equals("NNS")) {
				return "NN2";
			} else if (POS.equals("NNP") || POS.equals("NNPS")) {
				return "NP0";
			} else if (POS.equals("PRP")) {
				return "PNP";
			} else if (POS.equals("WP")) {
				return "PNQ";
			} else if (POS.equals("POS")) {
				return "POS";
			} else if (POS.equals("IN")) {
				return "PRP";
			} else if (POS.equals("TO")) {
				return "TO0";
			} else if (POS.equals("FW")) {
				return "UNC";
			} else if (POS.equals("MD")) {
				return "VM0";
			} else if (POS.equals("VB")) {
				return "VVB";
			} else if (POS.equals("VBD")) {
				return "VVD";
			} else if (POS.equals("VBG")) {
				return "VVG";
			} else if (POS.equals("VBP")) {
				return "VVI";
			} else if (POS.equals("VBN")) {
				return "VVN";
			} else if (POS.equals("VBX")) {
				return "VVZ";
			} else if (POS.equals("SYM")) {
				return "ZZ0";
			}
			return "NN0";
		}
		
		/**
		 * Get the document creation time
		 * @param nafFile
		 * @return
		 */
		private static String getDocCreationTime (KAFDocument nafFile){
			if (nafFile.getFileDesc() != null){
				return nafFile.getFileDesc().creationtime;
			}
			else{
				HashMap<String,Timex3> listTimex3 = getTimex3(nafFile);
				for(String txid : listTimex3.keySet()){
					if(listTimex3.get(txid).hasFunctionInDocument() 
							&& listTimex3.get(txid).getFunctionInDocument().equals("CREATION_TIME")){
						return listTimex3.get(txid).getValue();
					}
				}
			}
			return "";
			//else{
			/*DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
			Date date = new Date();
			return dateFormat.format(date).toString();*/
			//}
		}
		
		/**
		 * Get Term from a word id (wid)
		 * @param wid
		 * @param nafFile
		 * @return Term or null
		 */
		private static HashMap<String,Term> getTerm(KAFDocument nafFile) {
			HashMap<String, Term> listTerm = new HashMap<String,Term> ();
			ListIterator<Term> terml = nafFile.getTerms().listIterator();
			while (terml.hasNext()) {
				Term term = terml.next();
				ListIterator<WF> tarl = term.getSpan().getTargets().listIterator();
				while (tarl.hasNext()) {
					WF tar = tarl.next();
					listTerm.put(tar.getId(), term);
				}
			}
			return listTerm;
		}

		
		/**
		 * Get the smallest chunk in which a word "wid" is
		 * @param wid
		 * @param nafFile
		 * @return An array: [Chunk phrase, chunk id, position of the word in the chunk]
		 */
		private static String[] getSmallestChunk (String wid, HashMap<String,List<String>> list_chunks) {
			
			int chunk_length = 1000;
			String[] chunkInfo = {"O","c-1","0"};
			for(String chunk: list_chunks.keySet()){
				int ind = 0;
				Boolean find = false;
				if(list_chunks.get(chunk).contains(wid)){
					for(String stid: list_chunks.get(chunk)){
						ind++;
						if (stid.equals(wid)){
							find = true;
							break;
						}
					}
				}
				if (find == true && chunk_length >= list_chunks.get(chunk).size()){
				//if (find == true && chunk_length > list_chunks.get(chunk).size()){
					chunkInfo[0] = chunk.split(":")[1];
					chunkInfo[1] = chunk.split(":")[0];
					chunkInfo[2] = Integer.toString(ind);
					chunk_length = list_chunks.get(chunk).size();

					//System.out.println(chunk+" : "+wid);
					//System.out.println(chunkInfo[0]+" / "+chunkInfo[1]+" / "+chunkInfo[2]);
				}
			}
			return chunkInfo;
		}

		/**
		 * Get the smallest chunk in which a word "wid" is
		 * @param wid
		 * @param nafFile
		 * @return An array: [Chunk phrase, chunk id, position of the word in the chunk]
		 */
		private static String[] getSmallestChunk_old (String tid, int sentid, KAFDocument nafFile) {
			String[] chunkInfo = {"O","c-1","0"};
			int chunk_length = 1000;
			ListIterator<Tree> treeList = nafFile.getConstituentsBySent(sentid).listIterator();
			while(treeList.hasNext()){
				Tree t = treeList.next();
				constituency_tree (t);
				TreeNode r = t.getRoot();
			
				List<TreeNode> listNode = r.getChildren();
				String label = "";
				String chunkid = "";

				HashMap<String,String> idChunk = new HashMap<String,String> ();
				idChunk = getIdChunk(r,idChunk);
				
				HashMap<String,String> const_id_label = new HashMap<String,String> ();
				String labelTmp = "";
				const_id_label = getLabelNode(listNode, const_id_label,labelTmp);
				
				List<TreeNode> listAllNodes = new ArrayList<TreeNode> ();
				listAllNodes = getListAllNodes(listNode,listAllNodes);
				
				for (TreeNode n : listAllNodes){
					if(n.isTerminal()){
						List<Term> termList = ((Terminal) n).getSpan().getTargets();
						ListIterator<Term> tarl = termList.listIterator();
						
						if(idChunk.containsKey(n.getId()) && idChunk.containsKey(idChunk.get(n.getId()))){
							chunkid = idChunk.get(idChunk.get(n.getId()));
						}
						if(const_id_label.containsKey(n.getId()) && const_id_label.get(n.getId()) != null 
								&& const_id_label.get(n.getId()) != ""){
						label = const_id_label.get(n.getId());
						
						Boolean find = false;
						int ind = 0;
						while (find == false && tarl.hasNext()){
							Term tar = tarl.next();
							if (tar.getId().equals(tid)) {
								find = true;
							}
							ind ++;
						}
						if (find == true && chunk_length > termList.size()){
							chunkInfo[0] = label;
							chunkInfo[1] = chunkid;
							chunkInfo[2] = Integer.toString(ind);
							chunk_length = termList.size();	
						}
						}
					}
				}
			}
			
			return chunkInfo;
			
			/*ListIterator<Chunk> chunkl = nafFile.getChunks().listIterator();
			int chunk_length = 1000;
			String[] chunkInfo = {"O","c-1","0"};
			while (chunkl.hasNext()) {
				Chunk chunk = chunkl.next();
				List<Term> termList = chunk.getSpan().getTargets();
				ListIterator<Term> tarl = termList.listIterator();
				
				Boolean find = false;
				int ind = 0;
				while (find == false && tarl.hasNext()){
					Term tar = tarl.next();
					if (tar.getId().equals(wid)) {
						find = true;
					}
					ind ++;
				}
				if (find == true && chunk_length > termList.size()){
					chunkInfo[0] = chunk.getPhrase();
					chunkInfo[1] = chunk.getId();
					chunkInfo[2] = Integer.toString(ind);
					chunk_length = termList.size();	
				}
			}
			return chunkInfo;*/
		}
		
		private static HashMap<String,String> getIdChunk (TreeNode r, HashMap<String,String>idChunk){
			String prevId = r.getId();
			List<TreeNode> listNode = r.getChildren();
			for (TreeNode n : listNode){
				//if(n.isTerminal()){
				idChunk.put(n.getId(), prevId);	
				//}
				if(!n.isTerminal()){
					idChunk.putAll(getIdChunk(n, idChunk));
				}
			}
			
			return idChunk;
		}
		
		private static List<TreeNode> getListAllNodes(List<TreeNode> listNode, List<TreeNode> listAllNodes){
			//List<TreeNode> listAllNodes = new ArrayList<TreeNode> ();
			for(TreeNode n : listNode){
				listAllNodes.add(n);
				if(!n.isTerminal()){
					List<TreeNode> tmpList = new ArrayList<TreeNode> ();
					tmpList = getListAllNodes(n.getChildren(),listAllNodes);
					for (TreeNode ntmp : tmpList){
						if(!listAllNodes.contains(ntmp)){
							listAllNodes.add(ntmp);
						}
					}
				}
			}
			
			return listAllNodes;
		}
		 
		private static HashMap<String,String> getLabelNode(List<TreeNode> listNode, HashMap<String,String> const_id_label, String labelTmp){
			for (TreeNode n : listNode){
				if(!n.isTerminal()){
					//const_id_label.put(n.getId(),((NonTerminal) n).getLabel());
					Boolean hasChildren_terminal = false;
					Boolean temp = true;
					Boolean smaller = true;
					for(TreeNode nc : n.getChildren()){
						if (nc.isTerminal()){
							hasChildren_terminal = true;
						}
						if (!nc.isTerminal()){
							temp = false;
						}
						if (!nc.isTerminal() && ((NonTerminal)nc).getLabel().endsWith("P") && !((NonTerminal)nc).getLabel().equals("NNP")){
							smaller = false;
						}
					}
					
					if(!smaller){
						labelTmp = "";
					}
					else if(!hasChildren_terminal){
						labelTmp = ((NonTerminal) n).getLabel();
						if(!labelTmp.endsWith("P")){
							labelTmp = "";
						}
					}
					/*else if(hasChildren_terminal && !temp){
						labelTmp = "";
					}*/
					
					
					//label = ((NonTerminal) n).getLabel();
					//chunkid = n.getId();
					const_id_label.putAll(getLabelNode(n.getChildren(),const_id_label,labelTmp));
				}
				else{
					const_id_label.put(n.getId(), labelTmp);
					//labelTmp = "";
				}
			}
			
			return const_id_label;
		}
		
		/**
		 * Get Entity from a word id (wid)
		 * @param wid
		 * @param nafFile
		 * @return Entity or null
		 */
		//private static Entity getEntity (String wid, KAFDocument nafFile) {
		private static HashMap<String, Entity> getEntity (KAFDocument nafFile) {
			HashMap<String, Entity> listEntity = new HashMap<String, Entity> ();
			ListIterator<Entity> entl = nafFile.getEntities().listIterator();
			while (entl.hasNext()) {
				Entity ent = entl.next();
				ListIterator<Term> tarl = ent.getTerms().listIterator();
				
				while (tarl.hasNext()) {
					Term tar = tarl.next();
					listEntity.put(tar.getId(), ent);
				}
			}
			return listEntity;
		}
		
		
		/**
		 * Get the event type of a predicate
		 * @param pred
		 * @return
		 */
		private static String getEventType (Predicate pred){
			String eventType = "";
			ListIterator<ExternalRef> exRefl = pred.getExternalRefs().listIterator();
			while(exRefl.hasNext()){
				ExternalRef exRef = exRefl.next();
				if(exRef.getResource().equals("EventType")){
					if(eventType == "" || !eventType.equals("communication")){
						eventType = exRef.getReference();
					}
				}
			}
			return eventType;
		}

		/**
		 * Get a Predicate from a word id (wid)
		 * @param wid
		 * @param nafFile
		 * @return Predicate or null
		 */
		private static Predicate getPredicate (String wid, KAFDocument nafFile, int sent) {
			//ListIterator<Predicate> predl = nafFile.getPredicates().listIterator();
			//ListIterator<Predicate> predl = nafFile.getPredicatesBySent(sent).listIterator();
			List<Predicate> pl = nafFile.getPredicatesBySent(sent);
			if(pl != null){
				ListIterator<Predicate> predl = pl.listIterator();
				while (predl.hasNext()) {
					Predicate pred = predl.next();
					ListIterator<Term> tarl = pred.getTerms().listIterator();
					
					while (tarl.hasNext()) {
						Term tar = tarl.next();
						if (tar.getId().equals(wid)) {
							return pred;
						}
					}
				}
			}
			return null;
		}
		
		/** 
		 * Get Predicates
		 * @param wid
		 * @param nafFile
		 * @return Predicate or null
		 */
		//private static Coref getCoreferences (String wid, KAFDocument nafFile) {
		private static HashMap<String,Predicate> getPredicates (KAFDocument nafFile) {
			HashMap<String,Predicate> idTermPred = new HashMap<String,Predicate> ();
			
			ListIterator<Predicate> predl = nafFile.getPredicates().listIterator();
			while (predl.hasNext()) {
				Predicate pred = predl.next();
				Span<Term> s = pred.getSpan();
				ListIterator<Term> tarl = s.getTargets().listIterator();

				while (tarl.hasNext()) {
					Term tar = tarl.next();
					idTermPred.put(tar.getId(), pred);
				}
			}
			return idTermPred;
		}
		
		/**
		 * Get a Coreferences from a term id (wid)
		 * @param wid
		 * @param nafFile
		 * @return Predicate or null
		 */
		//private static Coref getCoreferences (String wid, KAFDocument nafFile) {
		private static HashMap<String,Coref> getCoreferences (KAFDocument nafFile) {
			HashMap<String,Coref> idTermCoref = new HashMap<String,Coref> ();
			
			ListIterator<Coref> corefl = nafFile.getCorefs().listIterator();
			while (corefl.hasNext()) {
				Coref co = corefl.next();
				if ((co.getType() != null && co.getType().equals("event")) || (co.getId().contains("event"))){
				
					ListIterator<Span<Term>> spanl = co.getSpans().listIterator();
					while(spanl.hasNext()){
						Span<Term> s = spanl.next();
						ListIterator<Term> tarl = s.getTargets().listIterator();
					
				
						while (tarl.hasNext()) {
							Term tar = tarl.next();
							idTermCoref.put(tar.getId(), co);
						}
					}
				}
			}
			return idTermCoref;
		}
		
		/**
		 * Get the Predicate to which a word "wid" is associated
		 * @param wid
		 * @param nafFile
		 * @return Predicate or null
		 */
		private static Predicate getPredicateFromToken (String wid, KAFDocument nafFile, int sent) {
			//ListIterator<Predicate> predl = nafFile.getPredicatesBySent(sent).listIterator();
			List<Predicate> pl = nafFile.getPredicatesBySent(sent);
			if(pl != null){
				ListIterator<Predicate> predl = pl.listIterator();
				while (predl.hasNext()) {
					Predicate pred = predl.next();
					ListIterator<Role> tokl = pred.getRoles().listIterator();
					while (tokl.hasNext()){
						Role r = tokl.next();
						ListIterator<Term> tarl = r.getTerms().listIterator();
						
						while (tarl.hasNext()) {
							Term tar = tarl.next();
							if (tar.getId().equals(wid)) {
								return pred;
							}
						}
					}
					
				}
			}
			return null;
		}
		
		
		/**
		 * Get the semantic role of an argument (word "wid") associated to the predicate "pred" 
		 * @param pred
		 * @param wid
		 * @return SemRole or null
		 */
		private static String getSemRoleFromToken (Predicate pred, String wid) {
			ListIterator<Role> tokl = pred.getRoles().listIterator();
			while (tokl.hasNext()){
				Role r = tokl.next();
				ListIterator<Term> tarl = r.getTerms().listIterator();
				
				while (tarl.hasNext()) {
					Term tar = tarl.next();
					if (tar.getId().equals(wid)) {
							return r.getSemRole();
					}
				}
			}
			return null;
		}
		
		/**
		 * Get Dependency in which wid is the from argument
		 * @param wid
		 * @param nafFile
		 * @return Dependency or null
		 */
		//private static List<Dep> getDep (String wid, KAFDocument nafFile) {
		private static HashMap<String,List<Dep>> getDep (KAFDocument nafFile) {
			HashMap<String,List<Dep>> idWFDepL = new HashMap<String,List<Dep>> ();
			
			ListIterator<Dep> depl = nafFile.getDeps().listIterator();
			//List<Dep> depLWid = new ArrayList<Dep> ();
			while (depl.hasNext()) {
				Dep dep = depl.next();
				ListIterator<WF> tarl = dep.getFrom().getSpan().getTargets().listIterator();
				
				while (tarl.hasNext()){
					WF w = tarl.next();
					if(idWFDepL.containsKey(w.getId())){
						idWFDepL.get(w.getId()).add(dep);
					}
					else{
						List<Dep> depLWid = new ArrayList<Dep> ();
						depLWid.add(dep);
						idWFDepL.put(w.getId(), depLWid);
					}
				}
			}
			return idWFDepL;
		}
		
		/**
		 * Get TLINK
		 * @param nafFile
		 * @return TLINK or null
		 */
		//private static List<Dep> getDep (String wid, KAFDocument nafFile) {
		private static HashMap<String,List<TLink>> getTlink (KAFDocument nafFile) {
			HashMap<String,List<TLink>> idFromTlinkL = new HashMap<String,List<TLink>> ();
			ListIterator<TLink> tlinkl = getTLinks(nafFile).listIterator();
			//List<Dep> depLWid = new ArrayList<Dep> ();
			while (tlinkl.hasNext()) {
				TLink tlink = tlinkl.next();
				List<TLink> listT = new ArrayList<TLink> ();
				if(idFromTlinkL.containsKey(tlink.getFrom().getId())){
					listT = idFromTlinkL.get(tlink.getFrom().getId());
				}
				listT.add(tlink);
				idFromTlinkL.put(tlink.getFrom().getId(),listT);
			}
			return idFromTlinkL;
		}
		
		public static List<TLink> getTLinks(KAFDocument nafFile){
			 List<Annotation> temprels = nafFile.getLayer(Layer.TEMPORAL_RELATIONS);
			    List<TLink> tlinks = new ArrayList<TLink>();
			    for (Annotation temprel : temprels) {
			        if (temprel instanceof TLink) {
			          tlinks.add((TLink) temprel);
			        }
			    }
			    return tlinks;
		}
		
		/**
		 * Get the Timex3 object for which the token wid is part of the Timex3 extent
		 * @param wid: word id
		 * @param nafFile
		 * @return Timex3 or null
		 */
		//private static Timex3 getTimex3 (String wid, KAFDocument nafFile) {
		private static HashMap<String,Timex3> getTimex3 (KAFDocument nafFile) {
			HashMap<String,Timex3> idTermTimex3 = new HashMap<String, Timex3> ();
			ListIterator<Timex3> txl = nafFile.getTimeExs().listIterator();
			while (txl.hasNext()) {
				Timex3 tx3= txl.next();
				if(tx3.getSpan() != null && tx3.getSpan().getTargets() != null){
					ListIterator<WF> tarl = tx3.getSpan().getTargets().listIterator();
				
					while (tarl.hasNext()){
						WF t = tarl.next();
						idTermTimex3.put(t.getId(), tx3);
					}
				}
			}
			return idTermTimex3;
		}
		
		/**
		 * Get the DCT object
		 * @param nafFile
		 * @return Timex3 or null
		 */
		private static Timex3 getDCT (KAFDocument nafFile) {
			ListIterator<Timex3> txl = nafFile.getTimeExs().listIterator();
			while (txl.hasNext()) {
				Timex3 tx3= txl.next();
				if(tx3.getFunctionInDocument() != null && tx3.getFunctionInDocument().equals("CREATION_TIME")){
					return tx3;
				}
			}
			return null;
		}
		
		
		private static List<TreeNode> get_constituent_tree_non_terminals(TreeNode tn, List<TreeNode> listtn){
			for(TreeNode tc : tn.getChildren()){
				if(!tc.isTerminal()){
					List<TreeNode> list_tmp = new ArrayList<TreeNode> ();
					//System.out.println("add list: "+((NonTerminal)tc).getLabel());
					listtn.add(tc);
					list_tmp = get_constituent_tree_non_terminals(tc,listtn);
					for (TreeNode tt : list_tmp){
						if(!listtn.contains(tt)){
							listtn.add(tt);
						}
					}
					
				}
			}
			return listtn;
		}
		
		private static List<TreeNode> get_constituent_tree_terminals(TreeNode tn, List<TreeNode> listtn){
			if(!tn.isTerminal()){
				for(TreeNode tc : tn.getChildren()){
					if(tc.isTerminal()){
						listtn.add(tc);
					}
					else{
						List<TreeNode> list_tmp = new ArrayList<TreeNode> ();
						list_tmp = get_constituent_tree_terminals(tc,listtn);
						for (TreeNode tt: list_tmp){
							if(!listtn.contains(tt)){
								listtn.add(tt);
							}
						}
					}
				}
			}
			else{
				listtn.add(tn);
			}
			return listtn;
		}
		
		private static HashMap<String,String> get_list_edges_from_to (TreeNode tn, HashMap<String,String> hash){
			if(!tn.isTerminal()){
				for (TreeNode tc: tn.getChildren()){
					hash.put(tc.getId(), tn.getId());
					hash.putAll(get_list_edges_from_to(tc,hash));
				}
			}
			return hash;
		}
		
		private static HashMap<String,List<String>> get_list_edges_to_from (TreeNode tn, HashMap<String,List<String>> hash){
			if(!tn.isTerminal()){
			for (TreeNode tc: tn.getChildren()){
				if(hash.containsKey(tn.getId())){
					hash.get(tn.getId()).add(tc.getId());
				}
				else{
					List<String> list_tmp = new ArrayList<String> ();
					list_tmp.add(tc.getId());
					hash.put(tn.getId(),list_tmp);
				}
				hash.putAll(get_list_edges_to_from(tc,hash));
			}
			}
			return hash;
		}
		

		private static HashMap<String,List<String>> get_list_terms_id_terid (List<TreeNode> list_ter){
		    HashMap<String,List<String>> list_terms_id_terid = new HashMap<String,List<String>> ();
		    for (TreeNode tn : list_ter){
		    	List<String> list_tmp = new ArrayList<String> ();
		    	for(Term t : ((Terminal)tn).getSpan().getTargets()){
		    		list_tmp.add(t.getId());
		    	}
		        list_terms_id_terid.put(tn.getId(),list_tmp);
		    }
		    return list_terms_id_terid;
		}

		
		private static String get_id_nt_chunk_ter(String tid, 
				HashMap<String,TreeNode> list_chunk_constituent,HashMap<String,String>list_edges_from_to){
		    Boolean find = false;
		    String toid = list_edges_from_to.get(tid);
		    
		    while (find == false){
		        if (list_chunk_constituent.containsKey(toid)){
		            find = true;
		        }
		        else if (list_edges_from_to.containsKey(toid)){
		            toid = list_edges_from_to.get(toid);
		        }
		        else{
		            find = true;
		        }
		    }
		    
		    return toid;
		}
		
		private static List<String> get_terminal_terms(String chunkid, HashMap<String,TreeNode> list_chunk_constituent,
				HashMap<String,List<String>> list_edges_to_from, List<String> list_terms){
			
			for (String foid : list_edges_to_from.get(chunkid)){
		        if (! foid.startsWith("nter")){
		            list_terms.add(foid);
		        }
		        else{
		            list_terms = get_terminal_terms(foid, list_chunk_constituent, list_edges_to_from, list_terms);
		        }
			}
			return list_terms;
		}
		
		private static List<String> get_terms_id (List<String> list_terid, HashMap<String,List<String>> list_terms_id_terid){
		    List<String> list_terms_id = new ArrayList<String>();
		    for (String terid : list_terid){
		        list_terms_id.addAll(list_terms_id_terid.get(terid));
		    }
		    return list_terms_id;
		}

		
		private static HashMap<String,List<String>> constituency_tree (Tree t){
			HashMap<String,TreeNode> list_chunk_constituent = new HashMap<String,TreeNode> ();
		    HashMap<String,List<String>> list_chunks = new HashMap<String,List<String>>();
		   
		    List<TreeNode> list_constituent_non_terminals = new ArrayList<TreeNode> ();
		    List<TreeNode> list_constituent_terminals =  new ArrayList<TreeNode> ();
		    list_constituent_non_terminals = get_constituent_tree_non_terminals (t.getRoot(), list_constituent_non_terminals);
		    list_constituent_terminals = get_constituent_tree_terminals(t.getRoot(), list_constituent_terminals);
		    
		    HashMap<String,String> list_edges_from_to = new HashMap<String,String> ();
		    HashMap<String,List<String>> list_edges_to_from = new HashMap<String,List<String>> ();
		    list_edges_from_to = get_list_edges_from_to(t.getRoot(),list_edges_from_to);
		    list_edges_to_from = get_list_edges_to_from(t.getRoot(),list_edges_to_from);

		    HashMap<String,List<String>> list_terms_id_terid = new HashMap<String,List<String>>();
		    list_terms_id_terid = get_list_terms_id_terid(list_constituent_terminals);
		    
		    for (TreeNode nt : list_constituent_non_terminals){
	            if (((NonTerminal)nt).getLabel().matches("NP|VP|PP|ADJP|ADVP|CONJP|INTJ|LST|PRT|SBAR|UCP")){
		    	//if (((NonTerminal)nt).getLabel().matches("NP|VP|PP|ADJP|ADVP")){
	            	list_chunk_constituent.put(nt.getId(),nt);
	            }
		    }
		    
		    for (String chunkid : list_chunk_constituent.keySet()){
		    	List<String> list_terms = new ArrayList<String> ();
		    	list_terms = get_terminal_terms(chunkid,list_chunk_constituent,list_edges_to_from,list_terms);
		    	List<String> list_terms_id = get_terms_id(list_terms,list_terms_id_terid);
		 
		    	String label = ((NonTerminal)list_chunk_constituent.get(chunkid)).getLabel();
		    	
		    	for (String termid : list_terms_id){
		    		if (list_chunks.containsKey(chunkid+":"+label)){
		    			list_chunks.get(chunkid+":"+label).add(termid.replace("ter", "t"));
		    		}
                    else{
                    	List<String> list_tmp = new ArrayList<String> ();
                    	list_tmp.add(termid.replace("ter","t"));
                        list_chunks.put(chunkid+":"+label,list_tmp);
                    }
                }
		    }
		    return list_chunks;

		}
		
}

