
/*

{   "width":800,

    "height":600,
    

}

*/
import processing.data.JSONObject;
import processing.core.PApplet;

// int id = json.getInt("id");
// String species = json.getString("species");
// String name = json.getString("name");

public final class Config {
    JSONObject json_file;    
    PApplet app;

    Config(PApplet _app){
        app = _app;
        load();

    }
    
   public  void load(){
        try{
      json_file =  app.loadJSONObject("settings.json");
        }catch(Exception e){
            
            System.out.println("Loading default settings");
        json_file =  app.loadJSONObject("default_settings.json");
            
        }
    }
    
   void save(){
        
    }
  

  String getString(String field){
    try{
      return json_file.getString(field);
    }catch(Exception e){
      System.err.println("Could not get "+field+"from json");
      return "";
    }

}
  int getInt(String field){return json_file.getInt(field); }
    int getInt(String field,int defaultVal){return json_file.getInt(field,defaultVal); }

  boolean getBoolean(String field,boolean defaultVal){ return json_file.getBoolean(field,defaultVal);}

  int width(){return getInt("width");}
  int height(){return getInt("height");}

}
