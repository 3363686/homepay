import org.apache.commons.io.FilenameUtils;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Scanner;

/**
 * Created by _ame_ on 2019-01-13 19:20:12.
 */
public class epd2psv{
    private static final String EPD_EXT = ".psv";
    private static final String EPD_KEY_FN = "psvkeys.txt";
    private static final int EPD_KEY_N = 30;
    private static final int EPD_VAL_LEN = 40;
    private static final String EPD_VAL_ABSENT = "----";
    private static final String EPD_VAL_EXTRA = "???";
public static void main( String[] args ){
    ArrayList<String> epdKey = new ArrayList<>(EPD_KEY_N);
    ArrayList<Character> epdKeyT = new ArrayList<>(EPD_KEY_N);
    int[] epdKeyDataPos;
    String[] epdKeyData;
    String epdKeyMagicFrom = "";
    String epdKeyMagicIns = "";
    String fileContent;
    StringBuilder fileData, psvLine;
    String filename = null;
    BufferedWriter writer;
    int i,j,k, startPos, epdKeyN;
    char tchr;
    String tstr;
  try{
// Read and parse Keywords file
    filename = epd2psv.class.getProtectionDomain().getCodeSource().getLocation().getPath();
    filename = FilenameUtils.getFullPath(filename) + EPD_KEY_FN;
    Scanner scanner = new Scanner(new File(filename));
    while( scanner.hasNextLine() ){
      tstr = scanner.nextLine();
      if( tstr.isEmpty() ) continue;    // Skip empty lines
      tchr = tstr.charAt(0);
      switch( tchr ){
        case '#':
          break;
        case '$':
          epdKeyMagicFrom = tstr.substring(1).trim();
          break;
        default:
          epdKey.add(tstr.substring(1).trim());
          epdKeyT.add(tchr);
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
// and find every keyword in fileData
  //noinspection Duplicates
  for( i = 0; i < epdKeyN; i++ ){
    int pos = fileData.indexOf(epdKey.get(i), startPos);
    if( pos > 0 ){
// Mark split points with "|" inserted before keywords
      fileData.setCharAt(pos -1, '|');
// Store epdKey Data Position
      epdKeyDataPos[i] = pos + epdKey.get(i).length();    // if epdKey found
    }else{
      epdKeyDataPos[i] = 0;                               // if epdKey not found
    }
  }
// Do split by "|"
  String[] records = fileData.substring(startPos).split("\\|");
  psvLine = new StringBuilder(EPD_VAL_LEN * epdKeyN);
  // Write all records & fields according to config settings
  String[] fields;
  for( i = 0; i < epdKeyN; i++ ){
    tchr = epdKeyT.get(i);
    if( tchr == '-' ) continue;           // Skip as cfg
    int pos = epdKeyDataPos[i];
    int fl = 0;
    if( pos > 0 ){
      epdKeyData[i] = fileData.substring(pos-1, fileData.indexOf("|", pos));
      fields = epdKeyData[i].split(" ");
      fl = fields.length;
    }
    switch( tchr ){
      case '=':
        if( fl   <= 1 ){
          if( fl <= 0 ) psvLine.append("-");
          else          psvLine.append("<");
          psvLine.append("| |");
        }else{
          psvLine.append(" |").append(fields[fl-1]).append("|");
        }
        break;
      case '2':
        if( fl   <= 5 ){
          if( fl <= 0 ) psvLine.append("-");
          else          psvLine.append("<");
          psvLine.append("| |");
        }else{
          if( fl >  6 ) psvLine.append(">");
          else          psvLine.append(" ");
          psvLine.append("|").append(fields[fl-1]).append(".").append(fields[fl-3]).append("|");
        }
        break;
      case '+':
        switch( fl ){
          case 0:
            psvLine.append("-| | | | |");
            break;
          case 1:
            psvLine.append("<| | | | |");
            break;
          case 2:
            psvLine.append("<| | | |").append(fields[1]).append("|");
            break;
          case 3:
            psvLine.append(" |").append(fields[1]).append("| | |").append(fields[2]).append("|");
            break;
          case 4:
            psvLine.append(" |").append(fields[1]).append("| |").append(fields[2]).append("|").append(fields[3]).append("|");
            break;
          case 5:
            psvLine.append(" |").append(fields[1]).append("|").append(fields[2]).append("|").append(fields[3]).append("|").append(fields[4]).append("|");
            break;
          case 6:
          default:
            psvLine.append(">|").append(fields[fl-4]).append("|").append(fields[fl-3]).append("|").append(fields[fl-2]).append("|").append(fields[fl-1]).append("|");
            break;
        }
        break;
      case '0':
      case '1':
      case '3':
      default:
        switch( fl ){
          case 0:
            psvLine.append("-| | | | | | | |");
            break;
          case 1:
            psvLine.append("<| | | | | | | |");
            break;
          case 2:
            psvLine.append("<| | | | | | |").append(fields[1] + "|");
            break;
          case 3:
            psvLine.append("<| | | |").append(fields[1] + "| | |" + fields[2] + "|");
            break;
          case 4:
            psvLine.append("<| | | |").append(fields[1] + "| |" + fields[2] + "|" + fields[3] + "|");
            break;
          case 5:
            psvLine.append("<| | | |").append(fields[1] + "|" + fields[2] + "|" + fields[3] + "|" + fields[4] + "|");
            break;
          case 6:
            psvLine.append(" |").append(fields[1] + "|" + fields[2] + "|" + fields[3] + "|" + fields[4] + "| | |" + fields[5] + "|");
            break;
          case 7:
            psvLine.append(" |").append(fields[1] + "|" + fields[2] + "|" + fields[3] + "|" + fields[4] + "| |" + fields[5] + "|" + fields[6] + "|");
            break;
          case 8:
            psvLine.append(" |").append(fields[1] + "|" + fields[2] + "|" + fields[3] + "|" + fields[4] + "|" + fields[5] + "|" + fields[6] + "|" + fields[7] + "|");
            break;
          case 9:
          default:
            psvLine.append(">|").append(fields[fl-7] + "|" + fields[fl-6] + "|" + fields[fl-5] + "|" + fields[fl-4] + "|" + fields[fl-3] + "|" + fields[fl-2] + "|" + fields[fl-1] + "|");
            break;
        }
        psvLine.append(tstr);
    }
  }
  writer.close();
  System.out.println(epdKeyMagicFrom);
  System.out.println(epdKeyMagicIns);
  for( i = 0; i < epdKeyN; i++ ){
    System.out.println(i + "." + epdKey.get(i));
  }
  System.out.println("OK");
}
}
