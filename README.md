# sf-audit-logging
Henter LightningUriEvents fra Salesforce. 
Identifiserer relevante oppslag og tilfører fødselsnummer
Overfører loggene til Arcsight

NAIS-jobb kjører hver natt
Det er mulig å overføre objekter enkeltvis ved behov:
/internal/fetchAndLog?entity=Account&eventDate=2025-06-01&offset=0

