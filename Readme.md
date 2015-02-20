## Visualize Streaming ACN E1.31 on the RVIP layout

This tool will receive and visualize DMX over Ethernet / Streaming ACN
/ sACN / E1.31 and displays it laid out for the RVIP. This is a
complete ripoff of SCANer

The side of the RVIP is illuminated using two sets of 4 panels arranged
horizontally, like so:

    panel1-panel2-panel3-panel4
       ---space for logo--
    panel5-panel6-panel7-panel8

Each PANEL consists of 5 ROWS of 20 pixels each in a Zig-Zag pattern.

    19 18 17 16 15 14 13 12 11 10 09 08 07 06 05 04 03 02 01 00 - Row 0
    20 21 ............................................... 38 39 - Row 1
    59 58 ............................................... 41 40 - Row 2
    60 61 ............................................... 78 79 - Row 3
    99 98 ............................................... 81 80 - Row 4

Pixels are roughly square.

Display starts at slot # 1 (DMX start code is ignored).  

This can be configured to receive multiple universes on a single IP
address, or in a universe per MC ip address configuration.  

JSON file can be used to set default universe and other parameters.  

    Keybindings:
    0-9: change universe

    R / r: Draw entire universe as RGB pixels. 
        Default to offset 0 so slot 1 == Red for pixel # 1
        The slot for each color is displayed at top of cell
    L / l: Display slot number for all cells.

    - : Shift RGB view left by one  (slot 2 == Red for pixel # 1)
    + : Shift RGB view right by one

    F: supply fake data to the visualization

TODO  
-- Select and display the network interface for receiving multicast (currently host default)  
-- Select universes > 9 via key or GUI.  
-- Parse and display more information from the ACN packet  

## Running
Pre-compiled.app has been exported from Processing, it is ZIPPed to ensure it comes out of Git OK.
To run more than one instance (watch multiple universes), run it from the Terminal:
open -n ./application.macosx/SACNer.app/
 
Otherwise put this folder into ~/Documents/Processing and download Processing 2 from http://processing.org
