import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;

import org.apache.commons.io.FilenameUtils;
/**
 * Created by _ame_ on 16.12.2018 20:48.
 */
public class EpdRead{
    private static final String EPD_EXT = ".epd";
    private static final String EPD_KEY_FN = "epdkeys.txt";
    private static final int EPD_KEY_N = 30;
    private static final String EPD_VAL_ABSENT = "----";
    private static final String EPD_VAL_EXTRA = "???";
public static void main( String[] args ){
    //String[] epdKey = new String[EPD_KEY_N];
    ArrayList<String> epdKey = new ArrayList<>(EPD_KEY_N);
    ArrayList<Character> epdKeyT = new ArrayList<>(EPD_KEY_N);
    int[] epdKeyDataPos;
    String[] epdKeyData;
    String epdKeyMagicFrom = "";
    String epdKeyMagicIns = "";
    String fileContent;
    StringBuilder fileData;
    String filename = null;
    BufferedWriter writer;
    int i,j,k, startPos, epdKeyN;
    char tchr;
    String tstr;
  try{
// Read and parse Keywords file
    filename = EpdRead.class.getProtectionDomain().getCodeSource().getLocation().getPath();
    filename = FilenameUtils.getFullPath(filename) + EPD_KEY_FN;
    Scanner scanner = new Scanner(new File(filename));
    while( scanner.hasNextLine() ){
      tstr = scanner.nextLine();
      if( tstr.isEmpty() ){
        continue;
      }
      tchr = tstr.charAt(0);
      switch( tchr ){
        case '#':
          break;
        case '$':
          epdKeyMagicFrom = tstr.substring(1).trim();
          break;
        case '!':
          epdKeyMagicIns = tstr.substring(1).trim();
          break;
        case '-':
        case '0':
        case '1':
        case '=':
        case '+':
          epdKey.add(tstr.substring(1).trim());
          epdKeyT.add(tchr);
          break;
        default:
          tstr = tstr.trim();
          if( ! tstr.isEmpty() ){
            epdKey.add(tstr);
            epdKeyT.add(' ');
          }
      }
    }
    epdKeyN = epdKey.size();
    epdKeyDataPos = new int[epdKeyN];
    epdKeyData    = new String[epdKeyN];
// Read all input file ("\\A")
    filename = args[0];
    scanner = new Scanner(new File(filename));
    scanner.useDelimiter("\\A");
    fileContent = scanner.next();
// Make output file name and open it
    filename = FilenameUtils.removeExtension(filename) + EPD_EXT;
    writer = new BufferedWriter(new FileWriter(filename));
  }catch( Exception e ){
    System.out.println("Cannot open/read input/write file `" + filename + "`");
    System.out.println(e.toString());
    return;
  }
// Remove line breaks since they break keywords
// Copy to new buffer with spare space for possible '|' at beg and spare closing '|' at the end
  fileData = new StringBuilder(" " + fileContent.replaceAll("\r\n", "") + "|");
// Start with the last epdKeyMagicFrom
  startPos = fileData.lastIndexOf(epdKeyMagicFrom);
  for( i = 0; i < epdKeyN; i++ ){
    int pos = fileData.indexOf(epdKey.get(i), startPos);//
    if( pos > 0 ){
// Mark split points with "|" inserted before keywords
      fileData.setCharAt(pos -1, '|');
// Store epdKey Data Position
      epdKeyDataPos[i] = pos + epdKey.get(i).length();
    }else{
      epdKeyDataPos[i] = 0;
    }
  }
// Do split by "|"
  String[] records = fileData.substring(startPos).split("\\|");
  try{
// Write all records in original order
    for( i = 0; i < records.length; i++ ){
      writer.write(i + "." + records[i] + "\r\n");
    }
    int maxKeyL = Collections.max(epdKey, Comparator.comparing(String::length)).length();
    writer.write("\r\n" );
    // Arrays.sort(records, Comparator.comparingInt(epdKey::indexOf));
// Write all records in config order
    for( i = 0; i < epdKeyN; i++ ){
      int pos = epdKeyDataPos[i];
      if( pos > 0 ){
        epdKeyData[i] = fileData.substring(pos, fileData.indexOf("|", pos));
        // writer.write("\r\n" + i + ". " + epdKey.get(i) + "|" + epdKeyData[i]);
        writer.write(String.format("%2d. %-"+maxKeyL+"s|%s%n", i, epdKey.get(i), epdKeyData[i]));
      }
    }
// Write all records & fields according to config settings
    String[] fields;
    for( i = 0; i < epdKeyN; i++ ){
      tchr = epdKeyT.get(i);
      if( tchr == '-' ) continue; // Skip as cfg
      int pos = epdKeyDataPos[i];
      if( pos <= 0 ) continue;              // Skip if no data (???)
      epdKeyData[i] = fileData.substring(pos-1, fileData.indexOf("|", pos));
      writer.write(String.format("%n%2d. %-"+maxKeyL+"s|", i, epdKey.get(i)));
      if( tchr == '0' ) continue; // Skip as cfg
      if( tchr == '1' ){
        writer.write(epdKeyData[i].substring(2)+"|");
        continue;
      }
      fields = epdKeyData[i].split(" ");
      int fl = fields.length;
      switch( tchr ){
        case '=':
          for( j = 1; j < fl; j++ ){
            writer.write(fields[j] + "|");
          }
          break;
        case '+':
          switch( fl ){
            case 1:
              tstr = " |" + " |" + EPD_VAL_ABSENT + "|" + " |" + " |" + EPD_VAL_ABSENT + "|";
              break;
            case 2:
              tstr = " |" + " |" + EPD_VAL_ABSENT + "|" + " |" + " |" + fields[1] + "|";
              break;
            case 3:
              tstr = " |" + " |" + fields[1] + "|" + " |" + " |" + fields[2] + "|";
              break;
            case 4:
              tstr = " |" + " |" + fields[1] + "|" + " |" + fields[2] + "|" + fields[3] + "|";
              break;
            case 5:
              tstr = " |" + " |" + fields[1] + "|" + fields[2] + "|" + fields[3] + "|" + fields[4] + "|";
              break;
            case 6:
            default:
              tstr = EPD_VAL_EXTRA + "|" + fl + "|" + fields[fl-4] + "|" + fields[fl-3] + "|" + fields[fl-2] + "|" + fields[fl-1] + "|";
              break;
          }
          writer.write(" |" + tstr);
          break;
        case ' ':
        default:
          switch( fl ){
            case 1:
              tstr = EPD_VAL_ABSENT + "|" + EPD_VAL_ABSENT + "|" + EPD_VAL_ABSENT + "|" + EPD_VAL_ABSENT + "|" + " |" + " |" + EPD_VAL_ABSENT + "|";
              break;
            case 2:
              tstr = EPD_VAL_ABSENT + "|" + EPD_VAL_ABSENT + "|" + EPD_VAL_ABSENT + "|" + EPD_VAL_ABSENT + "|" + " |" + " |" + fields[1] + "|";
              break;
            case 3:
              tstr = EPD_VAL_ABSENT + "|" + EPD_VAL_ABSENT + "|" + EPD_VAL_ABSENT + "|" + fields[1] + "|" + " |" + " |" + fields[2] + "|";
              break;
            case 4:
              tstr = EPD_VAL_ABSENT + "|" + EPD_VAL_ABSENT + "|" + EPD_VAL_ABSENT + "|" + fields[1] + "|" + " |" + fields[2] + "|" + fields[3] + "|";
              break;
            case 5:
              tstr = fields[1] + "|" + fields[2] + "|" + EPD_VAL_ABSENT + "|" + fields[3] + "|" + " |" + " |" + fields[4] + "|";
              break;
            case 6:
              tstr = fields[1] + "|" + fields[2] + "|" + fields[3] + "|" + fields[4] + "|" + " |" + " |" + fields[5] + "|";
              break;
            case 7:
              tstr = fields[1] + "|" + fields[2] + "|" + fields[3] + "|" + fields[4] + "|" + " |" + fields[5] + "|" + fields[6] + "|";
              break;
            case 8:
              tstr = fields[1] + "|" + fields[2] + "|" + fields[3] + "|" + fields[4] + "|" + fields[5] + "|" + fields[6] + "|" + fields[7] + "|";
              break;
            case 9:
            default:
              tstr = fields[fl-7] + "|" + fields[fl-6] + " " + EPD_VAL_EXTRA + "|" + fields[fl-5] + "|" + fields[fl-4] + "|" + fields[fl-3] + "|" + fields[fl-2] + "|" + fields[fl-1] + "|";
              break;
          }
          writer.write(tstr);
      }
    }
    writer.close();
  }catch( IOException e ){
    System.out.println("Cannot write output file `" + filename + "`");
  }
  System.out.println(epdKeyMagicFrom);
  System.out.println(epdKeyMagicIns);
  for( i = 0; i < epdKeyN; i++ ){
    System.out.println(i + "." + epdKey.get(i));
  }
  System.out.println("OK");
}
}
