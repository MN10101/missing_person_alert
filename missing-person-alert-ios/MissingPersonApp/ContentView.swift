import SwiftUI
import UIKit
import CoreLocation

struct Person: Identifiable, Codable {
    let id: Int64
    let fullName: String
    let imagePath: String
    let publishedAt: String
    let lastSeenLatitude: Double?
    let lastSeenLongitude: Double?

    enum CodingKeys: String, CodingKey {
        case id
        case fullName = "fullName"
        case imagePath = "imagePath"
        case publishedAt = "publishedAt"
        case lastSeenLatitude = "lastSeenLatitude"
        case lastSeenLongitude = "lastSeenLongitude"
    }
}

struct ContentView: View {
    @State private var name = ""
    @State private var lastSeenLatitude = ""
    @State private var lastSeenLongitude = ""
    @State private var selectedImage: UIImage? = nil
    @State private var alerts: [Person] = []
    @State private var message = ""
    @State private var isShowingImagePicker = false
    @State private var isLoading = false
    @StateObject private var locationManager = LocationManager()

    var body: some View {
        VStack {
            TextField(NSLocalizedString("full_name_label", comment: "Full Name"), text: $name)
                .textFieldStyle(RoundedBorderTextFieldStyle())
                .padding()
                .accessibilityLabel("Enter the full name of the missing person")

            TextField("Last Seen Latitude (Optional)", text: $lastSeenLatitude)
                .textFieldStyle(RoundedBorderTextFieldStyle())
                .padding()
                .keyboardType(.decimalPad)
                .accessibilityLabel("Enter the latitude of the last known location")

            TextField("Last Seen Longitude (Optional)", text: $lastSeenLongitude)
                .textFieldStyle(RoundedBorderTextFieldStyle())
                .padding()
                .keyboardType(.decimalPad)
                .accessibilityLabel("Enter the longitude of the last known location")

            Button(action: {
                isShowingImagePicker = true
            }) {
                Text(selectedImage == nil ? NSLocalizedString("select_image", comment: "Select Image") : NSLocalizedString("image_selected", comment: "Image Selected"))
                    .padding()
                    .frame(maxWidth: .infinity)
                    .background(selectedImage == nil ? Color.gray : Color.blue)
                    .foregroundColor(.white)
                    .clipShape(RoundedRectangle(cornerRadius: 8))
            }
            .padding(.horizontal)
            .sheet(isPresented: $isShowingImagePicker) {
                ImagePicker(image: $selectedImage)
            }
            .accessibilityLabel(selectedImage == nil ? "Select an image of the missing person" : "Image selected, tap to change")

            Button(action: {
                Task {
                    await publishAlert()
                }
            }) {
                Text(NSLocalizedString("publish_alert", comment: "Publish Alert"))
                    .padding()
                    .frame(maxWidth: .infinity)
                    .background(name.isEmpty || selectedImage == nil ? Color.gray : Color.blue)
                    .foregroundColor(.white)
                    .clipShape(RoundedRectangle(cornerRadius: 8))
            }
            .disabled(name.isEmpty || selectedImage == nil)
            .padding(.horizontal)

            if !message.isEmpty {
                Text(message)
                    .foregroundColor(message.contains("Error") ? .red : .green)
                    .padding()
                    .accessibilityLabel(message)
            }

            if isLoading {
                ProgressView()
                    .padding()
                    .accessibilityLabel("Loading")
            }

            List(alerts) { alert in
                HStack {
                    AsyncImage(url: URL(string: "http://192.168.1.100:8080/api/persons/image/\(alert.imagePath)")) { image in
                        image
                            .resizable()
                            .scaledToFill()
                    } placeholder: {
                        ProgressView()
                    }
                    .frame(width: 80, height: 80)
                    .clipShape(RoundedRectangle(cornerRadius: 8))
                    .accessibilityLabel("Photo of \(alert.fullName)")

                    VStack(alignment: .leading) {
                        Text(alert.fullName)
                            .font(.headline)
                        Text("Published: \(formatDate(alert.publishedAt))")
                            .font(.subheadline)
                            .foregroundColor(.gray)
                    }
                }
            }
        }
        .navigationTitle(NSLocalizedString("app_title", comment: "Missing Person Alert"))
        .onAppear {
            requestLocation()
            Task {
                await fetchAlerts()
            }
        }
    }

    func requestLocation() {
        locationManager.requestLocation { coordinate in
            Task {
                await fetchAlerts()
            }
        }
    }

    func formatDate(_ dateString: String) -> String {
        let formatter = ISO8601DateFormatter()
        if let date = formatter.date(from: dateString) {
            let displayFormatter = DateFormatter()
            displayFormatter.dateStyle = .medium
            displayFormatter.timeStyle = .short
            return displayFormatter.string(from: date)
        }
        return dateString
    }

    func fetchAlerts() async {
        // Load cached alerts first
        if let cachedData = UserDefaults.standard.data(forKey: "cachedAlerts"),
           let cachedAlerts = try? JSONDecoder().decode([Person].self, from: cachedData) {
            DispatchQueue.main.async {
                alerts = cachedAlerts
            }
        }

        isLoading = true
        var urlString = "http://192.168.1.100:8080/api/persons?page=0&size=10"
        if let location = locationManager.userLocation {
            urlString += "&userLatitude=\(location.latitude)&userLongitude=\(location.longitude)"
        }

        guard let url = URL(string: urlString) else {
            message = "Error: Invalid URL"
            isLoading = false
            return
        }

        var request = URLRequest(url: url)
        if let jsessionId = UserDefaults.standard.string(forKey: "JSESSIONID") {
            request.setValue("JSESSIONID=\(jsessionId)", forHTTPHeaderField: "Cookie")
        }

        do {
            let (data, _) = try await URLSession.shared.data(for: request)
            let decoder = JSONDecoder()
            let decoded = try decoder.decode([Person].self, from: data)
            DispatchQueue.main.async {
                alerts = decoded
                isLoading = false
                // Cache the alerts
                if let encoded = try? JSONEncoder().encode(decoded) {
                    UserDefaults.standard.set(encoded, forKey: "cachedAlerts")
                }
            }
        } catch {
            DispatchQueue.main.async {
                message = "Error fetching alerts: \(error.localizedDescription)"
                isLoading = false
            }
        }
    }

    func publishAlert() async {
        guard UserDefaults.standard.bool(forKey: "isAuthenticated"),
              let jsessionId = UserDefaults.standard.string(forKey: "JSESSIONID") else {
            message = "Bitte zuerst anmelden"
            return
        }

        guard !name.isEmpty, let image = selectedImage else {
            message = "Bitte füllen Sie alle erforderlichen Felder aus"
            return
        }

        if let lat = Double(lastSeenLatitude), (lat < 47.3 || lat > 55.1) {
            message = "Fehler: Breitengrad muss zwischen 47.3 und 55.1 liegen"
            return
        }
        if let lon = Double(lastSeenLongitude), (lon < 5.9 || lon > 15.0) {
            message = "Fehler: Längengrad muss zwischen 5.9 und 15.0 liegen"
            return
        }

        guard let imageData = image.jpegData(compressionQuality: 0.8), imageData.count <= 5 * 1024 * 1024 else {
            message = "Fehler: Bild muss JPEG/PNG sein und unter 5MB"
            return
        }

        isLoading = true
        guard let url = URL(string: "http://192.168.1.100:8080/api/persons/publish") else {
            message = "Fehler: Ungültige URL"
            isLoading = false
            return
        }

        var request = URLRequest(url: url)
        request.httpMethod = "POST"
        request.setValue("JSESSIONID=\(jsessionId)", forHTTPHeaderField: "Cookie")

        let boundary = "Boundary-\(UUID().uuidString)"
        request.setValue("multipart/form-data; boundary=\(boundary)", forHTTPHeaderField: "Content-Type")

        var body = Data()
        body.append("--\(boundary)\r\n".data(using: .utf8)!)
        body.append("Content-Disposition: form-data; name=\"name\"\r\n\r\n".data(using: .utf8)!)
        body.append("\(name)\r\n".data(using: .utf8)!)

        if let latitude = Double(lastSeenLatitude) {
            body.append("--\(boundary)\r\n".data(using: .utf8)!)
            body.append("Content-Disposition: form-data; name=\"lastSeenLatitude\"\r\n\r\n".data(using: .utf8)!)
            body.append("\(latitude)\r\n".data(using: .utf8)!)
        }
        if let longitude = Double(lastSeenLongitude) {
            body.append("--\(boundary)\r\n".data(using: .utf8)!)
            body.append("Content-Disposition: form-data; name=\"lastSeenLongitude\"\r\n\r\n".data(using: .utf8)!)
            body.append("\(longitude)\r\n".data(using: .utf8)!)
        }

        body.append("--\(boundary)\r\n".data(using: .utf8)!)
        body.append("Content-Disposition: form-data; name=\"image\"; filename=\"image.jpg\"\r\n".data(using: .utf8)!)
        body.append("Content-Type: image/jpeg\r\n\r\n".data(using: .utf8)!)
        body.append(imageData)
        body.append("\r\n".data(using: .utf8)!)
        body.append("--\(boundary)--\r\n".data(using: .utf8)!)

        request.httpBody = body

        do {
            let (data, response) = try await URLSession.shared.data(for: request)
            if let httpResponse = response as? HTTPURLResponse, httpResponse.statusCode == 200 {
                DispatchQueue.main.async {
                    message = "Meldung erfolgreich veröffentlicht!"
                    name = ""
                    lastSeenLatitude = ""
                    lastSeenLongitude = ""
                    selectedImage = nil
                    isLoading = false
                    Task {
                        await fetchAlerts()
                    }
                }
            } else {
                let errorMessage = String(data: data, encoding: .utf8) ?? "Unbekannter Fehler"
                DispatchQueue.main.async {
                    message = "Fehler: \(errorMessage)"
                    isLoading = false
                }
            }
        } catch {
            DispatchQueue.main.async {
                message = "Fehler beim Veröffentlichen: \(error.localizedDescription)"
                isLoading = false
            }
        }
    }
}

// Location Manager (unchanged)
class LocationManager: NSObject, ObservableObject, CLLocationManagerDelegate {
    private let locationManager = CLLocationManager()
    @Published var userLocation: CLLocationCoordinate2D?
    private var completion: ((CLLocationCoordinate2D) -> Void)?

    override init() {
        super.init()
        locationManager.delegate = self
        locationManager.desiredAccuracy = kCLLocationAccuracyBest
    }

    func requestLocation(completion: @escaping (CLLocationCoordinate2D) -> Void) {
        self.completion = completion
        if CLLocationManager.locationServicesEnabled() {
            switch locationManager.authorizationStatus {
            case .notDetermined:
                locationManager.requestWhenInUseAuthorization()
            case .restricted, .denied:
                print("Location access denied")
                completion(CLLocationCoordinate2D(latitude: 52.5200, longitude: 13.4050))
            case .authorizedWhenInUse, .authorizedAlways:
                locationManager.requestLocation()
            @unknown default:
                break
            }
        } else {
            print("Location services are not enabled")
            completion(CLLocationCoordinate2D(latitude: 52.5200, longitude: 13.4050))
        }
    }

    func locationManager(_ manager: CLLocationManager, didUpdateLocations locations: [CLLocation]) {
        if let location = locations.last {
            userLocation = location.coordinate
            completion?(location.coordinate)
        }
    }

    func locationManager(_ manager: CLLocationManager, didFailWithError error: Error) {
        print("Failed to get location: \(error)")
        completion?(CLLocationCoordinate2D(latitude: 52.5200, longitude: 13.4050))
    }

    func locationManagerDidChangeAuthorization(_ manager: CLLocationManager) {
        switch manager.authorizationStatus {
        case .authorizedWhenInUse, .authorizedAlways:
            locationManager.requestLocation()
        default:
            break
        }
    }
}

// Image Picker (unchanged)
struct ImagePicker: UIViewControllerRepresentable {
    @Binding var image: UIImage?
    @Environment(\.presentationMode) var presentationMode

    func makeUIViewController(context: Context) -> UIImagePickerController {
        let picker = UIImagePickerController()
        picker.delegate = context.coordinator
        picker.sourceType = .photoLibrary
        return picker
    }

    func updateUIViewController(_ uiViewController: UIImagePickerController, context: Context) {}

    func makeCoordinator() -> Coordinator {
        Coordinator(self)
    }

    class Coordinator: NSObject, UIImagePickerControllerDelegate, UINavigationControllerDelegate {
        let parent: ImagePicker

        init(_ parent: ImagePicker) {
            self.parent = parent
        }

        func imagePickerController(_ picker: UIImagePickerController,
                                   didFinishPickingMediaWithInfo info: [UIImagePickerController.InfoKey: Any]) {
            if let image = info[.originalImage] as? UIImage {
                parent.image = image
            }
            parent.presentationMode.wrappedValue.dismiss()
        }

        func imagePickerControllerDidCancel(_ picker: UIImagePickerController) {
            parent.presentationMode.wrappedValue.dismiss()
        }
    }
}

struct ContentView_Previews: PreviewProvider {
    static var previews: some View {
        ContentView()
    }
}