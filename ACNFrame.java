import processing.core.PApplet;
import java.net.*;
import java.io.*;
import java.util.Arrays;

public class ACNFrame {
    long timestamp;
    DatagramPacket packet; // raw received packet

    public String source_name;
    int[] data; // remember that java bytes are signed ... so instead create 8-bit ints
    int dataLen;
    int seq; // 8-bits unsigned
    int universe;
    int vector;
    boolean parsed;
    int rawSize;
    final static int ACN_PORT = 5568;

    int[] cid; // component ID
    int priority;

    final int ACN_IDX_VECTOR = 18;
    final int ACN_EXPECTED_VECTOR = 4;
    final int ACN_IDX_SEQ = 111;
    final int ACN_SOURCE_IDX = 44;
    final int ACN_SOURCE_LEN = 64;
    final int ACN_UNIVERSE_IDX = 113;
    final int ACN_PRIORITY_IDX = 108;
    final int ACN_CID_IDX = 22;

    final int ACN_DATA_LEN_IDX = 123;
    final int ACN_DATA_IDX = 123+2; // includes start code

    // fake way
    ACNFrame(){init(513);}

    ACNFrame(String name,int _universe,int[] justData){
        init(513);
        this.packet=null;
        parsed=true;
        priority = 100;
        timestamp=System.currentTimeMillis();
        set(_universe,justData);
        source_name = name;
    }

    ACNFrame(DatagramPacket p){
        this.packet = p;
        init(1);
        parsed=parse();
        if(!parsed)
            System.err.println("ERROR parsing incoming packet N="+p.getLength());
        timestamp = System.currentTimeMillis();
    }

    void clear(){Arrays.fill(data,0);}

    private void init(int len){
        source_name="";
        seq=0;
        universe=-1;
        data = new int[len];
        dataLen=0;
        parsed=false;
    }

    public int startCode(){
        if(!parsed)
            return 255;
        return data[0];
    }

    public String toString(){
        if(parsed)
            return String.format("ACN from '%s' universe %d n=%d seq=%d",source_name,universe,dataLen,seq);
        else
            return "Parse error";
    }

    public static String ip_for_universe(int universe){
        int high = (universe >> 8)&0xff;
        int low = universe & 0xff;
        return String.format("239.255.%d.%d",high,low);
    }



    // quick + dirty parsing
    private boolean parse(){

        int index = 0;
        DataInputStream input = new DataInputStream(new ByteArrayInputStream(packet.getData()));
        input.mark(999);
        /* System.out.println("RLP PDU "+packet.getData()[16]+","+packet.getData()[17]);
           System.out.println("FRAM PDU "+packet.getData()[38]+","+packet.getData()[39]);
           System.out.println("DMP PDU "+packet.getData()[115]+","+packet.getData()[116]);
        */

        try{
            byte[] working_copy = new byte[packet.getLength()];
            rawSize =packet.getLength();
            vector = pluckInt(input,ACN_IDX_VECTOR);

            if(vector != ACN_EXPECTED_VECTOR){
                System.err.println("Expected ACN vector == "+ACN_EXPECTED_VECTOR+" we don't recognize this one:"+vector);
                return false;
            }

            seq = pluckByte(input,ACN_IDX_SEQ);
            source_name = pluckString(input,ACN_SOURCE_IDX,ACN_SOURCE_LEN);
            universe = pluckShort(input,ACN_UNIVERSE_IDX);

            dataLen = pluckShort(input,ACN_DATA_LEN_IDX);

            // Includes start byte
            data = pluckBytes(input,ACN_DATA_IDX,dataLen);

            priority = pluckByte(input,ACN_PRIORITY_IDX);
            cid = pluckBytes(input,ACN_CID_IDX,16);

            // printBytes(data); // DEBUG

        } catch(Exception e) {
            System.out.println(e);
            return false;
        }
        return true;
    }

    private void printBytes(int[] buf){
        System.out.println( "length=" + buf.length );
        for(int i =0; i < buf.length; i++){
            if(i >0 && i % 32 ==0)
                System.out.println();
            System.out.print(String.format("%02X",(0xFF & ((int)buf[i]))));
            System.out.print(" ");
        } System.out.println();
    }

    private int[] pluckBytes(DataInputStream stream, int index,int len) throws IOException {
        stream.reset();
        stream.skipBytes(index);
        byte[] buf = new byte[len];
        int[] buf_int = new int[len];
        stream.readFully(buf);
        for(int i=0; i <len; i++)
            buf_int[i] = (0xFF & ((int)buf[i])); //turn into 8-bit unsigned int

        return buf_int;
    }

    private short pluckShort(DataInputStream stream,int index) throws IOException {
        stream.reset();
        stream.skipBytes(index);
        return stream.readShort();
    }
    private int pluckInt(DataInputStream stream,int index) throws IOException {
        stream.reset();
        stream.skipBytes(index);
        return stream.readInt();
    }

    private int pluckByte(DataInputStream stream,int index) throws IOException {
        // handle java's silly signed bytes
        stream.reset();
        stream.skipBytes(index);
        byte byte_temp = stream.readByte();
        return (0xFF & ((int)byte_temp));
    }

    private String pluckString(DataInputStream stream, int index, int len) throws IOException {
        stream.reset();
        stream.skipBytes(index);
        byte[] buf = new byte[len];
        stream.readFully(buf);

        return  new String(buf, "ISO-8859-1");
    }


    public byte[] raw_data(){return packet.getData();} // the UDP packet


    // External accessors

    public void set(int[] incoming){ // does not include start code

        if(data == null) //allocate full universe
            data = new int[513];

        data[0] = 0;     // startcode

        for(int i=1; i < incoming.length; i++){
            data[i] = incoming[i-1] & 0xFF;
        }
    }

    public void set(int _universe, int[] incoming){
        set(incoming);
        universe=_universe;
    }

    public int get(int idx){
        if(data == null || idx >=data.length)
            return 0;
        return data[idx];
    }
}
