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

public class TextProFileFormat {
	
	/* 
	 * write the content of lines in column format
	 */
	public static void writeTextProFile (String [][] lines, String fileNameOut, int nbCol){
		StringBuffer content = new StringBuffer();
		for(int i=0; i<lines.length; i++){
			if(lines[i][0] != null && (lines[i][1] != null || !lines[i][0].startsWith("#"))){
				for(int j=0; j<nbCol; j++){
					if(lines[i][j] == null){
						content.append("_NULL_\t");
					}
					else{
						content.append(lines[i][j]+"\t");
					}
				}
			}
			else if (lines[i][0] != null){
				content.append(lines[i][0]);
			}
			content.append("\n");
		}
		
		
		try {
			File file = new File(fileNameOut);
 
			// if file doesnt exists, then create it
			if (!file.exists()) {
				file.createNewFile();
			}
 
			FileWriter fw = new FileWriter(file.getAbsoluteFile());
			BufferedWriter bw = new BufferedWriter(fw);
			//bw.write(content);
			bw.write(content.toString());
			bw.close();
 
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	/*
	 * Read file with one token by line and information in column format (as TextPro format)
	 */
	public static String [][] readFileTextPro (String fileName, int nbCol, boolean onlyCol){
		String [][] lines = null;
		int i=0;
		try{
			InputStream ips=new FileInputStream(fileName); 
			InputStreamReader ipsr=new InputStreamReader(ips);
			BufferedReader br=new BufferedReader(ipsr);
			String line;
			
			LineNumberReader reader = new LineNumberReader(new FileReader(fileName));
		    while ((reader.readLine()) != null);
		    lines = new String [reader.getLineNumber()][nbCol];
		    reader.close();
			
			while ((line=br.readLine())!=null){
				String [] tok = new String [nbCol];
				if(!line.matches("^# .*")){
					int j=0;
					String [] col = line.split("\t");
					for(int k=0; k<nbCol && k<col.length; k++){
					//for(String t : line.split("\t")){
						tok[j] = col[k];
						j++;
					}
				}
				else if(!onlyCol){
					tok[0] = line;
				}
				lines[i] = tok;
				i++;
			}
			br.close(); 
		}		
		catch (Exception e){
			System.out.println(e.toString());
		}
		return lines;
	}
	
	
}
