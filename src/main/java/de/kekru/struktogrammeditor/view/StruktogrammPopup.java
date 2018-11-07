package de.kekru.struktogrammeditor.view;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPopupMenu;
import javax.swing.JSeparator;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;

import de.kekru.struktogrammeditor.control.Struktogramm;
import de.kekru.struktogrammeditor.struktogrammelemente.Fallauswahl;
import de.kekru.struktogrammeditor.struktogrammelemente.StruktogrammElement;
import de.kekru.struktogrammeditor.struktogrammelemente.Verzweigung;

//http://download.oracle.com/javase/tutorial/uiswing/components/menu.html
//http://download.oracle.com/javase/tutorial/uiswing/examples/components/MenuDemoProject/src/components/MenuDemo.java

public class StruktogrammPopup extends JPopupMenu implements PopupMenuListener{

	private static final long serialVersionUID = -6394669590636692950L;
	private int typ;
	private StruktogrammElement element; //das Element, bei dem das PopupMen� ge�ffnet wurde
	private Struktogramm struktogramm;

	public StruktogrammPopup(StruktogrammElement element, Struktogramm struktogramm){
		super();

		addPopupMenuListener(this);

		this.element = element;
		this.struktogramm = struktogramm;

		typ = Struktogramm.strElementZuTypnummer(element);

		StrPopupUntermenue untermenue = unterMenueEinfuegen("Zoom");
		untermenue.einfuegen("Gr��er",8,-1);
		untermenue.einfuegen("Kleiner",9,-1);
		untermenue.add(new JSeparator());
		untermenue.einfuegen("Breiter",10,-1);
		untermenue.einfuegen("Schmaler",11,-1);
		untermenue.add(new JSeparator());
		untermenue.einfuegen("H�her",12,-1);
		untermenue.einfuegen("Weniger hoch",13,-1);
		add(new JSeparator());

		einfuegen("Text �ndern",0);//die Nummer ist die Id, welche fest legt, was beim Klick passieren soll (siehe actionPerformed(...))
		einfuegen("Kopieren",7);
		einfuegen("L�schen...",1);

		switch(typ){
		case Struktogramm.typVerzweigung: //Bei Verzweigung diesen Men�punkt hinzuf�gen:
			add(new JSeparator());
			einfuegen("Ja- und Neinseite vertauschen",2);
			break;

		case Struktogramm.typFallauswahl: //Bei Fallauswahl diese Men�punkte hinzuf�gen:
			add(new JSeparator());

			einfuegen("Neuen Fall einf�gen",3);

			String[] faelle = element.gibFaelle();

			for (int i=0; i < faelle.length; i++){

				untermenue = unterMenueEinfuegen("Fall: "+faelle[i]);//Untermen�s f�r jeden Fall

				if (i > 0)//alle au�er den Linkesten kann man nach links schieben
					untermenue.einfuegen("Fall \""+faelle[i]+"\" nach links verschieben", 4, i);


				if (i < faelle.length -1){//Sonst-Fall kann man nicht l�schen und nicht nach rechts verschieben
					untermenue.einfuegen("Fall \""+faelle[i]+"\" nach rechts verschieben", 5, i);
					untermenue.einfuegen("Fall \""+faelle[i]+"\" entfernen", 6, i);
				}

			}
			break;
		}

	}


	private void einfuegen(String text,int id){
		add(new StrPopupItem(text,id));
	}


	private StrPopupUntermenue unterMenueEinfuegen(String text){
		StrPopupUntermenue tmp = new StrPopupUntermenue(element,struktogramm,text);
		add(tmp);
		return tmp;
	}




	public void popupMenuWillBecomeVisible(PopupMenuEvent e){
		struktogramm.setzePopupmenuSichtbar(true);
	}

	public void popupMenuWillBecomeInvisible(PopupMenuEvent e){
		struktogramm.setzePopupmenuSichtbar(false);
	}

	public void popupMenuCanceled(PopupMenuEvent e){
		struktogramm.setzePopupmenuSichtbar(false);
	}









	private class StrPopupUntermenue extends JMenu{

		private static final long serialVersionUID = 1L;
		//StruktogrammElement element;
		//Struktogramm struktogramm;

		public StrPopupUntermenue(StruktogrammElement element, Struktogramm struktogramm, String text){
			super(text);
			//this.element = element;
			//this.struktogramm = struktogramm;
		}

		private void einfuegen(String text,int id, int fallnummer){
			add(new StrPopupItem(text,id,fallnummer));
		}
	}



	private class StrPopupItem extends JMenuItem implements ActionListener{

		private static final long serialVersionUID = 1L;
		private int id;
		private int fallnummer;

		public StrPopupItem(String text, int id){
			this(text,id,-1);
		}

		public StrPopupItem(String text, int id, int fallnummer){
			super(text);
			this.id = id;
			this.fallnummer = fallnummer;
			addActionListener(this);
		}


		public void actionPerformed(ActionEvent e){

			boolean mussSpeicherpunktSetzen = true;
			
			//passende Aktion t�tigen
			switch(id){
			case 0: struktogramm.elementBefuellen(element); //EingabeDialog �ffnen, hier drin wird schon ein R�ckg�ngigpunkt gesetzt
			mussSpeicherpunktSetzen = false;
			break;

			case 1: struktogramm.elementLoeschen(element, true); //dieses Element l�schen, hier drin wird schon ein R�ckg�ngigpunkt gesetzt
			mussSpeicherpunktSetzen = false;
			break;

			case 2: ((Verzweigung)element).seitenVertauschen();
			break;

			case 3: ((Fallauswahl)element).erstelleNeueSpalte();
			break;

			case 4: ((Fallauswahl)element).spalteVerschieben(true,fallnummer); //Spalte nach links verschieben
			break;

			case 5: ((Fallauswahl)element).spalteVerschieben(false,fallnummer); //Spalte nach rechts verschieben
			break;

			case 6: String fallname = ((Fallauswahl)element).gibFaelle()[fallnummer]; //Spalte bzw. Fall l�schen
			if (JOptionPane.showConfirmDialog(null, "Den Fall \""+fallname+"\" mit dessen Unterelementen l�schen?", "Fall entfernen", JOptionPane.YES_NO_OPTION) == 0){
				((Fallauswahl)element).entferneSpalte(fallnummer);
			}else{
				mussSpeicherpunktSetzen = false;
			}
			break;

			case 7: struktogramm.gibTabbedPane().gibGUI().gibAuswahlPanel().setzeKopiertesStrElement(struktogramm.xmlErstellen(element)); //Abbild des Elementes in Kopier-Box ablegen
			mussSpeicherpunktSetzen = false;
			break;

			case 8: //Gr��er (x-Richtung und y-Richtung)
				struktogramm.zoom(1, 1, element);
				break;
				
			case 9: //Kleiner(x-Richtung und y-Richtung)
				struktogramm.zoom(-1, -1, element);
				break;
				
			case 10: //Breiter (x-Richtung)
				struktogramm.zoom(1, 0, element);
				break;
				
			case 11: //Schmaler (x-Richtung)
				struktogramm.zoom(-1, 0, element);
				break;
				
			case 12: //H�her (y-Richtung)
				struktogramm.zoom(0, 1, element);
				break;
				
			case 13: //Weniger Hoch (y-Richtung)
				struktogramm.zoom(0, -1, element);
				break;

			}


			struktogramm.zeichenbereichAktualisieren();
			struktogramm.zeichne();

			if (mussSpeicherpunktSetzen){
				struktogramm.rueckgaengigPunktSetzen();
			}

		}
	}

}