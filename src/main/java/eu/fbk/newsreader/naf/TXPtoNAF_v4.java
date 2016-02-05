/**
 * Anne-Lyse Minard
 * TXPtoNAF
 * Add tlinks and clinks layers in NAF.
 * use version 1.1.9 of kaflib (from EHU)
 */

package eu.fbk.newsreader.naf;

import ixa.kaflib.*;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;

public class TXPtoNAF_v4 {

    static final String JAXP_SCHEMA_LANGUAGE = "http://java.sun.com/xml/jaxp/properties/schemaLanguage";
    static final String JAXP_SCHEMA_LOCATION = "http://java.sun.com/xml/jaxp/properties/schemaSource";
    static final String W3C_XML_SCHEMA = "http://www.w3.org/2001/XMLSchema";

    static String NULLVALUE = "__NULL__";

    static HashMap<String, Integer> intCol = new HashMap<String, Integer>();

    static int endHeaderLine;

    static BufferedWriter buffout = null;
    static private String encodingOUT = "UTF8";

    public static void TXP2NAF(File f, String[][] lines, String beginTimeSpan, String eltName) throws IOException {
        KAFDocument nafFile = null;
        try {

            nafFile = KAFDocument.createFromFile(f);

        } catch (Exception e) {
            System.err.println("Errors when producing the NAF file");
            System.exit(2);
        }

        HashMap<String, String> termIdCoeventId = new HashMap<String, String>();
        HashMap<String, Coref> coeventIdElt = new HashMap<String, Coref>();
        HashMap<String, Timex3> timexIdElt = new HashMap<String, Timex3>();

        ListIterator<Coref> corefl = nafFile.getCorefs().listIterator();
        while (corefl.hasNext()) {
            Coref co = corefl.next();
            if (co.getId().startsWith("coevent")) {
                coeventIdElt.put(co.getId(), co);

                ListIterator<Span<Term>> spanl = co.getSpans().listIterator();
                while (spanl.hasNext()) {
                    Span<Term> s = spanl.next();
                    ListIterator<Term> tarl = s.getTargets().listIterator();

                    while (tarl.hasNext()) {
                        Term tar = tarl.next();
                        termIdCoeventId.put(tar.getId(), co.getId());
                    }
                }
            }
        }

        HashMap<String, String> termIdPredId = new HashMap<String, String>();
        HashMap<String, Predicate> predIdElt = new HashMap<String, Predicate>();

        ListIterator<Predicate> predl = nafFile.getPredicates().listIterator();
        while (predl.hasNext()) {
            Predicate pred = predl.next();
            predIdElt.put(pred.getId(), pred);

            Span<Term> s = pred.getSpan();
            ListIterator<Term> tarl = s.getTargets().listIterator();

            while (tarl.hasNext()) {
                Term tar = tarl.next();
                termIdPredId.put(tar.getId(), pred.getId());
            }
        }

        if (eltName.equals("TIMEX3")) {
            int cptTimex = 0;
            int space = 0;

            HashMap<String, Timex3> listTimexId = new HashMap<String, Timex3>();

            for (int i = 0; i < lines.length; i++) {
                if (lines[i][0] != null && lines[i][0].matches("# DATE: [0-9][0-9].*")) {
                    Timex3 tx = nafFile.newTimex3("tmx0", "DATE");
                    tx.setValue(lines[i][0].replace("# DATE: ", ""));
                    tx.setFunctionInDocument("CREATION_TIME");
                    listTimexId.put("tmx0", tx);
                }

                if (lines[i][0] != null && lines[i].length > 10 && lines[i][7] != null && !lines[i][0]
                        .startsWith("# ")) {
                    if (lines[i][9] != null && lines[i][9].startsWith("tmx") && lines[i][7].startsWith("B-")) {
                        String timextype = lines[i][7].replace("B-", "");
                        String value = lines[i][10];

                        if (!value.equals("")) {
                            Timex3 tx = nafFile.newTimex3(lines[i][9], timextype);
                            tx.setValue(value);
                            listTimexId.put(lines[i][9], tx);

                            if (lines[i][11] != null && !lines[i][11].equals("_NULL_") && !lines[i][11].equals("null")
                                    && !lines[i][11].equals("-")) {
                                tx.setAnchorTimeId(lines[i][11]);
                            }

                            if (lines[i][12] != null && !lines[i][12].equals("_NULL_") && !lines[i][12]
                                    .equals("null")) {
                                if (listTimexId.containsKey(lines[i][12])) {
                                    tx.setBeginPoint(listTimexId.get(lines[i][12]));
                                }
                            }

                            if (lines[i][13] != null && !lines[i][13].equals("_NULL_") && !lines[i][13]
                                    .equals("null")) {
                                if (listTimexId.containsKey(lines[i][13])) {
                                    tx.setEndPoint(listTimexId.get(lines[i][13]));
                                }
                            }

                            if (!lines[i][0].equals("ETX")) {
                                List<WF> list_wf = new ArrayList<WF>();
                                list_wf.add(nafFile.getWFs().get(i - space));

                                if (i + 1 < lines.length && lines[i + 1] != null && lines[i + 1].length > 10
                                        && lines[i + 1][0] != null && lines[i + 1][7] != null
                                        && lines[i + 1][7].startsWith("I-")) {
                                    for (int j = i + 1; j < lines.length; j++) {
                                        if (lines[j] != null && lines[j][7] != null && lines[j][7].startsWith("I-")) {
                                            list_wf.add(nafFile.getWFs().get(j - space));

                                        } else {
                                            break;
                                        }
                                    }
                                }

                                if (list_wf.size() > 0) {
                                    Span<WF> span = KAFDocument.newWFSpan();
                                    span.addTargets(list_wf);
                                    tx.setSpan(span);
                                    //tx.getSpan().addTargets(list_wf);
							    	/*for (int l=0; l<list_wf.size(); l++){
							    		tx.getSpan().addTarget(list_wf.get(l));
							    	}*/
                                }
                            }
                        }

                        cptTimex++;
                    }
                } else {
                    space += 1;
                }
            }

            LinguisticProcessor lp = nafFile.addLinguisticProcessor("timeExpressions", "TimePro-FBK");
            lp.setVersion("v1.5, based on Yamcha");
            lp.setBeginTimestamp(beginTimeSpan);
            lp.setEndTimestamp(getTodayDate());
        }

        if (eltName.equals("TLINK")) {
            ListIterator<Timex3> timexl = nafFile.getTimeExs().listIterator();
            while (timexl.hasNext()) {
                Timex3 tx = timexl.next();
                timexIdElt.put(tx.getId(), tx);
            }

            List<String> list_tlink_temp = new ArrayList<String>();

            //nafFile.removeLayer(KAFDocument.Layer.tlinks);
            int cptTlink = 0;
            for (int i = 0; i < lines.length; i++) {
                String from = lines[i][0];
                String to = lines[i][1];
                TLinkReferable newfrom = null;
                TLinkReferable newto = null;

                if (from != null && !from.equals("") && to != null && !to.equals("")) {

                    if (termIdPredId.containsKey(from.replace("e", "t"))) {
                        from = termIdPredId.get(from.replace("e", "t"));
                        newfrom = predIdElt.get(from);
                    } else {
                        newfrom = timexIdElt.get(from);
                    }

                    if (termIdPredId.containsKey(to.replace("e", "t"))) {
                        to = termIdPredId.get(to.replace("e", "t"));
                        newto = predIdElt.get(to);
                    } else {
                        newto = timexIdElt.get(to);
                    }

                    String tlinkid = "tlink" + Integer.toString(cptTlink);
                    String relType = lines[i][lines[i].length - 1];
                    String fromType = "event";
                    String toType = "event";

                    if (from.startsWith("tmx")) {
                        fromType = "timex";
                    }
                    if (to.startsWith("tmx")) {
                        toType = "timex";
                    }

                    String tlink_string = from + ":" + to + ":" + relType;
                    String inverse_tlink_string = to + ":" + from + ":" + getReverseRelation(relType);

                    if (!list_tlink_temp.contains(inverse_tlink_string)) {
                        list_tlink_temp.add(tlink_string);

                        if (relType != null && !relType.equals("O")) {
                            nafFile.newTLink(tlinkid, newfrom, newto, relType);
                        }
                    }

                    cptTlink++;
                }
            }

            LinguisticProcessor lp = nafFile.addLinguisticProcessor("temporalRelations", "TempRelPro-FBK");
            lp.setVersion("2.5.0");
            lp.setBeginTimestamp(beginTimeSpan);
            lp.setEndTimestamp(getTodayDate());
        }

        if (eltName.equals("CLINK")) {

            int cptClink = 0;
            for (int i = 0; i < lines.length; i++) {
                String from = lines[i][0];
                String to = lines[i][1];
                if (from != null && !from.equals("") && to != null && !to.equals("")) {

                    if (termIdPredId.containsKey(from.replace("e", "t"))) {
                        from = termIdPredId.get(from.replace("e", "t"));
                    }
                    if (termIdPredId.containsKey(to.replace("e", "t"))) {
                        to = termIdPredId.get(to.replace("e", "t"));
                    }

                    String clinkid = "clink" + Integer.toString(cptClink);
                    String relType = lines[i][lines[i].length - 2];
                    String orderRel = lines[i][lines[i].length - 1];

                    if (orderRel.equals("CLINK-R")) {
                        String temp = from;
                        from = to;
                        to = temp;
                    }

                    if (!relType.matches("[A-Z]+")) {
                        relType = "CAUSE";
                    }

                    if (relType.equals("O")) {
                        relType = "";
                    }

                    if (relType != null && !relType.equals("O")) {
                        nafFile.newCLink(clinkid, predIdElt.get(from), predIdElt.get(to));
                    }

                    cptClink++;
                }
            }

            LinguisticProcessor lp = nafFile.addLinguisticProcessor("causalRelations", "CausalRelPro-FBK");
            lp.setVersion("2.0.0");
            lp.setBeginTimestamp(beginTimeSpan);
            lp.setEndTimestamp(getTodayDate());
        }

        new PrintStream(System.out, true, "UTF-8").print(nafFile.toString());

    }

    private static String getReverseRelation(String relType) {
        if (relType.equals("BEFORE")) {
            return "AFTER";
        } else if (relType.equals("AFTER")) {
            return "BEFORE";
        } else if (relType.equals("IS_INCLUDED")) {
            return "INCLUDES";
        } else if (relType.equals("INCLUDES")) {
            return "IS_INCLUDED";
        } else if (relType.equals("BEGUN_BY")) {
            return "BEGINS";
        } else if (relType.equals("BEGINS")) {
            return "BEGUN_BY";
        } else if (relType.equals("ENDED_BY")) {
            return "ENDS";
        } else if (relType.equals("ENDS")) {
            return "ENDED_BY";
        } else if (relType.equals("IAFTER")) {
            return "IBEFORE";
        } else if (relType.equals("IBEFORE")) {
            return "IAFTER";
        } else if (relType.equals("SIMULTANEOUS")) {
            return "SIMULTANEOUS";
        }
        return "NONE";
    }

    public static String getTodayDate() {
        DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ");
        Date date = new Date();
        String dateString = dateFormat.format(date).toString();
        return dateString;
    }

    /**
     * @param args
     * @throws IOException
     */
    public static void main(String[] args) throws IOException {
        // TODO Auto-generated method stub
        int nbCol = 3;
        String beginTimeSpan = Long.toString(System.currentTimeMillis());
        if (args.length > 2) {
            beginTimeSpan = args[2];
        }

        String eltName = "TLINK";
        if (args.length > 3) {
            eltName = args[3];
            if (eltName.equals("CLINK")) {
                nbCol += 1;
            } else if (eltName.equals("TIMEX3")) {
                nbCol = 14;
            }
        }
        //read file containing the list of TLINK or CLINK
        String[][] lines = TextProFileFormat.readFileTextPro(args[1], nbCol, false);
        //add tlinks or clinks into NAF
        TXP2NAF(new File(args[0]), lines, beginTimeSpan, eltName);

        System.exit(1);
    }

}
