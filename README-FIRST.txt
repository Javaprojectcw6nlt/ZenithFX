ZENITHFX — START HERE
=====================

This is a starter project for the Java 25 / Spring Boot homework. It includes:
- ZenithFX dark purple theme and responsive navigation
- Hungarian National Bank SOAP endpoint integration (historical exchange rates)
- Chart.js chart for returned HNB rates
- OANDA practice API integration points for account, prices, candle history, positions, opening and closing trades
- Maven build configuration targeting Java 25
- a Windows Maven bootstrap script (mvnw.cmd)
- a basic Spring Boot test
- setup and submission checklist

IMPORTANT
---------
This is a working project scaffold, not a guarantee that every external API call
will work before you configure your OANDA practice account and verify the HNB
service response. You must run it, test each assignment item, capture your own
screenshots, and complete the required 10+ page PDF documentation. Do not submit
unverified code as completed work.

1. Extract this ZIP into a normal writable folder, e.g. C:\JavaProjects\ZenithFX.
2. Open VS Code -> File -> Open Folder -> select the ZenithFX folder.
3. Wait for Java extensions to finish importing the Maven project.
4. Copy application-local.example.properties to application-local.properties.
   Keep application-local.properties private; it is ignored by Git.
5. Edit application-local.properties and add your OANDA PRACTICE account ID,
   practice API token, and creator names / Neptun codes.
6. In the VS Code terminal (PowerShell or CMD), run:
      .\mvnw.cmd -version
      .\mvnw.cmd test
      .\mvnw.cmd spring-boot:run
   First run needs internet access to download Maven and Java dependencies.
7. Open http://localhost:8080 in your browser.
8. Check the SOAP page with a date range and currency. Then test each Forex page.
9. Before submission, run:
      .\mvnw.cmd clean package
   The output must be target\homework.jar. Copy it to the project root:
      copy target\homework.jar homework.jar
   Run the required command from the project root:
      java -jar homework.jar
10. Commit gradually in at least five meaningful commits, not one final upload.
11. Publish the GitHub repository as PUBLIC only after checking for secrets.
12. Make your own PDF documentation (minimum 10 pages) with screenshots,
    implementation explanations, GitHub URL, OANDA practice account login details
    as required by your lecturer, and creator contributions. Follow your
    lecturer's secure-submission instructions; never publish API tokens.

OANDA CONFIGURATION
-------------------
Use an OANDA practice account, not a live account. Create a practice API token
in your OANDA account. Put it only in application-local.properties. Never paste
the token into chat or commit it to GitHub.

HNB SOAP
--------
The SOAP page sends a SOAP request to the Hungarian National Bank web service.
The request/response format may change; if the service reports an error, inspect
the browser's message and the application terminal, then verify the current WSDL
and SOAPAction with the official MNB service documentation.

ASSIGNMENT CHECKLIST
--------------------
[ ] Java 25 verified
[ ] Homepage/theme documented
[ ] Creator names and Neptun codes in footer on every page
[ ] HNB currency dropdown, date range, data table and Chart.js chart
[ ] OANDA account information
[ ] Current instrument prices
[ ] Last 10 historical prices with instrument and granularity
[ ] Open a practice position using positive (long) or negative (short) units
[ ] Open positions table
[ ] Close a position by Trade ID
[ ] Public GitHub repository with at least five meaningful commits
[ ] Evidence of each member's contribution
[ ] Executable homework.jar in repository root and tested using Java 25
[ ] PDF named Name-NeptunCode.pdf, minimum 10 pages, submitted to Teams
[ ] Remove all credentials/secrets from commits and public repository
