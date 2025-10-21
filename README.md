# Health Insight �

A **Java Swing** desktop application that provides users with personalized health insights and preliminary diagnoses based on their reported symptoms.

## Overview
- Collects user details (name, age, sex) and selected symptoms  
- Uses **HashMap‑based logic** to map symptoms to possible conditions  
- Displays personalized health tips and recommendations  
- Allows users to **save assessments** and **view history**  

##  Tech Stack
| Layer | Technology |
|--------|-------------|
| **Language** | Java 17 |
| **UI Framework** | Java Swing |
| **Logic Core** | HashMap data structures |
| **Database** | MySQL |
| **Connector** | JDBC (MySQL Connector J) |

##  Database Structure
- **users** – (id, name, age, sex, created_at)  
- **assessments** – (id, user_id, symptoms, top_conditions, advice, urgent, notes, created_at)

##  Run Locally
1. Clone the repository  
   ```bash
   git clone https://github.com/adraj9999/HealthInsight.git
