import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class HMM {
	private static final int[][] P = null;
	public static Map<String,Double> token_lable_O = new HashMap<String, Double>();
	public static Map<String,Double> token_lable_I = new HashMap<String, Double>();
	public static Map<String,Double> token_lable_S = new HashMap<String, Double>();
	public static Map<String,Double> words_count = new HashMap<String, Double>();
	public static ArrayList<String> Pagelist=new ArrayList<String>();  //for each file the most_prob tag
	public static int []count;    //the sum of lable O
//	public static int count_I=0;     //the sum of lable I-GENE
	public static Boolean spill=false; 
	//public static int Num=100;
	public static int[]wordscount;
	public static Double bestProb=0.0;   //the biggest prob for a sentence
	public static ArrayList<String> bestpro_list=new ArrayList<String>();  //for each sentence the most_prob tag
	public static ArrayList<String> sentence=new ArrayList<String>(); 
	public static ArrayList<String> short_list=new ArrayList<String>();
	public static String RARElabel(String a)
	{
		//for part3 get the RARE subtype for a frequent words
		int flag=2;       //0 RARE   1 Last_Capitals
		               //2 All_Capitals  4 Numeric
		char []b=a.toCharArray();
		for(int i=0;i<b.length;i++)
		{			
			if((b[i]>='0')&&(b[i]<='9'))
			{
				a="_Numeric_";
				return a;				
			}
			if((b[i]<'A')||
					((b[i]>'Z')&&(b[i]<'a'))||
					(b[i]>'z'))
			{
				flag=0;
				break;
			}
		}
		int i=b.length-1;
		if((flag==0)&&
				(((b[i]>='A')&&(b[i]<='Z'))||
				 ((b[i]>='a')&&(b[i]<='z'))))			
			return "_Last_Capitals_";
		if(flag==2)
		{
	//		System.out.println(a+" "+"_All_Capitals_"+"\n");
			return "_All_Capitals_";
		}
		else
		{
	//		System.out.println(a+" "+"_RARE_"+"\n");
			return "_RARE_";
		}
		
		
	}
	public static void Proprocess() throws Exception, IOException
	{
		/**count the sum of eeach words
		 *  and replace the less frequent words with _RARE_
		 *  out the file to input.train**/
		String dir="gene.train";
		BufferedReader is=new BufferedReader(new InputStreamReader(new FileInputStream(dir),"UTF-8"));
		BufferedReader isr=new BufferedReader(new InputStreamReader(new FileInputStream(dir),"UTF-8"));
		OutputStreamWriter os=new OutputStreamWriter(new FileOutputStream("input.train"),"UTF-8");
		OutputStreamWriter osr=new OutputStreamWriter(new FileOutputStream("input.train_3"),"UTF-8");
		String line;
		while((line=is.readLine())!=null)
		{
			line=line.trim();
			if(line.equals(""))
				continue;
			String []token=line.split(" ");
			Double count=words_count.get(token[0]);
			if(count==null)
				count=0.0;
			count++;
			words_count.put(token[0], count);			
		}
		is.close();
		while((line=isr.readLine())!=null)
		{
			line=line.trim();
			if(line.equals(""))
			{
				os.write(line+"\n");
				osr.write(line+"\n");
				continue;
			}
			String []token=line.split(" ");
			Double count;
				count=words_count.get(token[0]);
			if((count!=null)&&(count>=5))
			{				
				os.write(line+"\n");
				osr.write(line+"\n");
				continue;
				
			}
			String b=line;
			b=RARElabel(token[0])+" "+token[1];
			//b=b.replace(token[0],);
		//	System.out.println(token[0]+"\t"+RARElabel(token[0]));
		//	line=line.replace(token[0],"_RARE_");
			line="_RARE_"+" "+token[1];
			
			os.write(line+"\n");
			osr.write(b+"\n");
			
		}
		isr.close();
		os.close();
		osr.close();
		System.out.println("done");
	}
    public static void ReadDocument() throws IOException, IOException
    {
    	//**读取训练数据**/
    	String dir="input.count_3";
    	wordscount=new int[2];
    	BufferedReader is=new BufferedReader(new InputStreamReader(new FileInputStream(dir),"UTF-8"));
//		OutputStreamWriter os=new OutputStreamWriter(new FileOutputStream("final_trigger"),"UTF-8");
		String line;
		String []label={"*","O","I-GENE","STOP"};
		while((line=is.readLine())!=null)
		{
			line=line.trim();
			if(line.equals(""))
				continue;
			String []token=line.split(" ");
			if(token[1].equals("WORDTAG"))
			{
				if(token[2].equals("O"))
				{
					Double count=Double.parseDouble(token[0]);
					token_lable_O.put(token[3], count);
					wordscount[0]++;
				}
				else if(token[2].equals("I-GENE"))
				{
					Double count=Double.parseDouble(token[0]);
					token_lable_I.put(token[3], count);
					wordscount[1]++;
				}	
				continue;
			}
			else if(token[1].equals("1-GRAM"))
			{
				if(token[2].equals("O"))
					count[0]=Integer.parseInt(token[0]);
				else if(token[2].equals("I-GENE"))
					count[1]=Integer.parseInt(token[0]);
				continue;
			}
			else if(token[1].equals("2-GRAM"))
			{
				String key=token[2]+"_"+token[3];
				Double count=Double.parseDouble(token[0]);
				token_lable_S.put(key, count);
				continue;
			}
			else if(token[1].equals("3-GRAM"))
			{
				String key=token[2]+"_"+token[3]+"_"+token[4];
				Double count=Double.parseDouble(token[0]);
				token_lable_S.put(key, count);	
				continue;
			}
				
		}
		is.close();		
    }
    public static Double Prob(String a,String b,String c)
    /**the prob(c|a,b)**/   //递归是需要，当前没用到
    {
    	String token1=a+"_"+b+"_"+c;
    	String token2=a+"_"+b;
    	Double count=token_lable_S.get(token1);
    	Double count_all=token_lable_S.get(token2);
    	if((count)!=null)
    	{
    		if(count_all!=0)
    		{
    			Double probi=(Double)(count)/count_all;
    		return probi;
    		}
    		else
    		{
    			System.out.println("文件读取出错"+token2+"的值为0");
    			return -1.0;
    		}
    	}
    	return 0.0;    	
    }
    @SuppressWarnings("unchecked")
	public static void Search(int i,ArrayList<String>list,Double P)
    {//递归求最佳链表，没用动态规划
    	Double pn;
    	Double an;
    	if((i-2)>=sentence.size())
    	{   
    		pn=Prob(list.get(i-2),list.get(i-1),"STOP");    	
    	    if(pn>0)
    	    {
    	    	 P=P*pn;
    		     if(P>bestProb)
    		     {
    		    	 bestpro_list.clear();
    		    	 bestpro_list.addAll(list);
    		        bestProb=P;   
    		     }
    	    }
    		return;
    	}
    	if(((an=token_lable_O.get(sentence.get(i-2)))!=null)&&
    			((pn=Prob(list.get(i-2).toString(),list.get(i-1).toString(),"O"))>0)
    			)
    	{    		
    		ArrayList<String> list1=new ArrayList<String>();
    		list1=(ArrayList<String>) list.clone();
    		list1.add("O");   		
    		Search(i+1,list1,P*pn*(an/count[0]));
    		list1.clear();
    		list1=null;
    	}
    	if(((an=token_lable_I.get(sentence.get(i-2)))!=null)&&((pn=Prob(list.get(i-2).toString(),list.get(i-1).toString(),"I-GENE"))>0)
    			)
    	{
    		ArrayList<String> list1=new ArrayList<String>();
    		list1=(ArrayList<String>) list.clone();
    		list.clear();
    		list1.add("I-GENE");   		
    		Search(i+1,list1,P*pn*(an/count[1]));
    		list1.clear();
    	}    
    	
    }
    public static void NoDPSearch(ArrayList<String>list)
    {//  	
    	int i=2;
    	Double p1,p2,a1,a2;
    	
    	while((i-2)<sentence.size())
    	{
    		p1=Prob(list.get(i-2).toString(),list.get(i-1).toString(),"O");
    		p2=Prob(list.get(i-2).toString(),list.get(i-1).toString(),"I-GENE");
    		a1=token_lable_O.get(sentence.get(i-2));
    		a2=token_lable_I.get(sentence.get(i-2));
    		if(a1==null)
    			a1=0.0;
    		if(a2==null)
    			a2=0.0;
    		a1=a1/count[0];
    		a2=a2/count[1];
    		p1=p1*a1;
    		p2=p2*a2;
    		if((a1>a2)&&(p1>p2))
    		{
    			list.add("O");   			
    		}
    		else
    		{  			
    			list.add("I-GENE");
    		}
    		i++;   		
    	}
    	bestpro_list.addAll(list);
    }
    public static String getlabel(int a)
    {
    	if(a==-1)
    		return "*";
    	else if(a==0)
    		return "O";
    	else if(a==1)
    		return "I-GENE";
    	else
		    return "STOP";
    	
    }
    public static int getbig(Double []P)
    {
    	int k=0;;
    	Double big=P[0];
    
    	for(int i=1;i<4;i++)
    	{
    		if(P[i]>=big)
    		{
    			k=i;
    			big=P[i];
    		}
    	}
    	return k;
    }
    public static Double prob(int a,int b)
    {
    	String token=getlabel(a)+"_"+getlabel(b);
    	Double count1=token_lable_S.get(token);
    	if(count1==null)
    		count1=0.0;
    	return count1/count[a];
    }
    public static void DPSearch_firstorder()
    {
    	Double []prob=new Double[2];
    	Double[] a=new Double[2];
    	int [][]prior=new int[sentence.size()+1][2];
    	for(int i=0;i<sentence.size()+1;i++)
    		for(int j=0;j<2;j++)
    			prior[i][j]=-2;
    	Double [][]P=new Double[sentence.size()+1][2];
    	P[0][0]=1.0;
    	P[0][1]=1.0;
    	int m=0;
    	for(;m<sentence.size();m++)
    	{
    		for(int j=0;j<2;j++)
    		{
    			a[0]=token_lable_O.get(sentence.get(m));
    			a[1]=token_lable_I.get(sentence.get(m));
    			if(a[0]==null)
    				a[0]=0.0;
    			if(a[1]==null)
    				a[1]=0.0;
    			a[j]=a[j]/count[j];
    			prob[0]=P[m][0]*prob(0,j)*a[j];
    			prob[1]=P[m][1]*prob(1,j)*a[j];
    			if(prob[0]>prob[1])
    			{
    				prior[m][j]=0;
    				P[m+1][j]=prob[0];
    			}
    			else
    			{
    				prior[m][j]=1;
    				P[m+1][j]=prob[1];
    			}
    			
    			
    		}
    	}
    		if(a[0]==null)
				a[0]=0.0;
    		prob[0]=P[m][0]*prob(0,2);
    		prob[1]=P[m][1]*prob(1,2);
    		if(prob[0]>prob[1])
    			prior[m][0]=0;
    		else 
    			prior[m][0]=1;
    		int k=sentence.size();
    		int []A=new int[k];
    		A[k-1]=prior[m][0];
    		k--;
    		while(k>0)
    		{
    			A[k-1]=prior[k][A[k]];
    			k--;   		
    		}
    		for(k=0;k<A.length;k++)
    			bestpro_list.add(getlabel(A[k]));
    			
    	
    }
    public static void DPSearch()
    {
    	Double[] prob=new Double[4];
    	Double []a=new Double[2];
    	int[][] M=new int[sentence.size()+1][4];
    	for(int i=0;i<sentence.size()+1;i++)
    		for(int j=0;j<4;j++)
    			M[i][j]=-2;
    	M[0][0]=-1;
    	M[0][1]=-1;
    	M[0][2]=-1;
    	M[0][3]=-1;
    	Double [][]P=new Double[sentence.size()+1][8];
    	Double a3,a4;
		a[0]=token_lable_O.get(sentence.get(0));
		a[1]=token_lable_I.get(sentence.get(0));
		if(a[0]==null)
			a[0]=0.0;
		if(a[1]==null)
			a[1]=0.0;
		a[0]=a[0]/count[0];
		a[1]=a[1]/count[1];
    	P[0][0]=Prob("*","*","O")*a[0];   	
    	// P[0][1]=Prob("*","*","O")*a1;
    	P[0][4]=Prob("*","*","I-GENE")*a[1];
    	// P[0][3]=Prob("*","*","I-GENE")*a2;
    	a[0]=token_lable_O.get(sentence.get(1));
		a[1]=token_lable_I.get(sentence.get(1));
		if(a[0]==null)
			a[0]=0.0;
		if(a[1]==null)
			a[1]=0.0;
		a[0]=a[0]/count[0];
		a[1]=a[1]/count[1];
		
		for(int j=0;j<2;j++)
		{
		   a[j]=a[j]/count[j];
		   P[1][j*4+0]=P[0][0]*Prob("*",getlabel(0),getlabel(j))*a[j];
		   P[1][j*4+2]=P[0][4]*Prob("*",getlabel(1),getlabel(j))*a[j];
		   P[1][j*4+1]=P[1][0];
		   P[1][j*4+3]=P[1][2];
		   if(P[1][j*4+0]>P[1][j*4+2])
		   {
			   M[1][j*2+0]=-1;
			   M[1][j*2+1]=0;
			   P[1][j*4]=P[1][j*4+0];
		   }
		   else
		   {
			   M[1][j*2]=-1;
			   M[1][j*2+1]=1;
			   P[1][j*4]=P[1][j*4+2];
		   }
		}
		int m=2;
		if(sentence.size()==2)
			System.out.println();
    	for( m=2;m<sentence.size();m++)
    	{
    		for(int j=0;j<2;j++)
    			{
    				a[0]=token_lable_O.get(sentence.get(m));
    				a[1]=token_lable_I.get(sentence.get(m));
    				a3=token_lable_O.get(sentence.get(m-1));
    				a4=token_lable_I.get(sentence.get(m-1));
    				if(a[0]==null)
    					a[0]=0.0;
    				if(a[1]==null)
    					a[1]=0.0;
    				if(a3==null)
    					a3=0.0;
    				if(a4==null)
    					a4=0.0;
    				a3=a3/count[0];
    				a4=a4/count[1];
    				a[j]=a[j]/count[j];
    				P[m][j*4+0]=P[m-2][0]*Prob(getlabel(M[m-2][1]),getlabel(0),getlabel(0))*a3*Prob(getlabel(0),getlabel(0),getlabel(j))*a[j];
    				P[m][j*4+1]=P[m-2][4]*Prob(getlabel(M[m-2][3]),getlabel(1),getlabel(0))*a3*Prob(getlabel(1),getlabel(0),getlabel(j))*a[j];
    				P[m][j*4+2]=P[m-2][0]*Prob(getlabel(M[m-2][1]),getlabel(0),getlabel(1))*a4*Prob(getlabel(0),getlabel(1),getlabel(j))*a[j];
    				P[m][j*4+3]=P[m-2][4]*Prob(getlabel(M[m-2][3]),getlabel(1),getlabel(1))*a4*Prob(getlabel(1),getlabel(1),getlabel(j))*a[j];
    				for(int i=0;i<4;i++)
    					prob[i]=P[m][j*4+i];
    				int p=getbig(prob);
    				P[m][j*4]=P[m][p+j*4];
    				M[m][j*2+0]=p%2;
    				M[m][j*2+1]=p/2;
    			}
    			
    	}
    		a3=token_lable_O.get(sentence.get(m-1));
			a4=token_lable_I.get(sentence.get(m-1));
			if(a3==null)
				a3=0.0;
			if(a4==null)
				a4=0.0;
			a3=a3/count[0];
			a4=a4/count[1];
    		P[m][0]=P[m-2][0]*Prob(getlabel(M[m-2][1]),getlabel(0),getlabel(0))*a3*Prob(getlabel(0),getlabel(0),"STOP");			
    	    P[m][1]=P[m-2][4]*Prob(getlabel(M[m-2][3]),getlabel(1),getlabel(0))*a3*Prob(getlabel(1),getlabel(0),"STOP");
			P[m][2]=P[m-2][0]*Prob(getlabel(M[m-2][1]),getlabel(0),getlabel(1))*a4*Prob(getlabel(0),getlabel(1),"STOP");
			P[m][3]=P[m-2][4]*Prob(getlabel(M[m-2][3]),getlabel(1),getlabel(1))*a4*Prob(getlabel(1),getlabel(1),"STOP");
			int p=getbig(P[m]);
			M[m][0]=p%2;
			M[m][1]=p/2;
		   int k=sentence.size();
		   int []A=new int[sentence.size()];
		   A[k-1]=M[k][1];
		   A[k-2]=M[k][0];
		   k=k-2;
		  while(k>0)
		{
			A[k-1]=M[k][A[k]*2+1];
			if(k<=1)
				break;
			A[k-2]=M[k][A[k]*2];
			k=k-2;
		}
		for(int i=0;i<A.length;i++)
			bestpro_list.add(getlabel(A[i]));
	//	System.out.println(bestpro_list);    	
    }
    public static void bestlist(ArrayList<String>sentence)
    {
    	/**for each sentence search (y-1,y0,y1.y2,,,yn+1)=arg P( x1,x2,x,,xn,y1,y2,,yn)
    	 ***/   
    	DPSearch();
    	//DPSearch_firstorder();
    	//System.out.println(bestpro_list);
    }
    public static void Tagfileprocess() throws IOException
    {
    	/**怼要标记文件进行预处理，把低频词换位“_RARE_”,得到文件ReadFile**/
    	String dir="gene.dev";
    	BufferedReader is=new BufferedReader(new InputStreamReader(new FileInputStream(dir),"UTF-8"));
    	OutputStreamWriter os=new OutputStreamWriter(new FileOutputStream("ReadFile"),"UTF-8");
    	OutputStreamWriter osr=new OutputStreamWriter(new FileOutputStream("ReadFile_3"),"UTF-8");
    	OutputStreamWriter os1=new OutputStreamWriter(new FileOutputStream("osout"),"UTF-8");
    	String line;
    	while((line=is.readLine())!=null)
    	{
    		line=line.trim();
    		
    		if(line.equals(""))
    		{
    			os.write(line+"\n");
    			osr.write(line+"\n");
    			continue;
    			
    		}  		
    		Double	count=words_count.get(line);
    	//	System.out.println(line+" "+count);
    		String b=line;
    		if((count==null)||(count<5))
    		{
    		    b=RARElabel(line);
    		    os1.write(line+" "+b+"\n");
    			line="_RARE_";
    		}
    		
    		os.write(line+"\n");
    		osr.write(b+"\n");
    		
    		
    	}
    	is.close();
    	os.close();
    	osr.close();
    	os1.close();
    		
    }
    public static void out() throws IOException, IOException
    {
    	String dir="ReadFile_3";
    	BufferedReader is=new BufferedReader(new InputStreamReader(new FileInputStream(dir),"UTF-8"));
    	BufferedReader is1=new BufferedReader(new InputStreamReader(new FileInputStream("gene.dev"),"UTF-8"));
    	OutputStreamWriter os=new OutputStreamWriter(new FileOutputStream("outputTag3"),"UTF-8");
    	String line;
    	while((line=is.readLine())!=null)
    	{
    		line=line.trim();
    		if((line.equals("")))
    		{		 
    			if(sentence.size()==0)
    				continue;
    	//		System.out.println(sentence.size());
    			bestlist(sentence);
    			if(bestpro_list!=null)
    			{
    				Pagelist.addAll(bestpro_list);
    		//		System.out.println("添加成功");  
    				bestpro_list.clear();
    				sentence.clear();
    				bestProb=0.0;
    				continue;
    			}
    			else{
    				System.out.println("添加失败");
    				is.close();
    				is1.close();
    				os.close();
    				return;
    			}   			
    		}
    		sentence.add(line);
    	}
    	is.close();
    	int i=0;
    	while((line=is1.readLine())!=null)
    	{
    		line=line.trim();
    		if(line.equals(""))
    		{
    			os.write("\n");
    			continue;
    		}   		
    		os.write(line+" "+Pagelist.get(i)+"\n");
    		i++;  
    	}
    	is1.close();
    	os.close();
    }
    public static void outTag1() throws IOException, IOException
    {
    	/**for each word search y=arg p(x|y) **/
    	
    	String dir="ReadFile";   	
		BufferedReader is=new BufferedReader(new InputStreamReader(new FileInputStream(dir),"UTF-8"));    	
		BufferedReader isr=new BufferedReader(new InputStreamReader(new FileInputStream("gene.dev"),"UTF-8"));   	
		OutputStreamWriter os=new OutputStreamWriter(new FileOutputStream("outTag1"),"UTF-8");
    	String line,line1;
    	while((line=is.readLine())!=null)
    	{
    		line=line.trim();
    		if(line.equals(""))
    		{
    			line1=isr.readLine();
    			line1=line1.trim();
    			os.write(line1+"\n");
    			continue;
    		}
    		if((line1=isr.readLine())==null)
    		{
    			is.close();
    	    	isr.close();
    	    	os.close();
    			return;
    		}
    		String tag;
    		Double count1=token_lable_O.get(line);
    		Double count2=token_lable_I.get(line);
    		
    		if(count1==null)
    			count1=0.0;
    		if(count2==null)
    			count2=0.0;
    		Double prob1=count1/count[0];
    		Double prob2=count2/count[1];
    		if(prob1>prob2)
    			tag="O";
    		else
    			tag="I-GENE";
    		os.write(line1+" "+tag+"\n");
    	}
    	is.close();
    	isr.close();
    	os.close();
    	System.out.println("done");
    }
    public static void main(String[] args) throws IOException, Exception
    {
    	count=new int[2];
    	ReadDocument();
   // 	Proprocess();
    	//System.out.println(RARElabel("I、l"));
   // 	Tagfileprocess();
//    	outTag1();
    	out();
    	System.out.println("done");
    }
}
