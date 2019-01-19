import org.apache.commons.io.FilenameUtils;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Scanner;

/**
 * Created by _ame_ on 2019-01-13 19:20:12.
 */
public class epd2psv{
    private static final String EPD_EXT = ".psv";
    private static final String EPD_KEY_FN = "psvKeys.txt";
    private static final int EPD_KEY_N = 30;
    private static final int EPD_VAL_LEN = 40;
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
  psvLine = new StringBuilder(EPD_VAL_LEN * epdKeyN);
  // Write all records & fields according to config settings
  String[] fields = new String[0];
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
      case '=':                                 // Only last 1
        if( fl   <= 1 ){
          if( fl <= 0 ) psvLine.append("-");
          else          psvLine.append("<");
          psvLine.append("| |");
        }else{
          psvLine.append(" |").append(fields[fl-1]).append("|");
        }
        break;
      case '2':                                 // Date
        if( fl   <= 5 ){
          if( fl <= 0 ) psvLine.append("-");
          else          psvLine.append("<");
          psvLine.append("| |");
        }else{
          if( fl >  6 ) psvLine.append(">");
          else          psvLine.append(" ");
          psvLine.append("|").append(fields[fl-2]).append(".").append(fields[fl-4]).append("|");
        }
        break;
      case '+':                                 // Totals
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
            psvLine.append("1|").append(fields[1]).append("| |").append(fields[2]).append("|").append(fields[3]).append("|");
            break;
          case 5:
            psvLine.append("2|").append(fields[1]).append("|").append(fields[2]).append("|").append(fields[3]).append("|").append(fields[4]).append("|");
            break;
          case 6:
          default:
            psvLine.append(">|").append(fields[fl-4]).append("|").append(fields[fl-3]).append("|").append(fields[fl-2]).append("|").append(fields[fl-1]).append("|");
            break;
        }
        break;
      case '0':                                 // Std
      case '1':                                 // Except 1st
      case '3':                                 // Water
      default:
        switch( fl ){
          case 0:
            psvLine.append("-| | | | | | | |");
            break;
          case 1:
            psvLine.append("<| | | | | | | |");
            break;
          case 2:
            psvLine.append("<| | | | | | |").append(fields[1]).append("|");
            break;
          case 3:
            psvLine.append("<| | | |").append(fields[1]).append("| | |").append(fields[2]).append("|");
            break;
          case 4:
            psvLine.append("<| | | |").append(fields[1]).append("| |").append(fields[2]).append("|").append(fields[3]).append("|");
            break;
          case 5:
            psvLine.append("<| | | |").append(fields[1]).append("|").append(fields[2]).append("|").append(fields[3]).append("|").append(fields[4]).append("|");
            break;
          case 6:
            psvLine.append(" |").append(fields[1]).append("|").append(fields[2]).append("|").append(fields[3]).append("|").append(fields[4]).append("| | |").append(fields[5]).append("|");
            break;
          case 7:
            psvLine.append("1|").append(fields[1]).append("|").append(fields[2]).append("|").append(fields[3]).append("|").append(fields[4]).append("| |").append(fields[5]).append("|").append(fields[6]).append("|");
            break;
          case 8:
            psvLine.append("2|").append(fields[1]).append("|").append(fields[2]).append("|").append(fields[3]).append("|").append(fields[4]).append("|").append(fields[5]).append("|").append(fields[6]).append("|").append(fields[7]).append("|");
            break;
          case 9:
          default:
            psvLine.append(">|").append(fields[fl - 7]).append("|").append(fields[fl - 6]).append("|").append(fields[fl - 5]).append("|").append(fields[fl - 4]).append("|").append(fields[fl - 3]).append("|").append(fields[fl - 2]).append("|").append(fields[fl - 1]).append("|");
            break;
        }
    }
    psvLine.append("\n");   // TMP
  }
  try{
    writer.write(String.valueOf(psvLine));
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
