package com.filesearch;

import java.io.File;


public class Main {

  public static void main(String[] args) {
  
  
  String s = "/sdcard";
 
 
  recuPrint(new File(s),0);
   
  }
  
  public static void recuPrint(File current,int sublevel)
  {
    
    
    for(int i=0;i<sublevel;i++)
    {
      System.out.print('\t');
    }
    //System.out.print('\n');
    if(current.isFile())
    {
      System.out.println(current.getName());
      return ;
    }else if(current.isDirectory()){
      System.out.println(current.getName());
      try{
      Thread.sleep(100);
      //to prevent deadlock (not responding condition)
      }catch(Exception e)
      {
        e.printStackTrace();
      }
      File[] all= current.listFiles();
      for(File f: all)
      {
        recuPrint(f,sublevel+1);
      }
    }
    
    
  }
  
  
}
