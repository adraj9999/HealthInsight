# Health Insight 

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


<img width="1919" height="994" alt="image" src="https://github.com/user-attachments/assets/ad44802f-36a8-45fb-bc0c-ad6f8369afa4" />
