importScripts('https://www.gstatic.com/firebasejs/9.22.0/firebase-app-compat.js');
importScripts('https://www.gstatic.com/firebasejs/9.22.0/firebase-messaging-compat.js');

firebase.initializeApp({
    apiKey: "AIzaSyAHGjEMz4QY2l2CvuGonUZUkuHnjXmk1-0",
    authDomain: "missingpersonsalert-1fe12.firebaseapp.com",
    projectId: "missingpersonsalert-1fe12",
    storageBucket: "missingpersonsalert-1fe12.firebasestorage.app",
    messagingSenderId: "95680482766",
    appId: "1:95680482766:web:7f1f2f896d246daf74ef9f",
    measurementId: "G-0M3N8ZNF0F"
});

const messaging = firebase.messaging();

messaging.onBackgroundMessage(function(payload) {
    const notificationTitle = payload.notification.title;
    const notificationOptions = {
        body: payload.notification.body,
//        icon: '/images/icon.png'
    };

    self.registration.showNotification(notificationTitle, notificationOptions);
});