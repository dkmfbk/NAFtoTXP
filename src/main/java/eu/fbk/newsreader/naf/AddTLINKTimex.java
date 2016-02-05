package eu.fbk.newsreader.naf;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class AddTLINKTimex {

    private static int colSentId = 0;
    private static int colTimexId = 0;
    private static int colTimex = 0;
    private static int colTimexVal = 0;
    private static int colEventId = 0;
    private static int colPairs = 0;
    private static int colMainVb = 0;
    private static int colPOS = 0;
    private static int colChunk = 0;
    private static int colTense = 0;
    private static int colEventClass = 0;
    private static int colLemma = 0;

    private static void init_col_id(HashMap<String, Integer> intCol) {
        if (intCol.containsKey("sentid")) {
            colSentId = intCol.get("sentid");
        }
        if (intCol.containsKey("timex_id")) {
            colTimexId = intCol.get("timex_id");
        }
        if (intCol.containsKey("timex_type")) {
            colTimex = intCol.get("timex_type");
        }
        if (intCol.containsKey("timex_value")) {
            colTimexVal = intCol.get("timex_value");
        }
        if (intCol.containsKey("event_id")) {
            colEventId = intCol.get("event_id");
        }
        if (intCol.containsKey("pairs")) {
            colPairs = intCol.get("pairs");
        }
        if (intCol.containsKey("main_verb")) {
            colMainVb = intCol.get("main_verb");
        }
        if (intCol.containsKey("POS")) {
            colPOS = intCol.get("POS");
        }
        if (intCol.containsKey("chunk")) {
            colChunk = intCol.get("chunk");
        }
        if (intCol.containsKey("tense+aspect+pol")) {
            colTense = intCol.get("tense+aspect+pol");
        }
        if (intCol.containsKey("predClass")) {
            colEventClass = intCol.get("predClass");
        }
        if (intCol.containsKey("lemma")) {
            colLemma = intCol.get("lemma");
        }
    }

    public static void add_tlinks_between_timexes
            (String[][] lines, int nbCol, HashMap<String, Integer> intCol) throws ParseException {

        init_col_id(intCol);

        List<timexStructure> list_timex = new ArrayList<timexStructure>();

        String dct_value = "";

        for (int i = 0; i < lines.length; i++) {

            if (lines[i][0] != null && lines[i][colTimexId] != null && lines[i][colTimexId].startsWith("tmx")
                    && lines[i][colTimex].startsWith("B-")) {
                if (lines[i][0].startsWith("DCT")) {
                    dct_value = lines[i][colTimexVal];
                } else {
                    timexStructure tmp = new timexStructure();
                    tmp.timexID = lines[i][colTimexId];
                    tmp.typeTimex = lines[i][colTimex];
                    tmp.value = lines[i][colTimexVal];
                    tmp.instance = "";
                    tmp.firstTok = "";
                    list_timex.add(tmp);
                }
            }
        }

        for (int i = 0; i < list_timex.size(); i++) {
            timexStructure timex = list_timex.get(i);
            if (timex.typeTimex.equals("B-DATE") || timex.typeTimex.equals("B-TIME")) {
                String value = timex.value;

                String tlink = "";

                if (value.matches("^[0-9]+-[0-9]+-[0-9]+")) {
                    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
                    java.util.Date d2 = sdf.parse(value);
                    java.util.Date d1 = sdf.parse(dct_value);
                    if (d1.after(d2)) {
                        tlink = "AFTER";
                    } else if (d1.equals(d2)) {
                        tlink = "IDENTITY";
                    } else {
                        tlink = "BEFORE";
                    }
                    for (int j = i + 1; j < list_timex.size(); j++) {
                        timexStructure timex2 = list_timex.get(j);
                        if (timex2.typeTimex.equals("B-DATE") || timex2.typeTimex.equals("B-TIME")) {
                            if (timex2.value.matches("^[0-9]+-[0-9]+-[0-9]+")) {
                                if (timex2.value.equals(value)) {
                                    System.out.println(timex2.timexID + "\t" + timex.timexID + "\t" + "IDENTITY");
                                    System.out.println(timex2.value + " --> " + value + " : " + "IDENTITY");
                                }
                            }
                        }
                    }
                } else if (value.matches("^[0-9]+$")) {
                    int dct_year = Integer.parseInt(dct_value.split("-")[0]);
                    int value_int = Integer.parseInt(value);
                    if (dct_year < value_int) {
                        tlink = "BEFORE";
                    } else if (dct_year > value_int) {
                        tlink = "AFTER";
                    }
                } else if (value.matches("^[0-9]+-[0-9]+$")) {
                    int dct_year = Integer.parseInt(dct_value.split("-")[0]);
                    int date_year = Integer.parseInt(value.split("-")[0]);
                    int dct_month = Integer.parseInt(dct_value.split("-")[1]);
                    int date_month = Integer.parseInt(value.split("-")[1]);

                    if (dct_year < date_year) {
                        tlink = "BEFORE";
                    } else if (dct_year > date_year) {
                        tlink = "AFTER";
                    } else if (dct_year == date_year) {
                        if (dct_month < date_month) {
                            tlink = "BEFORE";
                        } else if (dct_month > date_month) {
                            tlink = "AFTER";
                        }
                    }
                }

                if (!tlink.equals("")) {
                    System.out.println("t0\t" + timex.timexID + "\t" + tlink);
                    System.out.println(dct_value + " --> " + value + " : " + tlink);
                }
            }
        }

        int k = 0;
        for (int i = 0; i <= lines.length; i++) {

        }

        //return lines;
    }

    /**
     * @param args
     */
    public static void main(String[] args) {
        // TODO Auto-generated method stub

    }

}
