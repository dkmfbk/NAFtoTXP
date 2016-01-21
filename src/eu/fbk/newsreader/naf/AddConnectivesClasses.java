package eu.fbk.newsreader.naf;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.ListIterator;
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import ixa.kaflib.*;

public class AddConnectivesClasses {
	
	public static List<String> get_synt_tree (KAFDocument nafFile){
		List<String> syntTreeL = new ArrayList<String> ();
		List<Tree> treeL = nafFile.getConstituents();
		for (Tree t : treeL){
			String treeBracket = readChildTree (t.getRoot(), "");
			syntTreeL.add(treeBracket);
		}
		
		return syntTreeL;
	}
	

	private static String readChildTree (TreeNode tn, String treeBr){
		ListIterator<TreeNode> edgesL = tn.getChildren().listIterator();
		while(edgesL.hasNext()){
			TreeNode t = edgesL.next();
			if(t.isTerminal()){
				treeBr += ((Terminal) t).getStr();
				//System.out.print("terminal");
			}
			else{
				treeBr += "("+((NonTerminal) t).getLabel()+" ";
				treeBr = readChildTree(t,treeBr);
				treeBr += ") ";
			}
		}
		return treeBr;
	}
	
	private static HashMap<String,Integer> get_list_term_id_sentence (List<List<String>> wfIdBySentL, HashMap<String,String> wfTermL){
		HashMap<String,Integer> termIdSent = new HashMap<String,Integer> ();
		for (int j=0; j<wfIdBySentL.size();j++){
			for (String wfId : wfIdBySentL.get(j)){
				termIdSent.put(wfTermL.get(wfId), j);
			}
		}
		return termIdSent;
	}
	
	private static List<List<String>> get_terms_by_sentence(List<List<String>> wfIdBySentL, HashMap<String,String> wfTermL){
		List<List<String>> termsBySent = new ArrayList<List<String>> ();
		for (int j=0; j<wfIdBySentL.size();j++){
			List<String> temp = new ArrayList<String> ();
			for (String wfId : wfIdBySentL.get(j)){
				temp.add(wfTermL.get(wfId));
			}
			termsBySent.add(temp);
		}
		return termsBySent;
	}
	
	private static HashMap<String,String> get_list_word_term (List<Term> termL){
		HashMap<String,String> wfTermL = new HashMap<String,String>();
		for (Term t : termL){
			for (WF w : t.getWFs()){
				wfTermL.put(w.getId(), t.getId());
			}
		}
		return wfTermL;
	}
	
	private static HashMap<String,List<String>> get_list_term_word (List<Term> termL){
		HashMap<String,List<String>> termWfL = new HashMap<String,List<String>>();
		for (Term t : termL){
			List<String> temp = new ArrayList<String> ();
			for (WF w : t.getWFs()){
				temp.add(w.getId());
			}
			termWfL.put(t.getId(), temp);
		}
		return termWfL;
	}

	
	private static List<List<String>> get_list_words_by_sentence (List<WF> wfL){
		List<List<String>> listWfsSent = new ArrayList ();
		for (WF wf : wfL){
			int numSent = wf.getSent();
			if (listWfsSent.size() < numSent){
				List <String> temp = new ArrayList();
				listWfsSent.add(temp);
			}
			listWfsSent.get(numSent-1).add(wf.getId());
		}
		
		return listWfsSent;
	}
	
	private static HashMap<String,String> get_words_by_id(List<WF> wfL){
		HashMap<String,String> wfIdL = new HashMap ();
		for (WF wf : wfL){
			wfIdL.put(wf.getId(), wf.getForm());
		}
		return wfIdL;
	}
	
	private static void writeSyntTree (String f, List<String> syntTreeL){
		try{
			File fout = new File(f);
			if (!fout.exists()) {
				fout.createNewFile();
			}

			FileWriter fw = new FileWriter(fout.getAbsoluteFile());
			BufferedWriter bw = new BufferedWriter(fw);
			

			for (int i=0; i<syntTreeL.size(); i++){
				bw.write(syntTreeL.get(i)+"\n");
			}
			
			bw.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	
	private static void processAddDiscourse(String ftmp){
		try{
			String current_path = NAFtoTXP_v11.class.getProtectionDomain().getCodeSource().getLocation().getPath();
			current_path = current_path.substring(0, current_path.lastIndexOf("/"));
			current_path = current_path.replace("/lib", "");
			String [] cmd = new String [] {"perl", current_path+"/tools/addDiscourse/addDiscourse.pl", 
					"--parses", ftmp, "--output", ftmp};
			
			/*for (int i = 0; i < cmd.length; i++)
				System.out.print(cmd[i] + " ");
			System.out.println ();*/
			
			Process p = Runtime.getRuntime().exec(cmd);
			StreamGobbler errorGobbler = new 
	                StreamGobbler(p.getErrorStream(), "ERROR");
			StreamGobbler outputGobbler = new 
	                StreamGobbler(p.getInputStream(), "OUTPUT");
			errorGobbler.start();
	        outputGobbler.start();
	        
			p.waitFor();
		}
		 catch (Throwable t)
	      {
	        t.printStackTrace();
	      }
	}
	
	private static HashMap<String,String> get_connectives(String ftmp){
		HashMap<String,String> listConnectives = new HashMap<String,String> ();
		try{
			InputStream ips=new FileInputStream(ftmp); 
			InputStreamReader ipsr=new InputStreamReader(ips);
			BufferedReader br=new BufferedReader(ipsr);
			String line = "";
			int cpt = 1;
			while ((line=br.readLine())!=null){
				Pattern p = Pattern.compile("\\([^\\(\\)]+ ([^\\)\\(]+)\\)");
				Matcher m = p.matcher(line);
				int cptTerm = 1;
				while(m.find()){
					if (m.group(1).endsWith("#0")){}
					else if(m.group(1).contains("#")){
						String [] info = (m.group(1)).split("#");
						if(info.length > 2 && info[1].matches("[0-9]+")){
							listConnectives.put(cpt+":"+cptTerm+":"+info[0],info[2]);
						}
					}
					cptTerm ++;
				}
				cpt ++;
			}
			br.close(); 
		}		
		catch (Exception e){
			System.out.println(e.toString());
		}
		
		return listConnectives;
	}
	
	
	public static HashMap<String,String> getListConnectives (KAFDocument nafFile) throws IOException, InterruptedException {
		// TODO Auto-generated method stub
		//File f = new File(args[0]);
		//KAFDocument nafFile = KAFDocument.createFromFile(f);
		
		ListIterator<WF> tokenList = nafFile.getWFs().listIterator(); 
		
		List<String> syntTreeL = get_synt_tree(nafFile);
		
		Random random = new Random();
		int START = 0;
		int END = 10000;
		int randomNum = random.nextInt((END - START) + 1) + END;
		
		String filein = "/tmp/"+randomNum+"-parsed.tmp"; 
		writeSyntTree (filein,syntTreeL);
		
		processAddDiscourse (filein);
		
		HashMap<String,String> listConnectives = get_connectives(filein); 
		
		try{
			File filetmp = new File(filein);
			filetmp.delete();
		}
		catch(Exception e){
			e.printStackTrace();
		}
			
		return listConnectives;
	}
	
	
	/**
	 * @param args
	 * @throws IOException 
	 * @throws InterruptedException 
	 */
	/*public static void main(String[] args) throws IOException, InterruptedException {
		// TODO Auto-generated method stub
		File f = new File(args[0]);
		KAFDocument nafFile = KAFDocument.createFromFile(f);
		
		ListIterator<WF> tokenList = nafFile.getWFs().listIterator(); 
		
		List<String> syntTreeL = get_synt_tree(nafFile);

		writeSyntTree ("tmp/parsed.tmp",syntTreeL);
		
		processAddDiscourse ("tmp/parsed.tmp");
		
		HashMap<String,String> listConnectives = get_connectives("tmp/parsed.tmp"); 
		for (String k : listConnectives.keySet()){
			System.out.println(k+"\t"+listConnectives.get(k));
		}
	}*/

}



class StreamGobbler extends Thread {
    InputStream is;
    String type;
    OutputStream os;
    
    StreamGobbler(InputStream is, String type)
    {
        this(is, type, null);
    }
    
    StreamGobbler(InputStream is, String type, OutputStream redirect)
    {
        this.is = is;
        this.type = type;
        this.os = redirect;
    }
    
    public void run()
    {
        try
        {
            PrintWriter pw = null;
            if (os != null)
                pw = new PrintWriter(os);
                
            InputStreamReader isr = new InputStreamReader(is);
            BufferedReader br = new BufferedReader(isr);
            String line=null;
            while ( (line = br.readLine()) != null)
            {
                if (pw != null){
                    pw.println(line);
                    pw.flush();
                }
                //System.out.println(type + ">" + line);    
            }
            if (pw != null){
                
            	pw.close();
            }
        } catch (IOException ioe)
            {
            ioe.printStackTrace();  
            }
    }
}
