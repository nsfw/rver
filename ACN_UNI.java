import java.net.*;
import java.io.*;
// import java.util.Arrays;
import java.util.ArrayList;

// Unicast ACN receiver
class ACN_UNI implements Runnable {

    Thread thread;

    int startUniverse;
    int endUniverse;
    int port;

    ArrayList<ACNFrame> data;      // data from all subscribed universes

    DatagramSocket sock;
    int rxCount;

    ACN_UNI(int _start, int _end, int _port){
        startUniverse = _start;
        endUniverse = _end;
        port = _port;

        rxCount = 0;

        // initilize our universe array
        // data = new ArrayList<ACNFrame>(endUniverse - startUniverse + 1);
        data = new ArrayList<ACNFrame>();
        for(int uni=startUniverse; uni < endUniverse+1; uni++){
            data.add(new ACNFrame());
        }
        System.out.println("len = " + data.size());

        // initialize our socket
        System.out.println("Listening for sACN on port " + port);
        try {
            sock = new DatagramSocket(port);
        } catch(SocketException ex) {
            System.err.println(ex);
        }

        // receive in a seperate thread
        thread = new Thread(this);
        thread.start();
    }

    public void run() {
        while (Thread.currentThread() == thread) {
            receive();
        }
    }

    private boolean receive(){
        DatagramPacket rx_packet;

        try{
            if(!sock.isBound()  || sock.isClosed()){
                System.err.println("ACN_UNI - socket closed");
                return false;
            }

            byte dataRX[] = new byte[5000];
            rx_packet=new DatagramPacket(dataRX, dataRX.length);
            sock.receive(rx_packet);

            synchronized (data) {
                rxCount++;
                // parse it and store it in it's appropriate universe slot
                ACNFrame frame = new ACNFrame(rx_packet);
                int uni = frame.universe;
                if (uni >= startUniverse && uni <= endUniverse){
                    data.set(uni-startUniverse, frame);
                } else {
                    System.err.println("ACN_UNI - universe out of range " + uni);
                }
            }

        } catch ( SocketTimeoutException socketTimeoutException ){
            // normal
        } catch(IOException ex){
            System.err.println("Error in receive: " + ex);
            return false;
        }
        return false;
    }

    ACNFrame frameForUniverse(int uni){
        int index = uni - startUniverse;
        return data.get(index);
    }

    int fakePhase = 0;

    void fakeRx(){
        // put random data into all universes

        for(int uni=startUniverse; uni < endUniverse; uni++){

            int[] temp = new int[512];
            for(int i=0; i < temp.length; i++){
                temp[i] = (((fakePhase+i)%17)*32)%256;
            }
            ACNFrame fake = new ACNFrame("FAKE DATA",1,temp);
            fake.dataLen = 512;
            fake.priority = 99;//lower than default priority
            fake.seq = fakePhase;

            data.set(uni-startUniverse, fake);

            fakePhase++;
        }
    }
}
