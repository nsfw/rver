# usage: python ./test.py "pulse" 1
import time
import sys

# sys.path.insert(0,"../Lumos")

# needs lumos w/ unicast support
from lumos import DMXSource
import colorsys

FPS = 30.0#30.0
SLEEP_TIME=1.0/FPS

NUM_UNIVERSES = 8

PIXELS=50
STRING_LEN=512#2*PIXELS

import commands
import sys

def findWiredIP():
    ip="127.0.0.1"
    out=commands.getoutput("ifconfig en0")
    for entry in out.split("\n\t"):
        if entry.startswith("inet "): #IP4 only
            ip=entry.split(" ")[1]
    return ip

def findWifiIP():
    ip="127.0.0.1"
    out=commands.getoutput("ifconfig en1")
    for entry in out.split("\n\t"):
        if entry.startswith("inet "): #IP4 only
            ip=entry.split(" ")[1]
    return ip

def rgb_for_n(n,scale=255):
    #n=1
    i = n % 7 #RGBCMYW
    triplet = [0,0,0]

    if i&0b1:
        triplet[0]=scale
    if i & 0b010:
        triplet[1]=scale
    if i & 0b100:
        triplet[2]=scale
    return triplet

def color_for_n(n):
    triplet = rgb_for_n(n);
    scales = range(0,255)
    colors=[]
    for c in scales:
        colors+=rgb_for_n(c)
        # colors+=[int(s*c) for s in triplet]

    return colors[0:STRING_LEN]


# def scale_it(triplets):
#     scales = range(0,255)
#     colors=[]
#     for c in scales:
#         colors+=[int(s*c) for s in triplets]
#     return colors

def color_strips():
    t = []
    for x in range(0,7):
        t+=rgb_for_n(x)
    print t
    return scale_it(t*PIXELS)

def make_data(n, other=False):
    if other:
        return color_strips()
    else:
        return color_for_n(n)*PIXELS

def one_r_g_b():
    l=[]
    colors = []

    for b in [0,255]:
        for g in [0,255]:
            for r in [0,255]:
                if r > 0 or g > 0 or b > 0:
                    colors.append([r,g,b])
    levels = 24
    for triplet in colors:
        for level in range(0,levels):
            sf = float(level)/levels
            l.extend([int(sf*c) for c in  triplet])
    return l

def debug_rainbow(n=0):
    resolution = n*32
    l=[]
    s=1.0
    v=1.0#0.5+(float(NUM_UNIVERSES-n)/NUM_UNIVERSES)*0.5
    for x in range(0,512):
        h = (float(x)/512)*resolution
        r,g,b = colorsys.hsv_to_rgb(h, s, v)
        l.extend([int(255*r),int(255*g),int(255*b)])
    return l

# each slot is a nuber
def debug_slots(n):
    return list(range(0,255))+list(range(0,255))
# each triplet is the same number

def debug_pixel(n):
    l=[]
    for pixel in range(1,171):
        l.extend([pixel,pixel,pixel] )
    return l

def list_rotate(l,n):
    return l[n:] + l[:n]

def debug_rgb(n):
    l=[]
    triplets=[255,0,0, 0,255,0, 0,0,255]
    triplets = list_rotate(triplets, 3*(n%3))
    for pixel in range(1,60):
        l.extend(triplets)
    return l

def debug_one(n):
    return [128]+511*[0]

def debug_pulse(n):
    l=[]
    pulse = 0;
    attack = 8
    decay = 0.025
    for pixel in range(1,171):
        if(pixel < attack):
            pulse+=(1/float(attack))
        else:
            pulse*=(1-decay)
        pulse = min(pulse,1.0)
        r=255 if n%2==0 else 0
        g=255 if n%3==0 else 0
        b=255 if n%1==0 else 0
        l.extend([int(pulse*r),int(pulse*g),int(pulse*b)])
    return l

def debug_binary(n):
    l=[]
    ON = [255]*3; OFF=[0,32,128]
    for x in range(1,171):
        l.extend(ON) if x&(1<<n) else l.extend(OFF)
    return l;

def debug_black(n):
    return [0]*512

def debug_gray(n):
    return [128] * 512

fxes={"rgb":debug_rgb,
      "pixel":debug_pixel,
      "binary":debug_binary,
      "rainbow":debug_rainbow,
      "pulse":debug_pulse,
      "slots":debug_slots,
      "black":debug_black,
      "gray":debug_gray}

if len(sys.argv)>=2:
    fxname = sys.argv[1]
else:
    fxname="binary"

fn = None
if fn is None:
    try:
        fn = eval("debug_%s"%fxname)
    except:
        fn=debug_binary
        print "fallback to %s"

def send(data_for_sources,phase=0):
    iphase = (3*(-phase))%510
    # print iphase
    for uni in universes:
       # print "sending: " + uni
        try:
            src.send_data(data_for_sources[uni-1][iphase:iphase+STRING_LEN], universe=uni)
            time.sleep(0.001)
        except Exception as e:
            pass#print e

## Entry

print "fxname = %s - use fx %s"%(fxname,fn)
src = DMXSource(ip="127.0.0.1")
universes = range(1,NUM_UNIVERSES+1)

data=[fn(n)*6 for n in range(0,NUM_UNIVERSES)]

print "Sending on %d universes"%NUM_UNIVERSES
f=0
p=0
scroll=0

if len(sys.argv)>=3:
    scroll = int(sys.argv[2])

send([0]*512,0)
time.sleep(0.5)
while True:
    #print "send %d "%p
    send(data,p)
    time.sleep(SLEEP_TIME)
    f+=1
    if scroll > 0 and f % scroll==0:
            p+=1
  #
