# sf-audit-logging [<img align="right" src="https://github.githubassets.com/images/modules/logos_page/GitHub-Mark.png" width="18" alt="GitHub repository" />](https://github.com/navikt/sf-audit-logging)
Henter LightningUriEvents fra Salesforce. 
Identifiserer relevante oppslag og tilfører fødselsnummer
Overfører loggene til Arcsight

NAIS-jobb kjører hver natt
Det er mulig å overføre objekter enkeltvis ved behov:
/internal/fetchAndLog?entity=Account&eventDate=2025-06-01&offset=0

https://navikt.github.io/platforce-doc/security/audit-logging/

