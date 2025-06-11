const apiUrl = '/api/persons';
let currentPage = 0;
const pageSize = 10;
let userLocation = null;

// Firebase configuration
const firebaseConfig = {
    apiKey: "AIzaSyAHGjEMz4QY2l2CvuGonUZUkuHnjXmk1-0",
    authDomain: "missingpersonsalert-1fe12.firebaseapp.com",
    projectId: "missingpersonsalert-1fe12",
    storageBucket: "missingpersonsalert-1fe12.firebasestorage.app",
    messagingSenderId: "95680482766",
    appId: "1:95680482766:web:7f1f2f896d246daf74ef9f",
    measurementId: "G-0M3N8ZNF0F"
};

// Initialize Firebase
firebase.initializeApp(firebaseConfig);
const messaging = firebase.messaging();

document.addEventListener('DOMContentLoaded', async () => {
    console.log('DOM loaded, initializing...');
    const alertList = document.getElementById('alert-list');
    const loadingIndicator = document.getElementById('loading-indicator');

    if (alertList && loadingIndicator) {
        loadingIndicator.style.display = 'block';
    }

    // Check if geolocation consent and coordinates are stored
    const storedLocation = localStorage.getItem('userLocation');
    if (storedLocation) {
        userLocation = JSON.parse(storedLocation);
        console.log('Using stored location:', userLocation);
        loadAlerts();
    } else if (navigator.geolocation) {
        if (confirm("Diese Website erlauben, auf Ihren Standort zuzugreifen, um nahegelegene Meldungen anzuzeigen?")) {
            navigator.geolocation.getCurrentPosition(
                (position) => {
                    userLocation = {
                        latitude: position.coords.latitude,
                        longitude: position.coords.longitude
                    };
                    // Store location in localStorage
                    localStorage.setItem('userLocation', JSON.stringify(userLocation));
                    console.log('Benutzerstandort:', userLocation);
                    loadAlerts();
                },
                (error) => {
                    console.error('Geolocation-Fehler:', error.message);
                    showMessage('Ihr Standort konnte nicht abgerufen werden. Alle Meldungen werden angezeigt.', 'error');
                    loadAlerts();
                }
            );
        } else {
            showMessage('Standortzugriff verweigert. Alle Meldungen werden angezeigt.', 'error');
            loadAlerts();
        }
    } else {
        console.error('Geolocation wird von diesem Browser nicht unterstützt.');
        showMessage('Geolocation wird von Ihrem Browser nicht unterstützt. Alle Meldungen werden angezeigt.', 'error');
        loadAlerts();
    }

    // Register service worker and request notification permission
    if ('serviceWorker' in navigator) {
        try {
            const registration = await navigator.serviceWorker.register('/js/firebase-messaging-sw.js');
            console.log('Service Worker registriert mit Scope:', registration.scope);
            await requestNotificationPermission(registration);
        } catch (error) {
            console.error('Service Worker Registrierung fehlgeschlagen:', error);
        }
    }

    const form = document.getElementById('alert-form');
    if (form) {
        form.addEventListener('submit', handleFormSubmit);
    }
    const loadMoreButton = document.getElementById('load-more');
    if (loadMoreButton) {
        loadMoreButton.addEventListener('click', loadAlerts);
    }
});

async function requestNotificationPermission(registration) {
    try {
        const permission = await Notification.requestPermission();
        if (permission === 'granted') {
            console.log('Benachrichtigungsberechtigung erteilt.');
            const token = await messaging.getToken({
                vapidKey: 'BDQGFxZq6PbG1iC3gb13PSsoA50hfA0JrflHeW-RiFnRWJnwAYZQhVs-q2b6wIFUms7RYhRydmhJVGezSl1MTxI',
                serviceWorkerRegistration: registration
            });
            console.log('FCM Token:', token);
            await fetch('/api/notifications/register', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ token })
            });
        }
    } catch (error) {
        console.error('Fehler beim Anfordern der Benachrichtigungsberechtigung:', error);
    }
}

async function handleFormSubmit(event) {
    event.preventDefault();
    console.log('Form submitted');
    const form = event.target;
    const formData = new FormData();
    const name = form.name.value.trim();
    const image = form.image.files[0];
    const lastSeenLatitude = form.lastSeenLatitude.value;
    const lastSeenLongitude = form.lastSeenLongitude.value;

    if (!name || !image) {
        showMessage('Bitte füllen Sie alle erforderlichen Felder aus.', 'error');
        console.error('Form validation failed: name or image missing');
        return;
    }

    formData.append('name', name);
    formData.append('image', image);
    if (lastSeenLatitude) formData.append('lastSeenLatitude', lastSeenLatitude);
    if (lastSeenLongitude) formData.append('lastSeenLongitude', lastSeenLongitude);

    try {
        console.log('Sending POST to /api/persons/publish');
        const response = await fetch(`${apiUrl}/publish`, {
            method: 'POST',
            body: formData,
        });

        if (!response.ok) {
            const errorText = await response.text();
            throw new Error(errorText || 'Fehler beim Veröffentlichen');
        }

        const result = await response.json();
        console.log('Publish successful, person id:', result.id);
        window.location.href = `/alert-confirmation?id=${result.id}`;
    } catch (error) {
        console.error('Publish error:', error.message);
        showMessage(`Fehler: ${error.message}`, 'error');
    }
}

async function loadAlerts() {
    console.log('Loading alerts, page:', currentPage);
    const alertList = document.getElementById('alert-list');
    const loadingIndicator = document.getElementById('loading-indicator');

    if (alertList && loadingIndicator) {
        loadingIndicator.style.display = 'block';
        alertList.innerHTML = '';
    }

    try {
        let url = `${apiUrl}?page=${currentPage}&size=${pageSize}`;
        if (userLocation) {
            url += `&userLatitude=${userLocation.latitude}&userLongitude=${userLocation.longitude}`;
        }

        console.log('Fetching alerts from:', url);
        const response = await fetch(url, {
            method: 'GET',
            headers: {
                'Accept': 'application/json'
            }
        });

        if (!response.ok) {
            const errorText = await response.text();
            throw new Error(`HTTP error ${response.status}: ${errorText}`);
        }

        const alerts = await response.json();
        console.log('Received alerts:', JSON.stringify(alerts, null, 2));

        if (alertList) {
            if (alerts.length === 0 && currentPage === 0) {
                alertList.innerHTML = '<p>Keine aktuellen Meldungen vorhanden.</p>';
                console.log('No alerts found');
            } else {
                for (const alert of alerts) {
                    const imageUrl = `/api/persons/image/${alert.imagePath}`;
                    let locationName = '';
                    if (alert.lastSeenLatitude && alert.lastSeenLongitude) {
                        try {
                            const response = await fetch(
                                `https://nominatim.openstreetmap.org/reverse?format=json&lat=${alert.lastSeenLatitude}&lon=${alert.lastSeenLongitude}&zoom=10&addressdetails=1`,
                                {
                                    headers: {
                                        'User-Agent': 'MissingPersonAlertSystem/1.0 (contact: mamocool3@gmail.com)',
                                    },
                                }
                            );
                            if (!response.ok) {
                                throw new Error('Failed to fetch location name');
                            }
                            const data = await response.json();
                            locationName = data.display_name || `${alert.lastSeenLatitude}, ${alert.lastSeenLongitude}`;
                        } catch (error) {
                            console.error('Error fetching location name:', error.message);
                            locationName = `${alert.lastSeenLatitude}, ${alert.lastSeenLongitude}`;
                        }
                    }

                    const card = document.createElement('div');
                    card.className = 'alert-card';
                    card.innerHTML = `
                        <img src="${imageUrl}" alt="${alert.fullName}" onerror="this.src='/images/placeholder.jpg'; this.alt='Bild nicht verfügbar';" role="img" aria-label="Foto von ${alert.fullName}">
                        <div>
                            <h3>${alert.fullName}</h3>
                            <p>Veröffentlicht: ${new Date(alert.publishedAt).toLocaleString('de-DE')}</p>
                            ${locationName ? `<p>Zuletzt gesehen: ${locationName}</p>` : ''}
                        </div>
                    `;
                    alertList.appendChild(card);
                }
            }
        }

        currentPage++;
        const loadMoreButton = document.getElementById('load-more');
        if (loadMoreButton) {
            loadMoreButton.style.display = alerts.length === pageSize ? 'block' : 'none';
        }
    } catch (error) {
        console.error('Error loading alerts:', error.message);
        if (alertList) {
            alertList.innerHTML = '<p>Fehler beim Laden der Meldungen: ' + error.message + '</p>';
        }
        showMessage(`Fehler beim Laden der Meldungen: ${error.message}`, 'error');
    } finally {
        if (loadingIndicator) {
            loadingIndicator.style.display = 'none';
        }
    }
}

function showMessage(message, type) {
    const messageEl = document.getElementById('form-message');
    const debugEl = document.getElementById('debug-message');
    if (messageEl) {
        messageEl.textContent = message;
        messageEl.style.color = type === 'error' ? '#d9534f' : '#5cb85c';
    }
    if (debugEl) {
        debugEl.textContent = message;
        debugEl.style.color = type === 'error' ? '#d9534f' : '#5cb85c';
        debugEl.style.display = 'block';
    }
}