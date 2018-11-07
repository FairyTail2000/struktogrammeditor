package control;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.awt.dnd.DnDConstants;
import java.awt.dnd.DragGestureEvent;
import java.awt.dnd.DragGestureListener;
import java.awt.dnd.DragSource;
import java.awt.dnd.DragSourceDragEvent;
import java.awt.dnd.DragSourceDropEvent;
import java.awt.dnd.DragSourceEvent;
import java.awt.dnd.DragSourceListener;
import java.awt.dnd.DropTarget;
import java.awt.dnd.DropTargetDragEvent;
import java.awt.dnd.DropTargetDropEvent;
import java.awt.dnd.DropTargetEvent;
import java.awt.dnd.DropTargetListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.awt.image.BufferedImage;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;

import javax.imageio.ImageIO;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.JPanel;

import org.jdom.Document;
import org.jdom.Element;
import org.jdom.output.Format;
import org.jdom.output.XMLOutputter;

import struktogrammelemente.Anweisung;
import struktogrammelemente.Aufruf;
import struktogrammelemente.Aussprung;
import struktogrammelemente.DoUntilSchleife;
import struktogrammelemente.Endlosschleife;
import struktogrammelemente.Fallauswahl;
import struktogrammelemente.ForSchleife;
import struktogrammelemente.LeerElement;
import struktogrammelemente.Schleife;
import struktogrammelemente.StruktogrammElement;
import struktogrammelemente.StruktogrammElementListe;
import struktogrammelemente.Verzweigung;
import struktogrammelemente.WhileSchleife;
import view.EingabeDialog;
import view.StrFileFilter;
import view.StrTabbedPane;
import view.StruktogrammPopup;


public class Struktogramm extends JPanel implements MouseListener, MouseMotionListener, MouseWheelListener, DropTargetListener/*zum Empfangen von Drop*/, DragGestureListener, DragSourceListener /*letzten Beiden sind zum Ausl�sen von Drags*/{

	private static final long serialVersionUID = 8269048981647964473L;
	private StruktogrammElementListe liste; //Hauptliste, die alle weiteren Unterelemente hat
	private Graphics2D g; //Graphics des BufferedImage bild
	//private Graphics2D panelGraphics; //Graphics-Kontext des Struktogramms (es erbt von JPanel)
	private StruktogrammElement markiertesElement; //das mit der Maus markierte Element
	private BufferedImage bild; //hierauf zeichnen sich die StruktogrammElemente, anschlie�end wird alles auf das Struktogramm (also das JPanel) gezeichnet -> DoubleBuffering
	private int sperre = 0; //laufender int-Wert f�r entlasten der CPU (siehe auch n�chste Zeile und mausBewegt(...))
	private static final int sperreAktualisierung = 0;//10;//war gedacht, zum mindern der CPU-Last, weil MouseMoved sehr oft ausgel�st wurde; es wird nur alle sperreAktualisierung+1 Mal neu gezeichnet
	private boolean popupmenuSichtbar = false;
	//private JScrollPane scrollpane; //in diesem JScrollPane liegt das Struktogramm, scrollpane liegt wiederrum in einem JTabbedPane (siehe GUI)
	private StrTabbedPane tabbedpane; //f�r eine kennt-Beziehung mit dem JTabbedPane
	private Dimension dimGroesse; //Ausma�e des Struktogramms
	private static final int randLinks = 20; //Verschiebung der StruktogrammElemente nach rechts
	private static final int randOben = 20;  //Verschiebung der StruktogrammElemente nach unten
	private DragSource dragSource; //ben�tigt zum Ausl�sen eines Drag
	//private DropTarget dropTarget; //ben�tigt zum Empfangen eines Drop
	private StruktogrammPopup popup; //Popup-Kontextmen� bei Rechtsklick
	private StruktogrammElement dragZwischenlagerElement; //wenn ein Element aus dem bestehenden Struktogramm gezogen wird (Drag), wird es in dragZwischenlagerElement gespeichert
	private StruktogrammElementListe dragZwischenlagerListe; //die dragZwischenlagerElement �bergeordnete StruktogrammElementListe
	private Rectangle rectVorschau; //Bereich, wo das rote Vorschau-Rechteck erscheinen soll, wenn ein Element auf das Struktogramm gezogen wird; rectVorschau wird vom jeweiligen StruktogrammElement, dass sich an der aktuellen Mauspsosition befindet, gesetzt
	private String aktuellerSpeicherpfad; //der absolute Pfad, wo dieses Struktogramm gespeichert wurde, oder von wo es geladen wurde
	//private boolean ueberwacheResize = false; //speichert, ob das Struktogramm auf Gr��enver�nderungen der GUI reagieren soll; der Wert wird von der GUI gesetzt
	private ArrayList<Document> rueckgaengigListe; //Liste mit Document-Objekte, in denen jeweils ein komplettes Struktogramm gespeichert ist; nach jeder Ver�nderung wird ein neues Abbild des Struktogramms in die Liste abgelegt, f�r die R�ckg�ngig-Funktion
	private int posInRueckgaengigListe = 0; //aktueller Index, an welcher Stelle man sich in der R�ckg�ngig-Liste befindet; meist ist der Wert der letzte Index der R�ckg�ngig-Liste, au�er man hat auf R�ckg�ngig geklickt
	private int posInRueckgaengigListeWoZuletztGespeichert = -1; //Index, in der R�ckg�ngig-Liste bei dem zuletzt gespeichert wurde; wird ben�tigt, um das Sternchen (*) im JTabbedPane beim Zur�ckgehen in der R�ckg�ngig-Liste an der passenden Stelle auszublenden
	private Font fontStr = GlobalSettings.fontStandard;
	private Point[] pointMarkierungEckPunkte = null;

	//Konstanten die jedem StruktogrammElement einen int-Wert zuordnen
	public static final int typAnweisung = 0;
	public static final int typVerzweigung = 1;
	public static final int typFallauswahl = 2;
	public static final int typForSchleife = 3;
	public static final int typWhileSchleife = 4;
	public static final int typDoUntilSchleife = 5;
	public static final int typEndlosschleife = 6;
	public static final int typAussprung = 7;
	public static final int typAufruf = 8;
	public static final int typLeerElement = 9;
	
	private String struktogrammBeschreibung = "";

	public void setStruktogrammBeschreibung(String s){
		struktogrammBeschreibung = s;
	}
	
	public String getStruktogrammBeschreibung(){
		return struktogrammBeschreibung;
	}


	public Struktogramm(StrTabbedPane tabbedpane){
		super(true);

		setBackground(Color.white);

		this.tabbedpane = tabbedpane;

		setBounds(0,0,0,0);
		dimGroesse = getSize();


		aktuellerSpeicherpfad = "";

		liste = new StruktogrammElementListe(null);//StruktogrammElementListe erzeugen, ohne Graphics-Kontext, dieser wird erst in graphicsInitialisieren() gesetzt, weil das noch nicht fertig erzeugte Struktogramm noch keinen Graphics-Kontext hat
		liste.setzeBeschreibung("hauptliste");

		rueckgaengigListeInitialisieren();


		//Mouse Listener hinzuf�gen
		addMouseListener(this);
		addMouseMotionListener(this);


		if(GlobalSettings.isBeiMausradGroesseAendern()){
			addMouseWheelListener(this);
		}


		/*Drag & Drop aktivieren, siehe
        http://www.java2s.com/Code/Java/Swing-JFC/MakingaComponentDraggable.htm
        und
        http://www.java2s.com/Code/Java/Swing-JFC/PanelDropTarget.htm
		 */
		dragSource = new DragSource();
		dragSource.createDefaultDragGestureRecognizer(this, DnDConstants.ACTION_COPY_OR_MOVE, this);

		//dropTarget = 
		new DropTarget(this, DnDConstants.ACTION_COPY_OR_MOVE, this, true, null);



		rueckgaengigPunktSetzen(false);//R�ckg�ngigpunkt setzen, ohne den TabbedPane-Tab-Titel zu aktualisieren (Parameter: false), weil der Tab f�r dieses Struktogramm erst nach dem Erzeugen dieses Struktogramms erzeugt wird
	}


	public void mausradScrollEinOderAusschalten(boolean einschalten){
		if(einschalten){
			addMouseWheelListener(this);		
		}else{
			removeMouseWheelListener(this);
		}
	}

	public StruktogrammElementListe gibListe(){
		return liste;
	}

	public Graphics2D gibGraphics(){
		return g;
	}

	public StrTabbedPane gibTabbedPane(){
		return tabbedpane;
	}


	public void setzePopupmenuSichtbar(boolean neuerStatus){
		popupmenuSichtbar = neuerStatus;
	}


	//gibt die entsprechende Typnummer zu einen StruktogrammElement zur�ck
	public static int strElementZuTypnummer(StruktogrammElement str){

		if(str instanceof Verzweigung){//Wichtig: erst Verzweigung, dann Fallauswahl pr�fen, weil Verzweigung von Fallauswahl erbt und somit eine Verzweigung bei (str instanceof Fallauswahl) true ergibt, selbiges auch mit Anweisung und Aussprung/Aufruf
			return typVerzweigung;
		}else if(str instanceof Fallauswahl){
			return typFallauswahl;
		}else if(str instanceof ForSchleife){
			return typForSchleife;
		}else if(str instanceof WhileSchleife){
			return typWhileSchleife;
		}else if(str instanceof DoUntilSchleife){
			return typDoUntilSchleife;
		}else if(str instanceof Endlosschleife){
			return typEndlosschleife;
		}else if(str instanceof Aussprung){
			return typAussprung;
		}else if(str instanceof Aufruf){
			return typAufruf;
		}else if (str instanceof LeerElement){
			return typLeerElement;
		}else if (str instanceof Anweisung){
			return typAnweisung;
		}else{
			return -1;
		}
	}




	//erzeugt JScrollPane, legt dieses Struktogramm in das JScrollPane und gibt das JScrollPane zur�ck, damit es in das JTabbedPane gelegt werden kann (siehe StrTabbedPane.struktogrammHinzufuegen())
	//	public JScrollPane gibScrollPaneFuerContainer(){
	//		//		scrollpane = new JScrollPane(this);//http://www.dpunkt.de/java/Programmieren_mit_Java/Oberflaechenprogrammierung/14.html
	//		//		scrollpane.setBounds(getBounds());
	//		//
	//		//		scrollpane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
	//		//		scrollpane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS);
	//		//
	//		//		setBounds(scrollpane.getBounds());
	//		//
	//		//		scrollpane.addComponentListener(new ComponentListener() { //Listener zum Mitverfolgen von Gr��enver�nderungen durch den User
	//		//			public void componentResized(ComponentEvent e){
	//		//				if((g != null) && (ueberwacheResize)){//nur wenn die GUI ueberwacheResize true gesetzt hat, sonst wird es beim einf�gen neuer Elemente ausgef�hrt, wenn der Anzeigebereich verlassen wird, und beim Scrollpane gibt es Probleme
	//		//					zeichenbereichAktualisieren(); //wenn die Gr��e ge�ndert wurde, muss der Zeichenbereich aktualisiert werden, sonst wird das Struktogramm gestreckt oder verkleinert dargestellt
	//		//					zeichne();
	//		//					ueberwacheResize = false;
	//		//				}
	//		//			}
	//		//			public void componentMoved(ComponentEvent e){}
	//		//			public void componentShown(ComponentEvent e){}
	//		//			public void componentHidden(ComponentEvent e){}
	//		//		});
	//		//
	//		//		return scrollpane;
	//
	//
	//		return new JScrollPane(this);
	//
	//	}


	//Graphics-Kontext Panel (Struktogramm) wird gespeichert und ein BufferedImage wird erzeugt und dessen Graphics-Kontext an alle StruktogrammElemente weitergegeben
	public boolean graphicsInitialisieren(){
		//panelGraphics = (Graphics2D)getGraphics();//Panel Graphics-Kontext speichern

		//if((getWidth() > 0) || (getHeight() > 0)){
		if((dimGroesse.width > 0) || (dimGroesse.height > 0)){
			//bild = (BufferedImage)createImage(getWidth(),getHeight());//BufferedImage erzeugen
			bild = (BufferedImage)createImage(dimGroesse.width, dimGroesse.height);
			g = bild.createGraphics();//Graphics-Kontext in g speichern

			if(GlobalSettings.isKantenglaettungVerwenden()){
				g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
			}

			//g.setFont(new Font("serif", Font.PLAIN, 15)); //Schriftart f�r die StruktogrammElemente setzen
			g.setFont(fontStr);
			liste.graphicsAllerUnterlementeSetzen(g); //g an alle StruktogrammElemente weitergeben

			return true;
		}

		return false;
	}


	@Override
	public void paint (Graphics g){ //damit beim Scrollen gezeichnet wird, paint-Methode des JPanels �berschreiben und zeichne() hinzuf�gen
		super.paint(g);
		zeichne(g);
	}


	//	public void revalidate(){
	//		super.revalidate();
	//		if(liste != null){
	//			zeichenbereichAktualisieren();
	//		}
	//	}


	public void zeichne(){
		repaint();
	}

	//Das Struktogramm zeichnen
	public void zeichne(Graphics panelGraphics){

		if (g != null){
			//Zun�chst wird auf das BufferedImage bild mit dem Graphics-Kontext g gezeichnet

			//alten Inhalt mit einem wei�en ausgef�llten Rechteck �bermalen
			g.setColor(Color.white);
			g.fillRect(0, 0, getWidth(), getHeight());
			g.setColor(Color.black);
			
			if(!struktogrammBeschreibung.isEmpty()){
				Font f = g.getFont();
				g.setFont(new Font(f.getFamily(), f.getStyle(), 20));
				g.drawString(struktogrammBeschreibung, getXVerschiebungForCenteredText(struktogrammBeschreibung, dimGroesse.width, g), 35);
				g.setFont(f);
				g.drawRect(10,10,dimGroesse.width-21, dimGroesse.height -21);
				
			}

			//alle StruktogrammElemente zeichnen
			liste.alleZeichnen();


			if (rectVorschau != null){//wenn die Vorschau gezeichnet werden soll...
				g.setColor(Color.red);
				g.fillRect(rectVorschau.x,rectVorschau.y,rectVorschau.width,rectVorschau.height);//...rotes Rechteck zeichnen
			}


			if (dragZwischenlagerElement != null){//wenn gerade ein Element aus dem Struktogramm gezogen wird...
				Rectangle rectDragElement = dragZwischenlagerElement.gibRectangle();
				g.setColor(Color.blue);
				g.drawRect(rectDragElement.x,rectDragElement.y,rectDragElement.width,rectDragElement.height);//...blauen Rahmen zeichnen
				g.drawRect(rectDragElement.x+1,rectDragElement.y+1,rectDragElement.width-2,rectDragElement.height-2);//und noch einen Rahmen in dem Anderen zeichnen, damit die Umrandung breiter ist
			}


			if(pointMarkierungEckPunkte != null){
				//TODO Markierungsfunktion einbauen, Markierungs-Rechteck zeichnen geht (n�chsten beiden Zeilen benutzen)
				//g.setColor(Color.black);
				//g.drawRect(Math.min(pointMarkierungEckPunkte[0].x, pointMarkierungEckPunkte[1].x), Math.min(pointMarkierungEckPunkte[0].y, pointMarkierungEckPunkte[1].y), Math.abs(Math.min(pointMarkierungEckPunkte[0].x, dimGroesse.width -1) - Math.min(pointMarkierungEckPunkte[1].x, dimGroesse.width -1)), Math.abs(Math.min(pointMarkierungEckPunkte[0].y, dimGroesse.height -1) - Math.min(pointMarkierungEckPunkte[1].y, dimGroesse.height -1)));
			}



			//Point scrollweite = scrollpane.getViewport().getViewPosition();


			//panelGraphics.drawImage(bild,-scrollweite.x,-scrollweite.y,getWidth(),getHeight(),this);//auf Bild auf Panel zeichnen, dabei so verschieben, dass es mit den Scrollbalken passt
			panelGraphics.drawImage(bild, 0, 0, dimGroesse.width, dimGroesse.height, this);



		}else{ //g ist nicht gesetzt, also neu initialisieren
			if(graphicsInitialisieren()){
				zeichenbereichAktualisieren();
				zeichne();
			}

		}

		if (popupmenuSichtbar && (popup != null)){
			popup.repaint(); //Popup-Men� neu zeichnen, wenn es aktiv ist, damit es nicht vom Struktogramm �berzeichnet wird
		}
	}


	private int getXVerschiebungForCenteredText(String s, int breiteUntergrund, Graphics2D g) {
		int i = (g != null) ? (int) g.getFontMetrics().getStringBounds(s, g).getBounds().getWidth() : s.length() * 4;//http://www.tutorials.de/java/288641-textlaenge-pixel.html
		return (int) ((breiteUntergrund - i) / 2);
	}



	//	@Override //bevorzugte Gr��e soll die Gr��e des Panels sein (wichtig f�r das JScrollPane
	//	public Dimension getPreferredSize(){//http://www.java-forum.org/allgemeine-java-themen/95703-jscrollpane-jpanel-scrollen-nur-groesse-jpanels.html
	//		return dimGroesse;
	//	}



	//die Zeichenbereiche aller StruktogrammElemente werden gesetzt
	public void zeichenbereichAktualisieren(){


		//Point viewportPositionVorher = scrollpane.getViewport().getViewPosition(); //Abspeichern der aktuellen Position des gescrollten Bildes
		//scrollpane.getViewport().setViewPosition(new Point(0,0)); //zur�cksetzen der ViewPosition auf (0/0), sonst entstehen Probleme weiter unten und am Ende kann man nicht mehr das ganze Struktogramm durch Scrollen erreichen, weil es verschoben ist

		int randLinksNeu = randLinks + (!struktogrammBeschreibung.isEmpty() ? 20 : 0);
		int randObenNeu = randOben + (!struktogrammBeschreibung.isEmpty() ? 40 : 0);

		dimGroesse = liste.zeichenbereichAllerElementeAktualisieren(randLinksNeu,randObenNeu).getSize();//Zeichenbereich aller Unterelemente wird gesetzt (rekursiv), das erste hat seine linke obere Ecke bei (randLinks/randOben), zur�ckgegeben wird ein Rectangle, dessen Gr��e (getSize()) (Breite und Hoehe) in dimGroesse (Dimension-Objekt) gespeichert wird
		dimGroesse.width += randLinksNeu *2; //links und rechts Rand hinzuf�gen, sonst w�rde das Struktogramm bis ganz an den Rand des scrollpane-Ausschnittes gehen
		dimGroesse.height += randObenNeu *2; //oben und unten Rand hinzuf�gen


		//Dimension scrollpaneSichtbarerBereich = scrollpane.getViewport().getExtentSize();//Gr��e des sichtbaren Bereiches ermitteln, siehe: http://download.oracle.com/javase/1.4.2/docs/api/javax/swing/JViewport.html

		//Wenn der sichtbare Bereich Gr��er ist als das Struktogramm, wird die Struktogrammgr��e vergr��ert
		//		if (dimGroesse.width < scrollpaneSichtbarerBereich.width){
		//			dimGroesse.width = scrollpaneSichtbarerBereich.width;
		//		}
		//
		//		if (dimGroesse.height < scrollpaneSichtbarerBereich.height){
		//			dimGroesse.height = scrollpaneSichtbarerBereich.height;
		//		}

		setSize(dimGroesse);
		setPreferredSize(dimGroesse);
		graphicsInitialisieren(); //sonst ist alles sp�ter gestretcht, weil das BufferedImage noch immer die gleichen Ausma�e hat


		//scrollpane.getViewport().setViewPosition(viewportPositionVorher);//ViewPosition des JScrollPane auf die vorherige Position setzen


	}



	private void rueckgaengigListeInitialisieren(){//wird im Konstruktor und beim laden einer xml aus einer Datei aufgerufen
		rueckgaengigListe = new ArrayList<Document>();
	}


	public void rueckgaengigPunktSetzen(){
		rueckgaengigPunktSetzen(true);//true besagt, es soll der Titel des aktuellen Tab im JTabbedPane aktualisiert werden ((*) hinzuf�gen oder entfernen)
	}


	//ein R�ckg�ngig-Punkt wird gesetzt, damit der User sp�ter zu diesem zur�ckgehen kann
	public void rueckgaengigPunktSetzen(boolean tabbedpaneTitelAnpassen){

		for(int i = rueckgaengigListe.size() -1; i > posInRueckgaengigListe; i--){//alle Punkte, die nach dem aktuellen kommen, entfernen
			rueckgaengigListe.remove(i);

			if(i == posInRueckgaengigListeWoZuletztGespeichert){//wenn das Document gel�scht wird, bei dem das letzte Mal gespeichert wurde, wird posInRueckgaengigListeWoZuletztGespeichert zur�ckgesetzt
				posInRueckgaengigListeWoZuletztGespeichert = -1;
			}

		}


		rueckgaengigListe.add(xmlErstellen()); //der R�ckg�ngig-Liste wird ein xml-Document angehangen, welches das aktuelle Struktogramm darstellt
		posInRueckgaengigListe = rueckgaengigListe.size() -1; //aktuelle Position ist die Letzte in der Liste

		if(tabbedpaneTitelAnpassen)  //bei Bedarf Titel aktualisieren
			tabbedpane.titelDerAktuellenSeiteAlsBearbeitetOderAlsGespespeichertMarkieren(true);//true sagt, das Struktogramm wurde bearbeitet, also soll an den Titel ein (*) angehangen werden
	}



	//das Struktogramm vor der letzten �nderung wiederherstellen (R�ckg�ngig-Funktion)
	public void schrittZurueck(){
		if(posInRueckgaengigListe > 0){//wenn die aktuelle Position 0 ist, gibt es keine vorherigen R�ckg�ngig-Punkte
			posInRueckgaengigListe--;
			laden(rueckgaengigListe.get(posInRueckgaengigListe));//letzter R�ckg�ngig-Punkt wird geladen
		}
	}

	//R�ckg�ngig gemachtes wird widerrufen
	public void schrittNachVorne(){
		if(posInRueckgaengigListe < rueckgaengigListe.size() -1){//wenn die aktuelle Position kleiner als der letzte Index ist, gibt es Punkte zum nach vorne gehen
			posInRueckgaengigListe++;
			laden(rueckgaengigListe.get(posInRueckgaengigListe));//n�chster R�ckg�ngig-Punkt wird geladen
		}
	}



	//das StruktogrammElement an der Psosition (x/y) wurde mit der linken Maustaste angeklickt -> der EingabeDialog soll erscheinen
	private void elementAnPosBefuellen(int x, int y){
		elementBefuellen((StruktogrammElement)liste.gibElementAnPos(x,y,false));//elementBefuellen erh�lt das StruktogrammElement, welches sich an der Psosition (x/y) befindet
	}


	//EingabeDialog f�r das ausgew�hlte ElementAnzeigen und �nderungen ausf�hren
	public void elementBefuellen(StruktogrammElement element){

		if ((element != null) && !(element instanceof LeerElement)){//bei einem LeerElement, soll kein EingabeDialog erscheinen

			String[] text = eingabeBox(element); //EingabeDialog �ffnen

			if (text != null){//text ist null, wenn auf Abbrechen geklickt wurde

				element.setzeText(text); //text aus dem EingabeDialog setzen
				rueckgaengigPunktSetzen();
			}

			zeichenbereichAktualisieren();
			zeichne();
		}
	}




	//EingabeDialog f�r das ausgew�hlte StruktogrammElement anzeigen und Text zur�ckgeben, andere im EingabeDialog gemachten Angaben, werden von ihm selbst durchgef�hrt
	public String[] eingabeBox(StruktogrammElement tmp){

		EingabeDialog dialog = new EingabeDialog(tabbedpane.gibGUI(),"Inhalt �ndern",true, tmp);
		return dialog.gibTextArray(); //wenn Abbrechen gedr�ckt wurde, wird null zur�ckgegeben
	}






	/*markiert das Element, dass unter der Maus ist;
     das markierte Element wird gelb unterlegt gezeichnet;
     wenn vorschauFuerNeuesElement true ist, dann soll angezeigt werden, wo das neue Element,
     dass per Drag&Drop dahin gezogen wurde, eingef�gt werden w�rde, also wird
     rectVorschau gesetzt und in zeichne() ein rotes Rechteck gezeichnet*/
	private void vorschauMarkierungAnzeigen(int x, int y, boolean vorschauFuerNeuesElement){

		StruktogrammElement tmp = (StruktogrammElement)liste.gibElementAnPos(x,y,false);
		entmarkieren();//altes markiertes Element auf nicht markiert setzen

		if (tmp != null){


			markiertesElement = tmp;
			tmp.setzeMarkiert(true);//dieses Element wird jetzt gelb unterlegt gezeichnet

			if (vorschauFuerNeuesElement){
				rectVorschau = tmp.gibVorschauRect(new Point(x,y));//das Element unter der Maus gibt den Bereich der roten Vorschaumarkierung an
			}
		}
	}


	//das aktuell markierte Element wird auf nicht markiert gesetzt und rectVorschau f�r das rote Rechteck wird auf null gesetzt, es soll also nicht mehr gezeichnet werden
	private void entmarkieren(){
		if (markiertesElement != null){
			markiertesElement.setzeMarkiert(false);
		}

		rectVorschau = null;
	}




	//ein neues StruktogrammElement wird anhand der Typnummer erzeugt und zur�ckgegeben
	public StruktogrammElement neuesStruktogrammElement(int typ){
		//GUI gui = gibTabbedPane().gibGUI();  

		switch(typ){
		case 0: return new Anweisung(g);
		case 1: return new Verzweigung(g);
		case 2: return new Fallauswahl(g);
		case 3: return new ForSchleife(g);
		case 4: return new WhileSchleife(g);
		case 5: return new DoUntilSchleife(g);
		case 6: return new Endlosschleife(g);
		case 7: return new Aussprung(g);
		case 8: return new Aufruf(g);
		case 9: return new LeerElement(g);
		default: return null;
		}
	}


	/**
	 * Wird in KeyReleased in Controlling genutzt
	 */
	public void neuesElementAnAktuellerStelleEinfuegen(int typ){
		Point p = getMousePosition();
		gezogenesElementEinfuegen(p.x, p.y, typ);
	}

	/**
	 * Wird in KeyReleased in Controlling genutzt
	 */
	public void elementAnAktuellerStelleLoeschen(){
		Point p = getMousePosition();
		StruktogrammElement element = (StruktogrammElement)liste.gibElementAnPos(p.x, p.y, false);

		if(element != null){
			elementLoeschen(element, false);
		}
	}

	/**
	 * Wird in KeyReleased in Controlling genutzt
	 */
	public void zoomAktuellesElement(boolean groesser){
		Point p = getMousePosition();		
		if(p != null){

			StruktogrammElement element = (StruktogrammElement)liste.gibElementAnPos(p.x, p.y, false);

			if(element != null){
				zoom(groesser ? 1 : -1, groesser ? 1 : -1, element);
			}
		}
	}


	public StruktogrammElement getElementUnterMaus(){
		Point p = getMousePosition();

		if(p != null){
			return (StruktogrammElement)liste.gibElementAnPos(p.x, p.y, false);
		}

		return null;
	}

	//ein Drop wurde regstriert, er kam vom Auswahlpanel und es soll jetzt ein neues Element an der Position (x/y) eingef�gt werden
	private void gezogenesElementEinfuegen(int x, int y, int typ){
		StruktogrammElement neues = neuesStruktogrammElement(typ);

		if (neues != null){
			elementEinfuegen(x,y,neues,null);
		}
	}


	//das Element neues wird soll an die Position (x/y) eingef�gt werden
	private void elementEinfuegen(int x, int y, StruktogrammElement neues, StruktogrammElementListe listeNeue){

		StruktogrammElementListe listeZumEinfuegen = (StruktogrammElementListe)liste.gibElementAnPos(x,y,true);//ermitteln der Liste (Parameter ist true), die das StruktogrammElement hat, welches an der Position (x/y) ist

		if (listeZumEinfuegen != null){//wenn (x/y) au�erhalb des Struktogramms ist, ist listeZumEinfuegen null
			StruktogrammElement tmp = (StruktogrammElement)listeZumEinfuegen.gibElementAnPos(x,y,false);//das StruktogrammElement an der Position (x/y) ermitteln


			boolean oberhalbEinfuegen = false;

			if (tmp != null){
				oberhalbEinfuegen = tmp.neuesElementMussOberhalbPlatziertWerden(y);//das Element an der Position (x/y) wird gefragt, ob bei der �bergebenen y-Koordinate ein neues Element ober- oder unterhalb dieses Elementes eingef�gt werden muss
			}



			if (neues != null || listeNeue != null){

				if(neues != null){
					listeZumEinfuegen.hinzufuegen(neues,tmp,oberhalbEinfuegen); //das neue StruktogrammElement wird in die ermittelte Liste eingef�gt und zwar hinter oder vor tmp, je nachdem was in oberhalbEinfuegen steht
				}

				if(listeNeue != null){
					listeZumEinfuegen.hinzufuegen(listeNeue,tmp,oberhalbEinfuegen);
				}

				rectVorschau = null; //die rote Vorschau wird beendet

				zeichenbereichAktualisieren(); //da etwas in der Struktur des Struktogramms ver�ndert wurde, muss der Zeichenbereich aktualisiert werden...
				zeichne();
				rueckgaengigPunktSetzen(); //... und ein R�ckg�ngig-Punkt gesetzt werden
			}

		}


	}



	public StruktogrammElement gibZwischenlagerElement(){
		return dragZwischenlagerElement;
	}


	/*es wurde auf der Kopier-Box des AuswahlPanel ein Drag ausgel�st und der Drop wurde an der Position (x/y) empfangen,
     nun soll das Element, was in der Kopier-Box als Document gespeichert war, in das Struktogramm eingef�gt werden*/
	private void elementAusKopierFeldEinfuegen(int x, int y){
		XMLLeser xmlLeser = new XMLLeser();
		StruktogrammElementListe neue = xmlLeser.erstelleStruktogrammElementListe(tabbedpane.gibGUI().gibAuswahlPanel().gibKopiertesStrElement(),this);//der XMLLeser erstellt aus dem im AuswahlPanel gespeicherten Document ein StruktogrammElement mit eventuellen Unterelementen

		if(neue != null){
			elementEinfuegen(x,y,null,neue); //das erstellte StruktogrammElement wird eingef�gt
		}
	}



	public void elementAusKopierFeldEinfuegenAnMausPos(){
		Point p = getMousePosition();
		if(p != null){
			elementAusKopierFeldEinfuegen(p.x, p.y);
		}
	}



	//ein vorhandenes Element wurde per Drag & Drop an eine andere Position gezogen
	private void elementAusZwischenlagerEinfuegen(int x, int y){
		StruktogrammElement tmp = (StruktogrammElement)liste.gibElementAnPos(x,y,false);

		/*wenn an dieser Position ein Element ist, �ber oder unter dem das gezogene Element eingef�gt werden kann und
        wenn das gezogene Element nicht auf sich selber gezogen wurde und
        wenn das gezogene Element nicht auf eines seiner Unterelemente gezogen wurde,
        dann wird das gezogene Element an der entsprechenden Stelle eingef�gt*/
		if((tmp != null) && (tmp != dragZwischenlagerElement) && !dragZwischenlagerElement.istUnterelement(tmp)){

			elementAusZwischenlagerGanzEntfernen(); //von der alten Liste entfernen
			elementEinfuegen(x,y,dragZwischenlagerElement,null); //an die neue Position einf�gen
		}

	}


	//das Element im Zwischenlager (es ist also im Struktogramm und wurde gezogen) wird aus seiner alten Liste entfernt
	public void elementAusZwischenlagerGanzEntfernen(){
		dragZwischenlagerListe.entfernen(dragZwischenlagerElement);
	}





	//es wurde im Popup-Men� auf "L�schen..." geklickt und es soll das �bergebene StruktogrammElement gel�scht werden
	public void elementLoeschen(StruktogrammElement zuLoeschen, boolean vorherFragen){
		String frage;

		if ((zuLoeschen instanceof Schleife) || (zuLoeschen instanceof Fallauswahl)){//Schleifen, Fallauswahl und Verzweigung (Verzweigung ist eine Fallauswahl) haben Unterelemente, also angepasste Frage stellen
			frage = "Dieses Element und dessen Unterelemente entfernen?";
		}else{
			frage = "Dieses Element entfernen?";
		}

		//Zun�chst nachfragen, ob wirklich gel�scht werden soll
		if (!vorherFragen || JOptionPane.showConfirmDialog(null, frage, "L�schen", JOptionPane.YES_NO_OPTION) == 0){//0 hei�t ja, 1 hei�t nein wurde gedr�ckt

			StruktogrammElementListe tmp = liste.gibListeDieDasElementHat(zuLoeschen); //Liste, die das zu l�schende Element hat wird gesucht
			if (tmp != null){
				tmp.entfernen(zuLoeschen); //das Element wird entfernt
			}

			zeichenbereichAktualisieren();
			zeichne();
			rueckgaengigPunktSetzen();
		}
	}



	//Rechtsklick an der Position (x/y) wurde registriert, also Popup-Men� anzeigen
	private void popupMenueZeigen(int x, int y){
		StruktogrammElement tmp = (StruktogrammElement)liste.gibElementAnPos(x,y,false);

		if ((tmp != null)&&!(tmp instanceof LeerElement)){//LeerElemente brauchen kein Popup-Men�
			popup = new StruktogrammPopup(tmp,this); //erzeugen des Popup-Men�s; das Element, das unter der Maus ist (tmp), wird �bergeben
			popup.show(this,x,y);
		}
	}




	public String gibAktuellenSpeicherpfad(){
		return aktuellerSpeicherpfad;
	}


	private void setzeAktuellerSpeicherpfad(String pfad){
		aktuellerSpeicherpfad = pfad;
		tabbedpane.titelDerAktuellenSeiteSetzen(aktuellerSpeicherpfad.substring(pfad.lastIndexOf(File.separatorChar)+1));//der Titel des JTabbedPane wird gesetzt, dazu wird aus dem aktuellen Pfad der letzte \ oder / gesucht und der String danach �bergeben, es soll also nur der Dateiname, ohne Pfad im Titel des JTabbedPane erscheinen
	}


	private String extrahiereExtension(String pfad){//http://www.roseindia.net/java/string-examples/java-display-file.shtml
		return pfad.substring(pfad.lastIndexOf(".")+1);//Substring ab dem Zeichen nach dem letzten Punkt bis zum Ende
	}

	
	public BufferedImage generateImage(boolean mitRand){
		entmarkieren(); //damit keine farbigen Unterlegungen mehr da sind, entmarkieren ...
		zeichne();      //... und neu zeichnen
		return bild.getSubimage(mitRand ? 0 : randLinks, mitRand ? 0 : randOben, liste.gibBreite() + (mitRand ? 2*randLinks : 1), liste.gibHoehe() + (mitRand ? 2*randOben : 1)); //es wird der Teil des BufferedImage extrahiert, auf dem das Struktogramm ist mit ein paar Pixeln Rand an allen Seiten
	}

	//das aktuelle Struktogramm soll als Bilddatei abgespeichert werden
	public String alsBilddateiSpeichern(String voreingestellterPfad){//voreingestellterPfad ist der Pfad, bei dem der JFileChooser starten soll

		String pfad = saveFileChooser(new int[] {4,5,6,7,3}, voreingestellterPfad);//ein Speicherpfad wird mit einen JFileChooser durch den User ausgew�hlt; new int[] {4,5,6,7,3} gibt an, dass dabei die Dateifilter 4,5,6,7 und 3 (in dieser Reihenfolge) genutzt werden sollen (siehe StrFileFilter)

		if(!pfad.equals("")){//pfad ist "", wenn der User auf Abbrechen geklickt hat

			BufferedImage ausgabeBild = generateImage(false);

			//ausgabeBild mit dem ausgew�hlten Pfad abspeichert
			try{ //http://www.spotlight-wissen.de/archiv/message/476162.html
				BufferedOutputStream oStream = new BufferedOutputStream(new FileOutputStream(new File(pfad)));
				ImageIO.write(ausgabeBild, extrahiereExtension(pfad), oStream);
				oStream.close();
			}catch(IOException e1){

			}catch(Exception e2){

			}

		}

		return pfad; //zum sp�teren Speichern wird der Pfad zur�ckgeben an die GUI; beim erneuten Abspeichern eines Bildes, wird dann der Ordner in dem das aktuelle Bild ist, als Startordner f�r den JFileChooser verwendet
	}



	/*Abspeichern des Struktogramms als xml-Datei
     ist neuenSpeicherpfadAuswaehlenLassen true oder wenn die Datei noch nicht gespeichert worden ist, bzw. nicht aus einer Datei ge�ffnet wurde,
     dann wird der User zun�chst gefragt, wo gespeichert werden soll*/
	public String speichern(boolean neunenSpeicherpfadAuswaehlenLassen, String voreingestellterPfad){//voreingestellterPfad ist der Pfad, wo der JFileChooser starten soll, wenn aktuellerSpeicherpfad noch "" ist
		if (neunenSpeicherpfadAuswaehlenLassen || aktuellerSpeicherpfad.equals("")){
			xmlSpeichern(voreingestellterPfad); //es ist noch keine Datei gespeichert oder geladen worden, also nachfragen, wo gespeichert werden soll
		}else{
			xmlAbspeichernOhneFileChooser(aktuellerSpeicherpfad);//ohne Nachfragen abspeichern
		}

		return aktuellerSpeicherpfad; //zum sp�teren Speichern wird der Pfad zur�ckgeben an die GUI; beim erneuten Abspeichern eines Struktogrammes, wird dann der Ordner in dem das aktuelle Struktogramm ist, als Startordner f�r den JFileChooser verwendet
	}




	//die Datei, die dem �bergebenen Pfad zugeordnet ist, wird geladen
	public void laden(String pfad){
		XMLLeser tmp = new XMLLeser();
		tmp.ladeXLM(pfad,this); //alle in der Datei gespeicherten StruktogrammElemente werden hier erzeugt und in das Struktogramm integriert
		setzeAktuellerSpeicherpfad(pfad);
		rueckgaengigListeInitialisieren(); //alle alten R�ckg�ngig-Punkte entfernen
		rueckgaengigPunktSetzen(false);
		posInRueckgaengigListeWoZuletztGespeichert = 0; //der erste Speicherpunkt kommt von einer xml-Datei, also diese Position als gespeichert markieren
		zeichenbereichAktualisieren();
		zeichne();
	}


	//Struktogramm-Daten werden anhand des �bergebenen Document erzeugt (f�r die R�ckg�ngig-Funktion)
	private void laden(Document document){//R�ckg�ngig-Punkt muss hier nicht gesetzt werden, weil diese Methode nur mit Document Objekten aus der rueckgaengigListe angewandt wird
		XMLLeser tmp = new XMLLeser();
		tmp.ladeXLM(document,this);
		tabbedpane.titelDerAktuellenSeiteAlsBearbeitetOderAlsGespespeichertMarkieren(posInRueckgaengigListeWoZuletztGespeichert != posInRueckgaengigListe);//als bearbeitet markieren ("*" anh�ngen) (wenn posInRueckgaengigListeWoZuletztGespeichert != posInRueckgaengigListe) oder als abgespeichert (wenn posInRueckgaengigListeWoZuletztGespeichert == posInRueckgaengigListe)

		zeichenbereichAktualisieren();
		zeichne();
	}



	/*zeigt einen �ffnen-Dialog mit dem Startordner voreingestellterOrdnerpfad und
     gibt den vom User ausgew�hlten Pfad zur�ck;
     hat er Abbrechen angeklickt, wird "" zur�ckgegeben*/
	public static String oeffnenDialog(String voreingestellterOrdnerpfad, Component parentComponent){
		JFileChooser chooser = new JFileChooser();
		chooser.setFileFilter(new StrFileFilter(StrFileFilter.filterAlleSpeicherdateien));//StrFileFilter f�r .strk und .xml Dateien (siehe StrFileFilter)

		if(!voreingestellterOrdnerpfad.equals("")){
			chooser.setCurrentDirectory(new File(voreingestellterOrdnerpfad));//Startordner setzen
		}

		int returnVal = chooser.showOpenDialog(parentComponent);//�ffnen-Dialog anzeigen
		String pfad = "";

		if(returnVal == JFileChooser.APPROVE_OPTION) {//wenn �ffnen angeklickt wurde

			pfad = chooser.getSelectedFile().getAbsolutePath();
		}

		return pfad;
	}



	//aus diesem Struktogramm wird ein Document erstellt, das die xml-Daten enth�lt
	//http://www.ibm.com/developerworks/java/library/j-jdom/
	public Document xmlErstellen(){
		Element element = new Element("struktogramm");		

		element.setAttribute("fontfamily",XMLLeser.encodeS(fontStr.getFamily())).setAttribute("fontstyle",""+fontStr.getStyle()).setAttribute("fontsize",""+fontStr.getSize())
		.setAttribute("caption", XMLLeser.encodeS(struktogrammBeschreibung));


		Document myDocument = new Document(element);

		liste.schreibeXMLDatenAllerUnterElemente(element);

		return myDocument;
	}


	/*Aus dem �bergebenen StruktogrammElement und dessen Unterelementen wird ein Document erstellt,
     zum Kopieren und sp�terem einfuegen*/
	public Document xmlErstellen(StruktogrammElement wurzelElement){
		if(!(wurzelElement instanceof LeerElement)){//LeerElement kann nicht kopiert werden

			Element element = new Element("struktogrammelement");
			Document myDocument = new Document(element);

			wurzelElement.schreibeXMLDaten(element);

			return myDocument;
		}else{
			return null;
		}
	}


	//Speicherndialog wird aufgerufen, mit den angegebenen StrFileFilter-Nummern und dem angegebenen Ordnerpfad
	private String saveFileChooser(int[] struktogrammFilterNummern, String voreingestellterPfad){
		JFileChooser chooser = new JFileChooser();

		for (int i=0; i < struktogrammFilterNummern.length; i++){
			chooser.addChoosableFileFilter(new StrFileFilter(struktogrammFilterNummern[i]));//FileFilter hinzuf�gen
		}


		if(!voreingestellterPfad.equals("")){
			chooser.setCurrentDirectory(new File(voreingestellterPfad));//Startordner setzen
		}

		int returnVal = chooser.showSaveDialog(tabbedpane.gibGUI());//Speicherndialog anzeigen
		String pfad = "";

		if(returnVal == JFileChooser.APPROVE_OPTION){//wenn Speichern ausgew�hlt wurde

			pfad = chooser.getSelectedFile().getAbsolutePath();

			if(chooser.getFileFilter() instanceof StrFileFilter){//der erste Filter ist "Alle Dateien" und automatisch vorhanden und somit kein StrFileFilter, also erst pr�fen, ob der vom User gew�hlte Filter ein StrFileFilter ist
				pfad = ((StrFileFilter)chooser.getFileFilter()).erweiterungBeiBedarfAnhaengen(pfad);//Dateierweiterung bei Bedarf anh�ngen
			}

			if((new File(pfad)).exists()){ //wenn die ausgew�hlte Datei bereits existiert, erst nachfragen, ob diese �berschrieben werden soll
				Object[] options = {"Ja", "Nein"}; //http://download.oracle.com/javase/1.4.2/docs/api/javax/swing/JOptionPane.html
				if (0 != JOptionPane.showOptionDialog(null, "Die Datei "+pfad+" existiert bereits. \n Wirklich �berschreiben?", "Datei existiert", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE, null, options, options[1])){
					pfad = ""; //es wurde nicht ja gedr�ckt
				}
			}

		}

		return pfad; //der vom User gew�hlte Pfad wird zur�ckgegeben, wenn er auf Abbrechen, oder bei der Frage auf Nein geklickt hat, wird "" zur�ckgegeben
	}


	//Abspeichern als xml mit JFileChooser
	private void xmlSpeichern(String voreingestellterPfad){
		String pfad;

		if(!aktuellerSpeicherpfad.equals("")){//wenn die Datei schon gespeichert oder geladen wurde...
			pfad = aktuellerSpeicherpfad;//...wird dieser Pfad als Startordner f�r den JFileChooser genutzt...
		}else{
			pfad = voreingestellterPfad;//...sonst voreingestellterPfad (kommt von der GUI)
		}

		pfad = saveFileChooser(new int[] {1,2,0}, pfad);//Speicherdialog mit den StrFileFiltern 1,2 und 0

		if(!pfad.equals("")){//wenn nicht auf Abbrechen geklickt wurde...
			setzeAktuellerSpeicherpfad(pfad);
			xmlAbspeichernOhneFileChooser(pfad);//...wird gespeichert
		}
	}


	private void xmlAbspeichernOhneFileChooser(String pfad){
		Document myDocument = xmlErstellen(); //aus dem Struktogramm wird ein Document erstellt

		//myDocument wird mit einem XMLOutputter und FileWriter als xml-Datei gespeichert, siehe http://www.ibm.com/developerworks/java/library/j-jdom/
		XMLOutputter outputter;
		try{
			outputter = new XMLOutputter();
			outputter.setFormat(Format.getPrettyFormat());

			FileWriter writer = new FileWriter(pfad);
			outputter.output(myDocument, writer);
			writer.close();

			tabbedpane.titelDerAktuellenSeiteAlsBearbeitetOderAlsGespespeichertMarkieren(false);
			posInRueckgaengigListeWoZuletztGespeichert = posInRueckgaengigListe; //festhalten, welcher Zustand zuletzt gespeichert wurde
		}catch(java.io.IOException e){
			e.printStackTrace();
		}
	}







	/*Maus wurde bewegt, auf die Position (x/y), das Element an dieser Position
     wird gelb unterlegt und wenn vorschauFuerNeuesElement true ist,
     soll die rote Vorschaumarkierung angezeigt werden*/
	private void mausBewegt(int x, int y, boolean vorschauFuerNeuesElement){
		if (sperre == 0){//nur alle sperreAktualisierung Mal soll neu gezeichnet werden, um die CPU Last etwas zu mildern
			vorschauMarkierungAnzeigen(x,y,vorschauFuerNeuesElement);
			zeichne();
			sperre = sperreAktualisierung;
		}else{

			sperre--;
		}
	}




	public void mouseMoved(MouseEvent e){
		mausBewegt(e.getX(),e.getY(),false);
	}

	public void mouseDragged(MouseEvent e){
		Point p = e.getPoint();	

		if(pointMarkierungEckPunkte != null || liste.gibElementAnPos(p.x, p.y, false) == null){
			//Maus ist nicht �ber einem StruktogrammElement, oder einer StruktogrammElementListe

			if(pointMarkierungEckPunkte == null){
				pointMarkierungEckPunkte = new Point[2];
				pointMarkierungEckPunkte[0] = pointMarkierungEckPunkte[1] = p;
			}else{
				pointMarkierungEckPunkte[1] = p;
			}
		}

		mausBewegt(e.getX(),e.getY(),false);
	}

	public void mouseExited(MouseEvent e){

	}

	public void mouseEntered(MouseEvent e){

	}

	public void mouseReleased(MouseEvent e){

	}

	public void mousePressed(MouseEvent e){

	}

	public void mouseClicked(MouseEvent e){
		switch(e.getModifiers()){

		case MouseEvent.BUTTON1_MASK: //linke Maustaste
			elementAnPosBefuellen(e.getX(),e.getY()); //EingabeDialog �ffnen
			pointMarkierungEckPunkte = null; //Markierungsrechteck entfernen
			zeichne();
			break;

		case MouseEvent.BUTTON3_MASK: //rechte Maustaste
			popupMenueZeigen(e.getX(),e.getY()); //Popup-Men� zeigen
			break;
		}
	}








	//Methoden Drag ausgel�st
	//http://www.java2s.com/Code/Java/Swing-JFC/MakingaComponentDraggable.htm
	public void dragGestureRecognized(DragGestureEvent evt) {//ein Element aus dem Struktogramm wird weggezogen
		Point mausPos = bildschirmKoordZuStruktogrammKoord(evt.getDragOrigin());

		dragZwischenlagerListe = (StruktogrammElementListe)liste.gibElementAnPos(mausPos.x,mausPos.y,true);//Liste in der das gezogene Element ist wird ermittelt

		if (dragZwischenlagerListe != null){

			dragZwischenlagerElement = (StruktogrammElement)dragZwischenlagerListe.gibElementAnPos(mausPos.x,mausPos.y,false);//das gezogene Element wird gespeichert

			if ((dragZwischenlagerElement != null) && !(dragZwischenlagerElement instanceof LeerElement)){//wenn das Element nicht null ist und kein LeerElement ist...
				Transferable t = new StringSelection("z");//z f�r Element aus dem Zwischenlager

				dragSource.startDrag(evt, DragSource.DefaultCopyDrop, t, this);//...wird ein Drag ausgel�st

			}else{
				dragZwischenlagerElement = null; //LeerElement muss rausgenommen werden, sonst wird es blau umrahmt
			}

		}
	}

	public void dragEnter(DragSourceDragEvent evt){

	}

	public void dragOver(DragSourceDragEvent evt){

	}

	public void dragExit(DragSourceEvent evt){

	}

	public void dropActionChanged(DragSourceDragEvent evt){

	}

	//Drag & Drop ist vollst�ndig beendet und Drop wurde bereits bearbeitet...
	public void dragDropEnd(DragSourceDropEvent evt){
		dragZwischenlagerElement = null; //...also Zwischenlager leeren
		zeichne();
	}







	//DropTargetListener
	//Drop empfangen: http://www.java2s.com/Code/Java/Swing-JFC/PanelDropTarget.htm
	public void drop(DropTargetDropEvent event){

		try{
			event.acceptDrop(event.getSourceActions());

			Transferable tr = event.getTransferable();
			String dragTyp = (String)tr.getTransferData(tr.getTransferDataFlavors()[0]);

			Point mausPos;

			switch(dragTyp.charAt(0)){
			case 'n': //ein Element wurde aus dem AuswahlPanel gezogen
				int typ = Integer.parseInt(""+dragTyp.charAt(1));//typ gibt an welches Element erzeugt werden soll

				mausPos = bildschirmKoordZuStruktogrammKoord(event.getLocation());
				gezogenesElementEinfuegen(mausPos.x,mausPos.y,typ);
				break;

			case 'z': //aus den Struktogramm wurde ein Element gezogen und soll jetzt an einer anderen Stelle eingef�gt werden
				mausPos = bildschirmKoordZuStruktogrammKoord(event.getLocation());
				elementAusZwischenlagerEinfuegen(mausPos.x,mausPos.y);
				break;

			case 'k': //Aus dem Kopier-Label des AuswahlPanel wurde herausgezogen, und es soll jetzt hier eingef�gt werden
				mausPos = bildschirmKoordZuStruktogrammKoord(event.getLocation());
				elementAusKopierFeldEinfuegen(mausPos.x,mausPos.y);
				break;
			}

			event.dropComplete(true);
		}catch (Exception e){
			e.printStackTrace();
			event.rejectDrop();
		}
	}

	public void dragExit(DropTargetEvent evt){
		entmarkieren();
		zeichne();
	}

	public void dropActionChanged(DropTargetDragEvent evt){

	}

	public void dragEnter(DropTargetDragEvent evt){

	}

	public void dragOver(DropTargetDragEvent evt){
		Point mausPos = bildschirmKoordZuStruktogrammKoord(evt.getLocation());
		mausBewegt(mausPos.x,mausPos.y,true);//ein Element wird �ber das Struktogramm gezogen, also gelbe Unterlegung und rote Vorschau zeigen
	}



	//die Drag & Drop Methoden liefern Mauskoordinaten f�r den ganzen Bildschirm, hier werden sie zu Koordinaten des Struktogramms konvertiert
	//http://www.tutego.de/java/articles/Absolute-Koordinaten-Swing-Element.html
	public Point bildschirmKoordZuStruktogrammKoord(Point bildschirmKoord){
		Point scrollpanePoint = getParent().getLocation();
		return new Point(bildschirmKoord.x - scrollpanePoint.x, bildschirmKoord.y - scrollpanePoint.y);
	}




	//	public void setzeUeberwacheResize(boolean ueberwacheResize){
	//		this.ueberwacheResize = ueberwacheResize;
	//
	//		if(ueberwacheResize){
	//			//scrollpane.getViewport().setViewPosition(new Point(0,0));//Scroll nach oben setzen, sonst gibt es Scrollpane-Probleme beim Verkleinern
	//		}
	//	}



	@Override
	public void mouseWheelMoved(MouseWheelEvent e) {
		StruktogrammElement tmp = (StruktogrammElement)liste.gibElementAnPos(e.getX(), e.getY(), false);

		if(tmp != null){

			if(e.getWheelRotation() < 0){
				//Vergr��ern
				zoom(1,1,tmp);
			}else{
				//Verkleinern
				zoom(-1,-1,tmp);
			}			
		}
	}

	/**
	 * F�hrt das Vergr��ern oder Verkleinern eines Elementes durch.
	 * @param xMinusEinsNullOderEins F�r x-Richtung: 1 f�r Vergr��erung, 0 f�r keine Ver�nderung und -1 f�r Verkleinerung.
	 * @param yMinusEinsNullOderEins F�r y-Richtung: 1 f�r Vergr��erung, 0 f�r keine Ver�nderung und -1 f�r Verkleinerung.
	 * @param tmp Das StruktogrammElement, dessen Gr��e ge�ndert werden soll.
	 */
	public void zoom(int xMinusEinsNullOderEins, int yMinusEinsNullOderEins, StruktogrammElement tmp){
		tmp.zoomX(GlobalSettings.getXZoomProSchritt() * xMinusEinsNullOderEins);
		tmp.zoomY(GlobalSettings.getYZoomProSchritt() * yMinusEinsNullOderEins);

		zeichenbereichAktualisieren();
		zeichne();
		tabbedpane.titelDerAktuellenSeiteAlsBearbeitetOderAlsGespespeichertMarkieren(true);
	}


	public void zoomsZuruecksetzen(){
		rueckgaengigPunktSetzen(true);

		liste.zoomsAllerElementeZuruecksetzen();
		zeichenbereichAktualisieren();
		zeichne();

		rueckgaengigPunktSetzen(false);
	}


	public Font getFontStr() {
		return fontStr;
	}


	public void setFontStr(Font fontStr) {
		this.fontStr = fontStr;
	}


}