# EduAccountingSuite

**EduAccountingSuite** is a desktop accounting-cycle learning tool for students. It provides guided journal entries, CSV and OCR import, ledger posting, trial-balance and worksheet generation, and Excel/PDF/Word report export.

## App stack

- Java 8-compatible source code
- Java Swing desktop UI
- SQLite local database
- Maven
- Apache POI for Excel and Word exports
- iText 7 for PDF exports
- Tess4J/Tesseract for OCR imports

## Build

Install JDK 17 or newer and Maven, then run:

```powershell
mvn clean package
```

The executable bundle is created at:

```text
target\EduAccountingSuite-1.0-SNAPSHOT-all.jar
```

The application stores each student's database in:

```text
%USERPROFILE%\.EduAccountingSuite\accounting.db
```

## Create a Windows installer

From PowerShell:

```powershell
.\package-windows.ps1
```

The installer is written to `dist\EduAccountingSuite-1.0.0.exe`.

## Student use

Each student runs the application locally and receives an independent database. No student data is uploaded or shared by this desktop edition.
