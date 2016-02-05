package eu.fbk.newsreader.naf;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class AddTLINK {

    private static int colSentId = 0;
    private static int colTimexId = 0;
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

    public static String[][] add_candidate_pairs(String[][] lines, int nbCol, HashMap<String, Integer> intCol,
            boolean allEventTmx0Pairs) {

        init_col_id(intCol);

        String numSent = "0";
        Integer[][] sentences = get_sentences(lines);
        String event_prev_sent = "";
        boolean main_event_prev_sent = false;

        for (int j = 0; j < sentences.length; j++) {
            if (sentences[j] != null && sentences[j][0] != null && sentences[j][1] != null) {
                if (!main_event_prev_sent) {
                    event_prev_sent = "";
                }
                List<String> list_timex = new ArrayList<String>();
                List<String> list_event = new ArrayList<String>();
                HashMap<String, Integer> list_ev_tokid = new HashMap<String, Integer>();
                for (int i = sentences[j][0]; i <= sentences[j][1]; i++) {
                    if (lines[i][0] != null && lines[i][colTimexId] != null && lines[i][colTimexId].startsWith("tmx")) {
                        if (!list_timex.contains(lines[i][colTimexId])) {
                            list_timex.add(lines[i][colTimexId]);
                        }
                    } else if (lines[i][0] != null && lines[i][colEventId] != null && lines[i][colEventId]
                            .startsWith("e")) {
                        if (!list_event.contains(lines[i][colEventId])) {
                            list_event.add(lines[i][colEventId]);
                            list_ev_tokid.put(lines[i][colEventId], i);
                        }
                    }
                }

                int k = 0;
                Boolean sent_has_mainVerb = false;
                for (int i = sentences[j][0]; i <= sentences[j][1]; i++) {
                    if (k <= list_event.size() && lines[i][0] != null && lines[i][colEventId] != null
                            && lines[i][colEventId].startsWith("e")) {
                        //event-event in a sentence
                        for (int l = k + 1; l < list_event.size(); l++) {
                            int pos_ev2 = list_ev_tokid.get(list_event.get(l));
                            Boolean notlink = false;
                        /*if ((pos_ev2 == i+2 && lines[pos_ev2-1][colLemma].equals("to"))
								|| (lines[i][colLemma].matches("((seem)|(do)|(make)|(get)|(take)|(put)|(set)|(let))") && pos_ev2 < i+5)){
							notlink = true;
						}*/
						/*for(int e=i+1; e<pos_ev2; e++){
							if(lines[e][colLemma].matches("(that)|(which)") && !lines[i][colEventClass].equals("communication")){
								notlink = true;
							}
						}*/
                            if (i == pos_ev2 - 1) {
                                notlink = true;
                            }
                            if (lines[i][colChunk].contains("VP") && lines[pos_ev2][colChunk].contains("VP")) {
                                Boolean sameChunk = true;
                                for (int m = i; m < pos_ev2; m++) {
                                    if (!lines[m][colChunk].contains("I-VP")) {
                                        sameChunk = false;
                                    }
                                }
                                if (sameChunk) {
                                    notlink = true;
                                }
                            }

                            if (!notlink) {
                                if (lines[i][colPairs] == null) {
                                    lines[i][colPairs] = "";
                                } else {
                                    lines[i][colPairs] += "||";
                                }
                                lines[i][colPairs] += lines[i][colEventId] + ":" + list_event.get(l) + ":NONE";
                            }
                        }
                        //event-timex in a sentence
                        for (int l = 0; l < list_timex.size(); l++) {
                            if (eventCloserTimex(lines, sentences[j], list_timex.get(l), list_event.get(k))) {
                                if (lines[i][colPairs] == null) {
                                    lines[i][colPairs] = "";
                                } else {
                                    lines[i][colPairs] += "||";
                                }
                                lines[i][colPairs] += lines[i][colEventId] + ":" + list_timex.get(l) + ":NONE";
                            }
                        }
                        //event-t0
                        if (((lines[i][colPOS].startsWith("V") || lines[i][colChunk].endsWith("VP"))
                                && !lines[i][colTense].matches("((INFINITIVE)|(PRESPART)|(PASTPART))\\+.*"))
                                || allEventTmx0Pairs) {

                            if (lines[i][colPairs] == null) {
                                lines[i][colPairs] = "";
                            } else {
                                lines[i][colPairs] += "||";
                            }
                            lines[i][colPairs] += lines[i][colEventId] + ":" + "tmx0" + ":NONE";
                        }

                        //main event
                        if (lines[i][colMainVb] != null && lines[i][colMainVb].equals("mainVb")) {
                            if (!event_prev_sent.equals("")) {
                                if (lines[i][colPairs] == null) {
                                    lines[i][colPairs] = "";
                                } else {
                                    lines[i][colPairs] += "||";
                                }
                                lines[i][colPairs] += lines[i][colEventId] + ":" + event_prev_sent + ":NONE";
                            }
                            sent_has_mainVerb = true;
                            main_event_prev_sent = true;
                            event_prev_sent = lines[i][colEventId];
                        }

                        k++;
                    }
                }

                if (!sent_has_mainVerb) {
                    for (int i = sentences[j][0]; i <= sentences[j][1]; i++) {
                        if (lines[i][colEventId] != null && (lines[i][colEventClass] == "REPORTING") || (
                                lines[i][colEventClass] == "communication")) {
                            if (!event_prev_sent.equals("")) {
                                if (lines[i][colPairs] == null) {
                                    lines[i][colPairs] = "";
                                } else {
                                    lines[i][colPairs] += "||";
                                }
                                lines[i][colPairs] += lines[i][colEventId] + ":" + event_prev_sent + ":NONE";
                            }
                            main_event_prev_sent = true;
                            event_prev_sent = lines[i][colEventId];
                        }
                    }
                }

            }

        }

        for (int i = 0; i < lines.length; i++) {
            if (lines[i] != null && (lines[i][colPairs] == null || lines[i][colPairs].equals(""))) {
                lines[i][colPairs] = "O";
            }
        }

        return lines;
    }

    private static Boolean eventCloserTimex(String[][] lines, Integer[] sentences, String timex, String event) {
        int evPos = 0;
        int dist = 0;
        int txPos = 0;
        int prevEv = 0;
        for (int i = sentences[0]; i < sentences[1]; i++) {

            if (lines[i][colEventId] != null && lines[i][colEventId].equals(event)) {
                evPos = i;
                if (txPos > 0) {
                    dist = evPos - i;
                    if (prevEv > 0) {
                        if (dist <= txPos - prevEv) {
                            return true;
                        } else {
                            return false;
                        }
                    } else {
                        return true;
                    }
                }
            } else if (lines[i][colEventId] != null && lines[i][colEventId].startsWith("e") && evPos > 0
                    && txPos == 0) {
                return false;
            } else if (lines[i][colEventId] != null && lines[i][colEventId].startsWith("e") && txPos > 0
                    && evPos == 0) {
                return false;
            } else if (lines[i][colEventId] != null && lines[i][colEventId].startsWith("e") && txPos == 0) {
                prevEv = i;
            } else if (lines[i][colEventId] != null && lines[i][colEventId].startsWith("e") && txPos > 0 && evPos > 0) {
                if (dist <= i - txPos) {
                    return true;
                } else {
                    return false;
                }
            }
            if (lines[i][colTimexId] != null && lines[i][colTimexId].equals(timex)) {
                txPos = i;
                if (evPos > 0) {
                    dist = i - evPos;
                }
            }
        }
        return true;
    }

    private static Integer[][] get_sentences(String[][] lines) {
        int i = lines.length - 1;
        String numLastSent = "-1";
        while (numLastSent.equals("-1") && i > 0) {
            if (lines[i][0] != null && lines[i][colSentId] != null) {
                numLastSent = lines[i][colSentId];
            }
            i--;
        }
        Integer[][] sentences = new Integer[Integer.parseInt(numLastSent) + 1][2];

        String numSent = "-1";
        int firstId = -1;
        int lastId = -1;
        for (i = 5; i < lines.length; i++) {
            if (lines[i][0] != null && lines[i][colSentId] != null && !numSent.equals(lines[i][colSentId])) {
                if (!numSent.equals("-1")) {
                    lastId = i - 1;
                    sentences[Integer.parseInt(numSent)][0] = firstId;
                    sentences[Integer.parseInt(numSent)][1] = lastId;
                }
                numSent = lines[i][colSentId];
                firstId = i;
            }
        }
        lastId = lines.length - 1;
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
