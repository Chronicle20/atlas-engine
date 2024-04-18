package client.keybind;

import java.util.Arrays;

import net.packet.OutPacket;

public class MapleQuickslotBinding {
   public static final int QUICKSLOT_SIZE = 8;

   public static final byte[] DEFAULT_QUICKSLOTS = {0x2A, 0x52, 0x47, 0x49, 0x1D, 0x53, 0x4F, 0x51};

   private final byte[] m_aQuickslotKeyMapped;

   // Initializes quickslot object for the user.
   // aKeys' length has to be 8.
   public MapleQuickslotBinding(byte[] aKeys) {
      if (aKeys.length != QUICKSLOT_SIZE) {
         throw new IllegalArgumentException(String.format("aKeys' size should be %d", QUICKSLOT_SIZE));
      }

      this.m_aQuickslotKeyMapped = aKeys.clone();
   }

   public void Encode(OutPacket p) {
      // Quickslots are default.
      // The client will skip them and call CQuickslotKeyMappedMan::DefaultQuickslotKeyMap.
      if (Arrays.equals(this.m_aQuickslotKeyMapped, DEFAULT_QUICKSLOTS)) {
         p.writeBool(false);
         return;
      }

      p.writeBool(true);

      // For some reason Nexon sends these as integers, similar to CFuncKeyMappedMan.
      // However there's no evidence any key can be above 0xFF anyhow.
      // Regardless, we need to encode an integer to avoid an error 38 crash; as CFuncKeyMapped::m_aQuickslotKeyMapped is int[8].
      for (byte nKey : this.m_aQuickslotKeyMapped) {
         p.writeInt(nKey);
      }
   }

   public byte[] GetKeybindings() {
      return m_aQuickslotKeyMapped;
   }
}