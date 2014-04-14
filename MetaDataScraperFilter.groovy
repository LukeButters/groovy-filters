package com.funnelback.services.filter;

import com.funnelback.common.utils.TextUtils;

import java.text.*
import java.util.Calendar;
import java.util.regex.*;
import java.io.*;
import java.text.*

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.jsoup.nodes.Entities.EscapeMode;

import com.funnelback.common.*
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.log4j.Logger;

/*
	Meta Data Scraper

	http://confluence.cbr.au.funnelback.com:8080/display/PNS/Groovy+Filter+-+Meta+Data+Scraper
*/
@groovy.transform.InheritConstructors
public class MetaDataScraperFilter extends com.funnelback.common.filter.ScriptFilterProvider
{
   private static final Logger logger = Logger.getLogger(MetaDataScraperFilter.class);

   public final static String CONFIG_FILENAME = "filter.metadata-scraper.cfg";
   public final static String SEARCH_HOME =  Environment.getValidSearchHome();

   public String collectionName;

   //needs to be detected from indexer options
   public final String META_DATA_DELIMITER_CONFIG_NAME = "filter.meta_data_scraper.delimiter";
   public final String META_DATA_DELIMITER_DEFAULT = "|";
   public String metaDataDelimiter;

   //Class which represents all the meta data scraper config
   MetaDataScraperFactory scraperFactory;

   public MetaDataScraperFilter(String collectionName, boolean inlineFiltering)
   {
      super(collectionName, inlineFiltering);

      //Set the collection name
      this.collectionName = collectionName;

      //Set the filename
      String fileName = SEARCH_HOME + File.separator + "conf" + File.separator + collectionName + File.separator + CONFIG_FILENAME;

      metaDataDelimiter =  readCollectionConfig(META_DATA_DELIMITER_CONFIG_NAME, META_DATA_DELIMITER_DEFAULT );

      //Set up the config file
      BufferedReader reader = null;
      config = null;
      try
      {
         //opens a file in utf-8 mode
         reader = new BufferedReader(new InputStreamReader(new FileInputStream(new File(fileName)),"UTF8"));
         //create the factory which contains all the scraper config
         scraperFactory = new MetaDataScraperFactory(new MetaDataScraperConfig(reader, metaDataDelimiter, logger));
      }
      catch(Exception e)
      {
         println("${e.toString()}");
         logger.info("${e.toString ()}");
      }
      finally
      {
         if(reader != null)
         {
            reader.close();
         }
      }
   }


   // We filter all documents
   public Boolean isDocumentFilterable(String documentType)
   {
      return true;
   }

   /*
      called to filter document
      @input - text which is to be filtered such as html
      @documentType - html, doc, pdf etc
   */
   public String filter(String input, String documentType)
   {
      return filter(input, documentType, getURL(input));
   }

   public String filter(String input, String documentType, String address)
   {
      logger.info("Processing content from URL: '${address}' - With document type of '${documentType}'");

      //do nothing if scraper config is not found and 
      if(scraperFactory == null)
      {
         return input;
      }

      //For performance, ensure that we only process the current document with JSOUP if it is 
      //being referenced in the scraper configs 
      if(scraperFactory.containsURL(address) == false)
      {
         return input;
      }
      else
      {
         Document doc;

         try
         {
            doc = getDocument(input, address);

            if(documentType ==~ /\.html|html|\.pdf|pdf|/)
            {
               scraperFactory.process(doc, address);
            }
         }
         catch(Exception e)
         {
             //log the error
            logger.error(e.toString ());
         }

         //return the filtered html
         if(doc != null)
         {
            return doc.html();
         }
         else
         {
            return input;
         }         
      }
   }

   //Attempts to obtain the value of the @configName found in collection.cfg
   //Defaults to @defaultValue
   public String readCollectionConfig(String configName, String defaultValue )
   {
      String output = "";
      //Setup the URL Config
      try 
      {
         output = config.value(configName);

         if(output == null)
         {
           throw new Exception("No config found for '${configName}'")
         }
      }
      catch(Exception e) 
      {
         output = defaultValue;
         logger.info("Unable to find any value for ${configName}. Using default value of '${defaultValue}'");
      }

      return output;
   }

   //Converts a string representation of a html page to a JSOUP Document object
   //Ensures that the character encoding of the document is maintained
   public Document getDocument(String input, String address)
   {
      Document doc;

      //Converts the String into InputStream
      InputStream is = new ByteArrayInputStream(input.getBytes());
      BufferedInputStream bis = new BufferedInputStream(is);
      bis.mark(Integer.MAX_VALUE);
      //Get the character set
      String c = TextUtils.getCharSet(bis);
      bis.reset();
      //Create the JSOUP document object with the calculated character set
      doc = Jsoup.parse(bis, c, address);      
      doc.outputSettings().escapeMode(EscapeMode.xhtml);

      return doc;
   }

   /*
      Created by Alwyn I think to obtain the URL if it is not available via the standard
      Filter() function.
   */
   public String getURL(String input)
   {
      def matched = "";
      // Check for WARC style URL output
      def matcher = (input =~ /(?im)^\+\s+(http.+)$/);
      if (matcher.find())
      {
         matched = matcher[0][1];
         matched = matched.trim();
      }
      else
      {
         // If not matched, check for MirrorStore style URL output
         matcher = (input =~ /<BASE HREF="(http.+)">/);
         if (matcher.find())
         {
            matched = matcher[0][1];
            matched = matched.trim();
         }
      }

      return matched;
   }

   // A main method to allow very basic testing
   public static void main(String[] args)
   {
      def f = new MetaDataScraperFilter("demo-supercheap-auto-web", true).filter(new File("C:/Users/gioan/Desktop/test.html").getText(), ".html", "http://www.supercheapauto.com.au/online-store/products/Pressure-Washer-1450PSI-RS8135-1450PSI.aspx?pid=347104#Cross");

      /* Write out results UTF-8 mode */
      PrintWriter objPW;
      try
      {                    
         File extMDFile = new File('C:/Users/gioan/Desktop/results.html');
         //open a print writer stream to write the contents of the xml to file in utf8 format
         objPW = new PrintWriter(new OutputStreamWriter(new FileOutputStream(extMDFile),"UTF8"));
         objPW.print(f);

         //commit the changes to disk and close the file
         objPW.flush();
      }
      catch(Exception e)
      {
         println e.toString();
      }
      finally
      {
         //ensure that we always close the file
         if (objPW != null)
         {
            objPW.close();
         }
      }
   }
}

//Represents a class which invokes a meta data scraper config onto
//a document
public class MetaDataScraperFactory
{
   MetaDataScraperConfig config;

   public MetaDataScraperFactory(MetaDataScraperConfig config)
   {
      this.config = config;
   }

   //Returns true if the url matches any in the config
   public boolean containsURL(String address)
   {
      return config.containsURL(address);
   }

   //Scrap the data in the document based on the meta data config rules
   public void process(Document doc, String address)
   {
      config.configEntries.each()
      {
         if(it.matchURL(address))
         {
            processEntry(doc, it);  
         }         
      }
   }

   //Extract the contents from the @doc specified using  the configuration @entry
   //and add back as meta data
   public void processEntry(Document doc, MetaDataScraperConfigEntry entry)
   {
      Elements matches = doc.select(entry.selector);

      for(Element element: matches)
      {
         //Obtain the data which is to be stored in the contents
         String contentData = processElement(element, entry.extractionType, entry.attributeName);
         contentData = extractData(contentData, entry.metaValueType, entry.value);

         //Add the contents to the document if something is found
         if(contentData != "")
         {
            addMetaData(doc, entry.metaName, contentData);
         }

      }
   }

   //Extracts the contents of the element based on the @extraction type.
   //e.g.
   //text between tags - <div>text to be extracted</div>
   //text in an attribute - <a href="text to be extracted"> </a>
   public String processElement(Element element, String extractionType, String attributeName = "")
   {
      String result = "";

      if(element != null)
      {
         switch(extractionType)
         {
            case "text":
               result = element.text();
               break;
            case "attr":
               result = element.attr(attributeName);
               break;
            default:
            break;
         }
      }

      return result;
   }

   //Extracts the data from the string based on @metaValueType
   //e.g.
   //regex: this wont be extracted |but this will| - regex - |(.*)| will extract
   //       "but this will"
   //constant: this wont be extracted - constant - FIXED_CONSTANT will extract
   //       "FIXED_CONSTANT"
   public String extractData(String input, String metaValueType, String value)
   {
      String output = "";
      switch(metaValueType)
      {
         case "regex":
            //Default regex to (.+) if none is provided
            if(value == null || value == "''" || value=="\"\"")
            {
               value = "(.+)";
            }

            Matcher matches = input =~ /${value}/;

            String seperator = "";
            while(matches.find())
            {
               int numberGroups = matches.groupCount();

               //Only extract data if at least one group is specified
               if(numberGroups > 0)
               {
                  String newData = "";

                  //Extract all values within the group
                  for(int i = 1 ; i <= numberGroups; i++)
                  {
                     newData = newData + matches.group(i);
                  }

                  //Only add if data is not empty
                  if(newData != "")
                  {
                     output = output + newData;
                     output = output + seperator;

                     //Add the separator only after the first item
                     seperator = config.metaDataDelimiter;
                  }
               }
            }
            break;
         case "constant":
            output = value;
            break;
      }

      return output;
   }

   //Adds or appends a meta data entry with @name and contents of @value
   public void addMetaData(Document doc, String name, String value)
   {
      //Add meta data to head
      Element head = doc.head();

      //Assumes that meta data names are unique
      Element meta = doc.select("meta[name=${name}]").first();

      if(meta != null)
      {
         //Append the new value to the existing content
         String newContents = meta.attr("content") + config.metaDataDelimiter + value;
         meta.attr("content", newContents);
      }
      else
      {
         //Add a new meta data with @name and content of @value
         head.appendElement("meta").attr("name", "${name}").attr("content", value);
      }
   }
}

//Represents a collection of config entries
public class MetaDataScraperConfig
{
   public List<MetaDataScraperConfigEntry> configEntries;
   public String metaDataDelimiter;

   //Attempts to create config entries for each line found in @reader
   //and logs all errors to @logger
   public MetaDataScraperConfig(BufferedReader reader, String metaDataDelimiter, Logger logger)
   {
      logger.info("Begin reading meta data scraper config file");

      this.metaDataDelimiter = metaDataDelimiter;      

      configEntries = new ArrayList<MetaDataScraperConfigEntry>();

      String line;

      //Process individual lines of the config file
      while ((line = reader.readLine()) != null)
      {
         try
         {
            MetaDataScraperConfigEntry newEntry = new MetaDataScraperConfigEntry(line);
            configEntries.add(newEntry);

            println("Added: ${newEntry.toString ()}");
            logger.info("Added: ${newEntry.toString ()}");

         }
         catch(Exception e)
         {
            println("Error processing '${line}'");
            println("${e.toString ()}");
            logger.info("Error processing '${line}'");
            logger.info("${e.toString ()}");
         }
      }

      println("Completed reading meta data scraper config file");
      logger.info("Completed reading meta data scraper config file");
   }

   //Returns true if the url matches any in the config
   public boolean containsURL(String URL)
   {
      boolean result = false;

      configEntries.each()
      {
         if(it.matchURL(URL))
         {
            result = true;
         }
      }

      return result;
   }   
}

//Represents a line in the meta data scraper config file
public class MetaDataScraperConfigEntry
{
   public final static int EXPECTED_NUM_TOKENS = 7;
   public final static String MINIMAL_HTML = "<html><head></head><body></body></html>";

   public String url;
   public String metaName;
   public String selector;
   public String extractionType;
   public String attributeName;
   public String metaValueType;
   public String value;

   //Constructs the meta data scraper config entry using a comma delimtered String
   public MetaDataScraperConfigEntry(String input) throws Exception
   {
      try
      {
         //Convert the String input
         String [] aryOptions = processString(input);

         if(aryOptions.length == EXPECTED_NUM_TOKENS)
         {
            url = cleanse(aryOptions[0]).toLowerCase();
            metaName = cleanse(aryOptions[1]);
            //Ensures that selector is valid
            setSelector(cleanse(aryOptions[2]));
            extractionType = cleanse(aryOptions[3]).toLowerCase();
            attributeName = cleanse(aryOptions[4]);
            metaValueType = cleanse(aryOptions[5]).toLowerCase();
            value = cleanse(aryOptions[6]).trim();
         }
      }
      catch(Exception e)
      {
         throw e;
      }
   }

   //Ensures that the css selector is valid
   public void setSelector(String input)
   {
      try 
      {
         Document validationDoc = Jsoup.parse(MINIMAL_HTML);
         validationDoc.select(input);
         selector = input;    
      }
      catch(Exception e) 
      {
         throw new Exception("'${input}' is not a valid selector")
      }
   }

   //Converts a comma Delimitered String into a String array. Caters for quoted strings.
   public String [] processString(String input) throws Exception
   {
      if(isComment(input))
      {
         throw new Exception("Ignoring comments - '${input}'")
      }

      String[] tokens = input.split(/,(?=([^\"]*\"[^\"]*\")*[^\"]*$)/);

      if(tokens.length != EXPECTED_NUM_TOKENS)
      {
         println tokens.length.class
         println EXPECTED_NUM_TOKENS.class
         tokens.each()
         {
            println("token: ${it}");
         }

         throw new Exception("Invalid number of arguments for '${input}'")
      }

      return tokens;
   }

   //Determines if @input is a comment
   private boolean isComment(String input)
   {
      if(input.trim().charAt(0) == '#')
      {
         return true;
      }
      else
      {
         return false;
      }
   }

   //Removes leading and trailing spaces and quotes
   public String cleanse(String input)
   {
      String output = input.trim();

      //Remove leading and trailing quote
      Matcher matches = output =~ /"(.+)"/;

      if(matches.find())
      {
         output = matches.group(1);
      }

      return output;
   }

   //Returns true if the @input passed in matches the 
   //url pattern stored
   public boolean matchURL(String input)
   {
      Matcher matches = input =~ /${this.url}/ 
      if(matches.find())
      {
         return true;
      }      
      else
      {
         return false;
      }
   }

   //Produces a String representation of the config entry
   public String toString ()
   {
      return "{ url: '${url}'" +
         ", meta name: '${metaName}'" +
         ", selector: '${selector}'" +
         ", extraction type: '${extractionType}'" +
         ", attribute name: '${attributeName}'" +
         ", meta value type: '${metaValueType}'" +
         ", value: '${value}' }";
   }
}
