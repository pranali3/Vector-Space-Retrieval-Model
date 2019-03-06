/*
Priyanka Salian
679060524
Assignment2
*/
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.TreeMap;

import org.jsoup.Jsoup;
import org.jsoup.select.Elements;



public class Assignment2 {

	//Inverted index to store the word and list of document and term frequency
	TreeMap<String, TreeMap<Integer, Integer>> invertedIndx = new TreeMap<String, TreeMap<Integer, Integer>>();
	
	//Doc Data
	TreeMap<Integer, TreeMap<String, Integer>> docdata = new TreeMap<Integer, TreeMap<String, Integer>>();
		
	//Query Data
	TreeMap<Integer, TreeMap<String, Integer>> invertedIndxQuery = new TreeMap<Integer, TreeMap<String, Integer>>();
	
	//Store the stopwords
	List<String> stopwords = new ArrayList<String>();
	
	//Store query specific idfs for every word
	TreeMap<String, Double> idfWords = new TreeMap<String, Double>();
	
	//Store document length
	TreeMap<Integer, Double> docLength = new TreeMap<Integer, Double>();
	
	//Store query length
	TreeMap<Integer, Double> queryLength = new TreeMap<Integer, Double>();
	
	//Store Cosine Sort
	TreeMap<Integer, TreeMap<Integer, Double>> cosineSim = new TreeMap<Integer, TreeMap<Integer, Double>>();
	
	
	TreeMap<Integer, ArrayList<Integer>> relevance = new TreeMap<Integer, ArrayList<Integer>>();
	
	TreeMap<Integer, Integer> MaxTerm = new TreeMap<Integer, Integer>();
	public static void main(String[] args) {
		Assignment2 a = new Assignment2();
		Scanner s = new Scanner(System.in);
		//Collect Stopwords
		a.collectStopwords();
		//First clean the Document corpus
		System.out.println("Please enter the filepath: ");
		String filepath = s.nextLine();
		System.out.println("Please enter the query path: ");
		String querypath = s.nextLine();
		System.out.println("Please enter the relevant docs path: ");
		String relevancepath = s.nextLine();
		a.read_preprocess(filepath);
		
		//Read and clean Query files
		
		a.readQueryFile(querypath);
		
		//Calculate IDFs
		a.calculateIDFs();
		
		//Calculate docLength
		a.calculateDocumentLength();
		
		//Calculate Query Length
		a.calculateQueryLength();
		
		
		
		
		//Calculate cosine
		for(int i = 1; i <= 10; i++) {
			a.cosineFunction(i);;
		}
		//a.calculateCosineSim();
		
		a.relevanceDocs(relevancepath);
		double v10R = 0; double v50R = 0; double v100R = 0; double v500R = 0;
		double v10P = 0; double v50P = 0; double v100P = 0; double v500P = 0;
		for(int i = 1; i <= 10; i++) {
			v10R += a.calculateRecall(i, 10);
			v10P += a.calculatePrecision(i, 10);
			v50R += a.calculateRecall(i, 50);
			v50P += a.calculatePrecision(i, 50);
			v100R += a.calculateRecall(i, 100);
			v100P += a.calculatePrecision(i, 100);
			v500R += a.calculateRecall(i, 500);
			v500P += a.calculatePrecision(i, 500);
		}
		
		
		System.out.println("Precision:");
		System.out.println("10 :"+v10P/10.0);
		System.out.println("50 :"+v50P/50.0);
		System.out.println("100 :"+v100P/100.0);
		System.out.println("500 :"+v500P/500.0);
		
		System.out.println("Recall: ");
		System.out.println("10 :"+v10R/10.0);
		System.out.println("50 :"+v50R/50.0);
		System.out.println("100 :"+v100R/100.0);
		System.out.println("500 :"+v500R/500.0);
	}

	
	public void cosineFunction(int qId) {
		
		TreeMap<Integer, Double> temp = new TreeMap<Integer, Double>();;
		TreeMap<Integer, Double> tempSorted = new TreeMap<Integer, Double>(new FrequencyComparator(temp));
		
		for(Map.Entry<Integer, TreeMap<String, Integer>> docs: docdata.entrySet()) {
			float tfDoc = 0;
			float tfQue = 0;
			float inner_P = 0;
			float cosine = 0;
			for(Map.Entry<String, Integer> docW: docs.getValue().entrySet()) {
				for(Map.Entry<String, Integer> queW: invertedIndxQuery.get(qId).entrySet()) {
					if(queW.getKey().equals(docW.getKey())) {
						tfDoc = (float) (invertedIndx.get(queW.getKey()).get(docs.getKey()) * idfWords.get(queW.getKey()));
						tfQue = (float) (invertedIndxQuery.get(qId).get(queW.getKey()) * idfWords.get(queW.getKey()));
						inner_P += tfDoc * tfQue;
						
						}
					
				}
				
			}
			cosine = (float) (inner_P/(double) (docLength.get(docs.getKey()) * queryLength.get(qId)));
			temp.put(docs.getKey(), (double) cosine);
		}
		tempSorted.putAll(temp);
		cosineSim.put(qId, tempSorted);
	}
	
	public void read_preprocess(String filepath) {
		int count = 1;
		File f = new File(filepath);
		for (File file : f.listFiles()) {
			
			if (file.isDirectory()) {
				read_preprocess(filepath);
			} else {
				//Remove all the SGML tags and retain TITLE AND TEXT
				String st = RemoveSGML(file);
				preprocessDoc(st.toLowerCase(), count);
				count++;
				
			}
		}
	}
	
	public String RemoveSGML(File file) {
		StringBuilder sb = new StringBuilder();
		try {
			
			org.jsoup.nodes.Document doc =  Jsoup.parse(file, "UTF-8", "");
			Elements title = doc.select("title");
			sb.append(title.get(0).text());
			sb.append(" \t");
			Elements text = doc.select("text");
			sb.append(text.get(0).text());
			
		} catch (IOException e) {
			
			e.printStackTrace();
		}
		return sb.toString().trim();
	}
	
	public void preprocessDoc(String s, int id) {
		TreeMap<String, Integer> data = new TreeMap<String, Integer>();
		//Remove numbers
		s = s.replaceAll("[\\d.]", "");
		
		//Remove punctuations
		s = s.replaceAll("\\p{Punct}", "");
		s = s.trim();
		
		//Split based on whitespaces
		String[] words = s.split("\\s");
		for(String word: words) {
			
			word = word.trim();
			if(!stopwords.contains(word)) {
				if(!word.equals("")) {
				Porter p = new Porter();
				word = p.stripAffixes(word);
				if(!stopwords.contains(word)) {
					if(word.length() > 2 && !stopwords.contains(word)) {
							if(!invertedIndx.containsKey(word)) {
								TreeMap<Integer, Integer> temp = new TreeMap<Integer, Integer>();
								temp.put(id, 1);
								invertedIndx.put(word, temp);
							} else {
								if(invertedIndx.get(word).containsKey(id)) {
									int temp = invertedIndx.get(word).get(id)+1;
									invertedIndx.get(word).put(id, temp);
								} else {
									TreeMap<Integer, Integer> temp = invertedIndx.get(word);
									temp.put(id, 1);
									invertedIndx.put(word, temp);
								}
								
							}
							
							if(!data.containsKey(word)) {
								data.put(word, 1);
							} else {
								int temp = data.get(word)+1;
								data.put(word, temp);
							}
						}
						
				}
				}
			}
			
			
			}
		docdata.put(id, data);
		
	}
	public void readQueryFile(String querypath) {
		File file = new File(querypath);
		try {
			BufferedReader br = new BufferedReader(new FileReader(file));
			try {
				String st = "";
				int count = 1;
				while ((st = br.readLine()) != null) {
					
					cleanQueries(st.toLowerCase(), count);
					count++;
				}

			} catch (IOException e) {
				e.printStackTrace();
			}
		} catch (FileNotFoundException e) {

			e.printStackTrace();
		}
	}
	
	public void cleanQueries(String s, int id) {
		TreeMap<String, Integer> queryIdWordsFreq = new TreeMap<String, Integer>();
		
		//Split based on whitespaces
		String[] words = s.split("\\s");
		for(String word: words) {
			//Remove punctuations
			word = word.replaceAll("\\p{Punct}", " ");
			word = word.replace("^A-Za-z", " ");
			//Remove numbers
			word = word.replaceAll("[0-9]", "");
			Porter p = new Porter();
			word = word.trim();
			if(!word.equals("")) {
				if(!stopwords.contains(word)) {
					word = p.stripAffixes(word);
					if(!stopwords.contains(word)) {
						
						if(queryIdWordsFreq.containsKey(word)) {
							int freq = queryIdWordsFreq.get(word)+1;
							queryIdWordsFreq.put(word, freq);
						} else {
							queryIdWordsFreq.put(word, 1);
						}
					}
				}
				
				}
			}
		invertedIndxQuery.put(id, queryIdWordsFreq);
	}
	
	public void calculateIDFs() {
		//To store the frequency of the common term in a document
			for(Map.Entry<String, TreeMap<Integer, Integer>> entry : invertedIndx.entrySet()) {
				idfWords.put(entry.getKey(), Math.log(1400/entry.getValue().size())/Math.log(2));
				//System.out.println("Words: "+entry.getKey()+"\t IDF values:"+idfWords.get(entry.getKey()));
			}
	}
	
	public void calculateDocumentLength() {
		
		for(Map.Entry<Integer, TreeMap<String, Integer>> doc: docdata.entrySet()) {
			double length = 0.0;
			for(Map.Entry<String, Integer> data: doc.getValue().entrySet()) {
				//TF* IDF
				length += (double)Math.pow(invertedIndx.get(data.getKey()).get(doc.getKey()) * idfWords.get(data.getKey()), 2);
			
			}
			
			docLength.put(doc.getKey(), (double)Math.sqrt(length));
		}
	}
	
public void calculateQueryLength() {
		
		for(Map.Entry<Integer, TreeMap<String, Integer>> doc: invertedIndxQuery.entrySet()) {
			double length = 0.0;
			for(Map.Entry<String, Integer> data: doc.getValue().entrySet()) {
				//TF* IDF
				if(idfWords.get(data.getKey()) != null) {
					length += (double)Math.pow(data.getValue() * idfWords.get(data.getKey()),2);
				} else {
					length += 0;
				}
				
				
			}
			
			queryLength.put(doc.getKey(), (double)Math.sqrt(length));
		}
	}
	

	public void collectStopwords() {
		String stopS = "a associates able about above according accordingly across actually after afterwards again against all allow allows almost alone along already also although always am among amongst an and another any anybody anyhow anyone anything anyway anyways anywhere apart appear appreciate appropriate are around as aside ask asking associated at available away awfully b be became because become becomes becoming been before beforehand behind being believe below beside besides best better between beyond both brief but by c came can cannot cant cause causes certain certainly changes clearly com come comes concerning consequently consider considering contain containing contains corresponding could course currently d definitely described despite did different do does doing done down downwards during e each edu eg eight either else elsewhere enough entirely especially et etc even ever every everybody everyone everything everywhere ex exactly example except f far few fifth first five followed following follows for former formerly forth four from further furthermore g get gets getting given gives go goes going gone got gotten greetings h had happens hardly has have having he hello help hence her here hereafter hereby herein hereupon hers herself hi him himself his hither hopefully how howbeit however i ie if ignored immediate in inasmuch inc indeed indicate indicated indicates inner insofar instead into inward is it its itself j just k keep keeps kept know knows known l last lately later latter latterly least less lest let like liked likely little look looking looks ltd m mainly many may maybe me mean meanwhile merely might more moreover most mostly much must my myself n name namely nd near nearly necessary need needs neither never nevertheless new next nine no nobody non none noone nor normally not nothing novel now nowhere o obviously of off often oh ok okay old on once one ones only onto or other others otherwise ought our ours ourselves out outside over overall own p particular particularly per perhaps placed please plus possible presumably probably provides q que quite qv r rather rd re really reasonably regarding regardless regards relatively respectively right s said same saw say saying says second secondly see seeing seem seemed seeming seems seen self selves sensible sent serious seriously seven several shall she should since six so some somebody somehow someone something sometime sometimes somewhat somewhere soon sorry specified specify specifying still sub such sup sure t take taken tell tends th than thank thanks thanx that thats the their theirs them themselves then thence there thereafter thereby therefore therein theres thereupon these they think third this thorough thoroughly those though three through throughout thru thus to together too took toward towards tried tries truly try trying twice two u un under unfortunately unless unlikely until unto up upon us use used useful uses using usually uucp v value various very via viz vs w want wants was way we welcome well went were what whatever when whence whenever where whereafter whereas whereby wherein whereupon wherever whether which while whither who whoever whole whom whose why will willing wish with within without wonder would would x y yes yet you your yours yourself yourselves z zero nbsp http www writeln pdf html endobj obj aacute eacute iacute oacute uacute agrave egrave igrave ograve ugrave";
		String[] stopArr = stopS.split("\\s");
		for (String i : stopArr) {
			stopwords.add(i);
		}
	}
	
	public float calculateRecall(int queryId, int limit) {
		int relevantSize = relevance.get(queryId).size();
		float recall = 0;
		int count = 1;
		for(Map.Entry<Integer, Double> val: cosineSim.get(queryId).entrySet()) {
			
			if(count <= limit) {
				if(relevance.get(queryId).contains(val.getKey())) {
					recall++;
				}
				count++;
			} else {
				break;
			}
		}
		return recall/relevantSize;
	}
	
public float calculatePrecision(int queryId, int limit) {
	
	float precision = 0;
	int count = 1;
	for(Map.Entry<Integer, Double> val: cosineSim.get(queryId).entrySet()) {
		
		if(count <= limit) {
			if(relevance.get(queryId).contains(val.getKey())) {
				precision++;
			}
			count++;
		} else {
			break;
		}
	}
	return precision/limit;
	}

public void relevanceDocs(String relepath) {

	File file = new File(relepath);
	try {
		BufferedReader br = new BufferedReader(new FileReader(file));
		try {
			String st = "";
			while ((st = br.readLine()) != null) {
				String[] ids = st.split("\\s");
				int qid = Integer.parseInt(ids[0].trim());
				int dId = Integer.parseInt(ids[1].trim());
				if(relevance.containsKey(qid)) {
					relevance.get(qid).add(dId);
				} else {
					ArrayList<Integer> t = new ArrayList<Integer>();
					t.add(dId);
					relevance.put(qid, t);
				}
			}

		} catch (IOException e) {
			e.printStackTrace();
		}
	} catch (FileNotFoundException e) {

		e.printStackTrace();
	}

}
}
class FrequencyComparator implements Comparator<Integer> {

	private Map<Integer, Double> map;

	public FrequencyComparator(Map<Integer, Double> map) {
		this.map = map;
	}

	public int compare(Integer a, Integer b) {
		if (map.get(a) >= map.get(b)) {
			return -1;
		} else {
			return 1;
		}
	}
}



class NewString {
  public String str;

  NewString() {
    str = "";
  }
}

/**
 * The Porter stemmer for reducing words to their base stem form.
 *
 * @author Fotis Lazarinis
 */

class Porter {

  private String Clean(String str) {
    int last = str.length();

    Character ch = new Character(str.charAt(0));
    String temp = "";

    for (int i = 0; i < last; i++) {
      if (ch.isLetterOrDigit(str.charAt(i)))
        temp += str.charAt(i);
    }

    return temp;
  } //clean

  private boolean hasSuffix(String word, String suffix, NewString stem) {

    String tmp = "";

    if (word.length() <= suffix.length())
      return false;
    if (suffix.length() > 1)
      if (word.charAt(word.length() - 2) != suffix.charAt(suffix.length() - 2))
        return false;

    stem.str = "";

    for (int i = 0; i < word.length() - suffix.length(); i++)
      stem.str += word.charAt(i);
    tmp = stem.str;

    for (int i = 0; i < suffix.length(); i++)
      tmp += suffix.charAt(i);

    if (tmp.compareTo(word) == 0)
      return true;
    else
      return false;
  }

  private boolean vowel(char ch, char prev) {
    switch (ch) {
      case 'a':
      case 'e':
      case 'i':
      case 'o':
      case 'u':
        return true;
      case 'y': {

        switch (prev) {
          case 'a':
          case 'e':
          case 'i':
          case 'o':
          case 'u':
            return false;

          default:
            return true;
        }
      }

      default:
        return false;
    }
  }

  private int measure(String stem) {

    int i = 0, count = 0;
    int length = stem.length();

    while (i < length) {
      for (; i < length; i++) {
        if (i > 0) {
          if (vowel(stem.charAt(i), stem.charAt(i - 1)))
            break;
        } else {
          if (vowel(stem.charAt(i), 'a'))
            break;
        }
      }

      for (i++; i < length; i++) {
        if (i > 0) {
          if (!vowel(stem.charAt(i), stem.charAt(i - 1)))
            break;
        } else {
          if (!vowel(stem.charAt(i), '?'))
            break;
        }
      }
      if (i < length) {
        count++;
        i++;
      }
    } //while

    return (count);
  }

  private boolean containsVowel(String word) {

    for (int i = 0; i < word.length(); i++)
      if (i > 0) {
        if (vowel(word.charAt(i), word.charAt(i - 1)))
          return true;
      } else {
        if (vowel(word.charAt(0), 'a'))
          return true;
      }

    return false;
  }

  private boolean cvc(String str) {
    int length = str.length();

    if (length < 3)
      return false;

    if ((!vowel(str.charAt(length - 1), str.charAt(length - 2)))
        && (str.charAt(length - 1) != 'w') && (str.charAt(length - 1) != 'x') && (str.charAt(length - 1) != 'y')
        && (vowel(str.charAt(length - 2), str.charAt(length - 3)))) {

      if (length == 3) {
        if (!vowel(str.charAt(0), '?'))
          return true;
        else
          return false;
      } else {
        if (!vowel(str.charAt(length - 3), str.charAt(length - 4)))
          return true;
        else
          return false;
      }
    }

    return false;
  }

  private String step1(String str) {

    NewString stem = new NewString();

    if (str.charAt(str.length() - 1) == 's') {
      if ((hasSuffix(str, "sses", stem)) || (hasSuffix(str, "ies", stem))) {
        String tmp = "";
        for (int i = 0; i < str.length() - 2; i++)
          tmp += str.charAt(i);
        str = tmp;
      } else {
        if ((str.length() == 1) && (str.charAt(str.length() - 1) == 's')) {
          str = "";
          return str;
        }
        if (str.charAt(str.length() - 2) != 's') {
          String tmp = "";
          for (int i = 0; i < str.length() - 1; i++)
            tmp += str.charAt(i);
          str = tmp;
        }
      }
    }

    if (hasSuffix(str, "eed", stem)) {
      if (measure(stem.str) > 0) {
        String tmp = "";
        for (int i = 0; i < str.length() - 1; i++)
          tmp += str.charAt(i);
        str = tmp;
      }
    } else {
      if ((hasSuffix(str, "ed", stem)) || (hasSuffix(str, "ing", stem))) {
        if (containsVowel(stem.str)) {

          String tmp = "";
          for (int i = 0; i < stem.str.length(); i++)
            tmp += str.charAt(i);
          str = tmp;
          if (str.length() == 1)
            return str;

          if ((hasSuffix(str, "at", stem)) || (hasSuffix(str, "bl", stem)) || (hasSuffix(str, "iz", stem))) {
            str += "e";

          } else {
            int length = str.length();
            if ((str.charAt(length - 1) == str.charAt(length - 2))
                && (str.charAt(length - 1) != 'l') && (str.charAt(length - 1) != 's') && (str.charAt(length - 1) != 'z')) {

              tmp = "";
              for (int i = 0; i < str.length() - 1; i++)
                tmp += str.charAt(i);
              str = tmp;
            } else if (measure(str) == 1) {
              if (cvc(str))
                str += "e";
            }
          }
        }
      }
    }

    if (hasSuffix(str, "y", stem))
      if (containsVowel(stem.str)) {
        String tmp = "";
        for (int i = 0; i < str.length() - 1; i++)
          tmp += str.charAt(i);
        str = tmp + "i";
      }
    return str;
  }

  private String step2(String str) {

    String[][] suffixes = {{"ational", "ate"},
        {"tional", "tion"},
        {"enci", "ence"},
        {"anci", "ance"},
        {"izer", "ize"},
        {"iser", "ize"},
        {"abli", "able"},
        {"alli", "al"},
        {"entli", "ent"},
        {"eli", "e"},
        {"ousli", "ous"},
        {"ization", "ize"},
        {"isation", "ize"},
        {"ation", "ate"},
        {"ator", "ate"},
        {"alism", "al"},
        {"iveness", "ive"},
        {"fulness", "ful"},
        {"ousness", "ous"},
        {"aliti", "al"},
        {"iviti", "ive"},
        {"biliti", "ble"}};
    NewString stem = new NewString();


    for (int index = 0; index < suffixes.length; index++) {
      if (hasSuffix(str, suffixes[index][0], stem)) {
        if (measure(stem.str) > 0) {
          str = stem.str + suffixes[index][1];
          return str;
        }
      }
    }

    return str;
  }

  private String step3(String str) {

    String[][] suffixes = {{"icate", "ic"},
        {"ative", ""},
        {"alize", "al"},
        {"alise", "al"},
        {"iciti", "ic"},
        {"ical", "ic"},
        {"ful", ""},
        {"ness", ""}};
    NewString stem = new NewString();

    for (int index = 0; index < suffixes.length; index++) {
      if (hasSuffix(str, suffixes[index][0], stem))
        if (measure(stem.str) > 0) {
          str = stem.str + suffixes[index][1];
          return str;
        }
    }
    return str;
  }

  private String step4(String str) {

    String[] suffixes = {"al", "ance", "ence", "er", "ic", "able", "ible", "ant", "ement", "ment", "ent", "sion", "tion",
        "ou", "ism", "ate", "iti", "ous", "ive", "ize", "ise"};

    NewString stem = new NewString();

    for (int index = 0; index < suffixes.length; index++) {
      if (hasSuffix(str, suffixes[index], stem)) {

        if (measure(stem.str) > 1) {
          str = stem.str;
          return str;
        }
      }
    }
    return str;
  }

  private String step5(String str) {

    if (str.charAt(str.length() - 1) == 'e') {
      if (measure(str) > 1) {/* measure(str)==measure(stem) if ends in vowel */
        String tmp = "";
        for (int i = 0; i < str.length() - 1; i++)
          tmp += str.charAt(i);
        str = tmp;
      } else if (measure(str) == 1) {
        String stem = "";
        for (int i = 0; i < str.length() - 1; i++)
          stem += str.charAt(i);

        if (!cvc(stem))
          str = stem;
      }
    }

    if (str.length() == 1)
      return str;
    if ((str.charAt(str.length() - 1) == 'l') && (str.charAt(str.length() - 2) == 'l') && (measure(str) > 1))
      if (measure(str) > 1) {/* measure(str)==measure(stem) if ends in vowel */
        String tmp = "";
        for (int i = 0; i < str.length() - 1; i++)
          tmp += str.charAt(i);
        str = tmp;
      }
    return str;
  }

  private String stripPrefixes(String str) {

    String[] prefixes = {"kilo", "micro", "milli", "intra", "ultra", "mega", "nano", "pico", "pseudo"};

    int last = prefixes.length;
    for (int i = 0; i < last; i++) {
      if (str.startsWith(prefixes[i])) {
        String temp = "";
        for (int j = 0; j < str.length() - prefixes[i].length(); j++)
          temp += str.charAt(j + prefixes[i].length());
        return temp;
      }
    }

    return str;
  }


  private String stripSuffixes(String str) {

    str = step1(str);
    if (str.length() >= 1)
      str = step2(str);
    if (str.length() >= 1)
      str = step3(str);
    if (str.length() >= 1)
      str = step4(str);
    if (str.length() >= 1)
      str = step5(str);

    return str;
  }

  /**
   * Takes a String as input and returns its stem as a String.
   */
  public String stripAffixes(String str) {

    str = str.toLowerCase();
    str = Clean(str);

    if ((str != "") && (str.length() > 2)) {
      str = stripPrefixes(str);

      if (str != "")
        str = stripSuffixes(str);

    }

    return str;
  } //stripAffixes

 
} //class

