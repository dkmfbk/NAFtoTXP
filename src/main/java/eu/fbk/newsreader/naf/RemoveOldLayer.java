package eu.fbk.newsreader.naf;

public class RemoveOldLayer {

//	public static void main(String[] args) throws IOException, JDOMException {
//		//BufferedReader br = new BufferedReader(new FileReader(args[2]));
//		BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
//		//String fileNameOut = args[0];
//		String layers = args[0];
//		String [] list_layers = null;
//		if(layers.contains("\\+")){
//			list_layers = layers.split("\\+");
//		}
//		else{
//			list_layers = new String [1];
//			list_layers[0] = layers;
//		}
//
//		KAFDocument nafFile = KAFDocument.createFromStream(br);
//
//		for(int i=0; i<list_layers.length; i++){
//			if (list_layers[i].equals("srl")){
//				nafFile.removeLayer(KAFDocument.Layer.SRL);
//				//nafFile.removeLayer(KAFDocument.Layer.DEPS);
//			}
//			else if(list_layers[i].equals("tlink")){
//				nafFile.removeLayer(KAFDocument.Layer.TEMPORAL_RELATIONS);
//			}
//			else if(list_layers[i].equals("clink")){
//				nafFile.removeLayer(KAFDocument.Layer.CAUSAL_RELATIONS);
//			}
//			else if(list_layers[i].equals("entities")){
//				nafFile.removeLayer(KAFDocument.Layer.ENTITIES);
//			}
//		}
//
//		System.out.println(nafFile.toString());
//
//	}

}
