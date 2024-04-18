package provider;

import java.io.File;

import provider.wz.XMLWZFile;

public class MapleDataProviderFactory {
   private final static String wzPath = System.getProperty("wzpath");

   private static MapleDataProvider getWZ(File in, boolean provideImages) {
      return new XMLWZFile(in);
   }

   public static MapleDataProvider getDataProvider(File in) {
      return getWZ(in, false);
   }

   public static File fileInWZPath(String filename) {
      return new File(wzPath, filename);
   }
}