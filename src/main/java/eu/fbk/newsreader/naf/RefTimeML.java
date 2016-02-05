package eu.fbk.newsreader.naf;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class RefTimeML {

    static HashMap<Integer, eventStructure> listEvent = new HashMap<Integer, eventStructure>();
    static HashMap<Integer, timexStructure> listTimex = new HashMap<Integer, timexStructure>();
    static HashMap<String, List<linkStructure>> listTlink = new HashMap<String, List<linkStructure>>();
    //static HashMap<String,List<String>> tlink = new HashMap<String, List<String>>();

    static HashMap<String, List<linkStructure>> listSlink = new HashMap<String, List<linkStructure>>();
    static HashMap<String, List<linkStructure>> listAlink = new HashMap<String, List<linkStructure>>();

    /**
     * @param args
     */
    public static void main(String[] args) {
        // TODO Auto-generated method stub

    }

    private static String[][] addTimexRefToTXP(String[][] lines, timexStructure dct, int colTimex, int valTimex,
            int idTimex) {
        lines = addDCTRef(lines, dct, idTimex, colTimex, valTimex);

        int i = 0;
        int it = 0;

        for (int j = 0; j < lines.length; j++) {
            boolean match = true;
            /* if training corpus then add the timex ref annotation */
            if (it < listTimex.size() && lines[j][0] != null && lines[j][0].contentEquals(listTimex.get(it).firstTok)
                    && listTimex.get(it).instance != null) {
                if (listTimex.get(it).instance.contains(" ")) {
                    String[] tokTimex = listTimex.get(it).instance.split(" ");
                    for (int k = 0; k < tokTimex.length; k++) {
                        if (lines.length <= (j + k) || (lines[j + k][0] != null && !lines[j + k][0]
                                .equals(tokTimex[k]))) {
                            match = false;
                        }
                    }

                    if (match) {
                        for (int k = 0; k < tokTimex.length; k++) {
                            String prefix = "I-";
                            if (k == 0) {
                                prefix = "B-";
                            }
                            lines[j + k][colTimex] = prefix + listTimex.get(it).typeTimex;
                            lines[j + k][valTimex] = listTimex.get(it).value;
                            lines[j + k][idTimex] = listTimex.get(it).timexID.replace("t", "tmx");
                        }
                    } else if (lines[j][colTimex] == null) {
                        lines[j][colTimex] = "O";
                        lines[j][valTimex] = "O";
                        lines[j][idTimex] = "O";
                    }
                } else if (match) {
                    lines[j][colTimex] = "B-" + listTimex.get(it).typeTimex;
                    lines[j][valTimex] = listTimex.get(it).value;
                    lines[j][idTimex] = listTimex.get(it).timexID.replace("t", "tmx");
                } else {
                    lines[j][colTimex] = "O";
                    lines[j][valTimex] = "O";
                    lines[j][idTimex] = "O";
                }
                if (match) {
                    it++;
                }
            } else if (lines[j][colTimex] == null) {
                lines[j][colTimex] = "O";
                lines[j][valTimex] = "O";
                lines[j][idTimex] = "O";
            }
        }

        if (it < listTimex.size()) {
            for (int j = it; j < listTimex.size(); j++) {
                System.out.println(listTimex.get(j).instance);
            }
        }

        return lines;
    }

    private static String[][] addEventRefToTXP(String[][] lines, int event_id, int event_class, int idTimex) {
        int i = 0;
        int it = 0;

        for (int j = 0; j < lines.length; j++) {
            boolean match = true;
			/* if training corpus then add the timex ref annotation */
            if (it < listEvent.size() && lines[j][0] != null && lines[j][0].contentEquals(listEvent.get(it).instance)
                    && (lines[j][idTimex] == null || lines[j][idTimex].equals("O"))) {

                lines[j][event_id] = listEvent.get(it).eventID;
                lines[j][event_class] = listEvent.get(it).classEvent;

                it++;
            } else if (it < listEvent.size() && lines[j][0] != null && listEvent.get(it).instance.matches(".* .*")) {
                String[] event_tok = listEvent.get(it).instance.split(" ");
                if (lines[j][0].contentEquals(event_tok[0]) && lines[j + 1][0] != null && lines[j + 1][0]
                        .contentEquals(event_tok[1])) {
                    lines[j][event_id] = listEvent.get(it).eventID;
                    lines[j][event_class] = listEvent.get(it).classEvent;

                    lines[j + 1][event_id] = listEvent.get(it).eventID;
                    lines[j + 1][event_class] = listEvent.get(it).classEvent;

                    it++;
                } else if (lines[j][event_id] == null) {
                    lines[j][event_id] = "O";
                    lines[j][event_class] = "O";
                }
            } else if (lines[j][event_id] == null) {
                lines[j][event_id] = "O";
                lines[j][event_class] = "O";
            }
        }

        if (it < listEvent.size()) {
            for (int j = it; j < listEvent.size(); j++) {
                System.out.println(listEvent.get(j).instance);
            }
        }

        return lines;
    }

    private static String[][] addEventRefTrainToTXP(String[][] lines, int event_class, int event) {
        int i = 0;
        int it = 0;

        for (int j = 0; j < lines.length; j++) {
            boolean match = true;
			/* if training corpus then add the event ref annotation */
            if (it < listEvent.size() && lines[j][0] != null && lines[j][0].contentEquals(listEvent.get(it).instance)) {

                lines[j][event] = "EVENT";
                lines[j][event_class] = listEvent.get(it).classEvent;

                it++;
            } else if (it < listEvent.size() && lines[j][0] != null && listEvent.get(it).instance.matches(".* .*")) {
                String[] event_tok = listEvent.get(it).instance.split(" ");
                if (lines[j][0].contentEquals(event_tok[0]) && lines[j + 1][0] != null && lines[j + 1][0]
                        .contentEquals(event_tok[1])) {
                    lines[j][event] = "EVENT";
                    lines[j][event_class] = listEvent.get(it).classEvent;

                    lines[j + 1][event] = "EVENT";
                    lines[j + 1][event_class] = listEvent.get(it).classEvent;

                    it++;
                } else if (lines[j][event] == null) {
                    lines[j][event] = "O";
                    lines[j][event_class] = "O";
                }
            } else if (lines[j][event] == null) {
                lines[j][event] = "O";
                lines[j][event_class] = "O";
            }
        }

        if (it < listEvent.size()) {
            System.out.println("problem annotation events");
            for (int j = it; j < listEvent.size(); j++) {
                System.out.println(listEvent.get(j).instance);
            }
        }

        return lines;
    }

    private static List<HashMap<String, List<linkStructure>>> get_tlink_none(String[][] lines, int colPairs,
            String typeCorpus,
            HashMap<String, List<linkStructure>> listLink) {
        List<linkStructure> listTlinkAuto = new ArrayList<linkStructure>();
        List<linkStructure> listTlinkAutoRef = new ArrayList<linkStructure>();
        HashMap<String, List<linkStructure>> listTlinkNone = new HashMap<String, List<linkStructure>>();
        for (int i = 0; i < lines.length; i++) {
            if (lines[i][colPairs] != null && lines[i][colPairs].contains(":")) {
                String[] autoTlink = lines[i][colPairs].split("\\|\\|");
                if (autoTlink.length == 0) {
                    autoTlink[0] = lines[i][colPairs];
                }
                for (int j = 0; j < autoTlink.length; j++) {
                    String t = autoTlink[j];
                    String[] elt = t.split(":");
                    if (elt.length == 3) {
                        linkStructure tlinkTemp = new linkStructure();
                        tlinkTemp.relType = "NONE";
                        tlinkTemp.source = elt[0];
                        tlinkTemp.target = elt[1];
                        if (listLink.containsKey(elt[0])) {
                            for (linkStructure tlink : listLink.get(elt[0])) {
                                if (tlink.target.equals(elt[1])) {
                                    listTlinkAutoRef.add(tlinkTemp);
                                }
                            }
                        }
                        if (listLink.containsKey(elt[1])) {
                            for (linkStructure tlink : listLink.get(elt[1])) {
                                if (tlink.target.equals(elt[0])) {
                                    tlinkTemp.source = elt[1];
                                    tlinkTemp.target = elt[0];
                                    listTlinkAutoRef.add(tlinkTemp);
                                }
                            }
                        }
                        listTlinkAuto.add(tlinkTemp);
                    }
                }
            }
        }

        for (linkStructure tlink : listTlinkAuto) {
            if (!listTlinkAutoRef.contains(tlink)) {
                List<linkStructure> listTemp = new ArrayList<linkStructure>();
                if (listTlinkNone.containsKey(tlink.source)) {
                    listTemp = listTlinkNone.get(tlink.source);
                }
                listTemp.add(tlink);
                listTlinkNone.put(tlink.source, listTemp);
            }
        }

        HashMap<String, List<linkStructure>> listTlinkTemp = new HashMap<String, List<linkStructure>>();
        listTlinkTemp.putAll(listLink);

        if (typeCorpus.equals("eval")) {
            for (String srce : listTlinkTemp.keySet()) {
                List<linkStructure> listTempModif = new ArrayList<linkStructure>();
                listTempModif.addAll(listTlinkTemp.get(srce));
                List<linkStructure> listTemp = listTlinkTemp.get(srce);
                for (int j = listTemp.size() - 1; j >= 0; j--) {
                    linkStructure tlink = listTemp.get(j);
                    linkStructure tlinkTemp = new linkStructure();
                    tlinkTemp.source = tlink.source;
                    tlinkTemp.target = tlink.target;
                    tlinkTemp.relType = "NONE";
                    if (!inList(listTlinkAutoRef, tlinkTemp)) {
                        listTempModif.remove(j);
                    }
                }
                if (listTempModif.size() == 0) {
                    listLink.remove(srce);
                } else {
                    listLink.put(srce, listTempModif);
                }
            }
        }
        List<HashMap<String, List<linkStructure>>> returnList = new ArrayList<HashMap<String, List<linkStructure>>>();
        returnList.add(listTlinkNone);
        returnList.add(listLink);
        return returnList;
    }

    private static boolean inList(List<linkStructure> list, linkStructure t) {
        boolean find = false;
        for (linkStructure tl : list) {
            if (tl.source.equals(t.source) && tl.target.equals(t.target)) {
                find = true;
            }
        }
        return find;
    }

    private static String[][] addTlinkRefToTXP(String[][] lines, int colPairs, int event_id, int idTimex,
            String typeCorpus) {
        List<HashMap<String, List<linkStructure>>> returnList = get_tlink_none(lines, colPairs, typeCorpus, listTlink);
        HashMap<String, List<linkStructure>> listTlinkNone = returnList.get(0);
        listTlink = returnList.get(1);

        List<String> listIdTimex = new ArrayList<String>();
        List<String> listIdEvent = new ArrayList<String>();

        for (int t : listTimex.keySet()) {
            listIdTimex.add(listTimex.get(t).timexID.replace("t", "tmx"));
        }
        listIdTimex.add("tmx0");
        for (int t : listEvent.keySet()) {
            listIdEvent.add(listEvent.get(t).eventID);
        }

        for (int i = 0; i < lines.length; i++) {
            if (lines[i][event_id] != null && lines[i][event_id].startsWith("e")) {
                lines[i][colPairs] = "";
                if (listTlink.containsKey(lines[i][event_id]) || listTlinkNone.containsKey(lines[i][event_id])) {
                    if (listTlink.containsKey(lines[i][event_id])) {
                        String list_pairs = "";
                        for (linkStructure tlink : listTlink.get(lines[i][event_id])) {
                            if (listIdTimex.contains(tlink.target) || listIdEvent.contains(tlink.target)) {
                                if (!list_pairs.equals("")) {
                                    list_pairs += "||";
                                }
                                list_pairs += tlink.source + ":" + tlink.target + ":" + tlink.relType;
                            }
                        }
                        lines[i][colPairs] = list_pairs;
                    }
                    if (listTlinkNone.containsKey(lines[i][event_id])) {
                        String list_pairs = "";
                        if (lines[i][colPairs] != null) {
                            list_pairs = lines[i][colPairs];
                        }
                        for (linkStructure tlink : listTlinkNone.get(lines[i][event_id])) {
                            if (listIdTimex.contains(tlink.target) || listIdEvent.contains(tlink.target)) {
                                if (!list_pairs.equals("")) {
                                    list_pairs += "||";
                                }
                                list_pairs += tlink.source + ":" + tlink.target + ":" + tlink.relType;
                            }
                        }
                        lines[i][colPairs] = list_pairs;
                    }
                } else {
                    lines[i][colPairs] = "O";
                }
            } else if (lines[i][idTimex] != null && lines[i][idTimex].startsWith("tmx")) {
                lines[i][colPairs] = "";
                if (listTlink.containsKey(lines[i][idTimex]) || listTlinkNone.containsKey(lines[i][idTimex])) {
                    if (listTlink.containsKey(lines[i][idTimex])) {
                        String list_pairs = "";
                        for (linkStructure tlink : listTlink.get(lines[i][idTimex])) {
                            if (listIdTimex.contains(tlink.target) || listIdEvent.contains(tlink.target)) {
                                if (!list_pairs.equals("")) {
                                    list_pairs += "||";
                                }
                                list_pairs += tlink.source + ":" + tlink.target + ":" + tlink.relType;
                            }
                        }
                        lines[i][colPairs] = list_pairs;
                    }
                    if (listTlinkNone.containsKey(lines[i][idTimex])) {
                        String list_pairs = "";
                        if (lines[i][colPairs] != null) {
                            list_pairs = lines[i][colPairs];
                        }
                        for (linkStructure tlink : listTlinkNone.get(lines[i][idTimex])) {
                            if (listIdTimex.contains(tlink.target) || listIdEvent.contains(tlink.target)) {
                                if (!list_pairs.equals("")) {
                                    list_pairs += "||";
                                }
                                list_pairs += tlink.source + ":" + tlink.target + ":" + tlink.relType;
                            }
                        }
                        lines[i][colPairs] = list_pairs;
                    }
                } else {
                    lines[i][colPairs] = "O";
                }
            } else {
                lines[i][colPairs] = "O";
            }
        }

        return lines;
    }

    public static String[][] addTimeMLRefToTXP(String[][] lines, String refFileName, HashMap<String, Integer> idCol,
            boolean timex_ref, boolean event_ref, boolean tlink_ref, boolean slink_ref, boolean alink_ref,
            String typeCorpus) {
        listTimex.clear();
        listEvent.clear();
        listTlink.clear();
        listSlink.clear();
        listAlink.clear();

        timexStructure dct = readTimeMLRef(refFileName);

        if (timex_ref) {
            int colTimex = idCol.get("timex_type");
            int valTimex = idCol.get("timex_value");
            int idTimex = idCol.get("timex_id");
            lines = addTimexRefToTXP(lines, dct, colTimex, valTimex, idTimex);
        }

        if (event_ref && idCol.containsKey("event_ref_train")) {
            int classEvent = idCol.get("event_class");
            int event = idCol.get("event_ref_train");
            lines = addEventRefTrainToTXP(lines, classEvent, event);
        } else if (event_ref) {
            int idTimex = idCol.get("timex_id");
            int idEvent = idCol.get("event_id");
            int classEvent = idCol.get("pred_class");
            lines = addEventRefToTXP(lines, idEvent, classEvent, idTimex);
        }
        if (tlink_ref) {
            int idTimex = idCol.get("timex_id");
            int idEvent = idCol.get("event_id");
            int idPairs = idCol.get("pairs");
            lines = AddTLINK.add_candidate_pairs(lines, lines[0].length, idCol, true);
            lines = addTlinkRefToTXP(lines, idPairs, idEvent, idTimex, typeCorpus);
        }

        if (slink_ref) {
            int idEvent = idCol.get("event_id");
            int idSlink = idCol.get("slink");
            lines = addSlinkRefToTXP(lines, idSlink, idEvent);
        }

        if (alink_ref) {
            int idEvent = idCol.get("event_id");
            int idAlink = idCol.get("alink");
            lines = AddALINK.add_candidate_pairs(lines, lines[0].length, idCol);
            lines = addAlinkRefToTXP(lines, idAlink, idEvent, typeCorpus);
        }

        return lines;
    }

    private static String[][] addSlinkRefToTXP(String[][] lines, int idSlink, int idEvent) {

        for (int i = 0; i < lines.length; i++) {
            if (lines[i] != null && lines[i][0] != null && lines[i][idEvent] != null
                    && listSlink.containsKey(lines[i][idEvent])) {
                String slink_pairs = "";
                for (linkStructure link : listSlink.get(lines[i][idEvent])) {
                    if (!slink_pairs.equals("")) {
                        slink_pairs += "||";
                    }
                    slink_pairs += link.source + ":" + link.target + ":" + link.relType;
                }
                lines[i][lines[i].length - 1] = slink_pairs;
            } else {
                lines[i][lines[i].length - 1] = "O";
            }
        }

        return lines;
    }

    private static String[][] addAlinkRefToTXP(String[][] lines, int colPairs, int event_id, String typeCorpus) {
        List<HashMap<String, List<linkStructure>>> returnList = get_tlink_none(lines, colPairs, typeCorpus, listAlink);
        HashMap<String, List<linkStructure>> listTlinkNone = returnList.get(0);
        listAlink = returnList.get(1);

        List<String> listIdEvent = new ArrayList<String>();

        for (int t : listEvent.keySet()) {
            listIdEvent.add(listEvent.get(t).eventID);
        }

        for (int i = 0; i < lines.length; i++) {
            if (lines[i][event_id] != null && lines[i][event_id].startsWith("e")) {
                lines[i][colPairs] = "";
                if (listAlink.containsKey(lines[i][event_id]) || listTlinkNone.containsKey(lines[i][event_id])) {
                    if (listAlink.containsKey(lines[i][event_id])) {
                        String list_pairs = "";
                        for (linkStructure tlink : listAlink.get(lines[i][event_id])) {
                            if (listIdEvent.contains(tlink.target)) {
                                if (!list_pairs.equals("")) {
                                    list_pairs += "||";
                                }
                                list_pairs += tlink.source + ":" + tlink.target + ":" + tlink.relType;
                            }
                        }
                        lines[i][colPairs] = list_pairs;
                    }
                    if (listTlinkNone.containsKey(lines[i][event_id])) {
                        String list_pairs = "";
                        if (lines[i][colPairs] != null) {
                            list_pairs = lines[i][colPairs];
                        }
                        for (linkStructure tlink : listTlinkNone.get(lines[i][event_id])) {
                            if (listIdEvent.contains(tlink.target)) {
                                if (!list_pairs.equals("")) {
                                    list_pairs += "||";
                                }
                                list_pairs += tlink.source + ":" + tlink.target + ":" + tlink.relType;
                            }
                        }
                        lines[i][colPairs] = list_pairs;
                    }
                } else {
                    lines[i][colPairs] = "O";
                }
            } else {
                lines[i][colPairs] = "O";
            }
        }

        return lines;
    }

    private static String[][] addDCTRef(String[][] lines, timexStructure dct, int idTimex, int colType, int valTimex) {
        boolean posFind = false;
        int i = 0;
        while (!posFind && i < lines.length) {
            if (lines[i][0] != null && lines[i][0] == "DCT") {
                lines[i][0] += "_" + dct.value;
                lines[i][idTimex] = dct.timexID.replace("t", "tmx");
                lines[i][colType] = "B-" + dct.typeTimex;
                lines[i][valTimex] = dct.value;
                posFind = true;
            }
            i++;
        }

        return lines;
    }

    /**
     * read the timeML file ref and put information about event and timex in two hash: listEvent and listTimex
     *
     * @param file name
     */
    public static timexStructure readTimeMLRef(String refFileName) {
        HashMap<String, String> correspIdEv = new HashMap<String, String>();
        HashMap<String, String> correspIdEvInv = new HashMap<String, String>();

        timexStructure dct = new timexStructure();

        int cptTimex = 0;
        int cptEvent = 0;

        try {
            InputStream ips = new FileInputStream(refFileName);
            InputStreamReader ipsr = new InputStreamReader(ips);
            BufferedReader br = new BufferedReader(ipsr);
            String line;
            boolean inText = false;

            while ((line = br.readLine()) != null) {

                if (line.matches(".*<TEXT>.*")) {
                    inText = true;
                }

                if (!line.equals("") && line.matches(".*<TIMEX3.*") && !inText) {
                    Pattern p = Pattern.compile("id=\"([^\"]+)\"");
                    Matcher m = p.matcher(line);
                    if (m.find()) {
                        dct.timexID = m.group(1);
                    }
                    p = Pattern.compile("value=\"([^\"]+)\"");
                    m = p.matcher(line);
                    if (m.find()) {
                        dct.value = m.group(1);
                    }
                    p = Pattern.compile("type=\"([^\"]+)\"");
                    m = p.matcher(line);
                    if (m.find()) {
                        dct.typeTimex = m.group(1);
                    }

                }
				
				/* TIMEX3 */
                if (!line.equals("") && line.matches(".*<TIMEX3.*") && inText) {
                    Pattern p = Pattern.compile("<TIMEX3[^<]+</TIMEX3>");
                    Matcher m = p.matcher(line);
                    while (m.find()) {
                        timexStructure tmp = new timexStructure();
                        String timex = line.substring(m.start(), m.end());
                        Pattern p1 = Pattern
                                .compile("<TIMEX3[^>]*tid=\"(t[0-9]+)\"[^>]+type=\"([^\"]+)\"[^>]*> ?([^<]+)</TIMEX3>");
                        Matcher m1 = p1.matcher(timex);
                        if (m1.find()) {
                            String textTimex = m1.group(3);
                            textTimex = textTimex.replace(",", " ,");
                            textTimex = textTimex.replace("'s", " 's");
                            //textTimex = textTimex.replace(" '8", " ' 8");
                            textTimex = textTimex.replace("s'", "s '");
                            textTimex = textTimex.replace(". ,", ".,");
                            tmp.timexID = m1.group(1);
                            tmp.typeTimex = m1.group(2);
                            tmp.instance = textTimex;
                            tmp.firstTok = textTimex.split(" ")[0];
                        } else {
                            Pattern p2 = Pattern.compile(
                                    "<TIMEX3[^>]*type=\"([^\"]+)\"[^>]+tid=\"(t[0-9]+)\"[^>]*> ?([^<]+)</TIMEX3>");
                            Matcher m2 = p2.matcher(timex);
                            if (m2.find()) {
                                String textTimex = m2.group(3);
                                textTimex = textTimex.replace(",", " ,");
                                textTimex = textTimex.replace("'s", " 's");
                                textTimex = textTimex.replace(" '8", " ' 8");
                                textTimex = textTimex.replace(". ,", ".,");
                                tmp.timexID = m2.group(2);
                                tmp.typeTimex = m2.group(1);
                                tmp.instance = textTimex;
                                tmp.firstTok = textTimex.split(" ")[0];
                            } else {
                                System.out.println("don't match: " + timex);
                            }
                        }

                        Pattern p3 = Pattern.compile("value=\"([^\"]+)\"");
                        Matcher m3 = p3.matcher(timex);

                        if (m3.find()) {
                            tmp.value = m3.group(1);
                        } else {
                            tmp.value = null;
                        }

                        if (line.matches(".*" + timex + "-<.*") || line.matches(".*>-" + timex + ".*") || timex
                                .matches(".*[0-9]/[0-9].*")) {
                            System.out.println(timex);
                        } else {
                            listTimex.put(cptTimex, tmp);
                            cptTimex++;
                        }
                    }
                }
				
				/* EVENT */
                if (line.matches(".*<EVENT.*") && inText) {
                    Pattern p = Pattern.compile("<EVENT[^<]+</EVENT>");
                    Matcher m = p.matcher(line);
                    while (m.find()) {
                        eventStructure tmp = new eventStructure();
                        String event = line.substring(m.start(), m.end());
                        Pattern p1 = Pattern
                                .compile("<EVENT[^>]*eid=\"(e[0-9]+)\"[^>]*class=\"([^\"]+)\"[^>]*> ?([^<]+)</EVENT");
                        Matcher m1 = p1.matcher(event);
                        if (m1.find()) {
                            tmp.classEvent = m1.group(2);
                            tmp.eventID = m1.group(1);
                            tmp.instance = m1.group(3);
                        } else {
                            Pattern p2 = Pattern.compile(
                                    "<EVENT[^>]*class=\"([^\"]+)\"[^>]*eid=\"(e[0-9]+)\"[^>]*> ?([^<]+)</EVENT");
                            Matcher m2 = p2.matcher(event);
                            if (m2.find()) {
                                tmp.classEvent = m2.group(1);
                                tmp.eventID = m2.group(2);
                                tmp.instance = m2.group(3);
                            }
                        }

                        listEvent.put(cptEvent, tmp);
                        cptEvent++;
                    }
                }
				
				/* makeinstance: get the event instance id */
                if (line.matches("<MAKEINSTANCE.*")) {
                    Pattern p = Pattern
                            .compile("<MAKEINSTANCE[^>]+eventID=\"(e[0-9]+)\"[^>]+eiid=\"(ei[0-9]+)\"[^>]*/>");
                    Matcher m = p.matcher(line);
                    if (m.find()) {
                        correspIdEv.put(m.group(1), m.group(2));
                        correspIdEvInv.put(m.group(2), m.group(1));
                    } else {
                        Pattern p1 = Pattern
                                .compile("<MAKEINSTANCE[^>]+eiid=\"(ei[0-9]+)\"[^>]+eventID=\"(e[0-9]+)\"[^>]*/>");
                        Matcher m1 = p1.matcher(line);
                        if (m1.find()) {
                            correspIdEv.put(m1.group(2), m1.group(1));
                            correspIdEvInv.put(m1.group(1), m1.group(2));
                        }
                    }
                }
				
				/* get the relation ids */
                else if (line.matches("<TLINK.*")) {
                    linkStructure tlink = new linkStructure();

                    Pattern p = Pattern.compile("eventInstanceID=\"(ei[0-9]+)\"");
                    Matcher m = p.matcher(line);
                    if (m.find()) {
                        //List<String> temp = new ArrayList<String> ();
                        //if(tlink.containsKey(m.group(1))){
                        //temp.addAll(tlink.get(m.group(1)));
                        //}
                        //temp.add(m.group(2));
                        //tlink.put(m.group(1),temp);
                        tlink.source = correspIdEvInv.get(m.group(1));
                    }

                    //[^>]+relatedToEventInstance=\"(ei[0-9]+)\"[^>]*/>
                    Pattern p1 = Pattern.compile("timeID=\"(t[0-9]+)\"");
                    Matcher m1 = p1.matcher(line);
                    if (m1.find()) {
							/*List<String> temp = new ArrayList<String> ();
							if(tlink.containsKey(m1.group(2))){
								temp.addAll(tlink.get(m1.group(2)));
							}
							temp.add(m1.group(1));
							tlink.put(m1.group(2),temp);*/
                        tlink.source = m1.group(1).replace("t", "tmx");
                    }

                    Pattern p2 = Pattern.compile("relatedToTime=\"(t[0-9]+)\"");
                    Matcher m2 = p2.matcher(line);
                    if (m2.find()) {
								/*List<String> temp = new ArrayList<String> ();
								if(tlink.containsKey(m2.group(1))){
									temp.addAll(tlink.get(m2.group(1)));
								}
								temp.add(m2.group(2));
								tlink.put(m2.group(1),temp);*/

                        tlink.target = m2.group(1).replace("t", "tmx");
                    }

                    Pattern p3 = Pattern.compile("relatedToEventInstance=\"(ei[0-9]+)\"");
                    Matcher m3 = p3.matcher(line);
                    if (m3.find()) {
                        tlink.target = correspIdEvInv.get(m3.group(1));
                    }

                    Pattern p4 = Pattern.compile("relType=\"([^\"]+)\"");
                    Matcher m4 = p4.matcher(line);
                    if (m4.find()) {
                        String typeTmp = m4.group(1);
                        if (typeTmp.equals("IAFTER")) {
                            typeTmp = "AFTER";
                        } else if (typeTmp.equals("IBEFORE")) {
                            typeTmp = "BEFORE";
                        } else if (typeTmp.equals("DURING") && tlink.source.startsWith("e") && tlink.target
                                .startsWith("t")) {
                            typeTmp = "IS_INCLUDED";
                        } else if (typeTmp.equals("DURING") && tlink.source.startsWith("t") && tlink.target
                                .startsWith("e")) {
                            typeTmp = "INCLUDES";
                        }

                        tlink.relType = typeTmp;
                    }

                    Pattern p5 = Pattern.compile("lid=\"([^\"]+)\"");
                    Matcher m5 = p5.matcher(line);
                    if (m5.find()) {
                        tlink.tlinkID = m5.group(1);
                    }

                    if ((tlink.relType.equals("DURING") && tlink.source.startsWith("e") && tlink.target.startsWith("e")
                            || (tlink.relType.equals("IDENTITY")))) {

                    } else {
                        List<linkStructure> list_sce_rel = new ArrayList<linkStructure>();
                        if (listTlink.containsKey(tlink.source)) {
                            list_sce_rel = listTlink.get(tlink.source);
                        }

                        list_sce_rel.add(tlink);
                        listTlink.put(tlink.source, list_sce_rel);
                    }

                } else if (line.matches("<SLINK.*")) {
                    linkStructure slink = new linkStructure();

                    Pattern p = Pattern.compile("eventInstanceID=\"(ei[0-9]+)\"");
                    Matcher m = p.matcher(line);
                    if (m.find()) {
                        slink.source = correspIdEvInv.get(m.group(1));
                    }

                    Pattern p1 = Pattern.compile("subordinatedEventInstance=\"(ei[0-9]+)\"");
                    Matcher m1 = p1.matcher(line);
                    if (m1.find()) {
                        slink.target = correspIdEvInv.get(m1.group(1));
                    }

                    Pattern p4 = Pattern.compile("relType=\"([^\"]+)\"");
                    Matcher m4 = p4.matcher(line);
                    if (m4.find()) {
                        slink.relType = m4.group(1);
                    }

                    Pattern p5 = Pattern.compile("lid=\"([^\"]+)\"");
                    Matcher m5 = p5.matcher(line);
                    if (m5.find()) {
                        slink.tlinkID = m5.group(1);
                    }

                    List<linkStructure> list_sce_rel = new ArrayList<linkStructure>();
                    if (listSlink.containsKey(slink.source)) {
                        list_sce_rel = listSlink.get(slink.source);
                    }

                    list_sce_rel.add(slink);
                    listSlink.put(slink.source, list_sce_rel);

                } else if (line.matches("<ALINK.*")) {
                    linkStructure alink = new linkStructure();

                    Pattern p = Pattern.compile("eventInstanceID=\"(ei[0-9]+)\"");
                    Matcher m = p.matcher(line);
                    if (m.find()) {
                        alink.source = correspIdEvInv.get(m.group(1));
                    }

                    Pattern p1 = Pattern.compile("relatedToEventInstance=\"(ei[0-9]+)\"");
                    Matcher m1 = p1.matcher(line);
                    if (m1.find()) {
                        alink.target = correspIdEvInv.get(m1.group(1));
                    }

                    Pattern p4 = Pattern.compile("relType=\"([^\"]+)\"");
                    Matcher m4 = p4.matcher(line);
                    if (m4.find()) {
                        alink.relType = m4.group(1);
                    }

                    Pattern p5 = Pattern.compile("lid=\"([^\"]+)\"");
                    Matcher m5 = p5.matcher(line);
                    if (m5.find()) {
                        alink.tlinkID = m5.group(1);
                    }

                    List<linkStructure> list_sce_rel = new ArrayList<linkStructure>();
                    if (listAlink.containsKey(alink.source)) {
                        list_sce_rel = listAlink.get(alink.source);
                    }

                    list_sce_rel.add(alink);
                    listAlink.put(alink.source, list_sce_rel);

                }

            }

            br.close();
        } catch (Exception e) {
            System.out.println(e.toString());
        }

        return dct;

    }

}

class eventStructure {

    String classEvent;
    String eventID;
    String eventEID;
    String instance;
    String relation;
}

class timexStructure {

    String typeTimex;
    String timexID;
    String instance;
    String firstTok;
    String value;
}

class linkStructure {

    String tlinkID;
    String source;
    String target;
    String relType;
}

