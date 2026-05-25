# 🧵 TriRang — AI-Powered Hyperlocal Circular Textile Intelligence Platform

> **India's first AI-powered circular textile ecosystem where no textile ends in landfills.**
> 
> *Karrot (hyperlocal marketplace) + Jarvis (AI assistant) + Circular Economy Intelligence*

---

## Table of Contents
1. [Vision & Principles](#vision--principles)
2. [System Architecture](#system-architecture)
3. [Tech Stack](#tech-stack)
4. [User Roles & Workflows](#user-roles--workflows)
5. [Core Features](#core-features)
6. [Database Schema](#database-schema)
7. [AI & Voice Integration](#ai--voice-integration)
8. [Getting Started](#getting-started)
9. [Environment Variables](#environment-variables)
10. [API Structure](#api-structure)
11. [Development Phases](#development-phases)
12. [License](#license)

---

## Vision & Principles

1. **No textile lands in landfills** — Every cloth is classified into REWEAR, REUSE, or RECYCLE.
2. **Hyperlocal circularity** — Transactions happen within 1km–25km radii.
3. **Artisan empowerment** — Illiterate artisans use voice-first AI to find materials and sell products.
4. **Transparent & Trust-based** — Every pickup is QR-verified; every user has a TriTrust score.
5. **Multilingual by design** — English, हिंदी (Hindi), and मराठी (Marathi) supported natively.

---

## System Architecture

TriRang follows a **Monolith + AI Sidecar** pattern. Spring Boot owns the state; Python owns the models.
