Grindly App ReadMe
Team Members
•	Itumeleng Molawa – ST10373912
•	Ade-Eza Silongo – ST10361062
•	Lusanda Mlotshwa – ST10304204
•	Reaobaka Ntoagae – ST10402790
Purpose
Grindly is a mobile first Android application built to empower South African hustlers informal service providers such as tutors, stylists, and repairers by giving them visibility, credibility, and access to digital professionalism. The app bridges the gap between grassroots entrepreneurship and modern technology, transforming everyday skills into sustainable businesses. Hustlers gain a platform to present themselves professionally, clients benefit from secure and reliable bookings, and communities thrive through trust, transparency, and inclusivity. Grindly is not just an application, it is a movement towards empowerment and growth. By digitizing the informal service economy, it creates opportunities for hustlers to expand their reach, for clients to access dependable services, and for communities to connect through innovation and accessibility.
Target Users
•	Hustlers: Create service listings, manage bookings, and build credibility
•	Clients: Discover services, book appointments, and leave reviews
Design Philosophy
•	Inclusivity: Multi-language support (English, isiXhosa, isiZulu)
•	Security: Google SSO and biometric login (fingerprint, face unlock, PIN)
•	Transparency: State-based service tracking with clear booking statuses
•	Scalability: RESTful API and modular backend architecture
•	Accessibility: Offline mode for browsing services and favourites
•	Consistency: Defined colour scheme and typography for professional UI
Colour Scheme & UI Design
Grindly uses a clean, modern colour palette and layout that enhances readability, accessibility, and brand identity across all screens.
Primary Colours
•	Purple (#6A0DAD) – Used for primary buttons (e.g., “Log In”, “Sign Up”, “Book Now”), status indicators (e.g., “Accepted”, “Completed”), and sidebar highlights.
•	White (#FFFFFF) – Used for backgrounds, input fields, and top panel text (e.g., headers like “GrindlyApp1”, “Track Your Service”).

Accent & Status Colours
•	Lavender (#E6E6FA) – Used in top panels, form backgrounds, and dashboard cards
•	Blue (#1E90FF) – “Accepted” status
•	Yellow (#FFD700) – “On the Way” status
•	Green (#32CD32) – “In Progress” status
•	Grey (#A9A9A9) – “Pending” status
Typography
•	Font Family: Palanquin
•	Headers: Palanquin Medium, white text on lavender panels
•	Body Text: Palanquin Regular, dark purple or charcoal grey
•	Buttons: Palanquin Bold, white text on purple background
Layout & Navigation
•	Top Panels: Lavender background with white title text
•	Forms: White input fields with purple borders
•	Buttons: Rounded corners, purple fill, white text
•	Bottom Navigation Bar: Purple icons on white background
•	Sidebar Menu:
o	Activated by tapping the three-line icon (hamburger menu)
o	Slides in from the left
o	White background with purple text/icons
o	Navigation items: Home, Profile, My Bookings, Favourites, Settings, Logout
Technical Stack
Layer	Technology
Frontend	Kotlin (Android Studio)
Backend	Node.js + Express
Database	Firebase + RoomDB (offline sync)
Authentication	Google SSO, biometric login
Notifications	Firebase Cloud Messaging
Version Control	GitHub + GitHub Actions
Core Features
Feature	Description
Multi-language Support	English, isiXhosa, and isiZulu. Language switching updates UI instantly.
Authentication	Google Sign-In and biometric login with secure token lifecycle.
Booking System	Full lifecycle: Pending → Accepted → On my way → In Progress → Completed
Service Tracking	Real-time UI updates based on booking status. No GPS/live location.
Service Listings & Packages	Hustlers create listings with images, pricing, and bundled offerings.
Favourites & Reviews	Clients save services and leave ratings/comments. Offline-first with sync.
Offline Mode	Browse services and favourites offline; RoomDB syncs when reconnected. Users can browse services and they can add favourites which are synced only when they are online.
Push Notifications	Alerts for bookings creations and booking status updates.
CI/CD Pipeline	GitHub Actions for build, linting, testing, and APK generation.
User Guide
Installation
•	Download the APK file (Grindly_v1.0.0.apk) from GitHub Releases
•	Transfer it to your Android device
•	Enable “Install from Unknown Sources” if prompted
•	Tap the APK to install (requires Android 8.0 or higher)
Launch & Welcome Screen
•	Displays Grindly logo with three interconnected icons (pink, purple, orange)
•	Tagline: “Showcase. Connect. Thrive.”
•	Language selector appears immediately
•	Top panel uses lavender background with white title text
Language Settings
•	Tap Settings in the bottom navigation bar
•	Choose English, isiXhosa, or isiZulu
•	UI updates instantly without restarting
•	Language preference is stored locally and persists offline
Authentication
•	Tap “Continue with Google” for secure login
•	Enable biometric login (fingerprint, face unlock, or PIN)
•	Sessions managed with secure token storage and automatic refresh
Hustler Profile Setup
•	Add profile photo, service categories, and pricing
•	Create service listings with title, description, category, price, and images
•	Bundle multiple services into Service Packages
Browsing & Booking
•	Clients browse services by category or search
•	Tap a listing to view details, images, and reviews
•	Select a time slot and tap “Book Now”
•	Hustlers accept bookings if not they stay as pending
•	Booking status updates automatically through the lifecycle
Service Tracking
•	Booking states: Pending → Accepted → On my way → In Progress → Completed
•	Status displayed with colour-coded progress bar
•	No GPS or map view tracking is state-based only
•	Real-time updates for both hustlers and clients
Favourites & Reviews
•	Tap the heart icon to favourite a service
•	Favourites stored locally and synced when online
•	After completion, clients leave ratings and comments
•	Reviews appear on hustler profiles and influence credibility
Offline Mode
•	Browse services and favourites offline
•	RoomDB stores cached listings and saved favourites
•	Sync worker updates backend when reconnected
•	Timestamp-based conflict resolution with user prompts
•	Under favourites it shows the user the number of items that require syncing by clicking on the sync button which will then update when connected to a stable internet connection
Notifications
•	Push notifications for bookings and bookings status updates
•	Notification preferences managed in Settings





Final POE Release Notes – Grindly App v1.0.0
Version: v1.0.0 Release Date: 18 November 2025
1. Overview
This release delivers the final Grindly Android application for POE assessment. It includes the complete booking workflow, state-based service tracking, verified badge management, secure authentication options, offline browsing of services and favourites, multi-language support, notifications, service listings/packages, and reviews.
2. New & Completed Features
2.1 Authentication
•	Google Sign-In
•	Biometric and PIN login
•	JWT access/refresh token lifecycle
2.2 Booking & Scheduling
•	Full booking lifecycle
•	Server-side audit trail
2.3 Service Tracking
•	State-based tracking: Pending → Accepted → On my way → In Progress → Completed
•	Real-time UI updates
•	No GPS or live location
2.4 Offline Mode
•	RoomDB stores cached services and favourites
•	Offline browsing supported
•	Sync worker with conflict resolution
2.5 Push Notifications
•	FCM alerts for bookings, reviews, badge updates
•	Bookings and bookings status updates
2.6 Multi-Language Support
•	English, isiXhosa, isiZulu
•	Instant UI updates
2.7 Listings, Favourites & Reviews
•	Hustler service creation
•	Service packages
•	Favourites and reviews with sync
3. Bug Fixes & Stability Improvements
•	Fixed image preview crashes
•	Improved token refresh
•	Prevented duplicate offline actions
•	Resolved layout issues for isiXhosa and isiZulu
4. Testing Evidence
•	Manual testing on physical devices
•	Unit tests for ViewModels and utilities
•	Backend route testing
•	Demo video showcasing all rubric criteria
5. Deployment
•	APK Path: 
•	Play Console: Not published
•	CI/CD: GitHub Actions pipeline for build, lint, and APK generation
6. Known Limitations
•	GPS/live location tracking is not implemented service tracking is state-based only
•	Only three languages currently supported (English, isiXhosa, isiZulu)
•	Timestamp conflict resolution may be improved with CRDTs in future versions
•	In-app messaging between hustlers and clients is not yet implemented
7. POE Rubric Alignment
This release meets and exceeds all rubric requirements for the final Portfolio of Evidence submission:
•	✔ Google SSO authentication
•	✔ Biometric and PIN login
•	✔ NoSQL database usage (Firebase + RoomDB)
•	✔ Offline mode with sync and conflict resolution
•	✔ Real-time push notifications via Firebase Cloud Messaging
•	✔ Multi-language support with instant UI switching
•	✔ State-based service tracking with real-time updates
•	✔ Full booking workflow with lifecycle management
•	✔ CI/CD pipeline using GitHub Actions
•	✔ Screenshots and demo video included
•	✔ Professional documentation and user guide


8. Contributors
•	Itumeleng Molawa – ST10373912
•	Ade-Eza Silongo – ST10361062
•	Lusanda Mlotshwa – ST10304204
•	Reaobaka Ntoagae – ST10402790
9. Screenshots Included
Screenshot Name	Screenshots
Login screen	 
Sign-up form	 
Hustler profile setup with Service creation & verification	  
Service Booking form	  
Service tracking interface
	  
Real-time Notifications:
A real-time notification for a new booking request, has been received 	  
Offline Mode:
Offline Mode for the T-Mobile, illustrated when the internet connection is turned off the Offline Mode orange pop-up will appear showing you that you are currently in Offline Mode	  
10. Demo Video
A full walkthrough video demonstrating all features, workflows, and UI interactions is included in the repository. YouTube Video Link: 
11. APK File
•	Filename: Grindly_v1.0.0.apk
•	Type: APK for demonstration purposes
•	Location: Available in the GitHub Releases section
12. GitHub Link
https://github.com/ReaobakaNtoagae/AppGrindly.git 
