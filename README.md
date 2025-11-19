# Grindly App – README

## Team Members
- Itumeleng Molawa – ST10373912  
- Ade-Eza Silongo – ST10361062  
- Lusanda Mlotshwa – ST10304204  
- Reaobaka Ntoagae – ST10402790  

---

## Purpose
Grindly is a mobile-first Android application designed to empower South African hustlers—informal service providers such as tutors, stylists, and repairers—by giving them visibility, credibility, and access to digital professionalism.

It bridges the gap between grassroots entrepreneurship and modern technology, transforming everyday skills into sustainable businesses. Hustlers gain a platform to present themselves professionally, clients benefit from secure and reliable bookings, and communities thrive through trust, transparency, and inclusivity.

Grindly is not just an app—it’s a movement toward empowerment and growth.

---

## Target Users
- **Hustlers**: Create service listings, manage bookings, and build credibility  
- **Clients**: Discover services, book appointments, and leave reviews  

---

## Design Philosophy
- **Inclusivity**: Multi-language support (English, isiXhosa, isiZulu)  
- **Security**: Google SSO and biometric login (fingerprint, face unlock, PIN)  
- **Transparency**: State-based service tracking with clear booking statuses  
- **Scalability**: RESTful API and modular backend architecture  
- **Accessibility**: Offline mode for browsing services and favourites  
- **Consistency**: Defined colour scheme and typography for professional UI  

---

## Colour Scheme & UI Design

### Primary Colours

| Colour | Usage |
|--------|-------|
| Purple `#6A0DAD` | Primary buttons, status indicators, sidebar highlights |
| White `#FFFFFF` | Backgrounds, input fields, top panel text |

### Accent & Status Colours

| Colour | Status |
|--------|--------|
| Lavender `#E6E6FA` | Panels, form backgrounds, dashboard cards |
| Blue `#1E90FF` | Accepted |
| Yellow `#FFD700` | On the Way |
| Green `#32CD32` | In Progress |
| Grey `#A9A9A9` | Pending |

### Typography
- **Font Family**: Palanquin  
- **Headers**: Palanquin Medium, white text on lavender  
- **Body Text**: Palanquin Regular, dark purple or charcoal grey  
- **Buttons**: Palanquin Bold, white text on purple  

### Layout & Navigation
- Top panels: Lavender with white titles  
- Forms: White fields with purple borders  
- Buttons: Rounded corners, purple fill, white text  
- Bottom nav bar: Purple icons on white  
- Sidebar menu: Hamburger icon → slides in from left → white background, purple text/icons  
  - Items: Home, Profile, My Bookings, Favourites, Settings, Logout  

---

## Technical Stack

| Layer         | Technology                        |
|---------------|-----------------------------------|
| Frontend      | Kotlin (Android Studio)           |
| Backend       | Node.js + Express                 |
| Database      | Firebase + RoomDB (offline sync)  |
| Authentication| Google SSO, biometric login       |
| Notifications | Firebase Cloud Messaging          |
| CI/CD         | GitHub + GitHub Actions           |

---

## Core Features

| Feature | Description |
|--------|-------------|
| Multi-language Support | English, isiXhosa, isiZulu with instant UI switching |
| Authentication | Google Sign-In, biometric login, secure token lifecycle |
| Booking System | Full lifecycle: Pending → Accepted → On my way → In Progress → Completed |
| Service Tracking | Real-time UI updates, state-based (no GPS) |
| Listings & Packages | Hustlers create listings with images, pricing, bundles |
| Favourites & Reviews | Clients save services, leave ratings/comments |
| Offline Mode | Browse services/favourites offline; sync when reconnected |
| Push Notifications | Alerts for bookings and status updates |
| CI/CD Pipeline | GitHub Actions for build, lint, test, APK generation |

---

## User Guide

### Installation
1. Download `Grindly_v1.0.0.apk` from GitHub Releases  
2. Transfer to Android device  
3. Enable “Install from Unknown Sources”  
4. Tap APK to install (Android 8.0+)  

### Launch & Welcome
- Grindly logo with pink, purple, orange icons  
- Tagline: “Showcase. Connect. Thrive.”  
- Language selector appears immediately  

### Language Settings
- Navigate to Settings  
- Choose English, isiXhosa, or isiZulu  
- UI updates instantly and persists offline  

### Authentication
- Tap “Continue with Google”  
- Enable biometric login (fingerprint, face unlock, PIN)  
- Secure token storage and refresh  

### Hustler Profile Setup
- Add profile photo, categories, pricing  
- Create listings with title, description, price, images  
- Bundle services into packages  

### Browsing & Booking
- Clients browse or search services  
- Tap listing → view details → select time → Book Now  
- Hustlers accept bookings → status updates automatically  

### Service Tracking
- Booking states: Pending → Accepted → On my way → In Progress → Completed  
- Colour-coded progress bar  
- Real-time updates (no GPS)  

### Favourites & Reviews
- Tap heart icon to favourite  
- Offline storage, sync when online  
- Clients leave ratings/comments  
- Reviews influence credibility  

### Offline Mode
- RoomDB caches listings/favourites  
- Sync worker updates backend  
- Timestamp conflict resolution with user prompts  
- Sync button shows pending items  

### Notifications
- Push alerts for bookings and status updates  
- Preferences managed in Settings

---

## Final POE Release Notes – v1.0.0

- **Version**: v1.0.0  
- **Release Date**: 18 November 2025  

### Overview
Includes complete booking workflow, state-based tracking, verified badges, secure authentication, offline browsing, multi-language support, notifications, listings, and reviews.

### New & Completed Features
- Authentication: Google Sign-In, biometric/PIN login, JWT lifecycle  
- Booking & Scheduling: Full lifecycle, server-side audit trail  
- Service Tracking: State-based, real-time UI updates  
- Offline Mode: RoomDB caching, sync worker  
- Push Notifications: FCM alerts  
- Multi-Language Support: English, isiXhosa, isiZulu  
- Listings, Favourites & Reviews: Hustler creation, packages, sync  

### Bug Fixes & Stability
- Fixed image preview crashes  
- Improved token refresh  
- Prevented duplicate offline actions  
- Resolved isiXhosa/isiZulu layout issues  

### Testing Evidence
- Manual testing on devices  
- Unit tests for ViewModels/utilities  
- Backend route testing  
- Demo video covering rubric criteria  

### Deployment
- APK Path: GitHub Releases  
- Play Console: Not published  
- CI/CD: GitHub Actions pipeline  

### Known Limitations
- No GPS/live tracking (state-based only)  
- Only 3 languages supported  
- Conflict resolution may improve with CRDTs  
- No in-app messaging yet  

---

## POE Rubric Alignment
- Google SSO authentication  
- Biometric and PIN login  
- NoSQL database (Firebase + RoomDB)  
- Offline mode with sync  
- Real-time push notifications  
- Multi-language support  
- State-based service tracking  
- Full booking workflow  
- CI/CD pipeline  
- Screenshots and demo video  
- Professional documentation  

---

## Contributors
- Itumeleng Molawa – ST10373912  
- Ade-Eza Silongo – ST10361062  
- Lusanda Mlotshwa – ST10304204  
- Reaobaka Ntoagae – ST10402790  

---

## Demo Video
A full walkthrough video is available in the repository.

---

## APK File
- **Filename**: Grindly_v1.0.0.apk  
- **Type**: APK for demonstration  
- **Location**: GitHub Releases  

---

## GitHub Repository
[https://github.com/ReaobakaNtoagae/AppGrindly.git](https://github.com/ReaobakaNtoagae/AppGrindly.git)


