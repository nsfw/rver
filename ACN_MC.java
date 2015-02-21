import java.net.*;
import java.io.*;
import java.util.Arrays;

class ACN implements Runnable{
    Thread thread;

    ACNFrame last,last_ignored;
    int last_seq=0;

    long lastACNat;
    int listenUniverse;
    String sourceName;
    boolean _new_frame;
    Config config;
    int aliveCount; // 255 means fresh
    MulticastSocket sock;
    DatagramPacket rx_packet;
    int currentPriority;
    long currentPriorityTime; // ignore if not seen for awhile

    InetAddress ip;

    double rxDelta;

    ACN(int uni, Config _config){
      config=_config;
      listenUniverse=uni;
      sourceName="NONE";
      last = new ACNFrame();
      connect(listenUniverse);

      thread = new Thread(this);
      thread.start();
    }

    public int rxFps(){
        return rxDelta != 0 ? (int)(1/(rxDelta/1000.0)) : 0;
    }

    private boolean receive(){
        try{
            if(!sock.isBound()  || sock.isClosed())
                return false;

            byte dataRX[] = new byte[5000];
            rx_packet=new DatagramPacket(dataRX, dataRX.length);
            sock.receive(rx_packet);

            synchronized (last) {
                ACNFrame current = new ACNFrame(rx_packet);


                if(listenUniverse!=current.universe)
                    System.err.println("Warn: packet universe != socket universe "+current.universe+" !="+listenUniverse);
                if(current.startCode()!=0)
                    System.err.println("Warn: packet start code !=0! startCode="+current.startCode());


                if(last!=null && currentPriority > 0){
                    // compare this and last


                    if(current.priority !=last.priority)
                        System.err.println(String.format("Warn: detected different priorities: %s=%d %s=%d",last.source_name,last.priority,current.source_name,current.priority));



                    // enforce priority here
                    if(currentPriority > 0 && currentPriority > current.priority && System.currentTimeMillis()-currentPriorityTime < 500){
                        //priority is active - ignore
                        System.err.println(String.format("Ignoring '%s'  priority %d is lower than %d",current.source_name,current.priority,currentPriority));
                        last_ignored=current;
                        return false;
                    }else if(currentPriority != current.priority){
                        // we are done ignoring
                        last_ignored=null;
                    }

                    if(!Arrays.equals(current.cid,last.cid))
                        System.err.println("Warn: detected a muliple CID sources on this universe ");
                    if(current.seq > last_seq && current.seq-last_seq!=1 && last_seq!=0)
                        System.err.println("Warn:  sequence hiccup: "+last.seq+ " then "+current.seq);

                }
                currentPriority = current.priority;
                currentPriorityTime=System.currentTimeMillis();

                last_seq=current.seq;
                rxDelta = System.currentTimeMillis()-lastACNat;
                lastACNat=System.currentTimeMillis();
                sourceName=current.source_name;
                last=current;
                return true;
            }
        } catch ( SocketTimeoutException socketTimeoutException ){
            // normal
        } catch(IOException ex){
            System.err.println("Error in receive: " + ex);
            return false;
        }
        return false;
    }

    public boolean update(){

        return false;
    }

    static NetworkInterface iface;

    static NetworkInterface getInterface(){
        if(iface != null)
            return iface;
        String default_wired="en0";
        try{
            iface = NetworkInterface.getByName(default_wired);
            if(iface == null){
                System.err.println("could not get interface '"+default_wired+"' find something else..");
                iface = NetworkInterface.getNetworkInterfaces().nextElement();
            }
            System.out.println("Use network iface "+iface.getDisplayName()+" on IP's");

            for(InterfaceAddress ifaddr : iface.getInterfaceAddresses())
                System.out.println(ifaddr.getAddress());

            return iface;
        }catch(SocketException e){
            System.err.println("Could not get interface: "+e);
        }
        return null; //boo
    }

    private  void connect(int universe){
        rxDelta=0;
        try{
            if(sock != null){
                System.out.println("Leaving multicast group "+ip);
                sock.leaveGroup(ip);
                sock.close();
            }
        } catch(IOException e) {
            System.err.println("Error in connect-leave: "+e);
        }

        try {
            String acn_ip=ACNFrame.ip_for_universe(universe);
            System.out.println("Connect to ACN Universe "+universe+" @ "+acn_ip);

            ip=InetAddress.getByName(acn_ip);
            sock=new MulticastSocket(ACNFrame.ACN_PORT);

            sock.setSoTimeout(120);

            // bind to WIRED
            sock.setNetworkInterface(ACN.getInterface());
            sock.joinGroup(ip); //set up the receive
        } catch(UnknownHostException ex) {
            System.err.println(ex);
        } catch(IOException ex) {
            System.err.println("Error in connect-join: "+ex);
        }
    }

    ACNFrame lastframe(){return last; }
    int fakePhase = 0;

    void fakeRx(){
        int[] temp = new int[512];
        for(int i=0; i < temp.length; i++){
            temp[i] = (((fakePhase+i)%17)*32)%256;
        }
        ACNFrame fake = new ACNFrame("FAKE DATA",1,temp);
        fake.dataLen = 512;
        fake.priority = 99;//lower than default priority
        fake.seq = fakePhase;
        boolean success= rx(fake);
        fakePhase++;
    }

    boolean rx(ACNFrame current){

        if(current.startCode()!=0)
           System.err.println("Warn: packet start code !=0! startCode="+current.startCode());

        if(last!=null && currentPriority > 0){
            // compare this and last

            if(current.seq == last.seq)
                System.err.println(String.format("Warn: duplace sequence number  %d=%d",last.seq,current.seq));
            if(current.priority !=last.priority)
                System.err.println(String.format("Warn: detected different priorities: %s=%d %s=%d",last.source_name,last.priority,current.source_name,current.priority));

           // enforce priority here
            if(currentPriority > 0 && currentPriority > current.priority && System.currentTimeMillis()-currentPriorityTime < 500){
                //priority is active - ignore
                System.err.println(String.format("Ignoring '%s'  priority %d is lower than %d",current.source_name,current.priority,currentPriority));
                last_ignored=current;
                return false;
            } else if(currentPriority != current.priority){
                // we are done ignoring
                last_ignored=null;
            }

            if(!Arrays.equals(current.cid,last.cid))
                System.err.println("Warn: detected a muliple CID sources on this universe ");
            if(current.seq > last_seq && current.seq-last_seq!=1 && last_seq!=0)
                System.err.println("Warn:  sequence hiccup: "+last.seq+ " then "+current.seq);
        }

        currentPriority = current.priority;
        currentPriorityTime=System.currentTimeMillis();

        last_seq=current.seq;
        rxDelta = System.currentTimeMillis()-lastACNat;
        lastACNat=System.currentTimeMillis();
        sourceName=current.source_name;
        last=current;
        _new_frame=true;

        return true;
    }

    public boolean newFrame(){boolean was =_new_frame; _new_frame=false; return was;} // one-time flag
    public int currentUniverse(){return listenUniverse;}
    public String currentSourceName(){return sourceName;}

    public void setUniverse(int num){
        listenUniverse=num;
        last.universe = num;
        last.clear();
        connect(listenUniverse);
    }

    public boolean active(){return (System.currentTimeMillis()-lastACNat) < 500;}
    public int currentPriority(){return currentPriority;}

    public void run() {
        while (Thread.currentThread() == thread) {
            if(receive()){
                _new_frame=true; // flag from GUI that something happened
                aliveCount=255;
            }else {
                aliveCount-=200;
                if(aliveCount < 0)
                    aliveCount=0;
            }
        }
    }
}
