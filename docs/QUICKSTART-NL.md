# PocketPad: snel beginnen

Met PocketPad gebruik je je Android-telefoon als trackpad en toetsenbord voor een Windows-pc. De app zelf is Engelstalig.

## Installeren

Download de bestanden van de [officiële bèta-uitgave](https://github.com/Thesuperteacher/PocketPad/releases/tag/v0.3.1). Installeer de APK op Android 7 of nieuwer. Pak het Windows-pakket uit en houd alle DLL-bestanden bij `PocketPad.exe`. Je hebt Windows 10/11 x64 nodig.

De APK heeft een debugondertekening; het Windows-programma heeft geen uitgevershandtekening. Dit zijn experimentele versies. Vergelijk downloads met `SHA256SUMS.txt` en lees eventuele installatiewaarschuwingen. Schakel beveiligingssoftware niet uit.

## Verbinden

**Wifi:** verbind beide apparaten met hetzelfde vertrouwde netwerk. Kies de juiste netwerkadapter in PocketPad op de pc en klik op **Start connection**. Open de verbindingsknop op de telefoon en kies **Scan PC code · Wi-Fi / USB tethering**. Scan de code. Houd die code privé.

**USB-foutopsporing:** installeer Android Platform Tools, schakel USB-foutopsporing in en accepteer de melding op je ontgrendelde telefoon. Gebruik een datakabel. Voer `Start-USB.ps1` uit, kies **USB cable / this PC only** op de pc en start de verbinding. Kies op de telefoon **Scan PC code · USB debugging cable**. Voer het hulpscript opnieuw uit nadat je de kabel hebt losgemaakt en aangesloten.

**USB-tethering:** zet USB-tethering aan op de telefoon. Open het pc-programma opnieuw, kies de USB-netwerkadapter en scan via de optie voor wifi/tethering. Tethering kan ook mobiel internet delen.

**Bluetooth:** experimenteel; vereist Android 9+ en geschikte HID-firmware. Kies **Bluetooth mouse & keyboard** en koppel de pc. Het Windows-programma is hiervoor niet nodig. Open het toetsenbord handmatig. Bluetooth-tekst ondersteunt alleen afdrukbare ASCII-tekens en een Amerikaanse toetsenbordindeling op de pc. Gebruik USB of wifi voor accenten en emoji.

## Bedienen

| Gebaar | Actie |
|---|---|
| Eén vinger schuiven | Aanwijzer bewegen |
| Eén of twee keer tikken | Klikken of dubbelklikken |
| Tikken, loslaten, snel opnieuw aanraken en vasthouden | Slepen; vinger optillen om los te laten |
| Twee vingers schuiven | Verticaal scrollen |
| Met twee vingers tikken | Rechtermuisklik |
| Drag aanzetten | Muisknop vasthouden; opnieuw tikken om los te laten |

Bij USB en wifi kan het toetsenbord automatisch openen als je een ondersteund tekstveld op de pc selecteert. Typ op je telefoon en tik op **Send text**. Het tekstvak op de telefoon is een concept; toetsaanslagen worden niet direct verstuurd. Met **Keyboard** open je het toetsenbord zelf.

## Grenzen en hulp

De fysieke tests zijn vooral uitgevoerd op een Galaxy Z Fold5. Bluetooth en ondersteuning voor meer apparaten moeten nog worden getest. Niet elk aangepast tekstveld opent het toetsenbord automatisch. Beheerdersvensters, UAC en het aanmeldscherm vallen buiten de normale rechten.

Geen verbinding? Controleer de kabel, het USB-toestemmingsvenster, de gekozen netwerkadapter en de firewall. Gastnetwerken kunnen apparaten van elkaar scheiden. Stop de verbinding op de pc om de koppelcode ongeldig te maken.

De [Engelse handleiding](../README.md) bevat volledige instructies en ontwikkelinformatie. Deel bij foutmeldingen nooit koppelcodes, apparaatserienummers of persoonlijke tekst.
