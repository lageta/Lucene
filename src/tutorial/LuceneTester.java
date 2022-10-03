package tutorial;

import java.util.*;
import java.nio.file.Paths;
import java.io.IOException;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.search.spell.*;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.index.IndexWriterConfig;

public class LuceneTester {
   String indexDir = "Index";
   String dataDir = "Data";
   Indexer indexer;
   Searcher searcher;

   public static void main(String[] args) {
      LuceneTester tester;
      try {
         tester = new LuceneTester();
         tester.createIndex();
         Scanner sc = new Scanner(System.in);
         System.out.print("Enter your search: ");
         String str = sc.nextLine();
         tester.search(str);

      } catch (IOException e) {
         e.printStackTrace();
      } catch (ParseException e) {
         e.printStackTrace();
      }
   }

   private void createIndex() throws IOException {
      indexer = new Indexer(indexDir);
      int numIndexed;
      long startTime = System.currentTimeMillis();
      numIndexed = indexer.createIndex(dataDir, new TextFileFilter());
      long endTime = System.currentTimeMillis();
      indexer.close();
      System.out.println(numIndexed + " File indexed, time taken: "
            + (endTime - startTime) + " ms");
   }

   private void search(String searchQuery) throws IOException, ParseException {
      searcher = new Searcher(indexDir);
      long startTime = System.currentTimeMillis();
      TopDocs hits = searcher.search(searchQuery);
      long endTime = System.currentTimeMillis();

      System.out.println(hits.totalHits +
            " documents found. Time :" + (endTime - startTime));
      if (hits.totalHits == 0) {
         SpellChecker spellchecker = new SpellChecker(Indexer.indexDirectory);
         StandardAnalyzer analyzer = new StandardAnalyzer();
         IndexWriterConfig iwc = new IndexWriterConfig(analyzer);
         spellchecker.indexDictionary(new PlainTextDictionary(Paths.get("./Data/movies.dat")), iwc, false);
         spellchecker.setStringDistance(new JaroWinklerDistance());

         /*
          * Map<String, String[]> suggestions = new HashMap<>();
          * String[] words = searchQuery.split(" ");
          * for (String word : words) {
          * suggestions.put(word, spellchecker.suggestSimilar(word, 5));
          * }
          * Map<String, Integer> score = new HashMap<>();
          * 
          * for (Map.Entry<String, String[]> entry : suggestions.entrySet()) {
          * String[] array = entry.getValue();
          * for (int i = 0; i < array.length; i++) {
          * String movie = array[i];
          * if (score.containsKey(movie))
          * score.put(movie, score.get(movie) + (100 - i) * 100);
          * else
          * score.put(movie, (100 - i) * 100);
          * }
          * }
          * 
          * Set<String> realSuggestions = score.keySet();
          */

         String[] realSuggestions = spellchecker.suggestSimilar(searchQuery, 5);
         System.out.println("By '" + searchQuery + "' did you mean:");
         for (String suggestion : realSuggestions) {
            System.out.println("\t" + suggestion);
         }

      } else {
         for (ScoreDoc scoreDoc : hits.scoreDocs) {
            Document doc = searcher.getDocument(scoreDoc);
            System.out.println("File: "
                  + doc.get(LuceneConstants.FILE_PATH));
         }
      }
   }
}