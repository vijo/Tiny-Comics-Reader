/*-
 * Copyright (C) 2006 Erik Larsson
 * 
 * All rights reserved.
 * 
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA
 */

package org.catacombae.rarx;

//import org.catacombae.rarx.*;
import java.math.BigInteger;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.IOException;
import java.util.zip.CRC32;

public class DecompressionCode {
    private static final int MAXWINSIZE = 0x100000;
    private static final int MAXWINMASK = (MAXWINSIZE-1);
    private static final int UNP_MEMORY = MAXWINSIZE;
    
    public static final short NC = 298;                              /* alphabet = {0,1,2, .,NC - 1} */
    public static final short DC = 48;
    public static final short RC = 28;
    public static final short BC = 19;
    public static final short MC = 257;

    public static final int CODE_HUFFMAN = 0;
    public static final int CODE_LZ = 1;
    public static final int CODE_LZ2 = 2;
    public static final int CODE_REPEATLZ = 3;
    public static final int CODE_CACHELZ = 4;
    public static final int CODE_STARTFILE = 5;
    public static final int CODE_ENDFILE = 6;
    public static final int CODE_ENDMM = 7;
    public static final int CODE_STARTMM = 8;
    public static final int CODE_MMDELTA = 9;
    
    public static final int LHD_SPLIT_BEFORE = 1; //flag bit 0
    public static final int LHD_SPLIT_AFTER = 2; //flag bit 1
    public static final int LHD_PASSWORD = 4; //flag bit 2
    public static final int LHD_COMMENT = 8; //flag bit 3
    public static final int LHD_SOLID = 16; //flag bit 4

    public static final int LHD_WINDOWMASK = 0x00e0;
    public static final int LHD_WINDOW64 = 0;
    public static final int LHD_WINDOW128 = 32;
    public static final int LHD_WINDOW256 = 64;
    public static final int LHD_WINDOW512 = 96;
    public static final int LHD_WINDOW1024 = 128;
    public static final int LHD_DIRECTORY = 0x00e0;

    public static final int NROUNDS = 32;
    
    /* Static variables belonging to the function Unpack. */
    private static final short[] LDecode = { 0,  1,  2,  3,  4,  5,  6,  7,  8, 10, 12, 14, 16, 20,
					     24, 28, 32, 40, 48, 56, 64, 80, 96,112,128,160,192,224}; //unsigned char[14]
    private static final byte[] LBits = {0,0,0,0,0,0,0,0,1,1,1,1,2,2, //unsigned char[14]
					 2,2,3,3,3,3,4,4,4,4,5,5,5,5};
    private static final int[] DDecode = {0,     1,     2,     3,     4,     6,     8,     12,    16,    24,
					  32,    48,    64,    96,    128,   192,   256,   384,   512,   768,
                                          1024,  1536,  2048,  3072,  4096,  6144,  8192,  12288, 16384, 24576,
                                          32768, 49152, 65536, 98304, 131072,196608,262144,327680,393216,458752,
                                          524288,589824,655360,720896,786432,851968,917504,983040}; //int[48]
    private static final byte[] DBits = { 0, 0, 0, 0, 1, 1, 2, 2, 3, 3, 4, 4, 5, 5, 6, 6,
					  7, 7, 8, 8, 9, 9, 10,10,11,11,12,12,13,13,14,14,
					  15,15,16,16,16,16,16,16,16,16,16,16,16,16,16,16}; //unsigned char[48]
    private static final short[] SDDecode = {0,4,8,16,32,64,128,192}; //unsigned char[8]
    private static final byte[] SDBits = {2,2,3,4,5,6,6,6}; //unsigned char[8]
    
    public static final /*UBYTE*/ short[] InitSubstTable = {
	215, 19,149, 35, 73,197,192,205,249, 28, 16,119, 48,221,  2, 42
	,232,  1,177,233, 14, 88,219, 25,223,195,244, 90, 87,239,153,137
	,255,199,147, 70, 92, 66,246, 13,216, 40, 62, 29,217,230, 86,  6
	, 71, 24,171,196,101,113,218,123, 93, 91,163,178,202, 67, 44,235
	,107,250, 75,234, 49,167,125,211, 83,114,157,144, 32,193,143, 36
	,158,124,247,187, 89,214,141, 47,121,228, 61,130,213,194,174,251
	, 97,110, 54,229,115, 57,152, 94,105,243,212, 55,209,245, 63, 11
	,164,200, 31,156, 81,176,227, 21, 76, 99,139,188,127, 17,248, 51
	,207,120,189,210,  8,226, 41, 72,183,203,135,165,166, 60, 98,  7
	,122, 38,155,170, 69,172,252,238, 39,134, 59,128,236, 27,240, 80
	,131,  3, 85,206,145, 79,154,142,159,220,201,133, 74, 64, 20,129
	,224,185,138,103,173,182, 43, 34,254, 82,198,151,231,180, 58, 10
	,118, 26,102, 12, 50,132, 22,191,136,111,162,179, 45,  4,148,108
	,161, 56, 78,126,242,222, 15,175,146, 23, 33,241,181,190, 77,225
	,  0, 46,169,186, 68, 95,237, 65, 53,208,253,168,  9, 18,100, 52
	,116,184,160, 96,109, 37, 30,106,140,104,150,  5,204,117,112, 84
    }; // UBYTE[256]
    public static final /*UDWORD*/ int[] CRCTab = new int[256]; // Initialized in static constructor
    
    static {
	// Perform initialization of static structures and variables.
	//CRCTab = new int[256];
	InitCRC(CRCTab);
    }
    
    private CRC32 crc = new CRC32();

    public final /*UBYTE*/ short[] SubstTable = new short[256];
    
    private /*unsigned*/ byte PN1, PN2, PN3;
    
    /* Note: temp_output_buffer is allocated when a file is about to be
       extracted. It is allocated to NewLhd.UnpSize, which can be quite
       large. The assumption that the unpacked file can be stored in
       memory is awkward and must immediately be replaced with an output
       stream. */
    private /* unsigned char* */ byte[] temp_output_buffer;       /* extract files to this pointer*/
    private /* unsigned long* */ int temp_output_buffer_offset;   /* size of temp. extract buffer */
    //private /*unsigned int*/ int UnpPtr,WrPtr;
    private final /*unsigned short*/ short[] OldKey = new short[4];

    private static class Decode {
	public int MaxNum; // unsigned int
	public final int[] DecodeLen = new int[16]; // unsigned int[16]
	public final int[] DecodePos = new int[16]; // unsigned int[16]
	public final int[] DecodeNum; // unsigned int[]
	
	public Decode() {
	    this(2);
	}
	
	protected Decode(int decodeNumSize) { DecodeNum = new int[decodeNumSize]; }
    }
    private static class LitDecode extends Decode {
	public LitDecode() { super(NC); }
    }
    private static class DistDecode extends Decode {
	public DistDecode() { super(DC); }
    }
    private static class RepDecode extends Decode {
	public RepDecode() { super(RC); }
    }
    private static class MultDecode extends Decode {
	public MultDecode() { super(MC); }
    }
    private static class BitDecode extends Decode {
	public BitDecode() { super(BC); }
    }

    private final LitDecode LD = new LitDecode();
    private final DistDecode DD = new DistDecode();
    private final RepDecode RD = new RepDecode();
    private final MultDecode[] MD = { new MultDecode(), new MultDecode(), 
					     new MultDecode(), new MultDecode() }; //new MultDecode[4];
    private final BitDecode BD = new BitDecode();

    private final MultDecode[] MDPtr = { MD[0], MD[1], MD[2], MD[3] };


    /* *****************************
     * ** unpack stored RAR files **
     * *****************************/

//     BOOL UnstoreFile(void)
//     {
// 	if ((long)(*temp_output_buffer_offset=UnpRead(temp_output_buffer,
// 						      NewLhd.UnpSize))==-1)
// 	    {
// 		debug_log("Read error of stored file!");
// 		return FALSE;
// 	    }
// 	return TRUE;
//     }




    /* ****************************************
     * ** RAR decompression code starts here **
     * ****************************************/

    /* #define statements */
    public static class AudioVariables {
	public int K1,K2,K3,K4,K5; // Should be short...?
	public int D1,D2,D3,D4;
	public int LastDelta;
	public final int[] Dif = new int[11]; //unsigned
	public int ByteCount; //unsigned
	public int LastChar;

	public void zero() {
	    K1 = 0;
	    K2 = 0;
	    K3 = 0;
	    K4 = 0;
	    K5 = 0;
	    D1 = 0;
	    D2 = 0;
	    D3 = 0;
	    D4 = 0;
	    LastDelta = 0;
	    Util.zero(Dif);
	    ByteCount = 0;
	    LastChar = 0;
	}
    }

    public AudioVariables[] AudV = { new AudioVariables(), new AudioVariables(), 
					    new AudioVariables(), new AudioVariables() };

    /*
#define GetBits()                                                 \
        BitField = ( ( ( (UDWORD)InBuf[InAddr]   << 16 ) |        \
                       ( (UWORD) InBuf[InAddr+1] <<  8 ) |        \
                       (         InBuf[InAddr+2]       ) )        \
                       >> (8-InBit) ) & 0xffff;
    */
    private static int GetBits(byte[] inBuf, int inAddr, int inBit) {
	return ( ( ( (int)   (inBuf[inAddr]&0xFF)   << 16 ) |
		   ( (short) (inBuf[inAddr+1]&0xFF) <<  8 ) |
		   (         (inBuf[inAddr+2]&0xFF)       ) )
		 >>> (8-unsign(inBit)) ) & 0xffff;
    }
    
    /*
#define AddBits(Bits)                          \
        InAddr += ( InBit + (Bits) ) >> 3;     \
        InBit  =  ( InBit + (Bits) ) &  7;
    */
    
    /**
     * Adds <code>Bits</code> to the address specifiers.
     * InAddr is the high 32 bits of the 35-bit address (?) while InBit is the low 3 bits (?).
     * Modifies: InAddr, InBit
     * @param Bits the bits to add (interpreted as unsigned int)
     */
    private void AddBits(int Bits) {
	InAddr += (int)(( unsign(InBit) + unsign(Bits) ) >>> 3); // (InBit + Bits) / 8
        InBit  =  (int)(( unsign(InBit) + unsign(Bits) ) &  7);
    }

    public static int unsign(byte i) {
	return i & 0xFF;
    }
    public static int unsign(short i) {
	return i & 0xFFFF;
    }
    public static long unsign(int i) {
	return i & 0xFFFFFFFFL;
    }
    public static BigInteger unsign(long i) {
	return new BigInteger(1, Util.toByteArrayBE(i));
    }

    private static class Struct_NewFileHeader {
	short HeadCRC;
	byte HeadType;
	short Flags;
	short HeadSize;
	int PackSize;
	int UnpSize;
	byte HostOS;
	int FileCRC;
	int FileTime;
	byte UnpVer;
	byte Method;
	short NameSize;
	int FileAttr;
    };
    private NewFileHeader NewLhd;// = new NewFileHeader();
    
    private RARFileEntryStream ArcPtr = null;
    private int Encryption;
    private long UnpPackedSize;
    private /*unsigned long*/ int CurUnpRead, CurUnpWrite;

    //private byte[] UnpBuf;
    private int BitField; //unsigned int
    //private int Number; //unsigned int

    public final byte[] InBuf = new byte[8192];                  /* input read buffer            */

    public final byte[] UnpOldTable = new byte[MC*4];

    public int InAddr,InBit,ReadTop; //unsigned int

    //public int LastDist,LastLength; //unsigned int
    //private int Length,Distance; //unsigned int

    //public final int[] OldDist = new int[4]; //unsigned int
    //public int OldDistPtr; //unsigned int


    public int UnpAudioBlock;
    public int UnpChannels;
    public int CurChannel;
    public int ChannelDelta;
    private boolean FileFound;

    
    /* *** 38.3% of all CPU time is spent within this function!!!               */
    /**
     * Unpacks stuff.<p>
     *
     * <pre>
     * Global read set (constants):
     *       DBits                      (final byte[48])
     *       DDecode                    (final int[48])
     *       LBits                      (final byte[14])
     *       LDecode                    (final short[14])
     *       LHD_SOLID                  (final int)
     *       MAXWINMASK                 (final int)
     *       MDPtr                      (final {@link MultDecode}[4])
     *       SDBits                     (final byte[8])
     *       SDDecode                   (final short[8])
     *
     * Global read set (variables):
     *       FileFound                  (boolean)
     *       NewLhd                     ({@link Struct_NewFileHeader})
     *
     * Global modify set:
     *       BitField                   (int)
     *       CurChannel                 (int)
     *       DD                         (final {@link DistDecode})
     *       InAddr                     (int)
     *       InBit                      (int)
     *       InBuf                      (final byte[8192])
     *       LD                         (final {@link LitDecode})
     *       temp_output_buffer         (byte[])
     *       temp_output_buffer_offset  (int)
     *       UnpAudioBlock              (int)
     *       UnpChannels                (int)
     *
     * Local use set:
     *       Bits                       (int)
     * (in)  DestUnpSize                (long)
     *       Distance                   (int)
     *       LastDist                   (int)
     *       LastLength                 (int)
     *       Length                     (int)
     *       Number                     (int)
     *       OldDist                    (final int[4])
     *       OldDistPtr                 (int)
     * (in)  UnpAddr                    (byte[])
     *       UnpBuf                     (byte[])
     *       UnpPtr                     (int)
     *       WrPtr                      (int)
     *
     * Call set:
     *       {@link #UnpInitData}
     *       {@link #UnpReadBuf}
     *       {@link #ReadTables}
     *       {@link #debug_log}
     *       {@link System#arraycopy}
     *       {@link #DecodeNumber}
     *       {@link #DecodeAudio}
     *       {@link #GetBits}
     *       {@link #AddBits}
     *       {@link #ReadLastTables}
     * </pre>
     */
    public void Unpack(/*unsigned char*/ byte[] UnpAddr, /*long DestUnpSize, */RARFileEntryStream i_ArcPtr, OutputStream dataOut, NewFileHeader i_NewLhd) throws IOException {
	// catacombae
	ArcPtr = i_ArcPtr;
	NewLhd = i_NewLhd;
	long DestUnpSize = NewLhd.getUnpSize();
	UnpPackedSize = NewLhd.getPackSize();
	FileFound = true; //Otherwise no data will be written, just uncompressed and skipped (for the purpose of extracting solid archives).
	Encryption = 0;
	CurUnpRead = CurUnpWrite = 0;
	// /catacombae
	
	int Bits; // unsigned int	
	
	byte[] UnpBuf=UnpAddr;                           /* UnpAddr is a pointer to the unpack buffer */
	int UnpPtr = 0; // Pointer to where in the buffer we are at present
	int WrPtr = 0; // Pointer to where in the buffer UnpBuf we were at the last write
	int Length = 0;
	int Distance = 0;
	int LastDist = 0;
	int LastLength = 0;
	int Number = 0;
	final int[] OldDist = new int[4];
	int OldDistPtr = 0;

	System.out.println("UnpInitData");
	UnpInitData(UnpBuf);
	System.out.println("UnpReadBuf");
	UnpReadBuf(true);
	if((NewLhd.getFlags() & LHD_SOLID) == 0) {
	    System.out.println("ReadTables");
	    ReadTables();
	}
	
	DestUnpSize--;

	int l256 = 0, g269 = 0, e269 = 0, e256 = 0, l261 = 0, l270 = 0; // debug / understanding
	while(DestUnpSize>=0) {
	    System.out.println("Looping (DestUnpSize=" + DestUnpSize + ")");
	    UnpPtr &= MAXWINMASK;
	    
	    if(unsign(InAddr) > InBuf.length-30)
		UnpReadBuf(false);
	    if(((WrPtr-UnpPtr) & MAXWINMASK)<270 && WrPtr!=UnpPtr) {
		System.out.println("YO");
		if(FileFound) {
		    // Flush extracted data to file
		    if(!writeData(UnpBuf, UnpPtr, WrPtr, dataOut))
			DestUnpSize = -1;
		}
		WrPtr=UnpPtr;
	    }
	    else {
		//System.out.println("((" + WrPtr + "-" + UnpPtr + ") & MAXWINMASK) >= 270");
		//System.out.println(((WrPtr-UnpPtr) & MAXWINMASK) + "<270");
	    }
	    
	    if(UnpAudioBlock != 0) {
		Number = DecodeNumber(MDPtr[CurChannel]);
		if (Number==256) {
		    ReadTables();
		}
		else {
		    UnpBuf[UnpPtr++]=DecodeAudio(Number);
		    if (++CurChannel==UnpChannels)
			CurChannel=0;
		    DestUnpSize--;
		}
		//continue;
	    }
	    else {
		Number = DecodeNumber(LD);
		if(Number<256) { // stored
		    System.out.println(Number);
		    ++l256;
		    UnpBuf[UnpPtr++]=(byte)Number;
		    DestUnpSize--;
		    //continue;
		}
		else if(Number>269) {
		    ++g269;
		    Length = LDecode[Number-=270]+3;
		    if ((Bits=LBits[Number])>0) {
			BitField = GetBits(InBuf, InAddr, InBit);
			Length+=BitField>>>(16-Bits);
			AddBits(Bits);
		    }
		    
		    Number = DecodeNumber(DD);
		    Distance = DDecode[Number]+1;
		    if ((Bits=DBits[Number])>0) {
			BitField = GetBits(InBuf, InAddr, InBit);
			Distance += BitField >>> (16-Bits);
			AddBits(Bits);
		    }
		
		    if (Distance>=0x40000)
			Length++;
		
		    if (Distance>=0x2000)
			Length++;
		
		    LastDist=OldDist[OldDistPtr++ & 3]=Distance;
		    DestUnpSize-=(LastLength=Length);
		    while (Length-- != 0) {
			UnpBuf[UnpPtr]=UnpBuf[(UnpPtr-Distance) & MAXWINMASK];
			UnpPtr=(UnpPtr+1) & MAXWINMASK;
		    }
		
		    //continue;
		}
		else if(Number==269) {
		    ++e269;
		    ReadTables();
		    //continue;
		}
		else if(Number==256) {
		    ++e256;
		    Length = LastLength;
		    Distance = LastDist;
		    LastDist = OldDist[OldDistPtr++ & 3] = Distance;
		    DestUnpSize -= (LastLength=Length);
		    while(Length-- != 0) {
			UnpBuf[UnpPtr] = UnpBuf[(UnpPtr-Distance) & MAXWINMASK];
			UnpPtr = (UnpPtr+1) & MAXWINMASK;
		    }
		    //continue;
		}
		else if(Number<261) {
		    ++l261;
		    Distance=OldDist[(OldDistPtr-(Number-256)) & 3];
		    Number = DecodeNumber(RD);
		    Length=LDecode[Number]+2;
		    if ((Bits=LBits[Number])>0) {
			BitField = GetBits(InBuf, InAddr, InBit);
			Length+=BitField>>>(16-Bits);
			AddBits(Bits);
		    }
		    if (Distance>=0x40000)
			Length++;
		    if (Distance>=0x2000)
			Length++;
		    if (Distance>=0x101)
			Length++;
		    LastDist=OldDist[OldDistPtr++ & 3]=Distance;
		    DestUnpSize-=(LastLength=Length);
		    while (Length-- != 0) {
			UnpBuf[UnpPtr]=UnpBuf[(UnpPtr-Distance) & MAXWINMASK];
			UnpPtr=(UnpPtr+1) & MAXWINMASK;
		    }
		    //continue;
		}
		else if(Number<270) {
		    ++l270;
		    Distance=SDDecode[Number-=261]+1;
		    if ((Bits=SDBits[Number])>0) {
			BitField = GetBits(InBuf, InAddr, InBit);
			Distance+=BitField>>>(16-Bits);
			AddBits(Bits);
		    }
		    Length=2;
		    LastDist=OldDist[OldDistPtr++ & 3]=Distance;
		    DestUnpSize-=(LastLength=Length);
		    while (Length-- != 0) {
			UnpBuf[UnpPtr]=UnpBuf[(UnpPtr-Distance) & MAXWINMASK];
			UnpPtr=(UnpPtr+1) & MAXWINMASK;
		    }
		    //continue;
		}
		else
		    debug_log("This message will NEVER be printed. If it is printed anyway, kick the programmer.");
	    }
	}
	System.out.println("x < 256: " + l256);
	System.out.println("x > 269: " + g269);
	System.out.println("x = 269: " + e269);
	System.out.println("x = 256: " + e256);
	System.out.println("x < 261: " + l261);
	System.out.println("x < 270: " + l270);
	System.out.println("Total: " + (l256+g269+e269+e256+l261+l270));
	System.out.println("LD.DecodeLen: 0x" + Util.toHexStringBE(LD.DecodeLen));
	System.out.println("LD.DecodeNum: 0x" + Util.toHexStringBE(LD.DecodeNum));
	System.out.println("LD.DecodePos: 0x" + Util.toHexStringBE(LD.DecodePos));
	System.out.println("LD.MaxNum: 0x" + Util.toHexStringBE(LD.MaxNum));
	ReadLastTables();
	
	if (FileFound) {                           /* flush buffer                 */
	    // Flush extracted data to file
	    if(!writeData(UnpBuf, UnpPtr, WrPtr, dataOut))
		DestUnpSize = -1;
	}
	
	WrPtr=UnpPtr;
    }

    /**
     * Writes the data in <code>UnpBuf</code> to <code>dataOut</code> according to some rules.<p>
     * <pre>
     * Global read set:
     *       {@link #NewLhd}            ({@link NewFileHeader})
     *
     * Global modify set:
     *      *temp_output_buffer         (byte[])
     *       temp_output_buffer_offset  (int)
     *
     * Local use set:
     * (in)  UnpBuf                     (byte[])
     * </pre>
     */
    private boolean writeData(byte[] UnpBuf, int UnpPtr, int WrPtr, OutputStream dataOut) throws IOException {
	if (UnpPtr<WrPtr) {
	    debug_log("UnpPtr<WrPtr (" + UnpPtr + "<" + WrPtr + ")");
	    if((temp_output_buffer_offset + UnpPtr) > NewLhd.getUnpSize()) {
		debug_log("Fatal! Buffer overrun during decompression!");
		return false; //DestUnpSize=-1;
	    } else if(true) {
		int firstOutLengthOld = (0-WrPtr) & MAXWINMASK; // Don't understand...
		int firstOutLength = (UnpBuf.length-1) - WrPtr; //This should be equivalent to the above
		if(firstOutLengthOld != firstOutLength)
		    debug_log("Assumption broken for firstOutLength :(");
		
		dataOut.write(UnpBuf, WrPtr, firstOutLength);
		dataOut.write(UnpBuf, 0, UnpPtr);
		temp_output_buffer_offset += firstOutLength+UnpPtr;
	    } else {
		int firstOutLength = (0-WrPtr) & MAXWINMASK; // Don't understand...
		/* copy extracted data to output buffer                         */
		System.arraycopy(UnpBuf, WrPtr, temp_output_buffer, temp_output_buffer_offset, firstOutLength);
		/* update offset within buffer                                  */
		temp_output_buffer_offset += firstOutLength;
		/* copy extracted data to output buffer                         */
		System.arraycopy(UnpBuf, 0, temp_output_buffer, temp_output_buffer_offset, UnpPtr);
		/* update offset within buffer                                  */
		temp_output_buffer_offset += UnpPtr;
	    }
	} else {
	    debug_log("UnpPtr>WrPtr (" + UnpPtr + ">" + WrPtr + ")");
	    
	    if((temp_output_buffer_offset + (UnpPtr-WrPtr)) > NewLhd.getUnpSize()) {
		debug_log("Fatal! Buffer overrun during decompression!");
		//DestUnpSize=-1;
		return false;
	    } else if(true) {
		dataOut.write(UnpBuf, WrPtr, UnpPtr-WrPtr);
		temp_output_buffer_offset += UnpPtr-WrPtr;
	    } else {
		/* copy extracted data to output buffer                       */
		System.arraycopy(UnpBuf, WrPtr, temp_output_buffer, temp_output_buffer_offset, UnpPtr-WrPtr);
		temp_output_buffer_offset+=UnpPtr-WrPtr;        /* update offset within buffer */
	    }
	}
	return true;
    }
    /**
     * Reads into the buffer <code>InBuf</code> from the archive stream. If the flag FirstBuf
     * is set, the method preserves the last 32 bytes of InBuf and copies them to the front
     * of the buffer, filling the buffer with only <code>InBuf.length-32</code> bytes.<p>
     * <pre>
     * Global modify set:
     *       ReadTop                    (int)
     *       InAddr                     (int)
     *       Through UnpRead:
     *         UnpPackedSize            (int)
     *         CurUnpRead               (int)
     * 
     * Global read set:
     *       InBuf                      (byte[])          (contents modified in UnpRead)
     *
     * Local use set:
     * (in)  FirstBuf                   (boolean)
     *       RetCode                    (int)
     *
     * Call set:
     *       {@link UnpRead}
     * </pre>
     * @param FirstBuf flag indicating whether or not we will treat this read as the first read
     *                 to the buffer, thus destroying any previous content
     */
    private void UnpReadBuf(boolean FirstBuf) {
	int RetCode;
	if(FirstBuf) {
	    ReadTop = UnpRead(InBuf, 0, InBuf.length);
	    InAddr = 0;
	}
	else {
	    System.arraycopy(InBuf, InBuf.length-32, InBuf, 0, 32);
	    InAddr &= 0x1f; // discard all but the five least significant bytes... is this modulo 32? think so.
	    RetCode = UnpRead(InBuf, 32, InBuf.length-32);
	    if(RetCode > 0)
		ReadTop=RetCode+32;
	    else
		ReadTop=InAddr;
	}
    }

    /**
     * Reads <code>Count</code> bytes from the stream <code>ArcPtr</code> into 
     * the buffer <code>Addr</code> at position <code>offset</code>. If the
     * data is encrypted, it is automatically decrypted. (The variable
     * <code>Encryption</code> tells the function whether is shall consider
     * the data to be encrypted.)<p>
     * <pre>
     * Global read set:
     *       ArcPtr                     ({@link java.io.InputStream})
     *       Encryption                 (int)
     *       
     * Global modify set:
     *       UnpPackedSize              (int)
     *       CurUnpRead                 (int)
     * 
     * Local use set:
     * (in)  Addr                       (byte[])
     * (in)  offset                     (int)
     * (in)  Count                      (int)
     *       RetCode                    (int)
     *       I                          (int)
     *       ReadSize                   (int)
     *       TotalRead                  (int)
     *       ReadAddr                   (byte[])
     *       readAddrPointer            (int)
     *
     * Call set:
     *       tread(File, byte[], int, int)
     *       debug_log(String)
     *       DecryptBlock(byte[], int, int)
     * </pre>
     * @return the number of bytes read, or -1 if an error occurred.
     */
    private /*unsigned int*/ int UnpRead(/*unsigned char **/ byte[] Addr, int offset, /*unsigned int*/ int Count) {
	int RetCode=0;
	/*unsigned int*/ int I,ReadSize,TotalRead=0;
	/*unsigned char **/ byte[] ReadAddr;
	int readAddrPointer = offset; // catacombae
	ReadAddr=Addr;
	while(Count > 0) {
	    ReadSize=(/*unsigned int*/ int)((Count>(/*unsigned long*/ int)UnpPackedSize) ?
					      UnpPackedSize : Count);
	    if (ArcPtr==null)
		return(0);
	    RetCode=tread(ArcPtr,ReadAddr,readAddrPointer,ReadSize);
	    debug_log("Read " + RetCode + " from file.");
	    
	    CurUnpRead+=RetCode;
	    readAddrPointer+=RetCode;
	    TotalRead+=RetCode;
	    Count-=RetCode;
	    UnpPackedSize-=RetCode;
	    break; // Why the while-loop? if would work just as well.
	}
	if (RetCode!= -1) {
	    RetCode=TotalRead;
	    if (Encryption != 0) {
		if (Encryption<20) {
		    debug_log("Old Crypt() not supported!");
		}
		else {
		    for (I=0;I<(/*unsigned int*/ short)RetCode;I+=16)
			DecryptBlock(/*&Addr[I]*/Addr, I);
		}
	    }
	}
	return(RetCode);
    }


    /**
     * Reads and initializes the decompression tables.<p>
     * <pre>
     * Global read set (constants):
     *       BC                         (final short)
     *       DC                         (final short)
     *       MC                         (final short)
     *       NC                         (final short)
     *       RC                         (final short)
     *
     * Global read set (variables):
     *
     * Global modify set:
     *       BD                         (BitDecode)
     *       BitField                   (int)
     *       CurChannel                 (int)
     *       DD                         (DistDecode)
     *       InAddr                     (int)
     *       InBit                      (int) (through AddBits)
     *       InBuf                      (final byte[8192])
     *       MDPtr                      (final MultDecode[4])
     *       RD                         (RepDecode)
     *       UnpAudioBlock              (int)
     *       UnpChannels                (int)
     *       UnpOldTable                (final byte[MC*4])
     *
     *
     * Local use set:
     *       BitLength                  (final byte[BC])
     *       Table                      (final byte[MC*4])
     *       TableSize                  (int)
     *       N                          (int)
     *       I                          (int)
     *
     * Call set:
     *       UnpReadBuf
     *       GetBits
     *       Util.zero
     *       AddBits
     *       MakeDecodeTables
     *       DecodeNumber
     *       System.arraycopy
     * </pre>
     */
    private void ReadTables() {
	System.out.println("ReadTables():");
	final /*UBYTE*/ byte[] BitLength = new byte[BC];
	final /*unsigned char*/ byte[] Table = new byte[MC*4];
	/*int*/ int TableSize,N,I;

	if(InAddr>InBuf.length-25) {
	    System.out.println("InAddr == " + InAddr);
	    UnpReadBuf(false);
	}
	BitField = GetBits(InBuf, InAddr, InBit);
	UnpAudioBlock = (BitField & 0x8000);

	if((BitField & 0x4000) == 0)
	    Util.zero(UnpOldTable);
	AddBits(2);


	if(UnpAudioBlock != 0) {
	    UnpChannels=((BitField >>> 12) & 3)+1;
	    debug_log("WARNING: UnpChannels = " + UnpChannels);
	    if (CurChannel>=UnpChannels)
		CurChannel=0;
	    AddBits(2);
	    TableSize=(short)(MC*UnpChannels);
	}
	else
	    TableSize=NC+DC+RC;


	for (I=0;I<BC;I++) {
	    BitField = GetBits(InBuf, InAddr, InBit);
	    BitLength[I]=(/*UBYTE*/byte)(BitField >>> 12);
	    AddBits(4);
	}
	MakeDecodeTables(BitLength, BD, 0, BC);
	
	I=0;
	while(I<TableSize) {
	    if(InAddr>InBuf.length-5)
		UnpReadBuf(false);
	    int number = DecodeNumber(BD);
	    if(number<16)
		Table[I++]=(byte)((number+UnpOldTable[I]) & 0xf);
	    else
		if(number==16) {
		    BitField = GetBits(InBuf, InAddr, InBit);
		    N=((BitField >>> 14)+3);
		    AddBits(2);
		    while(N-- > 0 && I<TableSize) {
			Table[I]=Table[I-1];
			I++;
		    }
		}
		else {
		    if(number==17) {
			BitField = GetBits(InBuf, InAddr, InBit);
			N=((BitField >>> 13)+3);
			AddBits(3);
		    }
		    else {
			BitField = GetBits(InBuf, InAddr, InBit);
			N=((BitField >>> 9)+11);
			AddBits(7);
		    }
		    while(N-- > 0 && I<TableSize)
			Table[I++]=0;
		}
	}
	if(UnpAudioBlock != 0)
	    for(I=0; I<UnpChannels; I++)
		MakeDecodeTables(Table, MDPtr[I], (I*MC), MC);
	else {
	    MakeDecodeTables(Table, LD, 0, NC);
	    MakeDecodeTables(Table, DD, NC, DC);
	    MakeDecodeTables(Table, RD, (NC+DC), RC);
	}
	
	System.arraycopy(Table, 0, UnpOldTable, 0, UnpOldTable.length);
    }


    private void ReadLastTables() {
	if (ReadTop>=InAddr+5) {
	    if (UnpAudioBlock != 0) {
		int number = DecodeNumber(MDPtr[CurChannel]);
		if (number==256)
		    ReadTables();
	    }
	    else {
		int number = DecodeNumber(LD);
		if (number==269)
		    ReadTables();
	    }
	}
    }

    /**
     * Modifies the <code>Decode</code> object <code>Dec</code> supplied as parameter
     * according to the data in <code>LenTab</code>. Initializes <code>Dec</code> for
     * further use in decompression.<p>
     * <pre>
     * Global modify set:
     *       <empty>
     *
     * Global read set:
     *       <empty>
     * 
     * Local use set:
     * (in)  LenTab                     (byte[])
     * (i/o) Dec                        ({@link Decode})
     * (in)  offset                     (int)
     * (in)  Size                       (int)
     *       LenCount                   (final int[16])
     *       TmpPos                     (final int[16])
     *       I                          (int)
     *       M                          (int)
     *       N                          (int)
     * </pre>
     */
    private void MakeDecodeTables(/*unsigned char **/ byte[] LenTab,
				  Decode Dec,
				  int offset,
				  int Size) {
	final /*int*/int[] LenCount = new int[16];
	final /*int*/int[] TmpPos = new int[16];
	/*int*/int I;
	/*long*/int M, N;
	//memset(LenCount,0,sizeof(LenCount)); // Java does this automatically
	for(I=offset; I<offset+Size; I++)
	    LenCount[LenTab[I] & 0xF]++;

	LenCount[0]=0;
	for(TmpPos[0]=Dec.DecodePos[0]=Dec.DecodeLen[0]=0,N=0,I=1; I<16; I++) {
	    N = 2*(N+LenCount[I]);
	    M = N << (15-I);
	    if(M>0xFFFF)
		M=0xFFFF;
	    Dec.DecodeLen[I]=(/*unsigned int*/int)M;
	    TmpPos[I]=Dec.DecodePos[I]=((Dec.DecodePos[I-1] & 0xFFFF)+LenCount[I-1]);
	}

	for(I=offset; I<offset+Size; I++)
	    if(LenTab[I]!=0)
		Dec.DecodeNum[TmpPos[LenTab[I] & 0xF]++]=I;
	Dec.MaxNum=Size;
    }


    /* *** 52.6% of all CPU time is spent within this function!!!               */
    /**
     * Decodes a number from the supplied <code>Decode</code> object.<p>
     * <pre>
     * Global modify set:
     *       BitField                   (int)
     *       In AddBits:
     *         InAddr                   (int)
     *         InBit                    (int)
     *
     * Local use set:
     * (in)  Deco                       ({@link Decode})
     *       I                          (int)
     *       N                          (int)
     *
     * Call set:
     *       GetBits
     *       AddBits
     * </pre>
     *
     * @return the decoded number...?
     */
    private int DecodeNumber(Decode Deco) {
	/*unsigned int*/ int I;
	/*register unsigned int*/ int N;
	System.out.println("GetBits(" + InBuf + ", " + InAddr + ", " + InBit + ");");
	BitField = GetBits(InBuf, InAddr, InBit);
	
	N=(BitField & 0xFFFE);
	System.out.println("(1) N == " + N + " (BitField == " + BitField + ")");
	if(N<Deco.DecodeLen[8])  {
	    if(N<Deco.DecodeLen[4]) {
		if(N<Deco.DecodeLen[2]) {
		    if(N<Deco.DecodeLen[1])
			I=1;
		    else
			I=2;
		} else {
		    if(N<Deco.DecodeLen[3])
			I=3;
		    else
			I=4;
		}
	    } else {
		if(N<Deco.DecodeLen[6])  {
		    if(N<Deco.DecodeLen[5])
			I=5;
		    else
			I=6;
		} else {
		    if(N<Deco.DecodeLen[7])
			I=7;
		    else
			I=8;
		}
	    }
	} else {
	    if(N<Deco.DecodeLen[12]) {
		if(N<Deco.DecodeLen[10]) {
		    if(N<Deco.DecodeLen[9])
			I=9;
		    else
			I=10;
		} else {
		    if(N<Deco.DecodeLen[11])
			I=11;
		    else
			I=12;
		}
	    } else {
		if(N<Deco.DecodeLen[14]) {
		    if(N<Deco.DecodeLen[13])
			I=13;
		    else
			I=14;

		} else {
		    I=15;
		}
	    }

	}
	
	AddBits(I);
	N = Deco.DecodePos[I] + ((N-Deco.DecodeLen[I-1]) >>> (16-I));
	System.out.println("(2) N == " + N);
	if(N >= Deco.MaxNum)
	    N=0;
	System.out.println("(3) N == " + N);
	
	System.out.println("Deco.DecodeNum[" + N + "] == " + Deco.DecodeNum[N]);
	return Deco.DecodeNum[N];
    }

    /**
     * Intializes data for the unpack process by setting the variables <code>InAddr</code> and <code>InBit</code> to 0.<br>
     * If the archive is a solid archive the method also does the following:<br>
     * <ul>
     * <li>Set <code>ChannelDelta</code> and <code>CurChannel</code> to 0.</li>
     * <li>Zero all <code>AudioVariables</code> objects in <code>AudV</code> through {@link AudioVariables#zero}.</li>
     * <li>Zero the arrays <code>unpBuf</code> and <code>UnpOldTable</code>.</li>
     * </ul><p>
     * <pre>
     * Global read set:
     *       NewLhd                     ({@link Struct_NewFileHeader})
     *       LHD_SOLID                  (final int)
     *       MAXWINSIZE                 (final int)
     *
     * Global modify set:
     *       InAddr                     (int)
     *       InBit                      (int)
     *       ChannelDelta               (int)
     *       CurChannel                 (int)
     *       AudV                       ({@link AudioVariables})
     *       
     * Local use set:
     * (in)  unpBuf                     (byte[])
     * </pre>
     */
    private void UnpInitData(byte[] unpBuf) {
	InAddr=InBit=0;
	if(!((NewLhd.getFlags() & LHD_SOLID) != 0)) {
	    System.out.println("1");
	    ChannelDelta=CurChannel=0;

	    //memset(AudV,0,sizeof(AudV));
	    System.out.println("2");
	    for(AudioVariables av : AudV)
		av.zero();
	    //memset(OldDist,0,sizeof(OldDist));
	    //Util.zero(OldDist);
	    //OldDistPtr=0;
	    //LastDist=LastLength=0;
	    //memset(UnpBuf,0,MAXWINSIZE);
	    System.out.println("3");
	    Util.zero(unpBuf, 0, MAXWINSIZE);
	    //memset(UnpOldTable,0,sizeof(UnpOldTable));
	    System.out.println("4");
	    Util.zero(UnpOldTable);
	    //UnpPtr=WrPtr=0;
	}
    }

    /**
     * Does some kind of audio decoding that I'm not familiar with conceptually.<p>
     * <pre> 
     * Global read set:
     *       CurChannel                 (int)
     *
     * Global modify set:
     *       AudV                       ({@link AudioVariables}[])
     *       ChannelDelta               (int)
     *
     * Local use set:
     * (in)  Delta                      (int)
     *       V                          ({@link AudioVariables})
     *       Ch                         (int)
     *       NumMinDif                  (int)
     *       MinDif                     (int)
     *       PCh                        (int)
     *       I                          (int)
     * </pre>
     */
    private /*UBYTE*/ byte DecodeAudio(int Delta) {
	AudioVariables V;
	/*unsigned */int Ch;
	/*unsigned */int NumMinDif,MinDif;
	int PCh,I;
	
	V=AudV[CurChannel];
	V.ByteCount++;
	V.D4=V.D3;
	V.D3=V.D2;
	V.D2=V.LastDelta-V.D1;
	V.D1=V.LastDelta;
	PCh=8*V.LastChar+V.K1*V.D1+V.K2*V.D2+
	    V.K3*V.D3+V.K4*V.D4+V.K5*ChannelDelta;
	PCh=(PCh>>3) & 0xFF;

	Ch=PCh-Delta;

	I=((/*signed char*/byte)Delta)<<3;

	V.Dif[0]+=Math.abs(I);
	V.Dif[1]+=Math.abs(I-V.D1);
	V.Dif[2]+=Math.abs(I+V.D1);
	V.Dif[3]+=Math.abs(I-V.D2);
	V.Dif[4]+=Math.abs(I+V.D2);
	V.Dif[5]+=Math.abs(I-V.D3);
	V.Dif[6]+=Math.abs(I+V.D3);
	V.Dif[7]+=Math.abs(I-V.D4);
	V.Dif[8]+=Math.abs(I+V.D4);
	V.Dif[9]+=Math.abs(I-ChannelDelta);
	V.Dif[10]+=Math.abs(I+ChannelDelta);

	ChannelDelta=V.LastDelta=(/*signed char*/byte)(Ch-V.LastChar);
	V.LastChar=Ch;

	if((V.ByteCount & 0x1F)==0) {
	    MinDif=V.Dif[0];
	    NumMinDif=0;
	    V.Dif[0]=0;
	    for (I=1;(/*unsigned */int)I<V.Dif.length;I++) {
		if(V.Dif[I]<MinDif) {
		    MinDif=V.Dif[I];
		    NumMinDif=I;
		}
		V.Dif[I]=0;
	    }
	    switch(NumMinDif) {
	    case 1:
		if(V.K1>=-16)
		    V.K1--;
		break;
	    case 2:
		if(V.K1<16)
		    V.K1++;
		break;
	    case 3:
		if(V.K2>=-16)
		    V.K2--;
		break;
	    case 4:
		if(V.K2<16)
		    V.K2++;
		break;
	    case 5:
		if(V.K3>=-16)
		    V.K3--;
		break;
	    case 6:
		if(V.K3<16)
		    V.K3++;
		break;
	    case 7:
		if(V.K4>=-16)
		    V.K4--;
		break;
	    case 8:
		if(V.K4<16)
		    V.K4++;
		break;
	    case 9:
		if(V.K5>=-16)
		    V.K5--;
		break;
	    case 10:
		if(V.K5<16)
		    V.K5++;
		break;
	    }
	}
	return((/*UBYTE*/byte)Ch);
    }







    /* ***************************************************
     * ** CRCCrypt Code - decryption engine starts here **
     * ***************************************************/
    
    
    
/* #define rol(x,n)  (((x)<<(n)) | ((x)>>(8*sizeof(x)-(n)))) */
    /** Rotate left. Pure functional behavior, no side effects. */
    private static byte rol(byte value, int shift) {
	return (byte) (((value)<<(shift)) | ((value)>>>(8*1-(shift))));
    }
    /** @see #rol(byte,int) */
    private static short rol(short value, int shift) {
	return (short)(((value)<<(shift)) | ((value)>>>(8*2-(shift))));
    }
    /** @see #rol(byte,int) */
    private static int rol(int value, int shift) {
	return        (((value)<<(shift)) | ((value)>>>(8*4-(shift))));
    }
    /** @see #rol(byte,int) */
    private static long rol(long value, int shift) {
	return        (((value)<<(shift)) | ((value)>>>(8*8-(shift))));
    }

/* #define ror(x,n)  (((x)>>(n)) | ((x)<<(8*sizeof(x)-(n)))) */
    /** Rotate right. Pure functional behavior, no side effects. */
    private static byte ror(byte value, int shift) {
	return (byte) (((value)>>>(shift)) | ((value)<<(8*1-(shift))));
    }
    /** @see #ror(byte,int) */
    private static short ror(short value, int shift) {
	return (short)(((value)>>>(shift)) | ((value)<<(8*2-(shift))));
    }
    /** @see #ror(byte,int) */
    private static int ror(int value, int shift) {
	return        (((value)>>>(shift)) | ((value)<<(8*4-(shift))));
    }
    /** @see #ror(byte,int) */
    private static long ror(long value, int shift) {
	return        (((value)>>>(shift)) | ((value)<<(8*8-(shift))));
    }

/*
#define substLong(t) ( (UDWORD)SubstTable[(int)t&255] | \
           ((UDWORD)SubstTable[(int)(t>> 8)&255]<< 8) | \
           ((UDWORD)SubstTable[(int)(t>>16)&255]<<16) | \
           ((UDWORD)SubstTable[(int)(t>>24)&255]<<24) )
*/
    private int substLong(int t) {
	return ( ((/*UDWORD*/int)SubstTable[(int)(t>> 0)&255]<< 0) |
		 ((/*UDWORD*/int)SubstTable[(int)(t>> 8)&255]<< 8) |
		 ((/*UDWORD*/int)SubstTable[(int)(t>>16)&255]<<16) |
		 ((/*UDWORD*/int)SubstTable[(int)(t>>24)&255]<<24) );
    }

    //    public static final /*UDWORD*/ int[] CRCTab = new int[256]; // Initialized in static constructor


    public /*UDWORD*/ int[] Key = new int[4];

    
    public void EncryptBlock(/*UBYTE*/byte[] Buf) {
	//int I;

	int /*UDWORD*/ A,B,C,D,T,TA,TB;
// #ifdef NON_INTEL_BYTE_ORDER
	if(true) {
	    A = ((0xFF & Buf[0] ) | ((0xFF & Buf[1] )<<8)  | ((0xFF & Buf[2] )<<16) | ((0xFF & Buf[3] )<<24))^Key[0];
	    B = ((0xFF & Buf[4] ) | ((0xFF & Buf[5] )<<8)  | ((0xFF & Buf[6] )<<16) | ((0xFF & Buf[7] )<<24))^Key[1];
	    C = ((0xFF & Buf[8] ) | ((0xFF & Buf[9] )<<8)  | ((0xFF & Buf[10])<<16) | ((0xFF & Buf[11])<<24))^Key[2];
	    D = ((0xFF & Buf[12]) | ((0xFF & Buf[13])<<8)  | ((0xFF & Buf[14])<<16) | ((0xFF & Buf[15])<<24))^Key[3];
	}
	else { // The above code should yield the same result as these lines. Test this assumption.
	    A = Util.readIntLE(Buf, 0 )^Key[0];
	    B = Util.readIntLE(Buf, 4 )^Key[1];
	    C = Util.readIntLE(Buf, 8 )^Key[2];
	    D = Util.readIntLE(Buf, 12)^Key[3];
	}
// #else
//   UDWORD *BufPtr;
//   BufPtr=(UDWORD *)Buf;
//   A=BufPtr[0]^Key[0];
//   B=BufPtr[1]^Key[1];
//   C=BufPtr[2]^Key[2];
//   D=BufPtr[3]^Key[3];
// #endif
	for(int I=0;I<NROUNDS;I++) {
	    T=((C+rol(D,11))^Key[I&3]);
	    TA=A^substLong(T);
	    T=((D^rol(C,17))+Key[I&3]);
	    TB=B^substLong(T);
	    A=C;
	    B=D;
	    C=TA;
	    D=TB;
	}
// #ifdef NON_INTEL_BYTE_ORDER
	if(true) {
	    C ^= Key[0];
	    Buf[0]=(byte)(0xFF & C);
	    Buf[1]=(byte)(0xFF & (C>>>8));
	    Buf[2]=(byte)(0xFF & (C>>>16));
	    Buf[3]=(byte)(0xFF & (C>>>24));
	    D ^= Key[1];
	    Buf[4]=(byte)(0xFF & D);
	    Buf[5]=(byte)(0xFF & (D>>>8));
	    Buf[6]=(byte)(0xFF & (D>>>16));
	    Buf[7]=(byte)(0xFF & (D>>>24));
	    A ^= Key[2];
	    Buf[8]=(byte)(0xFF & A);
	    Buf[9]=(byte)(0xFF & (A>>>8));
	    Buf[10]=(byte)(0xFF & (A>>>16));
	    Buf[11]=(byte)(0xFF & (A>>>24));
	    B ^= Key[3];
	    Buf[12]=(byte)(0xFF & B);
	    Buf[13]=(byte)(0xFF & (B>>>8));
	    Buf[14]=(byte)(0xFF & (B>>>16));
	    Buf[15]=(byte)(0xFF & (B>>>24));
	}
	else {
	    System.arraycopy(Util.toByteArrayLE(C^Key[0]),  0, Buf, 0, 4);
	    System.arraycopy(Util.toByteArrayLE(D^Key[1]),  4, Buf, 0, 4);
	    System.arraycopy(Util.toByteArrayLE(A^Key[2]),  8, Buf, 0, 4);
	    System.arraycopy(Util.toByteArrayLE(B^Key[3]), 12, Buf, 0, 4);
	}
// #else
// 	BufPtr[0]=C^Key[0];
// 	BufPtr[1]=D^Key[1];
// 	BufPtr[2]=A^Key[2];
// 	BufPtr[3]=B^Key[3];
// #endif
	UpdKeys(Buf);
}


    /**
     * Decrypts a 16 byte block in the buffer <code>Buf</code> at position <code>offset</code>.
     * The decrypted data is stored at the same place in <code>Buf</code>, thus overwriting the
     * encrypted contents.<p>
     * <pre>
     * Global read set:
     *
     * Global modify set:
     *       In UpdKeys:
     *         Key                      (final int[4])
     *
     * Local use set:
     * (in)  Buf                        (byte[])
     * (in)  offset                     (int)
     *       InBuf                      (byte[])
     *       A                          (int)
     *       B                          (int)
     *       C                          (int)
     *       D                          (int)
     *       T                          (int)
     *       TA                         (int)
     *       TB                         (int)
     * </pre>
     */
    public void DecryptBlock(/*UBYTE*/byte[] Buf, int offset) {
	//int I;
	final int n = offset;
	byte[] /*UBYTE*/ InBuf = new byte[16];
	int /*UDWORD*/ A,B,C,D,T,TA,TB;
	System.arraycopy(Buf, n, InBuf, 0, InBuf.length); // memcpy(InBuf,Buf,sizeof(InBuf));
	
	// Swap all bytes since data is stored in little endian format.
// 	if(true) {
	A = ((0xFF & Buf[n+0] ) | ((0xFF & Buf[n+1] )<<8)  | ((0xFF & Buf[n+2] )<<16) | ((0xFF & Buf[n+3] )<<24))^Key[0];
	B = ((0xFF & Buf[n+4] ) | ((0xFF & Buf[n+5] )<<8)  | ((0xFF & Buf[n+6] )<<16) | ((0xFF & Buf[n+7] )<<24))^Key[1];
	C = ((0xFF & Buf[n+8] ) | ((0xFF & Buf[n+9] )<<8)  | ((0xFF & Buf[n+10])<<16) | ((0xFF & Buf[n+11])<<24))^Key[2];
	D = ((0xFF & Buf[n+12]) | ((0xFF & Buf[n+13])<<8)  | ((0xFF & Buf[n+14])<<16) | ((0xFF & Buf[n+15])<<24))^Key[3];
// 	}
// 	else { // The above code should yield the same result as these lines. Test this assumption.
	int A2 = Util.readIntLE(Buf, n+0 )^Key[0];
	int B2 = Util.readIntLE(Buf, n+4 )^Key[1];
	int C2 = Util.readIntLE(Buf, n+8 )^Key[2];
	int D2 = Util.readIntLE(Buf, n+12)^Key[3];
// 	}
	
	if(A != A2 || B != B2 || C != C2 || D != D2) {
	    System.out.println("Assumption broken!");
	    System.out.println(" A: 0x" + Util.toHexStringBE(A) + " A2: 0x" + Util.toHexStringBE(A2));
	    System.out.println(" B: 0x" + Util.toHexStringBE(B) + " B2: 0x" + Util.toHexStringBE(B2));
	    System.out.println(" C: 0x" + Util.toHexStringBE(C) + " C2: 0x" + Util.toHexStringBE(C2));
	    System.out.println(" D: 0x" + Util.toHexStringBE(D) + " D2: 0x" + Util.toHexStringBE(D2));
	}
	else
	    System.out.println("Assumption correct!");
	
	for(int I=NROUNDS-1; I>=0; I--) {
	    T=((C+rol(D,11))^Key[I&3]);
	    TA=A^substLong(T);
	    T=((D^rol(C,17))+Key[I&3]);
	    TB=B^substLong(T);
	    A=C;
	    B=D;
	    C=TA;
	    D=TB;
	}
	
	if(true) {
	    C ^= Key[0];
	    Buf[n+0 ]=(byte)(0xFF & C);
	    Buf[n+1 ]=(byte)(0xFF & (C>>>8));
	    Buf[n+2 ]=(byte)(0xFF & (C>>>16));
	    Buf[n+3 ]=(byte)(0xFF & (C>>>24));
	    D ^= Key[1];
	    Buf[n+4 ]=(byte)(0xFF & D);
	    Buf[n+5 ]=(byte)(0xFF & (D>>>8));
	    Buf[n+6 ]=(byte)(0xFF & (D>>>16));
	    Buf[n+7 ]=(byte)(0xFF & (D>>>24));
	    A ^= Key[2];
	    Buf[n+8 ]=(byte)(0xFF & A);
	    Buf[n+9 ]=(byte)(0xFF & (A>>>8));
	    Buf[n+10]=(byte)(0xFF & (A>>>16));
	    Buf[n+11]=(byte)(0xFF & (A>>>24));
	    B ^= Key[3];
	    Buf[n+12]=(byte)(0xFF & B);
	    Buf[n+13]=(byte)(0xFF & (B>>>8));
	    Buf[n+14]=(byte)(0xFF & (B>>>16));
	    Buf[n+15]=(byte)(0xFF & (B>>>24));
	}
	else {
	    System.arraycopy(Util.toByteArrayLE(C^Key[0]), 0, Buf, n+0 , 4);
	    System.arraycopy(Util.toByteArrayLE(D^Key[1]), 0, Buf, n+4 , 4);
	    System.arraycopy(Util.toByteArrayLE(A^Key[2]), 0, Buf, n+8 , 4);
	    System.arraycopy(Util.toByteArrayLE(B^Key[3]), 0, Buf, n+12, 4);
	}
	
	UpdKeys(InBuf);
    }


    /*
     * As we can't use unsigned data types in Java, we'll have to unsign the data
     * every time we use it. Modifications: unsign the usage of Buf[I] by "& 0xFF".
     */
    /**
     * Updates keys. ;)
     * <pre>
     * Global modify set:
     *       Key                        (int[4])
     * Global read set:
     *       CRCTab                     (int[256])
     * Local use set:
     * (in)  Buf                        (byte[])
     *       I                          (int)
     * </pre>
     */
    public void UpdKeys(byte[]/*UBYTE*/ Buf) {
	for(int I=0; I<16; I+=4) {
	    Key[0]^=CRCTab[Buf[I]   & 0xFF];                 /* xxx may be I'll rewrite this */
	    Key[1]^=CRCTab[Buf[I+1] & 0xFF];                 /* in asm for speedup           */
	    Key[2]^=CRCTab[Buf[I+2] & 0xFF];
	    Key[3]^=CRCTab[Buf[I+3] & 0xFF];
	}
    }

    /* Password is supposed to be trimmed to the actual password length and not zero-terminated. */
    /**
     * Sets crypt keys. ;)<br>
     * <code>Password</code> is supposed to be trimmed to the actual 
     * password length and not zero-terminated.<p>
     * <pre>
     * Global read set:
     *       InitSubstTable             (final short[256])
     *       CRCTab
     *
     * Global modify set:
     *       Key                        (final int[4])
     *       SubstTable                 (final short[256])
     *       In SetOldKeys:
     *         OldKey                   (final short[4])
     *         PN1                      (byte)
     *         PN2                      (byte)
     *         PN3                      (byte)
     *
     * Local use set:
     *       Password                   (byte[])
     *       I                          (int)
     *       J                          (int)
     *       K                          (int)
     *       N1                         (short)
     *       N2                         (short)
     *       Psw                        (final byte[256])
     *       Ch                         (short)
     *
     * Call set:
     *       SetOldKeys
     *       System.arraycopy
     *       EncryptBlock
     * </pre>
     */
    public void SetCryptKeys(byte[] Password) {
	/*unsigned int*/  int I,J,K, PswLength;
	/*unsigned char*/ short N1,N2;
	final /*unsigned char*/ byte[] Psw = new byte[256];
	
	/*UBYTE*/ short Ch;
	
	SetOldKeys(Password);
	
	Key[0]=0xD3A3B879; // Removed the L at the end (indicates 32 bits in C, 64 in Java)
	Key[1]=0x3F6D12F7; // -||-
	Key[2]=0x7515A235; // -||-
	Key[3]=0xA4E7F123; // -||-
	//memset(Psw,0,sizeof(Psw)); // Arrays are automatically initialized to 0 in Java.
	System.arraycopy(Password, 0, Psw, 0, (Password.length<Psw.length?Password.length:Psw.length));
	PswLength = Password.length;//strlen(Password);
	System.arraycopy(InitSubstTable, 0, SubstTable, 0, SubstTable.length); // memcpy(SubstTable,InitSubstTable,sizeof(SubstTable));
	
	for (J=0;J<256;J++) {
	    for (I=0;I<PswLength;I+=2) {
		N2=/*(unsigned char)*/(byte)CRCTab[((Psw[I+1]&0xFF)+J)&0xFF];
		for (K=1, N1=/*(unsigned char)*/(byte)CRCTab[((Psw[I]&0xFF)-J)&0xFF]; /* I had to add "&& (N1 < 256)",    */
		     (N1!=N2) && (N1 < 256);                                          /* because the system crashed with  */
		     N1++, K++) {                                                     /* encrypted RARs                   */
		    /* Swap(&SubstTable[N1],&SubstTable[(N1+I+K)&0xFF]);            */
		    Ch=SubstTable[N1];
		    SubstTable[N1]=SubstTable[(N1+I+K)&0xFF];
		    SubstTable[(N1+I+K)&0xFF]=Ch;
		}
	    }
	}
	byte[] currentPswBuf = new byte[16];
	for (I=0;I<PswLength;I+=16) {
	    System.arraycopy(Psw, I, currentPswBuf, 0, 16);
	    EncryptBlock(currentPswBuf);
	}
    }

    /**
     * Sets old keys. ;)
     * <pre>
     * Global read set:
     *       CRCTab                     (final int[256])
     *
     * Global modify set:
     *       OldKey                     (final short[4])
     *       PN1                        (byte)
     *       PN2                        (byte)
     *       PN3                        (byte)
     *
     * Local use set:
     * (in)  Password                   (byte[])
     *       PswCRC                     (int)
     *       Ch                         (byte)
     *
     * Call set:
     *       //CalcCRC32
     *       rol
     * </pre>      
     */
    private void SetOldKeys(/*char **/byte[] Password) {
	/*UDWORD*/int PswCRC;
	/*UBYTE*/byte Ch;
	crc.reset();
	crc.update(Password);
	PswCRC=(int)crc.getValue();//CalcCRC32(0xFFFFFFFF,/*(UBYTE*)*/Password,0,Util.strlen(Password));
	OldKey[0]=(/*UWORD*/short)PswCRC;
	OldKey[1]=(/*UWORD*/short)(PswCRC>>>16);
	OldKey[2]=OldKey[3]=0;
	PN1=PN2=PN3=0;
	//while ((Ch=*Password)!=0) {
	for(int i = 0; (Ch=Password[i])!=0; ++i) {
	    PN1+=Ch;
	    PN2^=Ch;
	    PN3+=Ch;
	    PN3=(/*UBYTE*/byte)rol(PN3,1);
	    OldKey[2]^=((/*UWORD*/short)(Ch^CRCTab[Ch]));
	    OldKey[3]+=((/*UWORD*/short)(Ch+(CRCTab[Ch]>>>16)));
	    //Password++;
	}
    }
    
    /** 
     * Initializes the CRC table <code>crcTab</code>, which has to have a length of 256 elements.
     * @param crcTab the array where the initialized CRC table is to be stored
     * @return the same array that was given as input parameter (for convenience)
     */
    static int[] InitCRC(int[] crcTab) {
	int I, J;
	/*UDWORD*/int C;
	for (I=0;I<256;I++) {
	    for (C=I,J=0;J<8;J++)
		C=(C & 1)!=0 ? (C>>>1)^0xEDB88320 : (C>>>1);
	    crcTab[I]=C;
	}
	return crcTab;
    }


    /**
     * Calculates the CRC32 checksum from its arguments.
     * <pre>
     * Global read set:
     *       CRCTab                     ([256])
     *
     * Local use set:
     * (in) StartCRC                   (int)
     * (in)  Addr                       (byte[])
     * (in)  offset                     (int)
     * (in)  Size                       (int)
     *       I                          (int)
     *
     * Purely functional behavior, no side effects.
     * </pre>
     * @return the updated sum
     */
    static int CalcCRC32(int StartCRC, byte[] Addr, int offset, int Size) {
	/*unsigned */int I;
	for (I=offset; I<Size; I++)
	    StartCRC = CRCTab[(byte)StartCRC ^ Addr[I]] ^ (StartCRC >>> 8);
	return(StartCRC);
    }
    
    /* No side effects. */
    private static void debug_log(String s) {
	System.err.println("DEBUG: " + s);
    }

    /** No side effects (except position in f increasing). */
    private static int tread(RARFileEntryStream f, byte[] buffer, int offset, int length) {
	try {
	    return f.read(buffer, offset, length);
	} catch(Exception e) { e.printStackTrace(); return -1; }
    }
}

/* **************************************************************************
 ****************************************************************************
 ****************************************************************************
 ************************************************************************** */

























/* **************************************************************************
 ****************************************************************************
 ****************************************************************************
 ****************************************************************************
 *******                                                              *******
 *******                                                              *******
 *******                                                              *******
 *******              D E B U G    F U N C T I O N S                  *******
 *******                                                              *******
 *******                                                              *******
 *******                                                              *******
 ****************************************************************************
 ****************************************************************************
 ****************************************************************************
 ************************************************************************** */
// #ifdef _DEBUG_LOG


// /* -- global stuff -------------------------------------------------------- */
// char  log_file_name[256];                   /* file name for the log file   */
// DWORD debug_start_time;                     /* starttime of debug           */
// BOOL  debug_started = FALSE;                /* debug_log writes only if     */
//                                             /* this is TRUE                 */
// /* ------------------------------------------------------------------------ */


// /* -- global functions ---------------------------------------------------- */
// void debug_init_proc(char *file_name)
// /* Create/Rewrite a log file                                                */
// {
//   FILE *fp;
//   char date[] = __DATE__;
//   char time[] = __TIME__;

//   debug_start_time = GetTickCount();        /* get start time               */
//   strcpy(log_file_name, file_name);         /* save file name               */

//   if((fp = fopen(log_file_name, CREATETEXT)) != NULL)
//   {
//     debug_started = TRUE;                   /* enable debug                 */
//     fprintf(fp, "Debug log of UniquE's RARFileLib\n"\
//                 "~^~~~~~~~~~~~~~~~~~~~~~~~~~~~~^~\n");
//     fprintf(fp, "(executable compiled on %s at %s)\n\n", date, time);
//     fclose(fp);
//   }
// }


// void debug_log_proc(char *text, char *sourcefile, int sourceline)
// /* add a line to the log file                                               */
// {
//   FILE *fp;

//   if(debug_started == FALSE) return;        /* exit if not initialized      */

//   if((fp = fopen(log_file_name, APPENDTEXT)) != NULL) /* append to logfile  */

//   {
//     fprintf(fp, " %8u ms (line %u in %s):\n              - %s\n",
//             (/*unsigned */int)(GetTickCount() - debug_start_time),
//             sourceline, sourcefile, text);
//     fclose(fp);
//   }
// }

// /* ------------------------------------------------------------------------ */
// #endif
/* **************************************************************************
 ****************************************************************************
 ****************************************************************************
 ************************************************************************** */

