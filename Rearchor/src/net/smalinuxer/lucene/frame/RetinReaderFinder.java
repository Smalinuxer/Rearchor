package net.smalinuxer.lucene.frame;

import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import net.smalinuxer.lucene.frame.IndexWriterQueue.IndexData;
import net.smalinuxer.lucene.utils.LuceneConfig;
import net.smalinuxer.lucene.utils.MMSegAnalyzer;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.Term;
import org.apache.lucene.queries.function.FunctionQuery;
import org.apache.lucene.queries.function.valuesource.OrdFieldSource;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.PhraseQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.search.WildcardQuery;
import org.apache.lucene.search.highlight.Fragmenter;
import org.apache.lucene.search.highlight.Highlighter;
import org.apache.lucene.search.highlight.InvalidTokenOffsetsException;
import org.apache.lucene.search.highlight.QueryScorer;
import org.apache.lucene.search.highlight.SimpleHTMLFormatter;
import org.apache.lucene.search.highlight.SimpleSpanFragmenter;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;

public class RetinReaderFinder {
	
	private static RetinReaderFinder finder;
	
	public static synchronized RetinReaderFinder newInstance(){
		if(finder == null){
			finder = new RetinReaderFinder();
		}
		return finder; 
	}
	
	private static DirectoryReader reader;
	
	private Directory directory;
	
	private static volatile boolean openIfChanged = false;
	
	private Timer timer;	//should be close
	
	private RetinReaderFinder(){
		timer = new Timer();  
		
		/*
		SimpleDateFormat dateFormatter = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
		Date startDate = null;
		try {
			startDate = dateFormatter.parse(LuceneConfig.LUCENE_REFRESH_START_TIME);
		} catch (ParseException e1) {
			e1.printStackTrace();
		}  
		*/
		
		timer.scheduleAtFixedRate(new TimerTask() {
			
			@Override
			public void run() {
				openIfChanged = true;
			}
		},LuceneConfig.LUCENE_REFRESH_START_DATE_NOW,LuceneConfig.LUCENE_REFRESH_READER);
		try {
			String path = System.getProperty("user.dir") + LuceneConfig.LUCENE_STORE_DIR;
			directory = FSDirectory.open(new File(path));
			reader = DirectoryReader.open(directory);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	
	
	
	/*
	public void searToHighlighterCss(Analyzer analyzer,IndexSearcher searcher) throws IOException, InvalidTokenOffsetsException{  
        Term term =new Term("sex", "男生");//查询条件，意思是我要查找性别为“男生”的人  
        TermQuery query =new TermQuery(term);  
        TopDocs docs =searcher.search(query, 10);//查找  
        System.out.println("searcherDoc()->男生人数："+docs.totalHits);  
          
        *//**自定义标注高亮文本标签*//*  
        SimpleHTMLFormatter formatter = new SimpleHTMLFormatter("<span class=\"hightlighterCss\">","</span>");  
        *//**创建QueryScorer*//*  
        QueryScorer scorer=new QueryScorer(query);  
        *//**创建Fragmenter*//*  
        Fragmenter fragmenter = new SimpleSpanFragmenter(scorer);  
        Highlighter highlight=new Highlighter(formatter,scorer);  
        highlight.setTextFragmenter(fragmenter);  
          
        int seq=0;  
        for(ScoreDoc doc:docs.scoreDocs){//获取查找的文档的属性数据  
            seq++;  
            int docID=doc.doc;  
            Document document =searcher.doc(docID);  
            String str="序号："+seq+",ID:"+document.get("id")+",姓名："+document.get("name")+"，性别：" ;  
            String value =document.get("sex");  
            if (value != null) {    
                TokenStream tokenStream = analyzer.tokenStream("sex", new StringReader(value));    
                String str1 = highlight.getBestFragment(tokenStream, value);    
                str=str+str1;    
            }     
            System.out.println("查询出人员:"+str);  
        }  
    }  */

	/**
	 * @param keyWord
	 * @param num
	 * @return
	 * @deprecated
	 */
	public List<IndexData> search0(List<String> keyWords,int num) {
		List<IndexData> list = null;
		MMSegAnalyzer analyzer = null;
		try {
			IndexSearcher searcher = getSearcher();
			analyzer = new MMSegAnalyzer();
			PhraseQuery query = new PhraseQuery();
			for(String str : keyWords) query.add(new Term("content", str));
			TopDocs td = searcher.search(query, num);
			
			SimpleHTMLFormatter formatter = new SimpleHTMLFormatter("<b style=\"color:red\">","</b>");
	        QueryScorer scorer=new QueryScorer(query);  
	        Fragmenter fragmenter = new SimpleSpanFragmenter(scorer);  
	        Highlighter highlight=new Highlighter(formatter,scorer);  
	        highlight.setTextFragmenter(fragmenter);  
	        
	        list = new ArrayList<IndexWriterQueue.IndexData>();
			for(ScoreDoc sDoc : td.scoreDocs){
				Document doc = searcher.doc(sDoc.doc);
				String value = doc.get("content");
	            TokenStream tokenStream = analyzer.tokenStream("content", new StringReader(value));    
	            String _frag = highlight.getBestFragment(tokenStream, value);
	            IndexData data = new IndexData();
	            data.url = doc.get("url");
	            data.title = doc.get("title");
	            data.content = _frag;
	            list.add(data);
			}
		} catch (IOException e) {
			e.printStackTrace();
		} catch (InvalidTokenOffsetsException e) {
			e.printStackTrace();
		}  finally{
			if(analyzer != null){
				analyzer.close();
				analyzer=null;
			}
		}
		return list;
	}
	
	
	/**
	 * @param keyWord
	 * @param num
	 * @return
	 */
	public List<IndexData> search0(String keyWord,int num) {
		List<IndexData> list = null;
		MMSegAnalyzer analyzer = null;
		try {
			IndexSearcher searcher = getSearcher();
			analyzer = new MMSegAnalyzer();
			KeyWordQueryParse parse = new KeyWordQueryParse("content",analyzer);
			Query query = parse.parse(keyWord);
//			Term term =new Term("content", keyWord);	
//	        TermQuery query =new TermQuery(term);  
			TopDocs td = searcher.search(query, num);
			
//	        SimpleHTMLFormatter formatter = new SimpleHTMLFormatter("<span class=\"hightlighterCss\">","</span>");  
			SimpleHTMLFormatter formatter = new SimpleHTMLFormatter("<b style=\"color:red\">","</b>");
	        QueryScorer scorer=new QueryScorer(query);  
	        Fragmenter fragmenter = new SimpleSpanFragmenter(scorer);  
	        Highlighter highlight=new Highlighter(formatter,scorer);  
	        highlight.setTextFragmenter(fragmenter);  
	        
	        list = new ArrayList<IndexWriterQueue.IndexData>();
			for(ScoreDoc sDoc : td.scoreDocs){
				Document doc = searcher.doc(sDoc.doc);
				String value = doc.get("content");
	            TokenStream tokenStream = analyzer.tokenStream("content", new StringReader(value));    
	            String _frag = highlight.getBestFragment(tokenStream, value);
	            IndexData data = new IndexData();
	            data.url = doc.get("url");
	            data.title = doc.get("title");
	            data.content = _frag;
	            list.add(data);
			}
		} catch (IOException e) {
			e.printStackTrace();
		} catch (InvalidTokenOffsetsException e) {
			e.printStackTrace();
		} catch (ParseException e) {
			e.printStackTrace();
		}  finally{
			if(analyzer != null){
				analyzer.close();
				analyzer=null;
			}
		}
		return list;
	}
	
	
	public List<IndexData> search(String keyWord,int num) {
		List<IndexData> list = null;
		MMSegAnalyzer analyzer = null;
		try {
			IndexSearcher searcher = getSearcher();
			analyzer = new MMSegAnalyzer();
			/*
			KeyWordQueryParse parse = new KeyWordQueryParse("content",analyzer);
			Query sub = parse.parse(keyWord);
			*/
			Term term =new Term("content", keyWord);  
			WildcardQuery sub = new WildcardQuery(term);
			OrdFieldSource source = new OrdFieldSource("score");
			FunctionQuery score = new FunctionQuery(source);	
			Query query = new MainScoreQuery(sub,score);
			TopDocs td = searcher.search(query, num);
			
//	        SimpleHTMLFormatter formatter = new SimpleHTMLFormatter("<span class=\"hightlighterCss\">","</span>");  
			SimpleHTMLFormatter formatter = new SimpleHTMLFormatter("<b style=\"color:red\">","</b>");
	        QueryScorer scorer=new QueryScorer(query);  
	        Fragmenter fragmenter = new SimpleSpanFragmenter(scorer);  
	        Highlighter highlight=new Highlighter(formatter,scorer);  
	        highlight.setTextFragmenter(fragmenter);  
	        
	        list = new ArrayList<IndexWriterQueue.IndexData>();
			for(ScoreDoc sDoc : td.scoreDocs){
				Document doc = searcher.doc(sDoc.doc);
				String value = doc.get("content");
	            TokenStream tokenStream = analyzer.tokenStream("content", new StringReader(value));    
	            String _frag = highlight.getBestFragment(tokenStream, value);
	            
	            IndexData data = new IndexData();
	            data.url = doc.get("url");
	            data.title = doc.get("title");
	            data.content = _frag;
	            list.add(data);
			}
		} catch (IOException e) {
			e.printStackTrace();
		} catch (InvalidTokenOffsetsException e) {
			e.printStackTrace();
		}  finally{
			if(analyzer != null){
				analyzer.close();
				analyzer=null;
			}
		}
		return list;
	}
	
	protected synchronized IndexSearcher getSearcher() {
		try{
			if(reader != null){
				//不处理同步因为,就一点差别无所谓
				if(openIfChanged){
					DirectoryReader nr = DirectoryReader.openIfChanged(reader);
					if(nr != null){
						reader.close();
						reader = nr;
					}
					openIfChanged = false; 
				} 
			} else {
				reader = DirectoryReader.open(directory);
			}
		} catch(IOException e){
			e.printStackTrace();
		}
		IndexSearcher searcher = new IndexSearcher(reader);
		return searcher;
	}
	
/*	
	*//**
	 *  test
	 *//*
	public void index() {
		IndexWriter writer = null;
		try {
			// 创建索引库
			directory = FSDirectory.open(new File(STORE_DIR));
			Analyzer an = new MMSegAnalyzer();
			
			// 创建索引配置文件
			IndexWriterConfig config = new IndexWriterConfig(
					Version.LUCENE_4_10_2, an);
			config.setOpenMode(OpenMode.CREATE);
			
			// 创建索引写入
			writer = new IndexWriter(directory, config);
			File indexedDir = new File(INDEXED_DIR);
			//将STORE_DIR 目录下的文件都建立索引,并且保存到INDEXED_DIR目录下
			
			for (File file : indexedDir.listFiles()) {
				Document doc = new Document();
				doc.add(new Field("content", readFile(file), Field.Store.YES,Index.ANALYZED));
				doc.add(new Field("fileName", file.getName(), Field.Store.YES,Index.NOT_ANALYZED));
				doc.add(new Field("Path", file.getAbsolutePath(),Field.Store.YES, Index.NOT_ANALYZED));
				doc.add(new IntField("score", new Random().nextInt(100),Field.Store.YES));
				writer.addDocument(doc);
			}
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			try {
				if (writer != null)
					writer.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
	*/
	/*
	public String readFile(File file) throws IOException{
		BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(file)));
		StringBuffer sb = new StringBuffer();
		String line = null;
		while((line = br.readLine()) != null){
			sb.append(line);
			sb.append("\r\n");
		}
		return sb.toString();
	}
	*/
	
	/**
	 * not perferf
	 * @param args
	 * @throws InvalidTokenOffsetsException 
	 * @throws IOException 
	 */
	/*
	public static void main(String[] args) throws IOException, InvalidTokenOffsetsException {
//		new RetinReaderFinder().index();
//		String seacher = new String("SSL".getBytes("UTF-8"),"UTF-8");
		List<IndexData> list = new RetinReaderFinder().search0("ssl", LuceneConfig.LUCENE_SHOW_NUM);
		for(IndexData data : list){
			System.out.println(data);
		}
		
//		new RetinReaderFinder().searToHighlighterCss(new MMSegAnalyzer(), new RetinReaderFinder().getSearcher());
	}*/
	public static void main(String[] args) {
		List<IndexData> list = new RetinReaderFinder().search0("ssl", LuceneConfig.LUCENE_SHOW_NUM);
		for(IndexData data : list){
			System.out.println(data);
		}
	}
	
}