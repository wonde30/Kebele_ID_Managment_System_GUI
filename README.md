# Kebele ID Management System v2.0

> A professional desktop application for managing resident identification records in Ethiopian kebele offices — replacing paper-based processes with a secure, role-based digital solution.

[![Build](https://img.shields.io/badge/build-passing-brightgreen.svg)]()
[![Java](https://img.shields.io/badge/Java-17-orange.svg)](https://www.oracle.com/java/)
[![Maven](https://img.shields.io/badge/Maven-3.6+-blue.svg)](https://maven.apache.org/)
[![SQLite](https://img.shields.io/badge/SQLite-3.45.1-lightblue.svg)](https://sqlite.org/)
[![BCrypt](https://img.shields.io/badge/Security-BCrypt-red.svg)]()
[![License](https://img.shields.io/badge/License-Academic-green.svg)]()
[![Platform](https://img.shields.io/badge/Platform-Windows%20%7C%20macOS%20%7C%20Linux-lightgrey.svg)]()
[![Version](https://img.shields.io/badge/Version-2.0-purple.svg)]()

---

## 📋 Table of Contents

- [Overview](#-overview)
- [Key Features](#-key-features)
- [Technology Stack](#%EF%B8%8F-technology-stack)
- [Architecture](#-architecture)
- [Quick Start](#-quick-start)
- [Building from Source](#%EF%B8%8F-building-from-source)
- [User Roles & Permissions](#-user-roles--permissions)
- [Security](#-security)
- [Database Schema](#-database-schema)
- [Configuration](#-configuration)
- [Troubleshooting](#-troubleshooting)
- [Changelog](#-changelog)
- [Documentation](#-documentation)
- [Contributing](#-contributing)
- [License](#-license)
- [Acknowledgments](#-acknowledgments)

---

## 🌍 Overview

The **Kebele ID Management System** digitizes and streamlines resident identification record management in Ethiopian kebele (local administrative) offices. Built with Java Swing, it provides a full-featured desktop GUI with role-based access, QR-coded ID card generation, approval workflows, and audit logging — all backed by a local SQLite database requiring zero server infrastructure.

### Why this system?

| Before | After |
|--------|-------|
| Paper ledgers, prone to loss | Digital records with automated backup |
| No access control | 5-tier role-based access (RBAC) |
| Manual ID card printing | Automated QR-coded ID card generation |
| No audit trail | Complete activity log of every action |
| Data silos | PDF & Excel export for reporting |

---

## ✨ Key Features

- **🔐 Role-Based Access Control** — 5 distinct user roles with granular permissions
- **👥 Resident Management** — Complete CRUD operations with photo capture support
- **🎫 ID Card Generation** — Professional ID cards with embedded QR codes
- **✅ Two-Stage Approval Workflow** — Supervisor/Admin sign-off before records are finalized
- **📊 Reports & Analytics** — Interactive charts and data visualization dashboards
- **📤 Data Export** — PDF (iText) and Excel (Apache POI) export functionality
- **📝 Activity Audit Log** — Immutable trail of all create/update/delete operations
- **💾 Automated Database Backup** — Scheduled and on-demand backup with restore
- **🔍 Advanced Search & Filter** — Search residents by name, ID, kebele, or status
- **📬 Feedback System** — Built-in feedback submission with priority tracking

---

## 🛠️ Technology Stack

| Component | Technology | Version |
|-----------|-----------|---------|
| **Language** | Java | 17 LTS |
| **GUI Framework** | Java Swing | Built-in |
| **Database** | SQLite | 3.45.1.0 |
| **Build Tool** | Maven | 3.6+ |
| **Password Hashing** | jBCrypt | 0.4 |
| **PDF Export** | iText | 5.5.13.3 |
| **Excel Export** | Apache POI | 5.2.5 |
| **QR Code Generation** | ZXing | 3.5.1 |

---

## 🏛️ Architecture

```
┌─────────────────────────────────────────────────────────┐
│                     Presentation Layer                   │
│           Java Swing GUI (ui/ package)                   │
├─────────────────────────────────────────────────────────┤
│                      Business Layer                      │
│    Auth │ Workflow │ Reports │ ID Generation │ Export    │
├─────────────────────────────────────────────────────────┤
│                      Data Layer                          │
│         SQLite via JDBC (db/ package)                    │
│         PreparedStatements — SQL injection safe          │
└─────────────────────────────────────────────────────────┘
```

**Design pattern:** Layered MVC — the UI layer never accesses the database directly; all data flows through business-logic classes in `auth/`, `model/`, and `utils/`.

---

## 🚀 Quick Start

### Prerequisites

| Requirement | Minimum | Recommended |
|-------------|---------|-------------|
| Java (JDK or JRE) | 17 | 21 LTS |
| RAM | 256 MB | 512 MB |
| Disk | 50 MB | 100 MB |
| OS | Windows 10, macOS 12, Ubuntu 20.04 | Latest |

> **Check your Java version:** `java -version`  
> Install Java 17+ from [Adoptium](https://adoptium.net/) if needed.

---

### Running the Application

**Option 1 — Executable (Windows, easiest)**
```bash
KebeleIDSystem.exe
```

**Option 2 — JAR (cross-platform)**
```bash
java -jar KebeleIDSystem-v2.0-build-{timestamp}.jar
```

**Option 3 — Batch file (Windows)**
```bash
RUN_APPLICATION.bat
```

---

### Default Login

```
Username: admin
Password: admin123
```

> ⚠️ **Change this password immediately after first login** via Settings → Change Password.

---

## 🏗️ Building from Source

```bash
# 1. Clone the repository
git clone https://github.com/yourusername/KebeleIDSystem.git
cd KebeleIDSystem

# 2. Build (Maven downloads all dependencies automatically)
mvn clean package

# 3. Run
java -jar target/KebeleIDSystem-v2.0-build-{timestamp}.jar
```

**Build output:**
```
target/
├── KebeleIDSystem-v2.0-build-{timestamp}.jar   ← runnable fat JAR
└── KebeleIDSystem.exe                           ← Windows executable
```

> All dependencies are bundled in the fat JAR — no external libraries required at runtime.

---

## 📁 Project Structure

```
KebeleIDSystem/
├── src/
│   └── main/
│       └── java/
│           ├── Main.java                  # Application entry point
│           ├── auth/                      # Login, registration, session
│           │   ├── AuthService.java
│           │   └── SessionManager.java
│           ├── config/                    # App configuration & constants
│           │   └── AppConfig.java
│           ├── db/                        # Database connection & migrations
│           │   ├── DatabaseManager.java
│           │   └── BackupService.java
│           ├── model/                     # POJOs: User, Resident, ActivityLog
│           ├── ui/                        # All Swing panels and dialogs
│           │   ├── LoginFrame.java
│           │   ├── MainDashboard.java
│           │   ├── ResidentPanel.java
│           │   ├── IDCardPanel.java
│           │   └── ReportsPanel.java
│           └── utils/                     # PDF, Excel, QR, validation helpers
│               ├── PDFExporter.java
│               ├── ExcelExporter.java
│               └── QRCodeGenerator.java
├── pom.xml                                # Maven configuration & dependencies
├── README.md                              # This file
├── FEATURES.md                            # Detailed feature documentation
├── HOW_TO_RUN.md                          # Extended running instructions
├── CHANGELOG.md                           # Version history
└── USER_GUIDE_BY_ROLE.md                  # Role-specific user guide
```

---

## 👥 User Roles & Permissions

| Role | Residents | ID Cards | Approvals | Reports | User Mgmt | Config |
|------|-----------|----------|-----------|---------|-----------|--------|
| **Admin** | ✅ Full | ✅ | ✅ | ✅ | ✅ | ✅ |
| **Supervisor** | ✅ Full | ✅ | ✅ | ✅ | ❌ | ❌ |
| **Staff** | ✅ Add/Edit | ✅ | ❌ | ✅ View | ❌ | ❌ |
| **Data Encoder** | ✅ Add/Edit | ❌ | ❌ | ❌ | ❌ | ❌ |
| **Viewer** | 🔍 Own record | ❌ | ❌ | ❌ | ❌ | ❌ |

New user accounts require Admin approval before first login.

---

## 🔒 Security

| Feature | Implementation |
|---------|---------------|
| Password hashing | BCrypt, work factor 12 |
| Access control | Role-Based (RBAC), enforced server-side |
| SQL injection | All queries use `PreparedStatement` |
| Input validation | Sanitized on both UI and DB layer |
| Audit trail | Immutable `activity_log` table |
| Session management | Single-session enforcement per user |

---

## 📊 Database Schema

```sql
users           -- Accounts with hashed passwords, roles, approval status
residents       -- Full identification records (name, DOB, photo path, kebele)
activity_log    -- Immutable audit trail (user, action, timestamp, details)
feedback        -- User-submitted feedback with priority & status tracking
```

The database file (`kebele_system.db`) is created automatically on first launch in the application directory. No manual setup required.

**Backup location:** `backups/kebele_system_backup_{timestamp}.db`

---

## ⚙️ Configuration

Application settings are managed via `config/AppConfig.java` and persisted in `app.properties`:

| Setting | Default | Description |
|---------|---------|-------------|
| `db.path` | `./kebele_system.db` | Database file location |
| `backup.dir` | `./backups/` | Backup directory |
| `backup.auto` | `true` | Enable automatic backup on exit |
| `session.timeout` | `30` | Idle session timeout (minutes) |
| `bcrypt.workfactor` | `12` | BCrypt hashing strength |

---

## 🐛 Troubleshooting

### Java not found
```
Error: 'java' is not recognized as an internal or external command
```
➡️ Install Java 17+ from [Adoptium](https://adoptium.net/) and ensure it's on your `PATH`.

---

### Application won't start
1. Confirm Java version: `java -version` (must be 17+)
2. Confirm you are in the correct directory
3. Try the full path: `java -jar "C:\path\to\KebeleIDSystem-v2.0.jar"`
4. Check for a `logs/app.log` file for error details

---

### Database locked / corrupted
```
SQLiteException: database is locked
```
➡️ Ensure no other instance of the app is running. If the issue persists, delete `kebele_system.db` — a fresh database will be auto-created on next launch. Restore from `backups/` if needed.

---

### ID card not generating
- Confirm the resident record has been **approved** (two-stage workflow)
- Confirm the resident has a valid photo on file
- Check write permissions on the output directory

---

### Login fails after password change
➡️ Use the **Forgot Password** option (Admin reset) or have an Admin reset your account via User Management.

---

### High memory usage
➡️ Add JVM flags for memory tuning:
```bash
java -Xms128m -Xmx512m -jar KebeleIDSystem-v2.0.jar
```

---

## 📄 Changelog

See [CHANGELOG.md](CHANGELOG.md) for full version history.

### v2.0 (2026) — Current
- Added two-stage approval workflow
- QR code integration on ID cards
- Excel export via Apache POI
- Activity audit log
- Automated database backup
- Feedback system with priority tracking

### v1.0 (2025) — Initial release
- Basic resident CRUD
- PDF ID card generation
- Single admin role

---

## 📖 Documentation

| Document | Description |
|----------|-------------|
| [FEATURES.md](FEATURES.md) | In-depth feature documentation |
| [HOW_TO_RUN.md](HOW_TO_RUN.md) | Extended setup and run instructions |
| [USER_GUIDE_BY_ROLE.md](USER_GUIDE_BY_ROLE.md) | Role-specific usage guide |
| [CHANGELOG.md](CHANGELOG.md) | Full version history |

---

## 🤝 Contributing

This is an academic project. Feedback and suggestions are welcome via the Issues tab.

1. Fork the repository
2. Create a feature branch: `git checkout -b feature/your-feature`
3. Commit your changes: `git commit -m "Add your feature"`
4. Push to the branch: `git push origin feature/your-feature`
5. Open a Pull Request

Please follow existing code style (Java conventions, Swing patterns used throughout).

---

## 🎓 Academic Information

| Field | Detail |
|-------|--------|
| **Developer** | Wondatir Fetene |
| **Student ID** | WOUR/2003/16 |
| **Institution** | Wollo University |
| **Program** | Computer Science (Final Year Project) |
| **Year** | 2026 |

---

## 📄 License

Developed for academic purposes at Wollo University. All third-party libraries are open-source and used in accordance with their respective licenses:

- SQLite JDBC — Apache 2.0
- jBCrypt — ISC License
- iText 5 — AGPL / Commercial
- Apache POI — Apache 2.0
- ZXing — Apache 2.0

---

## 🙏 Acknowledgments

- Ethiopian flag color scheme for the UI theme
- [Adoptium](https://adoptium.net/) for the open-source Java runtime
- The open-source maintainers of SQLite, iText, Apache POI, and ZXing
- Academic advisors and instructors at Wollo University

---

## 📞 Contact

| | |
|-|-|
| **Developer** | Wondatir Fetene |
| **Student ID** | WOUR/2003/16 |
| **Institution** | Wollo University |

---

<div align="center">
  <strong>Developed with ❤️ by Wondatir Fetene — Wollo University, 2026</strong>
</div>
