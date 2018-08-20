/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package svo.extractor;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import org.maltparser.concurrent.ConcurrentUtils;

/**
 *
 * @author wbolduc
 */
public class SVOExtractor {

    static HashSet VERBS = new HashSet(Arrays.asList("VB","VBD","VBG","VBN","VBP","VBZ"));
    static HashSet SUBJECTS = new HashSet(Arrays.asList("nsubj", "nsubjpass", "csubj", "csubjpass", "agent", "expl"));
    static HashSet OBJECTS = new HashSet(Arrays.asList("dobj", "pobj", "dative", "attr", "oprd"));
    static HashSet NOUNS = new HashSet(Arrays.asList("NN", "NNS", "NNP", "NNPS", "PRP", "WP"));
    
    static int progressInterval = 100;
    
    public static void main(String[] args) {
        if(args.length < 1)
        {
            System.out.println("Need a dependency parsed conll");
            return;
        }
        else if (args.length > 1)
        {
            System.out.println("Too many arguments");
        }
        
        File depParsed = new File(args[0]);
        
        if (!depParsed.isFile() && !depParsed.getName().endsWith(".conll"))
        {
            System.out.println(args[0] + " is not a valid file type");
            return;
        }
        
        //String depParsedConllIn = "E:\\YorkWork\\conllStages\\DepRelTagged\\Twitter-MeToo-en-2017-10-17-to-31-DepParsed.conll";
        System.out.println("Reading");
        List sentences = readSentences(depParsed);
        
        
        HashMap<SVO, Integer> counts = new HashMap<>();
        ArrayList<SVO> svos = new ArrayList<>();
        
        System.out.println("Extracting");
        for (int i = 0; i < sentences.size(); i++)
        {            
            svos.addAll(extractSVO((Word[])sentences.get(i)));
            if (i % progressInterval == 0)
                System.out.println(i);
        }
        
        System.out.println("Getting frequencies"); 
        svos.forEach(svo -> {
            Integer count = counts.get(svo);
            if(count == null)
                counts.put(svo, 1);
            else
                counts.put(svo, count+1);
        });
        
        System.out.println("Sorting");
        ArrayList<Tuple> freqs = new ArrayList<>();
        counts.entrySet().forEach((pair) -> freqs.add(new Tuple(pair.getKey(), pair.getValue())));
        Collections.sort(freqs,new Comparator<Tuple>(){
            @Override
            public int compare(final Tuple lhs,Tuple rhs) {
              if((Integer)lhs.y < (Integer)rhs.y)
                  return 1;
              else if ((Integer)lhs.y > (Integer)rhs.y)
                  return -1;
              return 0;
              }
          });
        
        freqs.forEach(freq -> System.out.println(Integer.toString((Integer)freq.y)+ " " + ((SVO)freq.x).toString()));
    }
    
    
    public static Word getRootConj(Word conjWord, Word[] sentence)
    {
        while ("conj".equals(conjWord.depRel))  //while still a conj word   //could optimize if conj turns out to be a tree and not a chain
        {
            conjWord = sentence[conjWord.head-1];
        }
        return conjWord;
    }
    
    public static ArrayList<SVO> extractSVO(Word[] sentence)
    {
        ArrayList<SVO> svos = new ArrayList<>();
        
        //create connection lists
        ArrayList<Word>[] connections = (ArrayList<Word>[])new ArrayList[sentence.length];
        for (int i = 0; i < sentence.length; i++)
            connections[i] = new ArrayList<>();
        
        //create tree
        for(Word word : sentence)
        {
            if (word.head > 0)
                connections[word.head-1].add(word);
        }
        
        //iterate through verbs
        for(Word word : sentence)
        {
            //if word is verb and not aux or conj
            if (VERBS.contains(word.pos) && !"aux".equals(word.depRel) && !"conj".equals(word.depRel))
            {
                //get the verb itself and all it's 'conjes'
                ArrayList<Word> verbs = getConjes(word, connections);
                
                for(Word verb : verbs)  //for each verb
                {
                    ArrayList<Word> subjs = new ArrayList<>();
                    ArrayList<Word> objs = new ArrayList<>();
                    for(Word connection : connections[verb.index-1])    //check verbs connections for subj
                    {
                        //for all subjects
                        if(SUBJECTS.contains(connection.depRel))    //subj found and not a conj //might reduce subjects
                        {
                            subjs.addAll(getConjes(connection, connections));
                        }
                        //find objects
                        else if("dobj".equals(connection.depRel))
                        {
                            objs.addAll(getConjes(connection, connections));
                        }
                        else if ("prep".equals(connection.depRel)) //looking for pobj
                        {
                            for(Word prepConnect : connections[connection.index-1])
                            {
                                if("pobj".equals(prepConnect.depRel))
                                {
                                    objs.addAll(getConjes(prepConnect, connections));   //apparently there can be multiple pobjs per prep
                                }
                                
                            }
                        }
                    }
                    
                    boolean negated = isNegated(verb, connections);
                    //todo: should I negate nouns
                    subjs.forEach((sub) -> {
                        objs.forEach((obj) -> {
                            svos.add(new SVO(sub.form, verb.form, obj.form, negated));
                        });
                    });
                }
            }
        }
        
        
        return svos;
    }
    
    public static boolean isNegated(Word verb, ArrayList<Word>[] connections)
    {
        boolean negated = false;
        
        for (Word connection : connections[verb.index-1])
        {
            if("neg".equals(connection.depRel))
                if (negated)
                    negated = false;
                else
                    negated = true;
        }
        return negated;
    }
    
    public static ArrayList<Word> getConjes(Word word, ArrayList<Word>[] connections)
    {//returns the word passed and it's conj connected words
        ArrayList<Word> conjes = new ArrayList<Word>();
        conjes.add(word);
        
        for (Word connection : connections[word.index-1])
        {
            if ("conj".equals(connection.depRel))
            {
                conjes.addAll(getConjes(connection, connections));
            }
        }
        return conjes;
    }
    
    
    public static Word[] conllSentenceSplitter(String[] sentence)
    {
        //FORM POS HEAD DEPREL
        Word[] splitSent = new Word[sentence.length];
        
        for(int i = 0; i < sentence.length; i++)
        {
            String conllWord = sentence[i];
            String[] cols = conllWord.split("\t");
            
            //is the word lemmatized?
            if ("_".equals(cols[2]))    //no
                splitSent[i] = new Word(cols[0],cols[1],cols[4],cols[6], cols[7]);
            else                        //yes
                splitSent[i] = new Word(cols[0],cols[2],cols[4],cols[6], cols[7]);
        }
        return splitSent;
    }
    
    public static List<Word[]> readSentences(File inputConll)
    {
        long startTime = System.nanoTime();
        List<Word[]> inSentences = new ArrayList<Word[]>();
        BufferedReader reader = null;
        try {
            reader = new BufferedReader(new InputStreamReader(new FileInputStream(inputConll), "UTF-8"));
            while (true) {
                // Reads a sentence from the input file
                String[] conllSentence = ConcurrentUtils.readSentence(reader);
                if (conllSentence.length == 0) {
                    break;
                }
                inSentences.add(conllSentenceSplitter(conllSentence));
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        System.out.println("Read " + Integer.toString(inSentences.size()) + " sentences time: " + Long.toString((System.nanoTime()-startTime)/1000000000));

        return inSentences;
    }
    
    public static void printSentence(Word[] sentence)
    {
        for(Word word : sentence)
            System.out.print(word.form+" ");
        System.out.println();
    }
}
