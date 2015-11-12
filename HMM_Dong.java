import java.util.*;
import java.io.*;

public class HMM_Dong
{
	static final String O = "O";
	static final String I = "I-GENE";
	static String[] states = new String[] { O, I };

	static HashSet<String> norare_words = new HashSet<String>();

	static HashMap<String, Double> transition_probability = new HashMap<String, Double>();
	static HashMap<String, Double> emission_probability = new HashMap<String, Double>();

	static final String train_path = "./data/gene.train";
	static final String train_count_path = "./data/gene.counts";
	static final String train_norare_path = "./data/gene.train.norare";
	static final String train_norare_count_path = "./data/gene.train.norare.counts";
	static final String train_splitrare_path = "./data/gene.train.splitrare";
	static final String train_splitrare_count_path = "./data/gene.train.splitrare.counts";

	static final String dev_path = "./data/gene.dev";
	static final String dev_p1_path = "./data/gene_dev.p1.out";
	static final String dev_p2_path = "./data/gene_dev.p2.out";
	static final String dev_p3_path = "./data/gene_dev.p3.out";

	// 筛选小于5次的词
	static void filter() throws Exception
	{
		HashMap<String, Integer> word_cnt = new HashMap<String, Integer>();
		BufferedReader br = new BufferedReader(new FileReader(train_count_path));
		String line = null;
		while ((line = br.readLine()) != null)
		{
			String[] strs = line.split(" ");
			int count = Integer.parseInt(strs[0]);
			String type = strs[1];

			if (type.equals("WORDTAG"))
			{
				String word = strs[3];

				if (word_cnt.containsKey(word) == false)
				{
					word_cnt.put(word, count);
				}
				else
				{
					word_cnt.put(word, count + word_cnt.get(word));
				}
			}
		}
		br.close();

		for (Map.Entry<String, Integer> entry : word_cnt.entrySet())
		{
			String word = entry.getKey();
			int count = entry.getValue();
			if (count >= 5 && word.equals("_RARE_") == false)
			{
				norare_words.add(word);
			}
		}

		FileWriter fw = new FileWriter(train_norare_path);
		br = new BufferedReader(new FileReader(train_path));
		while ((line = br.readLine()) != null)
		{
			if (line.equals(""))
			{
				fw.write("\n");
			}
			else
			{
				String[] strs = line.split(" ");
				String word = strs[0];
				if (norare_words.contains(word))
				{
					fw.write(line + "\n");
				}
				else
				{
					fw.write("_RARE_ " + strs[1] + "\n");
				}
			}
		}
		br.close();
		fw.close();
	}

	// 初始化发射概率和转移概率
	static void init(String count_path) throws Exception
	{
		int O_count = 0, I_count = 0;
		HashMap<String, Integer> word_cnt = new HashMap<String, Integer>();
		HashMap<String, Integer> word_tag_cnt = new HashMap<String, Integer>();
		HashMap<String, Integer> trigram_cnt = new HashMap<String, Integer>();
		HashMap<String, Integer> bigram_cnt = new HashMap<String, Integer>();

		BufferedReader br = new BufferedReader(new FileReader(count_path));
		String line = null;
		while ((line = br.readLine()) != null)
		{
			String[] strs = line.split(" ");
			int count = Integer.parseInt(strs[0]);
			String type = strs[1];

			if (type.equals("WORDTAG"))
			{
				String tag = strs[2];
				String word = strs[3];
				String key = word + " || " + tag;
				word_tag_cnt.put(key, count);

				if (word_cnt.containsKey(word) == false)
				{
					word_cnt.put(word, count);
				}
				else
				{
					word_cnt.put(word, count + word_cnt.get(word));
				}
			}

			if (type.equals("1-GRAM"))
			{
				if (strs[2].equals(I))
				{
					I_count = count;
				}
				else if (strs[2].equals(O))
				{
					O_count = count;
				}
			}

			if (type.equals("2-GRAM"))
			{
				String key = strs[2] + " || " + strs[3];
				bigram_cnt.put(key, count);
			}

			if (type.equals("3-GRAM"))
			{
				String key = strs[2] + " || " + strs[3] + " || " + strs[4];
				trigram_cnt.put(key, count);
			}
		}
		br.close();

		for (Map.Entry<String, Integer> entry : word_cnt.entrySet())
		{
			String word = entry.getKey();
			int count = entry.getValue();
			if (count >= 5 && !word.equals("_RARE_")
					&& !word.equals("_Numeric_")
					&& !word.equals("_AllCapital_")
					&& !word.equals("_EndCapital_"))
			{
				norare_words.add(word);
			}
		}

		for (Map.Entry<String, Integer> entry : word_tag_cnt.entrySet())
		{
			String[] strs = entry.getKey().split(" \\|\\| ");
			String word = strs[0];
			String tag = strs[1];
			int tag_word_cnt = entry.getValue();
			double tag_word_pro = 0;
			if (tag.equals(O))
			{
				tag_word_pro = (double) tag_word_cnt / O_count;
			}
			else if (tag.equals(I))
			{
				tag_word_pro = (double) tag_word_cnt / I_count;
			}
			emission_probability.put(tag + " || " + word, tag_word_pro);
		}

		for (Map.Entry<String, Integer> entry : trigram_cnt.entrySet())
		{
			String key = entry.getKey();
			int trigram_count = entry.getValue();
			String[] strs = key.split(" \\|\\| ");
			String bigram_key = strs[0] + " || " + strs[1];
			int bigram_count = bigram_cnt.get(bigram_key);
			double pro = (double) trigram_count / bigram_count;
			transition_probability.put(key, pro);
		}
	}

	static boolean isRareWord(String word)
	{
		if (norare_words.contains(word))
		{
			return false;
		}
		else
		{
			return true;
		}
	}

	public static void part1() throws Exception
	{
		BufferedReader br = new BufferedReader(new FileReader(dev_path));
		String line = null;
		FileWriter fw = new FileWriter(dev_p1_path);
		while ((line = br.readLine()) != null)
		{
			if (line.equals(""))
			{
				fw.write("\n");
				continue;
			}
			String word = line;
			if (isRareWord(word))
			{
				word = "_RARE_";
			}

			String key = O + " || " + word;
			double O_pro = 0, I_pro = 0;
			if (emission_probability.containsKey(key))
			{
				O_pro = emission_probability.get(key);
			}

			key = I + " || " + word;
			if (emission_probability.containsKey(key))
			{
				I_pro = emission_probability.get(key);
			}

			if (O_pro >= I_pro)
			{
				fw.write(line + " " + O + "\n");
			}
			else
			{
				fw.write(line + " " + I + "\n");
			}
		}
		fw.close();
	}

	static HashSet<String> K(int k)
	{
		HashSet<String> set = new HashSet<String>();
		if (k == -1 || k == 0)
		{
			set.add("*");
		}
		else
		{
			set.add(O);
			set.add(I);
		}
		return set;
	}

	static double q(String w, String u, String v)
	{
		return transition_probability.get(w + " || " + u + " || " + v);
	}

	static double e(String u, String x)
	{
		String key = u + " || " + x;
		if (emission_probability.containsKey(key))
		{
			return emission_probability.get(u + " || " + x);
		}
		else
		{
			return 0;
		}
	}

	static class Pi
	{
		int k;
		String u;
		String v;
		double pro;

		Pi(int k, String u, String v)
		{
			this.k = k;
			this.u = u;
			this.v = v;
		}
	}

	public static ArrayList<String> viterbi(String[] x) throws Exception
	{
		HashMap<String, Double> pi = new HashMap<String, Double>();
		HashMap<String, String> bp = new HashMap<String, String>();
		pi.put(0 + " || * || *", 1.0);

		int n = x.length - 1;
		String[] y = new String[n + 1];

		for (int k = 1; k <= n; ++k)
		{
			for (String u : K(k - 1))
			{
				for (String v : K(k))
				{
					String max_w = null;
					double max_pro = -1;
					for (String w : K(k - 2))
					{
						double pro = pi.get((k - 1) + " || " + w + " || " + u)
								* q(w, u, v) * e(v, x[k]);
						if (pro > max_pro)
						{
							max_pro = pro;
							max_w = w;
						}
					}
					bp.put(k + " || " + u + " || " + v, max_w);
					pi.put(k + " || " + u + " || " + v, max_pro);
				}
			}
		}

		double max_pro = -1;
		String max_u = null;
		String max_v = null;
		for (String u : K(n - 1))
		{
			for (String v : K(n))
			{
				double pro = pi.get(n + " || " + u + " || " + v)
						* q(u, v, "STOP");
				if (pro > max_pro)
				{
					max_u = u;
					max_v = v;
					max_pro = pro;
				}
			}
		}

		y[n - 1] = max_u;
		y[n] = max_v;

		for (int k = n - 2; k >= 1; --k)
		{
			y[k] = bp.get((k + 2) + " || " + y[k + 1] + " || " + y[k + 2]);
		}

		ArrayList<String> tagList = new ArrayList<String>();
		for (int i = 1; i <= n; ++i)
		{
			tagList.add(y[i]);
		}
		return tagList;
	}

	public static void part2() throws Exception
	{
		BufferedReader br = new BufferedReader(new FileReader(dev_path));
		String line = null;
		ArrayList<String> wordList = new ArrayList<String>();
		FileWriter fw = new FileWriter(dev_p2_path);
		while ((line = br.readLine()) != null)
		{
			if (line.equals(""))
			{
				int n = wordList.size();
				String[] x = new String[n + 1];
				for (int i = 0; i < wordList.size(); ++i)
				{
					String word = wordList.get(i);
					if (isRareWord(word))
					{
						word = "_RARE_";
					}
					x[i + 1] = word;
				}

				ArrayList<String> tagList = viterbi(x);

				for (int i = 0; i < tagList.size(); ++i)
				{
					String tag = tagList.get(i);
					String word = wordList.get(i);
					fw.write(word + " " + tag + "\n");
				}
				fw.write("\n");
				wordList.clear();
			}
			else
			{
				wordList.add(line);
			}
		}
		br.close();
		fw.close();
	}

	public static boolean isNumeric(String str)
	{
		for (int i = 0; i < str.length(); i++)
		{
			if (Character.isDigit(str.charAt(i)))
			{
				return true;
			}
		}
		return false;
	}

	public static boolean isAllCapital(String str)
	{
		for (int i = 0; i < str.length(); i++)
		{
			if (!Character.isUpperCase(str.charAt(i)))
			{
				return false;
			}
		}
		return true;
	}

	public static boolean isEndCapital(String str)
	{
		int upper_cnt = 0;
		for (int i = 0; i < str.length() - 1; i++)
		{
			if (Character.isUpperCase(str.charAt(i)))
			{
				++upper_cnt;
			}
		}
		if (upper_cnt != str.length() - 1)
		{
			if (Character.isUpperCase(str.charAt(str.length() - 1)))
			{
				return true;
			}
		}
		return false;
	}

	public static void splitRareWords() throws Exception
	{
		HashMap<String, Integer> word_cnt = new HashMap<String, Integer>();
		BufferedReader br = new BufferedReader(new FileReader(train_count_path));
		String line = null;
		while ((line = br.readLine()) != null)
		{
			String[] strs = line.split(" ");
			int count = Integer.parseInt(strs[0]);
			String type = strs[1];

			if (type.equals("WORDTAG"))
			{
				String word = strs[3];

				if (word_cnt.containsKey(word) == false)
				{
					word_cnt.put(word, count);
				}
				else
				{
					word_cnt.put(word, count + word_cnt.get(word));
				}
			}
		}
		br.close();

		for (Map.Entry<String, Integer> entry : word_cnt.entrySet())
		{
			String word = entry.getKey();
			int count = entry.getValue();
			if (count >= 5)
			{
				norare_words.add(word);
			}
		}

		FileWriter fw = new FileWriter(train_splitrare_path);
		br = new BufferedReader(new FileReader(train_path));
		while ((line = br.readLine()) != null)
		{
			if (line.equals(""))
			{
				fw.write("\n");
			}
			else
			{
				String[] strs = line.split(" ");
				String word = strs[0];
				if (norare_words.contains(word))
				{
					fw.write(line + "\n");
				}
				else
				{
					if (isNumeric(word))
					{
						fw.write("_Numeric_ " + strs[1] + "\n");
					}
					else if (isAllCapital(word))
					{
						fw.write("_AllCapital_ " + strs[1] + "\n");
					}
					else if (isEndCapital(word))
					{
						fw.write("_EndCapital_ " + strs[1] + "\n");
					}
					else
					{
						fw.write("_RARE_ " + strs[1] + "\n");
					}
				}
			}
		}
		br.close();
		fw.close();
	}

	public static void part3() throws Exception
	{

		BufferedReader br = new BufferedReader(new FileReader(dev_path));
		String line = null;
		ArrayList<String> wordList = new ArrayList<String>();
		FileWriter fw = new FileWriter(dev_p3_path);
		while ((line = br.readLine()) != null)
		{
			if (line.equals(""))
			{
				int n = wordList.size();
				String[] x = new String[n + 1];
				for (int i = 0; i < wordList.size(); ++i)
				{
					String word = wordList.get(i);

					if (isRareWord(word))
					{
						if (isNumeric(word))
						{
							word = "_Numeric_";
						}
						else if (isAllCapital(word))
						{
							word = "_AllCapital_";
						}
						else if (isEndCapital(word))
						{
							word = "_EndCapital_";
						}
						else
						{
							word = "_RARE_";
						}
					}
					x[i + 1] = word;
				}

				ArrayList<String> tagList = viterbi(x);
				for (int i = 0; i < tagList.size(); ++i)
				{
					String tag = tagList.get(i);
					String word = wordList.get(i);
					fw.write(word + " " + tag + "\n");
				}
				fw.write("\n");
				wordList.clear();
			}
			else
			{
				wordList.add(line);
			}
		}
		br.close();
		fw.close();
	}

	public static void main(String[] args) throws Exception
	{

		//		filter();

		//		init(train_norare_count_path);
		//		part1();
		//		part2();

		//		splitRareWords();

		init(train_splitrare_count_path);
		part3();

		System.exit(0);
	}

}
