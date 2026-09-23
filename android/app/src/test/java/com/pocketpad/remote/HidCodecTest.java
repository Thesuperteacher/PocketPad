package com.pocketpad.remote;
import org.junit.Test;
import static org.junit.Assert.*;
public class HidCodecTest {
    @Test public void printableAsciiIsMapped(){for(char c=32;c<=126;c++)assertNotNull("Character "+c,HidCodec.character(c));}
    @Test public void modifiersAndDigitsMatchUsbHid(){assertArrayEquals(new int[]{2,4},HidCodec.character('A'));assertArrayEquals(new int[]{0,39},HidCodec.character('0'));assertArrayEquals(new int[]{2,31},HidCodec.character('@'));assertArrayEquals(new int[]{0,49},HidCodec.character('\\'));}
    @Test public void unicodeIsExplicitlyUnsupportedOverHid(){assertNull(HidCodec.character('ñ'));assertNull(HidCodec.character('€'));}
    @Test public void reportsReleaseAllKeys(){assertEquals(8,HidCodec.keyboard(0,0).length);assertArrayEquals(new byte[8],HidCodec.keyboard(0,0));assertEquals(40,HidCodec.key("Enter"));assertEquals(0,HidCodec.key("unknown"));}
}
