import java.util.Arrays;
import java.util.HashMap;
import java.util.Scanner;
import java.util.Random;

public class stranicenje {
	
	static int N;
	static int M;
	static int t;
	static int prviUlazak = 1;
	static int slobodanOkvir = 0;
	
	static HashMap<Integer, Character[]> okvir = new HashMap<>(); //simulirani radni spremnik od M okvira velicine 64 okteta
    static HashMap<Integer, HashMap<Integer, Character[]>> disk = new HashMap<>(); // simulirani disk koji sluzi za pohranu sadrzaja stranica
	static HashMap<Integer, Short[]> tablicePrevodenja = new HashMap<>(); // tablica prevodenja za svaki od N procesa
	

	public static void main(String[] args) throws InterruptedException {
		Scanner sc = new Scanner(System.in);
		
		System.out.print("Upisite N - broj procesa: ");
		N = sc.nextInt();
		
		System.out.print("Upisite M - broj okvira: ");
		M = sc.nextInt();
		
		// inicijaliziranje svih struktura
		HashMap<Integer, Character[]> mapa2 = new HashMap<>();
		for(int i = 0; i < N; i++) {
			Integer iI = Integer.valueOf(i);
			for(int j = 0; j < 16; j++) {
				mapa2.put(j, new Character[64]);
				disk.put(iI, mapa2);
				for(int k = 0; k < 64; k++) {
					disk.get(iI).get(j)[k] = 0;
				}	
			}
		}
		
		for(int i = 0; i < M; i++) {
			Integer iI = Integer.valueOf(i);
			okvir.put(iI, new Character[64]);
			for(int j = 0; j < 64; j++) { //svaki je okvir velicine 64 bajtova
				okvir.get(iI)[j] = 0;
			}
		}
		
		for(int i = 0; i < N; i++) {
			Integer iI = Integer.valueOf(i);
			tablicePrevodenja.put(iI, new Short[16]);
			for(int j = 0; j < 16; j++) {
				tablicePrevodenja.get(iI)[j] = 0;
			}
		}
		
		t = 0;
		int i;
		Random randomAdresa = new Random();
		System.out.println("-------------------------");
		while(true) {
			for(int p = 0; p < N; p++) {
				System.out.println("proces: " + p);
				System.out.println("	t: " + t);
				int x = randomAdresa.nextInt(); // generiranje nasumicne logicke adrese
				int xBinarni = x & 0x3fe; // samo parne adrese
				System.out.println("	log.adresa: 0x" + Integer.toHexString(xBinarni));
				i = dohvatiSadrzaj(p, xBinarni);
				i = i + 1;
				prviUlazak = 0;
				zapisiSadrzaj(p, xBinarni, i);
				t = t + 1;
				Thread.sleep(1000);
				System.out.println("-------------------------");
				prviUlazak = 1;
			}
		}
	}
	
	private static int dohvatiSadrzaj(int brojProcesa, int logickaAdresa) {
		int fizickaAdresa = dohvatiFizickuAdresu(brojProcesa, logickaAdresa);
	    int fizickaAdresaOkvira = (fizickaAdresa >> 6) & 0b0000001111111111;
	    int pomakUnutarOkvira = fizickaAdresa & 0b0000000000111111;
		return okvir.get(fizickaAdresaOkvira)[pomakUnutarOkvira];
	}
	
	private static void zapisiSadrzaj(int brojProcesa, int logickaAdresa, int i) {
		int fizickaAdresa = dohvatiFizickuAdresu(brojProcesa, logickaAdresa);
	    int fizickaAdresaOkvira = (fizickaAdresa >> 6) & 0b0000001111111111;
	    int pomakUnutarOkvira = fizickaAdresa & 0b0000000000111111;
	    okvir.get(fizickaAdresaOkvira)[pomakUnutarOkvira] = (char) i;
		return;
	}
	
	private static int dohvatiFizickuAdresu(int brojProcesa, int logickaAdresa) {
		int konacnaAdresa = 0;
		int pomakUnutarOkvira = logickaAdresa & 0b0000000000111111; // pomak unutar okvira je posljednjih 6 bitova logicke adrese
		int indeksTabliceStranicenja = (logickaAdresa >> 6) & 0b0000000000001111; // indeks tablice stranicenja je 4 bitova logicke adrese
		Short zapisTabliceStranicenja = tablicePrevodenja.get(brojProcesa)[indeksTabliceStranicenja]; // dohvacanje zapisa tablice stranicenja
		int fizickaAdresaOkvira = (zapisTabliceStranicenja >> 6) & 0b0000000111111111; // razlaganje zapisa tablice stranicenja na fizicku adresu okvira i bit prisutnosti
		int bitPrisutnosti = (zapisTabliceStranicenja >> 5) & 0b0000000000000001;
		
		if(bitPrisutnosti == 0) { // ako dohvacena stranica trenutno nije na ram-u
			if(prviUlazak == 1) {
				System.out.println("	Promasaj!");
			}
			
			if(slobodanOkvir < M) { //ako ima slobodnog mjesta
				int counter = 0;
				for(Character ch: disk.get(brojProcesa).get(indeksTabliceStranicenja)) {//ucitaj sadrzaj stranice s diska
					okvir.get(slobodanOkvir)[counter] = ch;
					counter++;
				}
				if(prviUlazak == 1) {
					System.out.println("		dodijeljen okvir: 0x" + Integer.toHexString(slobodanOkvir));
				}
				
				//azuriraj tablicu prevodenja procesa p --> prvo dodaj adresu okvira, zatim digni bit prisutnosti i na kraju dodaj t za lru strategiju
				tablicePrevodenja.get(brojProcesa)[indeksTabliceStranicenja] = (short)(slobodanOkvir << 6);
				tablicePrevodenja.get(brojProcesa)[indeksTabliceStranicenja] = (short) (tablicePrevodenja.get(brojProcesa)[indeksTabliceStranicenja] | 0b100000);
				tablicePrevodenja.get(brojProcesa)[indeksTabliceStranicenja] = (short) (tablicePrevodenja.get(brojProcesa)[indeksTabliceStranicenja] + t);
			    konacnaAdresa = 0;
				konacnaAdresa = slobodanOkvir << 6;
				konacnaAdresa += pomakUnutarOkvira;
				
				if(prviUlazak == 1) {
					System.out.println("        fiz. adresa: 0x" + Integer.toHexString(konacnaAdresa));
					System.out.println("        zapis tablice: 0x" + Integer.toHexString(tablicePrevodenja.get(brojProcesa)[indeksTabliceStranicenja]));
					System.out.println("        sadrzaj adrese: " + (int)okvir.get(slobodanOkvir)[pomakUnutarOkvira]);
				}
				slobodanOkvir++;
				return konacnaAdresa;
			}
			
			else { //nema slobodnog mjesta, treba pronaci lru
				int minLRU = 30000;
				int LRUmetapodatak;
				int minAdresaTablicePrevodenja = 1000000;
				int minProces = 0;
				int minIndeks = 0;
				int minFizAdresaOkvira = 0;
				for(int p = 0; p < N; p++) {
					for(int j = 0; j < 16; j++) {
						int adresa = tablicePrevodenja.get(p)[j];
						if(prviUlazak == 1 && adresa != 0) {
							int bitPrisutnosti2 = (adresa >> 5) & 0b0000000000000001;
							/// System.out.println("bitP " + bitPrisutnosti2);
							if(bitPrisutnosti2 == 1) {
								LRUmetapodatak = adresa & 0b0000000000011111;
								if(LRUmetapodatak < minLRU) {
									minLRU = LRUmetapodatak;
									minAdresaTablicePrevodenja = adresa;
									minProces = p;
									minIndeks = j;
								}
							}
						}
					}
				}
			
				minFizAdresaOkvira = (minAdresaTablicePrevodenja >> 6) & 0b0000001111111111;
				if(prviUlazak == 1) {
					System.out.println("		Izbacujem stranicu 0x" + Integer.toHexString(minIndeks * 64) + " iz procesa " + minProces);
					System.out.println("		lru izbacene stranice: 0x" + Integer.toHexString(minLRU));
				}
				
				//spustanje bita prisutnosti izbacene stranice
				tablicePrevodenja.get(minProces)[minIndeks] = (short) (tablicePrevodenja.get(minProces)[minIndeks] & 0b1111111111011111);
				fizickaAdresaOkvira = (minAdresaTablicePrevodenja >> 6) & 0b0000001111111111;
				if(prviUlazak == 1) {
					System.out.println("        dodijeljen okvir: 0x" + Integer.toHexString(fizickaAdresaOkvira));
				}
				
				//kopiranje sadrzaja izbacene stranice na disk
			    Character[] kopija = okvir.get(fizickaAdresaOkvira).clone();
			    HashMap<Integer, Character[]> kopijaMape = new HashMap<>(disk.get(minProces));
			    kopijaMape.put(minIndeks, kopija);
			    disk.put(minProces, kopijaMape);
		
			    //ucitaj sadrzaj nove stranice s diska
			    int counter = 0;
				for(Character ch: disk.get(brojProcesa).get(indeksTabliceStranicenja)) {//ucitaj sadrzaj stranice s diska
					okvir.get(minFizAdresaOkvira)[counter] = ch;
					counter++;
				}

				//azuriraj tablicu prevodenja procesa p --> prvo dodaj adresu okvira, zatim digni bit prisutnosti i na kraju dodaj t za lru strategiju
				tablicePrevodenja.get(brojProcesa)[indeksTabliceStranicenja] = (short) (minFizAdresaOkvira << 6);
				tablicePrevodenja.get(brojProcesa)[indeksTabliceStranicenja] = (short) (tablicePrevodenja.get(brojProcesa)[indeksTabliceStranicenja] | 0b100000);
				tablicePrevodenja.get(brojProcesa)[indeksTabliceStranicenja] = (short) (tablicePrevodenja.get(brojProcesa)[indeksTabliceStranicenja] + t);
				
				konacnaAdresa = 0;
				konacnaAdresa = minFizAdresaOkvira << 6;
				konacnaAdresa += pomakUnutarOkvira;
				
				if(prviUlazak == 1) {
					System.out.println("        fiz. adresa: 0x" + Integer.toHexString(konacnaAdresa));
					System.out.println("        zapis tablice: 0x" + Integer.toHexString(tablicePrevodenja.get(brojProcesa)[indeksTabliceStranicenja]));
					System.out.println("        sadrzaj adrese: " + (int)okvir.get(minFizAdresaOkvira)[pomakUnutarOkvira]);
				}
			}	
		}
		
		else {
			konacnaAdresa = fizickaAdresaOkvira << 6;
			konacnaAdresa += pomakUnutarOkvira;
			
			tablicePrevodenja.get(brojProcesa)[indeksTabliceStranicenja] = (short) (tablicePrevodenja.get(brojProcesa)[indeksTabliceStranicenja] & 0b1111111111100000);
			tablicePrevodenja.get(brojProcesa)[indeksTabliceStranicenja] = (short) (tablicePrevodenja.get(brojProcesa)[indeksTabliceStranicenja] + t);
			
			if(prviUlazak == 1) {
			System.out.println("        fiz. adresa: 0x" + Integer.toHexString(konacnaAdresa));
			System.out.println("        zapis tablice: 0x" + Integer.toHexString(tablicePrevodenja.get(brojProcesa)[indeksTabliceStranicenja]));
			System.out.println("        sadrzaj adrese: " + (int)okvir.get(fizickaAdresaOkvira)[pomakUnutarOkvira]);
			}	
		}
		
		if(t == 31) {
			t = 0;
			for(int p = 0; p < N; p++) {
				for(int i = 0; i < 16; i++) {
					int zapisTabliceStranicenja2 = tablicePrevodenja.get(p)[i];
					zapisTabliceStranicenja2 = zapisTabliceStranicenja2 & 0b1111111111100000;
					tablicePrevodenja.get(p)[i] = (short) zapisTabliceStranicenja2;
				}
			}
			tablicePrevodenja.get(brojProcesa)[indeksTabliceStranicenja] = (short) (tablicePrevodenja.get(brojProcesa)[indeksTabliceStranicenja] | 0b0000000000000001);
		}
		
		return konacnaAdresa;
	}
}