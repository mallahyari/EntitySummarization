/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package cs.uga.edu.dicgenerator;

/**
 *
 * @author spiaotools
 */
//package spiaotools;

import java.io.*;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SentParDetector
{

    private String filterTerms[] = {
        "A", "An", "And", "Because", "But", "He", "How", "However", "It", "Nonetheless", 
        "She", "So", "That", "The", "These", "Therefore", "They", "This", "Those", "What", 
        "Where", "Which", "Why"
    };
    private ArrayList<String> filterTermList;
    Pattern initialPattern;
    private int TITLE_MAX_LEN;
    private String title_start;
    private String title_end;
    private boolean TAG_TITLE;
    private String CUSTOM_ABBREV_LIST;

    public SentParDetector() throws FileNotFoundException, UnsupportedEncodingException, IOException
    {
        initialPattern = Pattern.compile("[A-Z][.]");
        TITLE_MAX_LEN = 40;
        title_start = "<head>";
        title_end = "</head>\n\n";
        TAG_TITLE = false;
        filterTermList = new ArrayList<String>(filterTerms.length);
        for(int i = 0; i < filterTerms.length; i++)
        {
            filterTermList.add(filterTerms[i]);
        }

        //CUSTOM_ABBREV_LIST = getCustomAcronymList();
    }

    public void markTitle(boolean b)
    {
        TAG_TITLE = b;
    }

    public String markupRawText(int flag, String input)
    {
        if(input.equals(""))
        {
            System.err.println("Warning: No Input Text");
            return null;
        }
        String title = new String();
        StringBuffer txbody = new StringBuffer();
        if(TAG_TITLE)
        {
            TITLE_MAX_LEN = 70;
            TITLE_MAX_LEN = 70;
            int index1 = input.indexOf("\n");
            title = input.substring(0, index1).trim();
            if(title.equals(""))
            {
                for(; title.equals(""); title = input.substring(0, index1).trim())
                {
                    index1 = input.indexOf("\n", index1 + 1);
                }

            }
            if(title.length() <= TITLE_MAX_LEN)
            {
                input = input.substring(index1).trim();
            } else
            {
                title = "";
            }
        }
        input = input.replace("$", "\\$");
        input = (new StringBuilder()).append(input.replace('\r', ' ')).append(" </p>").toString();
        input = (new StringBuilder()).append(input).append("\n\n").toString();
        String str = input.replace('`', '\'');
        str = replacePattern(str, "(\\'\\')|[\u201D]", "\"");
        String regToMatch = "([.?!:\\)\\\"\\'\\]\\\u201C\\\u201D])(\\s+)?(\\n(\\s+)?\\n)(\\n+)?";
        String regForSub = "$1 </p>$4 <p> ";
        str = (new StringBuilder()).append("<p> ").append(replacePattern(str, regToMatch, regForSub)).toString();
        regToMatch = "([.?!]\\\"?\\'?\\)?\\]?)(\\s+)?(\\n?)\\s+([A-Z]|[0-9]|\\\"|\\'|\\()";
        regForSub = "$1 </s> ^ $4";
        str = replacePattern(str, regToMatch, regForSub);
        str = replacePattern(str, "<p>\\s+", "<p> ^ ");
        str = replacePattern(str, "</p>", "</s> </p>");
        str = replacePattern(str, "\\^\\s</s>\\s</p>", "</p>");
        str = replacePattern(str, "(\\^)\\s(\\d|\\d\\d)([.])\\s</s>\\s\\^", "$1 $2$3");
        str = replacePattern(str, "(<p>\\s+\\^)\\s+(\\d+|[A-Z])([.])\\s</s>\\s</p>\\s+<p>\\s+\\^", "$1 $2$3");
        str = replacePattern(str, "(</s>\\s+\\^)\\s+(\\d+|[A-Z])([.])\\s</s>\\s</p>\\s+<p>\\s+\\^", "$1 $2$3");
        str = replacePattern(str, "\\s([\"(]?)(Mr|Mrs|Dr|Prof|Ms|Sir|Sr|St|Mt|Gov|Sgt|Sen|Capt|Lt|Gen)([.])\\s</s>\\s\\^", " $1$2$3");
        str = replacePattern(str, "\\s(Jan|Feb|Mar|Apr|Jun|Jul|Aug|Sep|Sept|Oct|Nov|Dec)([.])\\s+</s>\\s+\\^\\s+([a" +
"-z])"
, " $1$2 $3");
        str = replacePattern(str, "\\s(Ltd|Mon|Tue|Wed|Thu|Thur|Fri|Sat|Sun)([.])\\s+</s>\\s+\\^\\s+([a-z])", " $1$2 $3");
        str = replacePattern(str, "\\s(max|min|Max|Min)([.])\\s+</s>\\s+\\^\\s+(\\d|[%$\243])", " $1$2 $3");
        str = replacePattern(str, "\\s([(]?)(kg|ft|oz|gm)([.])\\s</s>\\s\\^", " $1$2$3");
        str = replacePattern(str, "\\s([(]?)(Fig|Ref|ref)([.])\\s</s>\\s\\^", " $1$2$3");
        str = replacePattern(str, "\\s([(]?)(Co|et\\sal|pp|vs|eg|e[.]g|i[.]e|usu|ad|ed|eds|yr|yrs|lb|Cap|Col|Gen|Li" +
"eut|Esg)([.])\\s</s>\\s\\^"
, " $1$2$3");
        str = replacePattern(str, "\\s(\\d+)(l|lb)([.])\\s</s>\\s\\^", " $1$2$3");
        if(CUSTOM_ABBREV_LIST != null)
        {
            str = replacePattern(str, CUSTOM_ABBREV_LIST, " $1$2");
        }
        str = replacePattern(str, "(\\])(\\s+)([A-Z])", "$1 </s> \\^ $3");
        str = replacePattern(str, "([.?!]\\s+</s>\\s+\\^\\s+)([1-9]|[A-Z])([.]\\s+)</s>\\s+\\^", "$1 $2$3");
        str = replacePattern(str, "(\\s+[A-Z][.])\\s+</s>\\s+\\^\\s+([a-z])", "$1 $2");
        str = clearInBracket(str);
        str = replacePattern(str, "([A-Z]\\S+\\s+[A-Z][.]\\s+)</s>\\s+\\^", "$1 ");
        str = checkAcronym(str);
        str = replacePattern(str, "\\s<p>\\s</p>", "");
        str = str.replace("\\$", "$");
        StringTokenizer st = new StringTokenizer(str, "^");
        int count = 0;
        txbody.append(st.nextToken());
        for(; st.hasMoreTokens(); txbody.append((new StringBuilder()).append("<s n=\"").append(++count).append("\">").append(st.nextToken()).toString())) { }
        if(title.equals(""))
        {
            if(flag == 1)
            {
                return reformat1(txbody.toString());
            }
            if(flag == 2)
            {
                return reformat2(txbody.toString());
            }
            if(flag == 3)
            {
                return reformat3(txbody.toString());
            }
            if(flag == 4)
            {
                return reformat4(txbody.toString());
            }
            if(flag == 5)
            {
                return reformat5(txbody.toString(), input);
            } else
            {
                return reformatInternal(txbody.toString());
            }
        }
        if(flag == 1)
        {
            return (new StringBuilder()).append(title_start).append(title).append(title_end).append(reformat1(txbody.toString())).toString();
        }
        if(flag == 2)
        {
            return (new StringBuilder()).append(title).append("\n\n").append(reformat2(txbody.toString())).toString();
        }
        if(flag == 3)
        {
            return (new StringBuilder()).append("<p>\n<s>\n").append(title).append("\n</s>\n</p>").append(reformat3(txbody.toString())).toString();
        }
        if(flag == 4)
        {
            return (new StringBuilder()).append(title).append("^\n").append(reformat4(txbody.toString())).toString();
        }
        if(flag == 5)
        {
            return (new StringBuilder()).append(title).append("^\n").append(reformat5(txbody.toString(), input)).toString();
        } else
        {
            return (new StringBuilder()).append(title).append("^\n").append(reformatInternal(txbody.toString())).toString();
        }
    }

    private String checkAcronym(String text)
    {
        String acroRegexp = "([A-Z][.]){2,}\\s+</s>\\s+\\^\\s+([A-Z]\\w+)[,]?";
        Pattern acroPatrn = Pattern.compile(acroRegexp);
        Matcher acroMatch = acroPatrn.matcher(text);
        StringBuffer acrosb = new StringBuffer();
        do
        {
            if(!acroMatch.find())
            {
                break;
            }
            String checkedString = acroMatch.group(0);
            String CapIniWord = acroMatch.group(2);
            if(!filterTermList.contains(CapIniWord))
            {
                checkedString = checkedString.replaceFirst("(([A-Z][.]){2,})([,]?)\\s+</s>\\s+\\^\\s+([A-Z]\\w+)([,]?)", "$1 $3$4");
                acroMatch.appendReplacement(acrosb, checkedString);
            }
        } while(true);
        acroMatch.appendTail(acrosb);
        return acrosb.toString();
    }

    private String reformat1(String s)
    {
        StringTokenizer st = new StringTokenizer(s, " \n\t\r\f");
        StringBuffer sb = new StringBuffer();
        int space_flag = 0;
        while(st.hasMoreTokens()) 
        {
            String tk = st.nextToken();
            if(tk.equals("</s>") || tk.equals("<p>") || tk.equals("</p>"))
            {
                sb.append((new StringBuilder()).append(tk).append("\n").toString());
            } else
            if(tk.equals("<s"))
            {
                space_flag = 1;
                sb.append((new StringBuilder()).append(tk).append(" ").append(st.nextToken()).toString());
            } else
            if(space_flag == 0)
            {
                sb.append((new StringBuilder()).append(" ").append(tk).toString());
            } else
            {
                sb.append(tk);
                space_flag = 0;
            }
        }
        return sb.toString();
    }

    private String reformat2(String s)
    {
        StringTokenizer st = new StringTokenizer(s, " \n\t\r\f");
        StringBuffer sb = new StringBuffer();
        int space_flag = 0;
        do
        {
            if(!st.hasMoreTokens())
            {
                break;
            }
            String tk = st.nextToken();
            if(tk.equals("</s>") || tk.equals("</p>"))
            {
                sb.append("\n");
            } else
            if(tk.equals("<s"))
            {
                st.nextToken();
                space_flag = 1;
            } else
            if(!tk.equals("<p>"))
            {
                if(space_flag == 0)
                {
                    sb.append((new StringBuilder()).append(" ").append(tk).toString());
                } else
                {
                    sb.append(tk);
                    space_flag = 0;
                }
            }
        } while(true);
        return sb.toString();
    }

    private String reformat3(String s)
    {
        StringTokenizer st = new StringTokenizer(s, " \n\t\r\f");
        StringBuffer sb = new StringBuffer();
        int space_flag = 0;
        while(st.hasMoreTokens()) 
        {
            String tk = st.nextToken();
            if(tk.equals("</s>") || tk.equals("<p>") || tk.equals("</p>"))
            {
                sb.append((new StringBuilder()).append("\n").append(tk).append("\n").toString());
            } else
            if(tk.equals("<s"))
            {
                space_flag = 1;
                sb.append((new StringBuilder()).append(tk).append(" ").append(st.nextToken()).append("\n").toString());
            } else
            {
                sb.append((new StringBuilder()).append(tk).append(" ").toString());
            }
        }
        return sb.toString();
    }

    private String reformatInternal(String s)
    {
        StringTokenizer st = new StringTokenizer(s, " \n\t\r\f");
        StringBuffer sb = new StringBuffer();
        int space_flag = 0;
        do
        {
            if(!st.hasMoreTokens())
            {
                break;
            }
            String tk = st.nextToken();
            if(!tk.equals("</s>") && !tk.equals("<p>") && !tk.equals("</p>"))
            {
                if(tk.equals("<s"))
                {
                    space_flag = 1;
                    st.nextToken();
                    sb.append("^\n");
                } else
                if(space_flag == 0)
                {
                    sb.append((new StringBuilder()).append(" ").append(tk).toString());
                } else
                {
                    sb.append(tk);
                    space_flag = 0;
                }
            }
        } while(true);
        return sb.toString();
    }

    private String reformat4(String s)
    {
        StringTokenizer st = new StringTokenizer(s, " \n\t\r\f");
        StringBuffer sb = new StringBuffer();
        int space_flag = 0;
        do
        {
            if(!st.hasMoreTokens())
            {
                break;
            }
            String tk = st.nextToken();
            if(!tk.equals("<p>") && !tk.equals("</p>"))
            {
                if(tk.equals("</s>"))
                {
                    sb.append((new StringBuilder()).append(tk).append("\n").toString());
                } else
                if(tk.equals("<s"))
                {
                    space_flag = 1;
                    sb.append((new StringBuilder()).append(tk).append(" ").append(st.nextToken()).toString());
                } else
                if(space_flag == 0)
                {
                    sb.append((new StringBuilder()).append(" ").append(tk).toString());
                } else
                {
                    sb.append(tk);
                    space_flag = 0;
                }
            }
        } while(true);
        return sb.toString();
    }

    private String reformat5(String s, String origText)
    {
        StringTokenizer st = new StringTokenizer(s, " \n\t\r\f");
        StringBuffer sb = new StringBuffer();
        int space_flag = 0;
        ArrayList<int[]> spOffsets = new ArrayList<int[]>();
        int pCount = 0;
        int sCount = 0;
        int sStart = 0;
        int sEnd = 0;
        boolean recordSentStart = true;
        int startSerchPos = 0;
        int tkBegin = 0;
        int tkEnd = 0;
        do
        {
            if(!st.hasMoreTokens())
            {
                break;
            }
            String tk = st.nextToken();
            if(tk.equals("<p>"))
            {
                pCount++;
            } else
            if(tk.equals("<s"))
            {
                sCount++;
                recordSentStart = true;
            } else
            if(tk.equals("</s>"))
            {
                sEnd = tkEnd;
                int offsetRow[] = {
                    pCount, sCount, sStart, sEnd
                };
                spOffsets.add(offsetRow);
            } else
            if(!tk.equals("</p>") && !tk.matches("n=\"\\d+\">"))
            {
                tkBegin = origText.indexOf(tk, startSerchPos);
                tkEnd = tkBegin + tk.length();
                if(recordSentStart)
                {
                    sStart = tkBegin;
                    recordSentStart = false;
                }
                startSerchPos += tk.length();
            }
        } while(true);
        int passOffsets[];
        for(Iterator<int[]> itr = spOffsets.iterator(); itr.hasNext(); sb.append((new StringBuilder()).append(passOffsets[0]).append("\t").append(passOffsets[1]).append("\t").append(passOffsets[2]).append("\t").append(passOffsets[3]).append("\n").toString()))
        {
            passOffsets = (int[])(int[])itr.next();
        }

        return sb.toString();
    }

    private String replacePattern(String text, String ptrnStr, String replaceStr)
    {
        Pattern ptrn = Pattern.compile(ptrnStr);
        Matcher mtch = ptrn.matcher(text);
        return mtch.replaceAll(replaceStr);
    }

    private String clearInBracket(String txt)
    {
        StringBuffer sb = new StringBuffer();
        Pattern brackets = Pattern.compile("\\(.*?\\)", 32);
        Matcher matcher = brackets.matcher(txt);
        do
        {
            if(!matcher.find())
            {
                break;
            }
            String matchString = matcher.group();
            Matcher iniMatcher = initialPattern.matcher(matchString);
            if(iniMatcher.find())
            {
                StringTokenizer stk = new StringTokenizer(matchString, " \t\n\b\r");
                if(stk.countTokens() < 35)
                {
                    matchString = matchString.replaceAll("\\s+</s>\\s+\\^\\s+", " ");
                    matcher.appendReplacement(sb, matchString);
                }
            }
        } while(true);
        matcher.appendTail(sb);
        return sb.toString();
    }

    public String getStrFromInstr(InputStream is) throws UnsupportedEncodingException, IOException
    {
        StringBuffer sb;
        BufferedReader br = new BufferedReader(new InputStreamReader(is, "UTF-8"));
        sb = new StringBuffer();
        String line;
        while((line = br.readLine()) != null) 
        {
            sb.append((new StringBuilder()).append(line).append("\n").toString());
        }
        br.close();
        return sb.toString();
        //Exception e;
        //e;
        //e.printStackTrace();
        //return null;
    }

    private String getCustomAcronymList() throws FileNotFoundException, UnsupportedEncodingException, IOException
    {
        String abbrevListFilePath = "abbreviation_list.dat";
        File inf;
        inf = new File(abbrevListFilePath);
        if(inf != null)
        {
            //break;// MISSING_BLOCK_LABEL_26;
            System.err.println("No customosed abbreviation list found.");
            return null;
        }
        
        StringBuffer sb;
        InputStreamReader is = new InputStreamReader(new FileInputStream(inf), "UTF8");
        BufferedReader br = new BufferedReader(is);
        sb = new StringBuffer();
        sb.append("\\s([(]?)(");
        do
        {
            String line;
            if((line = br.readLine()) == null)
            {
                break;
            }
            if(!line.trim().equals("") && !line.startsWith("/*") && !line.startsWith("//"))
            {
                sb.append((new StringBuilder()).append("|").append(line).toString());
            }
        } while(true);
        br.close();
        sb.append(")\\s</s>\\s\\^");
        return sb.toString();
        //IOException e;
        //e;
        //return null;
    }

    public static void main(String args[]) throws UnsupportedEncodingException, IOException
    {
        SentParDetector spb = new SentParDetector();
//        InputStream instr = System.in;
        File file = new File("/Users/mehdi/document.txt");
        InputStream instr = new FileInputStream(file) ;
//        if(instr == null)
//        {
//            System.out.println("Command-line usage: cat input_text | Java -jar sptoollit.jar");
//            System.exit(1);
//        }
        String out = spb.getStrFromInstr(instr);
        if(out.equals(""))
        {
            System.out.println("Command-line usage: cat input_text | Java -jar sptoollit.jar");
            System.exit(1);
        }
        out = spb.markupRawText(1, out);
//        System.out.println(out);
        String s [] = out.split("\n");
        for (String ss : s)
        	System.out.println("sen:"+ss);
        out = null;
        System.exit(0);
    }
}
