package struktogrammelemente;

import java.awt.Graphics2D;

import other.JTextAreaEasy;
import view.CodeErzeuger;
import control.GlobalSettings;
import control.Struktogramm;

public class ForSchleife extends WhileSchleife { //erbt von WhileSchleife

   public ForSchleife(Graphics2D g){
      super(g);
      
      setzeText(GlobalSettings.gibElementBeschriftung(Struktogramm.typForSchleife));
   }
   
   
   @Override     //siehe DoUntilSchleife
   public void quellcodeGenerieren(int typ, int anzahlEingerueckt, int anzahlEinzuruecken, boolean alsKommentar, JTextAreaEasy textarea){
      String vorher = "";
      String nachher = "";


      switch(typ){
         case CodeErzeuger.typJava:
            vorher = "for("+co("kommentar")+co("text")+co("kommentarzu")+"){\n";
            nachher = "}\n";
            break;

         case CodeErzeuger.typDelphi:
            vorher = "for "+co("kommentar")+co("text")+co("kommentarzu")+" do \n"+einruecken("begin",anzahlEingerueckt)+"\n";
            nachher = "end;\n";
            break;
      }

      textarea.hinzufuegen(wandleZuAusgabe(vorher,typ,anzahlEingerueckt,alsKommentar));
      liste.quellcodeAllerUnterelementeGenerieren(typ,anzahlEingerueckt+anzahlEinzuruecken,anzahlEinzuruecken,alsKommentar,textarea);
      textarea.hinzufuegen(wandleZuAusgabe(nachher,typ,anzahlEingerueckt,alsKommentar));

   }
   
}